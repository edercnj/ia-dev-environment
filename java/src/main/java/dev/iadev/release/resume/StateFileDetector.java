package dev.iadev.release.resume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Detects in-flight release state files under a plans
 * directory and calculates the stale duration since the
 * last phase completed.
 *
 * <p>Only files matching {@code release-state-*.json}
 * whose {@code phase} is NOT {@code "COMPLETED"} are
 * returned. Path normalization rejects traversal
 * patterns (CWE-22).
 */
public final class StateFileDetector {

    private static final String GLOB_PATTERN =
            "release-state-*.json";
    private static final String COMPLETED = "COMPLETED";
    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    private final Path plansDir;

    public StateFileDetector(Path plansDir) {
        this.plansDir = plansDir;
    }

    /**
     * Scans {@code plansDir} for active (non-COMPLETED)
     * release state files and returns the first one found.
     *
     * @return detected state or empty if none active
     */
    public Optional<DetectedState> detect() {
        if (!Files.isDirectory(plansDir)) {
            return Optional.empty();
        }
        List<Path> stateFiles = listStateFiles();
        for (Path file : stateFiles) {
            Optional<DetectedState> result = parse(file);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    /**
     * Formats a {@link Duration} as a human-readable age
     * string (e.g. "2h 14min", "1d 2h 30min").
     *
     * @param age the duration to format
     * @return human-readable age string
     */
    public static String formatAge(Duration age) {
        long totalMinutes = age.toMinutes();
        if (totalMinutes < 1) {
            return "< 1min";
        }
        long days = totalMinutes / (60 * 24);
        long hours = (totalMinutes % (60 * 24)) / 60;
        long minutes = totalMinutes % 60;
        return buildAgeString(days, hours, minutes);
    }

    private List<Path> listStateFiles() {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                Files.newDirectoryStream(
                        plansDir, GLOB_PATTERN)) {
            for (Path entry : stream) {
                if (isSafePath(entry)) {
                    files.add(entry);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    private boolean isSafePath(Path file) {
        Path normalized = file.normalize().toAbsolutePath();
        Path base = plansDir.normalize().toAbsolutePath();
        return normalized.startsWith(base);
    }

    private Optional<DetectedState> parse(Path file) {
        try {
            JsonNode root = MAPPER.readTree(file.toFile());
            String phase = textOrNull(root, "phase");
            if (COMPLETED.equals(phase)) {
                return Optional.empty();
            }
            String version =
                    textOrNull(root, "version");
            String previousVersion =
                    textOrNull(root, "previousVersion");
            String lastCompleted =
                    textOrNull(root, "lastPhaseCompletedAt");
            Duration staleDuration =
                    calculateStaleDuration(lastCompleted);
            return Optional.of(new DetectedState(
                    version, phase, previousVersion,
                    staleDuration, file));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static String textOrNull(
            JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private static Duration calculateStaleDuration(
            String isoTimestamp) {
        if (isoTimestamp == null) {
            return Duration.ZERO;
        }
        Instant lastCompleted =
                Instant.parse(isoTimestamp);
        return Duration.between(lastCompleted, Instant.now());
    }

    private static String buildAgeString(
            long days, long hours, long minutes) {
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || sb.isEmpty()) {
            sb.append(minutes).append("min");
        }
        return sb.toString().trim();
    }
}
