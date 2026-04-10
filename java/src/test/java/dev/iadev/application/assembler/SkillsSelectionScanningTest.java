package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SkillsSelection — security scanning skill
 * selection based on ScanningConfig, QualityGateConfig,
 * and pentest flags.
 */
@DisplayName("SkillsSelection — scanning")
class SkillsSelectionScanningTest {

    @Nested
    @DisplayName("selectSecurityScanningSkills")
    class SelectSecurityScanningSkills {

        @Test
        @DisplayName("all flags false returns empty list")
        void select_allFlagsFalse_returnsEmpty() {
            ProjectConfig config =
                    TestConfigBuilder.builder().build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("sast enabled returns x-security-sast")
        void select_sastEnabled_returnsSastScan() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(
                                    true, false,
                                    false, false, false)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills)
                    .containsExactly("x-security-sast");
        }

        @Test
        @DisplayName("dast enabled returns x-security-dast")
        void select_dastEnabled_returnsDastScan() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(
                                    false, true,
                                    false, false, false)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills)
                    .containsExactly("x-security-dast");
        }

        @Test
        @DisplayName("secretScan enabled returns"
                + " x-security-secret-scan")
        void select_secretScanEnabled_returnsSecretScan() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(
                                    false, false,
                                    true, false, false)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills)
                    .containsExactly("x-security-secret-scan");
        }

        @Test
        @DisplayName("containerScan enabled returns"
                + " x-security-container")
        void select_containerScanEnabled_returnsContScan() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(
                                    false, false,
                                    false, true, false)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills)
                    .containsExactly("x-security-container");
        }

        @Test
        @DisplayName("infraScan enabled returns"
                + " x-security-infra")
        void select_infraScanEnabled_returnsInfraScan() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(
                                    false, false,
                                    false, false, true)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills)
                    .containsExactly("x-security-infra");
        }

        @Test
        @DisplayName("pentest enabled does not add"
                + " x-security-pentest to scanning skills"
                + " (delegated to selectPentestSkills)")
        void select_pentestEnabled_excludesPentest() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .pentest(true)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills)
                    .doesNotContain("x-security-pentest");
        }

        @Test
        @DisplayName("qualityGate sonarqube returns"
                + " x-security-sonar")
        void select_sonarqubeProvider_returnsSonarGate() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .qualityGateProvider("sonarqube")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills)
                    .containsExactly("x-security-sonar");
        }

        @Test
        @DisplayName("qualityGate sonarcloud returns"
                + " x-security-sonar")
        void select_sonarcloudProvider_returnsSonarGate() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .qualityGateProvider("sonarcloud")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills)
                    .containsExactly("x-security-sonar");
        }

        @Test
        @DisplayName("qualityGate none returns empty")
        void select_noneProvider_excludesSonarGate() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .qualityGateProvider("none")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("all flags enabled returns all skills")
        void select_allEnabled_returnsAllSkills() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(
                                    true, true,
                                    true, true, true)
                            .pentest(true)
                            .qualityGateProvider("sonarqube")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills).containsExactlyInAnyOrder(
                    "x-security-sast",
                    "x-security-dast",
                    "x-security-secret-scan",
                    "x-security-container",
                    "x-security-infra",
                    "x-security-sonar");
        }

        @Test
        @DisplayName("mixed flags returns matching skills")
        void select_mixedFlags_returnsMatchingSkills() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(
                                    true, false,
                                    true, false, false)
                            .pentest(true)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills).containsExactlyInAnyOrder(
                    "x-security-sast",
                    "x-security-secret-scan");
        }
    }

    @Nested
    @DisplayName("selectConditionalSkills includes scanning")
    class ConditionalIncludesScanning {

        @Test
        @DisplayName("aggregation includes scanning skills"
                + " when flags enabled")
        void conditional_scanningEnabled_includesSkills() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .scanningFlags(
                                    true, false,
                                    false, false, false)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);

            assertThat(skills).contains("x-security-sast");
        }

        @Test
        @DisplayName("aggregation excludes scanning skills"
                + " when all flags false")
        void conditional_scanningDisabled_excludesSkills() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);

            assertThat(skills)
                    .doesNotContain(
                            "x-security-sast",
                            "x-security-dast",
                            "x-security-secret-scan",
                            "x-security-container",
                            "x-security-infra",
                            "x-security-pentest",
                            "x-security-sonar");
        }
    }
}
