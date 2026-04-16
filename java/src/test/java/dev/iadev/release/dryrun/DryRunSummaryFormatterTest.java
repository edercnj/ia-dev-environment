package dev.iadev.release.dryrun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DryRunSummaryFormatter}.
 */
@DisplayName("DryRunSummaryFormatterTest")
class DryRunSummaryFormatterTest {

    @Test
    @DisplayName("format_happyPath"
            + "_includesVersionAndCounts")
    void format_happyPath_includesVersionAndCounts() {
        DryRunSummary summary = new DryRunSummary(
                "3.2.0", 13, 13, 0, 0, 0, 47,
                phaseResults(13));

        String output = DryRunSummaryFormatter.format(summary);

        assertThat(output).contains("3.2.0");
        assertThat(output).contains("13 / 13");
        assertThat(output).contains("47");
        assertThat(output).contains(
                "DRY-RUN MODE — "
                        + "no side effects were applied");
    }

    @Test
    @DisplayName("format_abortedRun"
            + "_indicatesPhasesNotReached")
    void format_abortedRun_indicatesPhasesNotReached() {
        DryRunSummary summary = new DryRunSummary(
                "3.2.0", 13, 5, 0, 1, 7, 20,
                phaseResults(13));

        String output = DryRunSummaryFormatter.format(summary);

        assertThat(output).contains("aborted");
        assertThat(output).contains("7");
    }

    private static List<DryRunPhaseResult> phaseResults(int count) {
        List<DryRunPhaseResult> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new DryRunPhaseResult(
                    "PHASE_" + i,
                    DryRunPhaseOutcome.SIMULATED,
                    List.of()));
        }
        return list;
    }
}
