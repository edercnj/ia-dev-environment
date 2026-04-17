package dev.iadev.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * Smoke integration test for {@link PiiAudit}. Builds a tiny
 * NDJSON tree with deliberately polluted entries, runs the
 * audit, and asserts that the tool (a) finds the expected
 * matches, (b) exits 1, and (c) ignores clean files.
 */
@DisplayName("PiiAudit — smoke IT")
class PiiAuditSmokeIT {

    @Test
    @DisplayName("detects >= 3 findings in polluted NDJSON"
            + " and exits 1")
    void audit_pollutedTree_returnsExitOne(
            @TempDir Path root) throws IOException {
        Path dirtyFile = root.resolve(
                "plans/epic-0040/telemetry/events.ndjson");
        Files.createDirectories(dirtyFile.getParent());
        String jwt = "eyJhbGciOiJIUzI1NiJ9"
                + ".eyJzdWIiOiJhbGljZSJ9"
                + ".sigSIG";
        Files.writeString(
                dirtyFile,
                line("leaked AKIAIOSFODNN7EXAMPLE")
                        + line("contact jane@example.com")
                        + line("Bearer " + jwt)
                        + line("clean line, no secret"),
                StandardCharsets.UTF_8);

        Path cleanFile = root.resolve(
                "plans/epic-0041/telemetry/events.ndjson");
        Files.createDirectories(cleanFile.getParent());
        Files.writeString(
                cleanFile,
                line("all good, no PII"),
                StandardCharsets.UTF_8);

        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine cmd = new CommandLine(new PiiAudit())
                .setOut(new PrintWriter(out))
                .setErr(new PrintWriter(err));

        int exit = cmd.execute(
                "--root", root.toString());

        assertThat(exit).isEqualTo(PiiAudit.EXIT_FINDINGS);
        String stdout = out.toString();
        assertThat(stdout)
                .contains("events.ndjson")
                .contains("aws_access_key")
                .contains("email")
                .contains("bearer");
    }

    @Test
    @DisplayName("clean tree returns exit 0 and no findings")
    void audit_cleanTree_returnsExitZero(
            @TempDir Path root) throws IOException {
        Path cleanFile = root.resolve(
                "plans/epic-0040/telemetry/events.ndjson");
        Files.createDirectories(cleanFile.getParent());
        Files.writeString(
                cleanFile,
                line("regular message with no secrets")
                        + line("phaseNumber: 2 retryCount: 1"),
                StandardCharsets.UTF_8);

        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine cmd = new CommandLine(new PiiAudit())
                .setOut(new PrintWriter(out))
                .setErr(new PrintWriter(err));

        int exit = cmd.execute(
                "--root", root.toString());

        assertThat(exit).isEqualTo(PiiAudit.EXIT_CLEAN);
        assertThat(out.toString())
                .contains("clean (0 findings)");
    }

