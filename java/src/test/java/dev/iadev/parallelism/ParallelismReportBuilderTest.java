package dev.iadev.parallelism;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ParallelismReportBuilderTest {

    private final ParallelismReportBuilder builder =
            new ParallelismReportBuilder();

    @Test
    void buildEpicReport_aggregatesMissingFootprintWarnings() {
        Map<String, StoryNode> stories = new LinkedHashMap<>();
        stories.put("story-0001-0001", new StoryNode(
                "story-0001-0001",
                FileFootprint.EMPTY,
                List.of()));
        stories.put("story-0001-0002", new StoryNode(
                "story-0001-0002",
                new FileFootprint(
                        Set.of("pkg/B.java"),
                        Set.of(),
                        Set.of()),
                List.of()));
        ParallelismEvaluator.Report report =
                builder.buildEpicReport(
                        stories,
                        List.of(),
                        List.of(List.of(
                                "story-0001-0001",
                                "story-0001-0002")));
        assertThat(report.scope()).isEqualTo("epic");
        assertThat(report.itemsAnalyzed()).isEqualTo(2);
        assertThat(report.warnings())
                .containsExactly(
                        "footprint missing for "
                                + "story-0001-0001");
        assertThat(report.exitCode()).isEqualTo(1);
    }

    @Test
    void buildEpicReport_hotspotTouchedByMultiple_isReported() {
        Map<String, StoryNode> stories = new LinkedHashMap<>();
        stories.put("story-0001-0001", new StoryNode(
                "story-0001-0001",
                new FileFootprint(
                        Set.of("pom.xml"),
                        Set.of(),
                        Set.of()),
                List.of()));
        stories.put("story-0001-0002", new StoryNode(
                "story-0001-0002",
                new FileFootprint(
                        Set.of("pom.xml"),
                        Set.of(),
                        Set.of()),
                List.of()));
        ParallelismEvaluator.Report report =
                builder.buildEpicReport(
                        stories,
                        List.of(),
                        List.of(List.of(
                                "story-0001-0001",
                                "story-0001-0002")));
        assertThat(report.hotspotTouches())
                .containsKey("pom.xml");
        assertThat(report.hotspotTouches().get("pom.xml"))
                .containsExactly(
                        "story-0001-0001",
                        "story-0001-0002");
    }

    @Test
    void buildEpicReport_singleStoryHotspot_isOmitted() {
        Map<String, StoryNode> stories = new LinkedHashMap<>();
        stories.put("story-0001-0001", new StoryNode(
                "story-0001-0001",
                new FileFootprint(
                        Set.of("pom.xml"),
                        Set.of(),
                        Set.of()),
                List.of()));
        ParallelismEvaluator.Report report =
                builder.buildEpicReport(
                        stories,
                        List.of(),
                        List.of(List.of("story-0001-0001")));
        assertThat(report.hotspotTouches()).isEmpty();
    }

    @Test
    void buildStoryPairReport_assemblesTwoStorySinglePhase() {
        StoryNode a = new StoryNode(
                "story-0001-0001",
                new FileFootprint(
                        Set.of("a.java"),
                        Set.of(),
                        Set.of()),
                List.of());
        StoryNode b = new StoryNode(
                "story-0001-0002",
                FileFootprint.EMPTY,
                List.of());
        ParallelismEvaluator.Report report =
                builder.buildStoryPairReport(
                        a, b, List.of());
        assertThat(report.scope()).isEqualTo("story");
        assertThat(report.itemsAnalyzed()).isEqualTo(2);
        assertThat(report.phases()).containsExactly(
                List.of("story-0001-0001",
                        "story-0001-0002"));
        assertThat(report.warnings())
                .containsExactly(
                        "footprint missing for "
                                + "story-0001-0002");
    }

    @Test
    void buildMissingFootprintWarnings_nonEmptyFootprintSkipped() {
        StoryNode withFp = new StoryNode(
                "story-0001-0001",
                new FileFootprint(
                        Set.of("a.java"),
                        Set.of(),
                        Set.of()),
                List.of());
        StoryNode empty = new StoryNode(
                "story-0001-0002",
                FileFootprint.EMPTY,
                List.of());
        List<String> warnings =
                ParallelismReportBuilder
                        .buildMissingFootprintWarnings(
                                List.of(withFp, empty));
        assertThat(warnings).containsExactly(
                "footprint missing for story-0001-0002");
    }
}
