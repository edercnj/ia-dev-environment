package dev.iadev.release.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NextAction} record.
 *
 * <p>Covers TPP-2 (constant) and TPP-4 (collection entry)
 * roundtrip per story-0039-0002 §7.
 */
@DisplayName("NextActionTest")
class NextActionTest {

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    @Test
    @DisplayName("record exposes label and command accessors")
    void recordExposesLabelAndCommand() {
        NextAction action = new NextAction(
                "Continue release",
                "/x-release --continue-after-merge");

        assertThat(action.label())
                .isEqualTo("Continue release");
        assertThat(action.command())
                .isEqualTo("/x-release --continue-after-merge");
    }

    @Test
    @DisplayName("record deserializes from JSON with"
            + " label and command keys")
    void recordDeserializesFromJson() throws Exception {
        String json = "{\"label\":\"L\","
                + "\"command\":\"/x-release\"}";

        NextAction action =
                MAPPER.readValue(json, NextAction.class);

        assertThat(action.label()).isEqualTo("L");
        assertThat(action.command()).isEqualTo("/x-release");
    }

    @Test
    @DisplayName("record serializes to JSON with"
            + " label and command keys")
    void recordSerializesToJson() throws Exception {
        NextAction action = new NextAction(
                "Label", "/x-release --continue-after-merge");

        String json = MAPPER.writeValueAsString(action);

        assertThat(json)
                .contains("\"label\":\"Label\"")
                .contains("\"command\":"
                        + "\"/x-release --continue-after-merge\"");
    }
}
