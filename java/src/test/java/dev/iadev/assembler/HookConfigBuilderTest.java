package dev.iadev.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for HookConfigBuilder — builds the hooks
 * configuration section for settings.json.
 */
@DisplayName("HookConfigBuilder")
class HookConfigBuilderTest {

    @Nested
    @DisplayName("appendHooksSection — hook JSON")
    class AppendHooksSection {

        @Test
        @DisplayName("contains PostToolUse event")
        void containsPostToolUse() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(sb);

            String result = sb.toString();
            assertThat(result)
                    .contains("\"PostToolUse\"");
        }

        @Test
        @DisplayName("contains Write|Edit matcher")
        void containsWriteEditMatcher() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(sb);

            String result = sb.toString();
            assertThat(result)
                    .contains("\"Write|Edit\"");
        }

        @Test
        @DisplayName("contains post-compile-check script")
        void containsPostCompileScript() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(sb);

            String result = sb.toString();
            assertThat(result)
                    .contains("post-compile-check.sh");
        }

        @Test
        @DisplayName("contains timeout of 60 seconds")
        void containsTimeout() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(sb);

            String result = sb.toString();
            assertThat(result)
                    .contains("\"timeout\": 60");
        }

        @Test
        @DisplayName("contains status message")
        void containsStatusMessage() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(sb);

            String result = sb.toString();
            assertThat(result)
                    .contains("Checking compilation...");
        }

        @Test
        @DisplayName("contains command type")
        void containsCommandType() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(sb);

            String result = sb.toString();
            assertThat(result)
                    .contains("\"type\": \"command\"");
        }

        @Test
        @DisplayName("hooks section references CLAUDE"
                + " project dir")
        void referencesProjectDir() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(sb);

            String result = sb.toString();
            assertThat(result)
                    .contains("$CLAUDE_PROJECT_DIR");
        }
    }
}
