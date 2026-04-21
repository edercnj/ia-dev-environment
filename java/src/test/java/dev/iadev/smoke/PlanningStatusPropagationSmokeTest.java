package dev.iadev.smoke;

import dev.iadev.adapter.inbound.cli.StatusFieldParserCli;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end smoke exercising the planning-status propagation
 * CLI against a sandbox filesystem that mirrors the real
 * epic/story layout. Validates the contract that SKILL.md
 * retrofits depend on (story-0046-0002 happy path, fail-loud,
 * and idempotency).
 */
class PlanningStatusPropagationSmokeTest {

    @Test
    void happyPath_readWriteReadRoundtrip(
            @TempDir Path sandbox) throws Exception {
        // Arrange — story starts as Pendente.
        Path story = sandbox.resolve("story-0046-0042.md");
        Files.writeString(story,
                "# Story\n\n"
                + "**ID:** story-0046-0042\n"
                + "**Status:** Pendente\n\n"
                + "Body\n",
                StandardCharsets.UTF_8);

        // Act 1 — read returns Pendente.
        Streams s1 = new Streams();
        int r1 = StatusFieldParserCli.run(
                new String[] {"read", story.toString()},
                s1.out, s1.err);
        assertEquals(0, r1);
        assertEquals("Pendente", s1.outStr().trim());

        // Act 2 — write Planejada.
        Streams s2 = new Streams();
        int r2 = StatusFieldParserCli.run(
                new String[] {
                        "write",
                        story.toString(),
                        "Planejada"},
                s2.out, s2.err);
        assertEquals(0, r2);

        // Assert — on-disk file shows Planejada.
        String onDisk = Files.readString(story,
                StandardCharsets.UTF_8);
        assertTrue(onDisk.contains(
                "**Status:** Planejada"),
                onDisk);

        // Act 3 — read confirms the new status.
        Streams s3 = new Streams();
        int r3 = StatusFieldParserCli.run(
                new String[] {"read", story.toString()},
                s3.out, s3.err);
        assertEquals(0, r3);
        assertEquals("Planejada", s3.outStr().trim());
    }

    @Test
    void failLoud_missingStoryFileExitsTwenty(
            @TempDir Path sandbox) {
        Path missing =
                sandbox.resolve("story-0046-0099.md");

        Streams s = new Streams();
        int exit = StatusFieldParserCli.run(
                new String[] {"read", missing.toString()},
                s.out, s.err);

        assertEquals(20, exit);
        assertTrue(s.errStr().contains(
                "STATUS_SYNC_FAILED"), s.errStr());
        assertTrue(s.errStr().contains(
                "story-0046-0099.md"), s.errStr());
    }

    @Test
    void idempotency_rereadAfterWriteShowsNewStatusOnly(
            @TempDir Path sandbox) throws Exception {
        Path story = sandbox.resolve("story-i.md");
        Files.writeString(story,
                "**Status:** Planejada\n",
                StandardCharsets.UTF_8);

        // Re-attempting Pendente -> Planejada is already done
        // (matrix rejects same-state transitions). Writing
        // Em Andamento is the legitimate next step.
        Streams s = new Streams();
        int exit = StatusFieldParserCli.run(
                new String[] {
                        "write",
                        story.toString(),
                        "Em Andamento"},
                s.out, s.err);
        assertEquals(0, exit);

        String onDisk = Files.readString(story,
                StandardCharsets.UTF_8);
        assertTrue(onDisk.contains(
                "**Status:** Em Andamento"), onDisk);
        // The old Pendente or Planejada line MUST be gone.
        assertEquals(1, countOccurrences(onDisk,
                "**Status:**"),
                "exactly one Status line after rewrite");
    }

    @Test
    void cleanWorkdir_writeProducesNoTempResidue(
            @TempDir Path sandbox) throws Exception {
        Path story = sandbox.resolve("story-c.md");
        Files.writeString(story,
                "**Status:** Pendente\n",
                StandardCharsets.UTF_8);

        Streams s = new Streams();
        int exit = StatusFieldParserCli.run(
                new String[] {
                        "write",
                        story.toString(),
                        "Planejada"},
                s.out, s.err);
        assertEquals(0, exit);

        // No .tmp remnants alongside the target.
        try (var stream = Files.list(sandbox)) {
            long tmpCount = stream
                    .filter(p -> p.getFileName().toString()
                            .endsWith(".tmp"))
                    .count();
            assertEquals(0L, tmpCount,
                    "no .tmp files should remain");
        }
    }

    private static int countOccurrences(String haystack,
            String needle) {
        int n = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx))
                != -1) {
            n++;
            idx += needle.length();
        }
        return n;
    }

    private static final class Streams {
        final ByteArrayOutputStream outBuf =
                new ByteArrayOutputStream();
        final ByteArrayOutputStream errBuf =
                new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(outBuf,
                true, StandardCharsets.UTF_8);
        final PrintStream err = new PrintStream(errBuf,
                true, StandardCharsets.UTF_8);

        String outStr() {
            return outBuf.toString(
                    StandardCharsets.UTF_8);
        }

        String errStr() {
            return errBuf.toString(
                    StandardCharsets.UTF_8);
        }
    }
}
