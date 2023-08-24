package af.ex.kube.kubeoperator.config;

import af.ex.kube.kubeoperator.ExecutionPlanReconciler;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OperatorConfig {

    @Bean
    public ExecutionPlanReconciler planController() {
        return new ExecutionPlanReconciler();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @SuppressWarnings("rawtypes")
    public Operator operator(List<Reconciler> controllers) {
        Operator operator = new Operator();
        controllers.forEach(operator::register);
        return operator;
    }
}
