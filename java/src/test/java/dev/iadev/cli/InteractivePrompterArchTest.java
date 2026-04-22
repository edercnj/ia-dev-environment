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
 * Tests for architecture pattern style selection and
 * mapping.
 *
 * <p>Covers GK-1, GK-2, GK-3 from story-0017-0010.</p>
 */
@DisplayName("InteractivePrompter — arch style")
class InteractivePrompterArchTest {

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

        // prompt_goLanguage_skipsArchPattern removed in EPIC-0048 full
        // cleanup — go no longer selectable.
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
        @DisplayName("kotlin no longer presents arch style (EPIC-0048)")
        void promptArchPattern_kotlin_nowExcluded() {
            assertThat(InteractivePrompter
                    .isArchPatternLanguage("kotlin"))
                    .isFalse();
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
}
