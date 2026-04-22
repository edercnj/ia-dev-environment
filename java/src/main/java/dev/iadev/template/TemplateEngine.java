package dev.iadev.template;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Template rendering engine wrapping Pebble with
 * Jinja2/Nunjucks compatibility.
 *
 * <p>Engine construction is delegated to
 * {@link TemplateEngineFactory}.</p>
 *
 * @see TemplateEngineFactory
 */
public final class TemplateEngine {

    /**
     * Regex pattern for legacy {@code {PLACEHOLDER}}
     * replacement.
     */
    static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile(
                    "(?<!\\{)\\{(\\w+)\\}(?!\\})");

    private final PebbleEngine fileEngine;
    private final PebbleEngine stringEngine;

    /**
     * Creates a template engine that loads templates from
     * the classpath only.
     */
    public TemplateEngine() {
        this((Path) null);
    }

    /**
     * Creates a template engine that loads templates from
     * the given filesystem directory and the classpath.
     *
     * @param basePath the base directory for filesystem
     *                 templates, or null for classpath only
     */
    public TemplateEngine(Path basePath) {
        this.fileEngine =
                TemplateEngineFactory
                        .buildFileEngine(basePath);
        this.stringEngine =
                TemplateEngineFactory.buildStringEngine();
    }

    /**
     * Renders a template file relative to the classpath or
     * filesystem base directory.
     *
     * @param templatePath relative path to the template
     * @param context      template context variables
     * @return the rendered string
     */
    public String render(
            String templatePath,
            Map<String, Object> context) {
        try {
            PebbleTemplate compiled =
                    fileEngine.getTemplate(templatePath);
            StringWriter writer = new StringWriter();
            compiled.evaluate(writer, context);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to render template: "
                            + templatePath, e);
        }
    }

    /**
     * Renders an inline template string with the given
     * context.
     *
     * @param template the template string
     * @param context  template context variables
     * @return the rendered string
     */
    public String renderString(
            String template,
            Map<String, Object> context) {
        try {
            PebbleTemplate compiled =
                    stringEngine.getTemplate(template);
            StringWriter writer = new StringWriter();
            compiled.evaluate(writer, context);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to render template string", e);
        }
    }

    /**
     * Replaces legacy {@code {KEY}} placeholders in
     * content with values from the context map.
     *
     * @param content the content with placeholders
     * @param context the key-value map for replacement
     * @return the content with placeholders replaced
     */
    public String replacePlaceholders(
            String content,
            Map<String, Object> context) {
        Matcher matcher =
                PLACEHOLDER_PATTERN.matcher(content);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase();
            Object value = context.get(key);
            if (value != null) {
                matcher.appendReplacement(
                        sb, Matcher.quoteReplacement(
                                value.toString()));
            } else {
                matcher.appendReplacement(
                        sb, Matcher.quoteReplacement(
                                matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Replaces all occurrences of a marker in base content
     * with the given section.
     *
     * @param baseContent the base content
     * @param section     the content to inject
     * @param marker      the marker string to replace
     * @return the content with markers replaced
     */
    public static String injectSection(
            String baseContent,
            String section,
            String marker) {
        return baseContent.replace(marker, section);
    }

    /**
     * Reads and concatenates files with a newline separator.
     *
     * @param paths list of file paths to concatenate
     * @return the concatenated content
     */
    public static String concatFiles(List<Path> paths) {
        return concatFiles(paths, "\n");
    }

    /**
     * Reads and concatenates files with the given separator.
     *
     * @param paths     list of file paths to concatenate
     * @param separator the separator between contents
     * @return the concatenated content
     */
    public static String concatFiles(
            List<Path> paths, String separator) {
        if (paths.isEmpty()) {
            return "";
        }
        List<String> contents =
                new ArrayList<>(paths.size());
        for (Path path : paths) {
            try {
                contents.add(Files.readString(
                        path, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Failed to read file: " + path, e);
            }
        }
        return String.join(separator, contents);
    }
}
