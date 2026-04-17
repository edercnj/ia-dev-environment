package dev.iadev.parallelism.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.parallelism.Collision;
import dev.iadev.parallelism.CollisionCategory;
import dev.iadev.parallelism.ParallelismEvaluator.Report;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JsonRendererTest {

    private final JsonRenderer renderer = new JsonRenderer();

    @Test
    @DisplayName("emptyReport_emitsEmptyArraysAndObjects")
    void render_emptyReport_emitsEmptyArraysAndObjects() {
        Report report = new Report(
                "epic", 0, List.of(), List.of(),
                List.of(), Map.of());

        String json = renderer.render("EPIC-X", report);

        assertThat(json)
                .startsWith("{\n")
                .contains("\"scopeLabel\": \"EPIC-X\"")
                .contains("\"scope\": \"epic\"")
                .contains("\"itemsAnalyzed\": 0")
                .contains("\"hardCount\": 0")
                .contains("\"regenCount\": 0")
                .contains("\"softCount\": 0")
                .contains("\"exitCode\": 0")
                .contains("\"collisions\": []")
                .contains("\"phases\": []")
                .contains("\"hotspotTouches\": {}")
                .contains("\"warnings\": []")
                .endsWith("}\n");
    }

    @Test
    @DisplayName("withCollisionsPhasesAndHotspots_emitsAllSections")
    void render_withCollisionsPhasesAndHotspots() {
        Collision a = new Collision(
                "story-A", "story-B",
                CollisionCategory.HARD,
                Set.of("pom.xml"), "hotspot: pom.xml");
        Collision b = new Collision(
                "story-A", "story-C",
                CollisionCategory.REGEN,
                Set.of(), null);
        Map<String, List<String>> hotspots = new LinkedHashMap<>();
        hotspots.put("pom.xml",
                List.of("story-A", "story-B"));
        hotspots.put("CHANGELOG.md", List.of("story-C"));
        Report report = new Report(
                "epic", 3, List.of(a, b),
                List.of(
                        List.of("story-A"),
                        List.of("story-B", "story-C")),
                List.of("warn-1", "warn-2"),
                hotspots);

        String json = renderer.render("EPIC-Y", report);

        assertThat(json)
                .contains("\"hardCount\": 1")
                .contains("\"regenCount\": 1")
                .contains("\"exitCode\": 2")
                .contains("\"a\": \"story-A\"")
                .contains("\"category\": \"HARD\"")
                .contains("\"reason\": \"hotspot: pom.xml\"")
                .contains("\"reason\": null")
                .contains("\"sharedPaths\": [\"pom.xml\"]")
                .contains("[\"story-B\", \"story-C\"]")
                .contains("\"warnings\": [\"warn-1\", \"warn-2\"]");
    }

    @Test
    @DisplayName("specialChars_areJsonEscaped")
    void render_specialChars_areJsonEscaped() {
        Collision c = new Collision(
                "task-A", "task-B",
                CollisionCategory.HARD,
                Set.of("a\\b", "\"quote\""),
                "line1\nline2\twith\rCR");
        Report report = new Report(
                "task", 2, List.of(c),
                List.of(), List.of("ctrl\u0001char"),
                Map.of());

        String json = renderer.render("scope\"with\\\"quote",
                report);

        assertThat(json)
                .contains(
                        "\"scopeLabel\": \"scope\\\"with\\\\\\\"quote\"")
                .contains("line1\\nline2\\twith\\rCR")
                .contains("\\u0001")
                .contains("\"a\\\\b\"")
                .contains("\"\\\"quote\\\"\"");
    }

    @Test
    @DisplayName("warningsOnlyPath_yieldsExitCodeOne")
    void render_warningsOnly_exitCodeIsOne() {
        Report report = new Report(
                "story", 1, List.of(),
                List.of(List.of("only-id")),
                List.of("missing footprint"),
                Map.of());

        String json = renderer.render("only-id", report);

        assertThat(json).contains("\"exitCode\": 1");
    }
}
