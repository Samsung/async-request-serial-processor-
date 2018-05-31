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

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.samsung.lib.requestserializer.Work;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestWork implements Work<Integer> {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(TestWork.class);

	private String name;

	private static final Map<String, AtomicInteger> REQUEST_KEY_COUNTMAP = new ConcurrentHashMap<String, AtomicInteger>();
	
	private static final AtomicLong TOTAL_WORKTIME = new AtomicLong();
	
	private static final Random RANDOM = new Random(System.currentTimeMillis());

	public TestWork(final String name) {
		this.name = name;
	}

	@Override
	public Integer call() {
		int workTime = RANDOM.nextInt(250); 
		try {
			Thread.sleep(workTime);
			TOTAL_WORKTIME.getAndAdd(workTime);
		} catch (InterruptedException e) {
			LOGGER.warn("Error during sleep", e);
		}		
		LOGGER.debug("My name is " + name);
		if (REQUEST_KEY_COUNTMAP.containsKey(name)) {
			REQUEST_KEY_COUNTMAP.get(name).incrementAndGet();
		} else {
			REQUEST_KEY_COUNTMAP.put(name, new AtomicInteger(1));
		}
		return workTime;
	}

	public static int getRequestKeyCount(final String name) {
		return REQUEST_KEY_COUNTMAP.get(name).intValue();
	}
	
	public static long getTotalWorkTime() {
		return TOTAL_WORKTIME.longValue();
	}
}