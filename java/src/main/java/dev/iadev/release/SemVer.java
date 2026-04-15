package dev.iadev.release;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Value object for a Semantic Versioning 2.0 version string.
 *
 * <p>Accepted grammar (subset of https://semver.org/spec/v2.0.0.html): three
 * dot-separated non-negative integers with no leading zeros, optionally
 * followed by a pre-release suffix prefixed with {@code -} and composed of
 * lowercase letters, digits, and dots. Build metadata is NOT supported
 * (intentional — release tags in this project do not carry it).</p>
 *
 * <p>Examples accepted: {@code 0.0.0}, {@code 1.2.3}, {@code 3.1.0-rc.1},
 * {@code 10.20.30}. Examples rejected: {@code 1.2}, {@code v1.2.3},
 * {@code 01.2.3}, {@code 1.2.3+build.1}.</p>
 */
public record SemVer(int major, int minor, int patch, String preRelease) {

    private static final Pattern PATTERN = Pattern.compile(
            "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-([a-z0-9.]+))?$");

    /** The implicit baseline version used when no tag exists in the repository. */
    public static final SemVer ZERO = new SemVer(0, 0, 0, null);

    public SemVer {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException(
                    "SemVer components must be non-negative: "
                            + major + "." + minor + "." + patch);
        }
        if (preRelease != null && preRelease.isEmpty()) {
            throw new IllegalArgumentException(
                    "preRelease must be null or non-empty; got empty string");
        }
    }

    /**
     * Parses a SemVer literal per the grammar documented on this class.
     *
     * @throws InvalidBumpException with code
     *         {@link InvalidBumpException.Code#VERSION_INVALID_FORMAT} when
     *         {@code raw} does not match the grammar.
     */
    public static SemVer parse(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.startsWith("v") ? raw.substring(1) : raw;
        Matcher matcher = PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new InvalidBumpException(
                    InvalidBumpException.Code.VERSION_INVALID_FORMAT,
                    "Not a SemVer literal: " + raw);
        }
        int ma = Integer.parseInt(matcher.group(1));
        int mi = Integer.parseInt(matcher.group(2));
        int pa = Integer.parseInt(matcher.group(3));
        String pre = matcher.group(4);
        return new SemVer(ma, mi, pa, pre);
    }

    /**
     * Returns the canonical string form ({@code MAJOR.MINOR.PATCH[-preRelease]}).
     * The leading {@code v} is NOT included — callers that need a git tag style
     * must prepend it.
     */
    @Override
    public String toString() {
        String base = major + "." + minor + "." + patch;
        return preRelease == null ? base : base + "-" + preRelease;
    }
}
