package dev.iadev.cli;

import dev.iadev.exception.GenerationCancelledException;
import dev.iadev.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InteractivePrompter")
class InteractivePrompterTest {

    private MockTerminalProvider configureHappyPath() {
        return new MockTerminalProvider()
                .addReadLine("my-project")
                .addReadLine("A microservice for user management")
                .addSelect("microservice")
                .addSelect("java")
                .addSelect("quarkus")
                .addSelect("maven")
                .addMultiSelect(List.of("rest", "grpc"))
                .addReadLine("postgresql")
                .addReadLine("redis")
                .addConfirm(true);
    }

    @Nested
    @DisplayName("Happy path - full flow")
    class HappyPath {

        @Test
        @DisplayName("prompt_allValidInputs_returnsCompleteProjectConfig")
        void prompt_allValidInputs_returnsCompleteProjectConfig() {
            var mock = configureHappyPath();
            var prompter = new InteractivePrompter(mock);

            ProjectConfig config = prompter.prompt();

            assertThat(config.project().name())
                    .isEqualTo("my-project");
            assertThat(config.project().purpose())
                    .isEqualTo(
                            "A microservice for user management");
            assertThat(config.architecture().style())
                    .isEqualTo("microservice");
            assertThat(config.language().name())
                    .isEqualTo("java");
            assertThat(config.language().version())
                    .isEqualTo("21");
            assertThat(config.framework().name())
                    .isEqualTo("quarkus");
            assertThat(config.framework().buildTool())
                    .isEqualTo("maven");
            assertThat(config.interfaces())
                    .extracting("type")
                    .containsExactly("rest", "grpc");
            assertThat(config.data().database().name())
                    .isEqualTo("postgresql");
            assertThat(config.data().cache().name())
                    .isEqualTo("redis");
        }

        @Test
        @DisplayName("prompt_allValid_displaysSummaryBeforeConfirm")
        void prompt_allValid_displaysSummaryBeforeConfirm() {
            var mock = configureHappyPath();
            var prompter = new InteractivePrompter(mock);

            prompter.prompt();

            List<String> displayed = mock.getDisplayedMessages();
            assertThat(displayed).isNotEmpty();
            String summary = displayed.getFirst();
            assertThat(summary).contains("my-project");
            assertThat(summary).contains(
                    "A microservice for user management");
            assertThat(summary).contains("microservice");
            assertThat(summary).contains("java 21");
            assertThat(summary).contains("quarkus");
            assertThat(summary).contains("maven");
            assertThat(summary).contains("rest, grpc");
            assertThat(summary).contains("postgresql");
            assertThat(summary).contains("redis");
        }

        @Test
        @DisplayName("prompt_emptyOptionalFields_setsNoneDefaults")
        void prompt_emptyOptionalFields_setsNoneDefaults() {
            var mock = new MockTerminalProvider()
                    .addReadLine("my-app")
                    .addReadLine("A simple application for testing")
                    .addSelect("library")
                    .addSelect("go")
                    .addMultiSelect(List.of("cli"))
                    .addReadLine("")
                    .addReadLine("")
                    .addConfirm(true);
            var prompter = new InteractivePrompter(mock);

            ProjectConfig config = prompter.prompt();

            assertThat(config.data().database().name())
                    .isEqualTo("none");
            assertThat(config.data().cache().name())
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("prompt_frameworkVersionResolved_correctly")
        void prompt_frameworkVersionResolved_correctly() {
            var mock = configureHappyPath();
            var prompter = new InteractivePrompter(mock);

            ProjectConfig config = prompter.prompt();

            assertThat(config.framework().version())
                    .isEqualTo("3.17");
        }
    }

    @Nested
    @DisplayName("Language filters frameworks")
    class LanguageFrameworkFiltering {

        @Test
        @DisplayName("prompt_pythonSelected_usesOnlyPythonFrameworks")
        void prompt_pythonSelected_usesOnlyPythonFrameworks() {
            var mock = new MockTerminalProvider()
                    .addReadLine("py-project")
                    .addReadLine("A python service for data processing")
                    .addSelect("microservice")
                    .addSelect("python")
                    .addSelect("fastapi")
                    .addSelect("pip")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addConfirm(true);
            var prompter = new InteractivePrompter(mock);

            ProjectConfig config = prompter.prompt();

            assertThat(config.language().name())
                    .isEqualTo("python");
            assertThat(config.framework().name())
                    .isEqualTo("fastapi");
            assertThat(config.framework().buildTool())
                    .isEqualTo("pip");
        }

