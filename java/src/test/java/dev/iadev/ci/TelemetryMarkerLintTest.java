package dev.iadev.ci;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.ci.TelemetryMarkerLint.Finding;
import dev.iadev.ci.TelemetryMarkerLint.FindingType;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TelemetryMarkerLint}. Covers the balance semantics
 * described in the Javadoc: duplicate starts, duplicate ends, dangling
 * ends, unclosed starts, and balanced (clean) flows.
 */
@DisplayName("TelemetryMarkerLint")
class TelemetryMarkerLintTest {

    private static final Path FAKE_FILE = Path.of("/tmp/fake/SKILL.md");

    @Nested
    @DisplayName("clean flows return no findings")
    class CleanFlows {

        @Test
        @DisplayName("empty file emits zero findings")
        void emptyFile_emitsZero() {
            List<Finding> findings = TelemetryMarkerLint.lintLines(
                    FAKE_FILE, List.of());
            assertThat(findings).isEmpty();
        }

        @Test
        @DisplayName("single balanced pair emits zero findings")
        void singleBalancedPair_emitsZero() {
            List<String> lines = List.of(
                    "telemetry-phase.sh start x-skill Phase-1",
                    "telemetry-phase.sh end   x-skill Phase-1 ok");
            List<Finding> findings = TelemetryMarkerLint.lintLines(
                    FAKE_FILE, lines);
            assertThat(findings).isEmpty();
        }

        @Test
        @DisplayName("multiple balanced pairs on different phases "
                + "emit zero findings")
        void multipleBalancedPairs_emitZero() {
            List<String> lines = List.of(
                    "telemetry-phase.sh start x-skill Phase-1",
                    "telemetry-phase.sh end   x-skill Phase-1 ok",
                    "telemetry-phase.sh start x-skill Phase-2",
                    "telemetry-phase.sh end   x-skill Phase-2 ok",
                    "telemetry-phase.sh start x-skill Phase-3",
                    "telemetry-phase.sh end   x-skill Phase-3 ok");
            List<Finding> findings = TelemetryMarkerLint.lintLines(
                    FAKE_FILE, lines);
            assertThat(findings).isEmpty();
        }
    }

    @Nested
    @DisplayName("violations are detected with precise line numbers")
    class Violations {

        @Test
        @DisplayName("two consecutive phase.start for same phase reports "
                + "DUPLICATE_START on the second occurrence")
        void duplicateStart_isDetected() {
            List<String> lines = List.of(
                    "telemetry-phase.sh start x-skill Phase-1",
                    "telemetry-phase.sh start x-skill Phase-1",
                    "telemetry-phase.sh end   x-skill Phase-1 ok");
            List<Finding> findings = TelemetryMarkerLint.lintLines(
                    FAKE_FILE, lines);
            assertThat(findings).hasSize(1);
            Finding f = findings.get(0);
            assertThat(f.type())
                    .isEqualTo(FindingType.DUPLICATE_START);
            assertThat(f.line()).isEqualTo(2);
            assertThat(f.skill()).isEqualTo("x-skill");
            assertThat(f.phase()).isEqualTo("Phase-1");
        }

        @Test
        @DisplayName("two consecutive phase.end for same phase reports "
                + "DUPLICATE_END")
        void duplicateEnd_isDetected() {
            List<String> lines = List.of(
                    "telemetry-phase.sh start x-skill Phase-1",
                    "telemetry-phase.sh end   x-skill Phase-1 ok",
                    "telemetry-phase.sh end   x-skill Phase-1 ok");
            List<Finding> findings = TelemetryMarkerLint.lintLines(
                    FAKE_FILE, lines);
            assertThat(findings).hasSize(1);
            Finding f = findings.get(0);
            assertThat(f.type())
                    .isEqualTo(FindingType.DUPLICATE_END);
            assertThat(f.line()).isEqualTo(3);
        }

        @Test
        @DisplayName("phase.end without preceding phase.start reports "
                + "DANGLING_END")
        void danglingEnd_isDetected() {
            List<String> lines = List.of(
                    "telemetry-phase.sh end   x-skill Phase-1 ok");
            List<Finding> findings = TelemetryMarkerLint.lintLines(
                    FAKE_FILE, lines);
            assertThat(findings).hasSize(1);
            assertThat(findings.get(0).type())
                    .isEqualTo(FindingType.DANGLING_END);
        }

        @Test
        @DisplayName("phase.start without matching phase.end reports "
                + "UNCLOSED_START")
        void unclosedStart_isDetected() {
            List<String> lines = List.of(
                    "telemetry-phase.sh start x-skill Phase-1",
                    "telemetry-phase.sh start x-other Phase-2",
                    "telemetry-phase.sh end   x-other Phase-2 ok");
            List<Finding> findings = TelemetryMarkerLint.lintLines(
                    FAKE_FILE, lines);
            assertThat(findings).hasSize(1);
            Finding f = findings.get(0);
            assertThat(f.type())
                    .isEqualTo(FindingType.UNCLOSED_START);
            assertThat(f.skill()).isEqualTo("x-skill");
            assertThat(f.phase()).isEqualTo("Phase-1");
        }

        @Test
        @DisplayName("interleaved skills/phases do not trigger false "
                + "positives")
        void interleavedPhases_cleanWhenBalanced() {
            List<String> lines = List.of(
                    "telemetry-phase.sh start x-a Phase-1",
                    "telemetry-phase.sh start x-b Phase-1",
                    "telemetry-phase.sh end   x-a Phase-1 ok",
                    "telemetry-phase.sh end   x-b Phase-1 ok");
            List<Finding> findings = TelemetryMarkerLint.lintLines(
                    FAKE_FILE, lines);
            assertThat(findings).isEmpty();
        }
    }

    @Nested
    @DisplayName("marker tokens are found inside markdown fences")
    class InlineMarkers {

        @Test
        @DisplayName("markers inside backtick-code survive the scan")
        void backtickCode_stillParsed() {
            List<String> lines = List.of(
                    "some prose",
                    "`telemetry-phase.sh start x-skill Phase-X`",
                    "more prose",
                    "`telemetry-phase.sh end   x-skill Phase-X ok`");
            List<Finding> findings = TelemetryMarkerLint.lintLines(
                    FAKE_FILE, lines);
            assertThat(findings).isEmpty();
        }
    }
}
