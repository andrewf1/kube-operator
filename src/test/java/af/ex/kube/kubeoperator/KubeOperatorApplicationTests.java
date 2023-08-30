package af.ex.kube.kubeoperator;

import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan;
import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan.ExecutionPlanSpec;
import af.ex.kube.kubeoperator.resource.custom.ExecutionPlan.ExecutionPlanSpec.Plan;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.junit.AbstractOperatorExtension;
import io.javaoperatorsdk.operator.junit.ClusterDeployedOperatorExtension;
import io.javaoperatorsdk.operator.springboot.starter.test.EnableMockOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@EnableMockOperator
class KubeOperatorApplicationTests {

	static KubernetesClient client = new KubernetesClientBuilder()
			.build();

	@RegisterExtension
	static AbstractOperatorExtension operator;

	static {
		try {
			operator = ClusterDeployedOperatorExtension.builder()
					.waitForNamespaceDeletion(true)
					.withOperatorDeployment(
							client.load(new FileInputStream("k8s/operator.yml")).items())
					.build();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

    ExecutionPlan executionPlan() {
		ExecutionPlan executionPlan = new ExecutionPlan();
		executionPlan.setMetadata(new ObjectMetaBuilder()
				.withName("test-execution-plan")
				.withNamespace(operator.getNamespace())
				.build());
		executionPlan.setSpec(new ExecutionPlanSpec());
		Plan plan = new Plan();
		plan.setPlanName("test-plan");
		plan.setDeploymentNames(List.of("nginx-deployment"));
		plan.setResources(List.of("R1"));
		executionPlan.getSpec().setPlans(List.of(plan));
		return executionPlan;
	}

	@Test
	void test() {
		var executionPlan = executionPlan();
		var executionPlanClient = client.resources(ExecutionPlan.class);
		executionPlanClient.inNamespace(operator.getNamespace())
				.resource(executionPlan)
				.create();

		await().atMost(1, MINUTES).untilAsserted(() -> {
			ExecutionPlan updatedEP = executionPlanClient.inNamespace(operator.getNamespace())
					.withName(executionPlan.getMetadata().getName())
					.get();
			assertNotNull(updatedEP.getStatus());
			assertEquals(updatedEP.getStatus().getDeploymentReplicaCounts().entrySet().size(), 1);
		});
	}

}
