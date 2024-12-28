package aop;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import okhttp3.Response;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

@Aspect
public class CombinedInterceptorAspect {
    private String interceptedUrl; // To store the intercepted URL
    private int interceptedStatusCode; // To store the intercepted status code

    private static WireMockServer wireMockServer; // WireMock server instance
    private boolean originalTestExecuted = false; // Flag to track original execution
    private String originalResponse = null; // To store the original response for mutation

    static {
        // Start WireMock server when the class is loaded
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8080));
        wireMockServer.start();
        System.out.println("WireMock server started on port 8080");
    }

    @Around("execution(@org.junit.jupiter.api.Test * *(..))")
    public Object interceptTestMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Intercepting test method: " + joinPoint.getSignature());

        System.out.println("Executing original test...");
        Object result = joinPoint.proceed();
        System.out.println("Original test execution completed.");

        if (originalResponse == null) {
            throw new IllegalStateException("Original response was not captured. Ensure response interceptors are working.");
        }

        // Rerun tests with mutated responses
        executeWithMutations(joinPoint);
        return result;
    }

    private void executeWithMutations(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Executing test reruns with mutated responses...");
        for (int mutationIndex = 0; mutationIndex < 2; mutationIndex++) {
            System.out.println("Setting up mutation #" + mutationIndex);
            setMutation(mutationIndex);

            System.out.println("Executing test with mutation #" + mutationIndex);
            try {
                joinPoint.proceed();
                System.out.println("Test passed for mutation #" + mutationIndex);
            } catch (Throwable t) {
                System.out.println("Test failed for mutation #" + mutationIndex + ": " + t.getMessage());
            }
        }

        System.out.println("All test executions (original + mutations) are completed.");
    }

    @Around("execution(* java.net.HttpURLConnection.connect(..))")
    public Object interceptHttpURLConnection(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Intercepted HttpURLConnection call");
        HttpURLConnection connection = (HttpURLConnection) joinPoint.getTarget();
        Object result = joinPoint.proceed();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            originalResponse = responseBuilder.toString();
            System.out.println("Response intercepted for HttpURLConnection: " + originalResponse);
        } catch (Exception e) {
            System.out.println("Unable to read response for HttpURLConnection: " + e.getMessage());
        }

        return result;
    }

    @Around("execution(* org.apache.http.impl.client.CloseableHttpClient.execute(..))")
    public Object interceptApacheHttpClient(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Intercepted Apache HttpClient call");

        Object[] args = joinPoint.getArgs();
        interceptedUrl = ((HttpGet)args[0]).getURI().toString();
        System.out.println("Intercepted URL " + interceptedUrl);
        Object result = joinPoint.proceed();

        if (result instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) result;

            // Extract response body and status code
            originalResponse = EntityUtils.toString(response.getEntity());
            interceptedStatusCode = response.getStatusLine().getStatusCode(); // Save the status code

            System.out.println("Response intercepted for Apache HttpClient: " + originalResponse);
            System.out.println("Status Code intercepted: " + interceptedStatusCode);

            // Reassign entity for reuse
            response.setEntity(new StringEntity(originalResponse));
        }

        return result;
    }




    @Around("execution(* okhttp3.Call.execute(..))")
    public Object interceptOkHttpClient(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Intercepted OkHttpClient call");
        Object result = joinPoint.proceed();

        if (result instanceof Response) {
            Response response = (Response) result;

            // Capture and log response body
            originalResponse = response.peekBody(Long.MAX_VALUE).string();
            System.out.println("Response intercepted for OkHttpClient: " + originalResponse);

            // Return the original response
            return response;
        }

        return result;
    }

    private void setMutation(int mutationIndex) {
        if (originalResponse == null) {
            throw new IllegalStateException("Cannot create mutations because `originalResponse` is null.");
        }

        String mutatedResponse;
        switch (mutationIndex) {
            case 0:
                mutatedResponse = originalResponse.replaceAll("\"(\\w+)\":\\s?\"[^\"]*\"", "\"$1\": null");
                break;
            case 1:
                mutatedResponse = originalResponse.replaceAll(":\\s?(\\d+)", ": 99999");
                break;
            default:
                mutatedResponse = originalResponse;
        }

        System.out.println("Mutated response created: " + mutatedResponse);
        injectMutatedResponse(mutatedResponse);
    }

    private void injectMutatedResponse(String mutatedResponse) {
        System.out.println("Injected mutated response: " + mutatedResponse);

        WireMock.stubFor(WireMock.any(WireMock.urlEqualTo(interceptedUrl))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(mutatedResponse)));
    }

    static {
        // Stop WireMock server when the class is unloaded
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (wireMockServer != null) {
                wireMockServer.stop();
                System.out.println("WireMock server stopped");
            }
        }));
    }
}
