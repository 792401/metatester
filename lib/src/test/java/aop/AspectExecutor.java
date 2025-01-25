package aop;

import com.github.tomakehurst.wiremock.client.WireMock;
import okhttp3.Response;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

@Aspect
public class AspectExecutor {
    private String interceptedUrl;
    private String originalResponse = null;
    private boolean firstRun = true;

    @Around("execution(@org.junit.jupiter.api.Test * *(..))")
    public Object interceptTestMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Intercepting test method: " + joinPoint.getSignature());

        System.out.println("Executing test...");
        Object result = joinPoint.proceed();

        if (firstRun) {
            if (originalResponse == null) {
                throw new IllegalStateException("Original response was not captured. Ensure response interceptors are working.");
            }
            firstRun = false;
            System.out.println("First run completed. Original response captured.");
        }
        executeWithSimulatedFaults(joinPoint);

        return result;
    }

    @Around("execution(* org.apache.http.impl.client.CloseableHttpClient.execute(..))")
    public Object interceptApacheHttpClient(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        if (args.length > 0 && args[0] instanceof HttpGet) {
            HttpGet originalRequest = (HttpGet) args[0];
            interceptedUrl = originalRequest.getURI().toString();
            System.out.println("Intercepted URL: " + interceptedUrl);

            if (!firstRun) {
                URI originalUri = new URI(interceptedUrl);
                URI redirectedUri = new URI("http", null, "localhost", 8080, originalUri.getPath(), originalUri.getQuery(), null);
                System.out.println("Redirecting request to: " + redirectedUri);

                HttpGet newRequest = new HttpGet(redirectedUri);
                newRequest.setHeaders(originalRequest.getAllHeaders());
                args[0] = newRequest;
            }
        }

        Object result = joinPoint.proceed(args);

        if (result instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) result;

            if (firstRun) {
                originalResponse = EntityUtils.toString(response.getEntity());
                System.out.println("Original response intercepted: " + originalResponse);

                // Reassign the entity for reuse during reruns
                response.setEntity(new StringEntity(originalResponse));
            } else {
                System.out.println("Rerun response intercepted (simulated fault applied).");
            }
        }

        return result;
    }


    @Around("execution(* okhttp3.Call.execute(..))")
    public Object interceptOkHttpClient(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Intercepted OkHttpClient call");
        Object result = joinPoint.proceed();

        if (result instanceof Response) {
            Response response = (Response) result;

            originalResponse = response.peekBody(Long.MAX_VALUE).string();
            System.out.println("Response intercepted for OkHttpClient: " + originalResponse);

            return response;
        }

        return result;
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

    private void executeWithSimulatedFaults(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Executing test reruns with simulated fault responses...");
        for (int simulatedFaultIndex = 0; simulatedFaultIndex < 2; simulatedFaultIndex++) {
            System.out.println("Setting up simulated fault #" + simulatedFaultIndex);
            setSimulatedFault(simulatedFaultIndex);

            System.out.println("Executing test with simulated fault #" + simulatedFaultIndex);
            try {
                joinPoint.proceed();
                System.err.println("[Fault not detected] Test passed for simulated fault #" + simulatedFaultIndex);
            } catch (Throwable t) {
                System.out.println("[Fault detected] Test failed for simulated fault #" + simulatedFaultIndex + ": " + t.getMessage());
            }
        }
        System.out.println("All test executions (original + simulated faults) are completed.");
    }



    private void setSimulatedFault(int simulatedFaultIndex) {
        if (originalResponse == null) {
            throw new IllegalStateException("Cannot create simulated fault because `originalResponse` is null.");
        }

        String currentResponse = originalResponse;

        String responseWithSimulatedFault;
        switch (simulatedFaultIndex) {
            case 0:
                responseWithSimulatedFault = currentResponse.replaceAll("\"(\\w+)\":\\s?\"[^\"]*\"", "\"$1\": null");
                break;
            case 1:
                responseWithSimulatedFault = currentResponse.replaceAll(":\\s?(\\d+)", ": 99999");
                break;
            default:
                responseWithSimulatedFault = currentResponse;
        }

        System.out.println("Simulated fault response created: " + responseWithSimulatedFault);
        injectResponseWithSimulatedFault(responseWithSimulatedFault);
    }

    private void injectResponseWithSimulatedFault(String responseWithSimulatedFault) {
        if (interceptedUrl == null || interceptedUrl.isEmpty()) {
            throw new IllegalStateException("Intercepted URL is null or empty. Cannot set up WireMock stub.");
        }

        String requestPath = URI.create(interceptedUrl).getPath();
        System.out.println("Setting up WireMock stub for path: " + requestPath);

        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(requestPath))
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseWithSimulatedFault)
                        .withStatus(200)));
    }

}
