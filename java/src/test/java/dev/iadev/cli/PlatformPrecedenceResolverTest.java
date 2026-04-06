package dev.iadev.cli;

import dev.iadev.domain.model.Platform;
import dev.iadev.testutil.TestConfigBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PlatformPrecedenceResolver — CLI > YAML >
 * default precedence resolution.
 *
 * <p>CLI platforms arrive as typed {@link Platform} values
 * via {@link PlatformConverter}. The "all" keyword is
 * represented as a null element in the list.</p>
 */
@DisplayName("PlatformPrecedenceResolver")
class PlatformPrecedenceResolverTest {

    @Nested
    @DisplayName("resolve() precedence")
    class ResolvePrecedence {

        @Test
        @DisplayName("null CLI + empty YAML = all")
        void resolve_nullCliEmptyYaml_returnsEmpty() {
            var config = TestConfigBuilder.builder()
                    .build();

            var result =
                    PlatformPrecedenceResolver.resolve(
                            null, config);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("empty CLI list + empty YAML = all")
        void resolve_emptyCliEmptyYaml_returnsEmpty() {
            var config = TestConfigBuilder.builder()
                    .build();

            var result =
                    PlatformPrecedenceResolver.resolve(
                            List.of(), config);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("absent CLI + YAML = YAML wins")
        void resolve_noCliWithYaml_yamlWins() {
            var config = TestConfigBuilder.builder()
                    .platforms(Set.of(
                            Platform.CLAUDE_CODE))
                    .build();

            var result =
                    PlatformPrecedenceResolver.resolve(
                            null, config);

            assertThat(result)
                    .containsExactly(Platform.CLAUDE_CODE);
        }

        @Test
        @DisplayName("CLI overrides YAML")
        void resolve_cliOverridesYaml_cliWins() {
            var config = TestConfigBuilder.builder()
                    .platforms(Set.of(
                            Platform.CLAUDE_CODE))
                    .build();

            var result =
                    PlatformPrecedenceResolver.resolve(
                            List.of(Platform.COPILOT),
                            config);

            assertThat(result)
                    .containsExactly(Platform.COPILOT);
        }

        @Test
        @DisplayName("CLI 'all' (null marker) overrides "
                + "YAML specific")
        void resolve_cliAllOverridesYaml_returnsEmpty() {
            var config = TestConfigBuilder.builder()
                    .platforms(Set.of(
                            Platform.CLAUDE_CODE))
                    .build();
            List<Platform> allMarker = new ArrayList<>();
            allMarker.add(null);

            var result =
                    PlatformPrecedenceResolver.resolve(
                            allMarker, config);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("CLI multiple overrides YAML")
        void resolve_cliMultiple_overridesYaml() {
            var config = TestConfigBuilder.builder()
                    .platforms(Set.of(Platform.CODEX))
                    .build();

            var result =
                    PlatformPrecedenceResolver.resolve(
                            List.of(Platform.CLAUDE_CODE,
                                    Platform.COPILOT),
                            config);

            assertThat(result)
                    .containsExactlyInAnyOrder(
                            Platform.CLAUDE_CODE,
                            Platform.COPILOT);
        }
    }

    @Nested
    @DisplayName("buildPlatformSet()")
    class BuildPlatformSet {

        @Test
        @DisplayName("null list returns empty set")
        void buildPlatformSet_null_returnsEmpty() {
            var result =
                    PlatformPrecedenceResolver
                            .buildPlatformSet(null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("empty list returns empty set")
        void buildPlatformSet_empty_returnsEmpty() {
            var result =
                    PlatformPrecedenceResolver
                            .buildPlatformSet(List.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("single platform returns singleton")
        void buildPlatformSet_single_returnsSingleton() {
            var result =
                    PlatformPrecedenceResolver
                            .buildPlatformSet(
                                    List.of(
                                            Platform.COPILOT));

            assertThat(result)
                    .containsExactly(Platform.COPILOT);
        }

        @Test
        @DisplayName("null marker (all) returns empty set")
        void buildPlatformSet_allMarker_returnsEmpty() {
            List<Platform> list = new ArrayList<>();
            list.add(null);

            var result =
                    PlatformPrecedenceResolver
                            .buildPlatformSet(list);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("multiple platforms returns EnumSet")
        void buildPlatformSet_multiple_returnsEnumSet() {
            var result =
                    PlatformPrecedenceResolver
                            .buildPlatformSet(
                                    List.of(
                                            Platform.CLAUDE_CODE,
                                            Platform.CODEX));

            assertThat(result)
                    .containsExactlyInAnyOrder(
                            Platform.CLAUDE_CODE,
                            Platform.CODEX);
        }
    }
}