    @Test
    @DisplayName("audit() returns a deterministic finding"
                + " list for a polluted file")
    void audit_programmaticApi_returnsFindings(
            @TempDir Path root) throws IOException {
        Path dirtyFile = root.resolve(
                "telemetry/events.ndjson");
        Files.createDirectories(dirtyFile.getParent());
        Files.writeString(
                dirtyFile,
                line("cpf 123.456.789-00")
                        + line("AKIAAAAAAAAAAAAAAAAA"),
                StandardCharsets.UTF_8);

        StringWriter out = new StringWriter();
        List<PiiAudit.Finding> findings = new PiiAudit()
                .audit(root, new PrintWriter(out));

        assertThat(findings).hasSize(2);
        // File order is line-by-line; line 1 = cpf,
        // line 2 = aws_access_key (matches the fixture).
        assertThat(findings.get(0).category()).isEqualTo(
                "cpf");
        assertThat(findings.get(0).lineNumber()).isEqualTo(1);
        assertThat(findings.get(1).category()).isEqualTo(
                "aws_access_key");
        assertThat(findings.get(1).lineNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("quiet flag suppresses per-finding lines")
    void audit_quiet_suppressesDetailLines(
            @TempDir Path root) throws IOException {
        Path dirtyFile = root.resolve(
                "telemetry/events.ndjson");
        Files.createDirectories(dirtyFile.getParent());
        Files.writeString(
                dirtyFile,
                line("AKIAAAAAAAAAAAAAAAAA"),
                StandardCharsets.UTF_8);

        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine cmd = new CommandLine(new PiiAudit())
                .setOut(new PrintWriter(out))
                .setErr(new PrintWriter(err));

        int exit = cmd.execute(
                "--root", root.toString(), "--quiet");

        assertThat(exit).isEqualTo(PiiAudit.EXIT_FINDINGS);
        String stdout = out.toString();
        assertThat(stdout)
                .contains("1 finding")
                .doesNotContain("aws_access_key:AKIA");
    }

    @Test
    @DisplayName("audit() with null reportSink produces"
            + " the same finding list without error")
    void audit_nullReportSink_suppressesOutput(
            @TempDir Path root) throws IOException {
        Path dirtyFile = root.resolve(
                "telemetry/events.ndjson");
        Files.createDirectories(dirtyFile.getParent());
        Files.writeString(
                dirtyFile,
                line("AKIAAAAAAAAAAAAAAAAA"),
                StandardCharsets.UTF_8);

        List<PiiAudit.Finding> findings = new PiiAudit()
                .audit(root, null);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).category())
                .isEqualTo("aws_access_key");
    }

    @Test
    @DisplayName("Finding record validates its fields")
    void finding_constructor_rejectsInvalidInputs() {
        Path any = Path.of(".");
        // line 0 is invalid.
        assertThat(org.junit.jupiter.api.Assertions
                .assertThrows(
                        IllegalArgumentException.class,
                        () -> new PiiAudit.Finding(
                                any, 0, "cat", "x")))
                .hasMessageContaining("lineNumber");

        // null category is invalid.
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> new PiiAudit.Finding(
                        any, 1, null, "x"));
    }

    @Test
    @DisplayName("Finding.format produces a grep-style"
            + " FILE:LINE:CATEGORY:SNIPPET line")
    void finding_format_isGrepStyle() {
        PiiAudit.Finding f = new PiiAudit.Finding(
                Path.of("/tmp/events.ndjson"),
                5,
                "email",
                "user@example.com");

        assertThat(f.format()).isEqualTo(
                "/tmp/events.ndjson:5:email:"
                        + "user@example.com");
    }

    @Test
    @DisplayName("long snippet is truncated to 40 chars"
            + " plus ellipsis")
    void audit_longMatch_snippetTruncated(
            @TempDir Path root) throws IOException {
        Path dirtyFile = root.resolve(
                "telemetry/events.ndjson");
        Files.createDirectories(dirtyFile.getParent());
        String longToken = "ghp_"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaa";
        Files.writeString(
                dirtyFile,
                line(longToken),
                StandardCharsets.UTF_8);

        StringWriter out = new StringWriter();
        List<PiiAudit.Finding> findings = new PiiAudit()
                .audit(root, new PrintWriter(out));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).snippet())
                .endsWith("...")
                .hasSize(43);
    }

    @Test
    @DisplayName("missing root yields exit 2")
    void audit_missingRoot_returnsExitTwo(
            @TempDir Path parent) {
        Path absent = parent.resolve("does/not/exist");
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine cmd = new CommandLine(new PiiAudit())
                .setOut(new PrintWriter(out))
                .setErr(new PrintWriter(err));

        int exit = cmd.execute(
                "--root", absent.toString());

        assertThat(exit).isEqualTo(PiiAudit.EXIT_ERROR);
        assertThat(err.toString())
                .contains("does not exist");
    }

    private static String line(String body) {
        return "{\"failureReason\":\""
                + body.replace("\"", "\\\"")
                + "\"}\n";
    }
}
