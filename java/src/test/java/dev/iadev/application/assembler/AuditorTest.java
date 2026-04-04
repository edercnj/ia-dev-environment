package dev.iadev.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Auditor -- rules directory auditing with
 * threshold checks.
 */
@DisplayName("Auditor")
class AuditorTest {

    @Nested
    @DisplayName("auditRulesContext — missing directory")
    class MissingDirectory {

        @Test
        @DisplayName("returns empty result when dir"
                + " does not exist")
        void audit_forMissingDir_emptyResult(
                @TempDir Path tempDir) {
            Path rulesDir =
                    tempDir.resolve("nonexistent");

            Auditor.AuditResult result =
                    Auditor.auditRulesContext(rulesDir);

            assertThat(result.totalFiles()).isZero();
            assertThat(result.totalBytes()).isZero();
            assertThat(result.fileSizes()).isEmpty();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("returns empty result when path"
                + " is a file not directory")
        void audit_forFile_emptyResult(@TempDir Path tempDir)
                throws IOException {
            Path file = tempDir.resolve("not-a-dir");
            Files.writeString(file, "x",
                    StandardCharsets.UTF_8);

            Auditor.AuditResult result =
                    Auditor.auditRulesContext(file);

            assertThat(result.totalFiles()).isZero();
            assertThat(result.totalBytes()).isZero();
        }
    }

    @Nested
    @DisplayName("auditRulesContext — within thresholds")
    class WithinThresholds {

