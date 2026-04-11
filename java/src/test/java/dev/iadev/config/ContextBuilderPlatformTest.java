package dev.iadev.config;

import dev.iadev.domain.model.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ContextBuilder.buildPlatformContext —
 * platform-aware template variables.
 */
@DisplayName("ContextBuilder — platform context")
class ContextBuilderPlatformTest {

    @Nested
    @DisplayName("buildPlatformContext")
    class BuildPlatformContext {

        @Test
        @DisplayName("contains platform keys")
        void build_anyPlatforms_containsKeys() {
            Map<String, Object> ctx =
                    ContextBuilder.buildPlatformContext(
                            Set.of(Platform.CLAUDE_CODE));

            assertThat(ctx).containsKeys(
                    "hasClaude",
                    "isMultiPlatform", "platforms");
        }

        @Test
        @DisplayName("claude-only sets correct flags")
        void build_claudeOnly_correctFlags() {
            Map<String, Object> ctx =
                    ContextBuilder.buildPlatformContext(
                            Set.of(Platform.CLAUDE_CODE));

            assertThat(ctx.get("hasClaude"))
                    .isEqualTo(true);
            assertThat(ctx.get("isMultiPlatform"))
                    .isEqualTo(false);
        }

        @Test
        @DisplayName("empty platforms means all active")
        void build_emptyPlatforms_allActive() {
            Map<String, Object> ctx =
                    ContextBuilder.buildPlatformContext(
                            Set.of());

            assertThat(ctx.get("hasClaude"))
                    .isEqualTo(true);
            assertThat(ctx.get("isMultiPlatform"))
                    .isEqualTo(false);
        }

        @Test
        @DisplayName("platforms list has cli names")
        void build_singlePlatform_cliNames() {
            Map<String, Object> ctx =
                    ContextBuilder.buildPlatformContext(
                            Set.of(Platform.CLAUDE_CODE));

            @SuppressWarnings("unchecked")
            List<String> platforms =
                    (List<String>) ctx.get("platforms");
            assertThat(platforms)
                    .containsExactly("claude-code");
        }
    }
}
