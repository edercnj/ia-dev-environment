package dev.iadev.smoke;

import dev.iadev.application.assembler.AgentsSelection;
import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.application.assembler.SkillsSelection;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.SecurityConfig;
import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration verification and smoke test for epic-0022
 * (Security Program).
 *
 * <p>Validates the complete wiring of all security
 * components: SkillsSelection, AgentsSelection,
 * SecurityConfig.fromMap(), settings.json merge, and
 * full pipeline generation with all security flags.</p>
 *
 * <p>Tests backward compatibility by verifying that
 * profiles without security flags produce zero new
 * security artifacts.</p>
 */
@DisplayName("SecurityIntegrationVerificationTest")
class SecurityIntegrationVerificationTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("SkillsSelection wiring")
    class SkillsSelectionWiring {

        @Test
        @DisplayName("all scanning flags enabled returns"
                + " 7 scanning skills")
        void select_allFlags_returns7ScanningSkills() {
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

            assertThat(skills)
                    .containsExactlyInAnyOrder(
                            "x-sast-scan",
                            "x-dast-scan",
                            "x-secret-scan",
                            "x-container-scan",
                            "x-infra-scan",
                            "x-pentest",
                            "x-sonar-gate");
        }

        @Test
        @DisplayName("selectPentestSkills returns x-pentest"
                + " when pentest enabled")
        void select_pentestTrue_returnsPentestSkill() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .pentest(true)
                            .build();

            List<String> skills =
                    SkillsSelection.selectPentestSkills(
                            config);

            assertThat(skills)
                    .containsExactly("x-pentest");
        }

        @Test
        @DisplayName("selectConditionalSkills includes all"
                + " scanning and pentest skills")
        void select_allFlags_conditionalIncludesAll() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningFlags(
                                    true, true,
                                    true, true, true)
                            .pentest(true)
                            .qualityGateProvider("sonarqube")
                            .securityFrameworks("pci-dss")
                            .compliance("pci-dss")
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectConditionalSkills(config);

            assertThat(skills)
                    .contains(
                            "x-sast-scan",
                            "x-dast-scan",
                            "x-secret-scan",
                            "x-container-scan",
                            "x-infra-scan",
                            "x-pentest",
                            "x-sonar-gate",
                            "x-review-security",
                            "x-review-compliance");
        }

        @Test
        @DisplayName("no flags returns no scanning skills")
        void select_noFlags_returnsEmpty() {
            ProjectConfig config =
                    TestConfigBuilder.builder().build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("sast + secretScan only returns"
                + " matching skills")
        void select_partialFlags_returnsMatching() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .scanningSast(true)
                            .scanningSecretScan(true)
                            .build();

            List<String> skills =
                    SkillsSelection
                            .selectSecurityScanningSkills(
                                    config);

            assertThat(skills)
                    .containsExactlyInAnyOrder(
                            "x-sast-scan",
                            "x-secret-scan");
            assertThat(skills)
                    .doesNotContain(
                            "x-dast-scan",
                            "x-container-scan",
                            "x-infra-scan",
                            "x-pentest",
                            "x-sonar-gate");
        }
    }

    @Nested
    @DisplayName("AgentsSelection wiring")
    class AgentsSelectionWiring {

        @Test
        @DisplayName("all security flags returns all"
                + " security agents")
        void select_allFlags_returnsAllSecurityAgents() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks(
                                    "pci-dss", "lgpd")
                            .pentest(true)
                            .container("docker")
                            .orchestrator("kubernetes")
                            .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains(
                            "appsec-engineer.md",
                            "compliance-auditor.md",
                            "pentest-engineer.md",
                            "devsecops-engineer.md");
        }

        @Test
        @DisplayName("no security flags excludes all"
                + " security agents")
        void select_noFlags_excludesSecurityAgents() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .doesNotContain(
                            "appsec-engineer.md",
                            "compliance-auditor.md",
                            "pentest-engineer.md");
        }

        @Test
        @DisplayName("pentest flag only adds"
                + " pentest-engineer")
        void select_pentestOnly_addsPentestEngineer() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .pentest(true)
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains("pentest-engineer.md");
            assertThat(agents)
                    .doesNotContain(
                            "appsec-engineer.md",
                            "compliance-auditor.md");
        }

        @Test
        @DisplayName("security frameworks add appsec"
                + " and compliance auditor")
        void select_frameworks_addAppsecAndAuditor() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp")
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .build();

            List<String> agents =
                    AgentsSelection
                            .selectConditionalAgents(config);

            assertThat(agents)
                    .contains(
                            "appsec-engineer.md",
                            "compliance-auditor.md");
            assertThat(agents)
                    .doesNotContain(
                            "pentest-engineer.md");
        }
    }

    @Nested
    @DisplayName("SecurityConfig.fromMap() wiring")
    class SecurityConfigFromMapWiring {

        @Test
        @DisplayName("parses all-flags-enabled config"
                + " without error")
        void fromMap_allFlags_parsesAllFields() {
            var scanMap = Map.<String, Object>of(
                    "sast", true,
                    "dast", true,
                    "secretScan", true,
                    "containerScan", true,
                    "infraScan", true);
            var qgMap = Map.<String, Object>of(
                    "provider", "sonarqube",
                    "serverUrl",
                    "https://sonar.example.com",
                    "qualityGate", "Sonar way");
            var map = Map.<String, Object>of(
                    "compliance",
                    List.of("pci-dss", "lgpd"),
                    "scanning", scanMap,
                    "qualityGate", qgMap,
                    "pentest", true);

            SecurityConfig result =
                    SecurityConfig.fromMap(map);

            assertThat(result.frameworks())
                    .containsExactly("pci-dss", "lgpd");
            assertThat(result.scanning().sast()).isTrue();
            assertThat(result.scanning().dast()).isTrue();
            assertThat(result.scanning().secretScan())
                    .isTrue();
            assertThat(result.scanning().containerScan())
                    .isTrue();
            assertThat(result.scanning().infraScan())
                    .isTrue();
            assertThat(result.qualityGate().provider())
                    .isEqualTo("sonarqube");
            assertThat(result.pentest()).isTrue();
        }

        @Test
        @DisplayName("empty map returns safe defaults")
        void fromMap_empty_returnsSafeDefaults() {
            SecurityConfig result =
                    SecurityConfig.fromMap(Map.of());

            assertThat(result.frameworks()).isEmpty();
            assertThat(result.scanning().sast()).isFalse();
            assertThat(result.scanning().dast()).isFalse();
            assertThat(result.scanning().secretScan())
                    .isFalse();
            assertThat(result.scanning().containerScan())
                    .isFalse();
            assertThat(result.scanning().infraScan())
                    .isFalse();
            assertThat(result.qualityGate().provider())
                    .isEqualTo("none");
            assertThat(result.pentest()).isFalse();
        }

        @Test
        @DisplayName("hasAnyScanning returns true when"
                + " at least one flag is set")
        void hasAnyScanning_oneFlag_returnsTrue() {
            SecurityConfig result = new SecurityConfig(
                    List.of(),
                    new SecurityConfig.ScanningConfig(
                            true, false,
                            false, false, false),
                    SecurityConfig.QualityGateConfig
                            .defaults(),
                    false, "local");

            assertThat(result.hasAnyScanning()).isTrue();
        }

        @Test
        @DisplayName("hasAnyScanning returns false when"
                + " all flags are off")
        void hasAnyScanning_noFlags_returnsFalse() {
            SecurityConfig result =
                    SecurityConfig.fromMap(Map.of());

            assertThat(result.hasAnyScanning()).isFalse();
        }
    }

    @Nested
    @DisplayName("All-flags-enabled smoke test")
    class AllFlagsEnabledSmokeTest {

        @Test
        @DisplayName("pipeline succeeds with all security"
                + " flags enabled")
        void pipeline_allFlags_succeeds() {
            ProjectConfig config = buildAllFlagsConfig();
            PipelineResult result =
                    runPipelineFor(config);

            assertThat(result.success())
                    .as("Pipeline must succeed with all"
                            + " security flags")
                    .isTrue();
        }

        @Test
        @DisplayName("all conditional scanning skills"
                + " are present")
        void pipeline_allFlags_conditionalSkillsPresent() {
            ProjectConfig config = buildAllFlagsConfig();
            Path outputDir = runAndGetOutputDir(config);

            List<String> expectedSkills = List.of(
                    "x-sast-scan",
                    "x-dast-scan",
                    "x-secret-scan",
                    "x-container-scan",
                    "x-infra-scan",
                    "x-pentest",
                    "x-sonar-gate");

            for (String skill : expectedSkills) {
                Path skillDir = outputDir.resolve(
                        ".claude/skills/" + skill);
                assertThat(skillDir)
                        .as("Conditional skill %s must"
                                + " exist", skill)
                        .isDirectory();
                Path skillMd = skillDir.resolve("SKILL.md");
                assertThat(skillMd)
                        .as("SKILL.md for %s must exist",
                                skill)
                        .exists();
            }
        }

        @Test
        @DisplayName("core security skills are present")
        void pipeline_allFlags_coreSecSkillsPresent() {
            ProjectConfig config = buildAllFlagsConfig();
            Path outputDir = runAndGetOutputDir(config);

            List<String> coreSecSkills = List.of(
                    "x-owasp-scan",
                    "x-hardening-eval",
                    "x-runtime-protection",
                    "x-supply-chain-audit",
                    "x-security-dashboard",
                    "x-security-pipeline");

            for (String skill : coreSecSkills) {
                Path skillDir = outputDir.resolve(
                        ".claude/skills/" + skill);
                assertThat(skillDir)
                        .as("Core security skill %s must"
                                + " exist", skill)
                        .isDirectory();
            }
        }

        @Test
        @DisplayName("security agents are present")
        void pipeline_allFlags_securityAgentsPresent() {
            ProjectConfig config = buildAllFlagsConfig();
            Path outputDir = runAndGetOutputDir(config);

            List<String> expectedAgents = List.of(
                    "pentest-engineer.md",
                    "appsec-engineer.md",
                    "devsecops-engineer.md",
                    "compliance-auditor.md");

            for (String agent : expectedAgents) {
                Path agentFile = outputDir.resolve(
                        ".claude/agents/" + agent);
                assertThat(agentFile)
                        .as("Agent %s must exist", agent)
                        .exists();
                assertThat(agentFile)
                        .as("Agent %s must not be empty",
                                agent)
                        .isNotEmptyFile();
            }
        }

        @Test
        @DisplayName("security anti-patterns rule"
                + " is present")
        void pipeline_allFlags_antiPatternsRulePresent() {
            ProjectConfig config = buildAllFlagsConfig();
            Path outputDir = runAndGetOutputDir(config);

            Path rule = outputDir.resolve(
                    ".claude/rules/"
                            + "12-security-anti-patterns.md");
            assertThat(rule)
                    .as("Security anti-patterns rule must"
                            + " exist")
                    .exists();
            assertThat(rule)
                    .as("Security anti-patterns rule must"
                            + " not be empty")
                    .isNotEmptyFile();
        }

        @Test
        @DisplayName("security baseline rule exists")
        void pipeline_allFlags_securityBaselinePresent() {
            ProjectConfig config = buildAllFlagsConfig();
            Path outputDir = runAndGetOutputDir(config);

            Path rule = outputDir.resolve(
                    ".claude/rules/"
                            + "06-security-baseline.md");
            assertThat(rule)
                    .as("Security baseline rule must"
                            + " exist")
                    .exists();
            assertThat(rule)
                    .as("Security baseline rule must"
                            + " not be empty")
                    .isNotEmptyFile();
        }

        @Test
        @DisplayName("no generated file is empty")
        void pipeline_allFlags_noEmptyFiles()
                throws Exception {
            ProjectConfig config = buildAllFlagsConfig();
            Path outputDir = runAndGetOutputDir(config);

            SmokeTestValidators.assertNoEmptyFiles(
                    outputDir);
        }
    }

    @Nested
    @DisplayName("Backward compatibility")
    class BackwardCompatibility {

        @Test
        @DisplayName("no scanning flags produces zero"
                + " conditional scanning skills")
        void pipeline_noFlags_noConditionalScanningSkills() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("spring", "3.4")
                            .container("docker")
                            .orchestrator("kubernetes")
                            .build();
            Path outputDir = runAndGetOutputDir(config);

            List<String> conditionalScanSkills = List.of(
                    "x-sast-scan",
                    "x-dast-scan",
                    "x-secret-scan",
                    "x-container-scan",
                    "x-infra-scan",
                    "x-pentest",
                    "x-sonar-gate");

            for (String skill : conditionalScanSkills) {
                Path skillDir = outputDir.resolve(
                        ".claude/skills/" + skill);
                assertThat(skillDir)
                        .as("Skill %s must NOT exist"
                                + " without flags", skill)
                        .doesNotExist();
            }
        }

        @Test
        @DisplayName("no security frameworks excludes"
                + " security agents")
        void pipeline_noFrameworks_noSecurityAgents() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("spring", "3.4")
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .build();
            Path outputDir = runAndGetOutputDir(config);

            List<String> secAgents = List.of(
                    "appsec-engineer.md",
                    "compliance-auditor.md",
                    "pentest-engineer.md");

            for (String agent : secAgents) {
                Path agentFile = outputDir.resolve(
                        ".claude/agents/" + agent);
                assertThat(agentFile)
                        .as("Agent %s must NOT exist"
                                + " without flags", agent)
                        .doesNotExist();
            }
        }
    }

    @Nested
    @DisplayName("Mixed flags")
    class MixedFlags {

        @Test
        @DisplayName("sast + secretScan only generates"
                + " matching skills")
        void pipeline_partialScan_onlyMatchingSkills() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("spring", "3.4")
                            .scanningSast(true)
                            .scanningSecretScan(true)
                            .container("docker")
                            .build();
            Path outputDir = runAndGetOutputDir(config);

            assertThat(outputDir.resolve(
                    ".claude/skills/x-sast-scan"))
                    .as("x-sast-scan must exist")
                    .isDirectory();
            assertThat(outputDir.resolve(
                    ".claude/skills/x-secret-scan"))
                    .as("x-secret-scan must exist")
                    .isDirectory();

            assertThat(outputDir.resolve(
                    ".claude/skills/x-dast-scan"))
                    .as("x-dast-scan must NOT exist")
                    .doesNotExist();
            assertThat(outputDir.resolve(
                    ".claude/skills/x-container-scan"))
                    .as("x-container-scan must NOT exist")
                    .doesNotExist();
            assertThat(outputDir.resolve(
                    ".claude/skills/x-infra-scan"))
                    .as("x-infra-scan must NOT exist")
                    .doesNotExist();
        }

        @Test
        @DisplayName("pentest only adds pentest skill"
                + " and agent")
        void pipeline_pentestOnly_addsOnlyPentest() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("spring", "3.4")
                            .pentest(true)
                            .securityFrameworks("owasp")
                            .container("docker")
                            .build();
            Path outputDir = runAndGetOutputDir(config);

            assertThat(outputDir.resolve(
                    ".claude/skills/x-pentest"))
                    .as("x-pentest skill must exist")
                    .isDirectory();
            assertThat(outputDir.resolve(
                    ".claude/agents/"
                            + "pentest-engineer.md"))
                    .as("pentest-engineer must exist")
                    .exists();
        }

        @Test
        @DisplayName("sonarqube provider adds sonar-gate"
                + " skill only")
        void pipeline_sonarqube_addsSonarGateOnly() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("spring", "3.4")
                            .qualityGateProvider("sonarqube")
                            .container("docker")
                            .build();
            Path outputDir = runAndGetOutputDir(config);

            assertThat(outputDir.resolve(
                    ".claude/skills/x-sonar-gate"))
                    .as("x-sonar-gate must exist")
                    .isDirectory();
            assertThat(outputDir.resolve(
                    ".claude/skills/x-sast-scan"))
                    .as("x-sast-scan must NOT exist")
                    .doesNotExist();
        }

        @Test
        @DisplayName("compliance frameworks add"
                + " auditor agent")
        void pipeline_compliance_addsAuditorAgent() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("spring", "3.4")
                            .securityFrameworks(
                                    "pci-dss", "lgpd")
                            .container("docker")
                            .build();
            Path outputDir = runAndGetOutputDir(config);

            assertThat(outputDir.resolve(
                    ".claude/agents/"
                            + "compliance-auditor.md"))
                    .as("compliance-auditor must exist")
                    .exists();
            assertThat(outputDir.resolve(
                    ".claude/agents/"
                            + "appsec-engineer.md"))
                    .as("appsec-engineer must exist")
                    .exists();
        }
    }

    // -- Helper methods --

    private ProjectConfig buildAllFlagsConfig() {
        return TestConfigBuilder.builder()
                .language("java", "21")
                .framework("spring", "3.4")
                .buildTool("maven")
                .scanningFlags(
                        true, true, true, true, true)
                .qualityGateProvider("sonarqube")
                .pentest(true)
                .securityFrameworks("pci-dss", "lgpd")
                .container("docker")
                .orchestrator("kubernetes")
                .clearInterfaces()
                .addInterface("rest")
                .build();
    }

    private PipelineResult runPipelineFor(
            ProjectConfig config) {
        Path outputDir = tempDir.resolve(
                "out-" + System.nanoTime());
        SmokeTestValidators.createDirectoryQuietly(
                outputDir);
        AssemblerPipeline pipeline =
                new AssemblerPipeline(
                        AssemblerPipeline.buildAssemblers());
        PipelineOptions options =
                new PipelineOptions(
                        false, true, false, null);
        return pipeline.runPipeline(
                config, outputDir, options);
    }

    private Path runAndGetOutputDir(ProjectConfig config) {
        Path outputDir = tempDir.resolve(
                "out-" + System.nanoTime());
        SmokeTestValidators.createDirectoryQuietly(
                outputDir);
        AssemblerPipeline pipeline =
                new AssemblerPipeline(
                        AssemblerPipeline.buildAssemblers());
        PipelineOptions options =
                new PipelineOptions(
                        false, true, false, null);
        PipelineResult result = pipeline.runPipeline(
                config, outputDir, options);
        assertThat(result.success())
                .as("Pipeline must succeed")
                .isTrue();
        return outputDir;
    }
}
