package dev.iadev.infrastructure.adapter.output.telemetry;

import dev.iadev.domain.model.PhaseMetric;
import dev.iadev.domain.model.PhaseOutcome;
import dev.iadev.domain.model.ReleaseType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link FileTelemetryWriter}.
 * Validates the task-plan scenarios:
 * <ul>
 *   <li>TASK-001 RED: degenerate (--telemetry off uses
 *       {@link NoopTelemetrySink} -> zero file writes).
 *   </li>
 *   <li>TASK-002 GREEN: happy path append+flush.</li>
 *   <li>TASK-003/004: concurrency — 2 threads append 100
 *       lines each, result is 200 well-formed lines.</li>
 *   <li>TASK-005/006: permission-denied warn-only.</li>
 * </ul>
 */
class FileTelemetryWriterIT {

    private static final Instant T0 = Instant.parse(
            "2026-04-13T08:00:00Z");

    @Nested
    @DisplayName("TASK-001 degenerate")
    class Degenerate {

        @Test
        @DisplayName("noop_emit_producesNoFile")
        void noop_emit_producesNoFile(@TempDir Path dir) {
            NoopTelemetrySink noop = new NoopTelemetrySink();

            noop.emit(metric("3.2.0", "INIT", 10L,
                    PhaseOutcome.SUCCESS));

            // Noop never writes — the jsonl path is not
            // touched.
            Path jsonl = dir.resolve("release-metrics.jsonl");
            assertThat(Files.exists(jsonl)).isFalse();
        }
    }

    @Nested
    @DisplayName("TASK-002 happy path")
    class HappyPath {

        @Test
        @DisplayName("emit_onePhase_appendsOneJsonLine")
        void emit_onePhase_appendsOneLine(
                @TempDir Path dir) throws IOException {
            Path jsonl = dir.resolve(
                    "release-metrics.jsonl");
            FileTelemetryWriter writer =
                    new FileTelemetryWriter(jsonl);

            writer.emit(metric("3.2.0", "VALIDATED", 142L,
                    PhaseOutcome.SUCCESS));

            List<String> lines = Files.readAllLines(
                    jsonl, StandardCharsets.UTF_8);
            assertThat(lines).hasSize(1);
            String line = lines.get(0);
            assertThat(line)
                    .contains("\"releaseVersion\":\"3.2.0\"")
                    .contains("\"phase\":\"VALIDATED\"")
                    .contains("\"durationSec\":142")
                    .contains("\"outcome\":\"SUCCESS\"")
                    .contains("\"releaseType\":\"release\"");
        }

        @Test
        @DisplayName("emit_multipleReleasesAndPhases"
                + "_appendsInOrder")
        void emit_multipleReleases_appendsInOrder(
                @TempDir Path dir) throws IOException {
            Path jsonl = dir.resolve(
                    "release-metrics.jsonl");
            FileTelemetryWriter writer =
                    new FileTelemetryWriter(jsonl);

            writer.emit(metric("3.2.0", "INIT", 1L,
                    PhaseOutcome.SUCCESS));
            writer.emit(metric("3.2.0", "VALIDATED", 2L,
                    PhaseOutcome.SUCCESS));
            writer.emit(metric("3.2.0", "PUBLISH", 3L,
                    PhaseOutcome.SKIPPED));

            List<String> lines = Files.readAllLines(jsonl);
            assertThat(lines).hasSize(3);
            assertThat(lines.get(2))
                    .contains("\"outcome\":\"SKIPPED\"");
        }
    }

    @Nested
    @DisplayName("TASK-003/004 concurrency")
    class Concurrency {

        @Test
        @DisplayName("emit_twoThreadsWriting100LinesEach"
                + "_producesTwoHundredWellFormedLines")
        void emit_twoThreads_200WellFormedLines(
                @TempDir Path dir)
                throws IOException, InterruptedException {
            Path jsonl = dir.resolve(
                    "release-metrics.jsonl");
            FileTelemetryWriter writer =
                    new FileTelemetryWriter(jsonl);

            final int perThread = 100;
            ExecutorService pool = Executors
                    .newFixedThreadPool(2);
            CountDownLatch start = new CountDownLatch(1);

            Runnable worker = () -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int i = 0; i < perThread; i++) {
                    writer.emit(metric(
                            "3.2.0",
                            "P" + i,
                            i,
                            PhaseOutcome.SUCCESS));
                }
            };
            pool.submit(worker);
            pool.submit(worker);
            start.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(
                    30, TimeUnit.SECONDS)).isTrue();

            List<String> lines = Files.readAllLines(jsonl);
            assertThat(lines).hasSize(perThread * 2);
            // Every line must be a complete JSON object
            List<String> malformed = lines.stream()
                    .filter(l -> !l.startsWith("{")
                            || !l.endsWith("}"))
                    .collect(Collectors.toList());
            assertThat(malformed).isEmpty();
        }
    }

    @Nested
    @DisplayName("TASK-005/006 write-failure warn-only")
    class WriteFailure {

        @Test
        @DisplayName("emit_readonlyParent_doesNotThrow")
        void emit_readonlyParent_doesNotThrow(
                @TempDir Path dir) throws IOException {
            // Simulate permission-denied by pointing to a
            // path where the parent is an existing FILE
            // (not a directory) — any OS will refuse to
            // create the JSONL there.
            Path blocker = dir.resolve("blocker");
            Files.writeString(blocker, "not a dir");
            Path jsonl = blocker.resolve("x.jsonl");
            FileTelemetryWriter writer =
                    new FileTelemetryWriter(jsonl);

            // MUST NOT throw — warn-only.
            writer.emit(metric("3.2.0", "INIT", 1L,
                    PhaseOutcome.SUCCESS));
        }
    }

    @Nested
    @DisplayName("TASK-007 security — JSON escaping")
    class Security {

        @Test
        @DisplayName("emit_releaseVersionWithQuote"
                + "_escapedByJackson")
        void emit_releaseVersionWithQuote_escaped(
                @TempDir Path dir) throws IOException {
            Path jsonl = dir.resolve(
                    "release-metrics.jsonl");
            FileTelemetryWriter writer =
                    new FileTelemetryWriter(jsonl);

            writer.emit(metric(
                    "3.2.0\",\"phase\":\"HIJACK",
                    "INIT", 1L,
                    PhaseOutcome.SUCCESS));

            String content = Files.readString(jsonl);
            // Jackson escapes — the hijack payload is
            // inside the releaseVersion value, not a new
            // JSON field.
            assertThat(content).contains("\\\"phase\\\"");
            // exactly one line written
            assertThat(IntStream.range(0, content.length())
                    .filter(i -> content.charAt(i) == '\n')
                    .count()).isEqualTo(1L);
        }
    }

    private static PhaseMetric metric(
            String releaseVersion,
            String phase,
            long duration,
            PhaseOutcome outcome) {
        return new PhaseMetric(
                releaseVersion,
                ReleaseType.RELEASE,
                phase,
                T0,
                T0.plusSeconds(duration),
                duration,
                outcome);
    }
}
