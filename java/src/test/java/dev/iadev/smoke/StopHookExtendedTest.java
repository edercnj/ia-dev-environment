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
 * Integration test for the EPIC-0057 story-0057-0006 extension to
 * the Camada 2 Stop hook ({@code verify-story-completion.sh}).
 *
 * <p>Verifies the hook now flags absent {@code dependency-audit-*}
 * (hard) and emits soft NOTICEs for {@code test-run-*.txt} +
 * {@code threat-model-story-*.md}, while still passing when all hard
 * artefacts are present.</p>
 */
@DisplayName("StopHookExtendedTest — verify-story-completion.sh new artifacts")
@DisabledOnOs(
        value = OS.WINDOWS,
        disabledReason = "POSIX execute bit + bash hook; mirrors smoke gating.")
class StopHookExtendedTest {

    @Test
    @DisplayName("hook source-of-truth carries the EPIC-0057 extension marker")
    void hook_carriesEpic57Marker() throws Exception {
        Path hook = repoRoot()
                .resolve("java/src/main/resources/targets/claude/hooks/"
                        + "verify-story-completion.sh");
        assertThat(hook).exists();

        String body = Files.readString(hook, StandardCharsets.UTF_8);
        assertThat(body)
                .as("hook must reference the EPIC-0057 extension")
                .contains("EPIC-0057")
                .contains("x-pr-watch-ci")
                .contains("x-dependency-audit")
                .contains("Camada 2 soft");
    }

    @Test
    @DisplayName("hook detects missing dependency-audit (hard) artifact")
    void hook_detectsMissingDependencyAudit(@TempDir Path fakeRepo)
            throws Exception {
        // Build a minimal repo on a feat/story-XXXX-YYYY branch with a
        // commit message matching the story-completion regex but no
        // evidence artifacts. The hook must exit 2 and reference the
        // missing dependency-audit file.
        runCommand(fakeRepo, "git", "init", "-q", "-b", "main");
        runCommand(fakeRepo, "git",
                "-c", "user.email=t@t", "-c", "user.name=t",
                "commit", "--allow-empty", "-q", "-m",
                "feat(story-0057-0006): test fixture");
        runCommand(fakeRepo, "git", "checkout", "-q",
                "-b", "feat/story-0057-0006-fixture");

        // Seed minimal telemetry directory + events.ndjson with a
        // recent gh pr create event so the hook treats this as a
        // story-completion turn.
        Path telDir = fakeRepo.resolve("plans/epic-0057/telemetry");
        Files.createDirectories(telDir);
        Files.writeString(telDir.resolve("events.ndjson"),
                "{\"tool_name\":\"Bash\",\"tool_input\":{\"command\":\"gh pr create -t fix\"}}\n",
                StandardCharsets.UTF_8);

        // Seed the four ORIGINAL hard artefacts so only the new
        // dependency-audit absence is the failure cause.
        Path plansDir = fakeRepo.resolve("plans/epic-0057/plans");
        Path reportsDir = fakeRepo.resolve("plans/epic-0057/reports");
        Files.createDirectories(plansDir);
        Files.createDirectories(reportsDir);
        Files.writeString(reportsDir.resolve(
                "verify-envelope-story-0057-0006.json"), "{}");
        Files.writeString(plansDir.resolve(
                "review-story-story-0057-0006.md"), "# review");
        Files.writeString(plansDir.resolve(
                "techlead-review-story-story-0057-0006.md"), "# tl");
        Files.writeString(reportsDir.resolve(
                "story-completion-report-story-0057-0006.md"), "# rep");

        Path hookSrc = repoRoot().resolve(
                "java/src/main/resources/targets/claude/hooks/"
                        + "verify-story-completion.sh");
        Path hookDest = fakeRepo.resolve(
                ".claude/hooks/verify-story-completion.sh");
        Files.createDirectories(hookDest.getParent());
        Files.copy(hookSrc, hookDest);
        hookDest.toFile().setExecutable(true);

        ProcessBuilder pb = new ProcessBuilder(
                "bash", hookDest.toString());
        pb.directory(fakeRepo.toFile());
        pb.redirectErrorStream(false);
        pb.environment().put("CLAUDE_PROJECT_DIR",
                fakeRepo.toString());
        Process p = pb.start();
        p.getOutputStream().write("{}\n".getBytes(StandardCharsets.UTF_8));
        p.getOutputStream().close();
        String stderr = new String(
                p.getErrorStream().readAllBytes(),
                StandardCharsets.UTF_8);
        if (!p.waitFor(15, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new RuntimeException("Hook timeout");
        }

        assertThat(p.exitValue())
                .as("missing dependency-audit must trigger exit 2")
                .isEqualTo(2);
        assertThat(stderr)
                .as("stderr must reference x-dependency-audit")
                .contains("x-dependency-audit")
                .contains("dependency-audit-story-0057-0006.md");
    }

    private int runCommand(Path workdir, String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workdir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getInputStream().readAllBytes();
        if (!p.waitFor(15, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new RuntimeException("Timeout: " + String.join(" ", cmd));
        }
        return p.exitValue();
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java")
                ? cwd.getParent()
                : cwd;
    }
}
