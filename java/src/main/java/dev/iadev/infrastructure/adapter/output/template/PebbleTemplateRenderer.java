package dev.iadev.infrastructure.adapter.output.template;

import dev.iadev.domain.port.output.TemplateRenderer;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Output adapter that implements {@link TemplateRenderer}
 * using the Pebble template engine.
 *
 * <p>Encapsulates all Pebble-specific configuration:
 * classpath loading, auto-escaping disabled, strict variables
 * disabled, newline trimming disabled, and the Python-bool
 * filter extension for byte-for-byte parity with the
 * TypeScript implementation.</p>
 *
 * <p><strong>Thread safety:</strong> This class is safe for
 * concurrent use. The {@link PebbleEngine} is immutable after
 * construction and template evaluation is thread-safe.</p>
 *
 * @see TemplateRenderer
 * @see PythonBoolExtension
 */
public final class PebbleTemplateRenderer
        implements TemplateRenderer {

    private final PebbleEngine engine;

    /**
     * Creates a renderer that loads templates from the
     * classpath with standard project configuration.
     */
    public PebbleTemplateRenderer() {
        this.engine = new PebbleEngine.Builder()
                .loader(new ClasspathLoader())
                .autoEscaping(false)
                .strictVariables(false)
                .newLineTrimming(false)
                .extension(new PythonBoolExtension())
                .build();
    }

    @Override
    public String render(
            String templatePath,
            Map<String, Object> context) {
        validateTemplatePath(templatePath);
        validateContext(context);

        try {
            PebbleTemplate compiled =
                    engine.getTemplate(templatePath);
            StringWriter writer = new StringWriter();
            compiled.evaluate(writer, context);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to render template: "
                            + templatePath, e);
        }
    }

    @Override
    public boolean templateExists(String templatePath) {
        validateTemplatePath(templatePath);
        try {
            engine.getTemplate(templatePath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void validateTemplatePath(
            String templatePath) {
        if (templatePath == null
                || templatePath.isBlank()) {
            throw new IllegalArgumentException(
                    "templatePath must not be null or blank");
        }
    }

    private static void validateContext(
            Map<String, Object> context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "context must not be null");
        }
    }
}
