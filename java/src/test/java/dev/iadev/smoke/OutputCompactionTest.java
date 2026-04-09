package dev.iadev.smoke;

import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates output compaction instructions in orchestrator
 * skill templates (story-0030-0004).
 *
 * <p>Acceptance criteria:
 * <ul>
 *   <li>CONTEXT MANAGEMENT instruction in orchestrators</li>
 *   <li>Selective checkpoint reads in x-dev-epic-implement</li>
 *   <li>Phase report delegated to subagent</li>
 *   <li>Compact TDD log format in x-tdd</li>
 * </ul>
 */
@DisplayName("Output Compaction (story-0030-0004)")
class OutputCompactionTest {

    private static final String PROFILE = "java-spring";

    @TempDir
    Path tempDir;

    private Path outputDir;
    private String epicContent;
    private String lifecycleContent;
    private String tddContent;

    @BeforeEach
    void setUp() throws IOException {
        outputDir = tempDir.resolve(PROFILE);
        SmokeTestValidators
                .createDirectoryQuietly(outputDir);
        ProjectConfig config =
                ConfigProfiles.getStack(PROFILE);
        AssemblerPipeline pipeline =
                new AssemblerPipeline(
                        AssemblerPipeline.buildAssemblers());
        PipelineOptions options = new PipelineOptions(
                false, true, false, null);
        PipelineResult result =
                pipeline.runPipeline(
                        config, outputDir, options);
        assertThat(result.success())
                .as("Pipeline must succeed")
                .isTrue();
        epicContent = readSkill("x-dev-epic-implement");
        lifecycleContent = readSkill("x-dev-lifecycle");
        tddContent = readSkill("x-tdd");
    }

    @Nested
    @DisplayName("Context Management instruction")
    class ContextManagement {

        @Test
        @DisplayName("x-dev-epic-implement contains"
                + " CONTEXT MANAGEMENT section")
        void epicImplement_containsContextMgmt() {
            assertThat(epicContent)
                    .contains("CONTEXT MANAGEMENT");
        }

        @Test
        @DisplayName("x-dev-lifecycle contains"
                + " CONTEXT MANAGEMENT section")
        void lifecycle_containsContextMgmt() {
            assertThat(lifecycleContent)
                    .contains("CONTEXT MANAGEMENT");
        }

        @Test
        @DisplayName("CONTEXT MANAGEMENT mentions"
                + " targeted reads")
        void epicImplement_mentionsTargetedReads() {
            assertThat(epicContent)
                    .contains("targeted reads");
        }

        @Test
        @DisplayName("CONTEXT MANAGEMENT mentions grep"
                + " for specific fields")
        void epicImplement_mentionsGrepForFields() {
            assertThat(epicContent)
                    .contains("grep");
        }

        @Test
        @DisplayName("lifecycle CONTEXT MANAGEMENT"
                + " mentions targeted reads")
        void lifecycle_mentionsTargetedReads() {
            assertThat(lifecycleContent)
                    .contains("targeted reads");
        }
    }

    @Nested
    @DisplayName("Selective checkpoint reads")
    class SelectiveCheckpoint {

        @Test
        @DisplayName("x-dev-epic-implement instructs"
                + " selective checkpoint reads")
        void epicImplement_selectiveCheckpointReads() {
            assertThat(epicContent)
                    .contains("selective");
        }

        @Test
        @DisplayName("checkpoint read references"
                + " offset/limit or grep")
        void epicImplement_checkpointReadPattern() {
            assertThat(epicContent).satisfiesAnyOf(
                    c -> assertThat(c)
                            .contains("offset/limit"),
                    c -> assertThat(c)
                            .contains("grep")
            );
        }

        @Test
        @DisplayName("instructs NOT to read full"
                + " execution-state.json")
        void epicImplement_doNotReadFull() {
            assertThat(epicContent).contains(
                    "Do NOT read full files into context");
        }
    }

    @Nested
    @DisplayName("Phase reports in subagent")
    class PhaseReportSubagent {

