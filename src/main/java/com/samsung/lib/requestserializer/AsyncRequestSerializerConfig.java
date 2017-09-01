package com.samsung.lib.requestserializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class AsyncRequestSerializerConfig {

  @Value("#{config['asyncrequestserializer.submit.retry-count'] ?: '500'}")
  public Integer submitRetryCount;

  @Value("#{config['asyncrequestserializer.submit.retry-delay'] ?: '150'}")
  public Integer submitRetryDelay;

  @Value("#{config['asyncrequestserializer.pool.size'] ?: '32'}")
  public Integer workerThreadPoolSize;

  @Value(value = "#{config['asyncreqserializer.poolableworkerthread.timeout'] ?: '100'}")
  public Integer localRequestQueueTimeOut;

}
