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
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification tests for the canonical telemetry event JSON Schema
 * (Draft 2020-12). The schema is the single source of truth for telemetry
 * capture, persistence, and analysis.
 *
 * <p>Covers RULE-001 (Event Schema Versioning), RULE-002 (NDJSON Append-Only),
 * and RULE-008 (Source of Truth: Resources) at the contract level.
 */
class TelemetryEventSchemaTest {

    private static final String SCHEMA_CLASSPATH =
            "/shared/templates/_TEMPLATE-TELEMETRY-EVENT.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonSchema schema;
    private static JsonNode schemaNode;

    @BeforeAll
    static void loadSchema() throws IOException {
        try (InputStream in =
                TelemetryEventSchemaTest.class.getResourceAsStream(SCHEMA_CLASSPATH)) {
            assertThat(in)
                    .as(
                            "Schema file must exist at classpath "
                                    + SCHEMA_CLASSPATH)
                    .isNotNull();
            schemaNode = MAPPER.readTree(in);
        }
        JsonSchemaFactory factory =
                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        schema = factory.getSchema(schemaNode);
    }

    @Test
    @DisplayName("validate_emptyObject_rejectsMissingRequired")
    void validate_emptyObject_rejectsMissingRequired() {
        Set<ValidationMessage> messages =
                schema.validate("{}", InputFormat.JSON);

        assertThat(messages)
                .as("Empty object must be rejected")
                .isNotEmpty();
        String joined = messages.toString();
        assertThat(joined)
                .contains("schemaVersion")
                .contains("eventId")
                .contains("timestamp")
                .contains("sessionId")
                .contains("type");
    }

    @Test
    @DisplayName("schemaDeclaration_usesDraft202012")
    void schemaDeclaration_usesDraft202012() {
        assertThat(schemaNode.path("$schema").asText())
                .isEqualTo("https://json-schema.org/draft/2020-12/schema");
    }

    @Test
    @DisplayName("requiredFields_containsAllFiveMandatoryFields")
    void requiredFields_containsAllFiveMandatoryFields() {
        JsonNode required = schemaNode.path("required");
        assertThat(required.isArray()).isTrue();
        assertThat(required)
                .extracting(JsonNode::asText)
                .containsExactlyInAnyOrder(
                        "schemaVersion",
                        "eventId",
                        "timestamp",
                        "sessionId",
                        "type");
    }

    @Test
    @DisplayName("typeEnum_containsAllElevenCanonicalValues")
    void typeEnum_containsAllElevenCanonicalValues() {
        JsonNode typeEnum =
                schemaNode.path("properties").path("type").path("enum");
        assertThat(typeEnum.isArray()).isTrue();
        assertThat(typeEnum)
                .extracting(JsonNode::asText)
                .containsExactlyInAnyOrder(
                        "skill.start",
                        "skill.end",
                        "phase.start",
                        "phase.end",
                        "tool.call",
                        "tool.result",
                        "session.start",
                        "session.end",
                        "subagent.start",
                        "subagent.end",
                        "error");
    }

    @Test
    @DisplayName("schemaVersion_regexEnforcesSemver")
    void schemaVersion_regexEnforcesSemver() {
        String pattern =
                schemaNode
                        .path("properties")
                        .path("schemaVersion")
                        .path("pattern")
                        .asText();
        assertThat(pattern).isEqualTo("^\\d+\\.\\d+\\.\\d+$");
    }

    @Test
    @DisplayName("durationMs_hasMinimumZero")
    void durationMs_hasMinimumZero() {
        JsonNode durationMs =
                schemaNode.path("properties").path("durationMs");
        assertThat(durationMs.path("minimum").asInt()).isZero();
    }
}
