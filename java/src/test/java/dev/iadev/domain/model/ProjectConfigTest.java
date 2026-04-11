package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Nested
    @DisplayName("Convenience accessors (Law of Demeter)")
    class ConvenienceAccessors {

        @Test
        @DisplayName("observabilityTool returns value from nested config")
        void observabilityTool_fullConfig_returnsValue() {
            var config = ProjectConfig.fromMap(
                    buildFullConfig());

            assertThat(config.observabilityTool())
                    .isEqualTo(config.infrastructure()
                            .observability().tool());
        }

        @Test
        @DisplayName("observabilityTool returns none for minimal config")
        void observabilityTool_minimalConfig_returnsNone() {
            var config = ProjectConfig.fromMap(
                    buildMinimalConfig());

            assertThat(config.observabilityTool())
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("observabilityTracing returns value from nested config")
        void observabilityTracing_fullConfig_returnsValue() {
            var config = ProjectConfig.fromMap(
                    buildFullConfig());

            assertThat(config.observabilityTracing())
                    .isEqualTo(config.infrastructure()
                            .observability().tracing());
        }

        @Test
        @DisplayName("observabilityTracing returns none for minimal config")
        void observabilityTracing_minimalConfig_returnsNone() {
            var config = ProjectConfig.fromMap(
                    buildMinimalConfig());

            assertThat(config.observabilityTracing())
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("observabilityMetrics returns value from nested config")
        void observabilityMetrics_fullConfig_returnsValue() {
            var config = ProjectConfig.fromMap(
                    buildFullConfig());

            assertThat(config.observabilityMetrics())
                    .isEqualTo(config.infrastructure()
                            .observability().metrics());
        }

        @Test
        @DisplayName("observabilityMetrics returns none for minimal config")
        void observabilityMetrics_minimalConfig_returnsNone() {
            var config = ProjectConfig.fromMap(
                    buildMinimalConfig());

            assertThat(config.observabilityMetrics())
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("databaseName returns value from nested data config")
        void databaseName_fullConfig_returnsPostgresql() {
            var config = ProjectConfig.fromMap(
                    buildFullConfig());

            assertThat(config.databaseName())
                    .isEqualTo("postgresql");
        }

        @Test
        @DisplayName("databaseName returns none for minimal config")
        void databaseName_minimalConfig_returnsNone() {
            var config = ProjectConfig.fromMap(
                    buildMinimalConfig());

            assertThat(config.databaseName())
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("migrationName returns value from nested data config")
        void migrationName_fullConfig_returnsValue() {
            var config = ProjectConfig.fromMap(
                    buildFullConfig());

            assertThat(config.migrationName())
                    .isEqualTo(config.data().migration().name());
        }

        @Test
        @DisplayName("migrationName returns none for minimal config")
        void migrationName_minimalConfig_returnsNone() {
            var config = ProjectConfig.fromMap(
                    buildMinimalConfig());

            assertThat(config.migrationName())
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("cacheName returns value from nested data config")
        void cacheName_fullConfig_returnsRedis() {
            var config = ProjectConfig.fromMap(
                    buildFullConfig());

            assertThat(config.cacheName())
                    .isEqualTo("redis");
        }

        @Test
        @DisplayName("cacheName returns none for minimal config")
        void cacheName_minimalConfig_returnsNone() {
            var config = ProjectConfig.fromMap(
                    buildMinimalConfig());

            assertThat(config.cacheName())
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("convenience accessors are identical to chained calls")
        void convenienceAccessors_whenCalled_matchChainedCalls() {
            var config = ProjectConfig.fromMap(
                    buildFullConfig());

            assertThat(config.observabilityTool())
                    .isEqualTo(config.infrastructure()
                            .observability().tool());
            assertThat(config.observabilityTracing())
                    .isEqualTo(config.infrastructure()
                            .observability().tracing());
            assertThat(config.observabilityMetrics())
                    .isEqualTo(config.infrastructure()
                            .observability().metrics());
            assertThat(config.databaseName())
                    .isEqualTo(config.data()
                            .database().name());
            assertThat(config.migrationName())
                    .isEqualTo(config.data()
                            .migration().name());
            assertThat(config.cacheName())
                    .isEqualTo(config.data()
                            .cache().name());
        }
    }

    @Nested
    @DisplayName("compliance field")
    class ComplianceField {

        @Test
        @DisplayName("defaults to 'none' when absent from config")
        void fromMap_noCompliance_defaultsToNone() {
            var config = ProjectConfig.fromMap(
                    buildMinimalConfig());

            assertThat(config.compliance())
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("returns 'none' when explicitly set")
        void fromMap_complianceNone_returnsNone() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("compliance", "none");

            var config = ProjectConfig.fromMap(map);

            assertThat(config.compliance())
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("returns 'pci-dss' when set")
        void fromMap_compliancePciDss_returnsPciDss() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("compliance", "pci-dss");

            var config = ProjectConfig.fromMap(map);

            assertThat(config.compliance())
                    .isEqualTo("pci-dss");
        }

        @Test
        @DisplayName("throws for unsupported compliance value")
        void fromMap_complianceInvalid_throwsException() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("compliance", "sox");

            assertThatThrownBy(
                    () -> ProjectConfig.fromMap(map))
                    .isInstanceOf(
                            ConfigValidationException.class)
                    .hasMessageContaining(
                            "Unsupported compliance value:"
                                    + " 'sox'")
                    .hasMessageContaining(
                            "Supported: none, pci-dss");
        }

        @Test
        @DisplayName("full config includes compliance in record")
        void fromMap_fullConfigWithCompliance_present() {
            var map = new HashMap<>(buildFullConfig());
            map.put("compliance", "pci-dss");

            var config = ProjectConfig.fromMap(map);

            assertThat(config.compliance())
                    .isEqualTo("pci-dss");
        }
    }

    @Nested
    @DisplayName("platform field")
    class PlatformField {

        @Test
        @DisplayName("absent platform defaults to empty set")
        void fromMap_noPlatform_defaultsToEmptySet() {
            var config = ProjectConfig.fromMap(
                    buildMinimalConfig());

            assertThat(config.platforms()).isEmpty();
        }

        @Test
        @DisplayName("platform string 'claude-code' parsed")
        void fromMap_platformString_parsedToSingletonSet() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("platform", "claude-code");

            var config = ProjectConfig.fromMap(map);

            assertThat(config.platforms())
                    .containsExactly(Platform.CLAUDE_CODE);
        }

        @Test
        @DisplayName("platform string 'all' returns empty set")
        void fromMap_platformAll_returnsEmptySet() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("platform", "all");

            var config = ProjectConfig.fromMap(map);

            assertThat(config.platforms()).isEmpty();
        }

        @Test
        @DisplayName("platform list parsed to set")
        void fromMap_platformList_parsedToSet() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("platform",
                    List.of("claude-code", "codex"));

            var config = ProjectConfig.fromMap(map);

            assertThat(config.platforms())
                    .containsExactlyInAnyOrder(
                            Platform.CLAUDE_CODE,
                            Platform.CODEX);
        }

        @Test
        @DisplayName("platform list with 'all' returns empty")
        void fromMap_platformListWithAll_returnsEmpty() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("platform", List.of("all"));

            var config = ProjectConfig.fromMap(map);

            assertThat(config.platforms()).isEmpty();
        }

        @Test
        @DisplayName("platform set is immutable")
        void fromMap_platforms_immutable() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("platform", "claude-code");

            var config = ProjectConfig.fromMap(map);

            assertThatThrownBy(() ->
                    config.platforms().add(Platform.CODEX))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("platform string 'codex' parsed")
        void fromMap_platformCodex_parsedCorrectly() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("platform", "codex");

            var config = ProjectConfig.fromMap(map);

            assertThat(config.platforms())
                    .containsExactly(Platform.CODEX);
        }

        @Test
        @DisplayName("platform list with both values")
        void fromMap_platformListBoth_parsedToSet() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("platform",
                    List.of("claude-code", "codex"));

            var config = ProjectConfig.fromMap(map);

            assertThat(config.platforms())
                    .containsExactlyInAnyOrder(
                            Platform.CLAUDE_CODE,
                            Platform.CODEX);
        }

        @Test
        @DisplayName("invalid platform string throws")
        void fromMap_invalidPlatform_throwsException() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("platform", "invalid-value");

            assertThatThrownBy(
                    () -> ProjectConfig.fromMap(map))
                    .isInstanceOf(
                            ConfigValidationException.class)
                    .hasMessageContaining(
                            "Invalid platform value:"
                                    + " 'invalid-value'")
                    .hasMessageContaining(
                            "Valid values");
        }

        @Test
        @DisplayName("invalid platform in list throws")
        void fromMap_invalidPlatformInList_throws() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("platform",
                    List.of("claude-code", "bad"));

            assertThatThrownBy(
                    () -> ProjectConfig.fromMap(map))
                    .isInstanceOf(
                            ConfigValidationException.class)
                    .hasMessageContaining(
                            "Invalid platform value:"
                                    + " 'bad'");
        }
    }

    @Nested
    @DisplayName("branching-model field")
    class BranchingModelField {

        @Test
        @DisplayName("defaults to GITFLOW when absent")
        void fromMap_noBranchingModel_defaultsToGitflow() {
            var config = ProjectConfig.fromMap(
                    buildMinimalConfig());

            assertThat(config.branchingModel())
                    .isEqualTo(BranchingModel.GITFLOW);
            assertThat(config.baseBranch())
                    .isEqualTo("develop");
        }

        @Test
        @DisplayName("parses 'gitflow' correctly")
        void fromMap_gitflow_parsedCorrectly() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("branching-model", "gitflow");

            var config = ProjectConfig.fromMap(map);

            assertThat(config.branchingModel())
                    .isEqualTo(BranchingModel.GITFLOW);
            assertThat(config.baseBranch())
                    .isEqualTo("develop");
        }

        @Test
        @DisplayName("parses 'trunk' correctly")
        void fromMap_trunk_parsedCorrectly() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("branching-model", "trunk");

            var config = ProjectConfig.fromMap(map);

            assertThat(config.branchingModel())
                    .isEqualTo(BranchingModel.TRUNK);
            assertThat(config.baseBranch())
                    .isEqualTo("main");
        }

        @Test
        @DisplayName("case-insensitive: 'TRUNK' resolves")
        void fromMap_upperCase_parsedCorrectly() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("branching-model", "TRUNK");

            var config = ProjectConfig.fromMap(map);

            assertThat(config.branchingModel())
                    .isEqualTo(BranchingModel.TRUNK);
        }

        @Test
        @DisplayName("invalid value throws")
        void fromMap_invalid_throwsException() {
            var map = new HashMap<>(buildMinimalConfig());
            map.put("branching-model", "invalid");

            assertThatThrownBy(
                    () -> ProjectConfig.fromMap(map))
                    .isInstanceOf(
                            ConfigValidationException.class)
                    .hasMessageContaining(
                            "Invalid branching-model:"
                                    + " 'invalid'")
                    .hasMessageContaining(
                            "Accepted values:"
                                    + " gitflow, trunk");
        }

        @Test
        @DisplayName("default is identical to explicit gitflow")
        void fromMap_defaultBehavior_identicalToExplicit() {
            var defaultConfig = ProjectConfig.fromMap(
                    buildMinimalConfig());

            var map = new HashMap<>(buildMinimalConfig());
            map.put("branching-model", "gitflow");
            var explicitConfig = ProjectConfig.fromMap(map);

            assertThat(defaultConfig.branchingModel())
                    .isEqualTo(
                            explicitConfig.branchingModel());
            assertThat(defaultConfig.baseBranch())
                    .isEqualTo(
                            explicitConfig.baseBranch());
        }
    }
}
