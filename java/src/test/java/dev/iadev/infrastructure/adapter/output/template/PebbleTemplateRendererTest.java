package dev.iadev.infrastructure.adapter.output.template;

import dev.iadev.domain.port.output.TemplateRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PebbleTemplateRenderer")
class PebbleTemplateRendererTest {

    private PebbleTemplateRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new PebbleTemplateRenderer();
    }

    @Nested
    @DisplayName("TemplateRenderer contract")
    class TemplateRendererContract {

        @Test
        @DisplayName("implements TemplateRenderer port")
        void class_implements_templateRendererPort() {
            assertThat(renderer)
                    .isInstanceOf(TemplateRenderer.class);
        }
    }

    @Nested
    @DisplayName("render() — degenerate cases (@GK-1)")
    class RenderDegenerateCases {

        @Test
        @DisplayName("null templatePath throws "
                + "IllegalArgumentException")
        void render_nullPath_throwsIllegalArgument() {
            assertThatThrownBy(() ->
                    renderer.render(null, Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("templatePath");
        }

        @Test
        @DisplayName("blank templatePath throws "
                + "IllegalArgumentException")
        void render_blankPath_throwsIllegalArgument() {
            assertThatThrownBy(() ->
                    renderer.render("", Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("templatePath");
        }

        @Test
        @DisplayName("whitespace-only templatePath throws "
                + "IllegalArgumentException")
        void render_whitespacePath_throwsIllegalArgument() {
            assertThatThrownBy(() ->
                    renderer.render("   ", Map.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("templatePath");
        }

        @Test
        @DisplayName("null context throws "
                + "IllegalArgumentException")
        void render_nullContext_throwsIllegalArgument() {
            assertThatThrownBy(() ->
                    renderer.render(
                            "templates/simple.md.j2", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("context");
        }
    }

    @Nested
    @DisplayName("render() — happy path (@GK-2)")
    class RenderHappyPath {

        @Test
        @DisplayName("renders template with variables")
        void render_withVariables_rendersCorrectly() {
            String result = renderer.render(
                    "templates/simple.md.j2",
                    Map.of("project_name", "my-project",
                            "project_purpose",
                            "A CLI tool"));

            assertThat(result).contains("# my-project");
            assertThat(result).contains("A CLI tool");
        }

        @Test
        @DisplayName("renders template with empty context")
        void render_emptyContext_rendersTemplate() {
            String result = renderer.render(
                    "templates/simple.md.j2", Map.of());

            assertThat(result).contains("#");
        }
    }

    @Nested
    @DisplayName("render() — error path (@GK-3)")
    class RenderErrorPath {

        @Test
        @DisplayName("non-existent template throws exception")
        void render_nonExistentTemplate_throwsException() {
            assertThatThrownBy(() ->
                    renderer.render(
                            "nonexistent-template.peb",
                            Map.of()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(
                            "nonexistent-template.peb");
        }
    }

    @Nested
    @DisplayName("render() — Python-bool filter (@GK-5)")
    class PythonBoolFilter {

        @Test
        @DisplayName("true renders as 'True' (Python-style)")
        void render_boolTrue_rendersPythonTrue() {
            String result = renderer.render(
                    "templates/python-bool-test.peb",
                    Map.of("value", true));

            assertThat(result).contains("True");
            assertThat(result).doesNotContain("true");
        }

        @Test
        @DisplayName("false renders as 'False' (Python-style)")
        void render_boolFalse_rendersPythonFalse() {
            String result = renderer.render(
                    "templates/python-bool-test.peb",
                    Map.of("value", false));

            assertThat(result).contains("False");
            assertThat(result).doesNotContain("false");
        }
    }

    @Nested
    @DisplayName("templateExists()")
    class TemplateExists {

        @Test
        @DisplayName("returns true for existing template")
        void templateExists_existing_returnsTrue() {
            boolean result = renderer.templateExists(
                    "templates/simple.md.j2");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false for non-existing template")
        void templateExists_nonExisting_returnsFalse() {
            boolean result = renderer.templateExists(
                    "templates/nonexistent.md.j2");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("null templatePath throws "
                + "IllegalArgumentException")
        void templateExists_nullPath_throwsIllegalArgument() {
            assertThatThrownBy(() ->
                    renderer.templateExists(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("templatePath");
        }

        @Test
        @DisplayName("blank templatePath throws "
                + "IllegalArgumentException")
        void templateExists_blankPath_throwsIllegalArgument() {
            assertThatThrownBy(() ->
                    renderer.templateExists(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("templatePath");
        }
    }

    @Nested
    @DisplayName("Engine configuration parity")
    class EngineConfigParity {

        @Test
        @DisplayName("autoEscaping is disabled — "
                + "HTML chars pass through")
        void render_htmlChars_notEscaped() {
            String result = renderer.render(
                    "templates/simple.md.j2",
                    Map.of("project_name",
                            "<strong>bold</strong>",
                            "project_purpose", "test"));

            assertThat(result)
                    .contains("<strong>bold</strong>");
        }

        @Test
        @DisplayName("strictVariables is disabled — "
                + "missing variables render empty")
        void render_missingVariable_rendersEmpty() {
            String result = renderer.render(
                    "templates/simple.md.j2", Map.of());

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("newLineTrimming is disabled — "
                + "newlines preserved")
        void render_newlines_preserved() {
            String result = renderer.render(
                    "templates/simple.md.j2",
                    Map.of("project_name", "test",
                            "project_purpose", "purpose"));

            assertThat(result).contains("\n");
        }
    }
}
