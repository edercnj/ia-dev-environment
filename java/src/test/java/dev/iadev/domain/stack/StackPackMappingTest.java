package dev.iadev.domain.stack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StackPackMapping")
class StackPackMappingTest {

    @Nested
    @DisplayName("FRAMEWORK_STACK_PACK")
    class FrameworkStackPackTests {

        @Test
        @DisplayName("contains exactly 11 entries")
        void frameworkStackPack_size_eleven() {
            assertThat(StackPackMapping.FRAMEWORK_STACK_PACK).hasSize(11);
        }

        @ParameterizedTest
        @CsvSource({
                "quarkus, quarkus-patterns",
                "spring-boot, spring-patterns",
                "nestjs, nestjs-patterns",
                "express, express-patterns",
                "fastapi, fastapi-patterns",
                "django, django-patterns",
                "gin, gin-patterns",
                "ktor, ktor-patterns",
                "axum, axum-patterns",
                "dotnet, dotnet-patterns",
                "click, click-cli-patterns"
        })
        @DisplayName("framework {0} maps to {1}")
        void frameworkStackPack_correctMapping(
                String framework, String expectedPack) {
            assertThat(StackPackMapping.FRAMEWORK_STACK_PACK.get(framework))
                    .isEqualTo(expectedPack);
        }
    }

    @Nested
    @DisplayName("getStackPackName()")
    class GetStackPackNameTests {

        @Test
        @DisplayName("returns pack name for known framework")
        void getStackPackName_quarkus_quarkusPatterns() {
            assertThat(StackPackMapping.getStackPackName("quarkus"))
                    .isEqualTo("quarkus-patterns");
        }

        @Test
        @DisplayName("returns empty for unknown framework")
        void getStackPackName_unknown_empty() {
            assertThat(StackPackMapping.getStackPackName("unknown"))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns pack for spring-boot")
        void getStackPackName_springBoot_springPatterns() {
            assertThat(StackPackMapping.getStackPackName("spring-boot"))
                    .isEqualTo("spring-patterns");
        }
    }
}
