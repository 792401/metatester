package aop;

import io.restassured.response.Response;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;

@Aspect
public class ApiInterceptorAspect {

    private static Runnable assertionRunner;

    @Around("execution(io.restassured.response.Response io.restassured.specification.RequestSpecification.get(..)) || " +
            "execution(io.restassured.response.Response io.restassured.specification.RequestSpecification.post(..))")
    public Object interceptResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("[RestAssuredResponseInterceptor] Intercepting method: " + joinPoint.getSignature());

        Response response = (Response) joinPoint.proceed();

        System.out.println("[RestAssuredResponseInterceptor] Original Response Body: " + response.getBody().asString());

        String modifiedBody = response.getBody().asString().replace("\"id\": 1", "\"id\": null");

        simulateAssertionsOnModifiedResponse(modifiedBody);

        return response;
    }

    private void simulateAssertionsOnModifiedResponse(String modifiedBody) {
        System.out.println("[RestAssuredResponseInterceptor] Modified Response Body: " + modifiedBody);

        if (assertionRunner != null) {
            try {
                // Simulate the failure assertion due to id being null
                assertionRunner.run(); // Re-run original assertions
                throw new AssertionError("[Modified Assertion] Test failed: Expected id=1, but id=null in the modified response.");
            } catch (AssertionError e) {
                System.err.println("[Assertion Result] Assertions failed for modified response: " + e.getMessage());
            }
        } else {
            System.out.println("[Assertion Result] No assertions captured to run on modified response.");
        }
    }

    @Around("execution(* org.junit.jupiter.api.Assertions.*(..))")
    public Object captureAssertions(ProceedingJoinPoint joinPoint) throws Throwable {
        assertionRunner = () -> {
            try {
                joinPoint.proceed();
            } catch (Throwable throwable) {
                throw new AssertionError(throwable);
            }
        };
        return joinPoint.proceed();
    }
}
