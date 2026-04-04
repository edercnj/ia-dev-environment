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
        void load_whenCalled_containsPostToolUse() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(sb);

            String result = sb.toString();
            assertThat(result)
                    .contains("\"PostToolUse\"");
        }

        @Test
        @DisplayName("contains Write|Edit matcher")
        void load_whenCalled_containsWriteEditMatcher() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(sb);

            String result = sb.toString();
            assertThat(result)
                    .contains("\"Write|Edit\"");
        }

        @Test
        @DisplayName("contains post-compile-check script")
        void load_whenCalled_containsPostCompileScript() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(sb);

            String result = sb.toString();
            assertThat(result)
                    .contains("post-compile-check.sh");
        }

        @Test
        @DisplayName("contains timeout of 60 seconds")
        void load_whenCalled_containsTimeout() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(sb);

            String result = sb.toString();
            assertThat(result)
                    .contains("\"timeout\": 60");
        }

        @Test
        @DisplayName("contains status message")
        void load_whenCalled_containsStatusMessage() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(sb);

            String result = sb.toString();
            assertThat(result)
                    .contains("Checking compilation...");
        }

        @Test
        @DisplayName("contains command type")
        void load_whenCalled_containsCommandType() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(sb);

            String result = sb.toString();
            assertThat(result)
                    .contains("\"type\": \"command\"");
        }

        @Test
        @DisplayName("hooks section references CLAUDE"
                + " project dir")
        void load_whenCalled_referencesProjectDir() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(sb);

            String result = sb.toString();
            assertThat(result)
                    .contains("$CLAUDE_PROJECT_DIR");
        }
    }
}
