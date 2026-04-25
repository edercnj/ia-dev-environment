package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@code scripts/audit-bypass-flags.sh}
 * (story-0057-0005). Exercises the four exit paths
 * (0 OK / 1 hard violation / 2 usage / 3 soft warnings) plus
 * the {@code --json} envelope shape.
 */
@DisplayName("AuditBypassFlagsTest — Rule 24 §30 + Rule 45 bypass-flag enforcement")
@DisabledOnOs(
        value = OS.WINDOWS,
        disabledReason = "Bash + POSIX execute bit; mirrors sibling smoke gating.")
class AuditBypassFlagsTest {

    @Test
    @DisplayName("clean repo (no bypass flags in happy-path) exits 0")
    void cleanRepo_exitsZero() throws Exception {
        int exit = runScript();
        assertThat(exit)
                .as("default invocation against canonical SKILL.md tree must exit 0")
                .isZero();
    }

    @Test
    @DisplayName("hard bypass flag in happy-path exits 1 (BYPASS_FLAG_VIOLATION)")
    void happyPathHardBypass_exitsOne(@TempDir Path tmp) throws Exception {
        Path skillDir = tmp.resolve("x-fixture-bypass");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"),
                """
                # x-fixture-bypass

                ## Phase 1 — Implement

                Run with --no-ci-watch flag for fast iteration.
                """,
                StandardCharsets.UTF_8);

        int exit = runScript("--skills-root", tmp.toString());
        assertThat(exit)
                .as("happy-path bypass flag must exit 1")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("bypass flag inside ## Recovery is permitted (exit 0)")
    void recoveryContextBypass_isPermitted(@TempDir Path tmp) throws Exception {
        Path skillDir = tmp.resolve("x-fixture-recovery");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"),
                """
                # x-fixture-recovery

                ## Phase 1 — Implement

                Normal execution.

                ## Recovery

                Use --no-ci-watch when CI is broken.
                """,
                StandardCharsets.UTF_8);

        int exit = runScript("--skills-root", tmp.toString());
        assertThat(exit)
                .as("bypass flag inside ## Recovery must NOT trigger violation")
                .isZero();
    }

    @Test
    @DisplayName("soft flag in happy-path exits 3 (SOFT_WARNINGS)")
    void happyPathSoftFlag_exitsThree(@TempDir Path tmp) throws Exception {
        Path skillDir = tmp.resolve("x-fixture-soft");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"),
                """
                # x-fixture-soft

                ## Phase 1 — Implement

                Run with --no-jira to skip Jira sync.
                """,
                StandardCharsets.UTF_8);

        int exit = runScript("--skills-root", tmp.toString());
        assertThat(exit)
                .as("soft flag must exit 3 (SOFT_WARNINGS)")
                .isEqualTo(3);
    }

    @Test
    @DisplayName("--strict promotes soft warnings to exit 1")
    void strictMode_promotesSoftToHard(@TempDir Path tmp) throws Exception {
        Path skillDir = tmp.resolve("x-fixture-strict");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"),
                """
                # x-fixture-strict

                ## Phase 1

                Use --no-jira sometimes.
                """,
                StandardCharsets.UTF_8);

        int exit = runScript("--skills-root", tmp.toString(), "--strict");
        assertThat(exit)
                .as("--strict must promote soft to exit 1")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("--json emits a valid envelope")
    void jsonMode_emitsValidEnvelope(@TempDir Path tmp) throws Exception {
        Path skillDir = tmp.resolve("x-fixture-json");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"),
                """
                # x-fixture-json

                ## Phase 1

                Run with --no-ci-watch.
                """,
                StandardCharsets.UTF_8);

        ProcessOutput result = captureScript(
                "--skills-root", tmp.toString(), "--json");
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout())
                .contains("\"status\":\"BYPASS_FLAG_VIOLATION\"")
                .contains("\"skillsScanned\":1")
                .contains("\"violationsFound\":1")
                .contains("\"flag\":\"--no-ci-watch\"")
                .contains("\"severity\":\"hard\"");
    }

    @Test
    @DisplayName("--help exits 2")
    void help_exitsTwo() throws Exception {
        ProcessOutput result = captureScript("--help");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stderr())
                .contains("audit-bypass-flags.sh")
                .contains("--skills-root")
                .contains("--strict");
    }

    private int runScript(String... args) throws Exception {
        return captureScript(args).exitCode();
    }

    private ProcessOutput captureScript(String... args) throws Exception {
        Path script = repoRoot()
                .resolve("scripts/audit-bypass-flags.sh");
        String[] cmd = new String[args.length + 2];
        cmd[0] = "bash";
        cmd[1] = script.toString();
        System.arraycopy(args, 0, cmd, 2, args.length);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(repoRoot().toFile());
        Process p = pb.start();
        String stdout = new String(
                p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(
                p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!p.waitFor(30, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new RuntimeException("Timeout running script");
        }
        return new ProcessOutput(p.exitValue(), stdout, stderr);
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java")
                ? cwd.getParent()
                : cwd;
    }

    private record ProcessOutput(
            int exitCode, String stdout, String stderr) {
    }
}
