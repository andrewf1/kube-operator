package af.ex.kube.kubeoperator;

import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan;
import io.javaoperatorsdk.operator.api.reconciler.*;

@ControllerConfiguration
public class ExecutionPlanReconciler implements Reconciler<ExecutionPlan>, Cleaner<ExecutionPlan> {

    @Override
    public UpdateControl<ExecutionPlan> reconcile(ExecutionPlan resource,
                                                  Context<ExecutionPlan> context) throws Exception {
        return UpdateControl.patchStatus(resource);
    }

    @Override
    public DeleteControl cleanup(ExecutionPlan resource, Context<ExecutionPlan> context) {
        return DeleteControl.defaultDelete();
    }
}
