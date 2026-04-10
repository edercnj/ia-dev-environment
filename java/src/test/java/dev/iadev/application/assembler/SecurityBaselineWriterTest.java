package dev.iadev.application.assembler;

import dev.iadev.domain.model.SecurityConfig;
import dev.iadev.domain.model.SecurityConfig.ScanningConfig;

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
 * Tests for SecurityBaselineWriter — conditional
 * "Automated Verification" section appended to
 * 06-security-baseline.md.
 */
@DisplayName("SecurityBaselineWriter")
class SecurityBaselineWriterTest {

    private static final String BASELINE_CONTENT =
            "# Rule 06 — Security Baseline\n"
                    + "## Secure Defaults\n"
                    + "## Forbidden\n"
                    + "## Defensive Coding\n";

    @Nested
    @DisplayName("appendVerificationSection")
    class AppendVerification {

        @Test
        @DisplayName("no scanning flags — file unchanged")
        void append_noFlags_fileUnchanged(
                @TempDir Path tempDir) throws IOException {
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);
            Path ruleFile = rulesDir.resolve(
                    "06-security-baseline.md");
            Files.writeString(ruleFile, BASELINE_CONTENT,
                    StandardCharsets.UTF_8);

            SecurityConfig config = new SecurityConfig(
                    List.of());

            List<String> result =
                    SecurityBaselineWriter
                            .appendVerificationSection(
                                    config, rulesDir);

            assertThat(result).isEmpty();
            assertThat(Files.readString(ruleFile,
                    StandardCharsets.UTF_8))
                    .isEqualTo(BASELINE_CONTENT);
        }

        @Test
        @DisplayName("sast only — appends SAST mappings")
        void append_sastOnly_appendsSastMappings(
                @TempDir Path tempDir) throws IOException {
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);
            Path ruleFile = rulesDir.resolve(
                    "06-security-baseline.md");
            Files.writeString(ruleFile, BASELINE_CONTENT,
                    StandardCharsets.UTF_8);

            SecurityConfig config = new SecurityConfig(
                    List.of(),
                    new ScanningConfig(
                            true, false, false,
                            false, false));

            List<String> result =
                    SecurityBaselineWriter
                            .appendVerificationSection(
                                    config, rulesDir);

            assertThat(result).hasSize(1);
            String content = Files.readString(ruleFile,
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .startsWith(BASELINE_CONTENT)
                    .contains("## Automated Verification")
                    .contains("x-security-sast")
                    .contains("Input deserialization")
                    .contains("String escaping")
                    .contains("Path operations")
                    .contains("Crypto RNG")
                    .contains("Symlink following")
                    .doesNotContain("x-security-secret-scan")
                    .doesNotContain("x-hardening-eval");
        }

        @Test
        @DisplayName("secretScan only — appends secret"
                + " mappings")
        void append_secretOnly_appendsSecretMappings(
                @TempDir Path tempDir) throws IOException {
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);
            Path ruleFile = rulesDir.resolve(
                    "06-security-baseline.md");
            Files.writeString(ruleFile, BASELINE_CONTENT,
                    StandardCharsets.UTF_8);

            SecurityConfig config = new SecurityConfig(
                    List.of(),
                    new ScanningConfig(
                            false, false, true,
                            false, false));

            List<String> result =
                    SecurityBaselineWriter
                            .appendVerificationSection(
                                    config, rulesDir);

            assertThat(result).hasSize(1);
            String content = Files.readString(ruleFile,
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .startsWith(BASELINE_CONTENT)
                    .contains("## Automated Verification")
                    .contains("x-security-secret-scan")
                    .contains("Hardcoded secrets")
                    .doesNotContain("x-security-sast")
                    .doesNotContain("x-hardening-eval");
        }

        @Test
        @DisplayName("dast only — appends hardening"
                + " mappings")
        void append_dastOnly_appendsDastMappings(
                @TempDir Path tempDir) throws IOException {
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);
            Path ruleFile = rulesDir.resolve(
                    "06-security-baseline.md");
            Files.writeString(ruleFile, BASELINE_CONTENT,
                    StandardCharsets.UTF_8);

            SecurityConfig config = new SecurityConfig(
                    List.of(),
                    new ScanningConfig(
                            false, true, false,
                            false, false));

            List<String> result =
                    SecurityBaselineWriter
                            .appendVerificationSection(
                                    config, rulesDir);

            assertThat(result).hasSize(1);
            String content = Files.readString(ruleFile,
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .startsWith(BASELINE_CONTENT)
                    .contains("## Automated Verification")
                    .contains("x-hardening-eval")
                    .contains("HTTP security headers")
                    .contains("TLS configuration")
                    .doesNotContain("x-security-sast")
                    .doesNotContain("x-security-secret-scan");
        }

