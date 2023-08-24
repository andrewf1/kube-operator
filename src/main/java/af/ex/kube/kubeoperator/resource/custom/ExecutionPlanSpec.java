package af.ex.kube.kubeoperator.resource.custom;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.PriorityQueue;

@Data
@Builder
public class ExecutionPlanSpec {
    private PriorityQueue<Plan> plans;

    @Data
    @Builder
    public static class Plan {
        private String planName;
        private String deploymentName;
        private List<String> resources;
    }
}
