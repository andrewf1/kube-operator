package af.ex.kube.kubeoperator.reconciler;

import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan;
import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan.ExecutionPlanSpec.Plan;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.PrimaryToSecondaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ControllerConfiguration
@Slf4j
public class ExecutionPlanReconciler implements Reconciler<ExecutionPlan>, Cleaner<ExecutionPlan>,
        EventSourceInitializer<ExecutionPlan>, ErrorStatusHandler<ExecutionPlan> {

    public static final String SELECTOR = "execution-plan-managed";

    @Override
    public UpdateControl<ExecutionPlan> reconcile(ExecutionPlan resource,
                                                  Context<ExecutionPlan> context) throws Exception {
        log.info("Reconciling resource {}", resource);
        Map<String, Integer> deploymentReplicas = new HashMap<>();
        context.getSecondaryResources(Deployment.class)
                .forEach(deployment -> {
                    log.info("Attempting to scale deployment {}", deployment);
                    context.getClient()
                            .apps()
                            .deployments()
                            .withName(deployment.getFullResourceName())
                            .patch(new DeploymentBuilder(deployment)
                                    .editSpec()
                                    .withReplicas(1)
                                    .endSpec()
                                    .build());
                    deploymentReplicas.put(deployment.getFullResourceName(),
                            context.getClient()
                                    .apps()
                                    .deployments()
                                    .withName(deployment.getFullResourceName())
                                    .get()
                                    .getSpec()
                                    .getReplicas());
                });

        deploymentReplicas.forEach((key, value) -> {
            if (value < 1) {
                resource.getStatus().setError(true);
                resource.getStatus().setReason(resource.getStatus()
                        .getReason()
                        .concat(String.format("; %s deployment has replica count of %d",
                                key,
                                value)));
            }
        });

        resource.getStatus().setDeploymentReplicaCounts(deploymentReplicas);
        return UpdateControl.patchStatus(resource);
    }

    @Override
    public DeleteControl cleanup(ExecutionPlan resource, Context<ExecutionPlan> context) {
        log.info("Deleting resource {}", resource);
        return DeleteControl.defaultDelete();
    }

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<ExecutionPlan> context) {
        log.info("Preparing event sources...");
        var deploymentEventSource = new InformerEventSource<>(InformerConfiguration.from(Deployment.class, context)
                .withLabelSelector(SELECTOR)
                .withPrimaryToSecondaryMapper((PrimaryToSecondaryMapper<ExecutionPlan>) primary ->
                        primary.getSpec().getPlans().stream()
                                .map(Plan::getDeploymentNames)
                                .flatMap(List::stream)
                                .map(deploymentName -> getDeploymentByName(deploymentName, context.getClient()))
                                .flatMap(deployment -> {
                                    deployment.addOwnerReference(primary);
                                    log.debug("Adding owner reference {} to secondary rsc {}",
                                            primary,
                                            deployment);
                                    return Stream.of(deployment);
                                })
                                .map(ResourceID::fromResource)
                                .collect(Collectors.toSet()))
                .build(), context);
        return EventSourceInitializer.nameEventSources(deploymentEventSource);
    }

    protected static Deployment getDeploymentByName(String name, KubernetesClient client) {
        return client.apps()
                .deployments()
                .withName(name)
                .get();
    }

    @Override
    public ErrorStatusUpdateControl<ExecutionPlan> updateErrorStatus(ExecutionPlan resource,
                                                                     Context<ExecutionPlan> context,
                                                                     Exception e) {
        return ErrorStatusUpdateControl.updateStatus(resource);
    }
}
