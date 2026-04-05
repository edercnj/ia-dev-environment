package dev.iadev.infrastructure.adapter.output.config;

import dev.iadev.domain.model.StackProfile;
import dev.iadev.domain.port.output.StackProfileRepository;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Output adapter that loads stack profiles from bundled YAML
 * configuration templates on the classpath.
 *
 * <p>Implements {@link StackProfileRepository} by scanning
 * {@code shared/config-templates/setup-config.*.yaml} resources,
 * parsing each with SnakeYAML (using {@link SafeConstructor}
 * for security), and mapping them to {@link StackProfile}
 * domain objects.</p>
 *
 * <p>Profiles are loaded eagerly at construction time and
 * cached in an unmodifiable map. The profile name is derived
 * from the filename: {@code setup-config.{name}.yaml}.</p>
 *
 * @see StackProfileRepository
 * @see StackProfile
 */
public final class YamlStackProfileRepository
        implements StackProfileRepository {

    private static final String TEMPLATE_DIR =
            "shared/config-templates";
    private static final String FILE_PREFIX =
            "setup-config.";
    private static final String FILE_SUFFIX = ".yaml";

    private final Map<String, StackProfile> profiles;

    /**
     * Creates a new repository by loading all bundled YAML
     * profile templates from the classpath.
     *
     * @throws IllegalStateException if profile loading fails
     */
    public YamlStackProfileRepository() {
        this.profiles = Collections.unmodifiableMap(
                loadBundledProfiles());
    }

    @Override
    public List<StackProfile> findAll() {
        return List.copyOf(profiles.values());
    }

    @Override
    public Optional<StackProfile> findByName(
            String profileName) {
        validateProfileName(profileName);
        return Optional.ofNullable(
                profiles.get(profileName));
    }

    @Override
    public boolean exists(String profileName) {
        validateProfileName(profileName);
        return profiles.containsKey(profileName);
    }

    private Map<String, StackProfile> loadBundledProfiles() {
        Map<String, StackProfile> result =
                new LinkedHashMap<>();
        List<String> profileNames = discoverProfileNames();
        for (String name : profileNames) {
            StackProfile profile = loadProfile(name);
            if (profile != null) {
                result.put(name, profile);
            }
        }
        return result;
    }

    private List<String> discoverProfileNames() {
        URL dirUrl = getClass().getClassLoader()
                .getResource(TEMPLATE_DIR);
        if (dirUrl == null) {
            return List.of();
        }
        return scanDirectory(dirUrl);
    }

    private List<String> scanDirectory(URL dirUrl) {
        try {
            URI dirUri = dirUrl.toURI();
            if (isJarUri(dirUri)) {
                return scanJarDirectory(dirUri);
            }
            return scanFilesystemDirectory(dirUri);
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(
                    "Failed to scan config templates "
                            + "directory: " + dirUrl, e);
        }
    }

    private boolean isJarUri(URI uri) {
        return "jar".equals(uri.getScheme());
    }

    private List<String> scanJarDirectory(URI dirUri)
            throws IOException {
        try (FileSystem fs = getOrCreateFileSystem(dirUri)) {
            Path dirPath = fs.getPath(TEMPLATE_DIR);
            return listProfileNames(dirPath);
        }
    }

    private FileSystem getOrCreateFileSystem(URI dirUri)
            throws IOException {
        try {
            return FileSystems.getFileSystem(dirUri);
        } catch (FileSystemNotFoundException e) {
            return FileSystems.newFileSystem(
                    dirUri, Map.of());
        }
    }

    private List<String> scanFilesystemDirectory(URI dirUri)
            throws IOException {
        return listProfileNames(Path.of(dirUri));
    }

    private List<String> listProfileNames(Path dirPath)
            throws IOException {
        try (Stream<Path> entries = Files.list(dirPath)) {
            return entries
                    .map(p -> p.getFileName().toString())
                    .filter(this::isProfileFile)
                    .map(this::extractProfileName)
                    .sorted()
                    .toList();
        }
    }

    private boolean isProfileFile(String fileName) {
        return fileName.startsWith(FILE_PREFIX)
                && fileName.endsWith(FILE_SUFFIX);
    }

    private String extractProfileName(String fileName) {
        return fileName.substring(
                FILE_PREFIX.length(),
                fileName.length() - FILE_SUFFIX.length());
    }

    @SuppressWarnings("unchecked")
    private StackProfile loadProfile(String profileName) {
        String resourcePath = TEMPLATE_DIR + "/"
                + FILE_PREFIX + profileName + FILE_SUFFIX;
        try (InputStream is = openResource(resourcePath)) {
            Map<String, Object> yamlMap =
                    parseYaml(is, resourcePath);
            return mapToStackProfile(
                    profileName, yamlMap);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load profile '%s' from %s"
                            .formatted(
                                    profileName,
                                    resourcePath),
                    e);
        }
    }

    private InputStream openResource(String resourcePath) {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalStateException(
                    "Config template not found on "
                            + "classpath: " + resourcePath);
        }
        return is;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYaml(
            InputStream is, String resourcePath) {
        Object parsed = new Yaml(new SafeConstructor(
                new LoaderOptions())).load(is);
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new IllegalStateException(
                    "Config template is not a valid "
                            + "YAML map: " + resourcePath);
        }
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    private StackProfile mapToStackProfile(
            String profileName,
            Map<String, Object> yamlMap) {
        String language = extractNestedString(
                yamlMap, "language", "name");
        String framework = extractNestedString(
                yamlMap, "framework", "name");
        String buildTool = extractNestedString(
                yamlMap, "framework", "build_tool");

        Map<String, Object> properties =
                new LinkedHashMap<>(yamlMap);

        return new StackProfile(
                profileName,
                language,
                framework,
                buildTool,
                properties);
    }

    @SuppressWarnings("unchecked")
    private String extractNestedString(
            Map<String, Object> root,
            String section,
            String field) {
        Object sectionObj = root.get(section);
        if (!(sectionObj instanceof Map<?, ?> sectionMap)) {
            return "";
        }
        Object value =
                ((Map<String, Object>) sectionMap).get(field);
        return value != null ? value.toString() : "";
    }

    private static void validateProfileName(
            String profileName) {
        if (profileName == null
                || profileName.isBlank()) {
            throw new IllegalArgumentException(
                    "Profile name must not be null or blank");
        }
    }
}
