package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TestingGate")
class TestingGateTest {

    private final TestingGate gate = new TestingGate();

    @Test
    @DisplayName("always includes x-test-e2e")
    void evaluate_always_includesE2e() {
        ProjectConfig config = TestConfigBuilder.builder()
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-test-e2e");
    }

    @Test
    @DisplayName("smoke+REST includes x-test-smoke-api")
    void evaluate_smokeRest_includesSmokeApi() {
        ProjectConfig config = TestConfigBuilder.builder()
                .smokeTests(true)
                .clearInterfaces()
                .addInterface("rest")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-test-smoke-api");
    }

    @Test
    @DisplayName("smoke without matching interface excludes"
            + " smoke skills")
    void evaluate_smokeNoIface_excludesSmokeSkills() {
        ProjectConfig config = TestConfigBuilder.builder()
                .smokeTests(true)
                .clearInterfaces()
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills)
                .doesNotContain("x-test-smoke-api")
                .doesNotContain("x-test-smoke-socket");
    }

    @Test
    @DisplayName("performanceTests includes x-test-perf")
    void evaluate_performanceTests_includesPerf() {
        ProjectConfig config = TestConfigBuilder.builder()
                .performanceTests(true)
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-test-perf");
    }

    @Test
    @DisplayName("contractTests includes x-test-contract")
    void evaluate_contractTests_includesContract() {
        ProjectConfig config = TestConfigBuilder.builder()
                .contractTests(true)
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-test-contract");
    }

    @Test
    @DisplayName("no testing flags returns only e2e")
    void evaluate_noFlags_onlyE2e() {
        ProjectConfig config = TestConfigBuilder.builder()
                .smokeTests(false)
                .performanceTests(false)
                .contractTests(false)
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills)
                .containsExactly("x-test-e2e");
    }
}
