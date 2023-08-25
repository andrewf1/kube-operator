package af.ex.kube.kubeoperator.config;

import af.ex.kube.kubeoperator.reconciler.ExecutionPlanDependentWorkflowReconciler;
import af.ex.kube.kubeoperator.reconciler.ExecutionPlanReconciler;
import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan;
import af.ex.kube.kubeoperator.resource.dependent.DeploymentDependentResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResourceConfig;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Workflow;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OperatorConfig {

    @Bean
    public ExecutionPlanReconciler reconciler() {
        return new ExecutionPlanReconciler();
    }

    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ExecutionPlanDependentWorkflowReconciler dependentWorkflowReconciler(KubernetesClient client) {
        DeploymentDependentResource dependentResource = new DeploymentDependentResource();
        dependentResource.setKubernetesClient(client);
        dependentResource.configureWith(new KubernetesDependentResourceConfig()
                .setLabelSelector(ExecutionPlanDependentWorkflowReconciler.DEPENDENT_RESOURCE_LABEL_SELECTOR));
        return new ExecutionPlanDependentWorkflowReconciler(new WorkflowBuilder<ExecutionPlan>()
                        .addDependentResource(dependentResource)
                        .build(),
                dependentResource);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @SuppressWarnings("rawtypes")
    public Operator operator(List<Reconciler> controllers) {
        Operator operator = new Operator();
        controllers.forEach(operator::register);
        return operator;
    }
}
