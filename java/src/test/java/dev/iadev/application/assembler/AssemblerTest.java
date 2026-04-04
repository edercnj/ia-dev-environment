package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Assembler interface contract (RULE-004).
 */
@DisplayName("Assembler interface")
class AssemblerTest {

    @Test
    @DisplayName("interface defines assemble method returning List<String>")
    void assemble_whenCalled_returnsFileList(@TempDir Path tempDir) {
        Assembler assembler = (config, engine, outputDir) ->
                List.of("file1.md", "file2.md");

        ProjectConfig config = TestConfigBuilder.minimal();
        TemplateEngine engine = new TemplateEngine();

        List<String> result = assembler.assemble(
                config, engine, tempDir);

        assertThat(result)
                .containsExactly("file1.md", "file2.md");
    }

    @Test
    @DisplayName("assembler can return empty list")
    void assemble_whenCalled_returnsEmptyList(@TempDir Path tempDir) {
        Assembler assembler = (config, engine, outputDir) ->
                List.of();

        List<String> result = assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), tempDir);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("interface is a functional interface")
    void assembler_whenCalled_isFunctionalInterface() {
        // Verify Assembler can be used as lambda
        Assembler lambda = (c, e, p) -> List.of();
        assertThat(lambda)
                .isInstanceOf(Assembler.class);
        assertThat(lambda.assemble(null, null, null))
                .isEmpty();
    }
}
