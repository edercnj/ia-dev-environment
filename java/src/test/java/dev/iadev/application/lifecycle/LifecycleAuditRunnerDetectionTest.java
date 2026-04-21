package dev.iadev.application.lifecycle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the three detection dimensions of
 * {@link LifecycleAuditRunner} against small synthetic
 * SKILL.md fixtures.
 */
@DisplayName("LifecycleAuditRunner — detection dimensions")
class LifecycleAuditRunnerDetectionTest {

    private Path writeSkill(Path dir, String name, String body)
            throws IOException {
        Path skillDir = dir.resolve(name);
        Files.createDirectories(skillDir);
        Path skill = skillDir.resolve("SKILL.md");
        Files.writeString(skill, body);
        return skill;
    }

    @Test
    @DisplayName("clean skill — zero violations")
    void cleanSkill_noViolations(@TempDir Path dir)
            throws IOException {
        String body = """
                # Skill clean
                ## Core Loop
                1. Phase 1 — do X
                2. Phase 2 — do Y
                ## Phase 1
                body
                ## Phase 2
                body
                """;
        writeSkill(dir, "x-clean", body);

        List<Violation> v = new LifecycleAuditRunner().scan(dir);

        assertThat(v).isEmpty();
    }

    @Test
    @DisplayName("orphan phase detected")
    void orphanPhase_detected(@TempDir Path dir)
            throws IOException {
        String body = """
                # Skill orphan
                ## Core Loop
                1. Phase 1 — do X
                ## Phase 1
                body
                ## Section 1.6b
                body that nothing else references
                """;
        writeSkill(dir, "x-orphan", body);

        List<Violation> v = new LifecycleAuditRunner().scan(dir);

        assertThat(v).anyMatch(x -> x.dimension().equals(
                LifecycleAuditRunner.DIM_ORPHAN_PHASE)
                && x.detail().contains("1.6b"));
    }

    @Test
    @DisplayName("write without commit detected")
    void writeWithoutCommit_detected(@TempDir Path dir)
            throws IOException {
        String body = """
                # Skill w
                ## Phase 1
                Write plans/epic-0001/reports/foo.md
                line
                line
                line
                no commit anywhere
                """;
        writeSkill(dir, "x-w", body);

        List<Violation> v = new LifecycleAuditRunner().scan(dir);

        assertThat(v).anyMatch(x -> x.dimension().equals(
                LifecycleAuditRunner.DIM_WRITE_WITHOUT_COMMIT));
    }

    @Test
    @DisplayName("write followed by x-git-commit — no violation")
    void writeWithCommit_ok(@TempDir Path dir)
            throws IOException {
        String body = """
                # Skill wc
                ## Phase 1
                Write plans/epic-0001/reports/foo.md
                Then invoke:
                Skill(skill: "x-git-commit", args: "...")
                """;
        writeSkill(dir, "x-wc", body);

        List<Violation> v = new LifecycleAuditRunner().scan(dir);

        assertThat(v).noneMatch(x -> x.dimension().equals(
                LifecycleAuditRunner.DIM_WRITE_WITHOUT_COMMIT));
    }

    @Test
    @DisplayName("skip flag in Core Loop detected")
    void skipInCoreLoop_detected(@TempDir Path dir)
            throws IOException {
        String body = """
                # Skill s
                ## Core Loop
                1. Do X
                2. Run with --skip-verification to proceed
                """;
        writeSkill(dir, "x-s", body);

        List<Violation> v = new LifecycleAuditRunner().scan(dir);

        assertThat(v).anyMatch(x -> x.dimension().equals(
                LifecycleAuditRunner.DIM_SKIP_IN_HAPPY_PATH));
    }

    @Test
    @DisplayName("skip flag in Recovery section — ignored")
    void skipInRecovery_ignored(@TempDir Path dir)
            throws IOException {
        String body = """
                # Skill r
                ## Core Loop
                1. Do X
                ## Recovery
                If stuck, run --skip-verification
                """;
        writeSkill(dir, "x-r", body);

        List<Violation> v = new LifecycleAuditRunner().scan(dir);

        assertThat(v).noneMatch(x -> x.dimension().equals(
                LifecycleAuditRunner.DIM_SKIP_IN_HAPPY_PATH));
    }

    @Test
    @DisplayName("audit-exempt comment suppresses violation")
    void auditExempt_suppresses(@TempDir Path dir)
            throws IOException {
        String body = """
                # Skill e
                ## Phase 1
                <!-- audit-exempt -->
                Write plans/epic-0001/reports/foo.md
                """;
        writeSkill(dir, "x-e", body);

        List<Violation> v = new LifecycleAuditRunner().scan(dir);

        assertThat(v).noneMatch(x -> x.dimension().equals(
                LifecycleAuditRunner.DIM_WRITE_WITHOUT_COMMIT));
    }

    @Test
    @DisplayName("scan of non-existent directory returns empty")
    void nonExistentRoot_empty(@TempDir Path dir) {
        Path missing = dir.resolve("nope");
        assertThat(new LifecycleAuditRunner().scan(missing))
                .isEmpty();
    }

    @Test
    @DisplayName("performance: 40 SKILL.md scan < 2s")
    void performance_under2s(@TempDir Path dir)
            throws IOException {
        String body = """
                # Skill perf
                ## Core Loop
                1. Phase 1
                ## Phase 1
                body
                """;
        for (int i = 0; i < 40; i++) {
            writeSkill(dir, "x-perf-" + i, body);
        }
        long t0 = System.nanoTime();
        new LifecycleAuditRunner().scan(dir);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertThat(ms).isLessThan(2000);
    }
}
