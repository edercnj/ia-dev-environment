package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CdWorkflowStep — generates CD workflow
 * artifacts (release, deploy-staging, deploy-production,
 * rollback).
 */
@DisplayName("CdWorkflowStep")
class CdWorkflowStepTest {

    @Nested
    @DisplayName("release workflow — always generated")
    class ReleaseWorkflow {

        @Test
        @DisplayName("generates release.yml in"
                + " .github/workflows")
        void assemble_always_generatesReleaseYml(
                @TempDir Path tempDir) {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "none", "java", "maven");

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files())
                    .anyMatch(f -> f.contains(
                            "release.yml"));
            assertThat(tempDir.resolve(
                    ".github/workflows/release.yml"))
                    .exists();
        }

        @Test
        @DisplayName("release.yml without Docker step"
                + " when container=none")
        void assemble_noContainer_noDockerStep(
                @TempDir Path tempDir) throws Exception {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "none", "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/release.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .doesNotContain(
                            "Build & Push Docker Image");
        }

        @Test
        @DisplayName("release.yml contains setup-java"
                + " for java/maven")
        void assemble_javaMaven_containsSetupJava(
                @TempDir Path tempDir) throws Exception {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/release.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("setup-java")
                    .contains("java-version");
        }

        @Test
        @DisplayName("release.yml contains Docker"
                + " step when container=docker")
        void assemble_docker_containsDockerStep(
                @TempDir Path tempDir) throws Exception {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/release.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Build & Push Docker Image");
        }
    }

    @Nested
    @DisplayName("deploy-staging — conditional")
    class DeployStagingWorkflow {

        @Test
        @DisplayName("generated when container=docker")
        void assemble_docker_generatesStagingYml(
                @TempDir Path tempDir) {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files())
                    .anyMatch(f -> f.contains(
                            "deploy-staging.yml"));
            assertThat(tempDir.resolve(
                    ".github/workflows/deploy-staging.yml"))
                    .exists();
        }

        @Test
        @DisplayName("skipped when container=none")
        void assemble_noContainer_skipsStagingYml(
                @TempDir Path tempDir) {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "none", "java", "maven");

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files())
                    .noneMatch(f -> f.contains(
                            "deploy-staging.yml"));
        }
    }

    @Nested
    @DisplayName("deploy-production — conditional")
    class DeployProductionWorkflow {

        @Test
        @DisplayName("generated when container=docker")
        void assemble_docker_generatesProductionYml(
                @TempDir Path tempDir) {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files())
                    .anyMatch(f -> f.contains(
                            "deploy-production.yml"));
            assertThat(tempDir.resolve(
                    ".github/workflows/"
                            + "deploy-production.yml"))
                    .exists();
        }

        @Test
        @DisplayName("contains workflow_dispatch trigger")
        void assemble_docker_hasWorkflowDispatch(
                @TempDir Path tempDir) throws Exception {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/"
                                    + "deploy-production.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("workflow_dispatch");
        }

        @Test
        @DisplayName("contains version input")
        void assemble_docker_hasVersionInput(
                @TempDir Path tempDir) throws Exception {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/"
                                    + "deploy-production.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content).contains("version:");
        }

        @Test
        @DisplayName("skipped when container=none")
        void assemble_noContainer_skipsProductionYml(
                @TempDir Path tempDir) {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "none", "java", "maven");

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files())
                    .noneMatch(f -> f.contains(
                            "deploy-production.yml"));
        }
    }

    @Nested
    @DisplayName("rollback — conditional")
    class RollbackWorkflow {

        @Test
        @DisplayName("generated when container=docker")
        void assemble_docker_generatesRollbackYml(
                @TempDir Path tempDir) {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files())
                    .anyMatch(f -> f.contains(
                            "rollback.yml"));
            assertThat(tempDir.resolve(
                    ".github/workflows/rollback.yml"))
                    .exists();
        }

        @Test
        @DisplayName("contains version input")
        void assemble_docker_hasVersionInput(
                @TempDir Path tempDir) throws Exception {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/rollback.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content).contains("version:");
        }

        @Test
        @DisplayName("contains environment choices")
        void assemble_docker_hasEnvironmentChoices(
                @TempDir Path tempDir) throws Exception {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/rollback.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("staging")
                    .contains("production");
        }

        @Test
        @DisplayName("skipped when container=none")
        void assemble_noContainer_skipsRollbackYml(
                @TempDir Path tempDir) {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "none", "java", "maven");

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files())
                    .noneMatch(f -> f.contains(
                            "rollback.yml"));
        }
    }

    @Nested
    @DisplayName("Git Flow triggers")
    class GitFlowTriggers {

        @Test
        @DisplayName("staging triggers on develop only")
        void assemble_staging_triggersOnDevelop(
                @TempDir Path tempDir) throws Exception {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/"
                                    + "deploy-staging.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("branches: [develop]");
            assertThat(content)
                    .doesNotContain("branches: [main]");
        }

        @Test
        @DisplayName("staging contains Git Flow comment")
        void assemble_staging_hasGitFlowComment(
                @TempDir Path tempDir) throws Exception {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/"
                                    + "deploy-staging.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Git Flow");
        }

        @Test
        @DisplayName("production triggers on main"
                + " and version tags")
        void assemble_production_triggersOnMainAndTags(
                @TempDir Path tempDir) throws Exception {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/"
                                    + "deploy-production.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("branches: [main]")
                    .contains("tags:")
                    .contains("'v*'");
        }

        @Test
        @DisplayName("production contains Git Flow"
                + " comment")
        void assemble_production_hasGitFlowComment(
                @TempDir Path tempDir) throws Exception {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/"
                                    + "deploy-production.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Git Flow");
        }

        @Test
        @DisplayName("production does not trigger"
                + " on develop")
        void assemble_production_excludesDevelop(
                @TempDir Path tempDir) throws Exception {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/"
                                    + "deploy-production.yml"),
                    StandardCharsets.UTF_8);
            String pushSection = extractPushSection(
                    content);
            assertThat(pushSection)
                    .doesNotContain("develop");
        }

        private String extractPushSection(String content) {
            int pushIdx = content.indexOf("push:");
            int wdIdx = content.indexOf(
                    "workflow_dispatch:", pushIdx);
            if (pushIdx < 0 || wdIdx < 0) {
                return content;
            }
            return content.substring(pushIdx, wdIdx);
        }
    }

    @Nested
    @DisplayName("file counts")
    class FileCounts {

        @Test
        @DisplayName("generates 4 files when"
                + " container=docker")
        void assemble_docker_generatesFourFiles(
                @TempDir Path tempDir) {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "docker", "java", "maven");

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files()).hasSize(4);
        }

        @Test
        @DisplayName("generates 1 file when"
                + " container=none")
        void assemble_noContainer_generatesOneFile(
                @TempDir Path tempDir) {
            CdWorkflowStep assembler =
                    new CdWorkflowStep();
            CicdContext cicdCtx = buildContext(
                    tempDir, "none", "java", "maven");

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files()).hasSize(1);
            assertThat(result.files().get(0))
                    .contains("release.yml");
        }
    }

    private static CicdContext buildContext(
            Path outputDir,
            String container,
            String language,
            String buildTool) {
        ProjectConfig config = TestConfigBuilder
                .builder()
                .language(language, "21")
                .buildTool(buildTool)
                .container(container)
                .orchestrator("none")
                .smokeTests(false)
                .build();
        TemplateEngine engine = new TemplateEngine();
        Map<String, Object> baseCtx =
                ContextBuilder.buildContext(config);
        Map<String, Object> stackCtx =
                CicdAssembler.buildStackContext(config);
        Map<String, Object> merged =
                new LinkedHashMap<>(baseCtx);
        merged.putAll(stackCtx);
        return new CicdContext(
                config, outputDir,
                resolveResources(),
                engine, merged);
    }

    private static Path resolveResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourceDir("shared")
                .getParent();
    }
}
