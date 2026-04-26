package dev.iadev.epic0058;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for EPIC-0058, Story 0058-0003: audit-flow-version.sh.
 *
 * <p>Validates script presence, executability, syntax, and --self-check exit 0.</p>
 */
@DisplayName("Epic0058FlowVersionAuditSmokeTest — audit-flow-version.sh")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Bash script tests require POSIX environment")
class Epic0058FlowVersionAuditSmokeTest {

    private static final Path REPO_ROOT = Paths.get("..").toAbsolutePath().normalize();
    private static final Path SCRIPT_PATH =
            REPO_ROOT.resolve("scripts/audit-flow-version.sh");
    private static final Path FIXTURES_DIR =
            REPO_ROOT.resolve("scripts/fixtures/audit-flow-version");

    @Test
    @DisplayName("audit-flow-version.sh exists in scripts/")
    void script_fileExists() {
        assertThat(SCRIPT_PATH)
                .as("audit-flow-version.sh must exist at %s", SCRIPT_PATH)
                .exists()
                .isRegularFile();
    }

    @Test
    @DisplayName("audit-flow-version.sh is executable")
    void script_isExecutable() throws IOException {
        assertThat(SCRIPT_PATH).exists();
        Set<PosixFilePermission> perms =
                Files.getPosixFilePermissions(SCRIPT_PATH);
        assertThat(perms)
                .as("Script must have owner execute permission")
                .contains(PosixFilePermission.OWNER_EXECUTE);
    }

    @Test
    @DisplayName("audit-flow-version.sh --self-check exits 0")
    void script_selfCheckPasses() throws IOException, InterruptedException {
        Process proc = new ProcessBuilder("bash", SCRIPT_PATH.toString(), "--self-check")
                .directory(REPO_ROOT.toFile())
                .redirectErrorStream(true)
                .start();
        int exitCode = proc.waitFor();
        assertThat(exitCode)
                .as("--self-check must exit 0 (script integrity valid)")
                .isZero();
    }

    @Test
    @DisplayName("Fixture directory exists with 4 JSON files and README")
    void fixtures_directoryHasRequiredFiles() {
        assertThat(FIXTURES_DIR).exists().isDirectory();

        assertThat(FIXTURES_DIR.resolve("valid-v1.json")).exists();
        assertThat(FIXTURES_DIR.resolve("valid-v2.json")).exists();
        assertThat(FIXTURES_DIR.resolve("missing.json")).exists();
        assertThat(FIXTURES_DIR.resolve("invalid.json")).exists();
        assertThat(FIXTURES_DIR.resolve("README.md")).exists();
    }

    @Test
    @DisplayName("valid-v2.json fixture has flowVersion 2")
    void fixture_validV2HasCorrectFlowVersion() throws IOException {
        String content = Files.readString(FIXTURES_DIR.resolve("valid-v2.json"));
        assertThat(content).contains("\"flowVersion\":\"2\"");
    }

    @Test
    @DisplayName("invalid.json fixture has flowVersion 3 (invalid)")
    void fixture_invalidHasInvalidFlowVersion() throws IOException {
        String content = Files.readString(FIXTURES_DIR.resolve("invalid.json"));
        assertThat(content).contains("\"flowVersion\":\"3\"");
    }

    @Test
    @DisplayName("missing.json fixture has no flowVersion field")
    void fixture_missingHasNoFlowVersionField() throws IOException {
        String content = Files.readString(FIXTURES_DIR.resolve("missing.json"));
        assertThat(content).doesNotContain("flowVersion");
    }
}
