package dev.iadev.parallelism;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.parallelism.StoryFootprintAggregator.Result;
import dev.iadev.parallelism.StoryFootprintAggregator.TaskFootprintSource;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StoryFootprintAggregatorTest {

    @Test
    void aggregate_emptyList_returnsEmptyFootprintWithNoTaskPlansWarning() {
        Result r = StoryFootprintAggregator.aggregate(List.of());

        assertThat(r.taskCount()).isZero();
        assertThat(r.footprint()).isEqualTo(FileFootprint.EMPTY);
        assertThat(r.warnings()).containsExactly("no task plans found");
    }

    @Test
    void aggregate_singleTaskWithFootprint_returnsThatFootprint() {
        FileFootprint fp = new FileFootprint(
                Set.of("src/A.java"),
                Set.of("pom.xml"),
                Set.of(".claude/skills/x-foo/SKILL.md"));
        Result r = StoryFootprintAggregator.aggregate(
                List.of(new TaskFootprintSource("TASK-0041-0003-001", fp)));

        assertThat(r.taskCount()).isEqualTo(1);
        assertThat(r.warnings()).isEmpty();
        assertThat(r.footprint().writes()).containsExactly("src/A.java");
        assertThat(r.footprint().reads()).containsExactly("pom.xml");
        assertThat(r.footprint().regens())
                .containsExactly(".claude/skills/x-foo/SKILL.md");
    }

    @Test
    void aggregate_threeDisjointTasks_unionsAllSubsectionsAlphabetically() {
        TaskFootprintSource t1 = new TaskFootprintSource(
                "TASK-0041-0003-001",
                new FileFootprint(
                        Set.of("src/C.java"),
                        Set.of("readme.md"),
                        Set.of()));
        TaskFootprintSource t2 = new TaskFootprintSource(
                "TASK-0041-0003-002",
                new FileFootprint(
                        Set.of("src/A.java", "src/B.java"),
                        Set.of(),
                        Set.of(".claude/skills/x-bar/SKILL.md")));
        TaskFootprintSource t3 = new TaskFootprintSource(
                "TASK-0041-0003-003",
                new FileFootprint(
                        Set.of("pom.xml"),
                        Set.of("CHANGELOG.md"),
                        Set.of(".claude/skills/x-baz/SKILL.md")));

        Result r = StoryFootprintAggregator.aggregate(List.of(t1, t2, t3));

        assertThat(r.taskCount()).isEqualTo(3);
        assertThat(r.warnings()).isEmpty();
        assertThat(r.footprint().writes()).containsExactly(
                "pom.xml", "src/A.java", "src/B.java", "src/C.java");
        assertThat(r.footprint().reads()).containsExactly(
                "CHANGELOG.md", "readme.md");
        assertThat(r.footprint().regens()).containsExactly(
                ".claude/skills/x-bar/SKILL.md",
                ".claude/skills/x-baz/SKILL.md");
    }

    @Test
    void aggregate_duplicatedPathsAcrossTasks_areDeduplicated() {
        FileFootprint shared = new FileFootprint(
                Set.of("src/Shared.java"),
                Set.of(),
                Set.of());
        Result r = StoryFootprintAggregator.aggregate(List.of(
                new TaskFootprintSource("TASK-001", shared),
                new TaskFootprintSource("TASK-002", shared)));

        assertThat(r.taskCount()).isEqualTo(2);
        assertThat(r.footprint().writes())
                .containsExactly("src/Shared.java")
                .hasSize(1);
    }

    @Test
    void aggregate_legacyTaskEmitsWarningAndProceedsWithOthers() {
        TaskFootprintSource legacy = new TaskFootprintSource(
                "TASK-0041-0003-002",
                FileFootprint.EMPTY);
        TaskFootprintSource valid = new TaskFootprintSource(
                "TASK-0041-0003-001",
                new FileFootprint(
                        Set.of("src/A.java"),
                        Set.of(),
                        Set.of()));

        Result r = StoryFootprintAggregator.aggregate(List.of(legacy, valid));

        assertThat(r.taskCount()).isEqualTo(2);
        assertThat(r.warnings()).containsExactly(
                "TASK-0041-0003-002 sem footprint (legacy)");
        assertThat(r.footprint().writes()).containsExactly("src/A.java");
    }

    @Test
    void aggregate_multipleLegacyTasks_warningsInLexicographicTaskIdOrder() {
        // Sources submitted in reverse task-ID order on purpose — the
        // aggregator must re-sort warnings by task-ID for determinism.
        TaskFootprintSource tC = new TaskFootprintSource(
                "TASK-0041-0003-003", FileFootprint.EMPTY);
        TaskFootprintSource tA = new TaskFootprintSource(
                "TASK-0041-0003-001", FileFootprint.EMPTY);
        TaskFootprintSource tB = new TaskFootprintSource(
                "TASK-0041-0003-002", FileFootprint.EMPTY);

        Result r = StoryFootprintAggregator.aggregate(List.of(tC, tA, tB));

        assertThat(r.taskCount()).isEqualTo(3);
        assertThat(r.footprint()).isEqualTo(FileFootprint.EMPTY);
        assertThat(r.warnings()).containsExactly(
                "TASK-0041-0003-001 sem footprint (legacy)",
                "TASK-0041-0003-002 sem footprint (legacy)",
                "TASK-0041-0003-003 sem footprint (legacy)");
    }

    @Test
    void aggregate_isDeterministic_twoRunsProduceSameResult() {
        TaskFootprintSource t1 = new TaskFootprintSource(
                "TASK-A",
                new FileFootprint(
                        Set.of("src/Z.java", "src/A.java"),
                        Set.of(),
                        Set.of()));
        TaskFootprintSource t2 = new TaskFootprintSource(
                "TASK-B",
                new FileFootprint(
                        Set.of("src/M.java"),
                        Set.of(),
                        Set.of()));

        Result r1 = StoryFootprintAggregator.aggregate(List.of(t1, t2));
        Result r2 = StoryFootprintAggregator.aggregate(List.of(t1, t2));

        assertThat(r1.footprint().writes())
                .containsExactlyElementsOf(r2.footprint().writes());
        assertThat(r1.warnings()).isEqualTo(r2.warnings());
        assertThat(r1.taskCount()).isEqualTo(r2.taskCount());
    }

    @Test
    void aggregate_resultWarnings_areUnmodifiable() {
        Result r = StoryFootprintAggregator.aggregate(List.of());
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> r.warnings().add("boom"));
    }

    @Test
    void resultConstructor_nullWarnings_defaultsToEmptyList() {
        Result r = new Result(
                FileFootprint.EMPTY, null, 0);

        assertThat(r.warnings()).isEmpty();
        assertThat(r.taskCount()).isZero();
    }
}
