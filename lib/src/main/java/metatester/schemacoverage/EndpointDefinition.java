package metatester.schemacoverage;

import io.swagger.v3.oas.models.Operation;

public class EndpointDefinition {
    private Operation operation;

    public EndpointDefinition(Operation operation) {
        this.operation = operation;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }
}
