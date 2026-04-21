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
 * Verifies that Rule 22 (Lifecycle Integrity) is copied from
 * the classpath resources into the generated
 * {@code .claude/rules/} output. Story-0046-0001,
 * TASK-0046-0001-001.
 *
 * <p>This test uses the real classpath under
 * {@code java/src/main/resources} so a regression (e.g. the
 * rule file being removed or renumbered silently) is surfaced
 * immediately.</p>
 */
@DisplayName("RulesAssembler — rule 22 Lifecycle Integrity")
class RulesAssemblerLifecycleIntegrityTest {

    @Test
    @DisplayName("22-lifecycle-integrity.md is copied to "
            + "output rules directory")
    void listRules_includesLifecycleIntegrity(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        RulesAssembler assembler = new RulesAssembler();
        ProjectConfig config = TestConfigBuilder.minimal();

        assembler.assemble(
                config, new TemplateEngine(), outputDir);

        Path ruleFile = outputDir.resolve(
                "rules/22-lifecycle-integrity.md");
        assertThat(ruleFile)
                .as("22-lifecycle-integrity.md must be copied "
                        + "from source. If this fails, verify "
                        + "that java/src/main/resources/"
                        + "targets/claude/rules/"
                        + "22-lifecycle-integrity.md exists.")
                .exists();
    }

    @Test
    @DisplayName("22-lifecycle-integrity.md contains all "
            + "mandatory sections")
    void listRules_lifecycleIntegrity_hasMandatorySections(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        RulesAssembler assembler = new RulesAssembler();
        ProjectConfig config = TestConfigBuilder.minimal();

        assembler.assemble(
                config, new TemplateEngine(), outputDir);

        String content = Files.readString(
                outputDir.resolve(
                        "rules/22-lifecycle-integrity.md"),
                StandardCharsets.UTF_8);

        assertThat(content)
                .contains("## Rule")
                .contains("## Scope")
                .contains("## Transition Matrix")
                .contains("## Enforcement")
                .contains("## Forbidden");
    }

    @Test
    @DisplayName("22-lifecycle-integrity.md contains "
            + "canonical enum values and matrix identifiers")
    void listRules_lifecycleIntegrity_containsCanonicalContent(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        RulesAssembler assembler = new RulesAssembler();
        ProjectConfig config = TestConfigBuilder.minimal();

        assembler.assemble(
                config, new TemplateEngine(), outputDir);

        String content = Files.readString(
                outputDir.resolve(
                        "rules/22-lifecycle-integrity.md"),
                StandardCharsets.UTF_8);

        assertThat(content)
                .contains("Pendente")
                .contains("Planejada")
                .contains("Em Andamento")
                .contains("Concluída")
                .contains("Falha")
                .contains("Bloqueada")
                .contains("StatusFieldParser")
                .contains("LifecycleTransitionMatrix")
                .contains("LifecycleAuditRunner")
                .contains("RULE-046-01");
    }
}
