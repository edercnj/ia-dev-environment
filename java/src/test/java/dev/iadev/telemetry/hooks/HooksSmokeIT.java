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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
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
 * End-to-end smoke test for the Phase 1 telemetry hook scripts.
 *
 * <p>Spawns each of the five hook scripts (telemetry-session,
 * telemetry-pretool, telemetry-posttool, telemetry-subagent,
 * telemetry-stop) as child processes with representative Claude Code
 * payloads and verifies that every resulting NDJSON line validates against
 * the canonical telemetry schema published by story-0040-0001.
 *
 * <p>This test is the primary coverage vehicle for the shell hooks. Bats
 * is not available in the baseline CI image, so the shell scripts are
 * exercised through {@link ProcessBuilder} and validated via the same
 * schema used by upstream Java tests. The test is skipped when bash or
 * jq are unavailable on the current OS (Windows developer boxes).
 */
class HooksSmokeIT {

    private static final String SCHEMA_CLASSPATH =
            "/shared/templates/_TEMPLATE-TELEMETRY-EVENT.json";

    private static final Path HOOKS_DIR = Paths.get(
            "src/main/resources/targets/claude/hooks");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonSchema schema;

    @BeforeAll
    static void loadSchema() throws IOException {
        try (InputStream in =
                HooksSmokeIT.class.getResourceAsStream(
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
    @DisplayName("fiveHooks_emitEventsThatValidateAgainstSchema")
    void fiveHooks_emitEventsThatValidateAgainstSchema(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        Path tmpDir = Files.createTempDirectory(projectDir, "tmpdir");

        runHook("telemetry-session.sh",
                "{\"session_id\":\"sess-abc\"}",
                projectDir, tmpDir);

        runHook("telemetry-pretool.sh",
                "{\"session_id\":\"sess-abc\","
                        + "\"tool_name\":\"Bash\","
                        + "\"tool_use_id\":\"use-1\"}",
                projectDir, tmpDir);

        // No sleep needed here: the downstream assertion on durationMs
        // (line ~144) accepts `isGreaterThanOrEqualTo(0)`, so a zero
        // diff between pretool/posttool is valid. Removed a 50 ms
        // Thread.sleep that violated Rule 05 (no sleep for async sync).

        runHook("telemetry-posttool.sh",
                "{\"session_id\":\"sess-abc\","
                        + "\"tool_name\":\"Bash\","
                        + "\"tool_use_id\":\"use-1\","
                        + "\"tool_response\":{\"is_error\":false}}",
                projectDir, tmpDir);

        runHook("telemetry-subagent.sh",
                "{\"session_id\":\"sess-abc\"}",
                projectDir, tmpDir);

        runHook("telemetry-stop.sh",
                "{\"session_id\":\"sess-abc\"}",
                projectDir, tmpDir);

        Path ndjson = projectDir.resolve(
                "plans/epic-0040/telemetry/events.ndjson");
        assertThat(ndjson)
                .as("NDJSON output file should exist")
                .exists();

        List<String> lines = Files.readAllLines(
                ndjson, StandardCharsets.UTF_8);
        assertThat(lines)
                .as("Expected 4 events: session.start, tool.call, "
                        + "subagent.end, session.end (pretool writes "
                        + "no event — only the start timestamp file).")
                .hasSize(4);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Set<ValidationMessage> errors = schema.validate(
                    line, InputFormat.JSON);
            assertThat(errors)
                    .as("Line " + (i + 1) + " must validate: " + line)
                    .isEmpty();
        }

        // Specific shape assertions.
        JsonNode sessionEvent = MAPPER.readTree(lines.get(0));
        assertThat(sessionEvent.get("type").asText())
                .isEqualTo("session.start");
        assertThat(sessionEvent.get("epicId").asText())
                .isEqualTo("EPIC-0040");

        JsonNode toolEvent = MAPPER.readTree(lines.get(1));
        assertThat(toolEvent.get("type").asText()).isEqualTo("tool.call");
        assertThat(toolEvent.get("tool").asText()).isEqualTo("Bash");
        assertThat(toolEvent.get("status").asText()).isEqualTo("ok");
        assertThat(toolEvent.get("durationMs").asInt())
                .as("Duration should be a non-negative int")
                .isGreaterThanOrEqualTo(0);

        JsonNode stopEvent = MAPPER.readTree(lines.get(3));
        assertThat(stopEvent.get("type").asText())
                .isEqualTo("session.end");
    }

    @Test
    @DisplayName("disableFlag_suppressesAllEmission")
    void disableFlag_suppressesAllEmission(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        Path tmpDir = Files.createTempDirectory(projectDir, "tmpdir");

        ProcessBuilder pb = new ProcessBuilder(
                "bash",
                HOOKS_DIR.resolve("telemetry-session.sh")
                        .toAbsolutePath().toString());
        pb.environment().put("CLAUDE_TELEMETRY_DISABLED", "1");
        pb.environment().put("CLAUDE_PROJECT_DIR",
                projectDir.toAbsolutePath().toString());
        pb.environment().put("TMPDIR",
                tmpDir.toAbsolutePath().toString());
        pb.redirectErrorStream(true);

        Process p = pb.start();
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(p.getOutputStream(),
                        StandardCharsets.UTF_8))) {
            w.write("{\"session_id\":\"sess-xyz\"}");
        }
        boolean finished = p.waitFor(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(p.exitValue()).isZero();

        Path telemDir = projectDir.resolve("plans");
        assertThat(Files.exists(telemDir))
                .as("When disabled, no plans/epic-XXXX/telemetry dir "
                        + "should be created")
                .isFalse();
    }

    @Test
    @DisplayName("emitHelper_scrubsAwsKeysAndJwts")
    void emitHelper_scrubsAwsKeysAndJwts(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        String payload = "{\"schemaVersion\":\"1.0.0\","
                + "\"eventId\":\"44444444-4444-4444-8444-444444444444\","
                + "\"timestamp\":\"2026-04-16T12:00:00Z\","
                + "\"sessionId\":\"sess-scrub\","
                + "\"type\":\"error\","
                + "\"epicId\":\"EPIC-0040\","
                + "\"failureReason\":\"leaked AKIAIOSFODNN7EXAMPLE and "
                + "eyJhbGciOiJIUzI1NiJ9.payload.signature\"}";

        ProcessBuilder pb = new ProcessBuilder(
                "bash",
                HOOKS_DIR.resolve("telemetry-emit.sh")
                        .toAbsolutePath().toString());
        pb.environment().put("CLAUDE_PROJECT_DIR",
                projectDir.toAbsolutePath().toString());
        pb.redirectErrorStream(true);

        Process p = pb.start();
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(p.getOutputStream(),
                        StandardCharsets.UTF_8))) {
            w.write(payload);
        }
        boolean finished = p.waitFor(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(p.exitValue()).isZero();

        Path ndjson = projectDir.resolve(
                "plans/epic-0040/telemetry/events.ndjson");
        assertThat(ndjson).exists();

        List<String> lines = Files.readAllLines(
                ndjson, StandardCharsets.UTF_8);
        assertThat(lines).hasSize(1);

        String written = lines.get(0);
        assertThat(written)
                .as("AWS key must be redacted")
                .doesNotContain("AKIAIOSFODNN7EXAMPLE");
        assertThat(written).contains("AKIA***REDACTED***");
        assertThat(written)
                .as("JWT must be redacted")
                .doesNotContain("eyJhbGciOiJIUzI1NiJ9");
        assertThat(written).contains("eyJ***REDACTED***");
    }

