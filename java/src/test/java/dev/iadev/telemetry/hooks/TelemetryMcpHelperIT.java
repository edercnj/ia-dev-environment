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
 * End-to-end smoke tests for {@code telemetry-phase.sh mcp-start} /
 * {@code mcp-end} sub-commands (story-0040-0008 TASK-001).
 *
 * <p>Covers the CLI contract declared in §5.1 of story-0040-0008:
 * <ul>
 *   <li>{@code mcp-start} with 3 args writes a timer file and emits a
 *       {@code tool.call} event with {@code tool=mcp__atlassian__<method>}
 *       and {@code metadata.mcpMethod}.</li>
 *   <li>{@code mcp-end} with 4 args emits a {@code tool.call} event with
 *       {@code status} and {@code durationMs} (when timer is present).</li>
 *   <li>{@code mcp-end} without prior {@code mcp-start} emits the event
 *       without {@code durationMs} (fail-open).</li>
 *   <li>Missing mcpMethod is fail-open (exit 0, no NDJSON).</li>
 * </ul>
 */
class TelemetryMcpHelperIT {

    private static final String SCHEMA_CLASSPATH =
            "/shared/templates/_TEMPLATE-TELEMETRY-EVENT.json";

    private static final Path HOOKS_DIR = Paths.get(
            "src/main/resources/targets/claude/hooks");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonSchema schema;

    @BeforeAll
    static void loadSchema() throws IOException {
        try (InputStream in =
                TelemetryMcpHelperIT.class.getResourceAsStream(
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
    @DisplayName("mcpStartAndEnd_emitsSchemaValidToolCallEventsWithDuration")
    void mcpStartAndEnd_emitsSchemaValidToolCallEventsWithDuration(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        runPhaseHelper(projectDir,
                "mcp-start", "x-jira-create-stories", "createJiraIssue");
        // intentional workload simulation for timing test; not a sync
        // primitive. The shell helper uses `date +%s` on BSD (macOS),
        // which has second-level granularity, so two invocations within
        // the same wall-clock second produce durationMs=0 and fail the
        // `isGreaterThan(0)` assertion below. Forcing ≥1.1s of wall
        // clock is the only way to guarantee a strictly positive diff
        // across GNU (millis) and BSD (seconds) timer backends.
        Thread.sleep(1100);
        runPhaseHelper(projectDir,
                "mcp-end", "x-jira-create-stories", "createJiraIssue", "ok");

        Path ndjson = projectDir.resolve(
                "plans/epic-0040/telemetry/events.ndjson");
        assertThat(ndjson).as("NDJSON output must exist").exists();

        List<String> lines = Files.readAllLines(
                ndjson, StandardCharsets.UTF_8);
        assertThat(lines)
                .as("Expected exactly two tool.call events")
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
                .isEqualTo("tool.call");
        assertThat(startEvent.get("skill").asText())
                .isEqualTo("x-jira-create-stories");
        assertThat(startEvent.get("tool").asText())
                .isEqualTo("mcp__atlassian__createJiraIssue");
        assertThat(startEvent.path("metadata").path("mcpMethod").asText())
                .isEqualTo("createJiraIssue");
        assertThat(startEvent.has("status")).isFalse();
        assertThat(startEvent.has("durationMs")).isFalse();

        JsonNode endEvent = MAPPER.readTree(lines.get(1));
        assertThat(endEvent.get("type").asText())
                .isEqualTo("tool.call");
        assertThat(endEvent.get("skill").asText())
                .isEqualTo("x-jira-create-stories");
        assertThat(endEvent.get("tool").asText())
                .isEqualTo("mcp__atlassian__createJiraIssue");
        assertThat(endEvent.get("status").asText()).isEqualTo("ok");
        assertThat(endEvent.has("durationMs"))
                .as("durationMs must be present when mcp-start ran first")
                .isTrue();
        assertThat(endEvent.get("durationMs").asInt())
                .as("durationMs must be positive after 1.1s sleep")
                .isGreaterThan(0);
        assertThat(endEvent.path("metadata").path("mcpMethod").asText())
                .isEqualTo("createJiraIssue");
    }

    @Test
    @DisplayName("mcpEndWithoutStart_emitsEventWithoutDuration")
    void mcpEndWithoutStart_emitsEventWithoutDuration(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        // Use a unique method name so no stale timer file from other tests
        // interferes with this "no prior mcp-start" scenario.
        String uniqueMethod = "noStartMethod"
                + System.nanoTime();

        runPhaseHelper(projectDir,
                "mcp-end", "x-jira-create-epic", uniqueMethod, "failed");

        List<String> lines = readNdjson(projectDir);
        assertThat(lines).hasSize(1);
        JsonNode event = MAPPER.readTree(lines.get(0));
        assertThat(event.get("type").asText()).isEqualTo("tool.call");
        assertThat(event.get("status").asText()).isEqualTo("failed");
        assertThat(event.path("metadata").path("mcpMethod").asText())
                .isEqualTo(uniqueMethod);
        assertThat(event.has("durationMs"))
                .as("durationMs must be absent when timer was missing")
                .isFalse();
    }

    @Test
    @DisplayName("mcpStartMissingMethod_failsOpen")
    void mcpStartMissingMethod_failsOpen(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        int exit = runHelperProcess(projectDir,
                "mcp-start", "x-jira-create-stories");
        assertThat(exit).isZero();

        Path ndjson = projectDir.resolve(
                "plans/epic-0040/telemetry/events.ndjson");
        assertThat(Files.exists(ndjson))
                .as("No NDJSON when mcpMethod is missing")
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
                        + "\"storyId\":\"story-0040-0008\"}");
        // Isolate the MCP timer directory per test invocation so tests
        // don't share state via /tmp.
        pb.environment().put("TMPDIR",
                projectDir.toAbsolutePath().toString());
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
