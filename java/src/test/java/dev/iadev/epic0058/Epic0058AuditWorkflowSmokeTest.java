package dev.iadev.epic0058;

import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for EPIC-0058, Story 0058-0008: audit.yml CI workflow.
 *
 * <p>Validates that audit.yml exists in the main repo and is generated
 * in projects produced by ia-dev-env via CicdAssembler + AuditWorkflowStep.</p>
 */
@DisplayName("Epic0058AuditWorkflowSmokeTest — audit.yml workflow")
class Epic0058AuditWorkflowSmokeTest {

    private static final Path REPO_ROOT =
            Paths.get("..").toAbsolutePath().normalize();
    private static final Path MAIN_WORKFLOW_PATH =
            REPO_ROOT.resolve(".github/workflows/audit.yml");

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("audit.yml exists in main repo .github/workflows/")
    void mainRepo_auditWorkflowExists() {
        assertThat(MAIN_WORKFLOW_PATH)
                .as("audit.yml must exist at %s", MAIN_WORKFLOW_PATH)
                .exists()
                .isRegularFile();
    }

    @Test
    @DisplayName("audit.yml contains audit-self-check job")
    void mainRepo_auditWorkflowHasSelfCheckJob() throws IOException {
        String content = Files.readString(
                MAIN_WORKFLOW_PATH, StandardCharsets.UTF_8);
        assertThat(content).contains("audit-self-check");
    }

    @Test
    @DisplayName("audit.yml contains audit-matrix job with fail-fast: true")
    void mainRepo_auditWorkflowHasFailFastMatrix() throws IOException {
        String content = Files.readString(
                MAIN_WORKFLOW_PATH, StandardCharsets.UTF_8);
        assertThat(content).contains("audit-matrix");
        assertThat(content).contains("fail-fast: true");
    }

    @Test
    @DisplayName("audit.yml triggers on develop and epic/** branches")
    void mainRepo_auditWorkflowTriggersOnCorrectBranches() throws IOException {
        String content = Files.readString(
                MAIN_WORKFLOW_PATH, StandardCharsets.UTF_8);
        assertThat(content).contains("develop");
        assertThat(content).contains("epic/**");
    }

    @Test
    @DisplayName("audit.yml matrix includes all 5 audit scripts")
    void mainRepo_auditWorkflowHasAllFiveScripts() throws IOException {
        String content = Files.readString(
                MAIN_WORKFLOW_PATH, StandardCharsets.UTF_8);
        assertThat(content).contains("audit-flow-version.sh");
        assertThat(content).contains("audit-epic-branches.sh");
        assertThat(content).contains("audit-skill-visibility.sh");
        assertThat(content).contains("audit-model-selection.sh");
        assertThat(content).contains("audit-execution-integrity.sh");
    }

    @Test
    @DisplayName("CicdAssembler generates audit.yml in output project")
    void cicdAssembler_generatesAuditWorkflow() {
        ProjectConfig config =
                ConfigProfiles.getStack("java-spring");
        AssemblerPipeline pipeline =
                new AssemblerPipeline(
                        AssemblerPipeline.buildAssemblers());
        PipelineOptions options = new PipelineOptions(
                false, true, false, null);

        PipelineResult result = pipeline.runPipeline(
                config, tempDir, options);
        assertThat(result.success())
                .as("Pipeline must succeed")
                .isTrue();

        Path auditWorkflow = tempDir
                .resolve(".github/workflows/audit.yml");
        assertThat(auditWorkflow)
                .as("audit.yml must be generated in output")
                .exists()
                .isRegularFile();
    }
}
