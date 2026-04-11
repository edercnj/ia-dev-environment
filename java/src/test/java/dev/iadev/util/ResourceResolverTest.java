package dev.iadev.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ResourceResolver}.
 *
 * <p>Tests the exploded classpath (file protocol) path which
 * is the default when running from IDE or Maven Surefire.
 * JAR extraction and shutdown hook paths are not exercised
 * in unit tests.</p>
 */
@DisplayName("ResourceResolver")
class ResourceResolverTest {

    @Nested
    @DisplayName("resolveResourcesRoot — single arg")
    @SuppressWarnings("removal")
    class SingleArg {

        @Test
        @DisplayName("known probe returns existing path")
        void knownProbe_whenCalled_returnsExistingPath() {
            Path root = ResourceResolver
                    .resolveResourcesRoot(
                            "shared/config-templates");

            assertThat(root).isAbsolute();
            assertThat(Files.isDirectory(root)).isTrue();
            assertThat(root.getFileName().toString())
                    .isNotEmpty();
        }

        @Test
        @DisplayName("unknown probe falls back to default")
        void unknownProbe_whenCalled_fallsBackToDefault() {
            Path root = ResourceResolver
                    .resolveResourcesRoot(
                            "nonexistent-resource-xyz");

            assertThat(root.toString())
                    .isEqualTo("src/main/resources");
        }
    }

    @Nested
    @DisplayName("resolveResourcesRoot — two args")
    @SuppressWarnings("removal")
    class TwoArgs {

        @Test
        @DisplayName("depth 1 navigates to parent")
        void depth1_whenCalled_navigatesToParent() {
            Path root = ResourceResolver
                    .resolveResourcesRoot(
                            "shared/config-templates", 1);

            assertThat(root).isAbsolute();
            assertThat(Files.isDirectory(root)).isTrue();
            assertThat(root.getFileName().toString())
                    .isNotEmpty();
        }

        @Test
        @DisplayName("depth 0 returns probe directory")
        void depth0_whenCalled_returnsProbeDirectory() {
            Path root = ResourceResolver
                    .resolveResourcesRoot(
                            "shared/config-templates", 0);

            assertThat(root.getFileName().toString())
                    .isEqualTo("config-templates");
        }

        @Test
        @DisplayName("unknown probe with depth"
                + " falls back to default")
        void unknownProbeWithDepth_withDepthFallsBackToDefault_fallsBack() {
            Path root = ResourceResolver
                    .resolveResourcesRoot(
                            "no-such-dir-abc", 2);

            assertThat(root.toString())
                    .isEqualTo("src/main/resources");
        }

        @Test
        @DisplayName("depth 2 navigates to grandparent")
        void depth2_whenCalled_navigatesToGrandparent() {
            Path root = ResourceResolver
                    .resolveResourcesRoot(
                            "shared/config-templates", 2);

            assertThat(root).isAbsolute();
            assertThat(Files.isDirectory(root)).isTrue();
        }
    }

    @Nested
    @DisplayName("resolveResourcesRoot — consistency")
    @SuppressWarnings("removal")
    class Consistency {

        @Test
        @DisplayName("multiple calls return same path")
        void multipleCalls_whenCalled_returnSamePath() {
            Path first = ResourceResolver
                    .resolveResourcesRoot(
                            "shared/config-templates");
            Path second = ResourceResolver
                    .resolveResourcesRoot(
                            "shared/config-templates");

            assertThat(first).isEqualTo(second);
        }

