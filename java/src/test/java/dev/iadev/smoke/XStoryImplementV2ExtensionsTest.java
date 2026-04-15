package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Story-0038-0006 validation: x-story-implement documents wave dispatch (v2) that
 * orchestrates tasks via task-implementation-map, ending the v1 'coalesce ad-hoc'
 * anti-pattern while preserving the legacy task-centric flow.
 */
class XStoryImplementV2ExtensionsTest {

    private static final Path SKILL_MD = Path.of(
            "src", "main", "resources", "targets", "claude", "skills",
            "core", "dev", "x-story-implement", "SKILL.md");

    @Nested
    class Description {
        @Test
        void mentionsWaveDispatchAndEpic0038() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Schema-aware")
                    .contains("EPIC-0038")
                    .contains("task-implementation-map");
        }
    }

    @Nested
    class V2Appendix {
        @Test
        void phase0f_schemaDetection() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 0f — Schema Version Detection")
                    .contains("SchemaVersionResolver");
        }

        @Test
        void phase1_readsMap() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 1 (v2) — Read Task Implementation Map")
                    .contains("MAP_NOT_FOUND");
        }

        @Test
        void phase2_parallelWaveDispatchViaSkillTool() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 2 (v2) — Wave Dispatch Loop")
                    .contains("sibling tool calls in a SINGLE assistant message")
                    .contains("Integration verification");
        }

        @Test
        void phase3_coalescedWaveHandling() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 3 (v2) — Coalesced Wave Handling")
                    .contains("RULE-TF-04");
        }

        @Test
        void phase4_storyAggregationReport() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 4 (v2) — Story-Level Aggregation")
                    .contains("story-implementation-report-STORY-");
        }

        @Test
        void benefitsTableDocumentsV1vsV2() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("v2 Benefits vs v1")
                    .contains("Task coalescing")
                    .contains("Parallelism")
                    .contains("Bisect-ability");
        }

        @Test
        void errorCodesEnumerated() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md)
                    .contains("MAP_NOT_FOUND")
                    .contains("WAVE_VERIFICATION_FAILED")
                    .contains("COALESCED_PARTNER_MISSING")
                    .contains("CROSS_WAVE_REGRESSION");
        }
    }
}
