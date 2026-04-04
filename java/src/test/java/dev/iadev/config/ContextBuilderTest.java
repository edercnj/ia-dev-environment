package dev.iadev.config;

import dev.iadev.domain.model.ArchitectureConfig;
import dev.iadev.domain.model.DataConfig;
import dev.iadev.domain.model.FrameworkConfig;
import dev.iadev.domain.model.InfraConfig;
import dev.iadev.domain.model.InterfaceConfig;
import dev.iadev.domain.model.LanguageConfig;
import dev.iadev.domain.model.McpConfig;
import dev.iadev.domain.model.ObservabilityConfig;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.ProjectIdentity;
import dev.iadev.domain.model.SecurityConfig;
import dev.iadev.domain.model.TechComponent;
import dev.iadev.domain.model.TestingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContextBuilder")
class ContextBuilderTest {

    private ProjectConfig buildFullConfig() {
        return new ProjectConfig(
                new ProjectIdentity(
                        "my-app", "A microservice"),
                new ArchitectureConfig(
                        "microservice", true, false,
                        false, "",
                        "eventstoredb", 100),
                List.of(
                        new InterfaceConfig(
                                "rest", "openapi", ""),
                        new InterfaceConfig(
                                "grpc", "proto3", "")),
                new LanguageConfig("java", "21"),
                new FrameworkConfig(
                        "spring-boot", "3.4",
                        "maven", true),
                buildFullDataConfig(),
                buildFullInfraConfig(),
                new SecurityConfig(
                        List.of("spring-security")),
                new TestingConfig(
                        true, true, true, 95, 90),
                new McpConfig(List.of()));
    }

    private DataConfig buildFullDataConfig() {
        return new DataConfig(
                new TechComponent("postgresql", "16"),
                new TechComponent("flyway", "9"),
                new TechComponent("redis", "7"));
    }

    private InfraConfig buildFullInfraConfig() {
        return new InfraConfig(
                "docker", "kubernetes", "kustomize",
                "terraform", "ecr", "kong", "istio",
                "aws",
                new ObservabilityConfig(
                        "prometheus", "micrometer",
                        "jaeger"));
    }

    private ProjectConfig buildMinimalConfig() {
        return new ProjectConfig(
                new ProjectIdentity("minimal", "Minimal test"),
                new ArchitectureConfig(
                        "library", false, false,
                        false, "",
                        "eventstoredb", 100),
                List.of(new InterfaceConfig("cli", "", "")),
                new LanguageConfig("python", "3.10"),
                new FrameworkConfig(
                        "click", "8.1", "pip", false),
                DataConfig.fromMap(Map.of()),
                InfraConfig.fromMap(Map.of()),
                SecurityConfig.fromMap(Map.of()),
                TestingConfig.fromMap(Map.of()),
                McpConfig.fromMap(Map.of()));
    }

    @Nested
    @DisplayName("buildContext() produces exactly 38 fields")
    class FieldCount {

        @Test
        @DisplayName("returns map with exactly 38 entries")
        void buildContext_fullConfig_returns38Fields() {
            Map<String, Object> context =
                    ContextBuilder.buildContext(
                            buildFullConfig());

            assertThat(context).hasSize(38);
        }

        @Test
        @DisplayName("returns map with 38 entries for minimal")
        void buildContext_minimalConfig_returns38Fields() {
            Map<String, Object> context =
                    ContextBuilder.buildContext(
                            buildMinimalConfig());

            assertThat(context).hasSize(38);
        }
    }

    @Nested
    @DisplayName("buildContext() field values")
    class FieldValues {

        @Test
        @DisplayName("project fields are correct")
        void buildContext_projectFields_correct() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildFullConfig());

