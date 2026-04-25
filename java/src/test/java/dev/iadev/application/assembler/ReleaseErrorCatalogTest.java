package dev.iadev.application.assembler;

import dev.iadev.testutil.SkillContentReader;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for story-0035-0007: Consolidated Error
 * Handling table with 25+ entries organized by phase.
 *
 * <p>Validates that the Error Handling section in the
 * x-release SKILL.md has the correct structure, count,
 * uniqueness, and format.</p>
 */
@DisplayName("x-release Error Catalog (story-0035-0007)")
class ReleaseErrorCatalogTest {

    private static final Pattern TABLE_ROW = Pattern
            .compile("^\\| \\d+");

    private static final Pattern ERROR_CODE = Pattern
            .compile(
                    "`([A-Z][A-Z0-9_]+)`");

    @Nested
    @DisplayName("Error Catalog — Entry Count")
    class EntryCount {

        @Test
        @DisplayName("error catalog has at least 25"
                + " entries")
        void errorCatalog_hasAtLeast25Entries(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            List<String> rows =
                    extractErrorTableRows(content);
            assertThat(rows)
                    .as("Error catalog must have >= 25"
                            + " entries")
                    .hasSizeGreaterThanOrEqualTo(25);
        }
    }

    @Nested
    @DisplayName("Error Catalog — Column Structure")
    class ColumnStructure {

        @Test
        @DisplayName("error table has 5 columns:"
                + " Phase, Code, Condition, Message,"
                + " Exit")
        void errorCatalog_hasFiveColumns(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("| Phase |")
                    .contains("Error Code")
                    .contains("Condition")
                    .contains("Message")
                    .contains("| Exit |");
        }

        @Test
        @DisplayName("each error row has exactly 5"
                + " pipe-separated columns")
        void errorCatalog_eachRowHasFiveColumns(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            List<String> rows =
                    extractErrorTableRows(content);
            for (String row : rows) {
                long pipeCount = row.chars()
                        .filter(c -> c == '|')
                        .count();
                assertThat(pipeCount)
                        .as("Row should have 6 pipes"
                                + " (5 columns): %s", row)
                        .isEqualTo(6);
            }
        }
    }

    @Nested
    @DisplayName("Error Catalog — Code Uniqueness")
    class CodeUniqueness {

        @Test
        @DisplayName("no duplicate error codes in"
                + " catalog")
        void errorCatalog_noDuplicateCodes(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            List<String> codes =
                    extractErrorCodes(content);
            Set<String> unique = new HashSet<>(codes);
            assertThat(unique)
                    .as("All error codes must be unique")
                    .hasSameSizeAs(codes);
        }
    }

    @Nested
    @DisplayName("Error Catalog — Code Format")
    class CodeFormat {

        @Test
        @DisplayName("all error codes are"
                + " UPPER_SNAKE_CASE")
        void errorCatalog_codesAreUpperSnakeCase(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            List<String> codes =
                    extractErrorCodes(content);
            for (String code : codes) {
                assertThat(code)
                        .as("Code must be"
                                + " UPPER_SNAKE_CASE:"
                                + " %s", code)
                        .matches(
                                "[A-Z][A-Z0-9_]+");
            }
        }
    }

    @Nested
    @DisplayName("Error Catalog — Known Error Codes")
    class KnownErrorCodes {

        @Test
        @DisplayName("catalog includes Phase 0 error"
                + " codes from story-0001")
        void errorCatalog_includesPhase0Codes(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("DEP_GH_MISSING")
                    .contains("DEP_JQ_MISSING")
                    .contains("DEP_GH_AUTH")
                    .contains("STATE_INVALID_JSON")
                    .contains("STATE_SCHEMA_VERSION")
                    .contains("RESUME_NO_STATE")
                    .contains("STATE_CONFLICT");
        }

