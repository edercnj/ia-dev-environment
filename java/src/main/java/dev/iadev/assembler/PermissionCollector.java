package dev.iadev.assembler;

import dev.iadev.domain.stack.StackMapping;
import dev.iadev.model.ProjectConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Collects CLI permissions from JSON source files based on
 * the project configuration.
 *
 * <p>Merges permission arrays from multiple JSON files based
 * on language, build tool, infrastructure, data, and testing
 * configuration. Permission sources are merged in order:
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
 * @see SettingsAssembler
 */
public final class PermissionCollector {

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

    PermissionCollector() {
        // package-private constructor
    }

    /**
     * Collects permissions from all applicable JSON source
     * files based on the project configuration.
     *
     * @param config       the project configuration
     * @param templatesDir the settings templates directory
     * @return deduplicated permission list
     */
    List<String> collect(
            ProjectConfig config, Path templatesDir) {
        List<String> raw =
                collectRaw(config, templatesDir);
        return deduplicate(raw);
    }

    /**
     * Collects raw permissions (may contain duplicates).
     *
     * @param config       the project configuration
     * @param templatesDir the settings templates directory
     * @return merged permission list
     */
    List<String> collectRaw(
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
}
