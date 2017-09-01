package com.samsung.lib.requestserializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.samsung.lib.requestserializer.AsyncRequestSerializer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@ContextConfiguration("/async-test-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class AsyncRequestSerializerTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncRequestSerializerTest.class);

  @Autowired
  AsyncRequestSerializer<Integer> asyncRequestSerializer;

  @Autowired
  AsyncRequestSerializer<StatisiticsWork.ScoreStat> asyncRequestSerializerStat;

  private final ExecutorService ex = Executors.newCachedThreadPool();

  private static final int SECOND = 1000;
  private static final int SLEEP_TIME = 5000;
  private static final int MAX_TRIAL = 500;
  private static final int USER_COUNT = 100;
  private static final double ERROR_TOLERANCE = 0.1;
  private static final int MAX_SCORE = 100;


  // @Test
  public void testSubmit() throws Exception {
    assertNotNull(asyncRequestSerializer);

    Map<String, AtomicInteger> rkm = new HashMap<>();

    long st = System.currentTimeMillis();
    Random random = new Random();
    TestWork work;
    for (int t = 0; t < MAX_TRIAL; t++) {
      int uid = random.nextInt(USER_COUNT);
      String uids = String.valueOf(uid);
      if (rkm.containsKey(uids)) {
        rkm.get(uids).incrementAndGet();
      } else {
        rkm.put(uids, new AtomicInteger(1));
      }
      work = new TestWork(uids);
      asyncRequestSerializer.submit(uids, work);
    }

    Thread.sleep(SLEEP_TIME);

    int totalrequest = 0;
    for (Entry<String, AtomicInteger> rk : rkm.entrySet()) {
      totalrequest += rk.getValue().intValue();
      assertEquals(rk.getValue().intValue(), TestWork.getRequestKeyCount(rk.getKey()));
    }

    assertEquals(MAX_TRIAL, totalrequest);

    LOGGER.info("Total unique uids processed -> {}", rkm.size());

    LOGGER.info("Total time each work taken -> {} sec.", TestWork.getTotalWorkTime() / SECOND);

    LOGGER.info("Time per request -> {} sec/req.", TestWork.getTotalWorkTime() / MAX_TRIAL);

    LOGGER.info("Total time to run test - {} sec.", (System.currentTimeMillis() - st) / SECOND);

  }

  /**
   * A test class to update user level min, max and mean
   */
  private static final class StatWorkAccumulator implements Runnable {

    private String userId;
    private Future<StatisiticsWork.ScoreStat> scoreStatCurrent;

    private static final Map<String, StatisiticsWork.ScoreStat> REQUEST_KEY_STAT = new ConcurrentHashMap<>();

    private static final Map<String, AtomicInteger> REQUEST_KEY_COUNTMAP = new ConcurrentHashMap<>();

    StatWorkAccumulator(String userId, Future<StatisiticsWork.ScoreStat> scoreStat) {
      this.userId = userId;
      this.scoreStatCurrent = scoreStat;
      // REQUEST KEY COUNT
      if (REQUEST_KEY_COUNTMAP.containsKey(userId)) {
        REQUEST_KEY_COUNTMAP.get(userId).incrementAndGet();
      } else {
        REQUEST_KEY_COUNTMAP.put(userId, new AtomicInteger(1));
      }
    }

    @Override
    public void run() {

      StatisiticsWork.ScoreStat scoreStatTemp;
      try {
        scoreStatTemp = scoreStatCurrent.get();
        StatisiticsWork.ScoreStat scoreStat;
        // REQUEST KEY MAP
        if (REQUEST_KEY_STAT.containsKey(userId)) {
          scoreStat = REQUEST_KEY_STAT.get(userId);
        } else {
          scoreStat = new StatisiticsWork.ScoreStat();
          REQUEST_KEY_STAT.put(userId, scoreStat);
        }
        scoreStat.maxScore = scoreStatTemp.maxScore;
        scoreStat.minScore = scoreStatTemp.minScore;
        scoreStat.mean = scoreStatTemp.mean;
      } catch (InterruptedException exception) {
        LOGGER.error(exception.getMessage());
        Thread.currentThread().interrupt();
      } catch (ExecutionException exception) {
        LOGGER.error(exception.getMessage());
      }
    }

  }

  @Test
  public void testStatWork() throws Exception {

    assertNotNull(asyncRequestSerializerStat);

    Comparator<String> byKey = (entry1, entry2) -> Integer.valueOf(entry1).compareTo(Integer.valueOf(entry2));

    Map<String, AtomicInteger> rkm = new TreeMap<>(byKey);

    long st = System.currentTimeMillis();
    Random random = new Random();
    Random randomScoreGenerator = new Random(System.currentTimeMillis());
    StatisiticsWork work;
    for (int t = 0; t < MAX_TRIAL; t++) {
      // assuming users are arriving at normal distribution
      int uid = (USER_COUNT / 2) + (int) (random.nextGaussian() * USER_COUNT / 2);
      String uids = String.valueOf(uid);
      if (rkm.containsKey(uids)) {
        rkm.get(uids).incrementAndGet();
      } else {
        rkm.put(uids, new AtomicInteger(1));
      }
      // generate a uniform distributed random number to emulate current
      // user score
      work = new StatisiticsWork(uids, randomScoreGenerator.nextInt(MAX_SCORE));
      Future<StatisiticsWork.ScoreStat> meanFuture = asyncRequestSerializerStat.submit(uids, work);

      ex.submit(new StatWorkAccumulator(uids, meanFuture));

    }

    ex.shutdown();
    ex.awaitTermination(SLEEP_TIME, TimeUnit.MILLISECONDS);

    int totalrequest = 0;
    for (Entry<String, AtomicInteger> rk : rkm.entrySet()) {
      StatisiticsWork.ScoreStat statisiticsWork = StatisiticsWork.getScoreStat(rk.getKey());

      totalrequest += rk.getValue().intValue();
      assertEquals(StatWorkAccumulator.REQUEST_KEY_COUNTMAP.get(rk.getKey()).intValue(),
          StatisiticsWork.getRequestKeyCount(rk.getKey()));

      assertEquals(StatWorkAccumulator.REQUEST_KEY_STAT.get(rk.getKey()).maxScore, statisiticsWork.maxScore,
          ERROR_TOLERANCE);
      assertEquals(StatWorkAccumulator.REQUEST_KEY_STAT.get(rk.getKey()).minScore, statisiticsWork.minScore,
          ERROR_TOLERANCE);
      assertEquals(StatWorkAccumulator.REQUEST_KEY_STAT.get(rk.getKey()).mean, statisiticsWork.mean, ERROR_TOLERANCE);
    }

    assertEquals(MAX_TRIAL, totalrequest);

    LOGGER.info("Total unique uids processed -> {}", rkm.size());

    LOGGER.info("Total time each work taken (in parallel) -> {} sec.", StatisiticsWork.getTotalWorkTime() / SECOND);

    LOGGER.info("Time per request -> {} ms/req.", StatisiticsWork.getTotalWorkTime() / MAX_TRIAL);

    LOGGER.info("Total time to run test - {} sec.", (System.currentTimeMillis() - st) / SECOND);
  }
}
