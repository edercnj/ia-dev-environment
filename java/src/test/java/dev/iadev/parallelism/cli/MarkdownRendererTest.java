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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @Nested
    @DisplayName("renderEmpty")
    class RenderEmpty {

        @Test
        void render_emptyReport_emitsDashRowAndNoItemsAndNone() {
            Report report = new Report(
                    "epic", 0, List.of(), List.of(),
                    List.of(), Map.of());

            String md = renderer.render("EPIC-0099", report);

            assertThat(md)
                    .contains("# Parallelism Evaluation — EPIC-0099")
                    .contains("**Scope:** epic | **Items analyzed:** 0")
                    .contains("0 hard, 0 regen, 0 soft")
                    .contains("| — | — | none | — |")
                    .contains("- (no items)")
                    .contains("## Hotspot Touches\n- (none)\n");
        }
    }

    @Nested
    @DisplayName("renderHardCollision")
    class RenderHardCollision {

        @Test
        void render_hardCollision_marksGroupAsSerialize() {
            Collision hard = new Collision(
                    "story-A", "story-B",
                    CollisionCategory.HARD,
                    Set.of("pom.xml"), "hotspot: pom.xml");
            Report report = new Report(
                    "epic", 2, List.of(hard),
                    List.of(List.of("story-A", "story-B")),
                    List.of(),
                    Map.of("pom.xml",
                            List.of("story-A", "story-B")));

            String md = renderer.render("epic-test", report);

            assertThat(md)
                    .contains(
                            "| story-A | story-B "
                                    + "| hard (hotspot: pom.xml) "
                                    + "| pom.xml |")
                    .contains("Group 1 (serialize)")
                    .contains("conflicts detected")
                    .contains("`pom.xml` touched by: "
                            + "story-A, story-B");
        }
    }

    @Nested
    @DisplayName("renderSoftCollision")
    class RenderSoftCollision {

        @Test
        void render_softCollision_groupStillParallel() {
            Collision soft = new Collision(
                    "story-A", "story-B",
                    CollisionCategory.SOFT,
                    Set.of(), null);
            Report report = new Report(
                    "epic", 2, List.of(soft),
                    List.of(List.of("story-A", "story-B")),
                    List.of(), Map.of());

            String md = renderer.render("scope", report);

            assertThat(md)
                    .contains("| story-A | story-B | soft | — |")
                    .contains("Group 1 (parallel)")
                    .contains("(no conflicts)");
        }
    }

    @Nested
    @DisplayName("renderWarnings")
    class RenderWarnings {

        @Test
        void render_warningsPresent_emitsWarningSection() {
            Report report = new Report(
                    "epic", 1, List.of(),
                    List.of(List.of("story-X")),
                    List.of("footprint missing for story-X"),
                    Map.of());

            String md = renderer.render("scope", report);

            assertThat(md)
                    .contains("## Warnings")
                    .contains(
                            "- footprint missing for story-X");
        }
    }

    @Nested
    @DisplayName("renderMultipleHotspots")
    class RenderMultipleHotspots {

        @Test
        void render_multipleHotspots_emitsAllInOrder() {
            Map<String, List<String>> hotspots =
                    new LinkedHashMap<>();
            hotspots.put("CHANGELOG.md",
                    List.of("story-A", "story-B"));
            hotspots.put("pom.xml", List.of("story-A"));
            Report report = new Report(
                    "story", 2, List.of(),
                    List.of(List.of("story-A", "story-B")),
                    List.of(), hotspots);

            String md = renderer.render("pair", report);

            assertThat(md)
                    .contains("`CHANGELOG.md` touched by: "
                            + "story-A, story-B")
                    .contains("`pom.xml` touched by: story-A");
        }
    }
}
