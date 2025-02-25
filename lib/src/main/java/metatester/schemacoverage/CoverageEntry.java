package metatester.schemacoverage;

import java.util.HashMap;
import java.util.Map;

public class CoverageEntry {
    private Map<String, Details> called = new HashMap<>();
    private Map<String, Details> notCalled = new HashMap<>();

    public Map<String, Details> getCalled() {
        return called;
    }

    public void setCalled(Map<String, Details> called) {
        this.called = called;
    }

    public Map<String, Details> getNotCalled() {
        return notCalled;
    }

    public void setNotCalled(Map<String, Details> notCalled) {
        this.notCalled = notCalled;
    }
}
