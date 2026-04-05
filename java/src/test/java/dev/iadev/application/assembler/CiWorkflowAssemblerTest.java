package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CiWorkflowAssembler — generates CI workflow
 * artifacts.
 */
@DisplayName("CiWorkflowAssembler")
class CiWorkflowAssemblerTest {

    @Nested
    @DisplayName("assemble — always generates ci.yml")
    class AlwaysGenerates {

        @Test
        @DisplayName("generates ci.yml in"
                + " .github/workflows")
        void assemble_whenCalled_generatesCiYml(@TempDir Path tempDir) {
            CiWorkflowAssembler assembler =
                    new CiWorkflowAssembler();
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
            CiWorkflowAssembler assembler =
                    new CiWorkflowAssembler();
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

    private static Path resolveResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot("shared/cicd-templates", 2);
    }
}
