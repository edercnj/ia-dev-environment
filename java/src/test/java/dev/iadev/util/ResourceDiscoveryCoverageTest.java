package dev.iadev.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional coverage tests for ResourceDiscovery —
 * targeting uncovered branches.
 */
@DisplayName("ResourceDiscovery — coverage")
class ResourceDiscoveryCoverageTest {

    @Nested
    @DisplayName("listResources — filesystem paths")
    class ListResourcesFilesystem {

        @Test
        @DisplayName("directory not found returns"
                + " empty list")
        void discover_whenCalled_directoryNotFound(@TempDir Path tempDir) {
            var discovery =
                    new ResourceDiscovery(tempDir);

            List<String> result =
                    discovery.listResources(
                            "nonexistent-dir");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("directory with files lists"
                + " regular files only")
        void discover_whenCalled_listsRegularFilesOnly(
                @TempDir Path tempDir) throws IOException {
            Path dir = tempDir.resolve("test-dir");
            Files.createDirectories(dir);
            Files.writeString(
                    dir.resolve("file1.md"),
                    "content1",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    dir.resolve("file2.txt"),
                    "content2",
                    StandardCharsets.UTF_8);
            Files.createDirectories(
                    dir.resolve("subdir"));

            var discovery =
                    new ResourceDiscovery(tempDir);

            List<String> result =
                    discovery.listResources("test-dir");

            assertThat(result)
                    .containsExactlyInAnyOrder(
                            "file1.md", "file2.txt");
        }

        @Test
        @DisplayName("file (not directory) returns"
                + " empty list")
        void discover_whenCalled_fileNotDirectory(@TempDir Path tempDir)
                throws IOException {
            Files.writeString(
                    tempDir.resolve("not-a-dir"),
                    "file content",
                    StandardCharsets.UTF_8);

            var discovery =
                    new ResourceDiscovery(tempDir);

            List<String> result =
                    discovery.listResources("not-a-dir");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("listResources — classpath")
    class ListResourcesClasspath {

        @Test
        @DisplayName("classpath listing returns empty"
                + " (not reliably supported)")
        void listResources_classpath_returnsEmpty() {
            var discovery = new ResourceDiscovery();

            List<String> result =
                    discovery.listResources(
                            "shared/config-templates");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("readResource — filesystem")
    class ReadResourceFilesystem {

        @Test
        @DisplayName("reads file from filesystem")
        void readResource_whenCalled_readsFromFilesystem(
                @TempDir Path tempDir) throws IOException {
            Files.writeString(
                    tempDir.resolve("test.md"),
                    "Hello World",
                    StandardCharsets.UTF_8);

            var discovery =
                    new ResourceDiscovery(tempDir);

            String content =
                    discovery.readResource("test.md");

            assertThat(content).isEqualTo("Hello World");
        }
    }
}
