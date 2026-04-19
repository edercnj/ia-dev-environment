package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InfraGate")
class InfraGateTest {

    private final InfraGate gate = new InfraGate();

    @Test
    @DisplayName("orchestrator configured includes"
            + " setup-environment")
    void evaluate_orchestrator_includesSetupEnvironment() {
        ProjectConfig config = TestConfigBuilder.builder()
                .orchestrator("kubernetes")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("setup-environment");
    }

    @Test
    @DisplayName("apiGateway configured includes"
            + " x-review-gateway")
    void evaluate_apiGateway_includesReviewGateway() {
        ProjectConfig config = TestConfigBuilder.builder()
                .apiGateway("kong")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-review-gateway");
    }

    @Test
    @DisplayName("observability tool configured includes"
            + " x-obs-instrument")
    void evaluate_observability_includesObsInstrument() {
        ProjectConfig config = TestConfigBuilder.builder()
                .observabilityTool("otel")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-obs-instrument");
    }

    @Test
    @DisplayName("all infra features 'none' returns empty")
    void evaluate_allNone_returnsEmpty() {
        ProjectConfig config = TestConfigBuilder.builder()
                .orchestrator("none")
                .apiGateway("none")
                .observabilityTool("none")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).isEmpty();
    }
}
