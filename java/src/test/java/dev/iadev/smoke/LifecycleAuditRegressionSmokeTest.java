package dev.iadev.smoke;

import dev.iadev.application.lifecycle.LifecycleAuditRunner;
import dev.iadev.application.lifecycle.Violation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E smoke test that injects one synthetic regression per
 * audit dimension into a sandbox skills tree and asserts the
 * audit detects all three. Protects against silent drift in
 * {@link LifecycleAuditRunner}'s detection heuristics.
 */
@DisplayName("Smoke — LifecycleAudit regression injection "
        + "(EPIC-0046)")
class LifecycleAuditRegressionSmokeTest {

    @Test
    @DisplayName("3 synthetic regressions (one per "
            + "dimension) are all detected")
    void threeRegressions_allDetected(@TempDir Path sandbox)
            throws IOException {
        // Dimension 1: orphan dotted sub-section
        Path d1 = sandbox.resolve("x-dim1");
        Files.createDirectories(d1);
        Files.writeString(d1.resolve("SKILL.md"), """
                # Dim 1
                ## Core Loop
                1. Phase 1 — do X
                ## Section 99.9b
                never referenced by anything
                """);

        // Dimension 2: write to report without commit
        Path d2 = sandbox.resolve("x-dim2");
        Files.createDirectories(d2);
        Files.writeString(d2.resolve("SKILL.md"), """
                # Dim 2
                ## Phase 1
                Write plans/epic-0099/reports/foo.md
                nothing commits
                """);

        // Dimension 3: skip flag in Core Loop happy path
        Path d3 = sandbox.resolve("x-dim3");
        Files.createDirectories(d3);
        Files.writeString(d3.resolve("SKILL.md"), """
                # Dim 3
                ## Core Loop
                1. Dispatch the wave
                2. Run --skip-verification to save time
                """);

        long t0 = System.nanoTime();
        List<Violation> v = new LifecycleAuditRunner()
                .scan(sandbox);
        long ms = (System.nanoTime() - t0) / 1_000_000;

        assertThat(v).as("at least one violation per "
                + "dimension")
                .anyMatch(x -> x.dimension().equals(
                        LifecycleAuditRunner
                                .DIM_ORPHAN_PHASE))
                .anyMatch(x -> x.dimension().equals(
                        LifecycleAuditRunner
                                .DIM_WRITE_WITHOUT_COMMIT))
                .anyMatch(x -> x.dimension().equals(
                        LifecycleAuditRunner
                                .DIM_SKIP_IN_HAPPY_PATH));

        assertThat(ms).as("scan under 2s performance budget")
                .isLessThan(2000);
    }

    @Test
    @DisplayName("clean sandbox — no violations")
    void cleanSandbox_noViolations(@TempDir Path sandbox)
            throws IOException {
        Path s = sandbox.resolve("x-clean");
        Files.createDirectories(s);
        Files.writeString(s.resolve("SKILL.md"), """
                # Clean
                ## Core Loop
                1. Phase 1
                ## Phase 1
                body
                """);

        List<Violation> v = new LifecycleAuditRunner()
                .scan(sandbox);

        assertThat(v).isEmpty();
    }
}
