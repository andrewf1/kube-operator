package af.ex.kube.kubeoperator.resource.dependent;

import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import static af.ex.kube.kubeoperator.ExecutionPlanReconciler.SELECTOR;

@KubernetesDependent(labelSelector = SELECTOR)
public class DeploymentDependentResource extends CRUDKubernetesDependentResource<Deployment, ExecutionPlan> {
    public DeploymentDependentResource() {
        super(Deployment.class);
    }

    @Override
    public Deployment desired(ExecutionPlan executionPlan, Context<ExecutionPlan> context) {
        // TODO: Shouldn't create a new deployment. Get one(s) that should exist and update replica set to launch 1
        return new Deployment();
    }
}
