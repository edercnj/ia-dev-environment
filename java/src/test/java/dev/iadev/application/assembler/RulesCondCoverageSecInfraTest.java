package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
/**
 * Coverage tests for RulesConditionals —
 * security, cloud, infrastructure, and placeholders.
 */
@DisplayName("RulesConditionals — sec + infra")
class RulesCondCoverageSecInfraTest {

    @Nested
    @DisplayName("assembleSecurityRules")
    class SecurityWithFrameworks {

        @Test
        @DisplayName("copies base and compliance")
        void assembleSecurityRules_copiesBaseAndCompliance(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .securityFrameworks("owasp", "pci-dss")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path secDir = resourceDir.resolve("knowledge/security");
            Files.createDirectories(secDir);
            Files.writeString(
                    secDir.resolve(
                            "application-security.md"),
                    "App security content");
            Files.writeString(
                    secDir.resolve("cryptography.md"),
                    "Crypto content");
            Path compDir = secDir.resolve("compliance");
            Files.createDirectories(compDir);
            Files.writeString(
                    compDir.resolve("owasp.md"),
                    "OWASP content");
            Files.writeString(
                    compDir.resolve("pci-dss.md"),
                    "PCI content");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals
                            .assembleSecurityRules(
                                    config, resourceDir,
                                    skillsDir);
            assertThat(result).hasSize(4);
            assertThat(result).anyMatch(
                    f -> f.contains("application-security"));
            assertThat(result).anyMatch(
                    f -> f.contains("cryptography"));
            assertThat(result).anyMatch(
                    f -> f.contains("owasp.md"));
            assertThat(result).anyMatch(
                    f -> f.contains("pci-dss.md"));
        }