    @Test
    @DisplayName("contextResolution_fallsBackToUnknown_whenNothingIdentifies")
    void contextResolution_fallsBackToUnknown_whenNothingIdentifies(
            @TempDir Path projectDir) throws Exception {
        assumeBashAvailable();
        assumeJqAvailable();
        assumeHooksDirExists();

        Path tmpDir = Files.createTempDirectory(projectDir, "tmpdir");

        // Explicitly pass an empty CLAUDE_TELEMETRY_CONTEXT to bypass the
        // env-var path; rely on the lack of a git repo + no
        // execution-state.json to force the "unknown" fallback.
        ProcessBuilder pb = new ProcessBuilder(
                "bash",
                HOOKS_DIR.resolve("telemetry-session.sh")
                        .toAbsolutePath().toString());
        pb.environment().put("CLAUDE_PROJECT_DIR",
                projectDir.toAbsolutePath().toString());
        pb.environment().put("TMPDIR",
                tmpDir.toAbsolutePath().toString());
        // Do NOT set CLAUDE_TELEMETRY_CONTEXT.
        pb.redirectErrorStream(true);

        Process p = pb.start();
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(p.getOutputStream(),
                        StandardCharsets.UTF_8))) {
            w.write("{\"session_id\":\"sess-unk\"}");
        }
        boolean finished = p.waitFor(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();
        assertThat(p.exitValue()).isZero();

        Path ndjson = projectDir.resolve(
                "plans/unknown/telemetry/events.ndjson");
        assertThat(ndjson)
                .as("Unknown epic should land under plans/unknown/")
                .exists();

        List<String> lines = Files.readAllLines(
                ndjson, StandardCharsets.UTF_8);
        assertThat(lines).hasSize(1);

        JsonNode event = MAPPER.readTree(lines.get(0));
        // epicId may be null or the literal "unknown"; the schema allows both.
        JsonNode epicId = event.get("epicId");
        if (epicId != null && !epicId.isNull()) {
            assertThat(epicId.asText()).isEqualTo("unknown");
        }
    }

    private void runHook(String script, String payload,
            Path projectDir, Path tmpDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "bash",
                HOOKS_DIR.resolve(script).toAbsolutePath().toString());
        pb.environment().put("CLAUDE_PROJECT_DIR",
                projectDir.toAbsolutePath().toString());
        pb.environment().put("CLAUDE_TELEMETRY_CONTEXT",
                "{\"epicId\":\"EPIC-0040\","
                        + "\"storyId\":\"story-0040-0003\"}");
        pb.environment().put("TMPDIR",
                tmpDir.toAbsolutePath().toString());
        pb.redirectErrorStream(true);

        Process p = pb.start();
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(p.getOutputStream(),
                        StandardCharsets.UTF_8))) {
            w.write(payload);
        }
        boolean finished = p.waitFor(10, TimeUnit.SECONDS);
        assertThat(finished)
                .as("Hook " + script + " did not finish in 10s")
                .isTrue();
        assertThat(p.exitValue())
                .as("Hook " + script + " must exit 0 (fail-open)")
                .isZero();
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
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
