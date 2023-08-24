package af.ex.kube.kubeoperator.resource.custom;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Group("af.executionplan")
@Version("v1")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExecutionPlan extends CustomResource<ExecutionPlanSpec, ExecutionPlanStatus> implements Namespaced {
}
