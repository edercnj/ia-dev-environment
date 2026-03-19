package dev.iadev.assembler;

import dev.iadev.domain.stack.StackMapping;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Assembles {@code .claude/settings.json} and
 * {@code .claude/settings.local.json} with permissions and
 * hook configurations.
 *
 * <p>This is the seventh assembler in the pipeline (position
 * 7 of 23 per RULE-005). It merges permission arrays from
 * multiple JSON source files based on the project's
 * language, build tool, infrastructure, data, and testing
 * configuration.</p>
 *
 * <p>Permission sources (merged in order):
 * <ol>
 *   <li>Base permissions (always included)</li>
 *   <li>Language/build-tool permissions</li>
 *   <li>Docker permissions (if container is docker/podman)</li>
 *   <li>Kubernetes permissions (if orchestrator is kubernetes)</li>
 *   <li>Docker Compose permissions (if orchestrator is docker-compose)</li>
 *   <li>Database permissions (if database has settings key)</li>
 *   <li>Cache permissions (if cache has settings key)</li>
 *   <li>Newman/testing permissions (if smoke tests enabled)</li>
 * </ol>
 *
 * <p>Hook configuration references the post-compile hook
 * script if one exists for the language/build-tool
 * combination.</p>
 *
 * <p>Example usage:
 * <pre>{@code
 * Assembler settings = new SettingsAssembler();
 * List<String> files = settings.assemble(
 *     config, engine, outputDir);
 * }</pre>
 * </p>
 *
 * @see Assembler
 * @see StackMapping
 */
public final class SettingsAssembler implements Assembler {

    private static final String SETTINGS_TEMPLATES_DIR =
            "settings-templates";
    private static final String SETTINGS_FILENAME =
            "settings.json";
    private static final String SETTINGS_LOCAL_FILENAME =
            "settings.local.json";
    private static final String BASE_FILE = "base.json";
    private static final String DOCKER_FILE = "docker.json";
    private static final String K8S_FILE = "kubernetes.json";
    private static final String COMPOSE_FILE =
            "docker-compose.json";
    private static final String NEWMAN_FILE =
            "testing-newman.json";
    private static final String CONTAINER_DOCKER = "docker";
    private static final String CONTAINER_PODMAN = "podman";
    private static final String ORCH_KUBERNETES = "kubernetes";
    private static final String ORCH_COMPOSE =
            "docker-compose";
    private static final int JSON_INDENT = 2;
    private static final int HOOK_TIMEOUT = 60;

    private final Path resourcesDir;

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
        List<String> raw =
                collectPermissions(config, templatesDir);
        List<String> permissions = deduplicate(raw);
        boolean hasHooks = !StackMapping.getHookTemplateKey(
                config.language().name(),
                config.framework().buildTool()).isEmpty();

