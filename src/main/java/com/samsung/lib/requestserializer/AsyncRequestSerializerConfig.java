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


public class AsyncRequestSerializerConfig {
  final int submitRetryCount;
  final int submitRetryDelay;
  final int workerThreadPoolSize;
  final int localRequestQueueTimeOut;

  private AsyncRequestSerializerConfig(Builder builder) {
    this.submitRetryCount = builder.submitRetryCount;
    this.submitRetryDelay = builder.submitRetryDelay;
    this.workerThreadPoolSize = builder.workerThreadPoolSize;
    this.localRequestQueueTimeOut = builder.localRequestQueueTimeOut;
  }

  public static class Builder {
    private int submitRetryCount = 500;
    private int submitRetryDelay = 150;
    private int workerThreadPoolSize = 32;
    private int localRequestQueueTimeOut = 100;

    public Builder setSubmitRetryCount(int submitRetryCount) {
      this.submitRetryCount = submitRetryCount;
      return this;
    }

    public Builder setSubmitRetryDelay(int submitRetryDelay) {
      this.submitRetryDelay = submitRetryDelay;
      return this;
    }

    public Builder setWorkerThreadPoolSize(int workerThreadPoolSize) {
      this.workerThreadPoolSize = workerThreadPoolSize;
      return this;
    }

    public Builder setLocalRequestQueueTimeOut(int localRequestQueueTimeOut) {
      this.localRequestQueueTimeOut = localRequestQueueTimeOut;
      return this;
    }

    public AsyncRequestSerializerConfig build() {
      return new AsyncRequestSerializerConfig(this);
    }
  }

}
