package dev.iadev.checkpoint;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link TaskEntry} record.
 */
class TaskEntryTest {

    @Nested
    class PendingFactory {

        @Test
        void pending_whenCalled_createsEntryWithDefaults() {
            var entry = TaskEntry.pending("TASK-0029-0001-001");

            assertThat(entry.taskId())
                    .isEqualTo("TASK-0029-0001-001");
            assertThat(entry.status())
                    .isEqualTo(TaskStatus.PENDING);
            assertThat(entry.prUrl()).isNull();
            assertThat(entry.prNumber()).isNull();
            assertThat(entry.branch()).isNull();
            assertThat(entry.startedAt()).isNull();
            assertThat(entry.completedAt()).isNull();
            assertThat(entry.attempts()).isZero();
            assertThat(entry.failureReason()).isNull();
        }
    }

    @Nested
    class FullConstructor {

        @Test
        void constructor_allFields_preservesValues() {
            var start = Instant.parse("2026-04-07T10:00:00Z");
            var end = Instant.parse("2026-04-07T11:30:00Z");
            var entry = new TaskEntry(
                    "TASK-0029-0001-001",
                    TaskStatus.DONE,
                    "https://github.com/org/repo/pull/42",
                    42, "feat/task-0029-0001-001",
                    start, end, 1, null
            );

            assertThat(entry.taskId())
                    .isEqualTo("TASK-0029-0001-001");
            assertThat(entry.status())
                    .isEqualTo(TaskStatus.DONE);
            assertThat(entry.prUrl())
                    .isEqualTo(
                            "https://github.com/org/repo/pull/42"
                    );
            assertThat(entry.prNumber()).isEqualTo(42);
            assertThat(entry.branch())
                    .isEqualTo("feat/task-0029-0001-001");
            assertThat(entry.startedAt()).isEqualTo(start);
            assertThat(entry.completedAt()).isEqualTo(end);
            assertThat(entry.attempts()).isEqualTo(1);
            assertThat(entry.failureReason()).isNull();
        }
    }

    @Nested
    class WithMethods {

        @Test
        void withStatus_whenCalled_returnsNewEntry() {
            var original =
                    TaskEntry.pending("TASK-0029-0001-001");
            var updated =
                    original.withStatus(TaskStatus.IN_PROGRESS);

            assertThat(updated.status())
                    .isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(original.status())
                    .isEqualTo(TaskStatus.PENDING);
            assertThat(updated.taskId())
                    .isEqualTo(original.taskId());
        }

        @Test
        void withPrUrl_whenCalled_returnsNewEntry() {
            var original =
                    TaskEntry.pending("TASK-0029-0001-001");
            var updated = original.withPrUrl(
                    "https://github.com/pull/42");

            assertThat(updated.prUrl())
                    .isEqualTo("https://github.com/pull/42");
            assertThat(original.prUrl()).isNull();
        }

        @Test
        void withPrNumber_whenCalled_returnsNewEntry() {
            var original =
                    TaskEntry.pending("TASK-0029-0001-001");
            var updated = original.withPrNumber(42);

            assertThat(updated.prNumber()).isEqualTo(42);
            assertThat(original.prNumber()).isNull();
        }

        @Test
        void withBranch_whenCalled_returnsNewEntry() {
            var original =
                    TaskEntry.pending("TASK-0029-0001-001");
            var updated = original.withBranch(
                    "feat/task-0029-0001-001");

            assertThat(updated.branch())
                    .isEqualTo("feat/task-0029-0001-001");
            assertThat(original.branch()).isNull();
        }

        @Test
        void withStartedAt_whenCalled_returnsNewEntry() {
            var original =
                    TaskEntry.pending("TASK-0029-0001-001");
            var now = Instant.now();
            var updated = original.withStartedAt(now);

            assertThat(updated.startedAt()).isEqualTo(now);
            assertThat(original.startedAt()).isNull();
        }

        @Test
        void withCompletedAt_whenCalled_returnsNewEntry() {
            var original =
                    TaskEntry.pending("TASK-0029-0001-001");
            var now = Instant.now();
            var updated = original.withCompletedAt(now);

            assertThat(updated.completedAt()).isEqualTo(now);
            assertThat(original.completedAt()).isNull();
        }

        @Test
        void withAttempts_whenCalled_returnsNewEntry() {
            var original =
                    TaskEntry.pending("TASK-0029-0001-001");
            var updated = original.withAttempts(3);

            assertThat(updated.attempts()).isEqualTo(3);
            assertThat(original.attempts()).isZero();
        }

