package metatester.aop;

import metatester.report.FaultSimulationReport;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class GlobalTestExecutionListener implements TestExecutionListener {

    private static boolean executed = false;
    private final boolean runWithMetatester = Boolean.getBoolean(System.getProperty("metatest"));
    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (!executed && runWithMetatester) {
            executed = true;
            System.out.println("All tests completed - Generating final report...");
            FaultSimulationReport.getInstance();
        }
    }
}