package dev.iadev.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("McpConfig")
class McpConfigTest {

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("creates config with servers list")
        void fromMap_withServers_listPopulated() {
            var map = Map.<String, Object>of(
                    "servers", List.of(
                            Map.<String, Object>of(
                                    "id", "s1",
                                    "url", "http://mcp1"),
                            Map.<String, Object>of(
                                    "id", "s2",
                                    "url", "http://mcp2")));

            var result = McpConfig.fromMap(map);

            assertThat(result.servers()).hasSize(2);
            assertThat(result.servers().get(0).id()).isEqualTo("s1");
            assertThat(result.servers().get(1).id()).isEqualTo("s2");
        }

        @Test
        @DisplayName("empty map defaults to empty servers list")
        void fromMap_emptyMap_emptyList() {
            var result = McpConfig.fromMap(Map.of());

            assertThat(result.servers()).isEmpty();
        }

        @Test
        @DisplayName("non-list servers value defaults to empty list")
        void fromMap_nonListServers_emptyList() {
            var map = Map.<String, Object>of("servers", "invalid");

            var result = McpConfig.fromMap(map);

            assertThat(result.servers()).isEmpty();
        }
    }

    @Test
    @DisplayName("servers list is immutable")
    void servers_immutable_throwsOnModification() {
        var config = new McpConfig(List.of());

        assertThatThrownBy(() -> config.servers().add(
                new McpServerConfig("s1", "http://x",
                        List.of(), Map.of())))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
