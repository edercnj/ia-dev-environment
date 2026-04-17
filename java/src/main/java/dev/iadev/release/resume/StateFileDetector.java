package dev.iadev.release.resume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.iadev.release.ReleaseContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

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

    /**
     * Strict SemVer grammar accepted by
     * {@link #resolveStatePath(Path, String, ReleaseContext)}.
     * Restricts input to {@code MAJOR.MINOR.PATCH[-preRelease]}
     * with lowercase alphanumerics only (story-0039-0014
     * TASK-010: OWASP A03 Injection — no path traversal).
     */
    private static final Pattern SEMVER_STRICT =
            Pattern.compile(
                    "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\."
                            + "(0|[1-9]\\d*)"
                            + "(?:-[a-z0-9.]+)?$");

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

    /**
     * Resolves the release-state file path for {@code version}
     * under the rules of {@code ctx} (story-0039-0014 §5.2 —
     * hotfix state files live alongside release-normal files
     * but with a distinct {@code release-state-hotfix-}
     * prefix so both flows can be in flight simultaneously
     * without conflict).
     *
     * <p>Security (TASK-010): {@code version} is validated
     * against a strict SemVer grammar before use in the
     * file name, preventing path traversal (CWE-22) via
     * {@code ../} or platform-specific separators.</p>
     *
     * @param plansDir parent directory (hardcoded prefix);
     *                 never {@code null}
     * @param version  target version string; must match
     *                 strict SemVer grammar
     * @param ctx      release context deciding the filename
     *                 prefix
     * @return resolved path
     *         ({@code release-state-<V>.json} or
     *         {@code release-state-hotfix-<V>.json})
     * @throws IllegalArgumentException when {@code version}
     *         fails the strict SemVer check
     */
    public static Path resolveStatePath(
            Path plansDir,
            String version,
            ReleaseContext ctx) {
        Objects.requireNonNull(plansDir, "plansDir");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(ctx, "ctx");
        if (!SEMVER_STRICT.matcher(version).matches()) {
            throw new IllegalArgumentException(
                    "version is not a strict SemVer "
                            + "literal: " + version);
        }
        String fileName = ctx.hotfix()
                ? "release-state-hotfix-"
                        + version + ".json"
                : "release-state-" + version + ".json";
        return plansDir.resolve(fileName);
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
        files.sort(Comparator.comparing(
                p -> p.getFileName().toString()));
        return files;
    }

    private boolean isSafePath(Path file) {
        try {
            if (Files.isSymbolicLink(file)) {
                return false;
            }
            Path base = plansDir.toRealPath();
            Path realFile = file.toRealPath();
            return realFile.startsWith(base);
        } catch (IOException e) {
            return false;
        }
    }

    private Optional<DetectedState> parse(Path file) {
        try {
            JsonNode root = MAPPER.readTree(file.toFile());
            Optional<String> phase = textField(root, "phase");
            Optional<String> version = textField(root, "version");
            if (phase.isEmpty() || version.isEmpty()) {
                return Optional.empty();
            }
            if (COMPLETED.equals(phase.get())) {
                return Optional.empty();
            }
            String previousVersion =
                    textField(root, "previousVersion")
                            .orElse(null);
            Duration staleDuration = calculateStaleDuration(
                    textField(root, "lastPhaseCompletedAt"));
            return Optional.of(new DetectedState(
                    version.get(), phase.get(), previousVersion,
                    staleDuration, file));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> textField(
            JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        return Optional.of(node.asText());
    }

    private static Duration calculateStaleDuration(
            Optional<String> isoTimestamp) {
        if (isoTimestamp.isEmpty()) {
            return Duration.ZERO;
        }
        try {
            Instant lastCompleted =
                    Instant.parse(isoTimestamp.get());
            return Duration.between(
                    lastCompleted, Instant.now());
        } catch (DateTimeParseException e) {
            return Duration.ZERO;
        }
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
