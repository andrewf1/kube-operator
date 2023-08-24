package af.ex.kube.kubeoperator.resource.custom;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class ExecutionPlanStatus extends ObservedGenerationAwareStatus {
    private Boolean error;
    private String reason;
}
