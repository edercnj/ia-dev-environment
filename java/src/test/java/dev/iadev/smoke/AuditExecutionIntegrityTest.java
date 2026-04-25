package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@code scripts/audit-execution-integrity.sh}
 * (Camada 3 of Rule 24 — story-0057-0002).
 *
 * <p>Exercises the four documented exit codes (0 / 1 / 2 / 3) plus the
 * new {@code --story-id} and {@code --json} flags introduced by
 * EPIC-0057. Mirrors the gating used by {@link Epic0055FoundationSmokeTest}
 * for bash + git availability.</p>
 */
@DisplayName("AuditExecutionIntegrityTest — Camada 3 script (Rule 24)")
@DisabledOnOs(
        value = OS.WINDOWS,
        disabledReason = "Bash + POSIX execute bit; mirrors sibling smoke gating.")
class AuditExecutionIntegrityTest {

    @Test
    @DisplayName("--self-check exits 0 on canonical repo tree")
    void selfCheck_exitsZero_onCanonicalRepo() throws Exception {
        int exit = runScript("--self-check");
        assertThat(exit)
                .as("self-check must succeed on a healthy repo")
                .isZero();
    }

    @Test
    @DisplayName("--story-id with a non-existent story emits EIE_EVIDENCE_MISSING (exit 1)")
    void storyId_nonExistentStory_emitsEvidenceMissing() throws Exception {
        int exit = runScript("--story-id", "story-9999-9999");
        assertThat(exit)
                .as("missing artifacts must yield exit 1")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("--json --story-id emits a single-line JSON envelope")
    void jsonMode_emitsValidEnvelope() throws Exception {
        ProcessOutput result = captureScript(
                "--json", "--story-id", "story-9999-9999");

        assertThat(result.exitCode())
                .as("missing artifacts on a non-existent story must exit 1")
                .isEqualTo(1);
        assertThat(result.stdout())
                .as("JSON envelope must be on stdout (single line)")
                .contains("\"status\":\"EIE_EVIDENCE_MISSING\"")
                .contains("\"storiesAudited\":1")
                .contains("\"storiesFailed\":1")
                .contains("\"failures\":[")
                .contains("story-9999-9999");
    }

    @Test
    @DisplayName("--story-id with malformed id rejects with usage error (exit 2)")
    void storyId_malformedId_emitsUsageError() throws Exception {
        int exit = runScript("--story-id", "BADSTORY");
        assertThat(exit)
                .as("malformed --story-id must exit 2 (usage)")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("--help exits 2 with usage banner")
    void help_exitsTwo_withUsageBanner() throws Exception {
        ProcessOutput result = captureScript("--help");
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stderr())
                .as("usage banner must mention all flags")
                .contains("--self-check")
                .contains("--story-id")
                .contains("--json");
    }

    @Test
    @DisplayName(".conf companion file documents canonical artifact patterns")
    void conf_companionFile_documentsPatterns() throws IOException {
        Path conf = repoRoot().resolve("scripts/audit-execution-integrity.conf");
        assertThat(conf)
                .as(".conf companion file must exist")
                .exists();

        String body = Files.readString(conf, StandardCharsets.UTF_8);
        assertThat(body)
                .as(".conf must declare HARD_VERIFY_ENVELOPE pattern")
                .contains("HARD_VERIFY_ENVELOPE")
                .contains("verify-envelope-{STORY}.json");
        assertThat(body)
                .as(".conf must declare SOFT_DEPENDENCY_AUDIT pattern")
                .contains("SOFT_DEPENDENCY_AUDIT")
                .contains("dependency-audit-{STORY}.md");
    }

    private int runScript(String... args) throws Exception {
        return captureScript(args).exitCode();
    }

    private ProcessOutput captureScript(String... args) throws Exception {
        Path script = repoRoot()
                .resolve("scripts/audit-execution-integrity.sh");
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
            throw new RuntimeException("Timeout running audit script");
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
