package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GithubAgentsAssembler —
 * event-engineer conditions and empty result.
 */
@DisplayName("GithubAgentsAssembler — events")
class GithubAgentsEventTest {

    @Nested
    @DisplayName("event conditional agents")
    class EventConditionalAgents {

        @Test
        @DisplayName("event-engineer when event-driven")
        void assemble_whenEventDriven_eventEngineer() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .eventDriven(true)
                            .clearInterfaces()
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("event-engineer.md");
        }

        @Test
        @DisplayName("event-engineer when consumer")
        void assemble_whenEventConsumer_eventEngineer() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("event-consumer")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("event-engineer.md");
        }

        @Test
        @DisplayName("event-engineer when producer")
        void assemble_whenEventProducer_eventEngineer() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("event-producer")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .contains("event-engineer.md");
        }

        @Test
        @DisplayName("no event-engineer when no events")
        void assemble_whenNoEvents_noEventEngineer() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .eventDriven(false)
                            .clearInterfaces()
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents)
                    .doesNotContain(
                            "event-engineer.md");
        }

        @Test
        @DisplayName("empty when all conditions false")
        void assemble_whenAllFalse_empty() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .eventDriven(false)
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            List<String> agents =
                    GithubAgentsAssembler
                            .selectGithubConditionalAgents(
                                    config);

            assertThat(agents).isEmpty();
        }
    }
}
