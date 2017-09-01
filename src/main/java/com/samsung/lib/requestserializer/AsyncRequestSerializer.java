package com.samsung.lib.requestserializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;


/**
 * This class is an entry point into request serialization framework. The caller
 * has to simply submit a request into this sub-system.
 * <p>
 * It has to be noted that submit is blocking call, so in case the sub-system is
 * fully occupied (i.e. all the pooled worker threads are allocated) The caller
 * will be blocked till at least one worker thread releases it self into pool.
 *
 * @author arun.y
 *
 */
@Component
public class AsyncRequestSerializer<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncRequestSerializer.class);

  private PoolableWorkerThreadPool<T> poolableWorkerThreadPool;

  @Autowired
  private AsyncRequestSerializerConfig asyncRequestSerializerConfig;

  @PostConstruct
  public void init() {
    LOGGER.debug("Initialized AsyncRequestSerializer");
    poolableWorkerThreadPool = new PoolableWorkerThreadPool<>(asyncRequestSerializerConfig);
    LOGGER.debug("Initialized AsyncRequestSerializer - [OK]");
  }

  /**
   * This function allows client application to submit AE request.
   * Please note this function is synchronized so if multiple thread submits
   * request, it can be blocking. Ideally in order to control the chronological
   * processing of request under same request, all the request with same request
   * key should be submitted by same thread at a time, if multiple thread
   * submits request with same request key, then chronological order may be
   * lost.
   *
   *
   * @param requestKey - request key is for current request (Should not be null)
   * @param request - an instance of {@link Work} (Should not be null)
   * @throws Exception - In case something fails or bad parameter is passed
   */
  public synchronized <U extends Work<T>> Future<T> submit(final String requestKey, final U request) throws Exception {

    // Parameter sanity check
    Objects.requireNonNull(requestKey, "Request key is mandatory field");
    Objects.requireNonNull(request, "Submitted request itself is null");

    LOGGER.debug("Assigning request to mapped worker thread");
    LOGGER.debug("Requesting worker thread for request-key {} from pool", requestKey);
    LOGGER.debug("Requesting worker thread for request-key {} from pool - [OK]", requestKey);
    int submitRetryCount = asyncRequestSerializerConfig.submitRetryCount;

    // Get thread, give it a name and assign request
    PoolableWorkerThread<T> poolableWorkerThread = poolableWorkerThreadPool.getPoolableWorkerThread(requestKey);
    LOGGER.debug("Received Worker Thread {} against request key {}", poolableWorkerThread.getName(), requestKey);
    Future<T> result = poolableWorkerThread.assign(request);

    // null return from assign method indicates, that by the time assign was
    // called The worker thread may have released itself into queue.
    while (submitRetryCount > 0 && result == null) {
      LOGGER.warn("Failed to submit request to worker thread {}, will try {} more times",
          poolableWorkerThread.getName(), submitRetryCount);
      --submitRetryCount;

      //  Sleep, Get thread, give it a name and assign request, if not success repeat.
      Thread.sleep(asyncRequestSerializerConfig.submitRetryDelay);
      LOGGER.debug("Requesting worker thread for request-key {} again from pool", requestKey);
      poolableWorkerThread = poolableWorkerThreadPool.getPoolableWorkerThread(requestKey);
      result = poolableWorkerThread.assign(request);
      if (result != null) {
        LOGGER.debug("Requesting worker thread for request-key {} again from pool - [OK]", requestKey);
      }
    }
    if (submitRetryCount <= 0) {
      LOGGER.error("Failed to submit request in {} attmepts", asyncRequestSerializerConfig.submitRetryCount);
      LOGGER.error("This is error situation");
      throw new AsyncRequestSerializerException("Error submitting request");
    }
    LOGGER.debug("Assigning request to mapped worker thread - [OK]");
    return result;
  }
}
