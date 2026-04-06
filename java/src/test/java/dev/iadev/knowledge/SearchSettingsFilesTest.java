package dev.iadev.knowledge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates search engine settings files exist and
 * contain correct CLI tools.
 */
@DisplayName("Search Settings Files")
class SearchSettingsFilesTest {

    private static final Path SETTINGS_BASE =
            resolveSettingsBase();

    private static Path resolveSettingsBase() {
        Path current = Path.of("").toAbsolutePath();
        if (current.endsWith("java")) {
            return current.resolve(
                    "src/main/resources/targets"
                            + "/claude/settings");
        }
        return current.resolve(
                "java/src/main/resources/targets"
                        + "/claude/settings");
    }

    @Nested
    @DisplayName("database-elasticsearch.json")
    class ElasticsearchSettings {

        private final Path file = SETTINGS_BASE.resolve(
                "database-elasticsearch.json");

        @Test
        @DisplayName("file exists")
        void elasticsearchSettings_exists() {
            assertThat(file).exists();
        }

        @Test
        @DisplayName("contains curl for ES API")
        void elasticsearchSettings_containsCurl()
                throws IOException {
            String content = Files.readString(
                    file, StandardCharsets.UTF_8);
            assertThat(content).contains("curl");
        }
    }

    @Nested
    @DisplayName("database-opensearch.json")
    class OpensearchSettings {

        private final Path file = SETTINGS_BASE.resolve(
                "database-opensearch.json");

        @Test
        @DisplayName("file exists")
        void opensearchSettings_exists() {
            assertThat(file).exists();
        }

        @Test
        @DisplayName("contains curl for OpenSearch API")
        void opensearchSettings_containsCurl()
                throws IOException {
            String content = Files.readString(
                    file, StandardCharsets.UTF_8);
            assertThat(content).contains("curl");
        }
    }
}