        @Test
        @DisplayName("all flags — includes all 10 mappings")
        void append_allFlags_includesAllMappings(
                @TempDir Path tempDir) throws IOException {
            Path rulesDir = tempDir.resolve("rules");
            Files.createDirectories(rulesDir);
            Path ruleFile = rulesDir.resolve(
                    "06-security-baseline.md");
            Files.writeString(ruleFile, BASELINE_CONTENT,
                    StandardCharsets.UTF_8);

            SecurityConfig config = new SecurityConfig(
                    List.of(),
                    new ScanningConfig(
                            true, true, true,
                            false, false));

            List<String> result =
                    SecurityBaselineWriter
                            .appendVerificationSection(
                                    config, rulesDir);

            assertThat(result).hasSize(1);
            String content = Files.readString(ruleFile,
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .startsWith(BASELINE_CONTENT)
                    .contains("## Automated Verification")
                    .contains("x-security-sast")
                    .contains("x-security-secret-scan")
                    .contains("x-hardening-eval");

            long dataRows = content.lines()
                    .filter(l -> l.startsWith("| ")
                            && !l.startsWith("| Requirement")
                            && !l.startsWith("| :---"))
                    .count();
            assertThat(dataRows).isEqualTo(10);
        }

        @Test
        @DisplayName("rule file missing — returns empty")
        void append_fileMissing_returnsEmpty(
                @TempDir Path tempDir) {
            Path rulesDir = tempDir.resolve("rules");

            SecurityConfig config = new SecurityConfig(
                    List.of(),
                    new ScanningConfig(
                            true, true, true,
                            false, false));

            List<String> result =
                    SecurityBaselineWriter
                            .appendVerificationSection(
                                    config, rulesDir);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("buildVerificationSection")
    class BuildSection {

        @Test
        @DisplayName("sast section has table header")
        void build_sast_hasTableHeader() {
            ScanningConfig scanning =
                    new ScanningConfig(
                            true, false, false,
                            false, false);

            String section =
                    SecurityBaselineWriter
                            .buildVerificationSection(
                                    scanning);

            assertThat(section)
                    .contains("## Automated Verification")
                    .contains("| Requirement | Verified By"
                            + " | How to Run |")
                    .contains("| :--- | :--- | :--- |");
        }

        @Test
        @DisplayName("sast rows contain copy-paste"
                + " ready commands")
        void build_sast_hasCopyPasteCommands() {
            ScanningConfig scanning =
                    new ScanningConfig(
                            true, false, false,
                            false, false);

            String section =
                    SecurityBaselineWriter
                            .buildVerificationSection(
                                    scanning);

            assertThat(section)
                    .contains(
                            "`/x-security-sast --scope owasp`");
        }

        @Test
        @DisplayName("secret scan row has valid command")
        void build_secretScan_hasValidCommand() {
            ScanningConfig scanning =
                    new ScanningConfig(
                            false, false, true,
                            false, false);

            String section =
                    SecurityBaselineWriter
                            .buildVerificationSection(
                                    scanning);

            assertThat(section)
                    .contains("`/x-security-secret-scan`");
        }

        @Test
        @DisplayName("dast rows have valid command")
        void build_dast_hasValidCommand() {
            ScanningConfig scanning =
                    new ScanningConfig(
                            false, true, false,
                            false, false);

            String section =
                    SecurityBaselineWriter
                            .buildVerificationSection(
                                    scanning);

            assertThat(section)
                    .contains("`/x-hardening-eval`");
        }

        @Test
        @DisplayName("section starts with generated note")
        void build_any_startsWithGeneratedNote() {
            ScanningConfig scanning =
                    new ScanningConfig(
                            true, false, false,
                            false, false);

            String section =
                    SecurityBaselineWriter
                            .buildVerificationSection(
                                    scanning);

            assertThat(section).contains(
                    "> This section is generated when "
                            + "security scanning skills "
                            + "are enabled.");
        }

        @Test
        @DisplayName("sast produces exactly 7 data rows")
        void build_sastOnly_producesSevenRows() {
            ScanningConfig scanning =
                    new ScanningConfig(
                            true, false, false,
                            false, false);

            String section =
                    SecurityBaselineWriter
                            .buildVerificationSection(
                                    scanning);

            long dataRows = section.lines()
                    .filter(l -> l.startsWith("| ")
                            && !l.startsWith(
                                    "| Requirement")
                            && !l.startsWith("| :---"))
                    .count();
            assertThat(dataRows).isEqualTo(7);
        }

        @Test
        @DisplayName("secretScan produces exactly 1"
                + " data row")
        void build_secretOnly_producesOneRow() {
            ScanningConfig scanning =
                    new ScanningConfig(
                            false, false, true,
                            false, false);

            String section =
                    SecurityBaselineWriter
                            .buildVerificationSection(
                                    scanning);

            long dataRows = section.lines()
                    .filter(l -> l.startsWith("| ")
                            && !l.startsWith(
                                    "| Requirement")
                            && !l.startsWith("| :---"))
                    .count();
            assertThat(dataRows).isEqualTo(1);
        }

        @Test
        @DisplayName("dast produces exactly 2 data rows")
        void build_dastOnly_producesTwoRows() {
            ScanningConfig scanning =
                    new ScanningConfig(
                            false, true, false,
                            false, false);

            String section =
                    SecurityBaselineWriter
                            .buildVerificationSection(
                                    scanning);

            long dataRows = section.lines()
                    .filter(l -> l.startsWith("| ")
                            && !l.startsWith(
                                    "| Requirement")
                            && !l.startsWith("| :---"))
                    .count();
            assertThat(dataRows).isEqualTo(2);
        }
    }
}
