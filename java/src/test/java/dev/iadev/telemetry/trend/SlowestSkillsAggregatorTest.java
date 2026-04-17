package dev.iadev.telemetry.trend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SlowestSkillsAggregatorTest {

    private final SlowestSkillsAggregator aggregator =
            new SlowestSkillsAggregator();

    @Test
    void rank_emptySeries_returnsEmptyList() {
        assertThat(aggregator.rank(List.of(), 10)).isEmpty();
    }

    @Test
    void rank_multipleSkills_sortedByAvgP95Desc() {
        List<EpicSkillP95> series = List.of(
                new EpicSkillP95("EPIC-0001", "foo", 500L, 10L),
                new EpicSkillP95("EPIC-0002", "foo", 500L, 10L),
                new EpicSkillP95("EPIC-0001", "bar", 1500L, 5L),
                new EpicSkillP95("EPIC-0002", "bar", 1500L, 5L),
                new EpicSkillP95("EPIC-0001", "baz", 800L, 2L),
                new EpicSkillP95("EPIC-0002", "baz", 800L, 2L));
        List<SlowSkill> out = aggregator.rank(series, 10);
        assertThat(out).hasSize(3);
        assertThat(out.get(0).skill()).isEqualTo("bar");
        assertThat(out.get(1).skill()).isEqualTo("baz");
        assertThat(out.get(2).skill()).isEqualTo("foo");
        assertThat(out.get(0).avgP95Ms()).isEqualTo(1500L);
        assertThat(out.get(0).invocations()).isEqualTo(10L);
    }

    @Test
    void rank_topNLessThanTotal_truncates() {
        List<EpicSkillP95> series = List.of(
                new EpicSkillP95("EPIC-0001", "a", 100L, 1L),
                new EpicSkillP95("EPIC-0001", "b", 200L, 1L),
                new EpicSkillP95("EPIC-0001", "c", 300L, 1L));
        List<SlowSkill> out = aggregator.rank(series, 2);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).skill()).isEqualTo("c");
        assertThat(out.get(1).skill()).isEqualTo("b");
    }

    @Test
    void rank_topNZero_returnsAll() {
        List<EpicSkillP95> series = List.of(
                new EpicSkillP95("EPIC-0001", "a", 100L, 1L),
                new EpicSkillP95("EPIC-0001", "b", 200L, 1L));
        List<SlowSkill> out = aggregator.rank(series, 0);
        assertThat(out).hasSize(2);
    }
}
