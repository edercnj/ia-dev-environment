package dev.iadev.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests that validate the Pebble template for CLAUDE.md
 * parses and renders cleanly with a dummy context.
 *
 * <p>Part of EPIC-0048 / story-0048-0010. The template is
 * consumed in story-0048-0011 by the new ClaudeMdAssembler;
 * this test is the RED-first contract gate per ADR-0048-B.
 */
@DisplayName("CLAUDE.md template (Pebble)")
class ClaudeMdTemplateSyntaxTest {

    private static final Path TEMPLATE_PATH = Path.of(
            "src/main/resources/shared/templates/CLAUDE.md");

    private TemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new TemplateEngine();
    }

    @Nested
    @DisplayName("existence and readability")
    class FilePresence {

        @Test
        @DisplayName("template file exists on classpath resource path")
        void template_exists() {
            assertThat(TEMPLATE_PATH).exists();
        }

        @Test
        @DisplayName("template is readable and non-empty")
        void template_readable() throws IOException {
            String content = Files.readString(TEMPLATE_PATH);
            assertThat(content).isNotEmpty();
            assertThat(content.length())
                    .as("template should carry enough context")
                    .isGreaterThanOrEqualTo(200);
        }
    }

    @Nested
    @DisplayName("Pebble parsing + rendering")
    class PebbleSyntax {

        @Test
        @DisplayName("parses and renders without exception under full context")
        void template_rendersFullContext_doesNotThrow()
                throws IOException {
            String content = Files.readString(TEMPLATE_PATH);
            Map<String, Object> ctx = fullContext();

            assertThatCode(() ->
                    engine.renderString(content, ctx))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rendered output contains all mandatory section headers")
        void template_rendered_containsSections()
                throws IOException {
            String content = Files.readString(TEMPLATE_PATH);
            String rendered = engine.renderString(
                    content, fullContext());

            assertThat(rendered)
                    .contains("# demo")
                    .contains("## Project Overview")
                    .contains("## Build")
                    .contains("## Test")
                    .contains("## Architecture Notes")
                    .contains("## Key Rules")
                    .contains("## Related Skills");
        }

        @Test
        @DisplayName("placeholders substituted — full context")
        void template_rendered_substitutesAll()
                throws IOException {
            String content = Files.readString(TEMPLATE_PATH);
            String rendered = engine.renderString(
                    content, fullContext());

            assertThat(rendered)
                    .contains("demo")
                    .contains("java")
                    .contains("spring-boot")
                    .contains("hexagonal")
                    .contains("postgresql")
                    .contains("rest")
                    .contains("mvn package")
                    .contains("mvn test")
                    .doesNotContain("{{")
                    .doesNotContain("}}");
        }

        @Test
        @DisplayName("absent DATABASES — conditional block omits line")
        void template_rendered_omitsDatabasesWhenAbsent()
                throws IOException {
            String content = Files.readString(TEMPLATE_PATH);
            Map<String, Object> ctx = fullContext();
            ctx.put("DATABASES", "");

            String rendered = engine.renderString(
                    content, ctx);

            assertThat(rendered).doesNotContain("Databases:");
        }

        @Test
        @DisplayName("absent INTERFACE_TYPES — conditional block omits line")
        void template_rendered_omitsInterfacesWhenAbsent()
                throws IOException {
            String content = Files.readString(TEMPLATE_PATH);
            Map<String, Object> ctx = fullContext();
            ctx.put("INTERFACE_TYPES", "");

            String rendered = engine.renderString(
                    content, ctx);

            assertThat(rendered).doesNotContain("Interfaces:");
        }
    }

    private static Map<String, Object> fullContext() {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("PROJECT_NAME", "demo");
        ctx.put("LANGUAGE", "java");
        ctx.put("FRAMEWORK", "spring-boot");
        ctx.put("ARCHITECTURE", "hexagonal");
        ctx.put("DATABASES", "postgresql");
        ctx.put("INTERFACE_TYPES", "rest");
        ctx.put("BUILD_COMMAND", "mvn package");
        ctx.put("TEST_COMMAND", "mvn test");
        return ctx;
    }
}
