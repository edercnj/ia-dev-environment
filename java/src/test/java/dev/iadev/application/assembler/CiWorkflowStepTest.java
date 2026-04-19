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
 * Tests for CiWorkflowStep — generates CI workflow
 * artifacts.
 */
@DisplayName("CiWorkflowStep")
class CiWorkflowStepTest {

    @Nested
    @DisplayName("assemble — always generates ci.yml")
    class AlwaysGenerates {

        @Test
        @DisplayName("generates ci.yml in"
                + " .github/workflows")
        void assemble_whenCalled_generatesCiYml(@TempDir Path tempDir) {
            CiWorkflowStep assembler =
                    new CiWorkflowStep();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .container("none")
                    .orchestrator("none")
                    .smokeTests(false)
                    .build();
            TemplateEngine engine = new TemplateEngine();
            Map<String, Object> ctx =
                    CicdAssembler.buildStackContext(config);
            CicdContext cicdCtx = new CicdContext(
                    config, tempDir, resolveResources(),
                    engine, ctx);

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files()).hasSize(1);
            assertThat(result.files().get(0))
                    .contains("ci.yml");
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("ci.yml file exists on disk")
        void assemble_whenCalled_ciYmlExistsOnDisk(@TempDir Path tempDir) {
            CiWorkflowStep assembler =
                    new CiWorkflowStep();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .buildTool("maven")
                    .build();
            TemplateEngine engine = new TemplateEngine();
            Map<String, Object> ctx =
                    CicdAssembler.buildStackContext(config);
            CicdContext cicdCtx = new CicdContext(
                    config, tempDir, resolveResources(),
                    engine, ctx);

            assembler.assemble(cicdCtx);

            assertThat(tempDir.resolve(
                    ".github/workflows/ci.yml"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("Git Flow triggers")
    class GitFlowTriggers {

        @Test
        @DisplayName("push triggers include develop,"
                + " release/**, hotfix/**")
        void assemble_gitFlow_pushTriggersMultiBranch(
                @TempDir Path tempDir) throws Exception {
            CiWorkflowStep assembler =
                    new CiWorkflowStep();
            CicdContext cicdCtx = buildFullContext(
                    tempDir, "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/ci.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("develop")
                    .contains("release/**")
                    .contains("hotfix/**");
        }

        @Test
        @DisplayName("push triggers do not include main")
        void assemble_gitFlow_pushExcludesMain(
                @TempDir Path tempDir) throws Exception {
            CiWorkflowStep assembler =
                    new CiWorkflowStep();
            CicdContext cicdCtx = buildFullContext(
                    tempDir, "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/ci.yml"),
                    StandardCharsets.UTF_8);
            String pushSection = extractSection(
                    content, "push:", "pull_request:");
            assertThat(pushSection)
                    .doesNotContain("[main");
        }

        @Test
        @DisplayName("PR triggers include develop and"
                + " main")
        void assemble_gitFlow_prTriggersBothBranches(
                @TempDir Path tempDir) throws Exception {
            CiWorkflowStep assembler =
                    new CiWorkflowStep();
            CicdContext cicdCtx = buildFullContext(
                    tempDir, "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/ci.yml"),
                    StandardCharsets.UTF_8);
            String prSection = extractSection(
                    content, "pull_request:", "env:");
            assertThat(prSection)
                    .contains("develop")
                    .contains("main");
        }

        @Test
        @DisplayName("contains Git Flow YAML comment")
        void assemble_gitFlow_containsComment(
                @TempDir Path tempDir) throws Exception {
            CiWorkflowStep assembler =
                    new CiWorkflowStep();
            CicdContext cicdCtx = buildFullContext(
                    tempDir, "java", "maven");

            assembler.assemble(cicdCtx);

            String content = Files.readString(
                    tempDir.resolve(
                            ".github/workflows/ci.yml"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Git Flow");
        }
    }

    private static CicdContext buildFullContext(
            Path outputDir,
            String language,
            String buildTool) {
        ProjectConfig config = TestConfigBuilder
                .builder()
                .language(language, "21")
                .buildTool(buildTool)
                .container("none")
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

    private static String extractSection(
            String content,
            String start,
            String end) {
        int startIdx = content.indexOf(start);
        int endIdx = content.indexOf(end, startIdx);
        if (startIdx < 0 || endIdx < 0) {
            return content;
        }
        return content.substring(startIdx, endIdx);
    }

    private static Path resolveResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourceDir("shared")
                .getParent();
    }
}
