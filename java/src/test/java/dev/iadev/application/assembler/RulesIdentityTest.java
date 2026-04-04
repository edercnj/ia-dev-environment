package dev.iadev.assembler;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for RulesIdentity — project identity content builder.
 */
@DisplayName("RulesIdentity")
class RulesIdentityTest {

    @Nested
    @DisplayName("buildContent")
    class BuildContent {

        @Test
        @DisplayName("contains project name in header")
        void create_whenCalled_containsProjectName() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .projectName("my-api")
                    .build();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content)
                    .contains("# Project Identity — my-api")
                    .contains("- **Name:** my-api");
        }

        @Test
        @DisplayName("contains language and version")
        void create_whenCalled_containsLanguageAndVersion() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .build();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content)
                    .contains("- **Language:** java 21")
                    .contains("| Language | java 21 |");
        }

        @Test
        @DisplayName("contains framework with version")
        void create_withVersion_containsFramework() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .framework("quarkus", "3.17")
                    .build();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content)
                    .contains("- **Framework:** quarkus 3.17")
                    .contains("| Framework | quarkus 3.17 |");
        }

        @Test
        @DisplayName("contains architecture style")
        void create_whenCalled_containsArchitectureStyle() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .archStyle("microservice")
                    .build();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content)
                    .contains(
                            "- **Architecture Style:**"
                                    + " microservice")
                    .contains(
                            "| Architecture |"
                                    + " microservice |");
        }

        @Test
        @DisplayName("contains DDD and event-driven flags")
        void create_whenCalled_containsDddAndEventDriven() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .domainDriven(true)
                    .eventDriven(true)
                    .build();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content)
                    .contains(
                            "- **Domain-Driven Design:**"
                                    + " true")
                    .contains(
                            "- **Event-Driven:** true");
        }

        @Test
        @DisplayName("contains interfaces list")
        void create_whenCalled_containsInterfacesList() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .clearInterfaces()
                    .addInterface("rest")
                    .addInterface("grpc")
                    .build();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content)
                    .contains("- **Interfaces:** rest, grpc");
        }

        @Test
        @DisplayName("contains build tool in tech stack")
        void create_whenCalled_containsBuildTool() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .buildTool("maven")
                    .build();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content)
                    .contains("| Build Tool | maven |");
        }

        @Test
        @DisplayName("contains native build flag")
        void create_whenCalled_containsNativeBuild() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .nativeBuild(true)
                    .build();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content)
                    .contains("| Native Build | true |");
        }

        @Test
        @DisplayName("contains testing flags")
        void create_whenCalled_containsTestingFlags() {
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content)
                    .contains("| Smoke Tests | true |")
                    .contains("| Contract Tests | false |");
        }

        @Test
        @DisplayName("contains source of truth hierarchy")
        void create_whenCalled_containsSourceOfTruth() {
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content)
                    .contains(
                            "## Source of Truth (Hierarchy)")
                    .contains("1. Epics / PRDs")
                    .contains("5. Source code");
        }

        @Test
        @DisplayName("contains constraints section")
        void create_whenCalled_containsConstraints() {
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content)
                    .contains("## Constraints")
                    .contains("Cloud-Agnostic")
                    .contains("Horizontal scalability")
                    .contains("Externalized configuration");
        }

        @Test
        @DisplayName("starts with global behavior header")
        void create_withGlobalBehavior_starts() {
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content).startsWith(
                    "# Global Behavior & Language Policy");
        }

        @Test
        @DisplayName("ends with trailing newline")
        void create_withNewline_ends() {
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content).endsWith("\n");
        }

        @Test
        @DisplayName("message broker is always none")
        void create_whenCalled_messageBrokerAlwaysNone() {
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content)
                    .contains("| Message Broker | none |");
        }

        @Test
        @DisplayName("resilience is always mandatory")
        void create_whenCalled_resilienceAlwaysMandatory() {
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content).contains(
                    "| Resilience |"
                            + " Mandatory (always enabled) |");
        }
    }

    @Nested
    @DisplayName("extractInterfaces")
    class ExtractInterfaces {

        @Test
        @DisplayName("joins multiple interfaces")
        void create_whenCalled_joinsMultiple() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .clearInterfaces()
                    .addInterface("rest")
                    .addInterface("grpc")
                    .addInterface("event-consumer")
                    .build();

            String result =
                    RulesIdentity.extractInterfaces(config);

            assertThat(result)
                    .isEqualTo(
                            "rest, grpc, event-consumer");
        }

        @Test
        @DisplayName("returns none for empty interfaces")
        void create_empty_returnsNone() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .clearInterfaces()
                    .build();

            String result =
                    RulesIdentity.extractInterfaces(config);

            assertThat(result).isEqualTo("none");
        }
    }

    @Nested
    @DisplayName("formatFrameworkVersion")
    class FormatFrameworkVersion {

        @Test
        @DisplayName("formats version with leading space")
        void create_whenCalled_formatsVersion() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .framework("quarkus", "3.17")
                    .build();

            String result = RulesIdentity
                    .formatFrameworkVersion(config);

            assertThat(result).isEqualTo(" 3.17");
        }

        @Test
        @DisplayName("returns empty for empty version")
        void create_whenCalled_emptyVersion() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .framework("quarkus", "")
                    .build();

            String result = RulesIdentity
                    .formatFrameworkVersion(config);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("fallbackDomainContent")
    class FallbackDomainContent {

        @Test
        @DisplayName("contains project name")
        void create_whenCalled_containsProjectName() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .projectName("my-service")
                    .build();

            String content = RulesIdentity
                    .fallbackDomainContent(config);

            assertThat(content)
                    .contains("my-service")
                    .contains("{DOMAIN_NAME}");
        }
    }
}
