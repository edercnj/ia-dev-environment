package dev.iadev.telemetry.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * End-to-end smoke test: build a telemetry event in-process (the same way a
 * producer in story-0040-0002 will), serialize it, and validate against the
 * canonical schema. The smoke test MUST complete quickly (< 2 s) so it can
 * run on every CI invocation.
 */
class SchemaSmokeTest {

    private static final String SCHEMA_CLASSPATH =
            "/shared/templates/_TEMPLATE-TELEMETRY-EVENT.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonSchema schema;

    @BeforeAll
    static void loadSchema() throws IOException {
        try (InputStream in =
                SchemaSmokeTest.class.getResourceAsStream(SCHEMA_CLASSPATH)) {
            assertThat(in).isNotNull();
            JsonSchemaFactory factory =
                    JsonSchemaFactory.getInstance(
                            SpecVersion.VersionFlag.V202012);
            schema = factory.getSchema(in);
        }
    }

    @Test
    @DisplayName("smoke_builtInlineEvent_validatesAgainstSchema")
    void smoke_builtInlineEvent_validatesAgainstSchema() {
        Instant start = Instant.now();

        ObjectNode event = buildValidEvent();

        Set<ValidationMessage> messages = schema.validate(event);
        assertThat(messages)
                .as("Inline-built canonical event must validate")
                .isEmpty();

        JsonNode parsed = MAPPER.valueToTree(event);
        assertThat(parsed.get("type").asText()).isEqualTo("skill.end");
        assertThat(parsed.get("status").asText()).isEqualTo("ok");

        Duration elapsed = Duration.between(start, Instant.now());
        assertThat(elapsed)
                .as("Smoke test must finish under 2 seconds")
                .isLessThan(Duration.ofSeconds(2));
    }

    @Test
    @DisplayName("smoke_builtInlineEvent_hasRequiredFields")
    void smoke_builtInlineEvent_hasRequiredFields() {
        ObjectNode event = buildValidEvent();

        assertThat(event.has("schemaVersion")).isTrue();
        assertThat(event.has("eventId")).isTrue();
        assertThat(event.has("timestamp")).isTrue();
        assertThat(event.has("sessionId")).isTrue();
        assertThat(event.has("type")).isTrue();
    }

    private static ObjectNode buildValidEvent() {
        ObjectNode event = MAPPER.createObjectNode();
        event.put("schemaVersion", "1.0.0");
        event.put("eventId", newUuidV4());
        event.put(
                "timestamp",
                Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
        event.put("sessionId", "smoke-session-001");
        event.put("type", "skill.end");
        event.put("skill", "x-story-implement");
        event.put("durationMs", 42L);
        event.put("status", "ok");
        return event;
    }

    private static String newUuidV4() {
        // Java's UUID.randomUUID() emits a random version-4 UUID, which
        // satisfies the schema's UUIDv4 pattern check.
        return UUID.randomUUID().toString();
    }
}
