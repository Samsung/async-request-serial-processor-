# async-request-serializer

A library to serially process asyncronous request. 

> This is a Java library to serially process asynchronous requests by some request criteria, yet parallely processing requests which 
> are independent (or not having same request criteria). This is generally required in scenarios where requests are arriving
> asynchronously and can be processed parrallely with a condition that request with certain criteria (e.g. request userId) to be 
> processed in serially. The measured performance shows good improvement over single threaded processing on a multicore machines.

## Use case

Imagine asynchronous requests are coming in a queue and consumer has to process all the requests. Also there is a restriction that request which belongs to a single key (say a user id) must be processed in order they arrived in a queue. In order to fulfill this serial processing requirement, one can choose to have a single thread consumer, which will guarnatee serial processing order. However this will not be efficient since requests which belong to differnent key, can still be processed in parallel. This library is exactly meant for doing it. 

Internally it maintains pool of worker thread, and assigngs a worker thred to incoming request, Each working thread will have it own internal queue, where incoming request from a given key will be appened. In order word it creates a stickyness between worker thread and key. The stickyness is only up to limited time, if there is no new request since last request is received, the worker thread is released back to thread pool. The pool will then reassign the worker thread to new incoming request.

## Getting Started (From source)

1. Clone this project
2. Edit src/main/resource/requestserializer.config as per your requirement
3. Build jar, mvn clean package
4. Include  async-request-serializer-1.0-SNAPSHOT.jar in your project


## API documentation

The primary classes in order to use this library is as follows, currently the library is using Spring framework (we are planning to drop that dependency though):

1. Autowire the AsyncRequestSerializer instance, this the entry point for this library. 
`@Autowired private AsyncRequestSerializer<T> asyncRequestSerializer` //replace T with the class representing a return class type.
2. Extend Work<T> class which has single function `public T call()`, this will be your actual logic that you want to run.
3. Submit your job by calling `asyncRequestSerializer.submit(String key, Work<T> work)`, this is non-blocking call and return Future<T>
4. From returned future object you can get result of your processing.  
  
## Sample code

```java
/**
* A sample work class, this is where 
* the actual business logic will go
*/
class MyWork implements Work<MyResponse> {
  public MyResponse call() {
      //your business logic
      ...
      MyResponse myResponse;
      return myResponse;
  }
}

@Autowired private AsyncRequestSerializer<> asyncRequestSerializer;

//create your work class
Work<MyResponse> work = new MyWork();

//Submit the work to async request serailzier, key should a string on which we want to serialize the processing. 
Future<MyResponse> myResponseFuture = asyncRequestSerializer.submit(key, work);  

MyResponse myResponse = myResponseFuture.get();  
```
