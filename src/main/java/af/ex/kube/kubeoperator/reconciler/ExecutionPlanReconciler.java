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
                .flatMap(deployment -> {
                    deployment.addOwnerReference(resource);
                    log.debug("Adding owner reference {} to dependent resource {}",
                            resource.getFullResourceName(),
                            deployment.getFullResourceName());
                    return Stream.of(deployment);
                })
                .forEach(deployment -> scaleDeployment(deployment, context.getClient()));

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

    private void scaleDeployment(Deployment deployment, KubernetesClient client) {
        log.info("Attempting to scale deployment {}", deployment);
        client.apps()
                .deployments()
                .inNamespace(deployment.getMetadata().getNamespace())
                .withName(deployment.getFullResourceName())
                .patch(new DeploymentBuilder(deployment)
                        .editSpec()
                        .withReplicas(deployment.getSpec().getReplicas() >= 1 ?
                                deployment.getSpec().getReplicas() :
                                1)
                        .endSpec()
                        .build());

        currentDeploymentReplicas.put(deployment.getFullResourceName(),
                client.apps()
                        .deployments()
                        .inNamespace(deployment.getFullResourceName())
                        .withName(deployment.getFullResourceName())
                        .get()
                        .getSpec()
                        .getReplicas());
        log.debug("{} deployment -> {} replicas",
                deployment.getFullResourceName(),
                currentDeploymentReplicas.get(deployment.getFullResourceName()));
    }

    @Override
    public DeleteControl cleanup(ExecutionPlan resource, Context<ExecutionPlan> context) {
        log.info("Deleting resource {}", resource);
        return DeleteControl.defaultDelete();
    }

    protected static Deployment getDeploymentByName(String name,
                                                    KubernetesClient client,
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
