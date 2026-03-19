package dev.iadev.domain.implementationmap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImplementationMapParserTest {

    @Nested
    class FullPipeline {

        @Test
        void parse_fiveStories_correctStoryCount() {
            var result = ImplementationMapParser.parse(
                    fiveStoryMarkdown());

            assertThat(result.stories()).hasSize(5);
        }

        @Test
        void parse_fiveStories_threePhases() {
            var result = ImplementationMapParser.parse(
                    fiveStoryMarkdown());

            assertThat(result.totalPhases()).isEqualTo(3);
        }

        @Test
        void parse_fiveStories_phaseZeroContainsRoots() {
            var result = ImplementationMapParser.parse(
                    fiveStoryMarkdown());

            assertThat(result.phases().get(0))
                    .containsExactlyInAnyOrder(
                            "story-001", "story-002");
        }

        @Test
        void parse_fiveStories_phaseOneContainsMiddle() {
            var result = ImplementationMapParser.parse(
                    fiveStoryMarkdown());

            assertThat(result.phases().get(1))
                    .containsExactlyInAnyOrder(
                            "story-003", "story-004");
        }

        @Test
        void parse_fiveStories_phaseTwoContainsFinal() {
            var result = ImplementationMapParser.parse(
                    fiveStoryMarkdown());

            assertThat(result.phases().get(2))
                    .containsExactly("story-005");
        }

        @Test
        void parse_fiveStories_criticalPathHasLength3() {
            var result = ImplementationMapParser.parse(
                    fiveStoryMarkdown());

            assertThat(result.criticalPath()).hasSize(3);
            assertThat(result.criticalPath().getLast())
                    .isEqualTo("story-005");
        }

        @Test
        void parse_fiveStories_criticalPathNodesMarked() {
            var result = ImplementationMapParser.parse(
                    fiveStoryMarkdown());

            var pathSet = Set.copyOf(result.criticalPath());
            for (var id : pathSet) {
                assertThat(result.stories().get(id)
                        .isOnCriticalPath()).isTrue();
            }
        }

        @Test
        void parse_fiveStories_noWarnings() {
            var result = ImplementationMapParser.parse(
                    fiveStoryMarkdown());

            assertThat(result.warnings()).isEmpty();
        }

        @Test
        void parse_fiveStories_dagNodePhasesSet() {
            var result = ImplementationMapParser.parse(
                    fiveStoryMarkdown());

            assertThat(result.stories().get("story-001")
                    .phase()).isEqualTo(0);
            assertThat(result.stories().get("story-003")
                    .phase()).isEqualTo(1);
            assertThat(result.stories().get("story-005")
                    .phase()).isEqualTo(2);
        }

        @Test
        void parse_fiveStories_blockedByAndBlocksSymmetric() {
            var result = ImplementationMapParser.parse(
                    fiveStoryMarkdown());

            var s001 = result.stories().get("story-001");
            assertThat(s001.blocks())
                    .containsExactlyInAnyOrder(
                            "story-003", "story-004");

            var s003 = result.stories().get("story-003");
            assertThat(s003.blockedBy())
                    .containsExactly("story-001", "story-002");
        }
    }

    @Nested
    class EmptyInput {

        @Test
        void parse_emptyContent_zeroPhases() {
            var result = ImplementationMapParser.parse("");

            assertThat(result.totalPhases()).isEqualTo(0);
            assertThat(result.stories()).isEmpty();
        }
    }

    @Nested
    class ErrorPaths {

        @Test
        void parse_circularDependency_throwsException() {
            var markdown = """
                    | ID | Title | Blocked By |
                    | :--- | :--- | :--- |
                    | s-A | Node A | s-C |
                    | s-B | Node B | s-A |
                    | s-C | Node C | s-B |
                    """;

            assertThatThrownBy(
                    () -> ImplementationMapParser.parse(markdown))
                    .isInstanceOf(
                            CircularDependencyException.class);
        }

        @Test
        void parse_missingReference_throwsInvalidDag() {
            var markdown = """
                    | ID | Title | Blocked By |
                    | :--- | :--- | :--- |
                    | s-002 | Child | s-001 |
                    """;

            assertThatThrownBy(
                    () -> ImplementationMapParser.parse(markdown))
                    .isInstanceOf(InvalidDagException.class)
                    .hasMessageContaining("s-001");
        }
    }

    @Nested
    class ExecutableStoriesIntegration {

        @Test
        void filter_afterParse_rootsExecutable() {
            var parsed = ImplementationMapParser.parse(
                    fiveStoryMarkdown());

            var executable = ExecutableStories.filter(
                    parsed.stories(), Set.of());

            assertThat(executable)
                    .containsExactlyInAnyOrder(
                            "story-001", "story-002");
        }

        @Test
        void filter_afterRootsComplete_middleExecutable() {
            var parsed = ImplementationMapParser.parse(
                    fiveStoryMarkdown());

            var executable = ExecutableStories.filter(
                    parsed.stories(),
                    Set.of("story-001", "story-002"));

            assertThat(executable)
                    .containsExactlyInAnyOrder(
                            "story-003", "story-004");
        }
    }

    @Nested
    class PartialExecutionIntegration {

        @Test
        void filterByPhase_phase1_returnsOnlyPhase1Stories() {
            var parsed = ImplementationMapParser.parse(
                    fiveStoryMarkdown());

            var result = PartialExecution.filterByPhase(
                    1, parsed.phases());

            assertThat(result)
                    .containsExactlyInAnyOrder(
                            "story-003", "story-004");
        }

        @Test
        void filterByPhase_phase0And2NotIncluded() {
            var parsed = ImplementationMapParser.parse(
                    fiveStoryMarkdown());

            var result = PartialExecution.filterByPhase(
                    1, parsed.phases());

            assertThat(result)
                    .doesNotContain("story-001", "story-002",
                            "story-005");
        }
    }

    private String fiveStoryMarkdown() {
        return """
                | ID | Title | Blocked By |
                | :--- | :--- | :--- |
                | story-001 | Root A | - |
                | story-002 | Root B | - |
                | story-003 | Middle AB | story-001, story-002 |
                | story-004 | Middle A | story-001 |
                | story-005 | Final | story-003, story-004 |
                """;
    }
}
