/*
  * Copyright (c) 2018 Samsung Electronics Co., Ltd All Rights Reserved
  *
  * Licensed under the Apache License, Version 2.0 (the License);
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an AS IS BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
 */
package com.samsung.lib.requestserializer;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;


/**
 * A generic blocking object pool. This is a thread safe object pool.
 * <p>
 * Underlying its based on commons pool library.
 * <p>
 * This class should have just one instance per pool.
 *
 * @author arun.y
 */
public class PoolableWorkerThreadPool<U> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PoolableWorkerThreadPool.class);

  private final GenericObjectPool<PoolableWorkerThread<U>> workerThreadPool;
  private final Map<String, PoolableWorkerThread<U>> requestKeyWorkerThreadMap;
  private final Object workerThreadPoolLock = new Object();

  /**
   * Construct a worker thread pool with size of predefined or default @see
   */
  public PoolableWorkerThreadPool(final AsyncRequestSerializerConfig asyncRequestSerializerConfig) {
    int availableProcessor = Runtime.getRuntime().availableProcessors();
    Config config = new Config();
    int poolsize = asyncRequestSerializerConfig.workerThreadPoolSize;
    config.maxActive = poolsize <= 0 ? availableProcessor : poolsize;
    config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_FAIL;
    this.workerThreadPool = new GenericObjectPool<>(
        new PoolableWorkerThreadFactory<U>(this, asyncRequestSerializerConfig), config);
    this.requestKeyWorkerThreadMap = new HashMap<>();
    LOGGER.debug("Initialized WorkerThreadPool of size {}", config.maxActive);
  }

  /**
   * This function returns an instance of pre-initialized
   * {@link PoolableWorkerThread} from underlying {@link GenericObjectPool}
   *
   * @throws Exception
   */
  public PoolableWorkerThread<U> getPoolableWorkerThread(final String requestKey) throws Exception {
    long st = System.currentTimeMillis();
    LOGGER.debug("Getting Lock to get new Worker Thread from a pool for request key {}", requestKey);
    synchronized (workerThreadPoolLock) {
      PoolableWorkerThread<U> poolableWorkerThread = requestKeyWorkerThreadMap.get(requestKey);
      if (poolableWorkerThread == null) {
        LOGGER.debug("No associated PoolableWorkerThread found for request key {}", requestKey);
        LOGGER.debug("Requesting worker thread pool to return an available worker thread, this call could be blocking");
        do {
          try {
            poolableWorkerThread = workerThreadPool.borrowObject();
          } catch (NoSuchElementException nsee) {
            LOGGER.debug("No thread worker available in pool, will wait till old worker thread returns itself");
            try {
              workerThreadPoolLock.wait();
              LOGGER.debug("Notified for worker thread availability!!, lets see if available");
            } catch (InterruptedException ie) {
              LOGGER.warn("someone interrupted while waiting for worker thread from pool, lets see if available");
            }
          }
        }
        while (poolableWorkerThread == null);
        LOGGER.debug("Worker thread received");
        requestKeyWorkerThreadMap.put(requestKey, poolableWorkerThread);
        poolableWorkerThread.setCurrentRequestKey(requestKey);
        LOGGER.debug("Worker thread {} mapped to request-key {}", poolableWorkerThread, requestKey);
      }
      LOGGER.debug("Worker thread returned in {} ms. from pool", System.currentTimeMillis() - st);
      return poolableWorkerThread;
    }
  }

  /**
   * This function returns and un-map outbound-key to Worker Thread
   * @throws Exception
   */
  public void returnPoolableWorkerThread(PoolableWorkerThread<U> workerThread) throws Exception {
    long st = System.currentTimeMillis();
    LOGGER.debug("Going to acquire lock to return WT {} to pool", workerThread.getName());
    synchronized (workerThreadPoolLock) {
      LOGGER.debug("Undo mapping of request-key {} from this worker thread", workerThread.getCurrentRequestKey());
      requestKeyWorkerThreadMap.remove(workerThread.getCurrentRequestKey());
      LOGGER.debug("Returning worker thread {} into pool", workerThread.getName());
      workerThreadPool.returnObject(workerThread);
      LOGGER.debug("Returning worker thread {} into pool - [OK]", workerThread.getName());
      workerThreadPoolLock.notifyAll();
    }
    LOGGER.debug("Return worker thread into pool - [OK]");
    LOGGER.debug("Worker thread returned in {} ms. to pool", System.currentTimeMillis() - st);
  }
}
