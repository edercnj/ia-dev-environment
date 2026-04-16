package dev.iadev.infrastructure.adapter.output.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.iadev.domain.model.PhaseMetric;
import dev.iadev.domain.port.output.TelemetrySink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * File-based {@link TelemetrySink} that appends one JSONL
 * line per phase to {@code plans/release-metrics.jsonl}
 * (story-0039-0012 §3.1). Writes are performed under a
 * {@link FileLock} so concurrent releases do not
 * interleave bytes. I/O failures are logged with the
 * {@code TELEMETRY_WRITE_FAILED} code (§5.3) and never
 * propagate — the release flow continues unaffected.
 *
 * <h2>Security</h2>
 * <ul>
 *   <li>JSON serialization is performed by Jackson
 *       (never string concat) — protects against JSON
 *       injection in {@code releaseVersion} or
 *       {@code phase}.</li>
 *   <li>The output path is provided by the application
 *       layer and fixed to
 *       {@code plans/release-metrics.jsonl}; no
 *       user-controlled path segment is accepted.</li>
 * </ul>
 */
public final class FileTelemetryWriter
        implements TelemetrySink {

    /** Error code surfaced in warning logs per §5.3. */
    public static final String WRITE_FAILED_CODE =
            "TELEMETRY_WRITE_FAILED";

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    FileTelemetryWriter.class);

    /**
     * Intra-JVM lock table keyed by absolute path.
     * {@link FileLock} only serializes across processes
     * — within the same JVM, threads must coordinate
     * explicitly. This map guarantees that two
     * {@link FileTelemetryWriter} instances pointing at
     * the same path share a single
     * {@link ReentrantLock}.
     */
    private static final ConcurrentHashMap<Path,
            ReentrantLock> PATH_LOCKS =
            new ConcurrentHashMap<>();

    private final Path outputPath;
    private final ObjectMapper mapper;

    /**
     * Creates a writer that appends metrics to the given
     * path, creating parent directories as needed.
     *
     * @param outputPath fixed JSONL target path; must not
     *                   be {@code null}
     */
    public FileTelemetryWriter(Path outputPath) {
        if (outputPath == null) {
            throw new IllegalArgumentException(
                    "outputPath must not be null");
        }
        this.outputPath = outputPath;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature
                        .WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void emit(PhaseMetric metric) {
        if (metric == null) {
            throw new IllegalArgumentException(
                    "metric must not be null");
        }
        try {
            String line = toJsonLine(metric);
            appendLineLocked(line);
        } catch (IOException e) {
            LOG.warn(
                    "{}: unable to append telemetry line "
                            + "to {} -- {}",
                    WRITE_FAILED_CODE,
                    outputPath,
                    e.getMessage());
        }
    }

    private String toJsonLine(PhaseMetric metric)
            throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("releaseVersion",
                metric.releaseVersion());
        payload.put("releaseType",
                metric.releaseType().wireValue());
        payload.put("phase", metric.phase());
        payload.put("startedAt",
                metric.startedAt().toString());
        payload.put("endedAt",
                metric.endedAt().toString());
        payload.put("durationSec", metric.durationSec());
        payload.put("outcome", metric.outcome().name());
        return mapper.writeValueAsString(payload)
                + System.lineSeparator();
    }

    private void appendLineLocked(String line)
            throws IOException {
        ensureParentExists();
        byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
        ReentrantLock jvmLock = PATH_LOCKS
                .computeIfAbsent(
                        outputPath.toAbsolutePath(),
                        p -> new ReentrantLock());
        jvmLock.lock();
        try {
            // FileLock coordinates across processes;
            // ReentrantLock coordinates within this JVM.
            // Explicit seek-to-EOF under both locks keeps
            // position+write atomic on macOS, where a
            // FileChannel opened with APPEND ignores its
            // own position.
            try (FileChannel channel = FileChannel.open(
                    outputPath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE);
                 FileLock ignored = channel.lock()) {
                long eof = channel.size();
                channel.position(eof);
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
        } finally {
            jvmLock.unlock();
        }
    }

    private void ensureParentExists() throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
}
