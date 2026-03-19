package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
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
        void containsProjectName() {
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
        void containsLanguageAndVersion() {
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
        void containsFrameworkWithVersion() {
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
        void containsArchitectureStyle() {
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
        void containsDddAndEventDriven() {
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
        void containsInterfacesList() {
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
        void containsBuildTool() {
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
        void containsNativeBuild() {
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
        void containsTestingFlags() {
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
        void containsSourceOfTruth() {
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
        void containsConstraints() {
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
        void startsWithGlobalBehavior() {
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content).startsWith(
                    "# Global Behavior & Language Policy");
        }

        @Test
        @DisplayName("ends with trailing newline")
        void endsWithNewline() {
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content).endsWith("\n");
        }

        @Test
        @DisplayName("message broker is always none")
        void messageBrokerAlwaysNone() {
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            String content =
                    RulesIdentity.buildContent(config);

            assertThat(content)
                    .contains("| Message Broker | none |");
        }

        @Test
        @DisplayName("resilience is always mandatory")
        void resilienceAlwaysMandatory() {
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
        void joinsMultiple() {
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
        void emptyReturnsNone() {
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
        void formatsVersion() {
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
        void emptyVersion() {
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
        void containsProjectName() {
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
