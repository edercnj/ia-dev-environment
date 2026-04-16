package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that rule 20 (telemetry privacy) is copied from the
 * classpath resources into the generated {@code .claude/rules/}
 * output. Story-0040-0005, TASK-0040-0005-001.
 *
 * <p>This test DOES NOT use the minimal in-memory resource
 * fixture (which only seeds a subset of core rules). It uses
 * the real classpath under {@code java/src/main/resources}
 * so that a regression in copying the rule file is surfaced.
 */
@DisplayName("RulesAssembler — rule 20 telemetry privacy")
class RulesAssemblerTelemetryTest {

    @Test
    @DisplayName("20-telemetry-privacy.md is copied to"
            + " output rules directory")
    void assemble_rule20_isCopiedToOutput(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        RulesAssembler assembler = new RulesAssembler();
        ProjectConfig config = TestConfigBuilder.minimal();

        assembler.assemble(
                config, new TemplateEngine(), outputDir);

        Path ruleFile = outputDir.resolve(
                "rules/20-telemetry-privacy.md");
        assertThat(ruleFile).exists();
    }

    @Test
    @DisplayName("20-telemetry-privacy.md contains the 8"
            + " mandatory blocked patterns and the whitelist"
            + " keys")
    void assemble_rule20_containsMandatorySections(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        RulesAssembler assembler = new RulesAssembler();
        ProjectConfig config = TestConfigBuilder.minimal();

        assembler.assemble(
                config, new TemplateEngine(), outputDir);

        String content = Files.readString(
                outputDir.resolve(
                        "rules/20-telemetry-privacy.md"),
                StandardCharsets.UTF_8);

        assertThat(content)
                .contains("AWS_KEY_REDACTED")
                .contains("AWS_SECRET_REDACTED")
                .contains("JWT_REDACTED")
                .contains("BEARER_REDACTED")
                .contains("GITHUB_TOKEN_REDACTED")
                .contains("EMAIL_REDACTED")
                .contains("CPF_REDACTED")
                .contains("USER:PASS_REDACTED");

        assertThat(content)
                .contains("retryCount")
                .contains("commitSha")
                .contains("filesChanged")
                .contains("linesAdded")
                .contains("linesDeleted")
                .contains("exitCode")
                .contains("toolAttempt")
                .contains("phaseNumber");
    }
}