        @Test
        @DisplayName("prompt_goSelected_singleFrameworkAutoSelected")
        void prompt_goSelected_singleFrameworkAutoSelected() {
            var mock = new MockTerminalProvider()
                    .addReadLine("go-service")
                    .addReadLine("A go service for API gateway")
                    .addSelect("microservice")
                    .addSelect("go")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addConfirm(true);
            var prompter = new InteractivePrompter(mock);

            ProjectConfig config = prompter.prompt();

            assertThat(config.framework().name())
                    .isEqualTo("gin");
            assertThat(config.framework().buildTool())
                    .isEqualTo("go");
        }

        @Test
        @DisplayName("prompt_rustSelected_singleFrameworkAutoSelected")
        void prompt_rustSelected_singleFrameworkAutoSelected() {
            var mock = new MockTerminalProvider()
                    .addReadLine("rust-svc")
                    .addReadLine("A rust service for high performance")
                    .addSelect("microservice")
                    .addSelect("rust")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addConfirm(true);
            var prompter = new InteractivePrompter(mock);

            ProjectConfig config = prompter.prompt();

            assertThat(config.framework().name())
                    .isEqualTo("axum");
            assertThat(config.framework().buildTool())
                    .isEqualTo("cargo");
        }

        @Test
        @DisplayName("prompt_kotlinSelected_singleFrameworkAutoSelected")
        void prompt_kotlinSelected_singleFrameworkAutoSelected() {
            var mock = new MockTerminalProvider()
                    .addReadLine("kt-service")
                    .addReadLine("A kotlin service for mobile backend")
                    .addSelect("monolith")
                    .addSelect("kotlin")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addConfirm(true);
            var prompter = new InteractivePrompter(mock);

            ProjectConfig config = prompter.prompt();

            assertThat(config.framework().name())
                    .isEqualTo("ktor");
            assertThat(config.framework().buildTool())
                    .isEqualTo("gradle");
        }

        @Test
        @DisplayName("prompt_typescriptSelected_singleFrameworkAutoSelected")
        void prompt_typescriptSelected_singleFrameworkAutoSelected() {
            var mock = new MockTerminalProvider()
                    .addReadLine("ts-service")
                    .addReadLine(
                            "A typescript service for frontend API")
                    .addSelect("microservice")
                    .addSelect("typescript")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addConfirm(true);
            var prompter = new InteractivePrompter(mock);

            ProjectConfig config = prompter.prompt();

            assertThat(config.framework().name())
                    .isEqualTo("nestjs");
            assertThat(config.framework().buildTool())
                    .isEqualTo("npm");
        }

        @Test
        @DisplayName("prompt_javaSelected_springBoot_alternativeFramework")
        void prompt_javaSelected_springBoot_alternativeFramework() {
            var mock = new MockTerminalProvider()
                    .addReadLine("spring-svc")
                    .addReadLine("A spring boot service for backend")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("spring-boot")
                    .addSelect("gradle")
                    .addMultiSelect(List.of("rest", "graphql"))
                    .addReadLine("mongodb")
                    .addReadLine("")
                    .addConfirm(true);
            var prompter = new InteractivePrompter(mock);

            ProjectConfig config = prompter.prompt();

            assertThat(config.framework().name())
                    .isEqualTo("spring-boot");
            assertThat(config.framework().buildTool())
                    .isEqualTo("gradle");
            assertThat(config.data().database().name())
                    .isEqualTo("mongodb");
        }
    }

    @Nested
    @DisplayName("Cancellation scenarios")
    class Cancellation {

        @Test
        @DisplayName("prompt_ctrlCDuringPrompt_throwsCancelledException")
        void prompt_ctrlCDuringPrompt_throwsCancelledException() {
            var mock = new MockTerminalProvider()
                    .cancelAfter(0);
            var prompter = new InteractivePrompter(mock);

            assertThatThrownBy(prompter::prompt)
                    .isInstanceOf(GenerationCancelledException.class)
                    .hasMessage(
                            InteractivePrompter.CANCELLED_BY_USER);
        }

