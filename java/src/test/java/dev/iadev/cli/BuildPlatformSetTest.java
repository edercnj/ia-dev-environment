package dev.iadev.cli;

import dev.iadev.domain.model.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PlatformPrecedenceResolver.buildPlatformSet — converts
 * the CLI List&lt;Platform&gt; to Set&lt;Platform&gt;.
 */
@DisplayName("PlatformPrecedenceResolver.buildPlatformSet")
class BuildPlatformSetTest {

    @Nested
    @DisplayName("Null input (no --platform flag)")
    class NullInput {

        @Test
        @DisplayName("null list returns empty set (no "
                + "filter)")
        void buildPlatformSet_null_returnsEmpty() {
            Set<Platform> result =
                    PlatformPrecedenceResolver.buildPlatformSet(null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Empty list")
    class EmptyList {

        @Test
        @DisplayName("empty list returns empty set")
        void buildPlatformSet_empty_returnsEmpty() {
            Set<Platform> result =
                    PlatformPrecedenceResolver.buildPlatformSet(
                            List.of());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("All keyword (null in list)")
    class AllKeyword {

        @Test
        @DisplayName("list with null returns empty set "
                + "(all = no filter)")
        void buildPlatformSet_containsNull_returnsEmpty() {
            List<Platform> list = new ArrayList<>();
            list.add(null);

            Set<Platform> result =
                    PlatformPrecedenceResolver.buildPlatformSet(list);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("list with platform and null returns "
                + "empty set (all prevails)")
        void buildPlatformSet_mixedWithNull_returnsEmpty() {
            List<Platform> list = new ArrayList<>();
            list.add(Platform.CLAUDE_CODE);
            list.add(null);

            Set<Platform> result =
                    PlatformPrecedenceResolver.buildPlatformSet(list);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Specific platforms")
    class SpecificPlatforms {

        @Test
        @DisplayName("single platform returns singleton "
                + "set")
        void buildPlatformSet_single_returnsSingleton() {
            Set<Platform> result =
                    PlatformPrecedenceResolver.buildPlatformSet(
                            List.of(Platform.CLAUDE_CODE));

            assertThat(result)
                    .containsExactly(Platform.CLAUDE_CODE);
        }

        @Test
        @DisplayName("single platform returns EnumSet")
        void buildPlatformSet_single_returnsEnumSet() {
            Set<Platform> result =
                    PlatformPrecedenceResolver.buildPlatformSet(
                            List.of(Platform.CLAUDE_CODE));

            assertThat(result).containsExactlyInAnyOrder(
                    Platform.CLAUDE_CODE);
        }

        @Test
        @DisplayName("duplicate platforms are deduplicated")
        void buildPlatformSet_duplicates_deduplicates() {
            Set<Platform> result =
                    PlatformPrecedenceResolver.buildPlatformSet(
                            Arrays.asList(
                                    Platform.CLAUDE_CODE,
                                    Platform.CLAUDE_CODE));

            assertThat(result)
                    .hasSize(1)
                    .containsExactly(Platform.CLAUDE_CODE);
        }
    }
}
