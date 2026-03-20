package dev.iadev.cli;
import dev.iadev.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
/**
 * Tests for InteractivePrompter — validation,
 * summary, config construction, and equivalence.
 */
@DisplayName("InteractivePrompter — validation")
class InteractivePrompterValidationTest {

    @Nested
    @DisplayName("Validation - kebab-case")
    class KebabCaseValidation {

        @ParameterizedTest
        @ValueSource(strings = {
                "my-project", "hello-world", "abc",
                "my-cool-app", "a-1", "test-123-app"
        })
        @DisplayName("valid kebab-case returns true")
        void isValidProjectName_valid_returnsTrue(
                String name) {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());
            assertThat(prompter.isValidProjectName(name))
                    .isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "MyProject", "my_project", "My-Project",
                "1-project", "a", "ab", "-project",
                "project-", "UPPERCASE", "has space"
        })
        @DisplayName("invalid kebab-case returns false")
        void isValidProjectName_invalid_returnsFalse(
                String name) {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());
            assertThat(prompter.isValidProjectName(name))
                    .isFalse();
        }

        @Test
        @DisplayName("null returns false")
        void isValidProjectName_null_returnsFalse() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());
            assertThat(prompter.isValidProjectName(null))
                    .isFalse();
        }

        @Test
        @DisplayName("invalid then valid re-prompts")
        void prompt_invalidThenValid_repropts() {
            var mock = new MockTerminalProvider()
                    .addReadLine("MyProject")
                    .addReadLine("my-project")
                    .addReadLine(
                            "A microservice for user management")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("quarkus")
                    .addSelect("maven")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addConfirm(true);
            var prompter = new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThat(config.project().name())
                    .isEqualTo("my-project");
            assertThat(mock.getDisplayedMessages())
                    .contains(
                            InteractivePrompter
                                    .KEBAB_ERROR);
        }
    }

    @Nested
    @DisplayName("Summary display")
    class SummaryDisplay {

        @Test
        @DisplayName("summary contains all fields")
        void prompt_displaysSummary_containsAll() {
            var mock = new MockTerminalProvider()
                    .addReadLine("my-project")
                    .addReadLine(
                            "A microservice for user management")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("quarkus")
                    .addSelect("maven")
                    .addMultiSelect(
                            List.of("rest", "grpc"))
                    .addReadLine("postgresql")
                    .addReadLine("redis")
                    .addConfirm(true);
            var prompter = new InteractivePrompter(mock);
            prompter.prompt();
            List<String> displayed =
                    mock.getDisplayedMessages();
            assertThat(displayed).hasSize(1);
            String summary = displayed.getFirst();
            assertThat(summary)
                    .contains(
                            "Project Configuration Summary:")
                    .contains("Name:")
                    .contains("Purpose:")
                    .contains("Architecture:")
                    .contains("Language:")
                    .contains("Framework:")
                    .contains("Build Tool:")
                    .contains("Interfaces:")
                    .contains("Database:")
                    .contains("Cache:");
        }

        @Test
        @DisplayName("empty database shows none")
        void prompt_emptyDatabase_showsNone() {
            var mock = new MockTerminalProvider()
                    .addReadLine("my-app")
                    .addReadLine(
                            "A simple application for testing")
                    .addSelect("library")
                    .addSelect("go")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addConfirm(true);
            var prompter = new InteractivePrompter(mock);
            prompter.prompt();
            String summary =
                    mock.getDisplayedMessages().getFirst();
            assertThat(summary)
                    .contains("Database:       none");
            assertThat(summary)
                    .contains("Cache:          none");
        }
    }

    @Nested
    @DisplayName("Config construction")
    class ConfigConstruction {

        @Test
        @DisplayName("sets correct defaults")
        void buildConfig_setsDefaults() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());
            ProjectConfig config = prompter.buildConfig(
                    new ProjectSummary(
                            "test-app",
                            "A test application for unit tests",
                            "microservice", "java",
                            "spring-boot", "maven",
                            List.of("rest"),
                            "postgresql", "redis"));
            assertThat(config.architecture().domainDriven())
                    .isFalse();
            assertThat(config.infrastructure().container())
                    .isEqualTo("docker");
            assertThat(config.testing().coverageLine())
                    .isEqualTo(95);
            assertThat(config.testing().coverageBranch())
                    .isEqualTo(90);
        }

        @Test
        @DisplayName("interfaces created correctly")
        void buildConfig_interfacesCreated() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());
            ProjectConfig config = prompter.buildConfig(
                    new ProjectSummary(
                            "my-app",
                            "Application for interface testing",
                            "microservice", "java",
                            "quarkus", "maven",
                            List.of("rest", "grpc", "cli"),
                            "", ""));
            assertThat(config.interfaces()).hasSize(3);
            assertThat(config.interfaces())
                    .extracting("type")
                    .containsExactly(
                            "rest", "grpc", "cli");
        }

        @Test
        @DisplayName("empty db/cache sets none")
        void buildConfig_emptyDbCache_setsNone() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());
            ProjectConfig config = prompter.buildConfig(
                    new ProjectSummary(
                            "my-app",
                            "Application for testing defaults",
                            "library", "typescript",
                            "nestjs", "npm",
                            List.of("rest"), "", ""));
            assertThat(config.data().database().name())
                    .isEqualTo("none");
            assertThat(config.data().cache().name())
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("language version resolved")
        void buildConfig_languageVersionResolved() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());
            ProjectConfig config = prompter.buildConfig(
                    new ProjectSummary(
                            "my-app",
                            "Application for version resolution",
                            "microservice", "python",
                            "fastapi", "pip",
                            List.of("rest"), "", ""));
            assertThat(config.language().version())
                    .isEqualTo("3.12");
            assertThat(config.framework().version())
                    .isEqualTo("0.115");
        }
    }

    @Nested
    @DisplayName("Purpose validation")
    class PurposeValidation {

        @Test
        @DisplayName("short purpose re-prompts")
        void prompt_shortPurpose_repropts() {
            var mock = new MockTerminalProvider()
                    .addReadLine("my-project")
                    .addReadLine("Too short")
                    .addReadLine(
                            "A valid purpose that is long enough")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("quarkus")
                    .addSelect("maven")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addConfirm(true);
            var prompter = new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThat(config.project().purpose())
                    .isEqualTo(
                            "A valid purpose that is long enough");
            assertThat(mock.getDisplayedMessages())
                    .contains(
                            InteractivePrompter
                                    .PURPOSE_ERROR);
        }
    }

    @Nested
    @DisplayName("Config equivalence")
    class ConfigEquivalence {

        @Test
        @DisplayName("has all required sections")
        void prompt_hasAllSections() {
            var mock = new MockTerminalProvider()
                    .addReadLine("my-project")
                    .addReadLine(
                            "A microservice for user management")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("quarkus")
                    .addSelect("maven")
                    .addMultiSelect(
                            List.of("rest", "grpc"))
                    .addReadLine("postgresql")
                    .addReadLine("redis")
                    .addConfirm(true);
            var prompter = new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThat(config.project().name())
                    .isEqualTo("my-project");
            assertThat(config.architecture().style())
                    .isNotBlank();
            assertThat(config.interfaces()).isNotEmpty();
            assertThat(config.language().name())
                    .isNotBlank();
        }

        @Test
        @DisplayName("config is immutable")
        void prompt_configIsImmutable() {
            var mock = new MockTerminalProvider()
                    .addReadLine("my-project")
                    .addReadLine(
                            "A microservice for user management")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("quarkus")
                    .addSelect("maven")
                    .addMultiSelect(
                            List.of("rest", "grpc"))
                    .addReadLine("postgresql")
                    .addReadLine("redis")
                    .addConfirm(true);
            var prompter = new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThatThrownBy(
                    () -> config.interfaces().add(null))
                    .isInstanceOf(
                            UnsupportedOperationException
                                    .class);
        }
    }
}
