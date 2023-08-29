package af.ex.kube.kubeoperator.probes;

import io.javaoperatorsdk.operator.Operator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProbesHandler {

    private final Operator operator;

    @GetMapping("/startup")
    public ResponseEntity<String> startupHandler() {
        if (operator.getRuntimeInfo().isStarted()) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body("Operator has started!");
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Operator has not started...");
    }

    @GetMapping("/healthz")
    public ResponseEntity<String> livenessHandler() {
        if (operator.getRuntimeInfo().allEventSourcesAreHealthy()) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body("Operator is healthy!");
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Operator is not healthy");
    }
}