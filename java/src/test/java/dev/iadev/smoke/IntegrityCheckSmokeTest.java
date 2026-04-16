package dev.iadev.smoke;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import dev.iadev.release.integrity.CheckStatus;
import dev.iadev.release.integrity.IntegrityChecker;
import dev.iadev.release.integrity.IntegrityReport;
import dev.iadev.release.integrity.RepoFileReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end smoke test: from repo filesystem fixture → {@link RepoFileReader} →
 * {@link IntegrityChecker} → {@link IntegrityReport}.
 *
 * <p>Validates Story 0039-0003 acceptance criteria §7 (Gherkin) at integration scope.</p>
 */
class IntegrityCheckSmokeTest {

    @Test
    @DisplayName("smoke_cleanRepoFixture_overallPass")
    void smoke_cleanRepoFixture_overallPass(@TempDir Path repo) throws IOException {
        writeRepoFixture(repo, "3.1.0-SNAPSHOT", "3.1.0", changelogWithEntry());

        IntegrityReport report = runIntegrity(repo);

        assertThat(report.overallStatus()).isEqualTo(CheckStatus.PASS);
        assertThat(report.errorCode()).isEmpty();
    }

    @Test
    @DisplayName("smoke_cumulativeDriftFixture_overallFailWithValidateIntegrityDrift")
    void smoke_cumulativeDriftFixture_overallFailWithValidateIntegrityDrift(@TempDir Path repo) throws IOException {
        writeRepoFixture(repo, "3.1.0-SNAPSHOT", "3.0.0", changelogEmpty());

        IntegrityReport report = runIntegrity(repo);

        assertThat(report.overallStatus()).isEqualTo(CheckStatus.FAIL);
        assertThat(report.errorCode()).contains("VALIDATE_INTEGRITY_DRIFT");
    }

    @Test
    @DisplayName("smoke_versionDriftOnly_overallFail")
    void smoke_versionDriftOnly_overallFail(@TempDir Path repo) throws IOException {
        writeRepoFixture(repo, "3.1.0-SNAPSHOT", "3.0.0", changelogWithEntry());

        IntegrityReport report = runIntegrity(repo);

        assertThat(report.overallStatus()).isEqualTo(CheckStatus.FAIL);
    }

    @Test
    @DisplayName("smoke_missingFiles_readerReturnsEmptyMap_runStillReports")
    void smoke_missingFiles_readerReturnsEmptyMap_runStillReports(@TempDir Path repo) {
        // Only CHANGELOG.md written; pom.xml/README.md absent.
        RepoFileReader reader = new RepoFileReader(repo);
        Map<String, String> files = reader.readTexts(List.of("pom.xml", "README.md"));

        IntegrityReport report = IntegrityChecker.run(
                reader.readText("CHANGELOG.md").orElse(null),
                files,
                "");

        // CHANGELOG absent → FAIL; version check PASS (no target); TODOs PASS.
        assertThat(report.overallStatus()).isEqualTo(CheckStatus.FAIL);
    }

    // ----- helpers -----

    private static IntegrityReport runIntegrity(Path repo) {
        RepoFileReader reader = new RepoFileReader(repo);
        Map<String, String> versionedFiles = reader.readTexts(
                List.of("pom.xml", "README.md", "CLAUDE.md"));
        String changelog = reader.readText("CHANGELOG.md").orElse(null);
        return IntegrityChecker.run(changelog, versionedFiles, "");
    }

    private static void writeRepoFixture(Path repo, String pomVersion, String readmeVersion,
                                         String changelogContent) throws IOException {
        Files.writeString(repo.resolve("pom.xml"),
                "<?xml version=\"1.0\"?>\n<project>\n  <version>" + pomVersion + "</version>\n</project>\n",
                StandardCharsets.UTF_8);
        Files.writeString(repo.resolve("README.md"),
                "# Project\n\n![badge](v" + readmeVersion + ")\n",
                StandardCharsets.UTF_8);
        Files.writeString(repo.resolve("CLAUDE.md"),
                "Current: v" + readmeVersion + "\n",
                StandardCharsets.UTF_8);
        Files.writeString(repo.resolve("CHANGELOG.md"), changelogContent, StandardCharsets.UTF_8);
    }

    private static String changelogWithEntry() {
        return """
                # Changelog

                ## [Unreleased]

                ### Added
                - Integrity checker

                ## [3.0.0] - 2025-01-01
                - Initial release
                """;
    }

    private static String changelogEmpty() {
        return """
                # Changelog

                ## [Unreleased]

                ## [3.0.0] - 2025-01-01
                - Initial release
                """;
    }
}