        @Test
        @DisplayName("catalog includes Phase 2 VALIDATE"
                + " codes from story-0002")
        void errorCatalog_includesValidateCodes(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("VALIDATE_DIRTY_WORKDIR")
                    .contains("VALIDATE_WRONG_BRANCH")
                    .contains("VALIDATE_EMPTY_UNRELEASED")
                    .contains("VALIDATE_BUILD_FAILED")
                    .contains("VALIDATE_COVERAGE_LINE")
                    .contains("VALIDATE_COVERAGE_BRANCH")
                    .contains("VALIDATE_GOLDEN_DRIFT")
                    .contains(
                            "VALIDATE_HARDCODED_VERSION");
        }

        @Test
        @DisplayName("catalog includes Phase 7 PR codes"
                + " from story-0003")
        void errorCatalog_includesPrCodes(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("PR_PUSH_REJECTED")
                    .contains("PR_NO_CHANGELOG_ENTRY")
                    .contains("PR_CREATE_FAILED");
        }

        @Test
        @DisplayName("catalog includes Phase 8 APPROVAL"
                + " codes from story-0004")
        void errorCatalog_includesApprovalCodes(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("APPROVAL_PR_STILL_OPEN")
                    .contains("APPROVAL_CANCELLED");
        }

        @Test
        @DisplayName("catalog includes Phase 9 RESUME"
                + " codes from story-0005")
        void errorCatalog_includesResumeCodes(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("RESUME_PR_NOT_MERGED")
                    .contains("RESUME_TAG_LOCAL_EXISTS")
                    .contains(
                            "RESUME_TAG_REMOTE_EXISTS");
        }

        @Test
        @DisplayName("catalog includes Phase 10"
                + " BACKMERGE codes from story-0006")
        void errorCatalog_includesBackmergeCodes(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("BACKMERGE_WRONG_PHASE")
                    .contains("BACKMERGE_UNEXPECTED");
        }

        @Test
        @DisplayName("catalog includes HOTFIX_INVALID"
                + "_BUMP from story-0007")
        void errorCatalog_includesHotfixInvalidBump(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("HOTFIX_INVALID_BUMP");
        }
    }

    @Nested
    @DisplayName("Error Catalog — Phase Coverage")
    class PhaseCoverage {

        @Test
        @DisplayName("catalog covers phases 0, 1, 2,"
                + " 7, 8, 9, 10")
        void errorCatalog_coversExpectedPhases(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            List<String> rows =
                    extractErrorTableRows(content);
            Set<String> phases = rows.stream()
                    .map(r -> r.split("\\|")[1].trim())
                    .collect(Collectors.toSet());
            assertThat(phases)
                    .contains("0", "1", "2", "7",
                            "8", "9", "10");
        }
    }

    private Path generateOutput(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler =
                new SkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), outputDir);
        return outputDir;
    }

    private String generateClaudeContent(Path tempDir)
            throws IOException {
        Path outputDir = generateOutput(tempDir);
        return SkillContentReader.readSkillWithReferences(
                outputDir, "x-release");
    }

    private List<String> extractErrorTableRows(
            String content) {
        String errorSection = extractErrorSection(
                content);
        return errorSection.lines()
                .filter(l -> TABLE_ROW.matcher(l).find())
                .collect(Collectors.toList());
    }

    private List<String> extractErrorCodes(
            String content) {
        String errorSection = extractErrorSection(
                content);
        return errorSection.lines()
                .filter(l -> TABLE_ROW.matcher(l).find())
                .map(l -> {
                    Matcher m = ERROR_CODE.matcher(l);
                    return m.find() ? m.group(1) : "";
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String extractErrorSection(String content) {
        int start = content.indexOf(
                "## Consolidated Error Catalog");
        if (start == -1) {
            start = content.indexOf(
                    "## Error Handling");
        }
        if (start == -1) {
            return "";
        }
        int end = content.indexOf("\n## ", start + 1);
        if (end == -1) {
            end = content.length();
        }
        return content.substring(start, end);
    }

}