        @Test
        @DisplayName("different probes both return"
                + " existing directories")
        void differentProbes_whenCalled_bothReturnDirs() {
            Path fromSkills = ResourceResolver
                    .resolveResourcesRoot(
                            "shared/config-templates");
            Path fromConfig = ResourceResolver
                    .resolveResourcesRoot(
                            "shared/config-templates");

            assertThat(Files.isDirectory(fromSkills))
                    .isTrue();
            assertThat(Files.isDirectory(fromConfig))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("resolveResourceDir")
    class ResolveResourceDir {

        // --- TPP: constant (simplest happy path) ---

        @Test
        @DisplayName("1-level path returns existing dir")
        void oneLevelPath_whenCalled_returnsExistingDir() {
            Path result = ResourceResolver
                    .resolveResourceDir("knowledge/core");

            assertThat(result).isAbsolute();
            assertThat(Files.isDirectory(result)).isTrue();
            assertThat(result.getFileName().toString())
                    .isEqualTo("core");
        }

        // --- TPP: scalar → collection (parametrized) ---

        @ParameterizedTest(name = "{0} resolves to dir {1}")
        @CsvSource({
                "knowledge/databases/cache, cache",
                "knowledge/databases/cache/redis, redis",
                "targets/claude/rules, rules",
                "shared/config-templates, config-templates"
        })
        @DisplayName("multi-level paths resolve correctly")
        void multiLevelPath_whenCalled_returnsExistingDir(
                String path, String expectedDirName) {
            Path result = ResourceResolver
                    .resolveResourceDir(path);

            assertThat(result).isAbsolute();
            assertThat(Files.isDirectory(result)).isTrue();
            assertThat(result.getFileName().toString())
                    .isEqualTo(expectedDirName);
        }

        // --- TPP: consistency check ---

        @Test
        @DisplayName("consistent with legacy method")
        @SuppressWarnings("removal")
        void consistent_whenCalled_matchesLegacy() {
            Path legacyRoot = ResourceResolver
                    .resolveResourcesRoot("knowledge", 1);
            Path newResult = ResourceResolver
                    .resolveResourceDir("knowledge/core");

            assertThat(newResult)
                    .isEqualTo(legacyRoot.resolve(
                            "knowledge/core"));
        }

        // --- TPP: cached calls return same instance ---

        @Test
        @DisplayName("repeated calls return cached result")
        void repeatedCalls_whenCalled_returnCachedPath() {
            Path first = ResourceResolver
                    .resolveResourceDir("knowledge/core");
            Path second = ResourceResolver
                    .resolveResourceDir("knowledge/core");

            assertThat(first).isEqualTo(second);
        }

        // --- TPP: boundary / error cases ---

        @Test
        @DisplayName("nonexistent path throws exception")
        void nonexistentPath_whenCalled_throwsException() {
            assertThatThrownBy(() ->
                    ResourceResolver.resolveResourceDir(
                            "nonexistent/path/dir"))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining(
                            "nonexistent/path/dir");
        }

        @Test
        @DisplayName("single-segment nonexistent throws")
        void singleSegmentNonexistent_whenCalled_throws() {
            assertThatThrownBy(() ->
                    ResourceResolver.resolveResourceDir(
                            "no-such-segment"))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining(
                            "no-such-segment");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "  "})
        @DisplayName("blank path throws exception")
        void blankPath_whenCalled_throwsException(
                String input) {
            assertThatThrownBy(() ->
                    ResourceResolver.resolveResourceDir(
                            input))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null path throws exception")
        void nullPath_whenCalled_throwsException() {
            assertThatThrownBy(() ->
                    ResourceResolver.resolveResourceDir(
                            null))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "knowledge/../../../etc/passwd",
                "../secret",
                "knowledge/core/../../..",
                "targets/claude/../../../etc"
        })
        @DisplayName("path traversal throws exception")
        void pathTraversal_whenCalled_throwsException(
                String malicious) {
            assertThatThrownBy(() ->
                    ResourceResolver.resolveResourceDir(
                            malicious))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("..");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/etc/passwd",
                "/tmp/secret",
                "C:\\Windows\\System32"
        })
        @DisplayName("absolute path throws exception")
        void absolutePath_whenCalled_throwsException(
                String absolute) {
            assertThatThrownBy(() ->
                    ResourceResolver.resolveResourceDir(
                            absolute))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("Absolute");
        }
    }

    @Nested
    @DisplayName("resolveResourceDir — acceptance")
    class ResolveResourceDirAcceptance {

