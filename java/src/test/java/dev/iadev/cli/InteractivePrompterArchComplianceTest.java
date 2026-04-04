package dev.iadev.cli;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for architecture pattern style, ArchUnit
 * validation, and compliance wizard steps.
 *
 * <p>Covers Gherkin scenarios GK-1 through GK-6 from
 * story-0017-0010.</p>
 */
@DisplayName("InteractivePrompter — arch + compliance")
class InteractivePrompterArchComplianceTest {

    @Nested
    @DisplayName("GK-1: non-java/kotlin skips arch style")
    class NonArchPatternLanguages {

        @ParameterizedTest
        @ValueSource(strings = {
                "go", "python", "rust", "typescript"})
        @DisplayName("skips arch pattern for non-java/kotlin")
        void promptArchPattern_nonJavaKotlin_skips(
                String lang) {
            assertThat(InteractivePrompter
                    .isArchPatternLanguage(lang))
                    .isFalse();
        }

        @Test
        @DisplayName("go wizard skips to compliance")
        void prompt_goLanguage_skipsArchPattern() {
            var mock = new MockTerminalProvider()
                    .addReadLine("go-service")
                    .addReadLine(
                            "A go service for gateway")
                    .addSelect("microservice")
                    .addSelect("go")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addMultiSelect(
                            List.of("pci-dss"))
                    .addConfirm(true);
            var prompter =
                    new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThat(
                    config.architecture().style())
                    .isEqualTo("microservice");
            assertThat(config
                    .architecture()
                    .validateWithArchUnit())
                    .isFalse();
            assertThat(
                    config.security().frameworks())
                    .containsExactly("pci-dss");
        }
    }

    @Nested
    @DisplayName("GK-2: java presents 5 arch options")
    class JavaArchPatternPresented {

        @Test
        @DisplayName("java presents architecture style")
        void promptArchPattern_java_presentsOptions() {
            assertThat(InteractivePrompter
                    .isArchPatternLanguage("java"))
                    .isTrue();
        }

        @Test
        @DisplayName("kotlin presents arch style")
        void promptArchPattern_kotlin_presentsOptions() {
            assertThat(InteractivePrompter
                    .isArchPatternLanguage("kotlin"))
                    .isTrue();
        }

        @Test
        @DisplayName("arch patterns has 5 options")
        void archPatternStyles_hasFiveOptions() {
            assertThat(LanguageFrameworkMapping
                    .ARCH_PATTERN_STYLES)
                    .containsExactly(
                            "layered", "hexagonal",
                            "cqrs", "event-driven",
                            "clean");
        }

        @Test
        @DisplayName("default is layered")
        void archPatternStyles_defaultIsLayered() {
            assertThat(LanguageFrameworkMapping
                    .ARCH_PATTERN_STYLES
                    .getFirst())
                    .isEqualTo("layered");
        }