        @Test
        @DisplayName("no warnings when file count and"
                + " bytes within limits")
        void auditRulesContext_noWarningsWithinLimits_succeeds(@TempDir Path tempDir)
                throws IOException {
            Path rulesDir =
                    Files.createDirectories(
                            tempDir.resolve("rules"));
            for (int i = 1; i <= 5; i++) {
                Files.writeString(
                        rulesDir.resolve(
                                "0" + i + "-rule.md"),
                        "content-" + i,
                        StandardCharsets.UTF_8);
            }

            Auditor.AuditResult result =
                    Auditor.auditRulesContext(rulesDir);

            assertThat(result.totalFiles())
                    .isEqualTo(5);
            assertThat(result.totalBytes())
                    .isGreaterThan(0);
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("exactly 10 files produces no"
                + " warning")
        void auditRulesContext_whenCalled_exactlyMaxNoWarning(@TempDir Path tempDir)
                throws IOException {
            Path rulesDir =
                    Files.createDirectories(
                            tempDir.resolve("rules"));
            for (int i = 1; i <= 10; i++) {
                Files.writeString(
                        rulesDir.resolve(
                                String.format(
                                        "%02d-rule.md", i)),
                        "content",
                        StandardCharsets.UTF_8);
            }

            Auditor.AuditResult result =
                    Auditor.auditRulesContext(rulesDir);

            assertThat(result.totalFiles())
                    .isEqualTo(10);
            assertThat(result.warnings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("auditRulesContext — thresholds exceeded")
    class ThresholdsExceeded {

        @Test
        @DisplayName("warns when file count exceeds maximum")
        void auditRulesContext_whenCalled_warnsFileCountExceeded(@TempDir Path tempDir)
                throws IOException {
            Path rulesDir =
                    Files.createDirectories(
                            tempDir.resolve("rules"));
            for (int i = 1; i <= 12; i++) {
                Files.writeString(
                        rulesDir.resolve(
                                String.format(
                                        "%02d-rule.md", i)),
                        "content",
                        StandardCharsets.UTF_8);
            }

            Auditor.AuditResult result =
                    Auditor.auditRulesContext(rulesDir);

            assertThat(result.totalFiles())
                    .isEqualTo(12);
            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "12 rule files exceeds"
                                    + " recommended maximum"
                                    + " of 10"));
        }

        @Test
        @DisplayName("warns when total bytes exceeds"
                + " maximum")
        void auditRulesContext_whenCalled_warnsBytesExceeded(@TempDir Path tempDir)
                throws IOException {
            Path rulesDir =
                    Files.createDirectories(
                            tempDir.resolve("rules"));
            // Create files totalling > 50KB
            String largeContent = "x".repeat(26_000);
            Files.writeString(
                    rulesDir.resolve("01-large.md"),
                    largeContent, StandardCharsets.UTF_8);
            Files.writeString(
                    rulesDir.resolve("02-large.md"),
                    largeContent, StandardCharsets.UTF_8);

            Auditor.AuditResult result =
                    Auditor.auditRulesContext(rulesDir);

            assertThat(result.totalBytes())
                    .isGreaterThan(
                            Auditor.MAX_TOTAL_BYTES);
            assertThat(result.warnings())
                    .anyMatch(w -> w.contains(
                            "total rules exceeds"
                                    + " recommended maximum"
                                    + " of 50KB"));
        }

        @Test
        @DisplayName("both warnings when both thresholds"
                + " exceeded")
        void auditRulesContext_whenCalled_bothWarnings(@TempDir Path tempDir)
                throws IOException {
            Path rulesDir =
                    Files.createDirectories(
                            tempDir.resolve("rules"));
            String largeContent = "x".repeat(5_000);
            for (int i = 1; i <= 12; i++) {
                Files.writeString(
                        rulesDir.resolve(
                                String.format(
                                        "%02d-rule.md", i)),
                        largeContent,
                        StandardCharsets.UTF_8);
            }

            Auditor.AuditResult result =
                    Auditor.auditRulesContext(rulesDir);

            assertThat(result.warnings()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("auditRulesContext — file sizes ordering")
    class FileSizesOrdering {

        @Test
        @DisplayName("fileSizes sorted by size descending")
        void auditRulesContext_whenCalled_sortedBySizeDescending(@TempDir Path tempDir)
                throws IOException {
            Path rulesDir =
                    Files.createDirectories(
                            tempDir.resolve("rules"));
            Files.writeString(
                    rulesDir.resolve("01-small.md"),
                    "x", StandardCharsets.UTF_8);
            Files.writeString(
                    rulesDir.resolve("02-large.md"),
                    "x".repeat(1000),
                    StandardCharsets.UTF_8);
            Files.writeString(
                    rulesDir.resolve("03-medium.md"),
                    "x".repeat(500),
                    StandardCharsets.UTF_8);

            Auditor.AuditResult result =
                    Auditor.auditRulesContext(rulesDir);

            assertThat(result.fileSizes())
                    .hasSize(3);
            assertThat(result.fileSizes().get(0).getKey())
                    .isEqualTo("02-large.md");
            assertThat(result.fileSizes().get(1).getKey())
                    .isEqualTo("03-medium.md");
            assertThat(result.fileSizes().get(2).getKey())
                    .isEqualTo("01-small.md");
        }
    }

    @Nested
    @DisplayName("auditRulesContext — non-md files")
    class NonMdFiles {

        @Test
        @DisplayName("ignores non-md files")
        void auditRulesContext_whenCalled_ignoresNonMdFiles(@TempDir Path tempDir)
                throws IOException {
            Path rulesDir =
                    Files.createDirectories(
                            tempDir.resolve("rules"));
            Files.writeString(
                    rulesDir.resolve("01-rule.md"),
                    "content", StandardCharsets.UTF_8);
            Files.writeString(
                    rulesDir.resolve("readme.txt"),
                    "content", StandardCharsets.UTF_8);

            Auditor.AuditResult result =
                    Auditor.auditRulesContext(rulesDir);

            assertThat(result.totalFiles())
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("constants")
    class Constants {

        @Test
        @DisplayName("MAX_FILE_COUNT is 10")
        void audit_whenCalled_maxFileCountIs10() {
            assertThat(Auditor.MAX_FILE_COUNT)
                    .isEqualTo(10);
        }

        @Test
        @DisplayName("MAX_TOTAL_BYTES is 51200 (50KB)")
        void audit_whenCalled_maxTotalBytesIs51200() {
            assertThat(Auditor.MAX_TOTAL_BYTES)
                    .isEqualTo(51_200L);
        }
    }
}
