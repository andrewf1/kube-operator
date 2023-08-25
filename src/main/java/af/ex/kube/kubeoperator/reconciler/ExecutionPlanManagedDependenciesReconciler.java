package af.ex.kube.kubeoperator.reconciler;

import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan;
import af.ex.kube.kubeoperator.resource.dependent.DeploymentDependentResource;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

@ControllerConfiguration(
        dependents = {
                @Dependent(type = DeploymentDependentResource.class)
        })
public class ExecutionPlanManagedDependenciesReconciler implements Reconciler<ExecutionPlan>,
        ErrorStatusHandler<ExecutionPlan>, Cleaner<ExecutionPlan> {

    public static final String SELECTOR = "managed";

    @Override
    public ErrorStatusUpdateControl<ExecutionPlan> updateErrorStatus(ExecutionPlan resource,
                                                                     Context<ExecutionPlan> context,
                                                                     Exception e) {
        resource.getStatus().setError(true);
        resource.getStatus().setReason(e.getMessage());
        return ErrorStatusUpdateControl.patchStatus(resource);
    }

    @Override
    public UpdateControl<ExecutionPlan> reconcile(ExecutionPlan resource,
                                                  Context<ExecutionPlan> context) throws Exception {
        return UpdateControl.patchStatus(resource);
    }

    @Override
    public DeleteControl cleanup(ExecutionPlan resource,
                                 Context<ExecutionPlan> context) {
        return DeleteControl.defaultDelete();
    }
}
