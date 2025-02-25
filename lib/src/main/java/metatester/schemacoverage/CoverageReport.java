package metatester.schemacoverage;

import java.util.HashMap;
import java.util.Map;

public class CoverageReport {
    private String dateGenerated;
    private String host;
    private Map<String, CoverageEntry> called = new HashMap<>();

    public String getDateGenerated() {
        return dateGenerated;
    }

    public void setDateGenerated(String dateGenerated) {
        this.dateGenerated = dateGenerated;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Map<String, CoverageEntry> getCalled() {
        return called;
    }

    public void setCalled(Map<String, CoverageEntry> called) {
        this.called = called;
    }
}