            assertThat(ctx.get("project_name"))
                    .isEqualTo("my-app");
            assertThat(ctx.get("project_purpose"))
                    .isEqualTo("A microservice");
        }

        @Test
        @DisplayName("language fields are correct")
        void buildContext_languageFields_correct() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildFullConfig());

            assertThat(ctx.get("language_name"))
                    .isEqualTo("java");
            assertThat(ctx.get("language_version"))
                    .isEqualTo("21");
        }

        @Test
        @DisplayName("framework fields are correct")
        void buildContext_frameworkFields_correct() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildFullConfig());

            assertThat(ctx.get("framework_name"))
                    .isEqualTo("spring-boot");
            assertThat(ctx.get("framework_version"))
                    .isEqualTo("3.4");
            assertThat(ctx.get("build_tool"))
                    .isEqualTo("maven");
        }

        @Test
        @DisplayName("architecture fields are correct")
        void buildContext_architectureFields_correct() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(
                            buildFullConfig());

            assertThat(ctx.get("architecture_style"))
                    .isEqualTo("microservice");
            assertThat(ctx.get("validate_with_archunit"))
                    .isEqualTo("False");
            assertThat(ctx.get("base_package"))
                    .isEqualTo("");
            assertThat(ctx.get("event_store"))
                    .isEqualTo("eventstoredb");
            assertThat(ctx.get("events_per_snapshot"))
                    .isEqualTo(100);
        }

        @Test
        @DisplayName("infrastructure fields are correct")
        void buildContext_infrastructureFields_correct() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildFullConfig());

            assertThat(ctx.get("container"))
                    .isEqualTo("docker");
            assertThat(ctx.get("orchestrator"))
                    .isEqualTo("kubernetes");
            assertThat(ctx.get("templating"))
                    .isEqualTo("kustomize");
            assertThat(ctx.get("iac"))
                    .isEqualTo("terraform");
            assertThat(ctx.get("registry"))
                    .isEqualTo("ecr");
            assertThat(ctx.get("api_gateway"))
                    .isEqualTo("kong");
            assertThat(ctx.get("service_mesh"))
                    .isEqualTo("istio");
        }

        @Test
        @DisplayName("data fields are correct")
        void buildContext_dataFields_correct() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildFullConfig());

            assertThat(ctx.get("database_name"))
                    .isEqualTo("postgresql");
            assertThat(ctx.get("migration_name"))
                    .isEqualTo("flyway");
            assertThat(ctx.get("cache_name"))
                    .isEqualTo("redis");
            assertThat(ctx.get("message_broker"))
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("testing numeric fields are correct")
        void buildContext_testingNumericFields_correct() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildFullConfig());

            assertThat(ctx.get("coverage_line")).isEqualTo(95);
            assertThat(ctx.get("coverage_branch")).isEqualTo(90);
        }

        @Test
        @DisplayName("interfaces_list is comma-separated")
        void buildContext_interfacesList_commaSeparated() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildFullConfig());

            assertThat(ctx.get("interfaces_list"))
                    .isEqualTo("rest, grpc");
        }
    }

    @Nested
    @DisplayName("Python-bool conversion (RULE-002)")
    class PythonBool {

        @Test
        @DisplayName("domain_driven is 'True' when true")
        void buildContext_domainDrivenTrue_pythonTrue() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildFullConfig());

            assertThat(ctx.get("domain_driven"))
                    .isEqualTo("True");
        }

        @Test
        @DisplayName("event_driven is 'False' when false")
        void buildContext_eventDrivenFalse_pythonFalse() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildFullConfig());

            assertThat(ctx.get("event_driven"))
                    .isEqualTo("False");
        }

        @Test
        @DisplayName("smoke_tests is 'True' when true")
        void buildContext_smokeTestsTrue_pythonTrue() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildFullConfig());

            assertThat(ctx.get("smoke_tests"))
                    .isEqualTo("True");
        }

        @Test
        @DisplayName("contract_tests is 'True' when true")
        void buildContext_contractTestsTrue_pythonTrue() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildFullConfig());

            assertThat(ctx.get("contract_tests"))
                    .isEqualTo("True");
        }

        @Test
        @DisplayName("performance_tests is 'True' when true")
        void buildContext_performanceTestsTrue_pythonTrue() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildFullConfig());

            assertThat(ctx.get("performance_tests"))
                    .isEqualTo("True");
        }

        @Test
        @DisplayName("all boolean fields are 'False' for minimal")
        void buildContext_minimalBooleans_allFalse() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildMinimalConfig());

            assertThat(ctx.get("domain_driven"))
                    .isEqualTo("False");
            assertThat(ctx.get("event_driven"))
                    .isEqualTo("False");
            assertThat(ctx.get("contract_tests"))
                    .isEqualTo("False");
        }

        @Test
        @DisplayName("smoke_tests defaults to 'True'")
        void buildContext_smokeTestsDefault_pythonTrue() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildMinimalConfig());

            assertThat(ctx.get("smoke_tests"))
                    .isEqualTo("True");
        }

        @Test
        @DisplayName("performance_tests defaults to 'True'")
        void buildContext_performanceTestsDefault_pythonTrue() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildMinimalConfig());

            assertThat(ctx.get("performance_tests"))
                    .isEqualTo("True");
        }
    }

    @Nested
    @DisplayName("default values for minimal config")
    class Defaults {

        @Test
        @DisplayName("database_name defaults to 'none'")
        void buildContext_noDatabase_defaultsToNone() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildMinimalConfig());

            assertThat(ctx.get("database_name"))
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("cache_name defaults to 'none'")
        void buildContext_noCache_defaultsToNone() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildMinimalConfig());

            assertThat(ctx.get("cache_name"))
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("coverage defaults to 95/90")
        void buildContext_defaultCoverage_9590() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildMinimalConfig());

            assertThat(ctx.get("coverage_line")).isEqualTo(95);
            assertThat(ctx.get("coverage_branch")).isEqualTo(90);
        }

        @Test
        @DisplayName("infrastructure defaults to docker/none/etc")
        void buildContext_defaultInfra_dockerDefaults() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildMinimalConfig());

            assertThat(ctx.get("container"))
                    .isEqualTo("docker");
            assertThat(ctx.get("orchestrator"))
                    .isEqualTo("none");
            assertThat(ctx.get("templating"))
                    .isEqualTo("kustomize");
            assertThat(ctx.get("iac"))
                    .isEqualTo("none");
            assertThat(ctx.get("registry"))
                    .isEqualTo("none");
            assertThat(ctx.get("api_gateway"))
                    .isEqualTo("none");
            assertThat(ctx.get("service_mesh"))
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("interfaces_list for single interface")
        void buildContext_singleInterface_noComma() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(buildMinimalConfig());

            assertThat(ctx.get("interfaces_list"))
                    .isEqualTo("cli");
        }
    }

    @Nested
    @DisplayName("interfaces_list edge cases")
    class InterfacesListEdgeCases {

        @Test
        @DisplayName("empty interfaces produces 'none'")
        void buildContext_emptyInterfaces_producesNone() {
            var config = new ProjectConfig(
                    new ProjectIdentity("test", "test"),
                    new ArchitectureConfig("library", false, false,
                            false, "", "eventstoredb", 100),
                    List.of(),
                    new LanguageConfig("java", "21"),
                    new FrameworkConfig(
                            "spring-boot", "3.4", "maven", false),
                    DataConfig.fromMap(Map.of()),
                    InfraConfig.fromMap(Map.of()),
                    SecurityConfig.fromMap(Map.of()),
                    TestingConfig.fromMap(Map.of()),
                    McpConfig.fromMap(Map.of()));

            Map<String, Object> ctx =
                    ContextBuilder.buildContext(config);

            assertThat(ctx.get("interfaces_list"))
                    .isEqualTo("none");
        }
    }

    @Nested
    @DisplayName("exact 38 field names")
    class ExactFieldNames {

        @Test
        @DisplayName("context contains all 38 expected keys")
        void buildContext_allExpectedKeys_present() {
            Map<String, Object> ctx =
                    ContextBuilder.buildContext(
                            buildFullConfig());

            assertThat(ctx).containsKeys(
                    "project_name",
                    "project_purpose",
                    "language_name",
                    "language_version",
                    "framework_name",
                    "framework_version",
                    "build_tool",
                    "architecture_style",
                    "domain_driven",
                    "event_driven",
                    "validate_with_archunit",
                    "base_package",
                    "event_store",
                    "events_per_snapshot",
                    "container",
                    "orchestrator",
                    "templating",
                    "iac",
                    "registry",
                    "api_gateway",
                    "service_mesh",
                    "database_name",
                    "migration_name",
                    "cache_name",
                    "message_broker",
                    "smoke_tests",
                    "contract_tests",
                    "performance_tests",
                    "coverage_line",
                    "coverage_branch",
                    "interfaces_list",
                    "has_event_interface",
                    "has_pci_dss",
                    "has_lgpd",
                    "review_max_score",
                    "review_go_threshold",
                    "review_conditional_rubric",
                    "review_conditional_criteria");
        }
    }

    @Nested
    @DisplayName("toPythonBool helper")
    class ToPythonBoolHelper {

        @Test
        @DisplayName("true converts to 'True'")
        void toPythonBool_true_returnsTrue() {
            assertThat(ContextBuilder.toPythonBool(true))
                    .isEqualTo("True");
        }

        @Test
        @DisplayName("false converts to 'False'")
        void toPythonBool_false_returnsFalse() {
            assertThat(ContextBuilder.toPythonBool(false))
                    .isEqualTo("False");
        }
    }
}
