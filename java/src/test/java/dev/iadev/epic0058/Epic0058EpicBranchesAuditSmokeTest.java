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
 * Smoke tests for EPIC-0058, Story 0058-0004: audit-epic-branches.sh.
 */
@DisplayName("Epic0058EpicBranchesAuditSmokeTest — audit-epic-branches.sh")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Bash script tests require POSIX environment")
class Epic0058EpicBranchesAuditSmokeTest {

    private static final Path REPO_ROOT = Paths.get("..").toAbsolutePath().normalize();
    private static final Path SCRIPT_PATH =
            REPO_ROOT.resolve("scripts/audit-epic-branches.sh");

    @Test
    @DisplayName("audit-epic-branches.sh exists in scripts/")
    void script_fileExists() {
        assertThat(SCRIPT_PATH).exists().isRegularFile();
    }

    @Test
    @DisplayName("audit-epic-branches.sh is executable")
    void script_isExecutable() throws IOException {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(SCRIPT_PATH);
        assertThat(perms).contains(PosixFilePermission.OWNER_EXECUTE);
    }

    @Test
    @DisplayName("audit-epic-branches.sh --self-check exits 0")
    void script_selfCheckPasses() throws IOException, InterruptedException {
        Process proc = new ProcessBuilder("bash", SCRIPT_PATH.toString(), "--self-check")
                .directory(REPO_ROOT.toFile())
                .redirectErrorStream(true)
                .start();
        int exitCode = proc.waitFor();
        assertThat(exitCode).as("--self-check must exit 0").isZero();
    }

    @Test
    @DisplayName("audit-epic-branches.sh has Catalogado em reference")
    void script_hasCatalogReference() throws IOException {
        String body = Files.readString(SCRIPT_PATH);
        assertThat(body).contains("Catalogado em");
    }

    @Test
    @DisplayName("audit-epic-branches.sh implements Rule 21 exit code contract")
    void script_implementsExitCodes() throws IOException {
        String body = Files.readString(SCRIPT_PATH);
        assertThat(body).contains("EPIC_BRANCH_VIOLATION");
        assertThat(body).contains("exit 0");
        assertThat(body).contains("exit 1");
        assertThat(body).contains("exit 2");
    }
}
