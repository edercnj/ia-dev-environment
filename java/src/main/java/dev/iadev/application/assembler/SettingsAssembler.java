package dev.iadev.application.assembler;

import dev.iadev.domain.stack.StackMapping;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembles {@code .claude/settings.json} and
 * {@code .claude/settings.local.json} with permissions and
 * hook configurations.
 *
 * <p>Coordinates three specialized collaborators:
 * <ul>
 *   <li>{@link PermissionCollector} — collects CLI
 *       permissions by profile</li>
 *   <li>{@link JsonSettingsBuilder} — builds JSON
 *       settings content</li>
 *   <li>{@link HookConfigBuilder} — configures post-compile
 *       hooks</li>
 * </ul>
 *
 * @see Assembler
 * @see PermissionCollector
 * @see JsonSettingsBuilder
 * @see HookConfigBuilder
 */
public final class SettingsAssembler implements Assembler {

    private static final String SETTINGS_TEMPLATES_DIR =
            "targets/claude/settings";
    private static final String SETTINGS_FILENAME =
            "settings.json";
    private static final String SETTINGS_LOCAL_FILENAME =
            "settings.local.json";

    private final Path resourcesDir;
    private final PermissionCollector permissionCollector;
    private final JsonSettingsBuilder jsonSettingsBuilder;

    /**
     * Creates a SettingsAssembler using classpath resources.
     */
    public SettingsAssembler() {
        this(resolveClasspathResources());
    }

    /**
     * Creates a SettingsAssembler with an explicit resources
     * directory.
     *
     * @param resourcesDir the base resources directory
     */
    public SettingsAssembler(Path resourcesDir) {
        this.resourcesDir = resourcesDir;
        this.permissionCollector = new PermissionCollector();
        this.jsonSettingsBuilder = new JsonSettingsBuilder();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates settings.json with merged permissions and
     * hook configuration, plus an empty settings.local.json.
     * Returns the list of generated file paths.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Path templatesDir = resourcesDir.resolve(
                SETTINGS_TEMPLATES_DIR);
        List<String> permissions =
                permissionCollector.collect(
                        config, templatesDir);
        HookPresence hookPresence = resolveHookPresence(
                config);

        List<String> results = new ArrayList<>();
        results.add(writeSettings(
                outputDir, permissions, hookPresence,
                config.telemetryEnabled()));
        results.add(writeSettingsLocal(outputDir));
        return results;
    }

    /**
     * Collects permissions from all applicable JSON source
     * files. Delegates to {@link PermissionCollector}.
     *
     * @param config       the project configuration
     * @param templatesDir the settings templates directory
     * @return merged permission list (may contain duplicates)
     */
    List<String> collectPermissions(
            ProjectConfig config, Path templatesDir) {
        return permissionCollector.collectRaw(
                config, templatesDir);
    }

    /**
     * Removes duplicates preserving insertion order.
     * Delegates to {@link PermissionCollector}.
     *
     * @param items the input list
     * @return deduplicated list
     */
    static List<String> deduplicate(List<String> items) {
        return PermissionCollector.deduplicate(items);
    }

    /**
     * Reads a JSON file containing a string array.
     * Delegates to {@link PermissionCollector}.
     *
     * @param filePath the JSON file to read
     * @return the parsed string array, or empty on error
     */
    static List<String> readJsonArray(Path filePath) {
        return PermissionCollector.readJsonArray(filePath);
    }

    /**
     * Parses a JSON string array without external
     * dependencies. Delegates to
     * {@link PermissionCollector}.
     *
     * @param json the JSON text
     * @return parsed list of strings
     */
    static List<String> parseJsonStringArray(String json) {
        return PermissionCollector.parseJsonStringArray(json);
    }

    /**
     * Builds the settings.json content as a formatted JSON
     * string with explicit telemetry control. Delegates to
     * {@link JsonSettingsBuilder}.
     *
     * @param permissions  the list of allowed commands
     * @param hookPresence whether to include hooks section
     * @param telemetryEnabled whether to emit telemetry hook
     *     entries
     * @return formatted JSON string
     */
    static String buildSettingsJson(
            List<String> permissions,
            HookPresence hookPresence,
            boolean telemetryEnabled) {
        return new JsonSettingsBuilder()
                .build(permissions, hookPresence,
                        telemetryEnabled);
    }

    /**
     * Builds the settings.local.json content.
     * Delegates to {@link JsonSettingsBuilder}.
     *
     * @return formatted JSON string with empty permissions
     */
    static String buildSettingsLocalJson() {
        return new JsonSettingsBuilder().buildLocal();
    }

    private static HookPresence resolveHookPresence(
            ProjectConfig config) {
        return HookPresence.of(
                !StackMapping.getHookTemplateKey(
                        config.language().name(),
                        config.framework().buildTool())
                        .isEmpty());
    }

    private String writeSettings(
            Path outputDir,
            List<String> permissions,
            HookPresence hookPresence,
            boolean telemetryEnabled) {
        Path dest = outputDir.resolve(SETTINGS_FILENAME);
        String content = jsonSettingsBuilder.build(
                permissions, hookPresence,
                telemetryEnabled);
        try {
            Files.writeString(
                    dest, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to write settings.json", e);
        }
        return dest.toString();
    }

    private String writeSettingsLocal(Path outputDir) {
        Path dest =
                outputDir.resolve(SETTINGS_LOCAL_FILENAME);
        String content = jsonSettingsBuilder.buildLocal();
        try {
            Files.writeString(
                    dest, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to write settings.local.json",
                    e);
        }
        return dest.toString();
    }

    private static Path resolveClasspathResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourceDir("shared")
                .getParent();
    }
}
