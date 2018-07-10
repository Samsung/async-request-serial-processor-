# async-request-serializer

A library to serially process asyncronous request. 

> This is a Java library to serially process asynchronous requests by some request criteria, yet parallely processing requests which 
> are independent (or not having same request criteria). This is generally required in scenarios where requests are arriving
> asynchronously and can be processed parrallely with a condition that request with certain criteria (e.g. request userId) to be 
> processed in serially. The measured performance shows good improvement over single threaded processing on a multicore machines.


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
