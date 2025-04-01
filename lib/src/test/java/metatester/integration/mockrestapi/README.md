# Integration tests

Testing Metatester integration with a mock REST api

1. Download Wiremock jar in ```lib/src/test/resources``` path
2. ```cd .\lib\src\test\resources\```
3. ```java -jar wiremock-standalone-3.10.0.jar --port 8089```
4. Run tests with ```./gradlew clean test --info```

```mappings``` folder contains mock api resources that are loaded by Wiremock service
