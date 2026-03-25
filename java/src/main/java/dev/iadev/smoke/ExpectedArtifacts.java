package dev.iadev.smoke;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads and provides typed access to the expected artifacts
 * manifest for all bundled profiles.
 *
 * <p>The manifest declares per-profile expectations: total
 * file count, expected directories, and file counts per
 * artifact category. Smoke tests use this to validate
 * pipeline output against concrete expectations.</p>
 *
 * <p>Usage:
 * <pre>{@code
 * var artifacts = ExpectedArtifacts.load(
 *     Path.of("expected-artifacts.json"));
 * ProfileArtifacts profile =
 *     artifacts.getProfile("java-quarkus");
 * }</pre>
 * </p>
 *
 * @see ProfileArtifacts
 */
public final class ExpectedArtifacts {

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    private final Map<String, ProfileArtifacts> profiles;

    private ExpectedArtifacts(
            Map<String, ProfileArtifacts> profiles) {
        this.profiles = Collections.unmodifiableMap(
                new LinkedHashMap<>(profiles));
    }

    /**
     * Loads the manifest from a filesystem path.
     *
     * @param jsonPath path to the JSON manifest file
     * @return loaded ExpectedArtifacts instance
     * @throws IllegalArgumentException if path is null
     *         or file does not exist
     * @throws IllegalStateException if JSON parsing fails
     */
    public static ExpectedArtifacts load(Path jsonPath) {
        validatePath(jsonPath);
        try {
            String content = Files.readString(jsonPath);
            return parseJson(content);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read manifest: "
                            + jsonPath, e);
        }
    }

    /**
     * Loads the manifest from the classpath.
     *
     * @param resourcePath classpath resource path
     * @return loaded ExpectedArtifacts instance
     * @throws IllegalArgumentException if resource is null
     * @throws IllegalStateException if loading fails
     */
    public static ExpectedArtifacts loadFromClasspath(
            String resourcePath) {
        if (resourcePath == null) {
            throw new IllegalArgumentException(
                    "Resource path must not be null");
        }
        try (InputStream is = ExpectedArtifacts.class
                .getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException(
                        "Resource not found: "
                                + resourcePath);
            }
            String content = new String(
                    is.readAllBytes());
            return parseJson(content);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load manifest from "
                            + "classpath: " + resourcePath,
                    e);
        }
    }

    /**
     * Returns the profile artifacts for the given name.
     *
     * @param name the profile name
     * @return the ProfileArtifacts for that profile
     * @throws IllegalArgumentException if name is null
     *         or profile not found
     */
    public ProfileArtifacts getProfile(String name) {
        if (name == null) {
            throw new IllegalArgumentException(
                    "Profile name must not be null");
        }
        ProfileArtifacts result = profiles.get(name);
        if (result == null) {
            throw new IllegalArgumentException(
                    "Profile not found: '%s'. "
                            + "Available profiles: %s"
                            .formatted(name,
                                    profiles.keySet()));
        }
        return result;
    }

    /**
     * Returns the set of all profile names in the manifest.
     *
     * @return unmodifiable set of profile name strings
     */
    public Set<String> getProfileNames() {
        return profiles.keySet();
    }

    private static void validatePath(Path jsonPath) {
        if (jsonPath == null) {
            throw new IllegalArgumentException(
                    "Manifest path must not be null");
        }
        if (!Files.exists(jsonPath)) {
            throw new IllegalArgumentException(
                    "Manifest file does not exist: "
                            + jsonPath);
        }
    }

    private static ExpectedArtifacts parseJson(
            String content) {
        try {
            JsonNode root = MAPPER.readTree(content);
            JsonNode profilesNode = root.get("profiles");
            if (profilesNode == null
                    || !profilesNode.isObject()) {
                throw new IllegalStateException(
                        "Failed to parse manifest: "
                                + "missing 'profiles' "
                                + "object");
            }

            Map<String, ProfileArtifacts> profiles =
                    new LinkedHashMap<>();
            var fields = profilesNode.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                profiles.put(entry.getKey(),
                        parseProfile(entry.getValue()));
            }
            return new ExpectedArtifacts(profiles);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse manifest JSON", e);
        }
    }

    private static ProfileArtifacts parseProfile(
            JsonNode node) {
        int totalFiles = node.get("totalFiles").asInt();
        List<String> directories = MAPPER.convertValue(
                node.get("directories"),
                new TypeReference<>() { });
        Map<String, Integer> categories = MAPPER.convertValue(
                node.get("categories"),
                new TypeReference<>() { });
        return new ProfileArtifacts(
                totalFiles, directories, categories);
    }
}
