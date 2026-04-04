package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CheckpointState} domain model record.
 */
class CheckpointStateTest {

    private static final Instant NOW = Instant.now();

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("creates with all fields populated")
        void create_allFields_returnsValidRecord() {
            var steps = Map.of("step1", "done");
            var meta = Map.<String, Object>of("key", "value");
            var state = new CheckpointState(
                    "exec-001", NOW, NOW, steps, meta);

            assertThat(state.executionId()).isEqualTo("exec-001");
            assertThat(state.createdAt()).isEqualTo(NOW);
            assertThat(state.updatedAt()).isEqualTo(NOW);
            assertThat(state.completedSteps())
                    .containsEntry("step1", "done");
            assertThat(state.metadata())
                    .containsEntry("key", "value");
        }

        @Test
        @DisplayName("defaults null completedSteps to empty map")
        void create_nullSteps_returnsEmptyMap() {
            var state = new CheckpointState(
                    "exec-001", NOW, NOW, null, Map.of());

            assertThat(state.completedSteps()).isEmpty();
        }

        @Test
        @DisplayName("defaults null metadata to empty map")
        void create_nullMetadata_returnsEmptyMap() {
            var state = new CheckpointState(
                    "exec-001", NOW, NOW, Map.of(), null);

            assertThat(state.metadata()).isEmpty();
        }

        @Test
        @DisplayName("creates defensive copy of completedSteps")
        void create_mutableSteps_returnsImmutableCopy() {
            var mutable = new java.util.HashMap<String, String>();
            mutable.put("step1", "done");
            var state = new CheckpointState(
                    "exec-001", NOW, NOW, mutable, Map.of());

            assertThatThrownBy(
                    () -> state.completedSteps().put("new", "val"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("creates defensive copy of metadata")
        void create_mutableMetadata_returnsImmutableCopy() {
            var mutable = new java.util.HashMap<String, Object>();
            mutable.put("key", "value");
            var state = new CheckpointState(
                    "exec-001", NOW, NOW, Map.of(), mutable);

            assertThatThrownBy(
                    () -> state.metadata().put("new", "val"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("rejects null executionId")
        void create_nullId_throwsException() {
            assertThatThrownBy(
                    () -> new CheckpointState(
                            null, NOW, NOW, Map.of(), Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("executionId");
        }

        @Test
        @DisplayName("rejects blank executionId")
        void create_blankId_throwsException() {
            assertThatThrownBy(
                    () -> new CheckpointState(
                            "  ", NOW, NOW, Map.of(), Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("executionId");
        }
    }
}
