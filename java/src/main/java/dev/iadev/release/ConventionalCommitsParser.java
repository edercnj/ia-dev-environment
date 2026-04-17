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
        Counts counts = new Counts();
        for (String payload : commits) {
            classifyOne(payload, counts);
        }
        return new CommitCounts(
                counts.feat, counts.fix, counts.perf,
                counts.breaking, counts.ignored);
    }

    /**
     * Classifies a single commit payload into {@code counts}.
     * Mirrors the original per-iteration logic with the same
     * branching semantics.
     */
    private static void classifyOne(
            String payload, Counts counts) {
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
            isBreaking = applyType(type, bang, counts);
            recognisedType = true;
        }
        if (!recognisedType) {
            counts.ignored++;
        }
        if (BREAKING_BODY.matcher(body).find()) {
            isBreaking = true;
        }
        if (isBreaking) {
            counts.breaking++;
        }
    }

    /**
     * Increments the counter for {@code type} and returns
     * whether the commit is breaking by bang.
     */
    private static boolean applyType(
            String type, boolean bang, Counts counts) {
        switch (type) {
            case "feat" -> {
                counts.feat++;
                return bang;
            }
            case "fix" -> {
                counts.fix++;
                return bang;
            }
            case "perf" -> {
                counts.perf++;
                return false;
            }
            default -> {
                counts.ignored++;
                return false;
            }
        }
    }

    /** Mutable per-classification accumulator. */
    private static final class Counts {
        int feat;
        int fix;
        int perf;
        int breaking;
        int ignored;
    }
}
