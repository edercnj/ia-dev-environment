package dev.iadev.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TemplateEngine")
class TemplateEngineTest {

    private TemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new TemplateEngine();
    }

    @Nested
    @DisplayName("renderString() with simple variables")
    class RenderStringSimple {

        @Test
        @DisplayName("renders single variable")
        void renderString_singleVariable_rendersValue() {
            String result = engine.renderString(
                    "Hello {{ name }}!",
                    Map.of("name", "World"));

            assertThat(result).isEqualTo("Hello World!");
        }

        @Test
        @DisplayName("renders multiple variables")
        void renderString_multipleVariables_rendersAll() {
            String result = engine.renderString(
                    "Hello {{ name }}, welcome to {{ project }}!",
                    Map.of("name", "World",
                            "project", "ia-dev-env"));

            assertThat(result)
                    .isEqualTo("Hello World, welcome to ia-dev-env!");
        }

        @Test
        @DisplayName("plain text without variables unchanged")
        void renderString_noVariables_returnsUnchanged() {
            String result = engine.renderString(
                    "plain text", Map.of());

            assertThat(result).isEqualTo("plain text");
        }

        @Test
        @DisplayName("empty string returns empty")
        void renderString_emptyString_returnsEmpty() {
            String result = engine.renderString("", Map.of());

            assertThat(result).isEqualTo("");
        }
    }

    // PythonBoolFilter integration class removed in EPIC-0048 full cleanup:
    // the Pebble `| python_bool` filter was dead code (no surviving template
    // referenced it). The ContextBuilder.toPythonBool(boolean) helper remains
    // as it is still used by ContextBuilder / ContextArchitectureBuilder to
    // format booleans as Python-style "True"/"False" strings in the template
    // context — that helper has dedicated tests in ContextBuilderTest.

    @Nested
    @DisplayName("replacePlaceholders()")
    class ReplacePlaceholders {

        @Test
        @DisplayName("replaces known keys")
        void replacePlaceholders_knownKeys_replacesAll() {
            String result = engine.replacePlaceholders(
                    "Stack: {LANGUAGE_NAME} {LANGUAGE_VERSION}",
                    Map.of("language_name", "java",
                            "language_version", "21"));

            assertThat(result).isEqualTo("Stack: java 21");
        }

        @Test
        @DisplayName("preserves unknown keys")
        void replacePlaceholders_unknownKey_preservesOriginal() {
            String result = engine.replacePlaceholders(
                    "{UNKNOWN_KEY}",
                    Map.of("other_key", "value"));

            assertThat(result).isEqualTo("{UNKNOWN_KEY}");
        }

        @Test
        @DisplayName("replaces known, preserves unknown")
        void replacePlaceholders_mixed_replacesOnlyKnown() {
            String result = engine.replacePlaceholders(
                    "{PROJECT_NAME} and {UNKNOWN}",
                    Map.of("project_name", "my-app"));

            assertThat(result)
                    .isEqualTo("my-app and {UNKNOWN}");
        }

        @Test
        @DisplayName("no placeholders returns unchanged")
        void replacePlaceholders_noPlaceholders_returnsUnchanged() {
            String result = engine.replacePlaceholders(
                    "plain text", Map.of());

            assertThat(result).isEqualTo("plain text");
        }

        @Test
        @DisplayName("empty string returns empty")
        void replacePlaceholders_emptyString_returnsEmpty() {
            String result = engine.replacePlaceholders(
                    "", Map.of());

            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("key lookup is case-insensitive")
        void replacePlaceholders_uppercaseKey_matchesLowercase() {
            String result = engine.replacePlaceholders(
                    "{PROJECT_NAME}",
                    Map.of("project_name", "my-app"));

            assertThat(result).isEqualTo("my-app");
        }

        @Test
        @DisplayName("non-string values converted via toString")
        void replacePlaceholders_intValue_convertedToString() {
            String result = engine.replacePlaceholders(
                    "{COVERAGE_LINE}",
                    Map.of("coverage_line", 95));

            assertThat(result).isEqualTo("95");
        }
    }

    @Nested
    @DisplayName("renderString() with conditionals")
    class Conditionals {

        @Test
        @DisplayName("if true renders block")
        void renderString_ifTrue_rendersBlock() {
            String template =
                    "{% if has_rest == 'True' %}REST API{% endif %}";
            String result = engine.renderString(
                    template, Map.of("has_rest", "True"));

            assertThat(result).isEqualTo("REST API");
        }

        @Test
        @DisplayName("if false skips block")
        void renderString_ifFalse_skipsBlock() {
            String template =
                    "{% if has_rest == 'True' %}REST API{% endif %}";
            String result = engine.renderString(
                    template, Map.of("has_rest", "False"));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("if-else renders correct branch")
        void renderString_ifElse_rendersCorrectBranch() {
            String template =
                    "{% if active %}yes{% else %}no{% endif %}";
            String result = engine.renderString(
                    template, Map.of("active", true));

            assertThat(result).isEqualTo("yes");
        }
    }

    @Nested
    @DisplayName("renderString() with loops")
    class Loops {

        @Test
        @DisplayName("for loop renders items")
        void renderString_forLoop_rendersItems() {
            String template =
                    "{% for item in items %}"
                            + "{{ item }} {% endfor %}";
            String result = engine.renderString(template,
                    Map.of("items",
                            List.of("alpha", "beta", "gamma")));

            assertThat(result).isEqualTo("alpha beta gamma ");
        }

        @Test
        @DisplayName("empty list renders nothing")
        void renderString_emptyList_rendersNothing() {
            String template =
                    "{% for item in items %}"
                            + "{{ item }}{% endfor %}";
            String result = engine.renderString(template,
                    Map.of("items", List.of()));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("render() from classpath")
    class RenderFromClasspath {

        @Test
        @DisplayName("renders template from classpath")
        void render_classpathTemplate_rendersCorrectly() {
            String result = engine.render(
                    "templates/simple.md.j2",
                    Map.of("project_name", "my-project",
                            "project_purpose",
                            "A CLI tool"));

            assertThat(result).contains("# my-project");
            assertThat(result).contains("A CLI tool");
        }

        @Test
        @DisplayName("non-existent template throws exception")
        void render_nonExistentTemplate_throwsException() {
            assertThatThrownBy(() ->
                    engine.render(
                            "templates/nonexistent.md.njk",
                            Map.of()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("nonexistent.md.njk");
        }
    }

    @Nested
    @DisplayName("render() from filesystem")
    class RenderFromFilesystem {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("renders template from filesystem path")
        void render_filesystemTemplate_rendersCorrectly()
                throws IOException {
            Path template = tempDir.resolve("test.md.j2");
            Files.writeString(template,
                    "# {{ title }}\n");

            TemplateEngine fsEngine =
                    new TemplateEngine(tempDir);

            String result = fsEngine.render(
                    "test.md.j2",
                    Map.of("title", "Hello"));

            assertThat(result).isEqualTo("# Hello\n");
        }
    }

    @Nested
    @DisplayName("autoEscaping disabled")
    class AutoEscaping {

        @Test
        @DisplayName("HTML chars are NOT escaped")
        void renderString_htmlChars_notEscaped() {
            String result = engine.renderString(
                    "{{ content }}",
                    Map.of("content",
                            "<strong>bold</strong>"));

            assertThat(result)
                    .isEqualTo("<strong>bold</strong>");
        }
    }

    @Nested
    @DisplayName("strictVariables disabled")
    class StrictVariables {

        @Test
        @DisplayName("missing variable renders empty string")
        void renderString_missingVar_rendersEmpty() {
            String result = engine.renderString(
                    "Hello {{ missing }}", Map.of());

            assertThat(result).isEqualTo("Hello ");
        }
    }

    @Nested
    @DisplayName("newLineTrimming disabled")
    class NewLineTrimming {

        @Test
        @DisplayName("newlines after tags are preserved")
        void renderString_newlineAfterTag_preserved() {
            String template =
                    "{% if true %}\nline\n{% endif %}\n";
            String result = engine.renderString(
                    template, Map.of());

            assertThat(result).isEqualTo("\nline\n\n");
        }
    }

    @Nested
    @DisplayName("injectSection (static)")
    class InjectSection {

        @Test
        @DisplayName("replaces marker with section")
        void injectSection_marker_replacesWithSection() {
            String result = TemplateEngine.injectSection(
                    "before MARKER after",
                    "INJECTED",
                    "MARKER");

            assertThat(result)
                    .isEqualTo("before INJECTED after");
        }

        @Test
        @DisplayName("replaces all occurrences")
        void injectSection_multipleMarkers_replacesAll() {
            String result = TemplateEngine.injectSection(
                    "A MARKER B MARKER C",
                    "X",
                    "MARKER");

            assertThat(result).isEqualTo("A X B X C");
        }

        @Test
        @DisplayName("missing marker returns unchanged")
        void injectSection_noMarker_returnsUnchanged() {
            String result = TemplateEngine.injectSection(
                    "no marker here",
                    "section",
                    "MISSING");

            assertThat(result).isEqualTo("no marker here");
        }

        @Test
        @DisplayName("empty section removes marker")
        void injectSection_emptySection_removesMarker() {
            String result = TemplateEngine.injectSection(
                    "before MARKER after",
                    "",
                    "MARKER");

            assertThat(result).isEqualTo("before  after");
        }
    }

    @Nested
    @DisplayName("concatFiles (static)")
    class ConcatFiles {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("concatenates two files with newline")
        void concatFiles_twoFiles_concatenatesWithNewline()
                throws IOException {
            Path fileA = tempDir.resolve("a.txt");
            Path fileB = tempDir.resolve("b.txt");
            Files.writeString(fileA, "content A");
            Files.writeString(fileB, "content B");

            String result = TemplateEngine.concatFiles(
                    List.of(fileA, fileB));

            assertThat(result)
                    .isEqualTo("content A\ncontent B");
        }

        @Test
        @DisplayName("custom separator used")
        void concatFiles_customSeparator_usesSeparator()
                throws IOException {
            Path fileA = tempDir.resolve("a.txt");
            Path fileB = tempDir.resolve("b.txt");
            Files.writeString(fileA, "A");
            Files.writeString(fileB, "B");

            String result = TemplateEngine.concatFiles(
                    List.of(fileA, fileB), "---");

            assertThat(result).isEqualTo("A---B");
        }

        @Test
        @DisplayName("empty list returns empty string")
        void concatFiles_emptyList_returnsEmpty() {
            String result = TemplateEngine.concatFiles(
                    List.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("single file returns content")
        void concatFiles_singleFile_returnsContent()
                throws IOException {
            Path file = tempDir.resolve("single.txt");
            Files.writeString(file, "only content");

            String result = TemplateEngine.concatFiles(
                    List.of(file));

            assertThat(result).isEqualTo("only content");
        }

        @Test
        @DisplayName("non-existent file throws exception")
        void concatFiles_nonExistentFile_throwsException() {
            Path missing = tempDir.resolve("missing.txt");

            assertThatThrownBy(() ->
                    TemplateEngine.concatFiles(
                            List.of(missing)))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
