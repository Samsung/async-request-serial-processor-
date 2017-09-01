package com.samsung.lib.requestserializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class PoolableWorkerThread<U> extends Thread {

  private static final Logger LOGGER = LoggerFactory.getLogger(PoolableWorkerThread.class);

  private final PoolableWorkerThreadPool myPool;
  private final AsyncRequestSerializerConfig asyncRequestSerializerConfig;

  private final BlockingQueue<FutureTask<U>> localRequestQueue = new LinkedBlockingQueue<>();
  private final Object localRequestQueueLock = new Object();
  private boolean isActive = false;
  /*
   * This flag will be set by destroy function of Thread pool Making this true will lead to stopping
   * current worker thread
   */
  private boolean isDestroyed = false;

  private String currentRequestKey;

  public PoolableWorkerThread(final PoolableWorkerThreadPool myPool,
                              final AsyncRequestSerializerConfig asyncRequestSerializerConfig) {
    this.myPool = myPool;
    this.asyncRequestSerializerConfig = asyncRequestSerializerConfig;
    LOGGER.info("Created PoolableWorkerThread attached to BlockingWorkerThreadPool");
  }

  String getCurrentRequestKey() {
    return currentRequestKey;
  }

  void setCurrentRequestKey(String currentRequestKey) {
    this.currentRequestKey = currentRequestKey;
  }

  @Override
  public void run() {
    try {
      LOGGER.debug("I am ready to run however, will block till something is assigned in my local work queue");
      FutureTask<U> request = localRequestQueue.take();
      if (request != null) {
        LOGGER.debug("Received request task from local work queue");
        doWork(request);
      } else {
        LOGGER.warn("Received null request task from local work queue. This should not happen!.");
      }
    } catch (InterruptedException interruptedException) {
      LOGGER.warn("Interrupted while blocking for request task in local work queue");
    } catch (ExecutionException executionException) {
      LOGGER.error("Error while executing local requests", executionException);
    }
    while (true) {
      try {
        FutureTask<U> request = localRequestQueue
            .poll(asyncRequestSerializerConfig.localRequestQueueTimeOut, TimeUnit.MILLISECONDS);
        if (request == null) {
          LOGGER.debug("Nothing received since past {} {}", asyncRequestSerializerConfig.localRequestQueueTimeOut,
              TimeUnit.MILLISECONDS);
          synchronized (localRequestQueueLock) {
            LOGGER.debug("Checking again in synchronized block if something got added, while polling timed-out");
            request = localRequestQueue.poll();
            if (request == null) {
              LOGGER.debug("Nothing really received in work queue, safe to release/detach itself from {} requestKey",
                  currentRequestKey);
              LOGGER.debug("Returning myself into pool");
              myPool.returnPoolableWorkerThread(this);
              LOGGER.debug("Returning myself into pool - [OK]");
              /*
               * sometime pool can call destroy immediately after returning object to pool, so in
               * that case, we need to return now
               */
              if (isDestroyed) {
                LOGGER.info("I am done, My pool wants me to die, stopping local executor service");
                return;
              }

              // PLACE to release/clean-up stuff
              isActive = false;
              currentRequestKey = null;
              do {
                try {
                  LOGGER.debug("Going to wait till notified");
                  /* To awaken, please call activate() */
                  localRequestQueueLock.wait();
                  LOGGER.debug("Notified!, time to get back to work");
                  break;// break from wait
                } catch (InterruptedException ie) {
                  LOGGER.warn("Interrupted or spurious wake up, will check if isActive is set");
                }
              } 
              while (!isActive);
              // check if this wake-up is to get back to work or to stop and
              // release this worker thread
              if (isActive) {
                LOGGER.debug("Wake up by, activate");
                // I was woke up by activate function as isActive is true,
                continue;// continue to look for new work
              } else {
                // isDestroyed be true
                LOGGER.debug("Wake up by, kill");
                LOGGER.info("I am done, My pool wants me to die, stopping local executor service");
                return;
              }
            } else {
              LOGGER.debug("something got added in work queue, while I timed-out, back to work");
            }
          }
        }
        // Do the real work
        doWork(request);
      } catch (Exception exception) {
        LOGGER.error("Error while executing local requests", exception);
      }
    }
  }

  private void doWork(FutureTask<U> request) throws ExecutionException, InterruptedException {
    long st = System.currentTimeMillis();
    LOGGER.debug("Blocking till work is completed!!");
    request.run();
    request.get();
    LOGGER.debug("Time to complete work is {} ms.", System.currentTimeMillis() - st);
    LOGGER.debug("Current localRequestQueue size is -> {}", localRequestQueue.size());
  }

  int getWorkQueueSize() {
    return localRequestQueue.size();
  }

  /**
   * Thread-Unsafe method to add outbound into worker thread local queue
   *
   * @param request - Instance of {@link Work}
   * @return - boolean value to indicate if submission is successful. False indicates, the worker
   *         thread is no longer active.
   */
  public Future<U> assign(Work<U> request) {
    LOGGER.debug("Aquaring lock to add request into local queue of Worker Thread {}", Thread.currentThread().getName());
    synchronized (localRequestQueueLock) {
      LOGGER.debug("Lock received");
      if (isActive) {
        FutureTask<U> future = new FutureTask<>(request);
        localRequestQueue.add(future);
        LOGGER.debug("Added request into local work queue");
        return future;
      } else {
        LOGGER.debug("The worker thread {} is no longer active, "
            + "client may need to get it again a new worker thread", Thread.currentThread().getName());
        return null;
      }
    }
  }

  /**
   * To be called by factory, upon getting this instance from pool
   */
  public void activate() {
    LOGGER.debug("Request to activate worker thread {}", Thread.currentThread().getName());
    synchronized (localRequestQueueLock) {
      LOGGER.debug("Got lock, notifying to wake up {}", Thread.currentThread().getName());
      // since 'this' is only thread waiting, it will wake-up 'this'
      // thread
      isActive = true;
      localRequestQueueLock.notify();
    }
    // PLACE to do any initialization
    LOGGER.debug("Request to activate worker thread - [OK]");
  }

  /**
   * To be called by pool to permanently release this worker thread
   */
  public void kill() {
    synchronized (localRequestQueueLock) {
      LOGGER.debug("notifying to kill {}", Thread.currentThread().getName());
      // since 'this' is only thread waiting, it will wake-up 'this'
      // thread
      isDestroyed = true;
      localRequestQueueLock.notify();
    }

  }

  /**
   * To be called by factory before releasing object back into pool
   */
  public void passivate() {
    LOGGER.debug("Request to passivate this worker thread {}", Thread.currentThread().getName());
    LOGGER.debug("Request to passivate this worker thread - [OK]");
  }
}
