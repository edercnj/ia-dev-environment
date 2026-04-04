package dev.iadev.application.assembler;

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
 * Tests for DocsAdrAssembler — generates docs/adr/README.md
 * and _TEMPLATE-ADR.md.
 */
@DisplayName("DocsAdrAssembler")
class DocsAdrAssemblerTest {

    private static final String VALID_TEMPLATE =
            "# ADR Template\n\n"
                    + "## Status\n\nProposed\n\n"
                    + "## Context\n\nContext here\n\n"
                    + "## Decision\n\nDecision here\n\n"
                    + "## Consequences\n\nConsequences\n";

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void implements_whenCalled_isAssemblerInstance() {
            DocsAdrAssembler assembler =
                    new DocsAdrAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — generates ADR artifacts")
    class AssembleAdr {

        @Test
        @DisplayName("generates README.md and"
                + " _TEMPLATE-ADR.md")
        void assemble_whenCalled_generatesTwoFiles(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = setupResources(
                    tempDir, VALID_TEMPLATE);
            Path outputDir = tempDir.resolve("output");

            DocsAdrAssembler assembler =
                    new DocsAdrAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(2);
        }

        @Test
        @DisplayName("README.md contains ADR title heading")
        void assemble_readme_containsTitle(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = setupResources(
                    tempDir, VALID_TEMPLATE);
            Path outputDir = tempDir.resolve("output");

            DocsAdrAssembler assembler =
                    new DocsAdrAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path readme = outputDir.resolve(
                    "docs/adr/README.md");
            String content = readFile(readme);
            assertThat(content).contains(
                    "# Architecture Decision Records");
        }

        @Test
        @DisplayName("README.md contains project name")
        void assemble_readme_containsProjectName(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = setupResources(
                    tempDir, VALID_TEMPLATE);
            Path outputDir = tempDir.resolve("output");

            DocsAdrAssembler assembler =
                    new DocsAdrAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-api")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path readme = outputDir.resolve(
                    "docs/adr/README.md");
            String content = readFile(readme);
            assertThat(content).contains("my-api");
        }

        @Test
        @DisplayName("README.md contains empty ADR table")
        void assemble_readme_containsEmptyTable(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = setupResources(
                    tempDir, VALID_TEMPLATE);
            Path outputDir = tempDir.resolve("output");

            DocsAdrAssembler assembler =
                    new DocsAdrAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path readme = outputDir.resolve(
                    "docs/adr/README.md");
            String content = readFile(readme);
            assertThat(content)
                    .contains("| ID | Title |"
                            + " Status | Date |");
        }

        @Test
        @DisplayName("_TEMPLATE-ADR.md is copied verbatim")
        void assemble_whenCalled_templateCopiedVerbatim(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = setupResources(
                    tempDir, VALID_TEMPLATE);
            Path outputDir = tempDir.resolve("output");

            DocsAdrAssembler assembler =
                    new DocsAdrAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path templateDest = outputDir.resolve(
                    "docs/adr/_TEMPLATE-ADR.md");
            String content = readFile(templateDest);
            assertThat(content)
                    .isEqualTo(VALID_TEMPLATE);
        }

        @Test
        @DisplayName("creates docs/adr/ subdirectory")
        void assemble_whenCalled_createsAdrSubdir(
                @TempDir Path tempDir)
                throws IOException {
            Path resourcesDir = setupResources(
                    tempDir, VALID_TEMPLATE);
            Path outputDir = tempDir.resolve("output");

            DocsAdrAssembler assembler =
                    new DocsAdrAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(
                    outputDir.resolve("docs/adr"))
                    .exists()
                    .isDirectory();
        }
    }

    @Nested
    @DisplayName("assemble — graceful no-op")
    class GracefulNoOp {

