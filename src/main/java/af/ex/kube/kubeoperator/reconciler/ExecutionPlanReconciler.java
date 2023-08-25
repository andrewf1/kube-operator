package af.ex.kube.kubeoperator.reconciler;

import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.PrimaryToSecondaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

import java.util.Map;
import java.util.Set;

@ControllerConfiguration
public class ExecutionPlanReconciler implements Reconciler<ExecutionPlan>, Cleaner<ExecutionPlan>,
        EventSourceInitializer<ExecutionPlan>, ErrorStatusHandler<ExecutionPlan> {

    public static final String SELECTOR = "";

    @Override
    public UpdateControl<ExecutionPlan> reconcile(ExecutionPlan resource,
                                                  Context<ExecutionPlan> context) throws Exception {
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
                .withPrimaryToSecondaryMapper((PrimaryToSecondaryMapper<? extends HasMetadata>) primary ->
                    Set.of()
                )
                .build(), context);
        return EventSourceInitializer.nameEventSources(deploymentEventSource);
    }


    @Override
    public ErrorStatusUpdateControl<ExecutionPlan> updateErrorStatus(ExecutionPlan resource,
                                                                     Context<ExecutionPlan> context,
                                                                     Exception e) {
        return ErrorStatusUpdateControl.updateStatus(resource);
    }
}
