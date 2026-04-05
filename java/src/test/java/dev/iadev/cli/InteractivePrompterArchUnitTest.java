package dev.iadev.cli;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ArchUnit conditional prompting and buildConfig
 * with new fields.
 *
 * <p>Covers GK-6 from story-0017-0010.</p>
 */
@DisplayName("InteractivePrompter — ArchUnit + buildConfig")
class InteractivePrompterArchUnitTest {

    @Nested
    @DisplayName("GK-6: hexagonal shows ArchUnit, "
            + "layered does not")
    class ArchUnitConditional {

        @Test
        @DisplayName("hexagonal triggers archunit prompt")
        void promptValidateArchUnit_hexagonal_prompts() {
            var mock = new MockTerminalProvider()
                    .addConfirm(false);
            var prompter =
                    new InteractivePrompter(mock);
            boolean result =
                    prompter.promptValidateArchUnit(
                            "hexagonal");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("clean triggers archunit prompt")
        void promptValidateArchUnit_clean_prompts() {
            var mock = new MockTerminalProvider()
                    .addConfirm(true);
            var prompter =
                    new InteractivePrompter(mock);
            boolean result =
                    prompter.promptValidateArchUnit(
                            "clean");
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("layered skips archunit prompt")
        void promptValidateArchUnit_layered_skips() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());
            boolean result =
                    prompter.promptValidateArchUnit(
                            "layered");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("cqrs skips archunit prompt")
        void promptValidateArchUnit_cqrs_skips() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());
            boolean result =
                    prompter.promptValidateArchUnit(
                            "cqrs");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("event-driven skips archunit")
        void promptValidateArchUnit_eventDriven_skips() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());
            boolean result =
                    prompter.promptValidateArchUnit(
                            "event-driven");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("empty skips archunit prompt")
        void promptValidateArchUnit_empty_skips() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());
            boolean result =
                    prompter.promptValidateArchUnit("");
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("kotlin hexagonal shows archunit")
        void prompt_kotlinHexagonal_showsArchUnit() {
            var mock = new MockTerminalProvider()
                    .addReadLine("kt-hex-svc")
                    .addReadLine(
                            "A kotlin hexagonal service")
                    .addSelect("microservice")
                    .addSelect("kotlin")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addSelect("hexagonal")
                    .addConfirm(true)
                    .addMultiSelect(List.of("none"))
                    .addConfirm(true);
            var prompter =
                    new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThat(
                    config.architecture().style())
                    .isEqualTo("hexagonal");
            assertThat(config
                    .architecture()
                    .validateWithArchUnit())
                    .isTrue();
        }

        @Test
        @DisplayName("kotlin layered no archunit prompt")
        void prompt_kotlinLayered_noArchUnit() {
            var mock = new MockTerminalProvider()
                    .addReadLine("kt-lay-svc")
                    .addReadLine(
                            "A kotlin layered service "
                                    + "testing")
                    .addSelect("microservice")
                    .addSelect("kotlin")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addSelect("layered")
                    .addMultiSelect(List.of("none"))
                    .addConfirm(true);
            var prompter =
                    new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThat(
                    config.architecture().style())
                    .isEqualTo("layered");
            assertThat(config
                    .architecture()
                    .validateWithArchUnit())
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("buildConfig with new fields")
    class BuildConfigNewFields {

        @Test
        @DisplayName("arch pattern overrides style")
        void buildConfig_archPattern_overridesStyle() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());
            ProjectConfig config =
                    prompter.buildConfig(
                            new ProjectSummary(
                                    "test-app",
                                    "A test application "
                                            + "for arch",
                                    "microservice",
                                    "java",
                                    "quarkus", "maven",
                                    List.of("rest"),
                                    "", "",
                                    "hexagonal",
                                    true,
                                    List.of("pci-dss")));
            assertThat(
                    config.architecture().style())
                    .isEqualTo("hexagonal");
            assertThat(config
                    .architecture()
                    .validateWithArchUnit())
                    .isTrue();
            assertThat(
                    config.security().frameworks())
                    .containsExactly("pci-dss");
        }

        @Test
        @DisplayName("empty arch pattern uses archStyle")
        void buildConfig_noArchPattern_usesArchStyle() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());
            ProjectConfig config =
                    prompter.buildConfig(
                            new ProjectSummary(
                                    "test-app",
                                    "A test application "
                                            + "for style",
                                    "microservice", "go",
                                    "gin", "go",
                                    List.of("rest"),
                                    "", "",
                                    "", false,
                                    List.of()));
            assertThat(
                    config.architecture().style())
                    .isEqualTo("microservice");
        }

        @Test
        @DisplayName("compliance maps to security config")
        void buildConfig_compliance_mapsToSecurity() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider());
            ProjectConfig config =
                    prompter.buildConfig(
                            new ProjectSummary(
                                    "test-app",
                                    "A test application "
                                            + "for compliance",
                                    "microservice", "go",
                                    "gin", "go",
                                    List.of("rest"),
                                    "", "",
                                    "", false,
                                    List.of("lgpd",
                                            "hipaa")));
            assertThat(
                    config.security().frameworks())
                    .containsExactly("lgpd", "hipaa");
        }
    }
}
