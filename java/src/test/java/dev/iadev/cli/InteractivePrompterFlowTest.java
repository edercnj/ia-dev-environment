package dev.iadev.cli;
import dev.iadev.exception.GenerationCancelledException;
import dev.iadev.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
/**
 * Tests for InteractivePrompter — happy path,
 * language filtering, cancellation, and defaults.
 */
@DisplayName("InteractivePrompter — flow")
class InteractivePrompterFlowTest {

    private MockTerminalProvider configureHappyPath() {
        return new MockTerminalProvider()
                .addReadLine("my-project")
                .addReadLine(
                        "A microservice for user management")
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
        @DisplayName("returns complete ProjectConfig")
        void prompt_allValidInputs_returnsComplete() {
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
        @DisplayName("displays summary before confirm")
        void prompt_allValid_displaysSummary() {
            var mock = configureHappyPath();
            var prompter = new InteractivePrompter(mock);
            prompter.prompt();
            List<String> displayed =
                    mock.getDisplayedMessages();
            assertThat(displayed).isNotEmpty();
            String summary = displayed.getFirst();
            assertThat(summary).contains("my-project");
            assertThat(summary).contains("microservice");
            assertThat(summary).contains("java 21");
            assertThat(summary).contains("quarkus");
            assertThat(summary).contains("maven");
            assertThat(summary).contains("rest, grpc");
            assertThat(summary).contains("postgresql");
            assertThat(summary).contains("redis");
        }

        @Test
        @DisplayName("empty optional sets none defaults")
        void prompt_emptyOptionalFields_setsNone() {
            var mock = new MockTerminalProvider()
                    .addReadLine("my-app")
                    .addReadLine(
                            "A simple application for testing")
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
        @DisplayName("framework version resolved")
        void prompt_frameworkVersionResolved() {
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
        @DisplayName("python uses python frameworks")
        void prompt_pythonSelected() {
            var mock = new MockTerminalProvider()
                    .addReadLine("py-project")
                    .addReadLine(
                            "A python service for data processing")
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
        }

        @Test
        @DisplayName("go auto-selects framework")
        void prompt_goSelected() {
            var mock = new MockTerminalProvider()
                    .addReadLine("go-service")
                    .addReadLine(
                            "A go service for API gateway")
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
        @DisplayName("rust auto-selects axum")
        void prompt_rustSelected() {
            var mock = new MockTerminalProvider()
                    .addReadLine("rust-svc")
                    .addReadLine(
                            "A rust service for high performance")
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
        }

        @Test
        @DisplayName("kotlin auto-selects ktor")
        void prompt_kotlinSelected() {
            var mock = new MockTerminalProvider()
                    .addReadLine("kt-service")
                    .addReadLine(
                            "A kotlin service for mobile backend")
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
        }

        @Test
        @DisplayName("typescript auto-selects nestjs")
        void prompt_typescriptSelected() {
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
        }

        @Test
        @DisplayName("java spring-boot alternative")
        void prompt_javaSpringBoot() {
            var mock = new MockTerminalProvider()
                    .addReadLine("spring-svc")
                    .addReadLine(
                            "A spring boot service for backend")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("spring-boot")
                    .addSelect("gradle")
                    .addMultiSelect(
                            List.of("rest", "graphql"))
                    .addReadLine("mongodb")
                    .addReadLine("")
                    .addConfirm(true);
            var prompter = new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThat(config.framework().name())
                    .isEqualTo("spring-boot");
            assertThat(config.data().database().name())
                    .isEqualTo("mongodb");
        }
    }

    @Nested
    @DisplayName("Cancellation scenarios")
    class Cancellation {

        @Test
        @DisplayName("ctrl-C throws cancelled")
        void prompt_ctrlC_throwsCancelled() {
            var mock = new MockTerminalProvider()
                    .cancelAfter(0);
            var prompter = new InteractivePrompter(mock);
            assertThatThrownBy(prompter::prompt)
                    .isInstanceOf(
                            GenerationCancelledException
                                    .class)
                    .hasMessage(
                            InteractivePrompter
                                    .CANCELLED_BY_USER);
        }

        @Test
        @DisplayName("ctrl-C during language select")
        void prompt_ctrlCDuringLanguage() {
            var mock = new MockTerminalProvider()
                    .addReadLine("my-project")
                    .addReadLine(
                            "A valid project purpose here")
                    .addSelect("microservice")
                    .cancelAfter(3);
            var prompter = new InteractivePrompter(mock);
            assertThatThrownBy(prompter::prompt)
                    .isInstanceOf(
                            GenerationCancelledException
                                    .class);
        }

        @Test
        @DisplayName("confirmation no throws cancelled")
        void prompt_confirmationNo_throwsCancelled() {
            var mock = new MockTerminalProvider()
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
                    .addConfirm(false);
            var prompter = new InteractivePrompter(mock);
            assertThatThrownBy(prompter::prompt)
                    .isInstanceOf(
                            GenerationCancelledException
                                    .class)
                    .hasMessage(
                            InteractivePrompter.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Defaults")
    class Defaults {

        @Test
        @DisplayName("default architecture is microservice")
        void prompt_defaultArchitecture() {
            assertThat(
                    LanguageFrameworkMapping
                            .ARCHITECTURE_STYLES
                            .getFirst())
                    .isEqualTo("microservice");
        }

        @Test
        @DisplayName("default language is java")
        void prompt_defaultLanguage() {
            assertThat(
                    LanguageFrameworkMapping.LANGUAGES
                            .getFirst())
                    .isEqualTo("java");
        }

        @Test
        @DisplayName("default interface is rest")
        void prompt_defaultInterface() {
            assertThat(
                    LanguageFrameworkMapping
                            .INTERFACE_TYPES
                            .getFirst())
                    .isEqualTo("rest");
        }

        @Test
        @DisplayName("default confirmation is true")
        void prompt_defaultConfirmation() {
            var mock = configureHappyPath();
            var prompter = new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThat(config.project().name())
                    .isEqualTo("my-project");
        }
    }
}
