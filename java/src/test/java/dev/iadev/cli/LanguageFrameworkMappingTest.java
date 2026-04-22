package dev.iadev.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LanguageFrameworkMapping (Java-only since v4.0.0 / EPIC-0048)")
class LanguageFrameworkMappingTest {

    @Nested
    @DisplayName("frameworksFor")
    class FrameworksFor {

        @Test
        @DisplayName("frameworksFor_java_returnsSpringBootAndQuarkus")
        void frameworksFor_java_returnsSpringBootAndQuarkus() {
            assertThat(LanguageFrameworkMapping.frameworksFor("java"))
                    .containsExactly("spring-boot", "quarkus");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "python", "go", "kotlin", "typescript", "rust", "csharp"})
        @DisplayName("frameworksFor_nonJava_returnsEmptyList")
        void frameworksFor_nonJava_returnsEmptyList(String lang) {
            assertThat(
                    LanguageFrameworkMapping.frameworksFor(lang))
                    .isEmpty();
        }

        @Test
        @DisplayName("frameworksFor_unknown_returnsEmptyList")
        void frameworksFor_unknown_returnsEmptyList() {
            assertThat(
                    LanguageFrameworkMapping.frameworksFor("unknown"))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("buildToolsFor")
    class BuildToolsFor {

        @Test
        @DisplayName("buildToolsFor_java_returnsMavenAndGradle")
        void buildToolsFor_java_returnsMavenAndGradle() {
            assertThat(
                    LanguageFrameworkMapping.buildToolsFor("java"))
                    .containsExactly("maven", "gradle");
        }

        @ParameterizedTest
        @ValueSource(strings = {"python", "go", "kotlin", "typescript", "rust"})
        @DisplayName("buildToolsFor_nonJava_returnsEmptyList")
        void buildToolsFor_nonJava_returnsEmptyList(String lang) {
            assertThat(
                    LanguageFrameworkMapping.buildToolsFor(lang))
                    .isEmpty();
        }

        @Test
        @DisplayName("buildToolsFor_unknown_returnsEmptyList")
        void buildToolsFor_unknown_returnsEmptyList() {
            assertThat(LanguageFrameworkMapping
                    .buildToolsFor("unknown"))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("defaultVersionFor")
    class DefaultVersionFor {

        @Test
        @DisplayName("defaultVersionFor_java_returns21")
        void defaultVersionFor_java_returns21() {
            assertThat(
                    LanguageFrameworkMapping.defaultVersionFor("java"))
                    .isEqualTo("21");
        }

        @ParameterizedTest
        @ValueSource(strings = {"python", "go", "kotlin", "typescript", "rust"})
        @DisplayName("defaultVersionFor_nonJava_returnsEmptyString")
        void defaultVersionFor_nonJava_returnsEmptyString(String lang) {
            assertThat(LanguageFrameworkMapping
                    .defaultVersionFor(lang))
                    .isEmpty();
        }

        @Test
        @DisplayName("defaultVersionFor_unknown_returnsEmptyString")
        void defaultVersionFor_unknown_returnsEmptyString() {
            assertThat(LanguageFrameworkMapping
                    .defaultVersionFor("unknown"))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("frameworkVersionFor")
    class FrameworkVersionFor {

        @Test
        @DisplayName("frameworkVersionFor_springBoot_returns3.4")
        void frameworkVersionFor_springBoot() {
            assertThat(LanguageFrameworkMapping
                    .frameworkVersionFor("spring-boot"))
                    .isEqualTo("3.4");
        }

        @Test
        @DisplayName("frameworkVersionFor_quarkus_returns3.17")
        void frameworkVersionFor_quarkus() {
            assertThat(LanguageFrameworkMapping
                    .frameworkVersionFor("quarkus"))
                    .isEqualTo("3.17");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "fastapi", "click-cli", "gin", "ktor", "nestjs", "axum"})
        @DisplayName("frameworkVersionFor_removedFramework_returnsEmpty")
        void frameworkVersionFor_removedFramework_returnsEmpty(
                String fw) {
            assertThat(LanguageFrameworkMapping
                    .frameworkVersionFor(fw))
                    .isEmpty();
        }

        @Test
        @DisplayName("frameworkVersionFor_unknown_returnsEmpty")
        void frameworkVersionFor_unknown_returnsEmpty() {
            assertThat(LanguageFrameworkMapping
                    .frameworkVersionFor("unknown"))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Static constants (Java-only)")
    class StaticConstants {

        @Test
        @DisplayName("languages_containsOnlyJava")
        void languages_whenCalled_containsOnlyJava() {
            assertThat(LanguageFrameworkMapping.LANGUAGES)
                    .containsExactly("java");
        }

        @Test
        @DisplayName("architectureStyles_containsThreeEntries")
        void architectureStyles_whenCalled_containsThreeEntries() {
            assertThat(LanguageFrameworkMapping.ARCHITECTURE_STYLES)
                    .hasSize(3)
                    .containsExactly(
                            "microservice", "monolith", "library");
        }

        @Test
        @DisplayName("archPatternLanguages_containsOnlyJava")
        void archPatternLanguages_containsOnlyJava() {
            assertThat(
                    LanguageFrameworkMapping.ARCH_PATTERN_LANGUAGES)
                    .containsExactly("java");
        }

        @Test
        @DisplayName("interfaceTypes_containsFiveEntries")
        void interfaceTypes_whenCalled_containsFiveEntries() {
            assertThat(LanguageFrameworkMapping.INTERFACE_TYPES)
                    .hasSize(5)
                    .containsExactly(
                            "rest", "grpc", "graphql",
                            "cli", "events");
        }

        @Test
        @DisplayName("allLanguagesHaveFrameworks")
        void map_whenCalled_allLanguagesHaveFrameworks() {
            for (String lang : LanguageFrameworkMapping.LANGUAGES) {
                assertThat(LanguageFrameworkMapping
                        .frameworksFor(lang))
                        .as("Frameworks for " + lang)
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("allLanguagesHaveBuildTools")
        void map_whenCalled_allLanguagesHaveBuildTools() {
            for (String lang : LanguageFrameworkMapping.LANGUAGES) {
                assertThat(LanguageFrameworkMapping
                        .buildToolsFor(lang))
                        .as("Build tools for " + lang)
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("allLanguagesHaveDefaultVersions")
        void map_whenCalled_allLanguagesHaveDefaultVersions() {
            for (String lang : LanguageFrameworkMapping.LANGUAGES) {
                assertThat(LanguageFrameworkMapping
                        .defaultVersionFor(lang))
                        .as("Default version for " + lang)
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("allFrameworksHaveVersions")
        void map_whenCalled_allFrameworksHaveVersions() {
            List<String> allFrameworks =
                    LanguageFrameworkMapping.FRAMEWORKS.values()
                            .stream()
                            .flatMap(List::stream)
                            .toList();
            for (String fw : allFrameworks) {
                assertThat(LanguageFrameworkMapping
                        .frameworkVersionFor(fw))
                        .as("Version for framework " + fw)
                        .isNotEmpty();
            }
        }
    }
}
