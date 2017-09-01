package com.samsung.lib.requestserializer;

/**
 * A singleton class to create a poison pill for localqueue under
 * PoolableWorkerThread
 * 
 * @author arun.y
 *
 */
public class PoisonWork<U> implements Work<U> {

  @Override
  public U call() {
    return null;
  }

}
