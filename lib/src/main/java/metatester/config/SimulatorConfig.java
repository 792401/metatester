package metatester.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

class SimulatorConfig {
    public Faults faults;
    public Endpoints endpoints;
    public Tests tests;
    public Report report;

    public static class Faults {
        public FaultDetail null_field;
        public FaultDetail missing_field;
        public FaultDetail invalid_data_type;
        public FaultDetail invalid_value;
        public FaultDetail http_method_change;
        public FaultDetail status_code_change;
        public DelayInjection delay_injection;
    }

    public static class FaultDetail {
        public boolean enabled;
    }

    public static class DelayInjection {
        public boolean enabled;
        public int delay_ms;
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

    public static SimulatorConfig loadConfig(String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(new File(filePath), SimulatorConfig.class);
    }

    public static void main(String[] args) {
        try {
            SimulatorConfig config = SimulatorConfig.loadConfig("./config.yml");
            System.out.println("Config Loaded Successfully:");
            System.out.println("Report Format: " + config.report.format);
            System.out.println("Excluded Endpoints: " + config.endpoints.exclude);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
