package dev.iadev.release;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-domain classifier that converts a list of commit payloads (subject + body
 * from {@code git log --format=%s%n%b}) into a {@link CommitCounts} aggregate.
 *
 * <p>Input shape: each element of the list is the full payload of a single
 * commit, i.e. the subject line followed optionally by a blank line and the
 * body. Body lines are separated by {@code \n}. Empty input produces
 * {@link CommitCounts#ZERO}.</p>
 *
 * <p>Regexes are pinned (no backtracking on unbounded quantifiers) and anchored
 * to line starts so input of any size classifies in {@code O(n)} — Rule 06 /
 * OWASP A03 (no ReDoS).</p>
 */
public final class ConventionalCommitsParser {

    /** Matches a Conventional Commit subject line; capture group 1 = type, 2 = bang. */
    static final Pattern SUBJECT = Pattern.compile(
            "^([a-z]+)(?:\\([^)]+\\))?(!?):\\s.+$");

    /** Matches a {@code BREAKING CHANGE:} footer anywhere in the body. */
    static final Pattern BREAKING_BODY = Pattern.compile(
            "(?m)^BREAKING CHANGE:\\s");

    private ConventionalCommitsParser() {
        // utility class, not instantiable
    }

    /**
     * Classifies each payload and returns the aggregate counts.
     *
     * @param commits list of commit payloads; never {@code null}, elements may be
     *                {@code null} or empty (treated as {@code ignored}).
     */
    public static CommitCounts classify(List<String> commits) {
        Objects.requireNonNull(commits, "commits");
        int feat = 0;
        int fix = 0;
        int perf = 0;
        int breaking = 0;
        int ignored = 0;
        for (String payload : commits) {
            String safe = payload == null ? "" : payload;
            int nl = safe.indexOf('\n');
            String subject = nl < 0 ? safe : safe.substring(0, nl);
            String body = nl < 0 ? "" : safe.substring(nl + 1);

            Matcher matcher = SUBJECT.matcher(subject);
            boolean recognisedType = false;
            boolean isBreaking = false;
            if (matcher.matches()) {
                String type = matcher.group(1);
                boolean bang = "!".equals(matcher.group(2));
                switch (type) {
                    case "feat" -> {
                        feat++;
                        recognisedType = true;
                        if (bang) {
                            isBreaking = true;
                        }
                    }
                    case "fix" -> {
                        fix++;
                        recognisedType = true;
                        if (bang) {
                            isBreaking = true;
                        }
                    }
                    case "perf" -> {
                        perf++;
                        recognisedType = true;
                    }
                    default -> {
                        ignored++;
                        recognisedType = true;
                    }
                }
            }
            if (!recognisedType) {
                ignored++;
            }
            if (BREAKING_BODY.matcher(body).find()) {
                isBreaking = true;
            }
            if (isBreaking) {
                breaking++;
            }
        }
        return new CommitCounts(feat, fix, perf, breaking, ignored);
    }
}
