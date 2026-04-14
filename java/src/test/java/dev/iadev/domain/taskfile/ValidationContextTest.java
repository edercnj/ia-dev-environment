package dev.iadev.domain.taskfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ValidationContextTest {

    @Test
    void of_filenameOnly_hasEmptyKnownTaskIds() {
        ValidationContext ctx = ValidationContext.of("task-TASK-0038-0001-001.md");
        assertThat(ctx.filename()).isEqualTo("task-TASK-0038-0001-001.md");
        assertThat(ctx.knownTaskIds()).isEmpty();
    }

    @Test
    void of_withKnownTaskIds_preservesSet() {
        ValidationContext ctx = ValidationContext.of(
                "task-TASK-0038-0001-001.md", Set.of("TASK-0038-0001-002"));
        assertThat(ctx.knownTaskIds()).isPresent();
        assertThat(ctx.knownTaskIds().get()).containsExactly("TASK-0038-0001-002");
    }

    @Test
    void knownTaskIdsSet_isImmutable() {
        ValidationContext ctx = ValidationContext.of(
                "task-TASK-0038-0001-001.md", Set.of("TASK-0038-0001-002"));
        assertThatThrownBy(() -> ctx.knownTaskIds().get().add("X"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructor_nullFilename_throwsNullPointer() {
        assertThatThrownBy(() -> new ValidationContext(null, java.util.Optional.empty()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullKnownTaskIds_throwsNullPointer() {
        assertThatThrownBy(() -> new ValidationContext("task.md", null))
                .isInstanceOf(NullPointerException.class);
    }
}
