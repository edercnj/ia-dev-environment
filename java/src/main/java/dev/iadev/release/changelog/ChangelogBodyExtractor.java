package dev.iadev.release.changelog;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-domain helper that extracts the body of a Keep-a-Changelog section
 * identified by a SemVer version (e.g. {@code ## [3.2.0]}).
 *
 * <p>The extractor returns the exact block between the target header and the
 * next {@code ## [} header (or end-of-file), with leading and trailing blank
 * lines trimmed. It is used by the {@code x-release} skill's Phase 11
 * PUBLISH step to compose the GitHub Release body.
 *
 * <p>Security: the version argument is validated against a strict SemVer
 * pattern before being interpolated into a regex, eliminating ReDoS and
 * regex-injection risks (RULE-006).
 */
public final class ChangelogBodyExtractor {

    /** Strict SemVer 2.0.0 pattern (MAJOR.MINOR.PATCH with optional pre-release/build). */
    private static final Pattern SEMVER = Pattern.compile(
            "^(0|[1-9][0-9]{0,9})\\.(0|[1-9][0-9]{0,9})\\.(0|[1-9][0-9]{0,9})"
                    + "(?:-[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?"
                    + "(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$");

    private ChangelogBodyExtractor() {
        // utility
    }

    /**
     * Extracts the body of the section {@code ## [version]} from {@code changelog}.
     *
     * @param changelog full CHANGELOG.md text (non-null)
     * @param version SemVer version string (non-null, must match SemVer 2.0.0)
     * @return {@link Optional#of(Object)} with the trimmed body when the section exists,
     *     {@link Optional#empty()} when the section is absent (or changelog is empty)
     * @throws NullPointerException if any argument is null
     * @throws IllegalArgumentException if {@code version} is not a valid SemVer
     */
    public static Optional<String> extract(final String changelog, final String version) {
        Objects.requireNonNull(changelog, "changelog must not be null");
        Objects.requireNonNull(version, "version must not be null");
        if (!SEMVER.matcher(version).matches()) {
            throw new IllegalArgumentException(
                    "version is not a valid SemVer: " + version);
        }
        if (changelog.isEmpty()) {
            return Optional.empty();
        }
        String quoted = Pattern.quote(version);
        Pattern section = Pattern.compile(
                "^## \\[" + quoted + "\\][^\\n]*\\n(.*?)(?=^## \\[|\\z)",
                Pattern.MULTILINE | Pattern.DOTALL);
        Matcher m = section.matcher(changelog);
        if (!m.find()) {
            return Optional.empty();
        }
        return Optional.of(trimBlankLines(m.group(1)));
    }

    private static String trimBlankLines(final String raw) {
        int start = 0;
        int end = raw.length();
        while (start < end && isBlankAt(raw, start)) {
            int lineEnd = raw.indexOf('\n', start);
            if (lineEnd < 0) {
                return "";
            }
            start = lineEnd + 1;
        }
        if (start >= end) {
            return "";
        }

        // Scan forward tracking the end of the last non-blank line. Trailing
        // whitespace-only lines (spaces / tabs followed by '\n', or '\n\n')
        // are excluded from the returned substring.
        int scan = start;
        int lastNonBlankEnd = start;
        while (scan < end) {
            int lineEnd = raw.indexOf('\n', scan);
            if (lineEnd < 0) {
                lineEnd = end;
            }
            if (!isBlankAt(raw, scan)) {
                lastNonBlankEnd = lineEnd;
            }
            scan = (lineEnd < end) ? lineEnd + 1 : end;
        }
        return raw.substring(start, lastNonBlankEnd);
    }

    private static boolean isBlankAt(final String s, final int from) {
        int i = from;
        while (i < s.length() && s.charAt(i) != '\n') {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
            i++;
        }
        return true;
    }
}