        @Test
        @DisplayName("java selects layered maps correctly")
        void prompt_javaLayered_mapsToConfig() {
            var mock = new MockTerminalProvider()
                    .addReadLine("java-svc")
                    .addReadLine(
                            "A java service for testing")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("quarkus")
                    .addSelect("maven")
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
    @DisplayName("GK-3: hexagonal + ArchUnit mapping")
    class HexagonalArchUnitMapping {

        @Test
        @DisplayName("hexagonal with archunit yes")
        void prompt_hexagonalArchUnit_mapsCorrectly() {
            var mock = new MockTerminalProvider()
                    .addReadLine("hex-svc")
                    .addReadLine(
                            "A hexagonal service for "
                                    + "testing")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("spring-boot")
                    .addSelect("maven")
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
        @DisplayName("clean with archunit yes")
        void prompt_cleanArchUnit_mapsCorrectly() {
            var mock = new MockTerminalProvider()
                    .addReadLine("clean-svc")
                    .addReadLine(
                            "A clean arch service "
                                    + "for testing")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("quarkus")
                    .addSelect("maven")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addSelect("clean")
                    .addConfirm(true)
                    .addMultiSelect(List.of("none"))
                    .addConfirm(true);
            var prompter =
                    new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThat(
                    config.architecture().style())
                    .isEqualTo("clean");
            assertThat(config
                    .architecture()
                    .validateWithArchUnit())
                    .isTrue();
        }

        @Test
        @DisplayName("hexagonal with archunit no")
        void prompt_hexagonalNoArchUnit_mapsFalse() {
            var mock = new MockTerminalProvider()
                    .addReadLine("hex-svc")
                    .addReadLine(
                            "A hexagonal service no "
                                    + "archunit")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("quarkus")
                    .addSelect("maven")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addSelect("hexagonal")
                    .addConfirm(false)
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
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("GK-4: compliance multi-select mapping")
    class ComplianceMultiSelect {

        @Test
        @DisplayName("pci-dss and lgpd maps correctly")
        void prompt_pciDssLgpd_mapsToSecurityConfig() {
            var mock = new MockTerminalProvider()
                    .addReadLine("comp-svc")
                    .addReadLine(
                            "A service for compliance "
                                    + "testing")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("quarkus")
                    .addSelect("maven")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addSelect("layered")
                    .addMultiSelect(
                            List.of("pci-dss", "lgpd"))
                    .addConfirm(true);
            var prompter =
                    new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThat(
                    config.security().frameworks())
                    .containsExactly(
                            "pci-dss", "lgpd");
        }

        @Test
        @DisplayName("none maps to empty list")
        void prompt_complianceNone_mapsToEmpty() {
            var mock = new MockTerminalProvider()
                    .addReadLine("comp-svc")
                    .addReadLine(
                            "A service for compliance "
                                    + "testing")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("quarkus")
                    .addSelect("maven")
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
                    config.security().frameworks())
                    .isEmpty();
        }

        @Test
        @DisplayName("all compliance frameworks selected")
        void prompt_allCompliance_mapsAll() {
            var mock = new MockTerminalProvider()
                    .addReadLine("comp-svc")
                    .addReadLine(
                            "A service for full "
                                    + "compliance")
                    .addSelect("microservice")
                    .addSelect("go")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addMultiSelect(List.of(
                            "pci-dss", "lgpd",
                            "sox", "hipaa"))
                    .addConfirm(true);
            var prompter =
                    new InteractivePrompter(mock);
            ProjectConfig config = prompter.prompt();
            assertThat(
                    config.security().frameworks())
                    .containsExactly(
                            "pci-dss", "lgpd",
                            "sox", "hipaa");
        }
    }

    @Nested
    @DisplayName("GK-5: none + other is error")
    class ComplianceNoneConflict {

        @Test
        @DisplayName("none + pci-dss re-prompts")
        void resolveCompliance_noneAndOther_error() {
            var prompter = new InteractivePrompter(
                    new MockTerminalProvider()
                            .addMultiSelect(
                                    List.of("pci-dss")));
            List<String> result =
                    prompter.resolveCompliance(
                            List.of("none", "pci-dss"));
            assertThat(result)
                    .containsExactly("pci-dss");
        }

        @Test
        @DisplayName("error message displayed")
        void resolveCompliance_displaysError() {
            var mock = new MockTerminalProvider()
                    .addMultiSelect(
                            List.of("pci-dss"));
            var prompter =
                    new InteractivePrompter(mock);
            prompter.resolveCompliance(
                    List.of("none", "pci-dss"));
            assertThat(mock.getDisplayedMessages())
                    .contains(InteractivePrompter
                            .COMPLIANCE_NONE_ERROR);
        }
    }

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
    @DisplayName("Summary display with new fields")
    class SummaryWithNewFields {

        @Test
        @DisplayName("summary shows arch pattern")
        void prompt_archPattern_displayedInSummary() {
            var mock = new MockTerminalProvider()
                    .addReadLine("hex-svc")
                    .addReadLine(
                            "A service for summary "
                                    + "testing")
                    .addSelect("microservice")
                    .addSelect("java")
                    .addSelect("quarkus")
                    .addSelect("maven")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addSelect("hexagonal")
                    .addConfirm(true)
                    .addMultiSelect(
                            List.of("pci-dss", "lgpd"))
                    .addConfirm(true);
            var prompter =
                    new InteractivePrompter(mock);
            prompter.prompt();
            String summary = mock
                    .getDisplayedMessages()
                    .getFirst();
            assertThat(summary)
                    .contains("Arch Pattern:  hexagonal");
            assertThat(summary)
                    .contains("ArchUnit:      yes");
            assertThat(summary)
                    .contains(
                            "Compliance:    pci-dss, lgpd");
        }

        @Test
        @DisplayName("summary hides arch pattern for go")
        void prompt_goLanguage_noArchPatternInSummary() {
            var mock = new MockTerminalProvider()
                    .addReadLine("go-svc")
                    .addReadLine(
                            "A go service for summary "
                                    + "testing")
                    .addSelect("microservice")
                    .addSelect("go")
                    .addMultiSelect(List.of("rest"))
                    .addReadLine("")
                    .addReadLine("")
                    .addMultiSelect(List.of("none"))
                    .addConfirm(true);
            var prompter =
                    new InteractivePrompter(mock);
            prompter.prompt();
            String summary = mock
                    .getDisplayedMessages()
                    .getFirst();
            assertThat(summary)
                    .doesNotContain("Arch Pattern:");
            assertThat(summary)
                    .doesNotContain("ArchUnit:");
            assertThat(summary)
                    .contains("Compliance:    none");
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
