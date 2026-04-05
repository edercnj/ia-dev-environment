package dev.iadev.domain.stack;

import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for StackValidator — Java version checks,
 * Django version checks, native build, and interface
 * types.
 */
@DisplayName("StackValidator — version validation")
class StackValidatorVersionBaseTest {

    @Nested
    @DisplayName("checkJavaFrameworkVersion()")
    class JavaVersionTests {

        @Test
        @DisplayName("Quarkus 3 with Java 11 fails")
        void java17_quarkus3Java11_error() {
            var config = TestConfigBuilder.builder()
                    .language("java", "11")
                    .framework("quarkus", "3.0")
                    .build();
            var errors = StackValidator
                    .checkJavaFrameworkVersion(config);
            assertThat(errors).isNotEmpty();
            assertThat(errors.get(0))
                    .contains("Java 17+");
        }

        @Test
        @DisplayName("Quarkus 3 with Java 21 passes")
        void java17_quarkus3Java21_noError() {
            var config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("Spring Boot 3 with Java 11 fails")
        void java17_springBoot3Java11_error() {
            var config = TestConfigBuilder.builder()
                    .language("java", "11")
                    .framework("spring-boot", "3.4")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isNotEmpty();
        }

        @Test
        @DisplayName("Quarkus 2 with Java 11 passes")
        void java17_quarkus2Java11_noError() {
            var config = TestConfigBuilder.builder()
                    .language("java", "11")
                    .framework("quarkus", "2.16")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("non-java skips check")
        void java17_python_skipped() {
            var config = TestConfigBuilder.builder()
                    .language("python", "3.12")
                    .framework("fastapi", "0.115")
                    .buildTool("pip")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("unparseable version skips")
        void java17_unparseableVersion_skipped() {
            var config = TestConfigBuilder.builder()
                    .language("java", "latest")
                    .framework("quarkus", "3.0")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("non-quarkus non-spring skips")
        void java17_javaWithKtor_skipped() {
            var config = TestConfigBuilder.builder()
                    .language("java", "11")
                    .framework("ktor", "3.0")
                    .buildTool("gradle")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("unparseable fw version skips")
        void java17_unparseableFwVersion_noError() {
            var config = TestConfigBuilder.builder()
                    .language("java", "11")
                    .framework("quarkus", "latest")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isEmpty();
        }

        @Test
        @DisplayName("Spring Boot 3 with Java 17 passes")
        void java17_springBoot3Java17_noError() {
            var config = TestConfigBuilder.builder()
                    .language("java", "17")
                    .framework("spring-boot", "3.4")
                    .build();
            assertThat(StackValidator
                    .checkJavaFrameworkVersion(config))
                    .isEmpty();
        }
    }

}