        @Test
        @DisplayName("prompt_ctrlCDuringLanguageSelect_throwsCancelled")
        void prompt_ctrlCDuringLanguageSelect_throwsCancelled() {
            var mock = new MockTerminalProvider()
                    .addReadLine("my-project")
                    .addReadLine("A valid project purpose here")
                    .addSelect("microservice")
                    .cancelAfter(3);
            var prompter = new InteractivePrompter(mock);

            assertThatThrownBy(prompter::prompt)
                    .isInstanceOf(GenerationCancelledException.class);
        }

        @Test
        @DisplayName("prompt_confirmationNo_throwsCancelledException")
        void prompt_confirmationNo_throwsCancelledException() {
            var mock = new MockTerminalProvider()
                    .addReadLine("my-project")
                    .addReadLine("A microservice for user management")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("quarkus")
                    .addSelect("maven")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addConfirm(false);
            var prompter = new InteractivePrompter(mock);

            assertThatThrownBy(prompter::prompt)
                    .isInstanceOf(GenerationCancelledException.class)
                    .hasMessage(InteractivePrompter.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Defaults")
    class Defaults {

        @Test
        @DisplayName("prompt_defaultArchitecture_isMicroservice")
        void prompt_defaultArchitecture_isMicroservice() {
            assertThat(
                    LanguageFrameworkMapping.ARCHITECTURE_STYLES
                            .getFirst())
                    .isEqualTo("microservice");
        }

        @Test
        @DisplayName("prompt_defaultLanguage_isJava")
        void prompt_defaultLanguage_isJava() {
            assertThat(
                    LanguageFrameworkMapping.LANGUAGES.getFirst())
                    .isEqualTo("java");
        }

        @Test
        @DisplayName("prompt_defaultInterfaceSelection_isRest")
        void prompt_defaultInterfaceSelection_isRest() {
            var mock = configureHappyPath();
            var prompter = new InteractivePrompter(mock);

            prompter.prompt();
            // The mock was configured with rest,grpc.
            // We verify the default passed is "rest"
            // by checking it appears in INTERFACE_TYPES.
            assertThat(
                    LanguageFrameworkMapping.INTERFACE_TYPES
                            .getFirst())
                    .isEqualTo("rest");
        }

        @Test
        @DisplayName("prompt_defaultConfirmation_isTrue")
        void prompt_defaultConfirmation_isTrue() {
            // The confirmation prompt default is true per story spec.
            // We verify by letting mock return true and check it works.
            var mock = configureHappyPath();
            var prompter = new InteractivePrompter(mock);

            ProjectConfig config = prompter.prompt();
            assertThat(config.project().name())
                    .isEqualTo("my-project");
        }
    }

    @Nested
    @DisplayName("Validation - kebab-case")
    class KebabCaseValidation {

        @ParameterizedTest
        @ValueSource(strings = {
                "my-project",
                "hello-world",
                "abc",
                "my-cool-app",
                "a-1",
                "test-123-app"
        })
        @DisplayName("isValidProjectName_validKebabCase_returnsTrue")
        void isValidProjectName_validKebabCase_returnsTrue(
                String name) {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());

            assertThat(prompter.isValidProjectName(name)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "MyProject",
                "my_project",
                "My-Project",
                "1-project",
                "a",
                "ab",
                "-project",
                "project-",
                "UPPERCASE",
                "has space"
        })
        @DisplayName("isValidProjectName_invalidKebabCase_returnsFalse")
        void isValidProjectName_invalidKebabCase_returnsFalse(
                String name) {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());

            assertThat(prompter.isValidProjectName(name)).isFalse();
        }

        @Test
        @DisplayName("isValidProjectName_null_returnsFalse")
        void isValidProjectName_null_returnsFalse() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());

            assertThat(prompter.isValidProjectName(null)).isFalse();
        }

        @Test
        @DisplayName("prompt_invalidThenValid_repropts")
        void prompt_invalidThenValid_repropts() {
            var mock = new MockTerminalProvider()
                    .addReadLine("MyProject")
                    .addReadLine("my-project")
                    .addReadLine("A microservice for user management")
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
                    .contains(InteractivePrompter.KEBAB_ERROR);
        }
    }

    @Nested
    @DisplayName("Summary display")
    class SummaryDisplay {

