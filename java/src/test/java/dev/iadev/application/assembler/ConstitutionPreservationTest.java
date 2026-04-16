package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;
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
 * Tests for CONSTITUTION.md preservation logic
 * (story-0016-0003).
 *
 * <p>Validates that existing CONSTITUTION.md files are
 * preserved by default during regeneration, and that
 * the overwriteConstitution flag forces regeneration.</p>
 */
@DisplayName("ConstitutionAssembler — preservation")
class ConstitutionPreservationTest {

    private static final String CUSTOM_CONTENT =
            "RULE-CUSTOM-001: regra do usuario";
    private static final String SKIP_MESSAGE =
            "CONSTITUTION.md exists — skipping "
                    + "(use --overwrite-constitution "
                    + "to regenerate)";
    private static final String OVERWRITE_MESSAGE =
            "CONSTITUTION.md overwritten "
                    + "(--overwrite-constitution active)";

    @Nested
    @DisplayName("no existing file — generates normally")
    class NoExistingFile {

        @Test
        @DisplayName("@GK-1: generates CONSTITUTION.md"
                + " when file does not exist")
        void assemble_noExistingFile_generatesFile(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    createAssembler(false);
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            Path constitutionFile =
                    outputDir.resolve("CONSTITUTION.md");
            assertThat(constitutionFile).exists();
        }

        @Test
        @DisplayName("generates without overwrite flag")
        void assemble_noExistingFile_noOverwrite_generates(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    createAssembler(false);
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("no skip warning when file absent")
        void assembleWithResult_noFile_noSkipWarning(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    createAssembler(false);
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            AssemblerResult result =
                    assembler.assembleWithResult(
                            config, engine, outputDir);

            assertThat(result.warnings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("existing file — skip by default")
    class ExistingFileSkipped {

        @Test
        @DisplayName("@GK-2: preserves existing"
                + " CONSTITUTION.md")
        void assemble_existingFile_preservesContent(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            createExistingConstitution(outputDir);
            ConstitutionAssembler assembler =
                    createAssembler(false);
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(
                    config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve("CONSTITUTION.md"));
            assertThat(content)
                    .contains(CUSTOM_CONTENT);
        }

        @Test
        @DisplayName("returns empty file list when"
                + " skipping")
        void assemble_existingFile_returnsEmptyList(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            createExistingConstitution(outputDir);
            ConstitutionAssembler assembler =
                    createAssembler(false);
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("@GK-2: emits skip warning message")
        void assembleWithResult_existingFile_emitsWarning(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            createExistingConstitution(outputDir);
            ConstitutionAssembler assembler =
                    createAssembler(false);
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            AssemblerResult result =
                    assembler.assembleWithResult(
                            config, engine, outputDir);

            assertThat(result.warnings())
                    .containsExactly(SKIP_MESSAGE);
        }
    }

    @Nested
    @DisplayName("overwrite flag — force regeneration")
    class OverwriteFlag {

        @Test
        @DisplayName("@GK-3: overwrites existing file"
                + " with --overwrite-constitution")
        void assemble_overwrite_regeneratesFile(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            createExistingConstitution(outputDir);
            ConstitutionAssembler assembler =
                    createAssembler(true);
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            String content = readFile(
                    outputDir.resolve("CONSTITUTION.md"));
            assertThat(content)
                    .doesNotContain(CUSTOM_CONTENT);
            assertThat(content)
                    .contains("## Invariants");
        }

        @Test
        @DisplayName("@GK-3: emits overwrite warning")
        void assembleWithResult_overwrite_emitsWarning(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            createExistingConstitution(outputDir);
            ConstitutionAssembler assembler =
                    createAssembler(true);
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            AssemblerResult result =
                    assembler.assembleWithResult(
                            config, engine, outputDir);

            assertThat(result.warnings())
                    .containsExactly(OVERWRITE_MESSAGE);
        }
    }

    @Nested
    @DisplayName("compliance none — no preservation check")
    class ComplianceNone {

        @Test
        @DisplayName("@GK-5: compliance none skips"
                + " existence check entirely")
        void assemble_complianceNone_noCheck(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            createExistingConstitution(outputDir);
            ConstitutionAssembler assembler =
                    createAssembler(false);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            AssemblerResult result =
                    assembler.assembleWithResult(
                            config, engine, outputDir);

            assertThat(result.files()).isEmpty();
            assertThat(result.warnings()).isEmpty();
            String content = readFile(
                    outputDir.resolve("CONSTITUTION.md"));
            assertThat(content)
                    .contains(CUSTOM_CONTENT);
        }
    }

    @Nested
    @DisplayName("backward compatibility")
    class BackwardCompatibility {

        @Test
        @DisplayName("default constructor preserves"
                + " original assemble behavior")
        void assemble_defaultConstructor_worksAsOriginal(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler();
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
        }

        @Test
        @DisplayName("path-only constructor preserves"
                + " original behavior")
        void assemble_pathConstructor_worksAsOriginal(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");
            ConstitutionAssembler assembler =
                    new ConstitutionAssembler(
                            resolveResourcesDir());
            ProjectConfig config = buildPciDssConfig();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
        }
    }

    private static ConstitutionAssembler createAssembler(
            boolean overwriteConstitution) {
        return new ConstitutionAssembler(
                resolveResourcesDir(),
                overwriteConstitution);
    }

    private static Path resolveResourcesDir() {
        return dev.iadev.util.ResourceResolver
                .resolveResourceDir("shared")
                .getParent();
    }

    private static void createExistingConstitution(
            Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        Files.writeString(
                outputDir.resolve("CONSTITUTION.md"),
                CUSTOM_CONTENT,
                StandardCharsets.UTF_8);
    }

    private static ProjectConfig buildPciDssConfig() {
        return TestConfigBuilder.builder()
                .projectName("test-project")
                .language("java", "21")
                .framework("spring-boot", "3.2")
                .archStyle("hexagonal")
                .securityFrameworks("pci-dss")
                .build();
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(
                    path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read: " + path, e);
        }
    }
}
