package dev.iadev.assembler;

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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CodexRequirementsAssembler")
class CodexRequirementsAssemblerTest {

    @Nested
    @DisplayName("deriveApprovalPolicy")
    class DeriveApprovalPolicy {

        @Test
        @DisplayName("returns suggest when security frameworks exist")
        void derive_whenSecurityPresent_returnsSuggest() {
            ProjectConfig config = TestConfigBuilder.builder()
                    .securityFrameworks("owasp")
                    .build();

            assertThat(CodexRequirementsAssembler
                    .deriveApprovalPolicy(config))
                    .isEqualTo("suggest");
        }

        @Test
        @DisplayName("returns on-request when no security frameworks")
        void derive_whenNoSecurity_returnsOnRequest() {
            ProjectConfig config = TestConfigBuilder.minimal();

            assertThat(CodexRequirementsAssembler
                    .deriveApprovalPolicy(config))
                    .isEqualTo("on-request");
        }
    }

    @Nested
    @DisplayName("assemble")
    class Assemble {

        @Test
        @DisplayName("generates requirements.toml")
        void assemble_whenCalled_generatesFile(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve(".codex");
            Files.createDirectories(outputDir);

            CodexRequirementsAssembler assembler =
                    new CodexRequirementsAssembler();
            List<String> files = assembler.assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), outputDir);

            assertThat(files).hasSize(1);
            Path req = outputDir.resolve("requirements.toml");
            assertThat(req).exists();
            String content = Files.readString(
                    req, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("[policy]")
                    .contains("approval_policy = \"on-request\"")
                    .contains("[sandbox]")
                    .contains("mode = \"workspace-write\"");
        }
    }
}
