package dev.iadev.telemetry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Streaming reader over an append-only NDJSON file of
 * {@link TelemetryEvent}s.
 *
 * <p>Designed for large logs: events are deserialized lazily, one per
 * {@link Stream#iterator()} advance, so peak heap never grows with the file
 * size. {@link #stream()} and {@link #streamSkippingInvalid()} MUST be used
 * in a try-with-resources block — the underlying {@link BufferedReader} is
 * closed when the stream is closed.</p>
 *
 * <p>{@link #count()} is a convenience that walks the file line-by-line
 * without constructing {@link TelemetryEvent} instances; it is cheaper than
 * materializing the stream just to count records and ignores blank lines.</p>
 */
public final class TelemetryReader {

    private static final Logger LOG =
            LoggerFactory.getLogger(TelemetryReader.class);

    private final Path path;

    private TelemetryReader(Path path) {
        this.path = path;
    }

    /**
     * Opens a reader over the given NDJSON file. The file is not opened
     * until the caller invokes {@link #stream()} /
     * {@link #streamSkippingInvalid()} / {@link #count()}; this lets readers
     * be constructed eagerly (e.g. by a CLI) without holding OS handles
     * open before any work runs.
     *
     * @param path the NDJSON file
     * @return a reader targeting {@code path}
     */
    public static TelemetryReader open(Path path) {
        Objects.requireNonNull(path, "path is required");
        return new TelemetryReader(path);
    }

    /** @return the NDJSON file path this reader targets */
    public Path path() {
        return path;
    }

    /**
     * Returns a lazy {@link Stream} over the events. Blank lines are
     * skipped. A malformed line aborts the stream with
     * {@link IllegalArgumentException} (callers who want to tolerate
     * corrupt lines should use {@link #streamSkippingInvalid()}).
     *
     * @return a stream of {@link TelemetryEvent}; MUST be closed via
     *         try-with-resources
     */
    public Stream<TelemetryEvent> stream() {
        return openStream(false);
    }

    /**
     * Returns a lazy {@link Stream} over the events that silently skips
     * blank lines and JSON-parse failures (logged as SLF4J warnings).
     *
     * @return a stream of successfully parsed events
     */
    public Stream<TelemetryEvent> streamSkippingInvalid() {
        return openStream(true);
    }

    /**
     * Counts non-blank lines in the file without deserializing events.
     *
     * @return the number of NDJSON records
     * @throws UncheckedIOException if the file cannot be read
     */
    public long count() {
        try {
            if (!Files.exists(path)) {
                return 0L;
            }
            try (Stream<String> lines = Files.lines(
                    path, StandardCharsets.UTF_8)) {
                return lines.filter(s -> !s.isBlank()).count();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "failed to count telemetry events in "
                            + path, e);
        }
    }

    private Stream<TelemetryEvent> openStream(
            boolean skipInvalid) {
        BufferedReader reader = openReader();
        Iterator<TelemetryEvent> iterator = new EventIterator(
                reader, skipInvalid, path);
        Stream<TelemetryEvent> stream =
                StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(
                                iterator,
                                Spliterator.ORDERED
                                        | Spliterator.NONNULL),
                        false);
        return stream.onClose(() -> {
            try {
                reader.close();
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "failed to close telemetry reader: "
                                + path, e);
            }
        });
    }

    private BufferedReader openReader() {
        try {
            if (!Files.exists(path)) {
                return new BufferedReader(
                        new java.io.StringReader(""));
            }
            return Files.newBufferedReader(
                    path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "failed to open telemetry reader: "
                            + path, e);
        }
    }

    private static final class EventIterator
            implements Iterator<TelemetryEvent> {

        private final BufferedReader reader;
        private final boolean skipInvalid;
        private final Path path;
        private TelemetryEvent nextEvent;
        private boolean exhausted;

        EventIterator(
                BufferedReader reader,
                boolean skipInvalid,
                Path path) {
            this.reader = reader;
            this.skipInvalid = skipInvalid;
            this.path = path;
        }

        @Override
        public boolean hasNext() {
            if (nextEvent != null) {
                return true;
            }
            if (exhausted) {
                return false;
            }
            nextEvent = advance();
            if (nextEvent == null) {
                exhausted = true;
                return false;
            }
            return true;
        }

        @Override
        public TelemetryEvent next() {
            if (!hasNext()) {
                throw new NoSuchElementException(
                        "no more telemetry events in "
                                + path);
            }
            TelemetryEvent current = nextEvent;
            nextEvent = null;
            return current;
        }

        private TelemetryEvent advance() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    try {
                        return TelemetryEvent.fromJsonLine(
                                line);
                    } catch (IllegalArgumentException e) {
                        if (!skipInvalid) {
                            throw e;
                        }
                        LOG.warn(
                                "skipping malformed telemetry"
                                        + " line in {}: {}",
                                path, e.getMessage());
                    }
                }
                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "failed to read telemetry line from "
                                + path, e);
            }
        }
    }
}
