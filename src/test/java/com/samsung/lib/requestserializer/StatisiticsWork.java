package com.samsung.lib.requestserializer;

import com.samsung.lib.requestserializer.Work;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A test work to compute mean and standard deviation of stream of numbers
 * 
 * @author arun.y
 *
 */
public class StatisiticsWork implements Work<StatisiticsWork.ScoreStat> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestWork.class);

  private String name;
  private double score;

  /**
   * outbound key to [mean][s.d.] mapping
   */
  private static final Map<String, ScoreStat> REQUEST_KEY_STAT = new ConcurrentHashMap<>();

  private static final Map<String, AtomicInteger> REQUEST_KEY_COUNTMAP = new ConcurrentHashMap<>();

  private static final AtomicLong TOTAL_WORKTIME = new AtomicLong();

  public StatisiticsWork(final String name, double score) {
    this.name = name;
    this.score = score;
  }

  @Override
  public ScoreStat call() {
    long st = System.currentTimeMillis();
    ScoreStat scoreStat;
    // REQUEST KEY COUNT
    if (REQUEST_KEY_COUNTMAP.containsKey(name)) {
      REQUEST_KEY_COUNTMAP.get(name).incrementAndGet();
    } else {
      REQUEST_KEY_COUNTMAP.put(name, new AtomicInteger(1));
    }
    // REQUEST KEY MAP
    if (REQUEST_KEY_STAT.containsKey(name)) {
      scoreStat = REQUEST_KEY_STAT.get(name);
    } else {
      scoreStat = new ScoreStat();
      REQUEST_KEY_STAT.put(name, scoreStat);
    }
    if (score > scoreStat.maxScore) {
      scoreStat.maxScore = score;
    }
    if (score < scoreStat.minScore) {
      scoreStat.minScore = score;
    }
    scoreStat.mean = ((scoreStat.mean * scoreStat.number) + score) / ++scoreStat.number;
    try {
      Thread.sleep((int) score * 10);
    } catch (InterruptedException exception) {
      LOGGER.trace("InterruptedException");
    }
    TOTAL_WORKTIME.getAndAdd(System.currentTimeMillis() - st);
    return scoreStat;
  }

  public static long getTotalWorkTime() {
    return TOTAL_WORKTIME.longValue();
  }

  public static int getRequestKeyCount(final String name) {
    return REQUEST_KEY_COUNTMAP.get(name).intValue();
  }

  public static ScoreStat getScoreStat(final String name) {
    return REQUEST_KEY_STAT.get(name);
  }

  public static class ScoreStat {
    public double maxScore = 0;
    public double minScore = 0;
    public double mean = 0;
    public int number;
  }

}


