package dev.iadev.model;

import dev.iadev.exception.ConfigValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProjectConfig")
class ProjectConfigTest {

    private Map<String, Object> buildFullConfig() {
        return Map.ofEntries(
                Map.entry("project", Map.of(
                        "name", "my-app",
                        "purpose", "A microservice")),
                Map.entry("architecture", Map.of(
                        "style", "microservice",
                        "domain_driven", true,
                        "event_driven", false)),
                Map.entry("interfaces", List.of(
                        Map.of("type", "rest",
                                "spec", "openapi-3.1"),
                        Map.of("type", "grpc"))),
                Map.entry("language", Map.of(
                        "name", "java",
                        "version", "21")),
                Map.entry("framework", Map.of(
                        "name", "spring-boot",
                        "version", "3.4",
                        "build_tool", "maven")),
                Map.entry("data", Map.of(
                        "database", Map.of(
                                "name", "postgresql",
                                "version", "16"),
                        "cache", Map.of(
                                "name", "redis",
                                "version", "7"))),
                Map.entry("security", Map.of(
                        "frameworks",
                        List.of("spring-security"))),
                Map.entry("testing", Map.of(
                        "coverage_line", 95,
                        "coverage_branch", 90,
                        "smoke_tests", true,
                        "contract_tests", false)),
                Map.entry("infrastructure", Map.of(
                        "container", "docker",
                        "orchestrator", "kubernetes")),
                Map.entry("mcp", Map.of(
                        "servers", List.of(
                                Map.of("id", "mcp1",
                                        "url", "http://mcp")))));
    }

    private Map<String, Object> buildMinimalConfig() {
        return Map.of(
                "project", Map.of(
                        "name", "minimal",
                        "purpose", "Minimal test"),
                "architecture", Map.of(
                        "style", "library"),
                "interfaces", List.of(
                        Map.of("type", "cli")),
                "language", Map.of(
                        "name", "python",
                        "version", "3.10"),
                "framework", Map.of(
                        "name", "click",
                        "version", "8.1"));
    }

    @Nested
    @DisplayName("fromMap() with full config")
    class FullConfig {

        @Test
        @DisplayName("creates complete ProjectConfig with all sections")
        void fromMap_fullConfig_allSectionsPopulated() {
            var config = ProjectConfig.fromMap(buildFullConfig());

            assertThat(config.project().name()).isEqualTo("my-app");
            assertThat(config.project().purpose())
                    .isEqualTo("A microservice");
            assertThat(config.architecture().style())
                    .isEqualTo("microservice");
            assertThat(config.architecture().domainDriven()).isTrue();
            assertThat(config.interfaces()).hasSize(2);
            assertThat(config.interfaces().get(0).type())
                    .isEqualTo("rest");
            assertThat(config.interfaces().get(1).type())
                    .isEqualTo("grpc");
            assertThat(config.language().name()).isEqualTo("java");
            assertThat(config.language().version()).isEqualTo("21");
            assertThat(config.framework().name())
                    .isEqualTo("spring-boot");
            assertThat(config.framework().buildTool())
                    .isEqualTo("maven");
            assertThat(config.data().database().name())
                    .isEqualTo("postgresql");
            assertThat(config.data().cache().name())
                    .isEqualTo("redis");
            assertThat(config.security().frameworks())
                    .containsExactly("spring-security");
            assertThat(config.testing().coverageLine())
                    .isEqualTo(95);
            assertThat(config.testing().coverageBranch())
                    .isEqualTo(90);
            assertThat(config.infrastructure().container())
                    .isEqualTo("docker");
            assertThat(config.mcp().servers()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("fromMap() with minimal config (optional sections absent)")
    class MinimalConfig {

        @Test
        @DisplayName("uses defaults for all optional sections")
        void fromMap_minimalConfig_optionalDefaultsApplied() {
            var config = ProjectConfig.fromMap(buildMinimalConfig());

            assertThat(config.project().name()).isEqualTo("minimal");
            assertThat(config.data().database().name())
                    .isEqualTo("none");
            assertThat(config.data().migration().name())
                    .isEqualTo("none");
            assertThat(config.data().cache().name())
                    .isEqualTo("none");
            assertThat(config.security().frameworks()).isEmpty();
            assertThat(config.testing().coverageLine())
                    .isEqualTo(95);
            assertThat(config.testing().coverageBranch())
                    .isEqualTo(90);
            assertThat(config.testing().smokeTests()).isTrue();
            assertThat(config.testing().contractTests()).isFalse();
            assertThat(config.infrastructure().container())
                    .isEqualTo("docker");
            assertThat(config.infrastructure().orchestrator())
                    .isEqualTo("none");
            assertThat(config.mcp().servers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("fromMap() with missing required sections")
    class MissingRequired {

        @Test
        @DisplayName("throws when project section is missing")
        void fromMap_missingProject_throwsException() {
            var map = new HashMap<>(buildMinimalConfig());
            map.remove("project");

            assertThatThrownBy(() -> ProjectConfig.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("project");
        }

        @Test
        @DisplayName("throws when architecture section is missing")
        void fromMap_missingArchitecture_throwsException() {
            var map = new HashMap<>(buildMinimalConfig());
            map.remove("architecture");

            assertThatThrownBy(() -> ProjectConfig.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("architecture");
        }

        @Test
        @DisplayName("throws when interfaces section is missing")
        void fromMap_missingInterfaces_throwsException() {
            var map = new HashMap<>(buildMinimalConfig());
            map.remove("interfaces");

            assertThatThrownBy(() -> ProjectConfig.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("interfaces");
        }

        @Test
        @DisplayName("throws when language section is missing")
        void fromMap_missingLanguage_throwsException() {
            var map = new HashMap<>(buildMinimalConfig());
            map.remove("language");

            assertThatThrownBy(() -> ProjectConfig.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("language");
        }

        @Test
        @DisplayName("throws when framework section is missing")
        void fromMap_missingFramework_throwsException() {
            var map = new HashMap<>(buildMinimalConfig());
            map.remove("framework");

            assertThatThrownBy(() -> ProjectConfig.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("framework");
        }
    }

    @Nested
    @DisplayName("fromMap() with invalid types")
    class InvalidTypes {

        @Test
        @DisplayName("throws when interfaces is not a list")
        void fromMap_interfacesNotList_throwsException() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("interfaces", "not-a-list");

            assertThatThrownBy(() -> ProjectConfig.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("interfaces")
                    .hasMessageContaining("List");
        }

        @Test
        @DisplayName("throws when project is not a map")
        void fromMap_projectNotMap_throwsException() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("project", "not-a-map");

            assertThatThrownBy(() -> ProjectConfig.fromMap(map))
                    .isInstanceOf(ConfigValidationException.class)
                    .hasMessageContaining("project")
                    .hasMessageContaining("Map");
        }
    }

    @Test
    @DisplayName("interfaces list is immutable")
    void interfaces_immutable_throwsOnModification() {
        var config = ProjectConfig.fromMap(buildMinimalConfig());

        assertThatThrownBy(() -> config.interfaces().add(
                new InterfaceConfig("grpc", "", "")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
