package dev.iadev.telemetry.hooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end smoke tests for {@code telemetry-phase.sh subagent-start} /
 * {@code subagent-end} sub-commands (story-0040-0007 TASK-001).
 *
 * <p>Covers the CLI contract declared in §5.1 of story-0040-0007:
 * <ul>
 *   <li>{@code subagent-start} with 3 args emits {@code subagent.start}
 *       with {@code metadata.role}.</li>
 *   <li>{@code subagent-end} with 4 args emits {@code subagent.end} carrying
 *       {@code status} and {@code metadata.role}.</li>
 *   <li>Invalid sub-command falls back to fail-open.</li>
 * </ul>
 */
class TelemetrySubagentHelperIT {

    private static final String SCHEMA_CLASSPATH =
            "/shared/templates/_TEMPLATE-TELEMETRY-EVENT.json";

    private static final Path HOOKS_DIR = Paths.get(
            "src/main/resources/targets/claude/hooks");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonSchema schema;

    @BeforeAll
    static void loadSchema() throws IOException {
        try (InputStream in =
                TelemetrySubagentHelperIT.class.getResourceAsStream(
                        SCHEMA_CLASSPATH)) {
            assertThat(in)
                    .as("Schema must exist at " + SCHEMA_CLASSPATH)
                    .isNotNull();
            JsonNode node = MAPPER.readTree(in);
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                    SpecVersion.VersionFlag.V202012);
            schema = factory.getSchema(node);
        }
    }

    @Test
    @DisplayName("subagentStartAndEnd_emitsSchemaValidEventsWithRole")
    void subagentStartAndEnd_emitsSchemaValidEventsWithRole(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        runPhaseHelper(projectDir,
                "subagent-start", "x-story-plan", "Architect");
        runPhaseHelper(projectDir,
                "subagent-end", "x-story-plan", "Architect", "ok");

        Path ndjson = projectDir.resolve(
                "plans/epic-0040/telemetry/events.ndjson");
        assertThat(ndjson).as("NDJSON output must exist").exists();

        List<String> lines = Files.readAllLines(
                ndjson, StandardCharsets.UTF_8);
        assertThat(lines)
                .as("Expected exactly two subagent events")
                .hasSize(2);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Set<ValidationMessage> errors = schema.validate(
                    line, InputFormat.JSON);
            assertThat(errors)
                    .as("Line " + (i + 1)
                            + " must validate against schema: " + line)
                    .isEmpty();
        }

        JsonNode startEvent = MAPPER.readTree(lines.get(0));
        assertThat(startEvent.get("type").asText())
                .isEqualTo("subagent.start");
        assertThat(startEvent.get("skill").asText())
                .isEqualTo("x-story-plan");
        assertThat(startEvent.path("metadata").path("role").asText())
                .isEqualTo("Architect");
        assertThat(startEvent.has("status")).isFalse();

        JsonNode endEvent = MAPPER.readTree(lines.get(1));
        assertThat(endEvent.get("type").asText())
                .isEqualTo("subagent.end");
        assertThat(endEvent.get("skill").asText())
                .isEqualTo("x-story-plan");
        assertThat(endEvent.path("metadata").path("role").asText())
                .isEqualTo("Architect");
        assertThat(endEvent.get("status").asText()).isEqualTo("ok");
    }

    @Test
    @DisplayName("subagentEndFailed_persistsStatusFailed")
    void subagentEndFailed_persistsStatusFailed(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        runPhaseHelper(projectDir,
                "subagent-end", "x-story-plan", "Security", "failed");

        List<String> lines = readNdjson(projectDir);
        assertThat(lines).hasSize(1);
        JsonNode event = MAPPER.readTree(lines.get(0));
        assertThat(event.get("type").asText())
                .isEqualTo("subagent.end");
        assertThat(event.get("status").asText()).isEqualTo("failed");
        assertThat(event.path("metadata").path("role").asText())
                .isEqualTo("Security");
    }

    @Test
    @DisplayName("invalidSubcommand_failsOpenAndWritesNothing")
    void invalidSubcommand_failsOpenAndWritesNothing(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        int exit = runHelperProcess(projectDir,
                "banana", "x-story-plan", "Architect");
        assertThat(exit)
                .as("Must exit 0 (fail-open) even on bad sub-command")
                .isZero();

        Path ndjson = projectDir.resolve(
                "plans/epic-0040/telemetry/events.ndjson");
        assertThat(Files.exists(ndjson))
                .as("No NDJSON file should be created on invalid args")
                .isFalse();
    }

    @Test
    @DisplayName("subagentStart_missingRole_failsOpen")
    void subagentStart_missingRole_failsOpen(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        int exit = runHelperProcess(projectDir,
                "subagent-start", "x-story-plan");
        assertThat(exit).isZero();

        Path ndjson = projectDir.resolve(
                "plans/epic-0040/telemetry/events.ndjson");
        assertThat(Files.exists(ndjson))
                .as("No NDJSON when role is missing")
                .isFalse();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private void runPhaseHelper(Path projectDir, String... args)
            throws Exception {
        int exit = runHelperProcess(projectDir, args);
        assertThat(exit)
                .as("Helper must exit 0 (fail-open)")
                .isZero();
    }

    private int runHelperProcess(Path projectDir, String... args)
            throws Exception {
        java.util.List<String> cmd = new java.util.ArrayList<>();
        cmd.add("bash");
        cmd.add(HOOKS_DIR.resolve("telemetry-phase.sh")
                .toAbsolutePath().toString());
        java.util.Collections.addAll(cmd, args);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.environment().put("CLAUDE_PROJECT_DIR",
                projectDir.toAbsolutePath().toString());
        pb.environment().put("CLAUDE_TELEMETRY_CONTEXT",
                "{\"epicId\":\"EPIC-0040\","
                        + "\"storyId\":\"story-0040-0007\"}");
        pb.redirectErrorStream(true);

        Process p = pb.start();
        boolean finished = p.waitFor(10, TimeUnit.SECONDS);
        assertThat(finished)
                .as("Helper did not finish in 10s")
                .isTrue();
        return p.exitValue();
    }

    private List<String> readNdjson(Path projectDir) throws IOException {
        Path ndjson = projectDir.resolve(
                "plans/epic-0040/telemetry/events.ndjson");
        assertThat(ndjson).exists();
        return Files.readAllLines(ndjson, StandardCharsets.UTF_8);
    }

    private void assumeBashAvailable() {
        assumeTrue(isToolAvailable("bash"),
                "bash not available on this platform");
    }

    private void assumeJqAvailable() {
        assumeTrue(isToolAvailable("jq"),
                "jq not available on this platform");
    }

    private void assumeHooksDirExists() {
        assumeTrue(Files.isDirectory(HOOKS_DIR),
                "Hooks source dir not found at " + HOOKS_DIR);
    }

    private boolean isToolAvailable(String tool) {
        try {
            Process p = new ProcessBuilder(
                    "bash", "-c",
                    "command -v " + tool + " >/dev/null 2>&1")
                    .start();
            return p.waitFor(5, TimeUnit.SECONDS)
                    && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
