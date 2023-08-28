package af.ex.kube.kubeoperator.reconciler;

import af.ex.kube.kubeoperator.resource.custom.*;
import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan.ExecutionPlanSpec.*;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.*;
import io.fabric8.kubernetes.client.*;
import io.javaoperatorsdk.operator.api.config.informer.*;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.event.*;
import io.javaoperatorsdk.operator.processing.event.source.*;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.*;

import java.util.*;
import java.util.stream.*;

@ControllerConfiguration
public class ExecutionPlanReconciler implements Reconciler<ExecutionPlan>, Cleaner<ExecutionPlan>,
        EventSourceInitializer<ExecutionPlan>, ErrorStatusHandler<ExecutionPlan> {

    public static final String SELECTOR = "execution-plan-managed";

    @Override
    public UpdateControl<ExecutionPlan> reconcile(ExecutionPlan resource,
                                                  Context<ExecutionPlan> context) throws Exception {
        Map<String, Integer> deploymentReplicas = new HashMap<>();
        context.getSecondaryResources(Deployment.class)
                .forEach(deployment -> {
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
        return DeleteControl.defaultDelete();
    }

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<ExecutionPlan> context) {
        var deploymentEventSource = new InformerEventSource<>(InformerConfiguration.from(Deployment.class, context)
                .withLabelSelector(SELECTOR)
                .withPrimaryToSecondaryMapper((PrimaryToSecondaryMapper<ExecutionPlan>) primary ->
                        primary.getSpec().getPlans().stream()
                                .map(Plan::getDeploymentNames)
                                .flatMap(List::stream)
                                .map(deploymentName -> getDeploymentByName(deploymentName, context.getClient()))
                                .flatMap(deployment -> {
                                    deployment.addOwnerReference(primary);
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
