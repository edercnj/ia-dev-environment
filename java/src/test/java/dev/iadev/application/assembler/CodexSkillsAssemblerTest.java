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

/**
 * Tests for CodexSkillsAssembler — copies .claude/skills/
 * to .agents/skills/ and .codex/skills/ for OpenAI Codex CLI.
 */
@DisplayName("CodexSkillsAssembler")
class CodexSkillsAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            assertThat(new CodexSkillsAssembler())
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("copySkillsTree")
    class CopySkillsTree {

        @Test
        @DisplayName("copies SKILL.md files")
        void assemble_whenCalled_copiesSkillMd(
                @TempDir Path tempDir) throws IOException {
            Path srcDir = tempDir.resolve("src-skills");
            Path skill = srcDir.resolve("my-skill");
            Files.createDirectories(skill);
            Files.writeString(
                    skill.resolve("SKILL.md"),
                    "---\nname: my-skill\n---\nContent",
                    StandardCharsets.UTF_8);

            Path destDir = tempDir.resolve("dest-skills");
            List<String> files =
                    CodexSkillsAssembler.copySkillsTree(
                            srcDir, destDir);

            assertThat(files).hasSize(1);
            assertThat(destDir.resolve(
                    "my-skill/SKILL.md")).exists();
        }

        @Test
        @DisplayName("copies references directory")
        void assemble_whenCalled_copiesReferences(
                @TempDir Path tempDir) throws IOException {
            Path srcDir = tempDir.resolve("src-skills");
            Path skill = srcDir.resolve("patterns");
            Files.createDirectories(skill);
            Files.writeString(
                    skill.resolve("SKILL.md"),
                    "---\nname: patterns\n---\n",
                    StandardCharsets.UTF_8);
            Path refs = skill.resolve("references");
            Files.createDirectories(refs);
            Files.writeString(
                    refs.resolve("pattern1.md"),
                    "Pattern content",
                    StandardCharsets.UTF_8);

            Path destDir = tempDir.resolve("dest-skills");
            List<String> files =
                    CodexSkillsAssembler.copySkillsTree(
                            srcDir, destDir);

            assertThat(files).hasSize(2);
            assertThat(destDir.resolve(
                    "patterns/references/pattern1.md"))
                    .exists();
        }

        @Test
        @DisplayName("recurses into subdirectories")
        void assemble_whenCalled_recursesIntoSubdirs(
                @TempDir Path tempDir) throws IOException {
            Path srcDir = tempDir.resolve("src-skills");
            Path nested =
                    srcDir.resolve("lib/x-lib-task");
            Files.createDirectories(nested);
            Files.writeString(
                    nested.resolve("SKILL.md"),
                    "---\nname: x-lib-task\n---\n",
                    StandardCharsets.UTF_8);

            Path destDir = tempDir.resolve("dest-skills");
            List<String> files =
                    CodexSkillsAssembler.copySkillsTree(
                            srcDir, destDir);

            assertThat(files).hasSize(1);
            assertThat(destDir.resolve(
                    "lib/x-lib-task/SKILL.md")).exists();
        }

        @Test
        @DisplayName("returns empty for empty source dir")
        void assemble_forEmpty_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path srcDir = tempDir.resolve("src-skills");
            Files.createDirectories(srcDir);

            Path destDir = tempDir.resolve("dest-skills");
            List<String> files =
                    CodexSkillsAssembler.copySkillsTree(
                            srcDir, destDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("sorts directories alphabetically")
        void assemble_whenCalled_sortedOutput(
                @TempDir Path tempDir) throws IOException {
            Path srcDir = tempDir.resolve("src-skills");
            Path skillZ = srcDir.resolve("z-skill");
            Path skillA = srcDir.resolve("a-skill");
            Files.createDirectories(skillZ);
            Files.createDirectories(skillA);
            Files.writeString(
                    skillZ.resolve("SKILL.md"),
                    "Z content", StandardCharsets.UTF_8);
            Files.writeString(
                    skillA.resolve("SKILL.md"),
                    "A content", StandardCharsets.UTF_8);

            Path destDir = tempDir.resolve("dest-skills");
            List<String> files =
                    CodexSkillsAssembler.copySkillsTree(
                            srcDir, destDir);

            assertThat(files).hasSize(2);
            assertThat(files.get(0))
                    .contains("a-skill");
            assertThat(files.get(1))
                    .contains("z-skill");
        }
    }

    @Nested
    @DisplayName("collectFiles")
    class CollectFiles {

        @Test
        @DisplayName("collects all files recursively")
        void assemble_whenCalled_collectsRecursively(
                @TempDir Path tempDir) throws IOException {
            Path dir = tempDir.resolve("refs");
            Path subDir = dir.resolve("sub");
            Files.createDirectories(subDir);
            Files.writeString(
                    dir.resolve("a.md"),
                    "A", StandardCharsets.UTF_8);
            Files.writeString(
                    subDir.resolve("b.md"),
                    "B", StandardCharsets.UTF_8);

            List<String> files =
                    CodexSkillsAssembler.collectFiles(dir);

            assertThat(files).hasSize(2);
        }

        @Test
        @DisplayName("returns empty for empty directory")
        void assemble_forEmpty_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path dir = tempDir.resolve("empty");
            Files.createDirectories(dir);

            List<String> files =
                    CodexSkillsAssembler.collectFiles(dir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("assemble — copies skills trees")
    class Assemble {

        @Test
        @DisplayName("copies skills from .claude/ to"
                + " .agents/ and .codex/")
        void assemble_whenCalled_copiesSkillsFromClaude(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = setupDirs(tempDir);

            CodexSkillsAssembler assembler =
                    new CodexSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
            assertThat(files).hasSize(2);
            assertThat(outputDir.resolve(
                    "skills/my-skill/SKILL.md")).exists();
            assertThat(outputDir.getParent().resolve(".codex")
                    .resolve("skills/my-skill/SKILL.md"))
                    .exists();
        }

        @Test
        @DisplayName("returns empty when .claude/skills/"
                + " is missing")
        void assemble_whenCalled_returnsEmptyWhenMissing(
                @TempDir Path tempDir) throws IOException {
            Path outputDir =
                    tempDir.resolve("out")
                            .resolve(".agents");
            Files.createDirectories(outputDir);
            // No .claude dir

            CodexSkillsAssembler assembler =
                    new CodexSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("preserves references directory")
        void assemble_whenCalled_preservesReferences(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = setupDirs(tempDir);

            // Add references to the skill
            Path refsDir = outputDir.getParent()
                    .resolve(".claude")
                    .resolve("skills")
                    .resolve("my-skill")
                    .resolve("references");
            Files.createDirectories(refsDir);
            Files.writeString(
                    refsDir.resolve("ref.md"),
                    "Reference content",
                    StandardCharsets.UTF_8);

            CodexSkillsAssembler assembler =
                    new CodexSkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "skills/my-skill/references/ref.md"))
                    .exists();
            assertThat(outputDir.getParent().resolve(".codex")
                    .resolve("skills/my-skill/references/ref.md"))
                    .exists();
        }

        private Path setupDirs(Path tempDir)
                throws IOException {
            Path outputDir =
                    tempDir.resolve("out")
                            .resolve(".agents");
            Files.createDirectories(outputDir);

            Path claudeSkillsDir = outputDir.getParent()
                    .resolve(".claude")
                    .resolve("skills");
            Path skill =
                    claudeSkillsDir.resolve("my-skill");
            Files.createDirectories(skill);
            Files.writeString(
                    skill.resolve("SKILL.md"),
                    "---\nname: my-skill\n---\nContent",
                    StandardCharsets.UTF_8);

            return outputDir;
        }
    }
}
