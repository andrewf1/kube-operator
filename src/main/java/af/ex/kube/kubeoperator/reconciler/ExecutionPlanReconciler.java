package af.ex.kube.kubeoperator.reconciler;

import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan;
import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan.ExecutionPlanSpec.Plan;
import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan.ExecutionPlanStatus;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@ControllerConfiguration
@Slf4j
public class ExecutionPlanReconciler implements Reconciler<ExecutionPlan>, Cleaner<ExecutionPlan>,
        ErrorStatusHandler<ExecutionPlan> {

    public static final String SELECTOR = "app.kubernetes.io/managed-by=execution-plan-operator";

    private final Map<String, Integer> currentDeploymentReplicas = new HashMap<>();

    @Override
    public UpdateControl<ExecutionPlan> reconcile(ExecutionPlan resource,
                                                  Context<ExecutionPlan> context) throws Exception {
        log.info("Reconciling resource {}", resource);
        resource.setStatus(new ExecutionPlanStatus());

        resource.getSpec().getPlans().stream()
                .map(Plan::getDeploymentNames)
                .flatMap(List::stream)
                .map(deploymentName -> getDeploymentByName(deploymentName,
                        context.getClient(),
                        resource.getMetadata().getNamespace()))
                .forEach(deployment -> scaleDeployment(Objects.requireNonNull(deployment),
                        context.getClient(),
                        deployment.getSpec().getReplicas() >= 1 ?
                                deployment.getSpec().getReplicas() :
                                1));

        currentDeploymentReplicas.forEach((key, value) -> {
            if (value < 1) {
                resource.getStatus().setError(true);
                resource.getStatus().setReason(resource.getStatus()
                        .getReason()
                        .concat(String.format("; %s deployment has replica count of %d",
                                key,
                                value)));
            }
        });

        if (!resource.getStatus().getError()) {
            resource.getStatus().setReason("All Plan deployments have replica count >= 1");
        }

        resource.getStatus().setDeploymentReplicaCounts(currentDeploymentReplicas);
        return UpdateControl.patchStatus(resource);
    }

    private void scaleDeployment(@NonNull Deployment deployment,
                                 @NonNull KubernetesClient client,
                                 int count) {
        log.info("Attempting to scale deployment {}", deployment.getMetadata().getName());
        client.apps()
                .deployments()
                .inNamespace(deployment.getMetadata().getNamespace())
                .withName(deployment.getMetadata().getName())
                .patch(new DeploymentBuilder(deployment)
                        .editSpec()
                        .withReplicas(count)
                        .endSpec()
                        .build());

        currentDeploymentReplicas.put(deployment.getMetadata().getName(),
                client.apps()
                        .deployments()
                        .inNamespace(deployment.getMetadata().getNamespace())
                        .withName(deployment.getMetadata().getName())
                        .get()
                        .getSpec()
                        .getReplicas());
        log.debug("{} deployment -> {} replicas",
                deployment.getMetadata().getName(),
                currentDeploymentReplicas.get(deployment.getMetadata().getName()));
    }

    @Override
    public DeleteControl cleanup(ExecutionPlan resource, Context<ExecutionPlan> context) {
        log.info("Deleting resource {}", resource.getMetadata().getName());

        resource.getSpec().getPlans().stream()
                .map(Plan::getDeploymentNames)
                .flatMap(List::stream)
                .map(deploymentName -> getDeploymentByName(deploymentName,
                        context.getClient(),
                        resource.getMetadata().getNamespace()))
                .forEach(deployment -> scaleDeployment(deployment,
                        context.getClient(),
                        0));

        return DeleteControl.defaultDelete();
    }

    protected static Deployment getDeploymentByName(String name,
                                                    @NonNull KubernetesClient client,
                                                    String namespace) {
        log.debug("Attempting to retrieve deployment {}:{}", namespace, name);
        return Objects.requireNonNull(client.apps()
                .deployments()
                .inNamespace(namespace)
                .withName(name)
                .get());
    }

    @Override
    public ErrorStatusUpdateControl<ExecutionPlan> updateErrorStatus(ExecutionPlan resource,
                                                                     Context<ExecutionPlan> context,
                                                                     Exception e) {
        return ErrorStatusUpdateControl.updateStatus(resource);
    }
}
