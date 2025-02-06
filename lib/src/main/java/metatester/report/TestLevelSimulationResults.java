package metatester.report;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestLevelSimulationResults {

    String test;
    boolean caught;
    String error;
}
