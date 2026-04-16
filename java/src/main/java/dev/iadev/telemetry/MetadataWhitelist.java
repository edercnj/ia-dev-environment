package dev.iadev.telemetry;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Closed list of metadata keys that are allowed to appear on a
 * persisted {@link TelemetryEvent}. Any other key is removed by
 * {@link TelemetryScrubber} with an {@code INFO} log line.
 *
 * <p>The set is intentionally small. Adding a new key requires
 * an ADR + rule 20 amendment + a new test case; the source of
 * truth lives in {@link #DEFAULT_ALLOWED_KEYS}.</p>
 *
 * <p>Instances are immutable and safe for concurrent use.</p>
 */
public final class MetadataWhitelist {

    /**
     * Canonical whitelist from rule 20 §3. Ordered for
     * deterministic log output.
     */
    public static final Set<String> DEFAULT_ALLOWED_KEYS;

    static {
        Set<String> keys = new LinkedHashSet<>();
        keys.add("retryCount");
        keys.add("commitSha");
        keys.add("filesChanged");
        keys.add("linesAdded");
        keys.add("linesDeleted");
        keys.add("exitCode");
        keys.add("toolAttempt");
        keys.add("phaseNumber");
        DEFAULT_ALLOWED_KEYS =
                Collections.unmodifiableSet(keys);
    }

    private final Set<String> allowed;

    /** Creates a whitelist seeded with {@link #DEFAULT_ALLOWED_KEYS}. */
    public MetadataWhitelist() {
        this(DEFAULT_ALLOWED_KEYS);
    }

    /**
     * Creates a whitelist with a custom key set. Used by tests
     * that need to verify boundary behaviour (empty whitelist,
     * superset whitelist, etc.).
     *
     * @param allowed the allow-list; must not be null
     */
    public MetadataWhitelist(Set<String> allowed) {
        this.allowed = Set.copyOf(
                Objects.requireNonNull(allowed, "allowed"));
    }

    /**
     * @param key candidate metadata key; may be null
     * @return {@code true} when the key is on the whitelist;
     *         {@code false} for null or unknown keys
     */
    public boolean isAllowed(String key) {
        return key != null && allowed.contains(key);
    }

    /** @return the immutable allow-list view. */
    public Set<String> allowedKeys() {
        return allowed;
    }
}
