package dev.iadev.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LanguageFrameworkMapping")
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

        @Test
        @DisplayName("frameworksFor_python_returnsFastapiAndClickCli")
        void frameworksFor_python_returnsFastapiAndClickCli() {
            assertThat(
                    LanguageFrameworkMapping.frameworksFor("python"))
                    .containsExactly("fastapi", "click-cli");
        }

        @Test
        @DisplayName("frameworksFor_go_returnsGin")
        void frameworksFor_go_returnsGin() {
            assertThat(LanguageFrameworkMapping.frameworksFor("go"))
                    .containsExactly("gin");
        }

        @Test
        @DisplayName("frameworksFor_kotlin_returnsKtor")
        void frameworksFor_kotlin_returnsKtor() {
            assertThat(
                    LanguageFrameworkMapping.frameworksFor("kotlin"))
                    .containsExactly("ktor");
        }

        @Test
        @DisplayName("frameworksFor_typescript_returnsNestjs")
        void frameworksFor_typescript_returnsNestjs() {
            assertThat(LanguageFrameworkMapping
                    .frameworksFor("typescript"))
                    .containsExactly("nestjs");
        }

        @Test
        @DisplayName("frameworksFor_rust_returnsAxum")
        void frameworksFor_rust_returnsAxum() {
            assertThat(
                    LanguageFrameworkMapping.frameworksFor("rust"))
                    .containsExactly("axum");
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

        @Test
        @DisplayName("buildToolsFor_python_returnsPip")
        void buildToolsFor_python_returnsPip() {
            assertThat(
                    LanguageFrameworkMapping.buildToolsFor("python"))
                    .containsExactly("pip");
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

        @ParameterizedTest
        @CsvSource({
                "java,21",
                "python,3.12",
                "go,1.22",
                "kotlin,2.0",
                "typescript,5",
                "rust,1.78"
        })
        @DisplayName("defaultVersionFor_knownLanguage_returnsVersion")
        void defaultVersionFor_knownLanguage_returnsVersion(
                String lang, String version) {
            assertThat(
                    LanguageFrameworkMapping.defaultVersionFor(lang))
                    .isEqualTo(version);
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

        @ParameterizedTest
        @CsvSource({
                "spring-boot,3.4",
                "quarkus,3.17",
                "fastapi,0.115",
                "click-cli,8.1",
                "gin,1.10",
                "ktor,3.0",
                "nestjs,10",
                "axum,0.7"
        })
        @DisplayName("frameworkVersionFor_known_returnsVersion")
        void frameworkVersionFor_known_returnsVersion(
                String fw, String version) {
            assertThat(LanguageFrameworkMapping
                    .frameworkVersionFor(fw))
                    .isEqualTo(version);
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
    @DisplayName("Static constants")
    class StaticConstants {

        @Test
        @DisplayName("languages_containsSixEntries")
        void languages_containsSixEntries() {
            assertThat(LanguageFrameworkMapping.LANGUAGES)
                    .hasSize(6);
        }

        @Test
        @DisplayName("architectureStyles_containsThreeEntries")
        void architectureStyles_containsThreeEntries() {
            assertThat(LanguageFrameworkMapping.ARCHITECTURE_STYLES)
                    .hasSize(3)
                    .containsExactly(
                            "microservice", "monolith", "library");
        }

        @Test
        @DisplayName("interfaceTypes_containsFiveEntries")
        void interfaceTypes_containsFiveEntries() {
            assertThat(LanguageFrameworkMapping.INTERFACE_TYPES)
                    .hasSize(5)
                    .containsExactly(
                            "rest", "grpc", "graphql",
                            "cli", "events");
        }

        @Test
        @DisplayName("allLanguagesHaveFrameworks")
        void allLanguagesHaveFrameworks() {
            for (String lang : LanguageFrameworkMapping.LANGUAGES) {
                assertThat(LanguageFrameworkMapping
                        .frameworksFor(lang))
                        .as("Frameworks for " + lang)
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("allLanguagesHaveBuildTools")
        void allLanguagesHaveBuildTools() {
            for (String lang : LanguageFrameworkMapping.LANGUAGES) {
                assertThat(LanguageFrameworkMapping
                        .buildToolsFor(lang))
                        .as("Build tools for " + lang)
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("allLanguagesHaveDefaultVersions")
        void allLanguagesHaveDefaultVersions() {
            for (String lang : LanguageFrameworkMapping.LANGUAGES) {
                assertThat(LanguageFrameworkMapping
                        .defaultVersionFor(lang))
                        .as("Default version for " + lang)
                        .isNotEmpty();
            }
        }

        @Test
        @DisplayName("allFrameworksHaveVersions")
        void allFrameworksHaveVersions() {
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