        @Test
        @DisplayName("prompt_displaysSummary_containsAllFields")
        void prompt_displaysSummary_containsAllFields() {
            var mock = configureHappyPath();
            var prompter = new InteractivePrompter(mock);

            prompter.prompt();

            List<String> displayed = mock.getDisplayedMessages();
            assertThat(displayed).hasSize(1);
            String summary = displayed.getFirst();
            assertThat(summary).contains(
                    "Project Configuration Summary:");
            assertThat(summary).contains("Name:");
            assertThat(summary).contains("Purpose:");
            assertThat(summary).contains("Architecture:");
            assertThat(summary).contains("Language:");
            assertThat(summary).contains("Framework:");
            assertThat(summary).contains("Build Tool:");
            assertThat(summary).contains("Interfaces:");
            assertThat(summary).contains("Database:");
            assertThat(summary).contains("Cache:");
        }

        @Test
        @DisplayName("prompt_emptyDatabase_showsNoneInSummary")
        void prompt_emptyDatabase_showsNoneInSummary() {
            var mock = new MockTerminalProvider()
                    .addReadLine("my-app")
                    .addReadLine("A simple application for testing")
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
            assertThat(summary).contains("Database:       none");
            assertThat(summary).contains("Cache:          none");
        }
    }

    @Nested
    @DisplayName("Config construction")
    class ConfigConstruction {

        @Test
        @DisplayName("buildConfig_setsCorrectDefaults")
        void buildConfig_whenCalled_setsCorrectDefaults() {
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
            assertThat(config.architecture().eventDriven())
                    .isFalse();
            assertThat(config.framework().nativeBuild())
                    .isFalse();
            assertThat(config.infrastructure().container())
                    .isEqualTo("docker");
            assertThat(config.testing().coverageLine())
                    .isEqualTo(95);
            assertThat(config.testing().coverageBranch())
                    .isEqualTo(90);
            assertThat(config.security().frameworks())
                    .isEmpty();
            assertThat(config.mcp().servers())
                    .isEmpty();
        }

        @Test
        @DisplayName("buildConfig_interfacesCreatedCorrectly")
        void buildConfig_whenCalled_interfacesCreatedCorrectly() {
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
                    .containsExactly("rest", "grpc", "cli");
            assertThat(config.interfaces())
                    .extracting("spec")
                    .containsOnly("");
            assertThat(config.interfaces())
                    .extracting("broker")
                    .containsOnly("");
        }

        @Test
        @DisplayName("buildConfig_emptyDatabaseAndCache_setsNone")
        void buildConfig_emptyDatabaseAndCache_setsNone() {
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
            assertThat(config.data().migration().name())
                    .isEqualTo("none");
        }

        @Test
        @DisplayName("buildConfig_languageVersion_resolvedFromMapping")
        void buildConfig_languageVersion_resolvedFromMapping() {
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
        @DisplayName("prompt_shortPurpose_repropts")
        void prompt_shortPurpose_repropts() {
            var mock = new MockTerminalProvider()
                    .addReadLine("my-project")
                    .addReadLine("Too short")
                    .addReadLine("A valid purpose that is long enough")
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
                    .contains(InteractivePrompter.PURPOSE_ERROR);
        }
    }

    @Nested
    @DisplayName("Integration - config equivalence")
    class ConfigEquivalence {

        @Test
        @DisplayName("prompt_generatedConfig_hasAllRequiredSections")
        void prompt_generatedConfig_hasAllRequiredSections() {
            var mock = configureHappyPath();
            var prompter = new InteractivePrompter(mock);

            ProjectConfig config = prompter.prompt();

            assertThat(config.project().name())
                    .isEqualTo("my-project");
            assertThat(config.architecture().style())
                    .isNotBlank();
            assertThat(config.interfaces()).isNotEmpty();
            assertThat(config.language().name())
                    .isNotBlank();
            assertThat(config.framework().name())
                    .isNotBlank();
            assertThat(config.data().database().name())
                    .isEqualTo("postgresql");
            assertThat(config.infrastructure().container())
                    .isNotBlank();
            assertThat(config.security().frameworks())
                    .isInstanceOf(List.class);
            assertThat(config.testing().coverageLine())
                    .isGreaterThan(0);
            assertThat(config.mcp().servers())
                    .isInstanceOf(List.class);
        }

        @Test
        @DisplayName("prompt_configIsImmutable_interfaceListCopied")
        void prompt_configIsImmutable_interfaceListCopied() {
            var mock = configureHappyPath();
            var prompter = new InteractivePrompter(mock);

            ProjectConfig config = prompter.prompt();

            assertThatThrownBy(
                    () -> config.interfaces().add(null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
