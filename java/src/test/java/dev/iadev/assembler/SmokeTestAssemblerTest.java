package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SmokeTestAssembler — generates smoke test
 * config conditionally.
 */
@DisplayName("SmokeTestAssembler")
class SmokeTestAssemblerTest {

    @Nested
    @DisplayName("assemble — smokeTests=true")
    class SmokeEnabled {

        @Test
        @DisplayName("generates smoke-config.md")
        void generatesSmokeConfig(
                @TempDir Path tempDir) {
            SmokeTestAssembler assembler =
                    new SmokeTestAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .smokeTests(true)
                    .container("none")
                    .orchestrator("none")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files()).hasSize(1);
            assertThat(result.files().get(0))
                    .contains("smoke-config.md");
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("smoke-config.md exists on disk")
        void fileExistsOnDisk(@TempDir Path tempDir) {
            SmokeTestAssembler assembler =
                    new SmokeTestAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .smokeTests(true)
                    .container("none")
                    .orchestrator("none")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            assembler.assemble(cicdCtx);

            assertThat(tempDir.resolve(
                    "tests/smoke/smoke-config.md"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("assemble — smokeTests=false")
    class SmokeDisabled {

        @Test
        @DisplayName("skips smoke-config.md when"
                + " smokeTests=false")
        void skipsSmokeConfig(@TempDir Path tempDir) {
            SmokeTestAssembler assembler =
                    new SmokeTestAssembler();
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .smokeTests(false)
                    .container("none")
                    .orchestrator("none")
                    .build();
            CicdContext cicdCtx = buildContext(
                    config, tempDir);

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "smokeTests is false"));
        }
    }

    @Nested
    @DisplayName("assemble — source file missing")
    class SourceMissing {

        @Test
        @DisplayName("returns empty result when"
                + " source file does not exist")
        void emptyWhenSourceMissing(
                @TempDir Path tempDir)
                throws IOException {
            SmokeTestAssembler assembler =
                    new SmokeTestAssembler();
            Path resDir = tempDir.resolve("res");
            Files.createDirectories(
                    resDir.resolve("cicd-templates"));
            Path outputDir = tempDir.resolve("output");

            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .smokeTests(true)
                    .build();
            Map<String, Object> ctx =
                    CicdAssembler.buildStackContext(config);
            CicdContext cicdCtx = new CicdContext(
                    config, outputDir, resDir,
                    new TemplateEngine(), ctx);

            CicdResult result = assembler.assemble(cicdCtx);

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }
    }

    private static CicdContext buildContext(
            ProjectConfig config, Path outputDir) {
        Map<String, Object> ctx =
                CicdAssembler.buildStackContext(config);
        return new CicdContext(
                config, outputDir, resolveResources(),
                new TemplateEngine(), ctx);
    }

    private static Path resolveResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot("cicd-templates");
    }
}
