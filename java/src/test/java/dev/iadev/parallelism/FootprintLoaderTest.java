package dev.iadev.parallelism;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FootprintLoaderTest {

    @TempDir
    Path tmp;

    private final FootprintLoader loader = new FootprintLoader();

    @Test
    void loadStories_readsFootprintAndBlockedBy()
            throws IOException {
        Files.writeString(tmp.resolve("story-0099-0001.md"),
                """
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | — | story-0099-0002 |
                ## File Footprint
                ### write:
                - pkg/A.java
                """);
        Files.writeString(tmp.resolve("story-0099-0002.md"),
                """
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | story-0099-0001 | — |
                ## File Footprint
                ### write:
                - pkg/B.java
                """);
        Map<String, StoryNode> stories =
                loader.loadStories(tmp);
        assertThat(stories).containsKeys(
                "story-0099-0001", "story-0099-0002");
        assertThat(stories.get("story-0099-0001").blockedBy())
                .isEmpty();
        assertThat(stories.get("story-0099-0002").blockedBy())
                .containsExactly("story-0099-0001");
        assertThat(stories.get("story-0099-0001")
                .footprint().writes())
                .containsExactly("pkg/A.java");
    }

    @Test
    void loadStories_missingDirectory_returnsEmptyMap()
            throws IOException {
        Path ghost = tmp.resolve("does-not-exist");
        Map<String, StoryNode> stories =
                loader.loadStories(ghost);
        assertThat(stories).isEmpty();
    }

    @Test
    void loadStories_ignoresNonMatchingFilenames()
            throws IOException {
        Files.writeString(tmp.resolve("README.md"), "ignored");
        Files.writeString(tmp.resolve("story-bad.md"),
                "also ignored");
        Files.writeString(tmp.resolve("story-0001-0001.md"),
                """
                ## File Footprint
                ### write:
                - only.java
                """);
        Map<String, StoryNode> stories =
                loader.loadStories(tmp);
        assertThat(stories.keySet())
                .containsExactly("story-0001-0001");
    }

    @Test
    void loadStories_legacyStoryWithoutFootprint_isEmpty()
            throws IOException {
        Files.writeString(tmp.resolve("story-0099-0003.md"),
                """
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | — | — |
                # Pre-epic-0041 story, no footprint block
                """);
        Map<String, StoryNode> stories =
                loader.loadStories(tmp);
        assertThat(stories).hasSize(1);
        assertThat(stories.get("story-0099-0003")
                .footprint().isEmpty()).isTrue();
    }

    @Test
    void parseBlockedBy_multipleDeps_areReturnedInOrder() {
        String body = """
                ## 1. Dependências
                | Blocked By | Blocks |
                | :--- | :--- |
                | story-0001-0001, story-0001-0002 | — |
                """;
        List<String> deps =
                FootprintLoader.parseBlockedBy(body);
        assertThat(deps).containsExactly(
                "story-0001-0001",
                "story-0001-0002");
    }

    @Test
    void topologicalPhases_splitsByDependencies() {
        Map<String, StoryNode> stories = Map.of(
                "story-0001-0001", new StoryNode(
                        "story-0001-0001",
                        FileFootprint.EMPTY,
                        List.of()),
                "story-0001-0002", new StoryNode(
                        "story-0001-0002",
                        FileFootprint.EMPTY,
                        List.of("story-0001-0001")));
        List<List<String>> phases =
                FootprintLoader.topologicalPhases(stories);
        assertThat(phases).hasSize(2);
        assertThat(phases.get(0))
                .containsExactly("story-0001-0001");
        assertThat(phases.get(1))
                .containsExactly("story-0001-0002");
    }

    @Test
    void topologicalPhases_cycleFallsBackToFinalWave() {
        Map<String, StoryNode> stories = Map.of(
                "story-0001-0001", new StoryNode(
                        "story-0001-0001",
                        FileFootprint.EMPTY,
                        List.of("story-0001-0002")),
                "story-0001-0002", new StoryNode(
                        "story-0001-0002",
                        FileFootprint.EMPTY,
                        List.of("story-0001-0001")));
        List<List<String>> phases =
                FootprintLoader.topologicalPhases(stories);
        assertThat(phases).hasSize(1);
        assertThat(phases.get(0))
                .containsExactlyInAnyOrder(
                        "story-0001-0001",
                        "story-0001-0002");
    }
}