        @Test
        @DisplayName("phase report generation"
                + " delegated to subagent")
        void epicImplement_phaseReportDelegated() {
            assertThat(epicContent)
                    .contains("subagent");
        }

        @Test
        @DisplayName("orchestrator receives only path"
                + " from phase report")
        void epicImplement_receivesOnlyPath() {
            assertThat(epicContent)
                    .contains("\"path\":");
        }

        @Test
        @DisplayName("phase report subagent returns"
                + " GENERATED status")
        void epicImplement_returnsGeneratedStatus() {
            assertThat(epicContent)
                    .contains("\"GENERATED\"");
        }
    }

    @Nested
    @DisplayName("Compact TDD log mode")
    class CompactTddLog {

        @Test
        @DisplayName("x-tdd contains compact log"
                + " instruction")
        void tdd_containsCompactLogInstruction() {
            assertThat(tddContent)
                    .contains("Compact");
        }

        @Test
        @DisplayName("compact format shows cycle pattern")
        void tdd_compactFormatCyclePattern() {
            assertThat(tddContent)
                    .contains("Cycle {cycleNumber}/"
                            + "{totalCycles}");
        }

        @Test
        @DisplayName("compact format includes RED"
                + " GREEN REFACTOR status markers")
        void tdd_compactFormatStatusMarkers() {
            assertThat(tddContent)
                    .contains("RED {redStatus}")
                    .contains("GREEN {greenStatus}")
                    .contains("REFACTOR {refactorStatus}");
        }

        @Test
        @DisplayName("compact mode triggered by"
                + " orchestrated execution")
        void tdd_compactModeWhenOrchestrated() {
            assertThat(tddContent)
                    .contains("orchestrated");
        }
    }

    @Nested
    @DisplayName("Multi-profile consistency")
    class MultiProfile {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke"
                        + ".OutputCompactionTest"
                        + "#representativeProfiles")
        @DisplayName("epic orchestrator contains"
                + " CONTEXT MANAGEMENT")
        void allProfiles_epicContainsContextMgmt(
                String profile) throws IOException {
            Path out = runProfile(profile);
            String content = Files.readString(
                    out.resolve(
                            ".claude/skills/"
                                    + "x-dev-epic-implement/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .as("[%s] x-dev-epic-implement must"
                                    + " contain CONTEXT"
                                    + " MANAGEMENT",
                            profile)
                    .contains("CONTEXT MANAGEMENT");
        }

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke"
                        + ".OutputCompactionTest"
                        + "#representativeProfiles")
        @DisplayName("x-tdd contains compact log"
                + " format")
        void allProfiles_tddContainsCompactLog(
                String profile) throws IOException {
            Path out = runProfile(profile);
            String content = Files.readString(
                    out.resolve(
                            ".claude/skills/"
                                    + "x-tdd/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .as("[%s] x-tdd must contain"
                                    + " compact log format",
                            profile)
                    .contains("Compact");
        }
    }

    static Stream<String> representativeProfiles() {
        return Stream.of(
                "go-gin",
                "java-quarkus",
                "kotlin-ktor",
                "python-fastapi",
                "rust-axum",
                "typescript-nestjs");
    }

    private Path runProfile(String profile)
            throws IOException {
        Path out = tempDir.resolve(
                "multi-" + profile);
        Files.createDirectories(out);
        ProjectConfig config =
                ConfigProfiles.getStack(profile);
        AssemblerPipeline pipeline =
                new AssemblerPipeline(
                        AssemblerPipeline.buildAssemblers());
        PipelineOptions options = new PipelineOptions(
                false, true, false, null);
        PipelineResult result =
                pipeline.runPipeline(
                        config, out, options);
        assertThat(result.success()).isTrue();
        return out;
    }

    private String readSkill(String skillName)
            throws IOException {
        return Files.readString(
                outputDir.resolve(
                        ".claude/skills/" + skillName
                                + "/SKILL.md"),
                StandardCharsets.UTF_8);
    }
}