        @Test
        @DisplayName("returns empty list when template"
                + " file absent")
        void assemble_whenCalled_returnsEmptyWhenTemplateAbsent(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            DocsAdrAssembler assembler =
                    new DocsAdrAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("returns empty list when template"
                + " lacks mandatory sections")
        void assemble_whenCalled_returnsEmptyWhenSectionsMissing(
                @TempDir Path tempDir)
                throws IOException {
            String incomplete = "# ADR\n\n## Status\n\n"
                    + "## Context\n\n"
                    + "Missing Decision and Consequences";
            Path resourcesDir = setupResources(
                    tempDir, incomplete);
            Path outputDir = tempDir.resolve("output");

            DocsAdrAssembler assembler =
                    new DocsAdrAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("does not create output directory"
                + " when template absent")
        void assemble_whenCalled_doesNotCreateOutputDir(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            DocsAdrAssembler assembler =
                    new DocsAdrAssembler(resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(outputDir).doesNotExist();
        }
    }

    @Nested
    @DisplayName("hasAllMandatorySections")
    class HasAllMandatorySections {

        @Test
        @DisplayName("returns true when all sections"
                + " present")
        void assemble_whenAllPresent_returnsTrue() {
            assertThat(
                    DocsAdrAssembler
                            .hasAllMandatorySections(
                                    VALID_TEMPLATE))
                    .isTrue();
        }

        @Test
        @DisplayName("returns false when Status missing")
        void assemble_whenStatusMissing_returnsFalse() {
            String noStatus = "## Context\n"
                    + "## Decision\n"
                    + "## Consequences\n";
            assertThat(
                    DocsAdrAssembler
                            .hasAllMandatorySections(
                                    noStatus))
                    .isFalse();
        }

        @Test
        @DisplayName("returns false when empty content")
        void assemble_whenEmpty_returnsFalse() {
            assertThat(
                    DocsAdrAssembler
                            .hasAllMandatorySections(""))
                    .isFalse();
        }

        @Test
        @DisplayName("returns false when Decision missing")
        void assemble_whenDecisionMissing_returnsFalse() {
            String noDecision = "## Status\n"
                    + "## Context\n"
                    + "## Consequences\n";
            assertThat(
                    DocsAdrAssembler
                            .hasAllMandatorySections(
                                    noDecision))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("getNextAdrNumber")
    class GetNextAdrNumber {

        @Test
        @DisplayName("returns 1 for non-existent directory")
        void assemble_forMissingDir_returns1(
                @TempDir Path tempDir) {
            Path missing = tempDir.resolve("no-such-dir");

            int next = DocsAdrAssembler
                    .getNextAdrNumber(missing);

            assertThat(next).isEqualTo(1);
        }

        @Test
        @DisplayName("returns 1 for empty directory")
        void assemble_forEmptyDir_returns1(
                @TempDir Path tempDir)
                throws IOException {
            Path adrDir = tempDir.resolve("adr");
            Files.createDirectories(adrDir);

            int next = DocsAdrAssembler
                    .getNextAdrNumber(adrDir);

            assertThat(next).isEqualTo(1);
        }

        @Test
        @DisplayName("returns max+1 when ADR files exist")
        void assemble_whenCalled_returnsMaxPlusOne(
                @TempDir Path tempDir)
                throws IOException {
            Path adrDir = tempDir.resolve("adr");
            Files.createDirectories(adrDir);
            Files.writeString(
                    adrDir.resolve(
                            "ADR-0001-first.md"),
                    "content", StandardCharsets.UTF_8);
            Files.writeString(
                    adrDir.resolve(
                            "ADR-0003-third.md"),
                    "content", StandardCharsets.UTF_8);

            int next = DocsAdrAssembler
                    .getNextAdrNumber(adrDir);

            assertThat(next).isEqualTo(4);
        }

        @Test
        @DisplayName("ignores non-matching files")
        void assemble_whenCalled_ignoresNonMatchingFiles(
                @TempDir Path tempDir)
                throws IOException {
            Path adrDir = tempDir.resolve("adr");
            Files.createDirectories(adrDir);
            Files.writeString(
                    adrDir.resolve("README.md"),
                    "content", StandardCharsets.UTF_8);
            Files.writeString(
                    adrDir.resolve(
                            "_TEMPLATE-ADR.md"),
                    "content", StandardCharsets.UTF_8);

            int next = DocsAdrAssembler
                    .getNextAdrNumber(adrDir);

            assertThat(next).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("formatAdrFilename")
    class FormatAdrFilename {

        @Test
        @DisplayName("formats with zero-padded number")
        void assemble_whenCalled_zeroPaddedNumber() {
            String name = DocsAdrAssembler
                    .formatAdrFilename(1, "My Decision");

            assertThat(name)
                    .isEqualTo(
                            "ADR-0001-my-decision.md");
        }

        @Test
        @DisplayName("handles large numbers")
        void assemble_whenCalled_largeNumber() {
            String name = DocsAdrAssembler
                    .formatAdrFilename(
                            42, "Another Choice");

            assertThat(name)
                    .isEqualTo(
                            "ADR-0042-another-choice.md");
        }

        @Test
        @DisplayName("sanitizes special characters")
        void assemble_whenCalled_sanitizesSpecialChars() {
            String name = DocsAdrAssembler
                    .formatAdrFilename(
                            5, "Use PostgreSQL & Redis!");

            assertThat(name).isEqualTo(
                    "ADR-0005-use-postgresql-redis.md");
        }

        @Test
        @DisplayName("handles empty title as untitled")
        void assemble_whenCalled_emptyTitleBecomesUntitled() {
            String name = DocsAdrAssembler
                    .formatAdrFilename(1, "");

            assertThat(name)
                    .isEqualTo("ADR-0001-untitled.md");
        }

        @Test
        @DisplayName("collapses consecutive hyphens")
        void assemble_whenCalled_collapsesConsecutiveHyphens() {
            String name = DocsAdrAssembler
                    .formatAdrFilename(
                            2, "one---two");

            assertThat(name)
                    .isEqualTo("ADR-0002-one-two.md");
        }
    }

    @Nested
    @DisplayName("buildReadmeContent")
    class BuildReadmeContent {

        @Test
        @DisplayName("starts with ADR title heading")
        void assemble_withTitle_starts() {
            String content =
                    DocsAdrAssembler
                            .buildReadmeContent("proj");

            assertThat(content).startsWith(
                    "# Architecture Decision Records");
        }

        @Test
        @DisplayName("contains project name in"
                + " blockquote")
        void assemble_whenCalled_containsProjectName() {
            String content =
                    DocsAdrAssembler
                            .buildReadmeContent(
                                    "my-service");

            assertThat(content)
                    .contains("**my-service**");
        }

        @Test
        @DisplayName("contains empty ADR table")
        void assemble_whenCalled_containsEmptyTable() {
            String content =
                    DocsAdrAssembler
                            .buildReadmeContent("proj");

            assertThat(content)
                    .contains("| ID | Title |"
                            + " Status | Date |");
            assertThat(content)
                    .contains("|----|-------|"
                            + "--------|------|");
        }

        @Test
        @DisplayName("contains creation instructions")
        void assemble_whenCalled_containsCreationInstructions() {
            String content =
                    DocsAdrAssembler
                            .buildReadmeContent("proj");

            assertThat(content)
                    .contains("## Creating a New ADR");
            assertThat(content)
                    .contains("_TEMPLATE-ADR.md");
        }
    }

    // --- helpers ---

    private static Path setupResources(
            Path tempDir, String templateContent)
            throws IOException {
        Path resourcesDir =
                tempDir.resolve("resources");
        Path templatesDir =
                resourcesDir.resolve("templates");
        Files.createDirectories(templatesDir);
        Files.writeString(
                templatesDir.resolve(
                        "_TEMPLATE-ADR.md"),
                templateContent, StandardCharsets.UTF_8);
        return resourcesDir;
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