        @Test
        void withFailureReason_whenCalled_returnsNewEntry() {
            var original =
                    TaskEntry.pending("TASK-0029-0001-001");
            var updated = original.withFailureReason(
                    "Compilation error");

            assertThat(updated.failureReason())
                    .isEqualTo("Compilation error");
            assertThat(original.failureReason()).isNull();
        }

        @Test
        void withMethods_chainCorrectly_allFieldsUpdated() {
            var entry =
                    TaskEntry.pending("TASK-0029-0001-001")
                            .withStatus(TaskStatus.PR_CREATED)
                            .withPrUrl("https://pr/1")
                            .withPrNumber(1)
                            .withBranch("feat/branch")
                            .withAttempts(2)
                            .withStartedAt(Instant.EPOCH);

            assertThat(entry.status())
                    .isEqualTo(TaskStatus.PR_CREATED);
            assertThat(entry.prUrl())
                    .isEqualTo("https://pr/1");
            assertThat(entry.prNumber()).isEqualTo(1);
            assertThat(entry.branch())
                    .isEqualTo("feat/branch");
            assertThat(entry.attempts()).isEqualTo(2);
            assertThat(entry.startedAt())
                    .isEqualTo(Instant.EPOCH);
        }
    }

    @Nested
    class OptionalAccessors {

        @Test
        void optionalPrUrl_whenNull_returnsEmpty() {
            var entry =
                    TaskEntry.pending("TASK-0029-0001-001");
            assertThat(entry.optionalPrUrl()).isEmpty();
        }

        @Test
        void optionalPrUrl_whenPresent_returnsValue() {
            var entry =
                    TaskEntry.pending("TASK-0029-0001-001")
                            .withPrUrl("https://pr/1");
            assertThat(entry.optionalPrUrl())
                    .contains("https://pr/1");
        }

        @Test
        void optionalPrNumber_whenNull_returnsEmpty() {
            var entry =
                    TaskEntry.pending("TASK-0029-0001-001");
            assertThat(entry.optionalPrNumber()).isEmpty();
        }

        @Test
        void optionalBranch_whenNull_returnsEmpty() {
            var entry =
                    TaskEntry.pending("TASK-0029-0001-001");
            assertThat(entry.optionalBranch()).isEmpty();
        }

        @Test
        void optionalStartedAt_whenNull_returnsEmpty() {
            var entry =
                    TaskEntry.pending("TASK-0029-0001-001");
            assertThat(entry.optionalStartedAt()).isEmpty();
        }

        @Test
        void optionalCompletedAt_whenNull_returnsEmpty() {
            var entry =
                    TaskEntry.pending("TASK-0029-0001-001");
            assertThat(entry.optionalCompletedAt()).isEmpty();
        }

        @Test
        void optionalFailureReason_whenNull_returnsEmpty() {
            var entry =
                    TaskEntry.pending("TASK-0029-0001-001");
            assertThat(entry.optionalFailureReason()).isEmpty();
        }
    }

    @Nested
    class TaskIdValidation {

        @Test
        void isValidTaskId_validFormat_returnsTrue() {
            assertThat(TaskEntry.isValidTaskId(
                    "TASK-0029-0001-001")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "TASK-29-1-1",
                "TASK-0029-0001",
                "task-0029-0001-001",
                "TASK-0029-0001-0001",
                "TASK-XXXX-0001-001",
                "",
                "random-string"
        })
        void isValidTaskId_invalidFormat_returnsFalse(
                String taskId) {
            assertThat(TaskEntry.isValidTaskId(taskId))
                    .isFalse();
        }

        @Test
        void isValidTaskId_null_returnsFalse() {
            assertThat(TaskEntry.isValidTaskId(null)).isFalse();
        }

        @Test
        void validateTaskId_validFormat_doesNotThrow() {
            TaskEntry.validateTaskId("TASK-0029-0001-001");
        }

        @Test
        void validateTaskId_invalidFormat_throwsWithMessage() {
            assertThatThrownBy(() ->
                    TaskEntry.validateTaskId("TASK-29-1-1"))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining(
                            "expected format TASK-XXXX-YYYY-NNN"
                    );
        }

        @Test
        void parseTaskId_validId_extractsComponents() {
            var parts = TaskEntry.parseTaskId(
                    "TASK-0029-0001-001");

            assertThat(parts.get("epicId")).isEqualTo("0029");
            assertThat(parts.get("storyId")).isEqualTo("0001");
            assertThat(parts.get("sequential"))
                    .isEqualTo("001");
        }

        @Test
        void parseTaskId_invalidId_throwsException() {
            assertThatThrownBy(() ->
                    TaskEntry.parseTaskId("TASK-29-1-1"))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }
    }
}
