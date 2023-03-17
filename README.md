* Make sure that your local env is set to the correct java version
* Update the number of max threads from `application.properties` file
* To run the test import `ws-client-test`
* For some of the tests depending on the number of max threads set it will take a while till they freeze
* Issue reproducable on `spring-cometd-v5-tomcat-v8`, `spring-cometd-v5-tomcat-v9`, `WebSocketEndPoint:onMessage` but not on `spring-cometd-v3-tomcat-v8` because it doesn't block `AbstractWebSocketTransaport.java:onMessage`
* To test that all threads are stuck visit localhost:8085/hello 