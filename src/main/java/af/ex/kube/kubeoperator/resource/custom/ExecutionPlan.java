package af.ex.kube.kubeoperator.resource.custom;

import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan.ExecutionPlanSpec;
import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan.ExecutionPlanStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Group("ex.operator")
@Version("v1")
public class ExecutionPlan extends CustomResource<ExecutionPlanSpec, ExecutionPlanStatus> implements Namespaced {

    @Getter
    @Setter
    public static class ExecutionPlanSpec {
        private List<Plan> plans;

        @Getter
        @Setter
        public static class Plan {
            private String planName;
            private List<String> deploymentNames;
            private List<String> resources;
        }
    }

    @Getter
    @Setter
    public static class ExecutionPlanStatus extends ObservedGenerationAwareStatus {
        private Boolean error = false;
        private String reason = "No error: Creation.";
        private Map<String, Integer> deploymentReplicaCounts = new HashMap<>();
    }
}
