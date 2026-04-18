package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityScanningGate")
class SecurityScanningGateTest {

    private final SecurityScanningGate gate =
            new SecurityScanningGate();

    @Test
    @DisplayName("SAST enabled includes x-security-sast")
    void evaluate_sast_includesSast() {
        ProjectConfig config = TestConfigBuilder.builder()
                .scanningSast(true)
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-security-sast");
    }

    @Test
    @DisplayName("DAST enabled includes x-security-dast")
    void evaluate_dast_includesDast() {
        ProjectConfig config = TestConfigBuilder.builder()
                .scanningDast(true)
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-security-dast");
    }

    @Test
    @DisplayName("secretScan enabled includes"
            + " x-security-secrets")
    void evaluate_secretScan_includesSecrets() {
        ProjectConfig config = TestConfigBuilder.builder()
                .scanningSecretScan(true)
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-security-secrets");
    }

    @Test
    @DisplayName("containerScan enabled includes"
            + " x-security-container")
    void evaluate_containerScan_includesContainer() {
        ProjectConfig config = TestConfigBuilder.builder()
                .containerScan(true)
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-security-container");
    }

    @Test
    @DisplayName("infraScan enabled includes x-security-infra")
    void evaluate_infraScan_includesInfra() {
        ProjectConfig config = TestConfigBuilder.builder()
                .infraScan(true)
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-security-infra");
    }

    @Test
    @DisplayName("quality gate provider not 'none' includes"
            + " x-security-sonar")
    void evaluate_qgProvider_includesSonar() {
        ProjectConfig config = TestConfigBuilder.builder()
                .qualityGateProvider("sonarqube")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).contains("x-security-sonar");
    }

    @Test
    @DisplayName("all flags off returns empty")
    void evaluate_allOff_returnsEmpty() {
        ProjectConfig config = TestConfigBuilder.builder()
                .scanningFlags(
                        false, false, false, false, false)
                .qualityGateProvider("none")
                .build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).isEmpty();
    }
}
