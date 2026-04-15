package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Story-0038-0004 validation: the x-story-plan SKILL.md now documents the v2 extensions
 * (schema-aware flow adding task-file emission, parallel x-task-plan invocations, and
 * task-implementation-map generation) while keeping the legacy v1 flow intact.
 */
class XStoryPlanV2ExtensionsTest {

    private static final Path SKILL_MD = Path.of(
            "src", "main", "resources", "targets", "claude", "skills",
            "core", "plan", "x-story-plan", "SKILL.md");

    @Nested
    class WorkflowHeader {

        @Test
        void workflowOverview_lists_v2_phases_4a_4b_4c() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md)
                    .contains("Phase 0b: SCHEMA VERSION DETECT")
                    .contains("Phase 4a: TASK BREAKDOWN")
                    .contains("Phase 4b: PARALLEL TASK PLANS")
                    .contains("Phase 4c: TASK MAP");
        }

        @Test
        void description_mentions_schemaAware_and_EPIC_0038() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Schema-aware").contains("EPIC-0038");
        }
    }

    @Nested
    class V2Appendix {

        @Test
        void appendix_documentsSchemaVersionDetection() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 0b — Schema Version Detection")
                    .contains("planningSchemaVersion")
                    .contains("SchemaVersionResolver");
        }

        @Test
        void appendix_documentsTaskBreakdownWithIOContracts() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 4a — Task Breakdown with I/O Contracts")
                    .contains("task-TASK-XXXX-YYYY-NNN.md")
                    .contains("RULE-TF-01")
                    .contains("RULE-TF-02");
        }

        @Test
        void appendix_documentsXTaskPlanInvocation() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 4b — Per-Task Plan via x-task-plan")
                    .contains("batches of up to 4")
                    .contains("Rule 13");
        }

        @Test
        void appendix_documentsTaskMapGeneration() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 4c — Task-Implementation-Map Generation")
                    .contains("task-map-gen")
                    .contains("TopologicalSorter");
        }

        @Test
        void appendix_documentsV2DoRExtensions() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 5 — DoR (v2 extensions)")
                    .contains("Task READY checks")
                    .contains("Plan presence")
                    .contains("Map integrity");
        }

        @Test
        void appendix_preservesV1BehaviorBootstrapNote() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("v1 behavior is **unchanged**")
                    .contains("EPIC-0038 itself is executed in v1");
        }
    }
}
