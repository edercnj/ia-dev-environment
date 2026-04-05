package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GithubInstructionsAssembler —
 * formatInterfaces, formatFrameworkVersion,
 * and replaceSingleBracePlaceholders.
 */
@DisplayName("GithubInstructionsAssembler — format")
class GithubInstructionsFormatTest {

    @Nested
    @DisplayName("formatInterfaces — interface formatting")
    class FormatInterfaces {

        @Test
        @DisplayName("REST uppercased")
        void formatInterfaces_whenCalled_restUppercased() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .formatInterfaces(config);

            assertThat(result).isEqualTo("REST");
        }

        @Test
        @DisplayName("GRPC uppercased")
        void formatInterfaces_whenCalled_grpcUppercased() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("grpc")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .formatInterfaces(config);

            assertThat(result).isEqualTo("GRPC");
        }

        @Test
        @DisplayName("mixed interfaces formatted"
                + " correctly")
        void formatInterfaces_whenCalled_mixedInterfaces() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface("rest")
                            .addInterface("grpc")
                            .addInterface("event-consumer")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .formatInterfaces(config);

            assertThat(result)
                    .isEqualTo(
                            "REST, GRPC, event-consumer");
        }

        @Test
        @DisplayName("empty interfaces returns none")
        void formatInterfaces_emptyInterfaces_returnsNone() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .formatInterfaces(config);

            assertThat(result).isEqualTo("none");
        }
    }

    @Nested
    @DisplayName("formatFrameworkVersion")
    class FormatFrameworkVersion {

        @Test
        @DisplayName("returns space-prefixed version"
                + " when present")
        void assemble_withSpace_returnsVersion() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("spring", "3.4")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .formatFrameworkVersion(
                                    config);

            assertThat(result).isEqualTo(" 3.4");
        }

        @Test
        @DisplayName("returns empty string when"
                + " version empty")
        void assemble_whenNoVersion_returnsEmpty() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("axum", "")
                            .build();

            String result =
                    GithubInstructionsAssembler
                            .formatFrameworkVersion(
                                    config);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("replaceSingleBracePlaceholders")
    class ReplacePlaceholders {

        @Test
        @DisplayName("replaces known placeholders")
        void assemble_whenCalled_replacesKnownPlaceholders() {
            String content = "Hello {name}, v{version}";
            Map<String, String> context = Map.of(
                    "name", "world",
                    "version", "1.0");

            String result =
                    GithubInstructionsAssembler
                            .replaceSingleBracePlaceholders(
                                    content, context);

            assertThat(result)
                    .isEqualTo("Hello world, v1.0");
        }

        @Test
        @DisplayName("preserves unknown placeholders")
        void assemble_whenCalled_preservesUnknownPlaceholders() {
            String content = "Value: {unknown}";
            Map<String, String> context = Map.of();

            String result =
                    GithubInstructionsAssembler
                            .replaceSingleBracePlaceholders(
                                    content, context);

            assertThat(result)
                    .isEqualTo("Value: {unknown}");
        }

        @Test
        @DisplayName("does not match double braces")
        void assemble_whenCalled_doesNotMatchDoubleBraces() {
            String content = "Keep {{this}} intact";
            Map<String, String> context = Map.of(
                    "this", "replaced");

            String result =
                    GithubInstructionsAssembler
                            .replaceSingleBracePlaceholders(
                                    content, context);

            assertThat(result)
                    .isEqualTo("Keep {{this}} intact");
        }
    }
}
