package metatester.schemacoverage;

import java.util.HashMap;
import java.util.Map;

public class CollectorData {
    private String dateCollected;
    private String host;
    private Map<String, Map<String, EndpointCall>> paths;

    public String getDateCollected() {
        return dateCollected;
    }

    public void setDateCollected(String dateCollected) {
        this.dateCollected = dateCollected;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Map<String, Map<String, EndpointCall>> getPaths() {
        if (this.paths == null) {
            this.paths = new HashMap<>();
        }
        return paths;
    }

    public void setPaths(Map<String, Map<String, EndpointCall>> paths) {
        this.paths = paths;
    }
}
