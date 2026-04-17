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
 * End-to-end smoke tests for {@code telemetry-phase.sh} (story-0040-0006).
 *
 * <p>Covers the CLI contract declared in §5.1 of the story:
 * <ul>
 *   <li>{@code start} with 3 args emits {@code phase.start}.</li>
 *   <li>{@code end} with 4 args emits {@code phase.end} carrying
 *       {@code status}.</li>
 *   <li>Invalid first-arg is swallowed (fail-open).</li>
 *   <li>{@code CLAUDE_TELEMETRY_DISABLED=1} suppresses emission.</li>
 *   <li>Missing emit helper (renamed on disk) does NOT abort the skill
 *       (fail-open).</li>
 *   <li>Every emitted line validates against the canonical telemetry
 *       event schema.</li>
 * </ul>
 */
class TelemetryPhaseHelperIT {

    private static final String SCHEMA_CLASSPATH =
            "/shared/templates/_TEMPLATE-TELEMETRY-EVENT.json";

    private static final Path HOOKS_DIR = Paths.get(
            "src/main/resources/targets/claude/hooks");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonSchema schema;

    @BeforeAll
    static void loadSchema() throws IOException {
        try (InputStream in =
                TelemetryPhaseHelperIT.class.getResourceAsStream(
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
    @DisplayName("startThenEnd_emitsTwoSchemaValidEventsWithSkillAndPhase")
    void startThenEnd_emitsTwoSchemaValidEventsWithSkillAndPhase(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        runPhaseHelper(projectDir,
                "start", "x-dev-story-implement", "Phase-1-Plan");
        runPhaseHelper(projectDir,
                "end", "x-dev-story-implement", "Phase-1-Plan", "ok");

        Path ndjson = projectDir.resolve(
                "plans/epic-0040/telemetry/events.ndjson");
        assertThat(ndjson).as("NDJSON output must exist").exists();

        List<String> lines = Files.readAllLines(
                ndjson, StandardCharsets.UTF_8);
        assertThat(lines)
                .as("Expected exactly two events: phase.start + phase.end")
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
                .isEqualTo("phase.start");
        assertThat(startEvent.get("skill").asText())
                .isEqualTo("x-dev-story-implement");
        assertThat(startEvent.get("phase").asText())
                .isEqualTo("Phase-1-Plan");
        assertThat(startEvent.has("status")).isFalse();

        JsonNode endEvent = MAPPER.readTree(lines.get(1));
        assertThat(endEvent.get("type").asText())
                .isEqualTo("phase.end");
        assertThat(endEvent.get("skill").asText())
                .isEqualTo("x-dev-story-implement");
        assertThat(endEvent.get("phase").asText())
                .isEqualTo("Phase-1-Plan");
        assertThat(endEvent.get("status").asText()).isEqualTo("ok");
    }

    @Test
    @DisplayName("endWithStatusFailed_persistsStatusFailed")
    void endWithStatusFailed_persistsStatusFailed(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        runPhaseHelper(projectDir,
                "end", "x-dev-story-implement", "Phase-3-Review",
                "failed");

        List<String> lines = readNdjson(projectDir);
        assertThat(lines).hasSize(1);
        JsonNode event = MAPPER.readTree(lines.get(0));
        assertThat(event.get("type").asText()).isEqualTo("phase.end");
        assertThat(event.get("status").asText()).isEqualTo("failed");
    }

    @Test
    @DisplayName("invalidFirstArg_failsOpenAndWritesNothing")
    void invalidFirstArg_failsOpenAndWritesNothing(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        int exit = runHelperProcess(projectDir,
                "bogus", "x-dev-story-implement", "Phase-1-Plan");
        assertThat(exit)
                .as("Must exit 0 (fail-open) even on bad first arg")
                .isZero();

        Path ndjson = projectDir.resolve(
                "plans/epic-0040/telemetry/events.ndjson");
        assertThat(Files.exists(ndjson))
                .as("No NDJSON file should be created on invalid args")
                .isFalse();
    }

    @Test
    @DisplayName("disableFlag_suppressesAllEmission")
    void disableFlag_suppressesAllEmission(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        ProcessBuilder pb = new ProcessBuilder(
                "bash",
                HOOKS_DIR.resolve("telemetry-phase.sh")
                        .toAbsolutePath().toString(),
                "start", "x-dev-story-implement", "Phase-1-Plan");
        pb.environment().put("CLAUDE_TELEMETRY_DISABLED", "1");
        pb.environment().put("CLAUDE_PROJECT_DIR",
                projectDir.toAbsolutePath().toString());
        pb.environment().put("CLAUDE_TELEMETRY_CONTEXT",
                "{\"epicId\":\"EPIC-0040\"}");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        boolean finished = p.waitFor(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(p.exitValue()).isZero();

        Path plans = projectDir.resolve("plans");
        assertThat(Files.exists(plans))
                .as("When disabled, nothing must be written to disk")
                .isFalse();
    }

    @Test
    @DisplayName("missingEmitHelper_skillKeepsRunningAndNothingPersisted")
    void missingEmitHelper_skillKeepsRunningAndNothingPersisted(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();

        // Stage a sandbox hook dir that omits telemetry-emit.sh so the
        // helper has to fall through the fail-open path.
        Path sandboxHooks = projectDir.resolve("hooks");
        Files.createDirectories(sandboxHooks);
        Files.copy(
                HOOKS_DIR.resolve("telemetry-phase.sh"),
                sandboxHooks.resolve("telemetry-phase.sh"));
        Files.copy(
                HOOKS_DIR.resolve("telemetry-lib.sh"),
                sandboxHooks.resolve("telemetry-lib.sh"));
        sandboxHooks.resolve("telemetry-phase.sh").toFile()
                .setExecutable(true);
        // Intentionally do NOT copy telemetry-emit.sh.

        ProcessBuilder pb = new ProcessBuilder(
                "bash",
                sandboxHooks.resolve("telemetry-phase.sh")
                        .toAbsolutePath().toString(),
                "start", "x-dev-story-implement", "Phase-1-Plan");
        pb.environment().put("CLAUDE_PROJECT_DIR",
                projectDir.toAbsolutePath().toString());
        pb.environment().put("CLAUDE_TELEMETRY_CONTEXT",
                "{\"epicId\":\"EPIC-0040\"}");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        boolean finished = p.waitFor(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(p.exitValue())
                .as("Fail-open: missing emit helper must still exit 0")
                .isZero();

        Path ndjson = projectDir.resolve(
                "plans/epic-0040/telemetry/events.ndjson");
        assertThat(Files.exists(ndjson))
                .as("No NDJSON should be written when emit helper is gone")
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
                        + "\"storyId\":\"story-0040-0006\"}");
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
