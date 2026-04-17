package dev.iadev.skills;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification test for story-0040-0009 TASK-0040-0009-001: asserts the
 * canonical {@code _TEMPLATE-SKILL.md} authoring template includes the
 * optional {@code Telemetry} section with helper invocations for phase,
 * subagent and MCP markers, plus a back-reference to Rule 13.
 *
 * <p>The template lives under
 * {@code java/src/main/resources/shared/templates/_TEMPLATE-SKILL.md}
 * alongside the other plan/review templates (see
 * {@code _TEMPLATE-ARCHITECTURE-PLAN.md}, etc.).
 */
class TemplateStructureTest {

    private static final Path TEMPLATE_PATH = Paths.get(
            "src/main/resources/shared/templates/_TEMPLATE-SKILL.md");

    private static final Pattern SECTION_HEADER =
            Pattern.compile("(?m)^## Telemetry \\(Optional\\)\\s*$");

    @Test
    @DisplayName("template_fileExists_locatedUnderSharedTemplates")
    void template_fileExists_locatedUnderSharedTemplates() {
        assertThat(TEMPLATE_PATH)
                .as("_TEMPLATE-SKILL.md must exist at %s", TEMPLATE_PATH)
                .exists()
                .isRegularFile();
    }

    @Test
    @DisplayName("template_telemetrySection_presentAsLevelTwoHeader")
    void template_telemetrySection_presentAsLevelTwoHeader() throws IOException {
        String body = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

        assertThat(SECTION_HEADER.matcher(body).find())
                .as("Section '## Telemetry (Optional)' must appear as a level-2 header")
                .isTrue();
    }

    @Test
    @DisplayName("template_telemetrySection_includesPhaseHelperCall")
    void template_telemetrySection_includesPhaseHelperCall() throws IOException {
        String body = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

        assertThat(body)
                .as("Telemetry section must include the phase-start helper call")
                .contains("telemetry-phase.sh start")
                .contains("telemetry-phase.sh end");
    }

    @Test
    @DisplayName("template_telemetrySection_includesSubagentAndMcpMarkers")
    void template_telemetrySection_includesSubagentAndMcpMarkers() throws IOException {
        String body = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

        assertThat(body)
                .as("Telemetry section must document subagent-start/end markers")
                .contains("subagent-start")
                .contains("subagent-end");
        assertThat(body)
                .as("Telemetry section must document mcp-start/end markers")
                .contains("mcp-start")
                .contains("mcp-end");
    }

    @Test
    @DisplayName("template_telemetrySection_referencesRule13")
    void template_telemetrySection_referencesRule13() throws IOException {
        String body = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

        assertThat(body)
                .as("Telemetry section must back-reference rule 13 "
                        + "(skill-invocation-protocol) for markers context")
                .contains("13-skill-invocation-protocol.md");
    }
}
