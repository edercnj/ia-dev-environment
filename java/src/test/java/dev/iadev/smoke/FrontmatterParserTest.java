package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FrontmatterParser}.
 *
 * <p>Tests YAML frontmatter extraction from markdown
 * files. Covers valid frontmatter, missing frontmatter,
 * malformed YAML, and content-after-frontmatter
 * extraction.</p>
 */
@DisplayName("FrontmatterParser")
class FrontmatterParserTest {

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        @DisplayName("extracts valid frontmatter fields")
        void parse_validFrontmatter_extractsFields() {
            String content = """
                    ---
                    name: my-skill
                    description: "A test skill"
                    ---
                    # Content here
                    """;

            FrontmatterParser.Result result =
                    FrontmatterParser.parse(content);

            assertThat(result.hasFrontmatter()).isTrue();
            assertThat(result.fields())
                    .containsEntry("name", "my-skill")
                    .containsEntry("description",
                            "A test skill");
        }

        @Test
        @DisplayName("returns empty when no frontmatter")
        void parse_noFrontmatter_returnsEmpty() {
            String content = "# Just a heading\nSome text";

            FrontmatterParser.Result result =
                    FrontmatterParser.parse(content);

            assertThat(result.hasFrontmatter()).isFalse();
            assertThat(result.fields()).isEmpty();
        }

        @Test
        @DisplayName("returns empty for empty string")
        void parse_emptyString_returnsEmpty() {
            FrontmatterParser.Result result =
                    FrontmatterParser.parse("");

            assertThat(result.hasFrontmatter()).isFalse();
            assertThat(result.fields()).isEmpty();
        }

        @Test
        @DisplayName("handles multiline description")
        void parse_multilineDescription_extractsAll() {
            String content = """
                    ---
                    name: x-story-create
                    description: >
                      A multiline description that spans
                      multiple lines in YAML.
                    ---
                    # Content
                    """;

            FrontmatterParser.Result result =
                    FrontmatterParser.parse(content);

            assertThat(result.hasFrontmatter()).isTrue();
            assertThat(result.fields())
                    .containsKey("name")
                    .containsKey("description");
            assertThat(result.fields().get("description")
                    .toString()).isNotBlank();
        }

        @Test
        @DisplayName("handles allowed-tools as list")
        void parse_listField_extractsList() {
            String content = """
                    ---
                    name: test-skill
                    description: "desc"
                    allowed-tools:
                      - Read
                      - Write
                    ---
                    # Body
                    """;

            FrontmatterParser.Result result =
                    FrontmatterParser.parse(content);

            assertThat(result.hasFrontmatter()).isTrue();
            assertThat(result.fields())
                    .containsKey("allowed-tools");
        }

        @Test
        @DisplayName("returns empty when only opening "
                + "delimiter exists")
        void parse_onlyOpeningDelimiter_returnsEmpty() {
            String content = "---\nname: test\nno closing";

            FrontmatterParser.Result result =
                    FrontmatterParser.parse(content);

            assertThat(result.hasFrontmatter()).isFalse();
        }

        @Test
        @DisplayName("ignores content not starting with ---")
        void parse_noStartDelimiter_returnsEmpty() {
            String content = """
                    Some text
                    ---
                    name: test
                    ---
                    """;

            FrontmatterParser.Result result =
                    FrontmatterParser.parse(content);

            assertThat(result.hasFrontmatter()).isFalse();
        }
    }

    @Nested
    @DisplayName("bodyAfterFrontmatter")
    class BodyAfterFrontmatter {

        @Test
        @DisplayName("returns content after closing ---")
        void bodyAfterFrontmatter_withFrontmatter_returnsBody() {
            String content = """
                    ---
                    name: test
                    ---
                    # Heading
                    Body content here.
                    """;

            FrontmatterParser.Result result =
                    FrontmatterParser.parse(content);

            assertThat(result.body()).contains("# Heading");
            assertThat(result.body())
                    .contains("Body content here.");
        }

        @Test
        @DisplayName("returns full content when no "
                + "frontmatter")
        void bodyAfterFrontmatter_noFrontmatter_returnsAll() {
            String content = "# Just content\nNo frontmatter";

            FrontmatterParser.Result result =
                    FrontmatterParser.parse(content);

            assertThat(result.body())
                    .contains("# Just content");
        }

        @Test
        @DisplayName("returns empty body when content is "
                + "only frontmatter")
        void bodyAfterFrontmatter_onlyFrontmatter_emptyBody() {
            String content = """
                    ---
                    name: test
                    ---
                    """;

            FrontmatterParser.Result result =
                    FrontmatterParser.parse(content);

            assertThat(result.body().trim()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getField")
    class GetField {

        @Test
        @DisplayName("returns field value when present")
        void getField_present_returnsValue() {
            String content = """
                    ---
                    name: my-skill
                    description: "desc"
                    ---
                    # Body
                    """;

            FrontmatterParser.Result result =
                    FrontmatterParser.parse(content);

            assertThat(result.getField("name"))
                    .isPresent()
                    .hasValue("my-skill");
        }

        @Test
        @DisplayName("returns empty when field missing")
        void getField_missing_returnsEmpty() {
            String content = """
                    ---
                    name: my-skill
                    ---
                    # Body
                    """;

            FrontmatterParser.Result result =
                    FrontmatterParser.parse(content);

            assertThat(result.getField("description"))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns empty for null field value")
        void getField_nullValue_returnsEmpty() {
            String content = """
                    ---
                    name:
                    ---
                    # Body
                    """;

            FrontmatterParser.Result result =
                    FrontmatterParser.parse(content);

            assertThat(result.getField("name")).isEmpty();
        }
    }
}
