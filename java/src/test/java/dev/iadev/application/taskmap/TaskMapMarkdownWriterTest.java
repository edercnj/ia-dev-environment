package dev.iadev.application.taskmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.taskfile.TestabilityKind;
import dev.iadev.domain.taskmap.RawTask;
import dev.iadev.domain.taskmap.TaskGraph;
import dev.iadev.domain.taskmap.TopologicalSorter;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TaskMapMarkdownWriterTest {

    private static RawTask independent(String id, String title, List<String> deps) {
        return new RawTask(id, title, deps, TestabilityKind.INDEPENDENT, List.of());
    }

    private static TaskGraph linearGraph() {
        return TopologicalSorter.sort(List.of(
                independent("T001", "alpha", List.of()),
                independent("T002", "beta", List.of("T001"))));
    }

    @Nested
    class StructuralAssertions {

        @Test
        void output_containsAllFourSectionsInOrder() {
            String md = TaskMapMarkdownWriter.write("story-0038-0002", linearGraph());
            int header = md.indexOf("# Task Implementation Map");
            int graph = md.indexOf("## Dependency Graph");
            int order = md.indexOf("## Execution Order");
            int coalesced = md.indexOf("## Coalesced Groups");
            int analysis = md.indexOf("## Parallelism Analysis");
            assertThat(header).isLessThan(graph);
            assertThat(graph).isLessThan(order);
            assertThat(order).isLessThan(coalesced);
            assertThat(coalesced).isLessThan(analysis);
        }

        @Test
        void header_includesStoryId() {
            String md = TaskMapMarkdownWriter.write("story-0038-0002", linearGraph());
            assertThat(md).startsWith("# Task Implementation Map — story-0038-0002");
        }

        @Test
        void mermaidBlock_isFencedAndUsesGraphTd() {
            String md = TaskMapMarkdownWriter.write("story-0038-0002", linearGraph());
            assertThat(md).contains("```mermaid\ngraph TD\n");
            assertThat(md).contains("```\n");
        }

        @Test
        void mermaidBlock_emitsOneNodePerTaskAndSortedEdges() {
            String md = TaskMapMarkdownWriter.write("story-0038-0002", linearGraph());
            assertThat(md).contains("T001[\"T001<br/>alpha\"]");
            assertThat(md).contains("T002[\"T002<br/>beta\"]");
            assertThat(md).contains("T001 --> T002");
        }
    }

    @Nested
    class ExecutionOrder {

        @Test
        void table_includesOneRowPerWaveWithBlocks() {
            String md = TaskMapMarkdownWriter.write("story-0038-0002", linearGraph());
            assertThat(md).contains("| 1 | T001 | T002 |");
            assertThat(md).contains("| 2 | T002 | — |");
        }

        @Test
        void coalescedSuperNode_rendersWithParensAndCombinedLabel() {
            TaskGraph g = TopologicalSorter.sort(List.of(
                    independent("T001", "a", List.of()),
                    new RawTask("T002", "b", List.of("T001"),
                            TestabilityKind.COALESCED, List.of("T003")),
                    new RawTask("T003", "c", List.of("T001"),
                            TestabilityKind.COALESCED, List.of("T002"))));
            String md = TaskMapMarkdownWriter.write("story-0038-0002", g);
            assertThat(md).contains("| 1 | T001 | (T002, T003) |");
            assertThat(md).contains("| 2 | (T002, T003) | — |");
        }
    }

    @Nested
    class CoalescedRendering {

        @Test
        void noCoalescedGroups_rendersDashPlaceholder() {
            String md = TaskMapMarkdownWriter.write("story-0038-0002", linearGraph());
            assertThat(md).contains("## Coalesced Groups\n\n—\n");
        }

        @Test
        void coalescedPair_isListedWithJustification() {
            TaskGraph g = TopologicalSorter.sort(List.of(
                    independent("T001", "a", List.of()),
                    new RawTask("T002", "b", List.of("T001"),
                            TestabilityKind.COALESCED, List.of("T003")),
                    new RawTask("T003", "c", List.of("T001"),
                            TestabilityKind.COALESCED, List.of("T002"))));
            String md = TaskMapMarkdownWriter.write("story-0038-0002", g);
            assertThat(md).contains("- (T002 + T003)").contains("RULE-TF-04");
        }
    }

    @Nested
    class ParallelismAnalysis {

        @Test
        void writesAllFourMetrics() {
            String md = TaskMapMarkdownWriter.write("story-0038-0002", linearGraph());
            assertThat(md).contains("- Total tasks: 2");
            assertThat(md).contains("- Number of waves: 2");
            assertThat(md).contains("- Largest wave size: 1");
            assertThat(md).contains("- Estimated speedup vs sequential: 1.00");
        }

        @Test
        void speedupFormat_usesDotAsDecimalSeparator() {
            TaskGraph g = TopologicalSorter.sort(List.of(
                    independent("T001", "a", List.of()),
                    independent("T002", "b", List.of()),
                    independent("T003", "c", List.of("T001", "T002"))));
            String md = TaskMapMarkdownWriter.write("story-0038-0002", g);
            assertThat(md).contains("Estimated speedup vs sequential: 1.50");
        }
    }

    @Nested
    class Idempotency {

        @Test
        void twoWritesOfSameInput_produceByteIdenticalOutput() {
            TaskGraph g = linearGraph();
            String first = TaskMapMarkdownWriter.write("story-0038-0002", g);
            String second = TaskMapMarkdownWriter.write("story-0038-0002", g);
            assertThat(first).isEqualTo(second);
            assertThat(first.getBytes()).isEqualTo(second.getBytes());
        }
    }

    @Nested
    class Validation {

        @Test
        void nullStoryId_throwsNullPointer() {
            assertThatThrownBy(() -> TaskMapMarkdownWriter.write(null, linearGraph()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void blankStoryId_throwsIllegalArgument() {
            assertThatThrownBy(() -> TaskMapMarkdownWriter.write("  ", linearGraph()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("storyId");
        }

        @Test
        void nullGraph_throwsNullPointer() {
            assertThatThrownBy(() -> TaskMapMarkdownWriter.write("story-X", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
