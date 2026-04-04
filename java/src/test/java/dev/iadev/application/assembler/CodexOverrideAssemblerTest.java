package dev.iadev.application.assembler;

import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CodexOverrideAssembler")
class CodexOverrideAssemblerTest {

    @Test
    @DisplayName("generates AGENTS.override.md at root")
    void assemble_whenCalled_generatesOverride(
            @TempDir Path tempDir) throws IOException {
        CodexOverrideAssembler assembler =
                new CodexOverrideAssembler();

        List<String> files = assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), tempDir);

        assertThat(files).hasSize(1);
        Path override = tempDir.resolve("AGENTS.override.md");
        assertThat(override).exists();
        String content = Files.readString(
                override, StandardCharsets.UTF_8);
        assertThat(content)
                .contains("HOW OVERRIDES WORK")
                .contains("REPLACES (not merges)")
                .contains("Add your override instructions");
    }
}
