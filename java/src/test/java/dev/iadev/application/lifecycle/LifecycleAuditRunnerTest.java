package dev.iadev.application.lifecycle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for the Layer-0 skeleton of
 * {@link LifecycleAuditRunner}. Real detection lives in
 * story-0046-0007 and is covered by a separate
 * {@code LifecycleIntegrityAuditTest} — this test locks down
 * the skeleton's stable API so downstream consumers can be
 * wired immediately.
 */
@DisplayName("LifecycleAuditRunner — skeleton contract")
class LifecycleAuditRunnerTest {

    @Test
    @DisplayName("scan returns empty list for a real "
            + "directory with no source files")
    void scan_emptyDirectory_returnsEmptyList(
            @TempDir Path dir) {
        LifecycleAuditRunner runner =
                new LifecycleAuditRunner();

        List<Violation> violations = runner.scan(dir);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("scan is null-safe and returns empty list "
            + "for null root")
    void scan_nullRoot_returnsEmptyList() {
        LifecycleAuditRunner runner =
                new LifecycleAuditRunner();

        List<Violation> violations = runner.scan(null);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Violation record exposes all four fields")
    void violation_recordExposesFields(@TempDir Path dir) {
        Path file = dir.resolve("skill.md");
        Violation v = new Violation(
                LifecycleAuditRunner.DIM_ORPHAN_PHASE, file, 42,
                "phase.start without phase.end");

        assertThat(v.dimension())
                .isEqualTo(LifecycleAuditRunner.DIM_ORPHAN_PHASE);
        assertThat(v.file()).isEqualTo(file);
        assertThat(v.line()).isEqualTo(42);
        assertThat(v.detail())
                .isEqualTo("phase.start without phase.end");
    }
}
