package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

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

/**
 * Tests that SkillsAssembler injects context-budget field
 * into generated SKILL.md frontmatter.
 */
@DisplayName("SkillsAssembler — context-budget injection")
class SkillsAssemblerContextBudgetTest {

    @Nested
    @DisplayName("core skill context-budget generation")
    class CoreSkillBudget {

        @Test
        @DisplayName("light skill gets context-budget: light")
        void assemble_lightSkill_injectsBudgetLight(
                @TempDir Path tempDir) throws IOException {
            createSkillTemplate(
                    tempDir, "x-simple", 100);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path generated = outputDir.resolve(
                    "skills/x-simple/SKILL.md");
            assertThat(generated).exists();
            String content = Files.readString(
                    generated, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("context-budget: light");
        }

        @Test
        @DisplayName("medium skill gets"
                + " context-budget: medium")
        void assemble_mediumSkill_injectsBudgetMedium(
                @TempDir Path tempDir) throws IOException {
            createSkillTemplate(
                    tempDir, "x-medium", 300);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path generated = outputDir.resolve(
                    "skills/x-medium/SKILL.md");
            assertThat(generated).exists();
            String content = Files.readString(
                    generated, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("context-budget: medium");
        }

        @Test
        @DisplayName("heavy skill gets"
                + " context-budget: heavy")
        void assemble_heavySkill_injectsBudgetHeavy(
                @TempDir Path tempDir) throws IOException {
            createSkillTemplate(
                    tempDir, "x-heavy", 600);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path generated = outputDir.resolve(
                    "skills/x-heavy/SKILL.md");
            assertThat(generated).exists();
            String content = Files.readString(
                    generated, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("context-budget: heavy");
        }

        @Test
        @DisplayName("budget field appears after"
                + " argument-hint")
        void assemble_withHint_budgetAfterHint(
                @TempDir Path tempDir) throws IOException {
            createSkillTemplate(tempDir, "x-test", 150);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path generated = outputDir.resolve(
                    "skills/x-test/SKILL.md");
            String content = Files.readString(
                    generated, StandardCharsets.UTF_8);
            int hintPos = content.indexOf("argument-hint");
            int budgetPos =
                    content.indexOf("context-budget");
            assertThat(budgetPos)
                    .isGreaterThan(hintPos);
        }
    }

    /**
     * Creates a skill template with the given number of
     * lines and standard YAML frontmatter.
     */
    private Path createSkillTemplate(
            Path tempDir,
            String skillName,
            int lineCount) throws IOException {
        Path skillDir = tempDir.resolve(
                "targets/claude/skills/core/"
                        + skillName);
        Files.createDirectories(skillDir);

        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("name: ").append(skillName).append("\n");
        sb.append("description: \"Test skill\"\n");
        sb.append("user-invocable: true\n");
        sb.append("argument-hint: \"[args]\"\n");
        sb.append("---\n");
        sb.append("## Content\n");
        int remaining = lineCount - 7;
        for (int i = 0; i < remaining; i++) {
            sb.append("Line ").append(i).append("\n");
        }

        Files.writeString(
                skillDir.resolve("SKILL.md"),
                sb.toString(),
                StandardCharsets.UTF_8);
        return skillDir;
    }
}