        @ParameterizedTest(name = "resolves {0}")
        @ValueSource(strings = {
                "targets/claude/rules",
                "targets/claude/agents",
                "targets/claude/skills",
                "knowledge/core",
                "knowledge/languages",
                "knowledge/frameworks",
                "knowledge/databases",
                "knowledge/patterns",
                "knowledge/protocols",
                "knowledge/security",
                "knowledge/infrastructure",
                "shared/config-templates",
                "shared/templates",
                "shared/cicd-templates",
                "shared/docs"
        })
        @DisplayName("all resource directories resolvable")
        void allResourceDirs_whenCalled_resolve(
                String dir) {
            Path result = ResourceResolver
                    .resolveResourceDir(dir);

            assertThat(result).isAbsolute();
            assertThat(Files.isDirectory(result)).isTrue();
        }
    }

    @Nested
    @DisplayName("shouldSkip")
    class ShouldSkip {

        @Test
        @DisplayName("skips META-INF directory")
        void metaInf_whenCalled_skipped() {
            Path metaInf = Path.of("META-INF");

            assertThat(ResourceResolver
                    .shouldSkip(metaInf)).isTrue();
        }

        @Test
        @DisplayName("does not skip regular directory")
        void regularDir_whenCalled_notSkipped() {
            Path regular = Path.of("templates");

            assertThat(ResourceResolver
                    .shouldSkip(regular)).isFalse();
        }

        @Test
        @DisplayName("does not skip root path")
        void rootPath_whenCalled_notSkipped() {
            Path root = Path.of("/");

            assertThat(ResourceResolver
                    .shouldSkip(root)).isFalse();
        }
    }

    @Nested
    @DisplayName("extractJarResources")
    class ExtractJarResources {

        @AfterEach
        void resetCache() {
            Path cached = ResourceResolver
                    .cachedExtractedDir;
            ResourceResolver.cachedExtractedDir = null;
            if (cached != null) {
                ResourceResolver.deleteQuietly(cached);
            }
        }

        @Test
        @DisplayName("extracts non-class files from JAR")
        void resolve_whenCalled_extractsNonClassFiles(
                @TempDir Path tempDir)
                throws IOException {
            Path jar = createTestJar(tempDir);
            URL jarUrl = buildJarUrl(jar, "templates/");

            Path result = ResourceResolver
                    .extractJarResources(jarUrl);

            assertThat(Files.isDirectory(result))
                    .isTrue();
            assertThat(result.resolve(
                    "templates/hello.txt")).exists();
        }

        @Test
        @DisplayName("skips .class files")
        void resolve_whenCalled_skipsClassFiles(
                @TempDir Path tempDir)
                throws IOException {
            Path jar = createTestJar(tempDir);
            URL jarUrl = buildJarUrl(jar, "templates/");

            Path result = ResourceResolver
                    .extractJarResources(jarUrl);

            assertThat(Files.walk(result)
                    .noneMatch(p -> p.toString()
                            .endsWith(".class")))
                    .isTrue();
        }

        @Test
        @DisplayName("skips META-INF contents")
        void resolve_whenCalled_skipsMetaInf(
                @TempDir Path tempDir)
                throws IOException {
            Path jar = createTestJar(tempDir);
            URL jarUrl = buildJarUrl(jar, "templates/");

            Path result = ResourceResolver
                    .extractJarResources(jarUrl);

            assertThat(Files.walk(result)
                    .noneMatch(p -> p.toString()
                            .contains("META-INF")))
                    .isTrue();
        }

        @Test
        @DisplayName("resolveFromJar caches result")
        void resolveFromJar_whenCalled_cachesResult(
                @TempDir Path tempDir)
                throws IOException {
            Path jar = createTestJar(tempDir);
            URL jarUrl = buildJarUrl(jar, "templates/");

            Path first = ResourceResolver
                    .resolveFromJar(jarUrl);
            Path second = ResourceResolver
                    .resolveFromJar(jarUrl);

            assertThat(first).isEqualTo(second);
        }

