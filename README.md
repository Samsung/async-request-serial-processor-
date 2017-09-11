# async-request-serializer

A library to serialize asyncronous request. 

> This is a Java library for serializing asynchronous requests by some request criteria, yet parallely processing requests which 
> are independent (or not having same request criteria). This is generally required in scenarios where requests are arriving
> asynchronously and can be processed parrallely with a condition that request with certain criteria (e.g. request userId) to be 
> processed in serially. The measured performance shows good improvement over single threaded processing.
