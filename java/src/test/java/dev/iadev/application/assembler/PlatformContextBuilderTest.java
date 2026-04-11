package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PlatformContextBuilder — computes
 * platform-aware template variables.
 */
@DisplayName("PlatformContextBuilder")
class PlatformContextBuilderTest {

    @Nested
    @DisplayName("buildPlatformFlags")
    class BuildPlatformFlags {

        @Test
        @DisplayName("all platforms set when empty (no filter)")
        void buildFlags_emptyPlatforms_allTrue() {
            Map<String, Object> flags =
                    PlatformContextBuilder
                            .buildPlatformFlags(Set.of());

            assertThat(flags.get("hasClaude"))
                    .isEqualTo(true);
            assertThat(flags.get("isMultiPlatform"))
                    .isEqualTo(false);
        }

        @Test
        @DisplayName("only claude when claude-code selected")
        void buildFlags_claudeOnly_onlyClaude() {
            Map<String, Object> flags =
                    PlatformContextBuilder
                            .buildPlatformFlags(
                                    Set.of(Platform
                                            .CLAUDE_CODE));

            assertThat(flags.get("hasClaude"))
                    .isEqualTo(true);
            assertThat(flags.get("isMultiPlatform"))
                    .isEqualTo(false);
        }

        @Test
        @DisplayName("all user-selectable treated as all")
        void buildFlags_allSelectable_allTrue() {
            Map<String, Object> flags =
                    PlatformContextBuilder
                            .buildPlatformFlags(
                                    Platform
                                            .allUserSelectable());

            assertThat(flags.get("hasClaude"))
                    .isEqualTo(true);
            assertThat(flags.get("isMultiPlatform"))
                    .isEqualTo(false);
        }

        @Test
        @DisplayName("platforms list contains cli names")
        void buildFlags_selected_platformsList() {
            Map<String, Object> flags =
                    PlatformContextBuilder
                            .buildPlatformFlags(
                                    Set.of(Platform
                                            .CLAUDE_CODE));

            @SuppressWarnings("unchecked")
            List<String> platforms =
                    (List<String>) flags.get("platforms");
            assertThat(platforms)
                    .containsExactly("claude-code");
        }

        @Test
        @DisplayName("empty platforms produces all cli names")
        void buildFlags_emptyPlatforms_allCliNames() {
            Map<String, Object> flags =
                    PlatformContextBuilder
                            .buildPlatformFlags(Set.of());

            @SuppressWarnings("unchecked")
            List<String> platforms =
                    (List<String>) flags.get("platforms");
            assertThat(platforms)
                    .containsExactly("claude-code");
        }
    }
}
