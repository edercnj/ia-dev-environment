package dev.iadev.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CoreStack")
class CoreStackTest {

    private static ProjectIdentity identity() {
        return new ProjectIdentity("my-app", "test");
    }

    private static ArchitectureConfig architecture() {
        return new ArchitectureConfig(
                "hexagonal", true, false, false, "com.x",
                new ArchitectureConfig.CqrsConfig(
                        "eventstoredb", 100, "", false, ""),
                true);
    }

    private static LanguageConfig language() {
        return new LanguageConfig("java", "21");
    }

    private static FrameworkConfig framework() {
        return new FrameworkConfig(
                "spring-boot", "3.4", "maven", false);
    }

    @Nested
    @DisplayName("compact constructor")
    class CompactConstructor {

        @Test
        @DisplayName("builds with all five required sections")
        void ctor_allSections_allSet() {
            List<InterfaceConfig> interfaces = List.of(
                    new InterfaceConfig("rest", "", ""));

            CoreStack stack = new CoreStack(
                    identity(), architecture(), interfaces,
                    language(), framework());

            assertThat(stack.project().name()).isEqualTo("my-app");
            assertThat(stack.architecture().style())
                    .isEqualTo("hexagonal");
            assertThat(stack.interfaces()).hasSize(1);
            assertThat(stack.language().name()).isEqualTo("java");
            assertThat(stack.framework().name())
                    .isEqualTo("spring-boot");
        }

        @Test
        @DisplayName("defensively copies the interfaces list")
        void ctor_mutatingOriginalList_doesNotAffectStack() {
            List<InterfaceConfig> mutable = new ArrayList<>();
            mutable.add(new InterfaceConfig("rest", "", ""));

            CoreStack stack = new CoreStack(
                    identity(), architecture(), mutable,
                    language(), framework());

            mutable.add(new InterfaceConfig("grpc", "", ""));

            assertThat(stack.interfaces()).hasSize(1);
        }

        @Test
        @DisplayName("exposed interfaces list is immutable")
        void ctor_returnedInterfaces_isUnmodifiable() {
            CoreStack stack = new CoreStack(
                    identity(), architecture(),
                    List.of(new InterfaceConfig("rest", "", "")),
                    language(), framework());

            assertThatThrownBy(() ->
                    stack.interfaces().add(
                            new InterfaceConfig(
                                    "grpc", "", "")))
                    .isInstanceOf(
                            UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("fromMap()")
    class FromMap {

        @Test
        @DisplayName("builds CoreStack from YAML-shaped root map")
        void fromMap_allRequiredSections_builds() {
            Map<String, Object> root = Map.of(
                    "project", Map.of(
                            "name", "svc",
                            "purpose", "a test service"),
                    "architecture", Map.of(
                            "style", "hexagonal",
                            "domain-driven", true,
                            "event-driven", false),
                    "language", Map.of(
                            "name", "java",
                            "version", "21"),
                    "framework", Map.of(
                            "name", "quarkus",
                            "version", "3.17",
                            "build-tool", "maven"));
            List<InterfaceConfig> interfaces = List.of(
                    new InterfaceConfig("rest", "", ""));

            CoreStack stack = CoreStack.fromMap(
                    root, interfaces);

            assertThat(stack.project().name()).isEqualTo("svc");
            assertThat(stack.framework().name())
                    .isEqualTo("quarkus");
            assertThat(stack.interfaces())
                    .containsExactlyElementsOf(interfaces);
        }

        @Test
        @DisplayName("throws when project section is missing")
        void fromMap_missingProject_throws() {
            Map<String, Object> root = Map.of(
                    "architecture", Map.of(
                            "style", "hexagonal",
                            "domain-driven", true,
                            "event-driven", false),
                    "language", Map.of(
                            "name", "java", "version", "21"),
                    "framework", Map.of(
                            "name", "quarkus",
                            "version", "3.17",
                            "build-tool", "maven"));

            assertThatThrownBy(() ->
                    CoreStack.fromMap(root, List.of()))
                    .isInstanceOf(
                            ConfigValidationException.class)
                    .hasMessageContaining("project");
        }
    }
}
