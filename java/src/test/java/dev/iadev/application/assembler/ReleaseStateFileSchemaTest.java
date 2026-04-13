package dev.iadev.application.assembler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates consistency between the state file schema
 * documentation and the SKILL.md of x-release.
 *
 * <p>Ensures the schema doc exists, mentions all 14
 * phases, documents all required and optional fields,
 * provides a valid JSON example, and is properly
 * cross-referenced from SKILL.md.</p>
 */
@DisplayName("ReleaseStateFileSchemaTest")
class ReleaseStateFileSchemaTest {

    private static final Path SKILL_PATH = Path.of(
            "src/main/resources/targets/claude/skills/"
                    + "core/x-release/SKILL.md");

    private static final Path SCHEMA_DOC_PATH = Path.of(
            "src/main/resources/targets/claude/skills/"
                    + "core/x-release/references/"
                    + "state-file-schema.md");

    private static final List<String> REQUIRED_PHASES =
            List.of(
                    "INITIALIZED", "DETERMINED",
                    "VALIDATED", "BRANCHED", "UPDATED",
                    "CHANGELOG_DONE", "COMMITTED",
                    "PR_OPENED", "APPROVAL_PENDING",
                    "MERGED", "TAGGED",
                    "BACKMERGE_OPENED",
                    "BACKMERGE_CONFLICT", "COMPLETED");

    private static final List<String>
            ALWAYS_PRESENT_FIELDS = List.of(
            "schemaVersion", "version", "phase",
            "branch", "baseBranch", "hotfix", "dryRun",
            "signedTag", "interactive", "startedAt",
            "lastPhaseCompletedAt", "phasesCompleted",
            "targetVersion", "previousVersion",
            "bumpType");

    private static final List<String>
            PHASE_DEPENDENT_FIELDS = List.of(
            "prNumber", "prUrl", "prTitle",
            "changelogEntry", "tagMessage");

    @Test
    @DisplayName("schema doc file exists on disk")
    void stateFileSchemaDocExists() {
        assertThat(SCHEMA_DOC_PATH)
                .as("references/state-file-schema.md"
                        + " must exist")
                .exists();
    }

    @Test
    @DisplayName("schema doc mentions all 14 required"
            + " phases")
    void schemaDocMentionsAllRequiredPhases()
            throws IOException {
        String content = Files.readString(
                SCHEMA_DOC_PATH);
        for (String phase : REQUIRED_PHASES) {
            assertThat(content)
                    .as("Schema doc must mention"
                            + " phase: %s", phase)
                    .contains(phase);
        }
    }

    @Test
    @DisplayName("schema doc mentions all always-present"
            + " and phase-dependent fields")
    void schemaDocMentionsAllFields()
            throws IOException {
        String content = Files.readString(
                SCHEMA_DOC_PATH);
        for (String field : ALWAYS_PRESENT_FIELDS) {
            assertThat(content)
                    .as("Schema doc must mention"
                            + " always-present field:"
                            + " %s", field)
                    .contains(field);
        }
        for (String field : PHASE_DEPENDENT_FIELDS) {
            assertThat(content)
                    .as("Schema doc must mention"
                            + " phase-dependent field:"
                            + " %s", field)
                    .contains(field);
        }
    }

    @Test
    @DisplayName("example JSON in schema doc is valid"
            + " and contains required fields")
    void exampleJsonInSchemaDocIsValid()
            throws IOException {
        String content = Files.readString(
                SCHEMA_DOC_PATH);
        int start = content.indexOf("```json");
        int end = content.indexOf("```", start + 7);
        assertThat(start)
                .as("JSON fence must be present")
                .isGreaterThanOrEqualTo(0);
        assertThat(end)
                .as("JSON fence must be closed"
                        + " after start")
                .isGreaterThan(start);
        String json = content.substring(
                start + 7, end).trim();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);

        assertThat(node.get("schemaVersion").asInt())
                .isEqualTo(1);
        assertThat(REQUIRED_PHASES)
                .contains(node.get("phase").asText());
        for (String field : ALWAYS_PRESENT_FIELDS) {
            assertThat(node.has(field))
                    .as("Example JSON missing"
                            + " always-present field:"
                            + " %s", field)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("SKILL.md references the schema doc"
            + " and schemaVersion field")
    void skillMdReferencesStateFileSchema()
            throws IOException {
        String content = Files.readString(SKILL_PATH);
        assertThat(content)
                .as("SKILL.md must reference the"
                        + " schema doc")
                .contains("state-file-schema.md");
        assertThat(content)
                .as("SKILL.md must mention"
                        + " schemaVersion field")
                .contains("schemaVersion");
    }

    @Test
    @DisplayName("SKILL.md defines all happy-path"
            + " phase transitions")
    void skillMdDefinesPhaseTransitions()
            throws IOException {
        String content = Files.readString(SKILL_PATH);
        List<String> transitions = List.of(
                "APPROVAL_PENDING",
                "RESUME-AND-TAG",
                "OPEN-RELEASE-PR",
                "BACK-MERGE-DEVELOP",
                "APPROVAL-GATE");
        for (String transition : transitions) {
            assertThat(content)
                    .as("SKILL.md must define"
                            + " phase: %s", transition)
                    .contains(transition);
        }
    }
}
