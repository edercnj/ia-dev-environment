package dev.iadev.parallelism;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParallelismEvaluatorTest {

    @TempDir
    Path tmp;

    private final ParallelismEvaluator evaluator =
            new ParallelismEvaluator();

    @Test
    void evaluateEpic_twoIndependentStories_noCollisions()
            throws IOException {
        writeStory(tmp, "story-0099-0001.md",
                """
                # Story 1
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | — | — |
                ## File Footprint
                ### write:
                - java/src/main/java/pkg/A.java
                """);
        writeStory(tmp, "story-0099-0002.md",
                """
                # Story 2
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | — | — |
                ## File Footprint
                ### write:
                - java/src/main/java/pkg/B.java
                """);
        var report = evaluator.evaluateEpic(tmp);
        assertThat(report.collisions()).isEmpty();
        assertThat(report.itemsAnalyzed()).isEqualTo(2);
        assertThat(report.exitCode()).isZero();
    }

    @Test
    void evaluateEpic_samePhaseHardConflict_exit2()
            throws IOException {
        writeStory(tmp, "story-0099-0001.md",
                """
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | — | — |
                ## File Footprint
                ### write:
                - shared.java
                """);
        writeStory(tmp, "story-0099-0002.md",
                """
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | — | — |
                ## File Footprint
                ### write:
                - shared.java
                """);
        var report = evaluator.evaluateEpic(tmp);
        assertThat(report.collisions()).hasSize(1);
        assertThat(report.collisions().get(0).category())
                .isEqualTo(CollisionCategory.HARD);
        assertThat(report.exitCode()).isEqualTo(2);
    }

    @Test
    void evaluateEpic_storyBlocksOther_splitIntoPhases()
            throws IOException {
        writeStory(tmp, "story-0099-0001.md",
                """
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | — | story-0099-0002 |
                ## File Footprint
                ### write:
                - shared.java
                """);
        writeStory(tmp, "story-0099-0002.md",
                """
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | story-0099-0001 | — |
                ## File Footprint
                ### write:
                - shared.java
                """);
        var report = evaluator.evaluateEpic(tmp);
        assertThat(report.phases()).hasSize(2);
        assertThat(report.collisions()).isEmpty();
        assertThat(report.exitCode()).isZero();
    }

    @Test
    void evaluateEpic_missingFootprint_warning_exit1()
            throws IOException {
        writeStory(tmp, "story-0099-0001.md",
                """
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | — | — |
                """);
        writeStory(tmp, "story-0099-0002.md",
                """
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | — | — |
                ## File Footprint
                ### write:
                - b.java
                """);
        var report = evaluator.evaluateEpic(tmp);
        assertThat(report.warnings())
                .anyMatch(w -> w.contains(
                        "story-0099-0001"));
        assertThat(report.exitCode()).isEqualTo(1);
    }

    @Test
    void evaluateEpic_hotspotTouchedByMultiple_isReported()
            throws IOException {
        writeStory(tmp, "story-0099-0001.md",
                """
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | — | — |
                ## File Footprint
                ### write:
                - pom.xml
                """);
        writeStory(tmp, "story-0099-0002.md",
                """
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | — | — |
                ## File Footprint
                ### write:
                - pom.xml
                """);
        var report = evaluator.evaluateEpic(tmp);
        assertThat(report.hotspotTouches())
                .containsKey("pom.xml");
        assertThat(report.hotspotTouches().get("pom.xml"))
                .containsExactly(
                        "story-0099-0001",
                        "story-0099-0002");
    }

    @Test
    void evaluateEpic_determinism_sameBytesAcrossRuns()
            throws IOException {
        writeStory(tmp, "story-0099-0001.md",
                """
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | — | — |
                ## File Footprint
                ### write:
                - shared.java
                """);
        writeStory(tmp, "story-0099-0002.md",
                """
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | — | — |
                ## File Footprint
                ### write:
                - shared.java
                """);
        var r1 = evaluator.evaluateEpic(tmp);
        var r2 = evaluator.evaluateEpic(tmp);
        assertThat(r1.collisions())
                .containsExactlyElementsOf(r2.collisions());
        assertThat(r1.phases()).isEqualTo(r2.phases());
    }

    @Test
    void evaluateStoryPair_producesSinglePhaseReport()
            throws IOException {
        writeStory(tmp, "story-0099-0001.md",
                """
                ## File Footprint
                ### write:
                - a.java
                """);
        writeStory(tmp, "story-0099-0002.md",
                """
                ## File Footprint
                ### write:
                - a.java
                """);
        var report = evaluator.evaluateStoryPair(
                tmp, "story-0099-0001", "story-0099-0002");
        assertThat(report.scope()).isEqualTo("story");
        assertThat(report.collisions()).hasSize(1);
        assertThat(report.phases()).hasSize(1);
    }

    private static void writeStory(
            Path dir, String name, String body)
            throws IOException {
        Files.writeString(dir.resolve(name), body);
    }
}
