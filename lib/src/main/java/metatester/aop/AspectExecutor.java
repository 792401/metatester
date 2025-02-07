package metatester.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import metatester.config.SimulatorConfig;
import metatester.report.FaultSimulationReport;
import metatester.report.TestLevelSimulationResults;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Aspect
public class AspectExecutor {
    private String interceptedUrl;
    private String originalResponse = null;
    private Map<String, Object> responseMap = new HashMap<>();
    private boolean firstRun = true;
    private final List<String> faults = SimulatorConfig.getFaults();
    private final FaultSimulationReport report = FaultSimulationReport.getInstance();

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
        if(!SimulatorConfig.isTestExcluded(joinPoint.getSignature().getName())){
            executeWithSimulatedFaults(joinPoint);
        }

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
                ObjectMapper objectMapper = new ObjectMapper();
                responseMap = objectMapper.readValue(originalResponse, new TypeReference<Map<String, Object>>() {});
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
        for(String field: responseMap.keySet()){
            for(String fault: faults){
                TestLevelSimulationResults testLevelSimulationResults = new TestLevelSimulationResults();
                testLevelSimulationResults.setTest(joinPoint.getSignature().getName());
                setFieldFault(field,fault);
                System.out.println("Executing test with simulated fault: %s for field %s fault, field");
                try {
                    joinPoint.proceed();
                    testLevelSimulationResults.setCaught(false);
                    System.err.println("[FAULT NOT DETECTED] Test passed for simulated fault "+ fault + " for field " + field);
                } catch (Throwable t) {
                    testLevelSimulationResults.setCaught(true);
                    testLevelSimulationResults.setError(t.getMessage());
                    System.out.println("[FAULT DETECTED] Test failed for simulated fault " + fault + " for field " + field);
                    System.out.println("[FAIL ERROR]: " + t.getMessage());
                }
                report.setEndpoint(URI.create(interceptedUrl).getPath())
                        .setTestResult(testLevelSimulationResults)
                        .setField(field)
                        .setFaultType(fault)
                        .apply();
            }
        }

        System.out.println("All test executions (original + simulated faults) are completed.");
    }

    private void setFieldFault(String field, String fault){
        if (originalResponse == null) {
            throw new IllegalStateException("Cannot create simulated fault because `originalResponse` is null.");
        }
        Map<String, Object> currentResponse = new HashMap<>(responseMap);
        switch (fault) {
            case "null_field" -> currentResponse.put(field, null);
            case "missing_field" -> currentResponse.remove(field);
            default -> currentResponse = currentResponse;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String responseAsString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(currentResponse);
            System.out.println("Simulated fault response created: " + responseAsString);
            injectResponseWithSimulatedFault(responseAsString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
