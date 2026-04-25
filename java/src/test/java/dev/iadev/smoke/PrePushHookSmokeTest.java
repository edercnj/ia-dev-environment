package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the EPIC-0057 story-0057-0007 pre-push hook
 * infrastructure. Validates that the hook script + installer +
 * decision document are all present and minimally well-formed.
 */
@DisplayName("PrePushHookSmokeTest — story-0057-0007 pre-push promotion")
@DisabledOnOs(
        value = OS.WINDOWS,
        disabledReason = "POSIX execute bit; mirrors sibling smoke gating.")
class PrePushHookSmokeTest {

    @Test
    @DisplayName(".githooks/pre-push exists and is executable")
    void prePushHook_existsAndIsExecutable() {
        Path hook = repoRoot().resolve(".githooks/pre-push");
        assertThat(hook)
                .as(".githooks/pre-push must exist")
                .exists();
        assertThat(Files.isExecutable(hook))
                .as(".githooks/pre-push must be POSIX-executable")
                .isTrue();
    }

    @Test
    @DisplayName("scripts/setup-hooks.sh exists and references .githooks")
    void setupHooks_existsAndReferencesHooksPath() throws IOException {
        Path setup = repoRoot().resolve("scripts/setup-hooks.sh");
        assertThat(setup).exists();
        assertThat(Files.isExecutable(setup)).isTrue();

        String body = Files.readString(setup, StandardCharsets.UTF_8);
        assertThat(body)
                .as("setup script must wire core.hooksPath to .githooks")
                .contains("core.hooksPath")
                .contains(".githooks");
    }

    @Test
    @DisplayName("pre-push hook references the EPIC-0057 critical smoke list")
    void prePushHook_referencesCriticalSmokeList() throws IOException {
        Path hook = repoRoot().resolve(".githooks/pre-push");
        String body = Files.readString(hook, StandardCharsets.UTF_8);

        assertThat(body)
                .as("pre-push must run Epic0047CompressionSmokeTest")
                .contains("Epic0047CompressionSmokeTest");
        assertThat(body)
                .as("pre-push must include EPIC-0057 audit tests")
                .contains("AuditExecutionIntegrityTest")
                .contains("AuditBypassFlagsTest");
        assertThat(body)
                .as("pre-push must support CLAUDE_SMOKE_DISABLED bypass")
                .contains("CLAUDE_SMOKE_DISABLED");
    }

    @Test
    @DisplayName("smoke-promotion-decision.md documents the chosen option")
    void decisionDoc_documentsChosenOption() throws IOException {
        Path doc = repoRoot()
                .resolve("plans/epic-0057/reports/smoke-promotion-decision.md");
        assertThat(doc).exists();

        String body = Files.readString(doc, StandardCharsets.UTF_8);
        assertThat(body)
                .as("decision doc must record the chosen option (B)")
                .contains("Option B")
                .contains("pre-push hook");
        assertThat(body.toLowerCase())
                .as("decision doc must include a rationale section")
                .contains("rationale");
        assertThat(body.toLowerCase())
                .contains("epic-0053");
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java")
                ? cwd.getParent()
                : cwd;
    }
}
