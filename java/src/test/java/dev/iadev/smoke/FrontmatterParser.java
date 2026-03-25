package dev.iadev.smoke;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Parses YAML frontmatter from markdown files.
 *
 * <p>Frontmatter is delimited by {@code ---} on the first
 * line and a closing {@code ---}. The YAML content between
 * the delimiters is parsed using SnakeYAML with
 * {@link SafeConstructor} for safe deserialization.</p>
 *
 * @see Result
 */
public final class FrontmatterParser {

    private static final String DELIMITER = "---";

    private FrontmatterParser() {
        // utility class
    }

    /**
     * Parses YAML frontmatter from the given markdown
     * content string.
     *
     * @param content the full markdown file content
     * @return a {@link Result} with parsed fields and body
     */
    public static Result parse(String content) {
        if (content == null || content.isEmpty()) {
            return new Result(
                    false, Collections.emptyMap(), content);
        }

        if (!content.startsWith(DELIMITER)) {
            return new Result(
                    false, Collections.emptyMap(), content);
        }

        int firstDelimEnd = content.indexOf('\n');
        if (firstDelimEnd < 0) {
            return new Result(
                    false, Collections.emptyMap(), content);
        }

        int secondDelimStart = content.indexOf(
                "\n" + DELIMITER, firstDelimEnd);
        if (secondDelimStart < 0) {
            return new Result(
                    false, Collections.emptyMap(), content);
        }

        String yamlBlock = content.substring(
                firstDelimEnd + 1, secondDelimStart);
        int bodyStart = secondDelimStart
                + 1 + DELIMITER.length();
        String body = bodyStart < content.length()
                ? content.substring(bodyStart)
                : "";

        Map<String, Object> fields = parseYaml(yamlBlock);
        if (fields == null) {
            return new Result(
                    false, Collections.emptyMap(), content);
        }

        return new Result(true, fields, body);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseYaml(
            String yamlBlock) {
        try {
            Yaml yaml = new Yaml(new SafeConstructor(
                    new LoaderOptions()));
            Object parsed = yaml.load(yamlBlock);
            if (parsed instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * The result of parsing a markdown file for
     * YAML frontmatter.
     *
     * @param hasFrontmatter whether valid frontmatter
     *                       was found
     * @param fields         the parsed frontmatter fields,
     *                       empty if no frontmatter
     * @param body           the content after frontmatter,
     *                       or full content if none
     */
    public record Result(
            boolean hasFrontmatter,
            Map<String, Object> fields,
            String body) {

        /**
         * Returns the value of a frontmatter field as a
         * string, if present and non-null.
         *
         * @param fieldName the field name to look up
         * @return the field value, or empty if missing
         *         or null
         */
        public Optional<String> getField(String fieldName) {
            Object value = fields.get(fieldName);
            if (value == null) {
                return Optional.empty();
            }
            String stringValue = value.toString().trim();
            if (stringValue.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(stringValue);
        }
    }
}
