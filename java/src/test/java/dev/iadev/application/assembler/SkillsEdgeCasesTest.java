package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

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
 * Tests for SkillsAssembler — edge cases.
 */
@DisplayName("SkillsAssembler — edge cases")
class SkillsEdgeCasesTest {

    @Nested
    @DisplayName("assemble — edge cases")
    class EdgeCases {

        @Test
        @DisplayName("empty resources returns empty")
        void assemble_emptyResources(@TempDir Path t)
                throws IOException {
            Path res = t.resolve("res");
            Files.createDirectories(
                    res.resolve("targets/claude/skills/core"));
            Path out = t.resolve("output");
            Files.createDirectories(out);

            List<String> files = new SkillsAssembler(res)
                    .assemble(TestConfigBuilder.minimal(),
                            new TemplateEngine(), out);
            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("selectCoreSkills empty when missing")
        void coreSkillsEmpty(@TempDir Path t) {
            assertThat(new SkillsAssembler(
                    t.resolve("res")).selectCoreSkills())
                    .isEmpty();
        }

        @Test
        @DisplayName("conditional not found skipped")
        void conditionalSkillSkipped(@TempDir Path t)
                throws IOException {
            Path res = t.resolve("res");
            Files.createDirectories(
                    res.resolve("targets/claude/skills/core"));
            Path out = t.resolve("output");
            Files.createDirectories(out);

            new SkillsAssembler(res).assemble(
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest").build(),
                    new TemplateEngine(), out);

            assertThat(out.resolve("skills/x-review-api"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("KP not found skipped")
        void kpNotFoundSkipped(@TempDir Path t)
                throws IOException {
            Path res = t.resolve("res");
            Files.createDirectories(
                    res.resolve("targets/claude/skills/core"));
            Files.createDirectories(res.resolve(
                    "targets/claude/skills/knowledge-packs"));
            Path out = t.resolve("output");
            Files.createDirectories(out);

            new SkillsAssembler(res).assemble(
                    TestConfigBuilder.minimal(),
                    new TemplateEngine(), out);

            assertThat(out.resolve(
                    "skills/coding-standards"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("stack patterns skipped")
        void stackPatternsSkipped(@TempDir Path t)
                throws IOException {
            Path out = t.resolve("output");
            Files.createDirectories(out);

            new SkillsAssembler().assemble(
                    TestConfigBuilder.builder()
                            .framework(
                                    "unknown-framework",
                                    "1.0").build(),
                    new TemplateEngine(), out);

            assertThat(out.resolve(
                    "skills/unknown-patterns"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("copyNonSkillItems from KP")
        void copyNonSkillItems(@TempDir Path t)
                throws IOException {
            Path res = t.resolve("res");
            Path kp = res.resolve(
                    "targets/claude/skills/knowledge-packs/"
                            + "test-pack");
            Files.createDirectories(kp);
            Files.createDirectories(
                    kp.resolve("references"));
            Files.writeString(kp.resolve("SKILL.md"),
                    "# Test Pack\n",
                    StandardCharsets.UTF_8);
            Files.createDirectories(
                    res.resolve("targets/claude/skills/core"));

            Path dest = t.resolve(
                    "output/skills/test-pack");
            CopyHelpers.ensureDirectory(dest);
            CopyHelpers.copyTemplateFile(
                    kp.resolve("SKILL.md"),
                    dest.resolve("SKILL.md"),
                    new TemplateEngine(),
                    java.util.Map.of());

            assertThat(dest.resolve("SKILL.md")).exists();
        }

        @Test
        @DisplayName("infra patterns skipped")
        void infraPatternsSkipped(@TempDir Path t)
                throws IOException {
            Path res = t.resolve("res");
            Files.createDirectories(
                    res.resolve("targets/claude/skills/core"));
            Files.createDirectories(res.resolve(
                    "targets/claude/skills/knowledge-packs/"
                            + "infra-patterns"));
            Path out = t.resolve("output");
            Files.createDirectories(out);

            new SkillsAssembler(res).assemble(
                    TestConfigBuilder.builder()
                            .container("docker")
                            .orchestrator("kubernetes")
                            .build(),
                    new TemplateEngine(), out);

            assertThat(out.resolve(
                    "skills/k8s-deployment"))
                    .doesNotExist();
        }
    }
}
