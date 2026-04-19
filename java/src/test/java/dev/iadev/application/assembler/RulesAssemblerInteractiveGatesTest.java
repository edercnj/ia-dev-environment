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
 * Verifies that rule 20 (interactive gates) is copied from the
 * classpath resources into the generated {@code .claude/rules/}
 * output. Story-0043-0001, TASK-0043-0001-004.
 *
 * <p>This test uses the real classpath under
 * {@code java/src/main/resources} so that a regression in
 * copying the rule file is surfaced immediately — removing
 * {@code 20-interactive-gates.md} from source must fail this
 * test with a clear message.</p>
 */
@DisplayName("RulesAssembler — rule 20 interactive gates")
class RulesAssemblerInteractiveGatesTest {

    @Test
    @DisplayName("20-interactive-gates.md is copied to"
            + " output rules directory")
    void listRules_includesInteractiveGates(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        RulesAssembler assembler = new RulesAssembler();
        ProjectConfig config = TestConfigBuilder.minimal();

        assembler.assemble(
                config, new TemplateEngine(), outputDir);

        Path ruleFile = outputDir.resolve(
                "rules/20-interactive-gates.md");
        assertThat(ruleFile)
                .as("20-interactive-gates.md must be "
                        + "copied from source. If this "
                        + "fails, verify that "
                        + "java/src/main/resources/targets"
                        + "/claude/rules/"
                        + "20-interactive-gates.md exists.")
                .exists();
    }

    @Test
    @DisplayName("20-interactive-gates.md contains 10"
            + " mandatory sections")
    void listRules_interactiveGates_hasTenSections(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        RulesAssembler assembler = new RulesAssembler();
        ProjectConfig config = TestConfigBuilder.minimal();

        assembler.assemble(
                config, new TemplateEngine(), outputDir);

        String content = Files.readString(
                outputDir.resolve(
                        "rules/20-interactive-gates.md"),
                StandardCharsets.UTF_8);

        long sectionCount = content.lines()
                .filter(l -> l.startsWith("## "))
                .count();
        assertThat(sectionCount)
                .as("Rule 20 must have exactly 10 "
                        + "mandatory sections (Rule, Scope, "
                        + "Canonical Option Menu, State File "
                        + "Schema, Default Behavior, FIX-PR "
                        + "Loop-Back, Deprecation of Opt-In "
                        + "Flags, Forbidden, Audit Command, "
                        + "Rationale)")
                .isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("20-interactive-gates.md contains"
            + " canonical slot labels and state schema")
    void listRules_interactiveGates_containsMandatoryContent(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        RulesAssembler assembler = new RulesAssembler();
        ProjectConfig config = TestConfigBuilder.minimal();

        assembler.assemble(
                config, new TemplateEngine(), outputDir);

        String content = Files.readString(
                outputDir.resolve(
                        "rules/20-interactive-gates.md"),
                StandardCharsets.UTF_8);

        assertThat(content)
                .contains("Continue (Recommended)")
                .contains("Run x-pr-fix and retry")
                .contains("Cancel the operation")
                .contains("GATE_FIX_LOOP_EXCEEDED")
                .contains("schemaVersion")
                .contains("fixAttempts")
                .contains("lastGateDecision")
                .contains("--non-interactive")
                .contains("AskUserQuestion");
    }
}
