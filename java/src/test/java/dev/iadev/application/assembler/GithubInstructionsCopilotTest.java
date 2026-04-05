package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GithubInstructionsAssembler —
 * buildCopilotInstructions and interface contract.
 */
@DisplayName("GithubInstructionsAssembler — copilot")
class GithubInstructionsCopilotTest {

    @Nested
    @DisplayName("assemble — implements Assembler")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            GithubInstructionsAssembler assembler =
                    new GithubInstructionsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("buildCopilotInstructions"
            + " — copilot-instructions.md generation")
    class BuildCopilotInstructions {

        @Test
        @DisplayName("contains project identity header")
        void buildCopilotInstructions_whenCalled_containsIdentityHeader() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-project")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result).contains(
                    "# Project Identity \u2014 my-project");
        }

        @Test
        @DisplayName("contains Identity section"
                + " with all fields")
        void buildCopilotInstructions_whenCalled_containsIdentitySection() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("api-pagamentos")
                            .archStyle("hexagonal")
                            .domainDriven(true)
                            .eventDriven(false)
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result)
                    .contains(
                            "- **Name:** api-pagamentos")
                    .contains(
                            "- **Architecture Style:**"
                                    + " hexagonal")
                    .contains(
                            "- **Domain-Driven Design:**"
                                    + " true")
                    .contains(
                            "- **Event-Driven:** false")
                    .contains(
                            "- **Language:** java 21")
                    .contains(
                            "- **Framework:** quarkus"
                                    + " 3.17");
        }

        @Test
        @DisplayName("contains Technology Stack table")
        void buildCopilotInstructions_whenCalled_containsStackTable() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("microservice")
                            .language("rust", "2024")
                            .framework("axum", "")
                            .buildTool("cargo")
                            .container("docker")
                            .orchestrator("kubernetes")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result)
                    .contains("## Technology Stack")
                    .contains(
                            "| Architecture |"
                                    + " Microservice |")
                    .contains(
                            "| Language | Rust 2024 |")
                    .contains("| Framework | Axum |")
                    .contains(
                            "| Build Tool | Cargo |")
                    .contains(
                            "| Container | Docker |")
                    .contains(
                            "| Orchestrator |"
                                    + " Kubernetes |")
                    .contains(
                            "| Resilience | Mandatory"
                                    + " (always enabled) |");
        }

        @Test
        @DisplayName("contains Constraints section")
        void buildCopilotInstructions_whenCalled_containsConstraints() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result)
                    .contains("## Constraints")
                    .contains("Cloud-Agnostic")
                    .contains("Horizontal scalability")
                    .contains(
                            "Externalized configuration");
        }

        @Test
        @DisplayName("contains Contextual Instructions"
                + " references")
        void buildCopilotInstructions_whenCalled_containsContextualRefs() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result)
                    .contains(
                            "## Contextual Instructions")
                    .contains(
                            "domain.instructions.md")
                    .contains(
                            "coding-standards"
                                    + ".instructions.md")
                    .contains(
                            "architecture"
                                    + ".instructions.md")
                    .contains(
                            "quality-gates"
                                    + ".instructions.md");
        }

        @Test
        @DisplayName("ends with trailing newline")
        void buildCopilotInstructions_whenCalled_endsWithTrailingNewline() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result).endsWith("\n");
            assertThat(result).doesNotEndWith("\n\n");
        }

        @Test
        @DisplayName("framework version appended"
                + " when present")
        void buildCopilotInstructions_whenCalled_frameworkVersionAppended() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result).contains(
                    "| Framework | Quarkus 3.17 |");
        }

        @Test
        @DisplayName("framework version omitted"
                + " when empty")
        void buildCopilotInstructions_whenCalled_frameworkVersionOmitted() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("axum", "")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .buildCopilotInstructions(
                                    config);

            assertThat(result).contains(
                    "| Framework | Axum |");
        }
    }
}
