package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityGate")
class SecurityGateTest {

    private final SecurityGate gate = new SecurityGate();

    @Test
    @DisplayName("security frameworks present includes"
            + " x-review-security")
    void evaluate_frameworks_includesReviewSecurity() {
        ProjectConfig config = TestConfigBuilder.builder()
                .securityFrameworks("spring-security")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills)
                .containsExactly("x-review-security");
    }

    @Test
    @DisplayName("no security frameworks returns empty")
    void evaluate_noFrameworks_returnsEmpty() {
        ProjectConfig config = TestConfigBuilder.builder()
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).isEmpty();
    }
}
