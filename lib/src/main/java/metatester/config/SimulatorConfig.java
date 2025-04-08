package metatester.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SimulatorConfig {
    public Faults faults;

    public Url url;
    public Endpoints endpoints;
    public Tests tests;
    public Report report;


    public static class Faults {
        static class Fault {
            public boolean enabled;
        }

        static class DelayInjection extends Fault {
            public int delay_ms;
        }

        public Fault null_field;
        public Fault missing_field;
        public Fault invalid_data_type;
        public Fault invalid_value;
        public Fault http_method_change;
        public Fault status_code_change;
        public DelayInjection delay_injection;
    }

    public static class Url {
        public List<String> exclude;
    }
    public static class Endpoints {
        public List<String> exclude;
    }

    public static class Tests {
        public List<String> exclude;
    }

    public static class Report {
        public String format;
        public String output_path;
    }


    private static SimulatorConfig getConfig(){
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream inputStream = SimulatorConfig.class.getClassLoader().getResourceAsStream("config.yml")) {
            if (inputStream == null) {
                throw new IOException("Configuration file not found in resources: config.yml");
            }
            return mapper.readValue(inputStream, SimulatorConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<FaultCollection> getEnabledFaults(){
        List<FaultCollection> enabledFaults = new ArrayList<>();
        Faults faults = getConfig().faults;

        if (faults.null_field.enabled) enabledFaults.add(FaultCollection.null_field);
        if (faults.missing_field.enabled) enabledFaults.add(FaultCollection.missing_field);
        if (faults.invalid_data_type.enabled) enabledFaults.add(FaultCollection.invalid_value);
        if (faults.http_method_change.enabled) enabledFaults.add(FaultCollection.http_method_change);

        return enabledFaults;
    }

    public static boolean isEndpointExcluded(String endpoint){
        return false;

    }

    public static boolean isTestExcluded(String testName){

        return false;

    }

}
