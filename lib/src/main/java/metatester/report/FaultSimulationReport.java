package metatester.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FaultSimulationReport {
    private static final FaultSimulationReport INSTANCE = new FaultSimulationReport();
    private static final String DEFAULT_REPORT_PATH = "fault_simulation_report.json";

    private String endpoint;
    private String field;
    private String simulatedFault;
    private TestLevelSimulationResults testResult;
    private final Map<String, Object> report = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();


    public static FaultSimulationReport getInstance() {
        return INSTANCE;
    }


    public FaultSimulationReport setEndpoint(String endpoint) {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint cannot be null or empty");
        }
        this.endpoint = endpoint;
        report.putIfAbsent(endpoint, new ConcurrentHashMap<>());
        return this;
    }

    public FaultSimulationReport setField(String field) {
        if (field == null || field.trim().isEmpty()) {
            throw new IllegalArgumentException("Field cannot be null or empty");
        }
        if (endpoint == null) {
            throw new IllegalStateException("Endpoint must be set before setting field");
        }
        this.field = field;
        Map<String, Object> endpointMap = getOrCreateMap(report, endpoint);
        if (endpointMap != null) {
            endpointMap.putIfAbsent(field, new ConcurrentHashMap<>());
        }
        return this;
    }

    public FaultSimulationReport setFaultType(String simulatedFault) {
        if (simulatedFault == null || simulatedFault.trim().isEmpty()) {
            throw new IllegalArgumentException("Simulated fault cannot be null or empty");
        }
        if (endpoint == null || field == null) {
            throw new IllegalStateException("Endpoint and field must be set before setting fault type");
        }
        this.simulatedFault = simulatedFault;

        Map<String, Object> endpointMap = getOrCreateMap(report, endpoint);
        if (endpointMap == null) return this;

        Map<String, Object> fieldMap = getOrCreateMap(endpointMap, field);
        if (fieldMap == null) return this;

        fieldMap.putIfAbsent(simulatedFault, Collections.synchronizedList(new ArrayList<>()));
        return this;
    }

    public FaultSimulationReport setTestResult(TestLevelSimulationResults testResult) {
        if (testResult == null) {
            throw new IllegalArgumentException("Test result cannot be null");
        }
        this.testResult = testResult;
        return this;
    }

    public void apply() {
        if (endpoint == null || field == null || simulatedFault == null || testResult == null) {
            throw new IllegalStateException("All fields (endpoint, field, simulatedFault, testResult) must be set before applying");
        }

        Map<String, Object> endpointMap = getOrCreateMap(report, endpoint);
        if (endpointMap == null) return;

        Map<String, Object> fieldMap = getOrCreateMap(endpointMap, field);
        if (fieldMap == null) return;

        List<Object> faultList = getOrCreateList(fieldMap, simulatedFault);
        if (faultList != null) {
            synchronized (faultList) {
                faultList.add(testResult);
            }
            saveReport();
        }
        resetState();
    }

    private void saveReport() {
        try {
            File reportFile = new File(DEFAULT_REPORT_PATH);
            if (reportFile.exists()) reportFile.delete();
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(DEFAULT_REPORT_PATH), report);
        } catch (IOException e) {
            System.err.println("Failed to save report: " + e.getMessage());
        }
    }

    public String toJson() throws IOException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrCreateMap(Map<String, Object> parentMap, String key) {
        if (parentMap == null || key == null) {
            return null;
        }
        try {
            return (Map<String, Object>) parentMap.computeIfAbsent(key,
                    k -> new ConcurrentHashMap<>());
        } catch (Exception e) {
            System.err.println("Error creating map for key " + key + ": " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> getOrCreateList(Map<String, Object> parentMap, String key) {
        if (parentMap == null || key == null) {
            return null;
        }
        try {
            return (List<Object>) parentMap.computeIfAbsent(key,
                    k -> Collections.synchronizedList(new ArrayList<>()));
        } catch (Exception e) {
            System.err.println("Error creating list for key " + key + ": " + e.getMessage());
            return null;
        }
    }

    private void resetState() {
        this.endpoint = null;
        this.field = null;
        this.simulatedFault = null;
        this.testResult = null;
    }
}