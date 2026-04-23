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
// imports used: TestConfigBuilder, ProjectConfig, TemplateEngine, List

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("KnowledgeAssembler")
class KnowledgeAssemblerTest {

    private final KnowledgeAssembler assembler =
            new KnowledgeAssembler();

    @Nested
    @DisplayName("assemble — degenerate case")
    class DegenerateCase {

        @Test
        @DisplayName("assemble_whenSourceDirEmpty_targeDirExistsAndIsEmpty")
        void assemble_whenSourceDirEmpty_targetDirExistsAndIsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path sourceDir = tempDir.resolve("source");
            Path targetDir = tempDir.resolve("target");
            Files.createDirectories(sourceDir);

            assembler.assemble(sourceDir, targetDir);

            assertThat(targetDir).exists();
            assertThat(targetDir).isEmptyDirectory();
        }
    }

    @Nested
    @DisplayName("assemble — happy path")
    class HappyPath {

        @Test
        @DisplayName("assemble_whenFileWithNoFrontmatter_copiedVerbatim")
        void assemble_whenFileWithNoFrontmatter_copiedVerbatim(
                @TempDir Path tempDir) throws IOException {
            Path sourceDir = tempDir.resolve("source");
            Path targetDir = tempDir.resolve("target");
            Files.createDirectories(sourceDir);
            String content = "# CQRS\nDetails here.\n";
            Files.writeString(
                    sourceDir.resolve("patterns.md"),
                    content,
                    StandardCharsets.UTF_8);

            assembler.assemble(sourceDir, targetDir);

            Path copied = targetDir.resolve("patterns.md");
            assertThat(copied).exists();
            assertThat(Files.readString(copied, StandardCharsets.UTF_8))
                    .isEqualTo(content);
        }

        @Test
        @DisplayName("assemble_whenFileWithValidFrontmatter_copiedVerbatim")
        void assemble_whenFileWithValidFrontmatter_copiedVerbatim(
                @TempDir Path tempDir) throws IOException {
            Path sourceDir = tempDir.resolve("source");
            Path targetDir = tempDir.resolve("target");
            Files.createDirectories(sourceDir);
            String content = """
                    ---
                    name: architecture
                    description: Full architecture reference.
                    tags: [hexagonal, solid]
                    ---

                    # Architecture
                    """;
            Files.writeString(
                    sourceDir.resolve("architecture.md"),
                    content,
                    StandardCharsets.UTF_8);

            assembler.assemble(sourceDir, targetDir);

            Path copied = targetDir.resolve("architecture.md");
            assertThat(copied).exists();
            assertThat(Files.readString(copied, StandardCharsets.UTF_8))
                    .isEqualTo(content);
        }

        @Test
        @DisplayName("assemble_whenSubdirectory_structurePreserved")
        void assemble_whenSubdirectory_structurePreserved(
                @TempDir Path tempDir) throws IOException {
            Path sourceDir = tempDir.resolve("source");
            Path subDir = sourceDir.resolve("patterns-outbox");
            Path targetDir = tempDir.resolve("target");
            Files.createDirectories(subDir);
            Files.writeString(
                    subDir.resolve("index.md"),
                    "# Outbox\n",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    subDir.resolve("transactional.md"),
                    "# Transactional\n",
                    StandardCharsets.UTF_8);

            assembler.assemble(sourceDir, targetDir);

            assertThat(targetDir.resolve("patterns-outbox/index.md"))
                    .exists();
            assertThat(targetDir.resolve("patterns-outbox/transactional.md"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("assemble — error paths")
    class ErrorPaths {

        @Test
        @DisplayName("assemble_whenFrontmatterHasUserInvocable_throwsIllegalStateException")
        void assemble_whenFrontmatterHasUserInvocable_throwsIllegalStateException(
                @TempDir Path tempDir) throws IOException {
            Path sourceDir = tempDir.resolve("source");
            Path targetDir = tempDir.resolve("target");
            Files.createDirectories(sourceDir);
            Files.writeString(
                    sourceDir.resolve("bad.md"),
                    "---\nname: bad\nuser-invocable: false\n---\n# Bad\n",
                    StandardCharsets.UTF_8);

            assertThatThrownBy(
                    () -> assembler.assemble(sourceDir, targetDir))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("bad.md")
                    .hasMessageContaining("user-invocable");
        }

        @Test
        @DisplayName("assemble_whenFrontmatterHasAllowedTools_throwsIllegalStateException")
        void assemble_whenFrontmatterHasAllowedTools_throwsIllegalStateException(
                @TempDir Path tempDir) throws IOException {
            Path sourceDir = tempDir.resolve("source");
            Path targetDir = tempDir.resolve("target");
            Files.createDirectories(sourceDir);
            Files.writeString(
                    sourceDir.resolve("bad.md"),
                    "---\nname: bad\nallowed-tools: [Read]\n---\n# Bad\n",
                    StandardCharsets.UTF_8);

            assertThatThrownBy(
                    () -> assembler.assemble(sourceDir, targetDir))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("bad.md")
                    .hasMessageContaining("allowed-tools");
        }

        @Test
        @DisplayName("assemble_whenFrontmatterHasArgumentHint_throwsIllegalStateException")
        void assemble_whenFrontmatterHasArgumentHint_throwsIllegalStateException(
                @TempDir Path tempDir) throws IOException {
            Path sourceDir = tempDir.resolve("source");
            Path targetDir = tempDir.resolve("target");
            Files.createDirectories(sourceDir);
            Files.writeString(
                    sourceDir.resolve("bad.md"),
                    "---\nname: bad\nargument-hint: foo\n---\n# Bad\n",
                    StandardCharsets.UTF_8);

            assertThatThrownBy(
                    () -> assembler.assemble(sourceDir, targetDir))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("argument-hint");
        }

        @Test
        @DisplayName("assemble_whenNonMdFileInSource_throwsIllegalStateException")
        void assemble_whenNonMdFileInSource_throwsIllegalStateException(
                @TempDir Path tempDir) throws IOException {
            Path sourceDir = tempDir.resolve("source");
            Path targetDir = tempDir.resolve("target");
            Files.createDirectories(sourceDir);
            Files.write(
                    sourceDir.resolve("binary.png"),
                    new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47});

            assertThatThrownBy(
                    () -> assembler.assemble(sourceDir, targetDir))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("binary.png");
        }
    }

    @Nested
    @DisplayName("assemble — Assembler interface")
    class AssemblerInterface {

        @Test
        @DisplayName("assemble_whenSourceDirAbsent_returnsEmptyList")
        void assemble_whenSourceDirAbsent_returnsEmptyList(
                @TempDir Path tempDir) {
            Path nonExistent = tempDir.resolve("does-not-exist");
            KnowledgeAssembler ka =
                    new KnowledgeAssembler(nonExistent);
            ProjectConfig config = TestConfigBuilder.minimal();
            Path outputDir = tempDir.resolve("output");

            List<String> result = ka.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("assemble_whenSourceHasFiles_returnsGeneratedPaths")
        void assemble_whenSourceHasFiles_returnsGeneratedPaths(
                @TempDir Path tempDir) throws IOException {
            Path sourceDir = tempDir.resolve("src");
            Files.createDirectories(sourceDir);
            Files.writeString(
                    sourceDir.resolve("test.md"),
                    "# Test\n",
                    StandardCharsets.UTF_8);
            KnowledgeAssembler ka =
                    new KnowledgeAssembler(sourceDir);
            ProjectConfig config = TestConfigBuilder.minimal();
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            List<String> result = ka.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(result).hasSize(1);
            assertThat(result.get(0))
                    .endsWith("test.md");
        }
    }

    @Nested
    @DisplayName("assemble — edge cases")
    class EdgeCases {

        @Test
        @DisplayName("assemble_whenDotfileInSource_dotfileSkipped")
        void assemble_whenDotfileInSource_dotfileSkipped(
                @TempDir Path tempDir) throws IOException {
            Path sourceDir = tempDir.resolve("source");
            Path targetDir = tempDir.resolve("target");
            Files.createDirectories(sourceDir);
            Files.writeString(
                    sourceDir.resolve("valid.md"),
                    "# Valid\n",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    sourceDir.resolve(".gitkeep"),
                    "",
                    StandardCharsets.UTF_8);

            assembler.assemble(sourceDir, targetDir);

            assertThat(targetDir.resolve("valid.md")).exists();
            assertThat(targetDir.resolve(".gitkeep")).doesNotExist();
        }

        @Test
        @DisplayName("assemble_whenFrontmatterHasNoClosingDelimiter_fileAccepted")
        void assemble_whenFrontmatterHasNoClosingDelimiter_fileAccepted(
                @TempDir Path tempDir) throws IOException {
            Path sourceDir = tempDir.resolve("source");
            Path targetDir = tempDir.resolve("target");
            Files.createDirectories(sourceDir);
            String content = "---\nname: no-close\n# Content\n";
            Files.writeString(
                    sourceDir.resolve("noclose.md"),
                    content,
                    StandardCharsets.UTF_8);

            assembler.assemble(sourceDir, targetDir);

            assertThat(targetDir.resolve("noclose.md")).exists();
        }
    }
}
