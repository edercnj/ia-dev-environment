package dev.iadev.adapter.inbound.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LifecycleAuditCli — exit codes")
class LifecycleAuditCliTest {

    private static String str(ByteArrayOutputStream b) {
        return b.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("scan of clean tree returns EXIT_OK (0)")
    void cleanTree_exit0(@TempDir Path dir) throws IOException {
        Path s = dir.resolve("x-clean");
        Files.createDirectories(s);
        Files.writeString(s.resolve("SKILL.md"),
                "# Clean\n## Core Loop\n1. Phase 1\n## "
                        + "Phase 1\nbody\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int code = LifecycleAuditCli.run(
                new String[]{"scan", "--skills-root",
                        dir.toString()},
                new PrintStream(out), new PrintStream(err));

        assertThat(code).isEqualTo(LifecycleAuditCli.EXIT_OK);
        assertThat(str(out)).contains("0 violations");
    }

    @Test
    @DisplayName("scan with violations returns "
            + "EXIT_VIOLATIONS (11)")
    void dirtyTree_exit11(@TempDir Path dir) throws IOException {
        Path s = dir.resolve("x-dirty");
        Files.createDirectories(s);
        Files.writeString(s.resolve("SKILL.md"),
                "# Dirty\n## Core Loop\n1. Do X\n"
                        + "2. Run --skip-verification now\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int code = LifecycleAuditCli.run(
                new String[]{"scan", "--skills-root",
                        dir.toString()},
                new PrintStream(out), new PrintStream(err));

        assertThat(code)
                .isEqualTo(LifecycleAuditCli.EXIT_VIOLATIONS);
        assertThat(str(err))
                .contains("LIFECYCLE_AUDIT_REGRESSION");
    }

    @Test
    @DisplayName("usage error returns EXIT_USAGE (2)")
    void badArgs_exit2() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int code = LifecycleAuditCli.run(
                new String[]{"nope"},
                new PrintStream(out), new PrintStream(err));

        assertThat(code).isEqualTo(LifecycleAuditCli.EXIT_USAGE);
    }

    @Test
    @DisplayName("--json emits JSON array")
    void jsonFlag_emitsArray(@TempDir Path dir)
            throws IOException {
        Path s = dir.resolve("x-j");
        Files.createDirectories(s);
        Files.writeString(s.resolve("SKILL.md"),
                "# J\n## Core Loop\n1. Run "
                        + "--skip-verification fast\n");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        int code = LifecycleAuditCli.run(
                new String[]{"scan", "--skills-root",
                        dir.toString(), "--json"},
                new PrintStream(out), new PrintStream(err));

        assertThat(code)
                .isEqualTo(LifecycleAuditCli.EXIT_VIOLATIONS);
        assertThat(str(out)).startsWith("[")
                .contains("\"dimension\"");
    }
}