        @Test
        @DisplayName("base missing returns compliance")
        void assembleSecurityRules_baseMissing(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .securityFrameworks("owasp")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path compDir = resourceDir.resolve(
                    "knowledge/security/compliance");
            Files.createDirectories(compDir);
            Files.writeString(
                    compDir.resolve("owasp.md"),
                    "OWASP content");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals
                            .assembleSecurityRules(
                                    config, resourceDir,
                                    skillsDir);
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("compliance file missing skipped")
        void assembleSecurityRules_complianceMissing(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .securityFrameworks("unknown-fw")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path secDir = resourceDir.resolve("knowledge/security");
            Files.createDirectories(secDir);
            Files.writeString(
                    secDir.resolve(
                            "application-security.md"),
                    "App sec");
            Files.createDirectories(
                    secDir.resolve("compliance"));
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals
                            .assembleSecurityRules(
                                    config, resourceDir,
                                    skillsDir);
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("assembleCloudKnowledge")
    class CloudWithProvider {

        @Test
        @DisplayName("aws copies cloud file")
        void assembleCloudKnowledge_aws(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .cloudProvider("aws")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path cloudDir = resourceDir.resolve(
                    "knowledge/cloud-providers");
            Files.createDirectories(cloudDir);
            Files.writeString(
                    cloudDir.resolve("aws.md"),
                    "AWS patterns");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals
                            .assembleCloudKnowledge(
                                    config, resourceDir,
                                    skillsDir);
            assertThat(result).hasSize(1);
            assertThat(result.get(0))
                    .contains("cloud-aws.md");
        }

        @Test
        @DisplayName("provider file missing empty")
        void assembleCloudKnowledge_providerMissing(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .cloudProvider("gcp")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir.resolve(
                    "knowledge/cloud-providers"));
            List<String> result =
                    RulesConditionals
                            .assembleCloudKnowledge(
                                    config, resourceDir,
                                    tempDir.resolve(
                                            "skills"));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("empty provider returns empty")
        void assembleCloudKnowledge_emptyProvider(
                @TempDir Path tempDir) {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .cloudProvider("")
                    .build();
            List<String> result =
                    RulesConditionals
                            .assembleCloudKnowledge(
                                    config,
                                    tempDir.resolve("res"),
                                    tempDir.resolve(
                                            "skills"));
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("assembleInfraKnowledge")
    class InfraKnowledge {

        @Test
        @DisplayName("k8s copies deployment patterns")
        void assembleInfra_k8s(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .orchestrator("kubernetes")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path k8sDir = resourceDir.resolve(
                    "knowledge/infrastructure/kubernetes");
            Files.createDirectories(k8sDir);
            Files.writeString(
                    k8sDir.resolve(
                            "deployment-patterns.md"),
                    "K8s patterns");
            List<String> result =
                    RulesConditionals
                            .assembleInfraKnowledge(
                                    config, resourceDir,
                                    tempDir.resolve(
                                            "skills"));
            assertThat(result).anyMatch(
                    f -> f.contains("k8s-deployment.md"));
        }

        @Test
        @DisplayName("k8s file missing empty result")
        void assembleInfra_k8sFileMissing(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .orchestrator("kubernetes")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir.resolve(
                    "knowledge/infrastructure/kubernetes"));
            List<String> result =
                    RulesConditionals
                            .assembleInfraKnowledge(
                                    config, resourceDir,
                                    tempDir.resolve(
                                            "skills"));
            assertThat(result).noneMatch(
                    f -> f.contains("k8s-deployment"));
        }

        @Test
        @DisplayName("docker copies container files")
        void assembleInfra_docker(@TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path containers = resourceDir.resolve(
                    "knowledge/infrastructure/containers");
            Files.createDirectories(containers);
            Files.writeString(
                    containers.resolve(
                            "dockerfile-patterns.md"),
                    "Dockerfile");
            Files.writeString(
                    containers.resolve(
                            "registry-patterns.md"),
                    "Registry");
            List<String> result =
                    RulesConditionals
                            .assembleInfraKnowledge(
                                    config, resourceDir,
                                    tempDir.resolve(
                                            "skills"));
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("container=none no container files")
        void assembleInfra_noContainer(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("none")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            List<String> result =
                    RulesConditionals
                            .assembleInfraKnowledge(
                                    config, resourceDir,
                                    tempDir.resolve(
                                            "skills"));
            assertThat(result).noneMatch(
                    f -> f.contains("dockerfile"));
        }

        @Test
        @DisplayName("container files missing empty")
        void assembleInfra_containerFilesMissing(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("docker")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            List<String> result =
                    RulesConditionals
                            .assembleInfraKnowledge(
                                    config, resourceDir,
                                    tempDir.resolve(
                                            "skills"));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("terraform copies IaC")
        void assembleInfra_terraform(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .iac("terraform")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path iacDir = resourceDir.resolve(
                    "knowledge/infrastructure/iac");
            Files.createDirectories(iacDir);
            Files.writeString(
                    iacDir.resolve(
                            "terraform-patterns.md"),
                    "TF");
            List<String> result =
                    RulesConditionals
                            .assembleInfraKnowledge(
                                    config, resourceDir,
                                    tempDir.resolve(
                                            "skills"));
            assertThat(result).anyMatch(
                    f -> f.contains("iac-terraform.md"));
        }

        @Test
        @DisplayName("iac=none no IaC files")
        void assembleInfra_noIac(
                @TempDir Path tempDir) {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .iac("none")
                    .build();
            List<String> result =
                    RulesConditionals
                            .assembleInfraKnowledge(
                                    config,
                                    tempDir.resolve("res"),
                                    tempDir.resolve(
                                            "skills"));
            assertThat(result).noneMatch(
                    f -> f.contains("iac-"));
        }

        @Test
        @DisplayName("empty iac no IaC files")
        void assembleInfra_emptyIac(
                @TempDir Path tempDir) {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .iac("")
                    .build();
            List<String> result =
                    RulesConditionals
                            .assembleInfraKnowledge(
                                    config,
                                    tempDir.resolve("res"),
                                    tempDir.resolve(
                                            "skills"));
            assertThat(result).noneMatch(
                    f -> f.contains("iac-"));
        }

        @Test
        @DisplayName("iac file missing empty")
        void assembleInfra_iacFileMissing(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .iac("pulumi")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir.resolve(
                    "knowledge/infrastructure/iac"));
            List<String> result =
                    RulesConditionals
                            .assembleInfraKnowledge(
                                    config, resourceDir,
                                    tempDir.resolve(
                                            "skills"));
            assertThat(result).noneMatch(
                    f -> f.contains("iac-"));
        }
    }

    @Nested
    @DisplayName("copyDatabaseRefs — placeholder")
    class PlaceholderReplacement {

        @Test
        @DisplayName("placeholders replaced")
        void copyDatabaseRefs_placeholdersReplaced(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("postgresql", "16")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(
                    resourceDir.resolve("knowledge/databases"));
            Files.writeString(
                    resourceDir.resolve(
                            "knowledge/databases/version-matrix.md"),
                    "DB: {DATABASE_NAME}");
            Path skillsDir = tempDir.resolve("skills");
            Map<String, Object> context = Map.of(
                    "database_name", "postgresql");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    context));
            assertThat(result).isNotEmpty();
            Path target = skillsDir.resolve(
                    "database-patterns/references/"
                            + "version-matrix.md");
            String content = Files.readString(
                    target, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("postgresql");
        }
    }
}
