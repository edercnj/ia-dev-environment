package dev.iadev.application.lifecycle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ReportCommitMessageBuilder} — the
 * canonical builder of Conventional Commit messages for epic
 * report commits added in story-0046-0005.
 *
 * <p>TPP ordering: degenerate input validation → simple
 * execution-plan message → phase-report message with wave
 * number → message body content.</p>
 */
class ReportCommitMessageBuilderTest {

    @Test
    void executionPlan_nullEpicId_throws() {
        assertThatThrownBy(
                () -> ReportCommitMessageBuilder.executionPlan(null, 3, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("epicId");
    }

    @Test
    void executionPlan_blankEpicId_throws() {
        assertThatThrownBy(
                () -> ReportCommitMessageBuilder.executionPlan("  ", 3, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("epicId");
    }

    @Test
    void executionPlan_negativeWaves_throws() {
        assertThatThrownBy(
                () -> ReportCommitMessageBuilder.executionPlan("0046", -1, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("waves");
    }

    @Test
    void executionPlan_negativeStories_throws() {
        assertThatThrownBy(
                () -> ReportCommitMessageBuilder.executionPlan("0046", 3, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stories");
    }

    @Test
    void executionPlan_subjectLineMatchesContract() {
        String msg = ReportCommitMessageBuilder.executionPlan("0046", 3, 7);
        String subject = msg.split("\n", 2)[0];
        assertThat(subject).isEqualTo("docs(epic-0046): add execution plan");
    }

    @Test
    void executionPlan_bodyIncludesWaveAndStoryCountsAndSchema() {
        String msg = ReportCommitMessageBuilder.executionPlan("0046", 3, 7);
        assertThat(msg).contains("- Waves: 3");
        assertThat(msg).contains("- Stories: 7");
        assertThat(msg).contains("- Schema: v2.0");
        assertThat(msg).contains(
                "Refs: plans/epic-0046/reports/execution-plan-epic-0046.md");
    }

    @Test
    void phaseReport_nullEpicId_throws() {
        assertThatThrownBy(
                () -> ReportCommitMessageBuilder.phaseReport(null, 2, 5, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("epicId");
    }

    @Test
    void phaseReport_waveZero_throws() {
        assertThatThrownBy(
                () -> ReportCommitMessageBuilder.phaseReport("0046", 0, 5, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("wave");
    }

    @Test
    void phaseReport_negativeStoryCount_throws() {
        assertThatThrownBy(
                () -> ReportCommitMessageBuilder.phaseReport("0046", 2, -1, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("storyCount");
    }

    @Test
    void phaseReport_subjectLineMatchesContract() {
        String msg = ReportCommitMessageBuilder.phaseReport("0046", 2, 5, 5);
        String subject = msg.split("\n", 2)[0];
        assertThat(subject).isEqualTo("docs(epic-0046): add phase-2 report");
    }

    @Test
    void phaseReport_bodyIncludesWaveSummaryAndCommits() {
        String msg = ReportCommitMessageBuilder.phaseReport("0046", 2, 5, 5);
        assertThat(msg).contains("- Wave 2 complete: 5 stories DONE");
        assertThat(msg).contains("- Commits: 5 story-finalize");
        assertThat(msg).contains(
                "Refs: plans/epic-0046/reports/phase-report-epic-0046-wave2.md");
    }

    @Test
    void phaseReport_differentEpicId_routesSubjectAndRef() {
        String msg = ReportCommitMessageBuilder.phaseReport("0099", 1, 3, 3);
        assertThat(msg).startsWith("docs(epic-0099): add phase-1 report");
        assertThat(msg).contains(
                "Refs: plans/epic-0099/reports/phase-report-epic-0099-wave1.md");
    }
}
