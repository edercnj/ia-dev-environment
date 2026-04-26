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
 * Smoke test for Camada 3 enforcement infrastructure
 * (story-0057-0002). Verifies the audit script + companion
 * config are present, executable, and wired into CI.
 */
@DisplayName("AuditCamada3SmokeTest — Camada 3 infra wired")
@DisabledOnOs(
        value = OS.WINDOWS,
        disabledReason = "POSIX execute bit; mirrors sibling smoke gating.")
class AuditCamada3SmokeTest {

    @Test
    @DisplayName("audit-execution-integrity.sh exists and is executable")
    void script_existsAndIsExecutable() {
        Path script = repoRoot()
                .resolve("scripts/audit-execution-integrity.sh");
        assertThat(script)
                .as("audit script must exist")
                .exists();
        assertThat(Files.isExecutable(script))
                .as("audit script must be POSIX-executable")
                .isTrue();
    }

    @Test
    @DisplayName("audit-execution-integrity.conf companion exists")
    void confFile_exists() {
        Path conf = repoRoot()
                .resolve("scripts/audit-execution-integrity.conf");
        assertThat(conf)
                .as(".conf companion must exist")
                .exists();
    }

    @Test
    @DisplayName("CI workflow wires the Camada 3 audit step")
    void ciWorkflow_wiresCamada3Step() throws IOException {
        Path workflow = repoRoot()
                .resolve(".github/workflows/ci-release.yml");
        assertThat(workflow)
                .as("CI workflow must exist")
                .exists();

        String body = Files.readString(workflow, StandardCharsets.UTF_8);
        assertThat(body)
                .as("CI workflow must run audit-execution-integrity.sh")
                .contains("audit-execution-integrity.sh");
        assertThat(body)
                .as("CI step name must reference Camada 3")
                .contains("Audit Execution Integrity (Camada 3)");
    }

    @Test
    @DisplayName("baseline file exists and uses canonical comment header")
    void baseline_existsWithHeader() throws IOException {
        Path baseline = repoRoot()
                .resolve("audits/execution-integrity-baseline.txt");
        assertThat(baseline)
                .as("baseline file must exist")
                .exists();

        String head = Files.readString(baseline, StandardCharsets.UTF_8);
        assertThat(head)
                .as("baseline must declare its purpose at the top")
                .contains("Execution Integrity Baseline")
                .contains("Rule 24");
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java")
                ? cwd.getParent()
                : cwd;
    }
}
