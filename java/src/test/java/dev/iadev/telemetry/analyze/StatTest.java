package dev.iadev.telemetry.analyze;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class StatTest {

    @Test
    void constructor_validInputs_buildsImmutableStat() {
        Stat stat = new Stat(
                "x-story-implement", 5, 1000L, 200L, 180L, 500L,
                List.of("EPIC-0040"));

        assertThat(stat.name()).isEqualTo("x-story-implement");
        assertThat(stat.invocations()).isEqualTo(5);
        assertThat(stat.totalMs()).isEqualTo(1000L);
        assertThat(stat.avgMs()).isEqualTo(200L);
        assertThat(stat.p50Ms()).isEqualTo(180L);
        assertThat(stat.p95Ms()).isEqualTo(500L);
        assertThat(stat.epicIds()).containsExactly("EPIC-0040");
    }

    @Test
    void constructor_nullEpicIds_defaultsToEmptyList() {
        Stat stat = new Stat(
                "skill", 1, 1L, 1L, 1L, 1L, null);

        assertThat(stat.epicIds()).isEmpty();
    }

    @Test
    void constructor_blankName_throws() {
        assertThatThrownBy(() -> new Stat(
                " ", 1, 1L, 1L, 1L, 1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name must not be blank");
    }

    @Test
    void constructor_nullName_throws() {
        assertThatThrownBy(() -> new Stat(
                null, 1, 1L, 1L, 1L, 1L, List.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");
    }

    @Test
    void constructor_negativeInvocations_throws() {
        assertThatThrownBy(() -> new Stat(
                "x", -1, 1L, 1L, 1L, 1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invocations");
    }

    @Test
    void constructor_negativeTotalMs_throws() {
        assertThatThrownBy(() -> new Stat(
                "x", 1, -1L, 1L, 1L, 1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalMs");
    }

    @Test
    void constructor_negativeAvgMs_throws() {
        assertThatThrownBy(() -> new Stat(
                "x", 1, 1L, -1L, 1L, 1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("avgMs");
    }

    @Test
    void constructor_negativeP50Ms_throws() {
        assertThatThrownBy(() -> new Stat(
                "x", 1, 1L, 1L, -1L, 1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("p50Ms");
    }

    @Test
    void constructor_negativeP95Ms_throws() {
        assertThatThrownBy(() -> new Stat(
                "x", 1, 1L, 1L, 1L, -1L, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("p95Ms");
    }

    @Test
    void epicIds_afterMutationAttempt_stableCopy() {
        List<String> mutable = new java.util.ArrayList<>();
        mutable.add("EPIC-0040");
        Stat stat = new Stat(
                "x", 1, 1L, 1L, 1L, 1L, mutable);
        mutable.add("EPIC-0041");

        assertThat(stat.epicIds()).containsExactly("EPIC-0040");
    }
}
