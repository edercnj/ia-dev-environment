package dev.iadev.util;

import dev.iadev.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ResourceDiscovery")
class ResourceDiscoveryTest {

    @Nested
    @DisplayName("resourceExists")
    class ResourceExists {

        @Test
        @DisplayName("returns true for existing classpath resource")
        void existingResource_whenCalled_returnsTrue() {
            var discovery = new ResourceDiscovery();

            assertThat(discovery.resourceExists(
                    "config-templates/setup-config.java-spring.yaml"))
                    .isTrue();
        }

        @Test
        @DisplayName("returns false for non-existing resource")
        void nonExistingResource_forNonExistingresource_returnsfalse() {
            var discovery = new ResourceDiscovery();

            assertThat(discovery.resourceExists(
                    "inexistente/foo.txt"))
                    .isFalse();
        }

        @Test
        @DisplayName("returns true for filesystem resource "
                + "when resources-dir is set")
        void filesystemResource_whenResourcesDir_returnstrue(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(tempDir.resolve("templates"));
            Files.writeString(
                    tempDir.resolve("templates/test.txt"), "content");
            var discovery = new ResourceDiscovery(tempDir);

            assertThat(discovery.resourceExists("templates/test.txt"))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("findResource via classpath")
    class FindResourceClasspath {

        @Test
        @DisplayName("returns valid URL for classpath resource")
        void classpathResource_whenCalled_returnsValidUrl() {
            var discovery = new ResourceDiscovery();

            URL url = discovery.findResource(
                    "config-templates/setup-config.java-spring.yaml");

            assertThat(url).isNotNull();
            assertThat(url.toString()).contains(
                    "config-templates/setup-config.java-spring.yaml");
        }

        @Test
        @DisplayName("returns URL for core resource")
        void coreResource_forCoreResource_returnsValidUrl() {
            var discovery = new ResourceDiscovery();

            URL url = discovery.findResource("core/01-clean-code.md");

            assertThat(url).isNotNull();
        }
    }

    @Nested
    @DisplayName("findResource via filesystem (--resources-dir)")
    class FindResourceFilesystem {

        @Test
        @DisplayName("prioritizes filesystem over classpath")
        void findResource_whenCalled_filesystemPrioritized(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(
                    tempDir.resolve("config-templates"));
            Files.writeString(
                    tempDir.resolve(
                            "config-templates/setup-config.java-spring.yaml"),
                    "filesystem-version");
            var discovery = new ResourceDiscovery(tempDir);

            URL url = discovery.findResource(
                    "config-templates/setup-config.java-spring.yaml");

            assertThat(url.getProtocol()).isEqualTo("file");
            assertThat(url.getPath()).contains(tempDir.toString());
        }

        @Test
        @DisplayName("falls back to classpath when not found "
                + "in filesystem")
        void findResource_whenCalled_fallsBackToClasspath(@TempDir Path tempDir) {
            var discovery = new ResourceDiscovery(tempDir);

            URL url = discovery.findResource(
                    "config-templates/setup-config.java-spring.yaml");

            assertThat(url).isNotNull();
        }
    }

    @Nested
    @DisplayName("findResource not found")
    class FindResourceNotFound {

        @Test
        @DisplayName("throws exception with clear message")
        void nonExistingResource_whenCalled_throwsWithMessage() {
            var discovery = new ResourceDiscovery();

            assertThatThrownBy(
                    () -> discovery.findResource("inexistente/foo.txt"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(
                            "Resource not found: inexistente/foo.txt")
                    .hasMessageContaining("classpath");
        }

        @Test
        @DisplayName("exception message includes filesystem strategy "
                + "when resources-dir is set")
        void withResourcesDir_whenResourcesDir_messageincludesfilesystem(
                @TempDir Path tempDir) {
            var discovery = new ResourceDiscovery(tempDir);

            assertThatThrownBy(
                    () -> discovery.findResource("inexistente/foo.txt"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("filesystem")
                    .hasMessageContaining("classpath");
        }
    }

    @Nested
    @DisplayName("readResource")
    class ReadResource {

        @Test
        @DisplayName("reads classpath resource as UTF-8 string")
        void classpathResource_whenCalled_readsContent() {
            var discovery = new ResourceDiscovery();

            String content = discovery.readResource(
                    "config-templates/setup-config.java-spring.yaml");

            assertThat(content).isNotEmpty();
            assertThat(content).contains("java-spring");
        }

        @Test
        @DisplayName("reads filesystem resource as UTF-8")
        void filesystemResource_whenCalled_readsContent(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(tempDir.resolve("test"));
            Files.writeString(
                    tempDir.resolve("test/hello.txt"),
                    "Hello UTF-8: acentos e simbolos",
                    StandardCharsets.UTF_8);
            var discovery = new ResourceDiscovery(tempDir);

            String content = discovery.readResource("test/hello.txt");

            assertThat(content).isEqualTo(
                    "Hello UTF-8: acentos e simbolos");
        }

        @Test
        @DisplayName("preserves UTF-8 encoding")
        void discover_whenCalled_preservesUtf8Encoding(@TempDir Path tempDir)
                throws IOException {
            String utf8Content =
                    "Conteudo com acentuacao: e, a, o, u, c";
            Files.createDirectories(tempDir.resolve("test"));
            Files.writeString(
                    tempDir.resolve("test/utf8.txt"),
                    utf8Content, StandardCharsets.UTF_8);
            var discovery = new ResourceDiscovery(tempDir);

            String result = discovery.readResource("test/utf8.txt");

            assertThat(result).isEqualTo(utf8Content);
        }

        @Test
        @DisplayName("preserves LF line endings")
        void discover_whenCalled_preservesLfLineEndings(@TempDir Path tempDir)
                throws IOException {
            String lfContent = "line1\nline2\nline3\n";
            Files.createDirectories(tempDir.resolve("test"));
            Files.write(
                    tempDir.resolve("test/lf.txt"),
                    lfContent.getBytes(StandardCharsets.UTF_8));
            var discovery = new ResourceDiscovery(tempDir);

            String result = discovery.readResource("test/lf.txt");

            assertThat(result).isEqualTo(lfContent);
            assertThat(result).doesNotContain("\r\n");
        }

        @Test
        @DisplayName("throws exception for non-existing resource")
        void nonExistingResource_forNonExistingresource_throws() {
            var discovery = new ResourceDiscovery();

            assertThatThrownBy(
                    () -> discovery.readResource("inexistente/bar.txt"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listResources")
    class ListResources {

        @Test
        @DisplayName("lists files in filesystem directory")
        void filesystemDirectory_whenCalled_listsFiles(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(tempDir.resolve("mydir"));
            Files.writeString(
                    tempDir.resolve("mydir/a.txt"), "a");
            Files.writeString(
                    tempDir.resolve("mydir/b.txt"), "b");
            var discovery = new ResourceDiscovery(tempDir);

            List<String> result = discovery.listResources("mydir");

            assertThat(result)
                    .containsExactlyInAnyOrder("a.txt", "b.txt");
        }

        @Test
        @DisplayName("returns empty list for empty directory")
        void emptyDirectory_forEmptyDirectory_returnsEmptyList(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(tempDir.resolve("empty"));
            var discovery = new ResourceDiscovery(tempDir);

            List<String> result = discovery.listResources("empty");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty list for non-existing directory")
        void nonExistingDirectory_forNonExistingdirectory_returnsemptylist() {
            var discovery = new ResourceDiscovery();

            List<String> result = discovery.listResources(
                    "nonexistent-dir-xyz");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("all 8 config-templates accessible")
    class ConfigTemplatesAccessible {

        @ParameterizedTest
        @ValueSource(strings = {
                "config-templates/setup-config.go-gin.yaml",
                "config-templates/setup-config.java-quarkus.yaml",
                "config-templates/setup-config.java-spring.yaml",
                "config-templates/setup-config.kotlin-ktor.yaml",
                "config-templates/setup-config.python-click-cli.yaml",
                "config-templates/setup-config.python-fastapi.yaml",
                "config-templates/setup-config.rust-axum.yaml",
                "config-templates/setup-config.typescript-nestjs.yaml"
        })
        @DisplayName("config template exists and is readable")
        void configTemplate_existsAndReadable(String path) {
            var discovery = new ResourceDiscovery();

            assertThat(discovery.resourceExists(path)).isTrue();

            String content = discovery.readResource(path);
            assertThat(content).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("constructor variants")
    class ConstructorVariants {

        @Test
        @DisplayName("no-arg constructor uses classpath only")
        void noArgConstructor_whenCalled_classpathOnly() {
            var discovery = new ResourceDiscovery();

            assertThat(discovery.getResourcesDir()).isNull();
        }

        @Test
        @DisplayName("path constructor sets resources-dir")
        void pathConstructor_whenCalled_setsResourcesDir(@TempDir Path tempDir) {
            var discovery = new ResourceDiscovery(tempDir);

            assertThat(discovery.getResourcesDir()).isEqualTo(tempDir);
        }

        @Test
        @DisplayName("null path constructor behaves like no-arg")
        void nullPathConstructor_whenCalled_behavesLikeNoArg() {
            var discovery = new ResourceDiscovery(null);

            assertThat(discovery.getResourcesDir()).isNull();
        }
    }
}
