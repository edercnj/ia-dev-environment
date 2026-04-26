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
 * Smoke tests for EPIC-0058, Story 0058-0005: audit-skill-visibility.sh.
 */
@DisplayName("Epic0058SkillVisibilityAuditSmokeTest — audit-skill-visibility.sh")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Bash script tests require POSIX environment")
class Epic0058SkillVisibilityAuditSmokeTest {

    private static final Path REPO_ROOT = Paths.get("..").toAbsolutePath().normalize();
    private static final Path SCRIPT_PATH =
            REPO_ROOT.resolve("scripts/audit-skill-visibility.sh");

    @Test
    @DisplayName("audit-skill-visibility.sh exists in scripts/")
    void script_fileExists() {
        assertThat(SCRIPT_PATH).exists().isRegularFile();
    }

    @Test
    @DisplayName("audit-skill-visibility.sh is executable")
    void script_isExecutable() throws IOException {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(SCRIPT_PATH);
        assertThat(perms).contains(PosixFilePermission.OWNER_EXECUTE);
    }

    @Test
    @DisplayName("audit-skill-visibility.sh --self-check exits 0")
    void script_selfCheckPasses() throws IOException, InterruptedException {
        Process proc = new ProcessBuilder("bash", SCRIPT_PATH.toString(), "--self-check")
                .directory(REPO_ROOT.toFile())
                .redirectErrorStream(true)
                .start();
        int exitCode = proc.waitFor();
        assertThat(exitCode).as("--self-check must exit 0").isZero();
    }

    @Test
    @DisplayName("audit-skill-visibility.sh passes on current repo (no violations)")
    void script_passesOnCurrentRepo() throws IOException, InterruptedException {
        Process proc = new ProcessBuilder("bash", SCRIPT_PATH.toString())
                .directory(REPO_ROOT.toFile())
                .redirectErrorStream(false)
                .start();
        int exitCode = proc.waitFor();
        assertThat(exitCode)
                .as("Scan of current repo must pass (exit 0)")
                .isZero();
    }

    @Test
    @DisplayName("audit-skill-visibility.sh checks ORPHAN_SCRIPT_REFERENCE (Rule 26 RULE-004)")
    void script_checksOrphanScriptReferences() throws IOException {
        String body = Files.readString(SCRIPT_PATH);
        assertThat(body).contains("ORPHAN_SCRIPT_REFERENCE");
    }

    @Test
    @DisplayName("audit-skill-visibility.sh checks x-internal-* visibility convention")
    void script_checksInternalSkillVisibility() throws IOException {
        String body = Files.readString(SCRIPT_PATH);
        assertThat(body).contains("visibility: internal");
        assertThat(body).contains("user-invocable: false");
        assertThat(body).contains("INTERNAL SKILL");
    }
}
