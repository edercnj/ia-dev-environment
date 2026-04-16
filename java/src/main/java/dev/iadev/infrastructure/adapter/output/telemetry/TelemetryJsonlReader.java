package dev.iadev.infrastructure.adapter.output.telemetry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.iadev.domain.model.PhaseMetric;
import dev.iadev.domain.model.PhaseOutcome;
import dev.iadev.domain.model.ReleaseType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Reads {@code plans/release-metrics.jsonl} and converts
 * each non-blank line into a {@link PhaseMetric}. Pure
 * adapter — passes a {@link Stream} to the domain
 * {@link dev.iadev.domain.telemetry.BenchmarkAnalyzer}
 * per DIP (story-0039-0012 escalation note TASK-009).
 */
public final class TelemetryJsonlReader {

    private static final TypeReference<Map<String, Object>>
            LINE_TYPE = new TypeReference<>() { };

    private final ObjectMapper mapper;

    public TelemetryJsonlReader() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    /**
     * Reads all lines from the given JSONL path. Returns
     * an empty stream if the file does not exist (fresh
     * repository).
     *
     * @param path JSONL path; must not be {@code null}
     * @return stream of parsed metrics in file order
     * @throws UncheckedIOException if the file exists but
     *         cannot be read
     */
    public Stream<PhaseMetric> read(Path path) {
        if (path == null) {
            throw new IllegalArgumentException(
                    "path must not be null");
        }
        if (!Files.exists(path)) {
            return Stream.empty();
        }
        try {
            return Files.readAllLines(path).stream()
                    .filter(l -> !l.isBlank())
                    .map(this::parseLine);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read telemetry JSONL: "
                            + path, e);
        }
    }

    private PhaseMetric parseLine(String line) {
        try {
            Map<String, Object> raw = mapper.readValue(
                    line, LINE_TYPE);
            return new PhaseMetric(
                    (String) raw.get("releaseVersion"),
                    ReleaseType.fromWire(
                            (String) raw.get("releaseType")),
                    (String) raw.get("phase"),
                    Instant.parse(
                            (String) raw.get("startedAt")),
                    Instant.parse(
                            (String) raw.get("endedAt")),
                    ((Number) raw.get("durationSec"))
                            .longValue(),
                    PhaseOutcome.valueOf(
                            (String) raw.get("outcome")));
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to parse telemetry line: "
                            + line, e);
        }
    }
}
