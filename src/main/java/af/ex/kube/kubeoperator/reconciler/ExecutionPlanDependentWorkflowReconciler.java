package af.ex.kube.kubeoperator.reconciler;

import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan;
import af.ex.kube.kubeoperator.resource.custom.ExecutionPlanStatus;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Workflow;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import lombok.AllArgsConstructor;

import java.util.Map;

@ControllerConfiguration(labelSelector = ExecutionPlanDependentWorkflowReconciler.DEPENDENT_RESOURCE_LABEL_SELECTOR)
@AllArgsConstructor
public class ExecutionPlanDependentWorkflowReconciler implements Reconciler<ExecutionPlan>,
        ErrorStatusHandler<ExecutionPlan>, EventSourceInitializer<ExecutionPlan> {

    public static final String DEPENDENT_RESOURCE_LABEL_SELECTOR = "!low-level";
    private final Workflow<ExecutionPlan> workflow;
    private KubernetesDependentResource<Deployment, ExecutionPlan> dependentResource;

    @Override
    public UpdateControl<ExecutionPlan> reconcile(ExecutionPlan resource,
                                                  Context<ExecutionPlan> context) throws Exception {
        workflow.reconcile(resource, context);
        resource.setStatus(ExecutionPlanStatus.builder()
                .error(false)
                .reason(context.getSecondaryResource(Deployment.class)
                        .orElseThrow()
                        .getMetadata()
                        .getName() + " is created.")
                .build());
        return UpdateControl.patchStatus(resource);
    }

    @Override
    public ErrorStatusUpdateControl<ExecutionPlan> updateErrorStatus(ExecutionPlan resource,
                                                                     Context<ExecutionPlan> context, Exception e) {
        resource.getStatus().setError(true);
        resource.getStatus().setReason(e.getMessage());
        return ErrorStatusUpdateControl.updateStatus(resource);
    }

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<ExecutionPlan> context) {
        return EventSourceInitializer.nameEventSourcesFromDependentResource(context, dependentResource);
    }
}
