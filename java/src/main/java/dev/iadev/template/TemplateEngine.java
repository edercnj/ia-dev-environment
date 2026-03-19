package dev.iadev.template;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.loader.DelegatingLoader;
import io.pebbletemplates.pebble.loader.FileLoader;
import io.pebbletemplates.pebble.loader.Loader;
import io.pebbletemplates.pebble.loader.StringLoader;
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
 * Template rendering engine wrapping Pebble with Jinja2/Nunjucks
 * compatibility.
 *
 * <p>Provides three rendering modes:
 * <ul>
 *   <li>{@link #render(String, Map)} — renders a template file
 *       from classpath or filesystem</li>
 *   <li>{@link #renderString(String, Map)} — renders an inline
 *       template string</li>
 *   <li>{@link #replacePlaceholders(String, Map)} — replaces
 *       legacy {@code {KEY}} placeholders</li>
 * </ul>
 *
 * <p>Configuration (matching Nunjucks behavior):
 * <ul>
 *   <li>autoEscaping disabled (Markdown/YAML output)</li>
 *   <li>strictVariables disabled (missing vars = empty)</li>
 *   <li>newLineTrimming disabled (preserve whitespace)</li>
 *   <li>{@code python_bool} filter registered (RULE-002)</li>
 * </ul>
 *
 * @see PythonBoolFilter
 */
public final class TemplateEngine {

    /**
     * Regex pattern for legacy {@code {PLACEHOLDER}}
     * replacement. Matches single-brace word placeholders,
     * matching the TypeScript {@code /\{(\w+)\}/g} pattern.
     */
    static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("(?<!\\{)\\{(\\w+)\\}(?!\\})");

    private final PebbleEngine fileEngine;
    private final PebbleEngine stringEngine;

    /**
     * Creates a template engine that loads templates from the
     * classpath only.
     */
    public TemplateEngine() {
        this((Path) null);
    }

    /**
     * Creates a template engine that loads templates from the
     * given filesystem directory and the classpath.
     *
     * @param basePath the base directory for filesystem
     *                 templates, or {@code null} for classpath
     *                 only
     */
    public TemplateEngine(Path basePath) {
        this.fileEngine = buildFileEngine(basePath);
        this.stringEngine = buildStringEngine();
    }

    /**
     * Renders a template file relative to the classpath or
     * filesystem base directory.
     *
     * @param templatePath relative path to the template file
     * @param context      template context variables
     * @return the rendered string
     * @throws RuntimeException if the template is not found
     *                          or rendering fails
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
     * Renders an inline template string with the given context.
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
     * Replaces legacy {@code {KEY}} placeholders in content
     * with values from the context map.
     *
     * <p>For each match, the placeholder key is converted to
     * lowercase and looked up in the context. If found, the
     * placeholder is replaced with the value's string
     * representation. If not found, the placeholder is
     * preserved verbatim.</p>
     *
     * @param content the content with placeholders
     * @param context the key-value map for replacement
     * @return the content with known placeholders replaced
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
     * @param baseContent the base content containing markers
     * @param section     the content to inject
     * @param marker      the marker string to replace
     * @return the content with all markers replaced
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
     * @throws RuntimeException if any file cannot be read
     */
    public static String concatFiles(List<Path> paths) {
        return concatFiles(paths, "\n");
    }

    /**
     * Reads and concatenates files with the given separator.
     *
     * @param paths     list of file paths to concatenate
     * @param separator the separator between file contents
     * @return the concatenated content, or empty for no files
     * @throws RuntimeException if any file cannot be read
     */
    public static String concatFiles(
            List<Path> paths, String separator) {
        if (paths.isEmpty()) {
            return "";
        }
        List<String> contents = new ArrayList<>(paths.size());
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

    private static PebbleEngine buildFileEngine(Path basePath) {
        List<Loader<?>> loaders = new ArrayList<>(2);
        if (basePath != null) {
            FileLoader fileLoader = new FileLoader();
            fileLoader.setPrefix(
                    basePath.toAbsolutePath().toString());
            loaders.add(fileLoader);
        }
        ClasspathLoader classpathLoader = new ClasspathLoader();
        loaders.add(classpathLoader);

        Loader<?> loader = loaders.size() == 1
                ? loaders.getFirst()
                : new DelegatingLoader(loaders);

        return new PebbleEngine.Builder()
                .loader(loader)
                .autoEscaping(false)
                .strictVariables(false)
                .newLineTrimming(false)
                .extension(new PythonBoolExtension())
                .build();
    }

    private static PebbleEngine buildStringEngine() {
        return new PebbleEngine.Builder()
                .loader(new StringLoader())
                .autoEscaping(false)
                .strictVariables(false)
                .newLineTrimming(false)
                .extension(new PythonBoolExtension())
                .build();
    }
}
