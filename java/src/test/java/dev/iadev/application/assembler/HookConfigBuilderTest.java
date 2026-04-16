package dev.iadev.application.assembler;

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

            HookConfigBuilder.appendHooksSection(
                    sb, true, false);

            String result = sb.toString();
            assertThat(result)
                    .contains("\"PostToolUse\"");
        }

        @Test
        @DisplayName("contains Write|Edit matcher")
        void load_whenCalled_containsWriteEditMatcher() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(
                    sb, true, false);

            String result = sb.toString();
            assertThat(result)
                    .contains("\"Write|Edit\"");
        }

        @Test
        @DisplayName("contains post-compile-check script")
        void load_whenCalled_containsPostCompileScript() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(
                    sb, true, false);

            String result = sb.toString();
            assertThat(result)
                    .contains("post-compile-check.sh");
        }

        @Test
        @DisplayName("contains timeout of 60 seconds")
        void load_whenCalled_containsTimeout() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(
                    sb, true, false);

            String result = sb.toString();
            assertThat(result)
                    .contains("\"timeout\": 60");
        }

        @Test
        @DisplayName("contains status message")
        void load_whenCalled_containsStatusMessage() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(
                    sb, true, false);

            String result = sb.toString();
            assertThat(result)
                    .contains("Checking compilation...");
        }

        @Test
        @DisplayName("contains command type")
        void load_whenCalled_containsCommandType() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(
                    sb, true, false);

            String result = sb.toString();
            assertThat(result)
                    .contains("\"type\": \"command\"");
        }

        @Test
        @DisplayName("hooks section references CLAUDE"
                + " project dir")
        void load_whenCalled_referencesProjectDir() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(
                    sb, true, false);

            String result = sb.toString();
            assertThat(result)
                    .contains("$CLAUDE_PROJECT_DIR");
        }
    }

    @Nested
    @DisplayName("appendHooksSection — telemetry variants"
            + " (story-0040-0004)")
    class TelemetryVariants {

        @Test
        @DisplayName("no legacy + no telemetry emits empty"
                + " hooks block")
        void noFlags_emitsEmptyHooksBlock() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(
                    sb, false, false);

            String result = sb.toString();
            assertThat(result).contains("\"hooks\": {");
            assertThat(result)
                    .doesNotContain("PostToolUse");
            assertThat(result)
                    .doesNotContain("telemetry-");
        }

        @Test
        @DisplayName("telemetry only emits 5 events")
        void telemetryOnly_emitsFiveEvents() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(
                    sb, false, true);

            String result = sb.toString();
            assertThat(result).contains("SessionStart");
            assertThat(result).contains("PreToolUse");
            assertThat(result).contains("PostToolUse");
            assertThat(result).contains("SubagentStop");
            assertThat(result).contains("Stop");
            assertThat(result)
                    .contains("telemetry-posttool.sh");
        }

        @Test
        @DisplayName("both flags coexist in PostToolUse"
                + " array")
        void bothFlags_postToolUseHasTwoEntries() {
            StringBuilder sb = new StringBuilder();

            HookConfigBuilder.appendHooksSection(
                    sb, true, true);

            String result = sb.toString();
            assertThat(result)
                    .contains("post-compile-check.sh");
            assertThat(result)
                    .contains("telemetry-posttool.sh");
            assertThat(result).contains("\"Write|Edit\"");
        }
    }
}
