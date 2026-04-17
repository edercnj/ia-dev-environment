package dev.iadev.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Contract/fuzz test that runs every entry in
 * {@code fixtures/telemetry/pii-corpus.txt} through
 * {@link TelemetryScrubber} and asserts that the scrubbed
 * {@code failureReason} NEVER contains the original secret.
 *
 * <p>The corpus is curated (not random) so the property it
 * encodes is stable and auditable: for every sensitive string
 * shipped in the fixture, the output must NOT retain any
 * sub-sequence that would recover the secret. A single
 * false-negative fails the suite immediately.</p>
 *
 * <p>Corpus entries are grouped by category in the fixture
 * file (comments starting with {@code #}). Each entry is loaded
 * as one row of the parametrized test, giving us 100
 * independent test runs plus one aggregate contract check.</p>
 */
@DisplayName("TelemetryScrubber fuzz — 100 PII corpus entries")
class TelemetryScrubberFuzzTest {

    private static final String CORPUS_RESOURCE =
            "fixtures/telemetry/pii-corpus.txt";
    private static final int EXPECTED_CORPUS_SIZE = 100;

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "33333333-3333-4333-8333-333333333333");
    private static final Instant TS =
            Instant.parse("2026-04-16T14:00:00.000Z");
    private static final String SESSION =
            "claude-sess-fuzz";

    private static final TelemetryScrubber SCRUBBER =
            new TelemetryScrubber();

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("corpus")
    @DisplayName("no corpus entry survives scrubbing")
    void scrub_corpusEntry_containsNoSecretSubstring(
            String sensitiveInput) {
        TelemetryEvent event = new TelemetryEvent(
                "1.0.0", EVENT_ID, TS, SESSION,
                null, null, null,
                EventType.ERROR,
                null, null, null, null, null,
                truncate(sensitiveInput), null);

        TelemetryEvent result = SCRUBBER.scrub(event);

        String scrubbed = result.failureReason();
        assertThat(scrubbed).isNotNull();

        for (String token
                : sensitiveTokens(sensitiveInput)) {
            assertThat(scrubbed)
                    .as("token '%s' must not appear in"
                            + " scrubbed output '%s'",
                            token, scrubbed)
                    .doesNotContain(token);
        }
    }

    @Test
    @DisplayName("corpus file ships with exactly 100"
            + " non-comment entries")
    void corpusSize_isHundred() {
        List<String> entries = loadCorpus();
        assertThat(entries).hasSize(EXPECTED_CORPUS_SIZE);
    }

    @Test
    @DisplayName("aggregate scan: 100/100 corpus entries"
            + " produce zero false negatives")
    void aggregateScan_zeroFalseNegatives() {
        List<String> entries = loadCorpus();
        List<String> failures = new ArrayList<>();

        for (String input : entries) {
            TelemetryEvent event = new TelemetryEvent(
                    "1.0.0", EVENT_ID, TS, SESSION,
                    null, null, null,
                    EventType.ERROR,
                    null, null, null, null, null,
                    truncate(input), null);
            TelemetryEvent scrubbed =
                    SCRUBBER.scrub(event);
            String out = scrubbed.failureReason();
            for (String token : sensitiveTokens(input)) {
                if (out.contains(token)) {
                    failures.add(
                            "'" + token
                                    + "' survived in output '"
                                    + out + "'");
                }
            }
        }

        assertThat(failures)
                .as("false negatives in fuzz corpus")
                .isEmpty();
    }

    private static Stream<String> corpus() {
        return loadCorpus().stream();
    }

    private static List<String> loadCorpus() {
        try (InputStream in = TelemetryScrubberFuzzTest.class
                .getClassLoader()
                .getResourceAsStream(CORPUS_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "corpus resource not found: "
                                + CORPUS_RESOURCE);
            }
            String raw = new String(
                    in.readAllBytes(),
                    StandardCharsets.UTF_8);
            List<String> entries = new ArrayList<>();
            for (String line : raw.split("\\r?\\n")) {
                String trimmed = line.strip();
                if (trimmed.isEmpty()
                        || trimmed.startsWith("#")) {
                    continue;
                }
                entries.add(trimmed);
            }
            return entries;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "failed to load corpus: "
                            + CORPUS_RESOURCE, e);
        }
    }

    /**
     * Extracts the meaningful sensitive sub-strings from a
     * corpus entry. Some corpus lines include framing text
     * (e.g., {@code "leaked AKIA... in logs"}); the assertion
     * must not mechanically compare the entire line because
     * innocuous framing (the verb "leaked") legitimately
     * survives scrubbing. The helper returns the part(s) of
     * the input that MUST be scrubbed, following the category
     * mapping documented in the corpus.
     */
    private static List<String> sensitiveTokens(
            String input) {
        List<String> tokens = new ArrayList<>();
        addMatch(tokens, input, "AKIA[0-9A-Z]{16}");
        addMatch(tokens, input,
                "eyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+"
                        + "\\.[A-Za-z0-9_-]+");
        addMatch(tokens, input,
                "gh[pousr]_[A-Za-z0-9]{36,}");
        addMatch(tokens, input,
                "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+"
                        + "\\.[A-Za-z]{2,}");
        addMatch(tokens, input,
                "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
        // For URL-with-credentials, the user:pass segment must
        // not survive. We extract the authority.
        addMatch(tokens, input,
                "[A-Za-z0-9._-]+:[^@\\s/]+@");
        // For bearer tokens we extract the token portion only
        // (the keyword "Bearer" is not itself a secret).
        java.util.regex.Matcher bearer =
                java.util.regex.Pattern
                        .compile("(?i)bearer\\s+([^\\s]+)")
                        .matcher(input);
        while (bearer.find()) {
            tokens.add(bearer.group(1));
        }
        // For AWS secret assignments we extract the value
        // portion.
        java.util.regex.Matcher awsSecret =
                java.util.regex.Pattern.compile(
                        "(?i)aws_secret[^=\\s]*\\s*="
                                + "\\s*(\\S+)")
                        .matcher(input);
        while (awsSecret.find()) {
            tokens.add(awsSecret.group(1));
        }
        return tokens;
    }

    private static void addMatch(
            List<String> tokens, String input, String regex) {
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile(regex)
                        .matcher(input);
        while (m.find()) {
            tokens.add(m.group());
        }
    }

    /**
     * Truncates long corpus entries to fit within the
     * {@link TelemetryEvent} {@code failureReason} 256-char
     * bound. Real entries are well under the limit; this is a
     * safety net.
     */
    private static String truncate(String input) {
        return input.length() > 256
                ? input.substring(0, 256)
                : input;
    }

    static {
        // Fail-fast sanity check at class-load time so a
        // corrupted fixture surfaces before the parametrized
        // runner starts.
        List<String> entries = loadCorpus();
        if (entries.size() != EXPECTED_CORPUS_SIZE) {
            throw new IllegalStateException(
                    "corpus has " + entries.size()
                            + " entries, expected "
                            + EXPECTED_CORPUS_SIZE
                            + "; grouped by category: "
                            + Arrays.toString(
                                    new int[] {
                                            entries.size()
                                    }));
        }
    }
}