        List<String> results = new ArrayList<>();
        results.add(writeSettings(
                outputDir, permissions, hasHooks));
        results.add(writeSettingsLocal(outputDir));
        return results;
    }

    /**
     * Collects permissions from all applicable JSON source
     * files based on the project configuration.
     *
     * @param config       the project configuration
     * @param templatesDir the settings templates directory
     * @return merged permission list (may contain duplicates)
     */
    List<String> collectPermissions(
            ProjectConfig config, Path templatesDir) {
        List<String> result = mergeFile(
                List.of(), BASE_FILE, templatesDir);
        String langKey = StackMapping.getSettingsLangKey(
                config.language().name(),
                config.framework().buildTool());
        if (!langKey.isEmpty()) {
            result = mergeFile(
                    result, langKey + ".json", templatesDir);
        }
        result = collectInfra(config, templatesDir, result);
        result = collectData(config, templatesDir, result);
        if (config.testing().smokeTests()) {
            result = mergeFile(
                    result, NEWMAN_FILE, templatesDir);
        }
        return result;
    }

    private List<String> collectInfra(
            ProjectConfig config,
            Path templatesDir,
            List<String> result) {
        String container =
                config.infrastructure().container();
        if (CONTAINER_DOCKER.equals(container)
                || CONTAINER_PODMAN.equals(container)) {
            result = mergeFile(
                    result, DOCKER_FILE, templatesDir);
        }
        String orch =
                config.infrastructure().orchestrator();
        if (ORCH_KUBERNETES.equals(orch)) {
            result = mergeFile(
                    result, K8S_FILE, templatesDir);
        } else if (ORCH_COMPOSE.equals(orch)) {
            result = mergeFile(
                    result, COMPOSE_FILE, templatesDir);
        }
        return result;
    }

    private List<String> collectData(
            ProjectConfig config,
            Path templatesDir,
            List<String> result) {
        String dbKey = StackMapping.getDatabaseSettingsKey(
                config.data().database().name());
        if (!dbKey.isEmpty()) {
            result = mergeFile(
                    result, dbKey + ".json", templatesDir);
        }
        String cacheKey = StackMapping.getCacheSettingsKey(
                config.data().cache().name());
        if (!cacheKey.isEmpty()) {
            result = mergeFile(
                    result, cacheKey + ".json", templatesDir);
        }
        return result;
    }

    private List<String> mergeFile(
            List<String> base,
            String filename,
            Path templatesDir) {
        Path filePath = templatesDir.resolve(filename);
        if (!Files.exists(filePath)) {
            return base;
        }
        List<String> overlay = readJsonArray(filePath);
        List<String> merged = new ArrayList<>(base);
        merged.addAll(overlay);
        return merged;
    }

    /**
     * Removes duplicates preserving insertion order.
     *
     * @param items the input list
     * @return deduplicated list
     */
    static List<String> deduplicate(List<String> items) {
        return new ArrayList<>(new LinkedHashSet<>(items));
    }

    /**
     * Reads a JSON file containing a string array.
     *
     * <p>Expects the file to contain a JSON array of strings
     * (e.g., {@code ["Bash(git *)", "Bash(ls *)"]}). Returns
     * an empty list if the file cannot be read or parsed.</p>
     *
     * @param filePath the JSON file to read
     * @return the parsed string array, or empty on error
     */
    static List<String> readJsonArray(Path filePath) {
        try {
            String text = Files.readString(
                    filePath, StandardCharsets.UTF_8);
            return parseJsonStringArray(text);
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * Parses a JSON string array without external
     * dependencies.
     *
     * <p>Handles the specific format used by settings
     * templates: a flat JSON array of strings.</p>
     *
     * @param json the JSON text
     * @return parsed list of strings
     */
    static List<String> parseJsonStringArray(String json) {
        String trimmed = json.trim();
        if (!trimmed.startsWith("[")
                || !trimmed.endsWith("]")) {
            return List.of();
        }
        String inner =
                trimmed.substring(1, trimmed.length() - 1)
                        .trim();
        if (inner.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < inner.length()) {
            i = skipWhitespace(inner, i);
            if (i >= inner.length()) {
                break;
            }
            if (inner.charAt(i) == '"') {
                int end = findClosingQuote(inner, i + 1);
                result.add(inner.substring(i + 1, end));
                i = end + 1;
            }
            i = skipToNextElement(inner, i);
        }
        return result;
    }

    private static int skipWhitespace(String s, int pos) {
        while (pos < s.length()
                && Character.isWhitespace(s.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static int findClosingQuote(String s, int pos) {
        while (pos < s.length()) {
            char c = s.charAt(pos);
            if (c == '\\') {
                pos += 2;
                continue;
            }
            if (c == '"') {
                return pos;
            }
            pos++;
        }
        return s.length();
    }

    private static int skipToNextElement(String s, int pos) {
        while (pos < s.length()
                && s.charAt(pos) != ',') {
            pos++;
        }
        return pos + 1;
    }

    private String writeSettings(
            Path outputDir,
            List<String> permissions,
            boolean hasHooks) {
        Path dest = outputDir.resolve(SETTINGS_FILENAME);
        String content = buildSettingsJson(
                permissions, hasHooks);
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
        String content = buildSettingsLocalJson();
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

    /**
     * Builds the settings.json content as a formatted JSON
     * string.
     *
     * @param permissions the list of allowed commands
     * @param hasHooks    whether to include hooks section
     * @return formatted JSON string
     */
    static String buildSettingsJson(
            List<String> permissions, boolean hasHooks) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(indent(1)).append("\"permissions\": {\n");
        sb.append(indent(2)).append("\"allow\": [\n");
        for (int i = 0; i < permissions.size(); i++) {
            sb.append(indent(3))
                    .append('"')
                    .append(escapeJson(permissions.get(i)))
                    .append('"');
            if (i < permissions.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append(indent(2)).append("]\n");
        if (hasHooks) {
            sb.append(indent(1)).append("},\n");
            appendHooksSection(sb);
        } else {
            sb.append(indent(1)).append("}\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private static void appendHooksSection(
            StringBuilder sb) {
        sb.append(indent(1)).append("\"hooks\": {\n");
        sb.append(indent(2))
                .append("\"PostToolUse\": [\n");
        sb.append(indent(3)).append("{\n");
        sb.append(indent(4))
                .append("\"matcher\": \"Write|Edit\",\n");
        sb.append(indent(4)).append("\"hooks\": [\n");
        sb.append(indent(5)).append("{\n");
        sb.append(indent(6))
                .append("\"type\": \"command\",\n");
        sb.append(indent(6))
                .append("\"command\": ")
                .append("\"\\\"$CLAUDE_PROJECT_DIR\\\"")
                .append("/.claude/hooks/")
                .append("post-compile-check.sh\",\n");
        sb.append(indent(6))
                .append("\"timeout\": ")
                .append(HOOK_TIMEOUT)
                .append(",\n");
        sb.append(indent(6))
                .append("\"statusMessage\": ")
                .append("\"Checking compilation...\"\n");
        sb.append(indent(5)).append("}\n");
        sb.append(indent(4)).append("]\n");
        sb.append(indent(3)).append("}\n");
        sb.append(indent(2)).append("]\n");
        sb.append(indent(1)).append("}\n");
    }

    /**
     * Builds the settings.local.json content.
     *
     * @return formatted JSON string with empty permissions
     */
    static String buildSettingsLocalJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(indent(1)).append("\"permissions\": {\n");
        sb.append(indent(2)).append("\"allow\": []\n");
        sb.append(indent(1)).append("}\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String indent(int level) {
        return "  ".repeat(level);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static Path resolveClasspathResources() {
        var url = SettingsAssembler.class.getClassLoader()
                .getResource(SETTINGS_TEMPLATES_DIR);
        if (url == null) {
            return Path.of("src/main/resources");
        }
        return Path.of(url.getPath()).getParent();
    }
}
