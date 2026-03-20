package dev.iadev.domain.stack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CoreKpRouting")
class CoreKpRoutingTest {

    @Nested
    @DisplayName("CORE_TO_KP_MAPPING")
    class CoreToKpMappingTests {

        @Test
        @DisplayName("contains exactly 12 static routes")
        void coreMapping_size_twelve() {
            assertThat(CoreKpRouting.CORE_TO_KP_MAPPING).hasSize(12);
        }

        @Test
        @DisplayName("first route maps clean-code to coding-standards")
        void coreMapping_first_cleanCode() {
            var first = CoreKpRouting.CORE_TO_KP_MAPPING.get(0);
            assertThat(first.sourceFile())
                    .isEqualTo("01-clean-code.md");
            assertThat(first.kpName())
                    .isEqualTo("coding-standards");
            assertThat(first.destFile())
                    .isEqualTo("clean-code.md");
        }

        @Test
        @DisplayName("last route maps refactoring to coding-standards")
        void coreMapping_last_refactoring() {
            var last = CoreKpRouting.CORE_TO_KP_MAPPING
                    .get(CoreKpRouting.CORE_TO_KP_MAPPING.size() - 1);
            assertThat(last.sourceFile())
                    .isEqualTo("14-refactoring-guidelines.md");
            assertThat(last.kpName())
                    .isEqualTo("coding-standards");
            assertThat(last.destFile())
                    .isEqualTo("refactoring-guidelines.md");
        }

        @Test
        @DisplayName("list is immutable")
        void coreMapping_whenCalled_immutable() {
            assertThat(CoreKpRouting.CORE_TO_KP_MAPPING)
                    .isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("CONDITIONAL_CORE_KP")
    class ConditionalCoreKpTests {

        @Test
        @DisplayName("contains exactly 1 conditional route")
        void conditionalKp_size_one() {
            assertThat(CoreKpRouting.CONDITIONAL_CORE_KP).hasSize(1);
        }

        @Test
        @DisplayName("conditional route excludes library style")
        void conditionalKp_whenCalled_excludesLibrary() {
            var route = CoreKpRouting.CONDITIONAL_CORE_KP.get(0);
            assertThat(route.sourceFile())
                    .isEqualTo("12-cloud-native-principles.md");
            assertThat(route.conditionField())
                    .isEqualTo("architecture_style");
            assertThat(route.conditionExclude())
                    .isEqualTo("library");
        }
    }

    @Nested
    @DisplayName("getActiveRoutes()")
    class GetActiveRoutesTests {

        @Test
        @DisplayName("microservice includes cloud-native route (13 total)")
        void getActiveRoutes_microservice_includesCloudNative() {
            var config = new TestProjectConfigBuilder()
                    .architectureStyle("microservice")
                    .build();

            var routes = CoreKpRouting.getActiveRoutes(config);

            assertThat(routes).hasSize(13);
            assertThat(routes.stream()
                    .anyMatch(r -> "12-cloud-native-principles.md"
                            .equals(r.sourceFile())))
                    .isTrue();
        }

        @Test
        @DisplayName("library excludes cloud-native route (12 total)")
        void getActiveRoutes_library_excludesCloudNative() {
            var config = new TestProjectConfigBuilder()
                    .architectureStyle("library")
                    .build();

            var routes = CoreKpRouting.getActiveRoutes(config);

            assertThat(routes).hasSize(12);
            assertThat(routes.stream()
                    .noneMatch(r -> "12-cloud-native-principles.md"
                            .equals(r.sourceFile())))
                    .isTrue();
        }

        @Test
        @DisplayName("monolith includes cloud-native route")
        void getActiveRoutes_monolith_includesCloudNative() {
            var config = new TestProjectConfigBuilder()
                    .architectureStyle("monolith")
                    .build();

            var routes = CoreKpRouting.getActiveRoutes(config);

            assertThat(routes).hasSize(13);
        }
    }

    @Nested
    @DisplayName("resolveConditionValue()")
    class ResolveConditionValueTests {

        @Test
        @DisplayName("architecture_style returns config style")
        void resolveCondition_architectureStyle_returnsStyle() {
            var config = new TestProjectConfigBuilder()
                    .architectureStyle("microservice")
                    .build();

            var value = CoreKpRouting.resolveConditionValue(
                    config, "architecture_style");

            assertThat(value).isEqualTo("microservice");
        }

        @Test
        @DisplayName("unknown field returns empty string")
        void resolveCondition_unknownField_empty() {
            var config = new TestProjectConfigBuilder().build();

            var value = CoreKpRouting.resolveConditionValue(
                    config, "unknown_field");

            assertThat(value).isEmpty();
        }
    }
}