        private Path createTestJar(Path dir)
                throws IOException {
            Path jarPath = dir.resolve("test.jar");
            try (JarOutputStream jos =
                         new JarOutputStream(
                                 Files.newOutputStream(
                                         jarPath))) {
                addJarEntry(jos, "templates/",
                        null);
                addJarEntry(jos, "templates/hello.txt",
                        "hello world");
                addJarEntry(jos, "Example.class",
                        "fake class data");
                addJarEntry(jos, "META-INF/",
                        null);
                addJarEntry(
                        jos, "META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0\n");
            }
            return jarPath;
        }

        private void addJarEntry(
                JarOutputStream jos,
                String name,
                String content) throws IOException {
            jos.putNextEntry(new JarEntry(name));
            if (content != null) {
                jos.write(content.getBytes(
                        StandardCharsets.UTF_8));
            }
            jos.closeEntry();
        }

        private URL buildJarUrl(
                Path jar, String entry)
                throws IOException {
            return new URL("jar:"
                    + jar.toUri() + "!/" + entry);
        }
    }

    @Nested
    @DisplayName("createSecureTempDir")
    class CreateSecureTempDir {

        @Test
        @DisabledOnOs(OS.WINDOWS)
        @DisplayName("creates temp dir with 700 permissions"
                + " on POSIX")
        void posixPermissions_with700PermissionsOnPosix_700() throws IOException {
            Path tempDir = ResourceResolver
                    .createSecureTempDir("test-secure-");
            try {
                Set<PosixFilePermission> perms =
                        Files.getPosixFilePermissions(
                                tempDir);
                assertThat(perms)
                        .containsExactlyInAnyOrder(
                                PosixFilePermission
                                        .OWNER_READ,
                                PosixFilePermission
                                        .OWNER_WRITE,
                                PosixFilePermission
                                        .OWNER_EXECUTE);
            } finally {
                Files.deleteIfExists(tempDir);
            }
        }

        @Test
        @DisplayName("returns existing directory")
        void resolve_whenCalled_returnsExistingDirectory()
                throws IOException {
            Path tempDir = ResourceResolver
                    .createSecureTempDir("test-res-");
            try {
                assertThat(tempDir).exists();
                assertThat(tempDir).isDirectory();
            } finally {
                Files.deleteIfExists(tempDir);
            }
        }
    }

    @Nested
    @DisplayName("deleteQuietly")
    class DeleteQuietly {

        @Test
        @DisplayName("deletes directory with files")
        void dirWithFiles_whenCalled_deleted(
                @TempDir Path tempDir) throws IOException {
            Path subDir = tempDir.resolve("sub");
            Files.createDirectories(subDir);
            Files.writeString(
                    subDir.resolve("a.txt"),
                    "content",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    subDir.resolve("b.txt"),
                    "content",
                    StandardCharsets.UTF_8);

            ResourceResolver.deleteQuietly(subDir);

            assertThat(subDir).doesNotExist();
        }

        @Test
        @DisplayName("deletes nested directory tree")
        void nestedTree_whenCalled_deleted(
                @TempDir Path tempDir) throws IOException {
            Path root = tempDir.resolve("root");
            Path nested = root.resolve("a/b/c");
            Files.createDirectories(nested);
            Files.writeString(
                    nested.resolve("file.txt"),
                    "data",
                    StandardCharsets.UTF_8);

            ResourceResolver.deleteQuietly(root);

            assertThat(root).doesNotExist();
        }

        @Test
        @DisplayName("handles nonexistent directory")
        void nonExistentDir_whenCalled_noError() {
            Path noSuch = Path.of(
                    "/tmp/no-such-dir-xyz-test");

            assertThatCode(() ->
                    ResourceResolver.deleteQuietly(noSuch))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deletes empty directory")
        void emptyDir_whenCalled_deleted(
                @TempDir Path tempDir) throws IOException {
            Path empty = tempDir.resolve("empty");
            Files.createDirectories(empty);

            ResourceResolver.deleteQuietly(empty);

            assertThat(empty).doesNotExist();
        }
    }
}
