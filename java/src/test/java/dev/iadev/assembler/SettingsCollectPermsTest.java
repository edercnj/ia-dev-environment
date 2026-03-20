package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SettingsAssembler —
 * collectPermissions method.
 */
@DisplayName("SettingsAssembler — collectPermissions")
class SettingsCollectPermsTest {

    @Nested
    @DisplayName("collectPermissions — permission merging")
    class CollectPermissions {

        @Test
        @DisplayName("maven config includes base + maven"
                + " permissions")
        void collectPermissions_maven_includesBaseAndMaven(
                @TempDir Path tempDir)
                throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);

            SettingsAssembler assembler =
                    new SettingsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    assembler.collectPermissions(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(git *)")
                    .contains("Bash(mvn *)");
        }

        @Test
        @DisplayName("docker container adds docker"
                + " permissions")
        void collectPermissions_docker_addsDockerPerms(
                @TempDir Path tempDir)
                throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);

            SettingsAssembler assembler =
                    new SettingsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .container("docker")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    assembler.collectPermissions(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(docker build *)");
        }

        @Test
        @DisplayName("kubernetes orchestrator adds k8s"
                + " permissions")
        void collectPermissions_k8s_addsK8sPerms(
                @TempDir Path tempDir)
                throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);

            SettingsAssembler assembler =
                    new SettingsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .container("none")
                            .orchestrator("kubernetes")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    assembler.collectPermissions(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(kubectl get *)");
        }

        @Test
        @DisplayName("smoke tests add newman permissions")
        void collectPermissions_whenCalled_smokeTestsAddNewman(
                @TempDir Path tempDir)
                throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);

            SettingsAssembler assembler =
                    new SettingsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(true)
                            .build();

            List<String> perms =
                    assembler.collectPermissions(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(newman *)");
        }

        private Path setupTemplatesDir(Path tempDir)
                throws IOException {
            Path templatesDir = tempDir.resolve(
                    "settings-templates");
            Files.createDirectories(templatesDir);
            Files.writeString(
                    templatesDir.resolve("base.json"),
                    "[\"Bash(git *)\"]",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    templatesDir.resolve(
                            "java-maven.json"),
                    "[\"Bash(mvn *)\"]",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    templatesDir.resolve("docker.json"),
                    "[\"Bash(docker build *)\"]",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    templatesDir.resolve(
                            "kubernetes.json"),
                    "[\"Bash(kubectl get *)\"]",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    templatesDir.resolve(
                            "testing-newman.json"),
                    "[\"Bash(newman *)\"]",
                    StandardCharsets.UTF_8);
            return templatesDir;
        }
    }
}
