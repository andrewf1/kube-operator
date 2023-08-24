package af.ex.kube.kubeoperator;

import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

@ControllerConfiguration
public class ExecutionPlanReconciler implements Reconciler<ExecutionPlan> {

    @Override
    public UpdateControl<ExecutionPlan> reconcile(ExecutionPlan resource,
                                                  Context<ExecutionPlan> context) throws Exception {
        return UpdateControl.patchStatus(resource);
    }
}
