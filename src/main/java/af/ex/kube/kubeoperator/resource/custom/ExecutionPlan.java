package af.ex.kube.kubeoperator.resource.custom;

import af.ex.kube.kubeoperator.config.OperatorConfig;
import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan.*;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

@Group("ex.operator")
@Version("v1")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExecutionPlan extends CustomResource<ExecutionPlanSpec, ExecutionPlanStatus> implements Namespaced {

    @Data
    @Builder
    public static class ExecutionPlanSpec {
        private PriorityQueue<Plan> plans;

        @Data
        @Builder
        public static class Plan {
            private String planName;
            private List<String> deploymentNames;
            private List<String> resources;
        }
    }

    @Data
    @Builder
    @EqualsAndHashCode(callSuper = true)
    public static class ExecutionPlanStatus extends ObservedGenerationAwareStatus {
        private Boolean error;
        private String reason;
        private Map<String, Integer> deploymentReplicaCounts;
    }
}
