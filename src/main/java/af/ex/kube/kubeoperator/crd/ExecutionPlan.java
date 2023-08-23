package af.ex.kube.kubeoperator.crd;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Group("af.executionplan")
@Version("v1")
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
public class ExecutionPlan extends CustomResource<ExecutionPlanSpec, ExecutionPlanStatus> {

}
