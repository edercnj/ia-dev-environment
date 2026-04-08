package dev.iadev.checkpoint;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link TaskStatus} enum transitions and properties.
 */
class TaskStatusTest {

    @Test
    void values_whenCalled_containsNineStatuses() {
        assertThat(TaskStatus.values()).hasSize(9);
    }

    @Test
    void valueOf_allStatuses_parsesCorrectly() {
        assertThat(TaskStatus.valueOf("PENDING"))
                .isEqualTo(TaskStatus.PENDING);
        assertThat(TaskStatus.valueOf("IN_PROGRESS"))
                .isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(TaskStatus.valueOf("PR_CREATED"))
                .isEqualTo(TaskStatus.PR_CREATED);
        assertThat(TaskStatus.valueOf("PR_APPROVED"))
                .isEqualTo(TaskStatus.PR_APPROVED);
        assertThat(TaskStatus.valueOf("PR_MERGED"))
                .isEqualTo(TaskStatus.PR_MERGED);
        assertThat(TaskStatus.valueOf("DONE"))
                .isEqualTo(TaskStatus.DONE);
        assertThat(TaskStatus.valueOf("BLOCKED"))
                .isEqualTo(TaskStatus.BLOCKED);
        assertThat(TaskStatus.valueOf("FAILED"))
                .isEqualTo(TaskStatus.FAILED);
        assertThat(TaskStatus.valueOf("SKIPPED"))
                .isEqualTo(TaskStatus.SKIPPED);
    }

    @Nested
    class ValidTransitions {

        @ParameterizedTest
        @CsvSource({
                "PENDING, IN_PROGRESS",
                "PENDING, BLOCKED",
                "PENDING, SKIPPED",
                "IN_PROGRESS, PR_CREATED",
                "IN_PROGRESS, FAILED",
                "IN_PROGRESS, BLOCKED",
                "PR_CREATED, PR_APPROVED",
                "PR_CREATED, FAILED",
                "PR_APPROVED, PR_MERGED",
                "PR_APPROVED, FAILED",
                "PR_MERGED, DONE",
                "BLOCKED, PENDING",
                "FAILED, PENDING"
        })
        void canTransitionTo_validTransition_returnsTrue(
                TaskStatus from, TaskStatus to) {
            assertThat(from.canTransitionTo(to)).isTrue();
        }

        @ParameterizedTest
        @CsvSource({
                "PENDING, IN_PROGRESS",
                "PENDING, BLOCKED",
                "PENDING, SKIPPED",
                "IN_PROGRESS, PR_CREATED",
                "IN_PROGRESS, FAILED",
                "IN_PROGRESS, BLOCKED",
                "PR_CREATED, PR_APPROVED",
                "PR_CREATED, FAILED",
                "PR_APPROVED, PR_MERGED",
                "PR_APPROVED, FAILED",
                "PR_MERGED, DONE",
                "BLOCKED, PENDING",
                "FAILED, PENDING"
        })
        void validateTransition_validTransition_doesNotThrow(
                TaskStatus from, TaskStatus to) {
            from.validateTransition(to);
        }
    }

    @Nested
    class InvalidTransitions {

        @ParameterizedTest
        @CsvSource({
                "DONE, IN_PROGRESS",
                "DONE, PENDING",
                "DONE, FAILED",
                "SKIPPED, PENDING",
                "SKIPPED, IN_PROGRESS",
                "PENDING, DONE",
                "PENDING, PR_CREATED",
                "IN_PROGRESS, DONE",
                "IN_PROGRESS, PENDING",
                "PR_CREATED, DONE",
                "PR_CREATED, PENDING",
                "PR_APPROVED, PENDING",
                "PR_MERGED, PENDING",
                "PR_MERGED, FAILED"
        })
        void canTransitionTo_invalidTransition_returnsFalse(
                TaskStatus from, TaskStatus to) {
            assertThat(from.canTransitionTo(to)).isFalse();
        }

        @Test
        void validateTransition_doneToInProgress_throws() {
            assertThatThrownBy(() ->
                    TaskStatus.DONE.validateTransition(
                            TaskStatus.IN_PROGRESS))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(
                            "DONE -> IN_PROGRESS "
                                    + "is not a valid transition"
                    );
        }

        @Test
        void validateTransition_skippedToPending_throws() {
            assertThatThrownBy(() ->
                    TaskStatus.SKIPPED.validateTransition(
                            TaskStatus.PENDING))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(
                            "SKIPPED -> PENDING "
                                    + "is not a valid transition"
                    );
        }
    }

    @Nested
    class TerminalStatus {

        @Test
        void isTerminal_done_returnsTrue() {
            assertThat(TaskStatus.DONE.isTerminal()).isTrue();
        }

        @Test
        void isTerminal_skipped_returnsTrue() {
            assertThat(TaskStatus.SKIPPED.isTerminal()).isTrue();
        }

        @Test
        void isTerminal_pending_returnsFalse() {
            assertThat(TaskStatus.PENDING.isTerminal())
                    .isFalse();
        }

        @Test
        void isTerminal_inProgress_returnsFalse() {
            assertThat(TaskStatus.IN_PROGRESS.isTerminal())
                    .isFalse();
        }

        @Test
        void isTerminal_failed_returnsFalse() {
            assertThat(TaskStatus.FAILED.isTerminal())
                    .isFalse();
        }

        @Test
        void isTerminal_blocked_returnsFalse() {
            assertThat(TaskStatus.BLOCKED.isTerminal())
                    .isFalse();
        }

        @Test
        void isTerminal_prCreated_returnsFalse() {
            assertThat(TaskStatus.PR_CREATED.isTerminal())
                    .isFalse();
        }

        @Test
        void isTerminal_prApproved_returnsFalse() {
            assertThat(TaskStatus.PR_APPROVED.isTerminal())
                    .isFalse();
        }

        @Test
        void isTerminal_prMerged_returnsFalse() {
            assertThat(TaskStatus.PR_MERGED.isTerminal())
                    .isFalse();
        }
    }
}
