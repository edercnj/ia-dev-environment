package dev.iadev.model;

import dev.iadev.exception.ConfigValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("McpServerConfig")
class McpServerConfigTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("creates config with all fields")
        void fromMap_allFields_allSet() {
            var map = Map.<String, Object>of(
                    "id", "firecrawl",
                    "url", "https://mcp.firecrawl.dev",
                    "capabilities", List.of("scrape", "crawl"),
                    "env", Map.of("API_KEY", "secret123"));

            var result = McpServerConfig.fromMap(map);

            assertThat(result.id()).isEqualTo("firecrawl");
            assertThat(result.url())
                    .isEqualTo("https://mcp.firecrawl.dev");
            assertThat(result.capabilities())
                    .containsExactly("scrape", "crawl");
            assertThat(result.env())
                    .containsEntry("API_KEY", "secret123");
        }

        @Test
        @DisplayName("defaults capabilities and env to empty")
        void fromMap_onlyRequired_defaultsEmpty() {
            var map = Map.<String, Object>of(
                    "id", "server1",
                    "url", "http://localhost:3000");

            var result = McpServerConfig.fromMap(map);

            assertThat(result.capabilities()).isEmpty();
            assertThat(result.env()).isEmpty();
        }

        @Test
        @DisplayName("throws when id is missing")
        void fromMap_missingId_throwsException() {
            var map = Map.<String, Object>of(
                    "url", "http://localhost");

            assertThatThrownBy(() -> McpServerConfig.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("id");
        }

        @Test
        @DisplayName("throws when url is missing")
        void fromMap_missingUrl_throwsException() {
            var map = Map.<String, Object>of("id", "server1");

            assertThatThrownBy(() -> McpServerConfig.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("url");
        }
    }

    @Test
    @DisplayName("capabilities list is immutable")
    void capabilities_immutable_throwsOnModification() {
        var config = new McpServerConfig(
                "s1", "http://x",
                List.of("read"), Map.of());

        assertThatThrownBy(
                () -> config.capabilities().add("write"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("env map is immutable")
    void env_immutable_throwsOnModification() {
        var config = new McpServerConfig(
                "s1", "http://x",
                List.of(), Map.of("KEY", "val"));

        assertThatThrownBy(
                () -> config.env().put("NEW", "val"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
