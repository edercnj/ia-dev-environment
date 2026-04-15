package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Story-0038-0005 validation: x-task-implement SKILL.md documents the v2 task-first
 * execution path (task file + plan + map consumption, pre-gates, per-cycle TDD,
 * output verification, atomic commit) while preserving the legacy v1 Double-Loop flow.
 */
class XTaskImplementV2ExtensionsTest {

    private static final Path SKILL_MD = Path.of(
            "src", "main", "resources", "targets", "claude", "skills",
            "core", "dev", "x-task-implement", "SKILL.md");

    @Nested
    class DescriptionAndHeader {

        @Test
        void description_mentionsSchemaAwareTaskFirst() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Schema-aware")
                    .contains("task-TASK-XXXX-YYYY-NNN.md")
                    .contains("plan-task-TASK-XXXX-YYYY-NNN.md");
        }
    }

    @Nested
    class V2Appendix {

        @Test
        void phase0c_documentsSchemaDetection() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 0c — Schema Version Detection")
                    .contains("SchemaVersionResolver");
        }

        @Test
        void phase0d_resolvesAllThreeArtifacts() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 0d — Input Resolution")
                    .contains("TASK_ARTIFACT_NOT_FOUND");
        }

        @Test
        void phase0e_documentsPreExecutionGates() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 0e — Pre-Execution Gates")
                    .contains("UNMET_DEPENDENCY")
                    .contains("INDEPENDENT")
                    .contains("REQUIRES_MOCK")
                    .contains("COALESCED");
        }

        @Test
        void phase2_perCycleTddLoop() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 2 (v2) — Per-Cycle TDD Loop")
                    .contains("TPP order")
                    .contains("tdd-log-TASK-");
        }

        @Test
        void phase3_outputVerificationTable() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 3 (v2) — Post-Execution Output Verification")
                    .contains("OUTPUT_CONTRACT_VIOLATION");
        }

        @Test
        void phase4_atomicCommitViaXGitCommit() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 4 (v2) — Atomic Commit via x-git-commit")
                    .contains("RULE-TF-04")
                    .contains("Coalesces-with:");
        }

        @Test
        void phase5_statusReportShape() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md).contains("Phase 5 (v2) — Status Report")
                    .contains("commitSha")
                    .contains("coverageDelta")
                    .contains("wallclockMs");
        }

        @Test
        void errorCodes_enumerated() throws IOException {
            String md = Files.readString(SKILL_MD, StandardCharsets.UTF_8);
            assertThat(md)
                    .contains("TASK_ARTIFACT_NOT_FOUND")
                    .contains("UNMET_DEPENDENCY")
                    .contains("SCHEMA_VIOLATION")
                    .contains("OUTPUT_CONTRACT_VIOLATION")
                    .contains("RED_NOT_OBSERVED")
                    .contains("REFACTOR_BROKE_TESTS");
        }
    }
}
