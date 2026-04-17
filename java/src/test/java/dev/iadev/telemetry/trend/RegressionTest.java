package dev.iadev.telemetry.trend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegressionTest {

    @Test
    void construct_validValues_succeeds() {
        Regression r = new Regression("foo", 100L, 140L, 40.0,
                List.of("EPIC-0001", "EPIC-0002"));
        assertThat(r.skill()).isEqualTo("foo");
        assertThat(r.baselineP95Ms()).isEqualTo(100L);
        assertThat(r.currentP95Ms()).isEqualTo(140L);
        assertThat(r.deltaPct()).isEqualTo(40.0);
        assertThat(r.epicsAnalyzed())
                .containsExactly("EPIC-0001", "EPIC-0002");
    }

    @Test
    void construct_blankSkill_throws() {
        assertThatThrownBy(() -> new Regression("   ", 100L, 140L,
                40.0, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construct_negativeBaseline_throws() {
        assertThatThrownBy(() -> new Regression("foo", -1L, 140L,
                40.0, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construct_negativeCurrent_throws() {
        assertThatThrownBy(() -> new Regression("foo", 100L, -1L,
                40.0, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construct_nullEpicsList_replacesWithEmpty() {
        Regression r = new Regression("foo", 100L, 140L, 40.0, null);
        assertThat(r.epicsAnalyzed()).isEmpty();
    }

    @Test
    void epicsAnalyzed_isDefensivelyCopied() {
        List<String> mutable = new ArrayList<>();
        mutable.add("EPIC-0001");
        Regression r = new Regression("foo", 100L, 140L, 40.0, mutable);
        mutable.add("EPIC-0002");
        assertThat(r.epicsAnalyzed()).containsExactly("EPIC-0001");
    }
}
