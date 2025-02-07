package metatester.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimulatorConfig {
    public Faults faults;
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


    public static SimulatorConfig getConfig(){
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

    public static List<String> getFaults(){
        List<String> faults = new ArrayList<>();

        if (getConfig().faults.null_field.enabled) faults.add("null_field");
        if (getConfig().faults.missing_field.enabled) faults.add("missing_field");
        if (getConfig().faults.invalid_data_type.enabled) faults.add("invalid_data_type");
        if (getConfig().faults.http_method_change.enabled) faults.add("http_method_change");

        return faults;
    }
    public static boolean isEndpointExcluded(String endpoint){

        return false;

    }

    public static boolean isTestExcluded(String testName){

        return false;

    }

}
