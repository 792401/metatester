package metatester.runner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import metatester.config.SimulatorConfig;
import metatester.http.HTTPFactory;
import metatester.http.Request;
import metatester.http.Response;
import metatester.report.FaultSimulationReport;
import metatester.report.TestLevelSimulationResults;
import org.aspectj.lang.ProceedingJoinPoint;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Runner {
    private boolean firstRun = true;
   private static final Runner INSTANCE = new Runner();

   private final FaultSimulationReport report = FaultSimulationReport.getInstance();
   private Response originalResponse;
   private Request originalRequest;
   private final List<String> faults = SimulatorConfig.getFaults();

    public static Runner getInstance(){
        return INSTANCE;
    }

    public boolean isFirstRun() {
        return firstRun;
    }

    public void setFirstRun(boolean firstRun) {
        this.firstRun = firstRun;
    }

    public Response getOriginalResponse() {
        return originalResponse;
    }

    public void setOriginalResponse(Object originalResponse) {
        Response response = HTTPFactory.createResponseFrom(originalResponse);
        this.originalResponse = response;
    }

    public Request getOriginalRequest() {
        return originalRequest;
    }

    public void setOriginalRequest(Object originalRequest) {
        Request request = HTTPFactory.createRequestFrom(originalRequest);
        this.originalRequest = request;
    }

    private  void setFieldFault(String field, String fault){
        if (originalResponse == null) {
            throw new IllegalStateException("Cannot create simulated fault because originalResponse is null.");
        }

        Map<String, Object> currentResponse = new HashMap<>(originalResponse.getResponseAsMap());
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

    public void executeTestWithSimulatedFaults(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("Executing test reruns with simulated fault responses...");
        String interceptedUrl = originalRequest.getUrl();
        for(String field: originalResponse.getResponseAsMap().keySet()){
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

    private void injectResponseWithSimulatedFault(String responseWithSimulatedFault) {
        String interceptedUrl = originalRequest.getUrl();
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
