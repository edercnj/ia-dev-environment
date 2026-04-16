package dev.iadev.telemetry.schema;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Parametrized verification that the canonical fixtures under
 * src/test/resources/fixtures/telemetry/ are classified correctly by the
 * published JSON Schema.
 *
 * <p>3 valid fixtures exercise the happy paths declared in story-0040-0001
 * (session.start minimal, skill.end with duration, tool.call with failure).
 * 2 invalid fixtures cover the boundary/error scenarios from Section 7
 * (invalid type value, negative durationMs).
 */
class TelemetryFixturesTest {

    private static final String SCHEMA_CLASSPATH =
            "/shared/templates/_TEMPLATE-TELEMETRY-EVENT.json";
    private static final String FIXTURES_CLASSPATH =
            "/fixtures/telemetry/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonSchema schema;

    @BeforeAll
    static void loadSchema() throws IOException {
        try (InputStream in =
                TelemetryFixturesTest.class.getResourceAsStream(
                        SCHEMA_CLASSPATH)) {
            assertThat(in).isNotNull();
            JsonNode schemaNode = MAPPER.readTree(in);
            JsonSchemaFactory factory =
                    JsonSchemaFactory.getInstance(
                            SpecVersion.VersionFlag.V202012);
            schema = factory.getSchema(schemaNode);
        }
    }

    @ParameterizedTest(name = "valid fixture {0} passes schema")
    @ValueSource(
            strings = {
                "session-start-minimal.json",
                "skill-end-with-duration.json",
                "tool-call-failed.json"
            })
    void validFixture_passesSchema(String fixtureName) throws IOException {
        String payload = readFixture(fixtureName);
        Set<ValidationMessage> messages =
                schema.validate(payload, InputFormat.JSON);
        assertThat(messages)
                .as(
                        "Fixture " + fixtureName
                                + " must be accepted by the schema")
                .isEmpty();
    }

    @ParameterizedTest(name = "invalid fixture {0} rejected by schema")
    @ValueSource(
            strings = {"invalid-type.json", "negative-duration.json"})
    void invalidFixture_rejectedBySchema(String fixtureName)
            throws IOException {
        String payload = readFixture(fixtureName);
        Set<ValidationMessage> messages =
                schema.validate(payload, InputFormat.JSON);
        assertThat(messages)
                .as(
                        "Fixture " + fixtureName
                                + " must be rejected by the schema")
                .isNotEmpty();
    }

    @Test
    @DisplayName("invalidType_errorCitesTypeField")
    void invalidType_errorCitesTypeField() throws IOException {
        String payload = readFixture("invalid-type.json");
        Set<ValidationMessage> messages =
                schema.validate(payload, InputFormat.JSON);
        assertThat(messages.toString()).contains("type");
    }

    @Test
    @DisplayName("negativeDuration_errorCitesDurationField")
    void negativeDuration_errorCitesDurationField() throws IOException {
        String payload = readFixture("negative-duration.json");
        Set<ValidationMessage> messages =
                schema.validate(payload, InputFormat.JSON);
        assertThat(messages.toString()).contains("durationMs");
    }

    private static String readFixture(String name) throws IOException {
        String path = FIXTURES_CLASSPATH + name;
        try (InputStream in =
                TelemetryFixturesTest.class.getResourceAsStream(path)) {
            assertThat(in)
                    .as("Fixture must exist at classpath " + path)
                    .isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
