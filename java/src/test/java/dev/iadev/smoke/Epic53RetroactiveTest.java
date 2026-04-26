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
 * Verifies the EPIC-0057 story-0057-0008 retroactive backfill: both
 * EPIC-0053 stories now satisfy the Camada 3 audit, and the four
 * backfilled aggregator files are in place with the canonical
 * {@code retroactive-backfill} marker.
 */
@DisplayName("Epic53RetroactiveTest — story-0057-0008 retroactive backfill")
@DisabledOnOs(
        value = OS.WINDOWS,
        disabledReason = "Bash audit script + POSIX path resolution.")
class Epic53RetroactiveTest {

    @Test
    @DisplayName("audit script exits 0 for story-0053-0001 after backfill")
    void audit_story0001_exitsZero() throws Exception {
        int exit = runAudit("--story-id", "story-0053-0001");
        assertThat(exit)
                .as("story-0053-0001 must pass Camada 3 after backfill")
                .isZero();
    }

    @Test
    @DisplayName("audit script exits 0 for story-0053-0002 after backfill")
    void audit_story0002_exitsZero() throws Exception {
        int exit = runAudit("--story-id", "story-0053-0002");
        assertThat(exit)
                .as("story-0053-0002 must pass Camada 3 after backfill")
                .isZero();
    }

    @Test
    @DisplayName("4 backfilled aggregator files exist with retroactive marker")
    void backfilledFiles_existWithMarker() throws IOException {
        String[] files = {
            "plans/epic-0053/plans/review-story-story-0053-0001.md",
            "plans/epic-0053/plans/techlead-review-story-story-0053-0001.md",
            "plans/epic-0053/plans/review-story-story-0053-0002.md",
            "plans/epic-0053/plans/techlead-review-story-story-0053-0002.md",
        };
        for (String f : files) {
            Path p = repoRoot().resolve(f);
            assertThat(p)
                    .as("backfilled aggregator must exist: %s", f)
                    .exists();
            String body = Files.readString(p, StandardCharsets.UTF_8);
            assertThat(body)
                    .as("%s must carry the retroactive-backfill marker", f)
                    .contains("retroactive-backfill: EPIC-0057 story-0057-0008");
        }
    }

    @Test
    @DisplayName("decision document records BACKFILL choice with rationale")
    void decisionDoc_recordsBackfillChoice() throws IOException {
        Path doc = repoRoot()
                .resolve("plans/epic-0057/reports/epic-0053-retroactive-decision.md");
        assertThat(doc).exists();

        String body = Files.readString(doc, StandardCharsets.UTF_8);
        assertThat(body)
                .as("decision doc must record BACKFILL for both stories")
                .contains("story-0053-0001")
                .contains("story-0053-0002")
                .contains("BACKFILL");
        assertThat(body.toLowerCase())
                .contains("rationale")
                .contains("baseline immutability");
    }

    @Test
    @DisplayName("baseline file has NO entries for EPIC-0053 (backfill chosen)")
    void baseline_noEpic53Entries() throws IOException {
        Path baseline = repoRoot()
                .resolve("audits/execution-integrity-baseline.txt");
        assertThat(baseline).exists();

        String body = Files.readString(baseline, StandardCharsets.UTF_8);
        long epic53Lines = body.lines()
                .filter(l -> !l.trim().startsWith("#"))
                .filter(l -> l.contains("story-0053-"))
                .count();
        assertThat(epic53Lines)
                .as("baseline must NOT carry EPIC-0053 entries (backfill chosen)")
                .isZero();
    }

    private int runAudit(String... args) throws Exception {
        Path script = repoRoot()
                .resolve("scripts/audit-execution-integrity.sh");
        String[] cmd = new String[args.length + 2];
        cmd[0] = "bash";
        cmd[1] = script.toString();
        System.arraycopy(args, 0, cmd, 2, args.length);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(repoRoot().toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getInputStream().readAllBytes();
        if (!p.waitFor(30, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new RuntimeException("Audit timeout");
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
