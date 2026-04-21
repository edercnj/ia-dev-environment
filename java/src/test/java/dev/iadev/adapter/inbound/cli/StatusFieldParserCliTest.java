package dev.iadev.adapter.inbound.cli;

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
 * Exercises the {@link StatusFieldParserCli} entry point. The
 * CLI is the bridge between SKILL.md retrofits (Bash) and the
 * Java helper layer. Tests follow Rule 05 naming convention:
 * {@code [method]_[scenario]_[expectedBehavior]}.
 */
class StatusFieldParserCliTest {

    @Test
    void run_readValidFile_printsLabelAndExitsZero(
            @TempDir Path dir) throws Exception {
        Path file = dir.resolve("story-x.md");
        Files.writeString(file,
                "# Title\n\n**Status:** Pendente\n",
                StandardCharsets.UTF_8);

        CaptureStreams cs = new CaptureStreams();
        int exit = StatusFieldParserCli.run(
                new String[] {"read", file.toString()},
                cs.out(), cs.err());

        assertEquals(0, exit);
        assertEquals("Pendente",
                cs.stdout().trim());
    }

    @Test
    void run_readFileWithoutStatusLine_exitsStatusSyncFailed(
            @TempDir Path dir) throws Exception {
        Path file = dir.resolve("plain.md");
        Files.writeString(file, "# Title\n",
                StandardCharsets.UTF_8);

        CaptureStreams cs = new CaptureStreams();
        int exit = StatusFieldParserCli.run(
                new String[] {"read", file.toString()},
                cs.out(), cs.err());

        assertEquals(20, exit);
        assertTrue(cs.stderr().contains("STATUS_SYNC_FAILED"),
                cs.stderr());
    }

    @Test
    void run_readMissingFile_exitsStatusSyncFailed(
            @TempDir Path dir) {
        Path missing = dir.resolve("absent.md");

        CaptureStreams cs = new CaptureStreams();
        int exit = StatusFieldParserCli.run(
                new String[] {"read", missing.toString()},
                cs.out(), cs.err());

        assertEquals(20, exit);
        assertTrue(cs.stderr().contains("absent.md"),
                "stderr must echo path: " + cs.stderr());
    }

    @Test
    void run_writeValidTransition_updatesFileExitsZero(
            @TempDir Path dir) throws Exception {
        Path file = dir.resolve("story.md");
        Files.writeString(file,
                "# Title\n\n**Status:** Pendente\n\nBody\n",
                StandardCharsets.UTF_8);

        CaptureStreams cs = new CaptureStreams();
        int exit = StatusFieldParserCli.run(
                new String[] {
                        "write",
                        file.toString(),
                        "Planejada"},
                cs.out(), cs.err());

        assertEquals(0, exit);
        String updated = Files.readString(file,
                StandardCharsets.UTF_8);
        assertTrue(updated.contains(
                "**Status:** Planejada"),
                updated);
    }

    @Test
    void run_writeForbiddenTransition_exitsTransitionInvalid(
            @TempDir Path dir) throws Exception {
        Path file = dir.resolve("story.md");
        Files.writeString(file,
                "**Status:** Pendente\n",
                StandardCharsets.UTF_8);

        CaptureStreams cs = new CaptureStreams();
        int exit = StatusFieldParserCli.run(
                new String[] {
                        "write",
                        file.toString(),
                        "Concluída"},
                cs.out(), cs.err());

        assertEquals(40, exit);
        assertTrue(cs.stderr().contains("Pendente"),
                cs.stderr());
        assertTrue(cs.stderr().contains("Concluída"),
                cs.stderr());
    }

    @Test
    void run_writeMissingFile_exitsStatusSyncFailed(
            @TempDir Path dir) {
        Path missing = dir.resolve("missing.md");

        CaptureStreams cs = new CaptureStreams();
        int exit = StatusFieldParserCli.run(
                new String[] {
                        "write",
                        missing.toString(),
                        "Planejada"},
                cs.out(), cs.err());

        assertEquals(20, exit);
    }

    @Test
    void run_writeUnknownLabel_exitsStatusSyncFailed(
            @TempDir Path dir) throws Exception {
        Path file = dir.resolve("story.md");
        Files.writeString(file,
                "**Status:** Pendente\n",
                StandardCharsets.UTF_8);

        CaptureStreams cs = new CaptureStreams();
        int exit = StatusFieldParserCli.run(
                new String[] {
                        "write",
                        file.toString(),
                        "NotARealStatus"},
                cs.out(), cs.err());

        assertEquals(20, exit);
    }

    @Test
    void run_writeToFileWithoutStatus_exitsStatusSyncFailed(
            @TempDir Path dir) throws Exception {
        Path file = dir.resolve("plain.md");
        Files.writeString(file, "# No status line\n",
                StandardCharsets.UTF_8);

        CaptureStreams cs = new CaptureStreams();
        int exit = StatusFieldParserCli.run(
                new String[] {
                        "write",
                        file.toString(),
                        "Planejada"},
                cs.out(), cs.err());

        assertEquals(20, exit);
    }

    @Test
    void run_noArgs_printsUsageExitsTwo() {
        CaptureStreams cs = new CaptureStreams();
        int exit = StatusFieldParserCli.run(
                new String[0],
                cs.out(), cs.err());

        assertEquals(2, exit);
        assertTrue(cs.stderr().toLowerCase()
                        .contains("usage"),
                cs.stderr());
    }

    @Test
    void run_unknownSubcommand_exitsTwo() {
        CaptureStreams cs = new CaptureStreams();
        int exit = StatusFieldParserCli.run(
                new String[] {"frobnicate", "x"},
                cs.out(), cs.err());

        assertEquals(2, exit);
    }

    @Test
    void run_readNoPath_exitsTwo() {
        CaptureStreams cs = new CaptureStreams();
        int exit = StatusFieldParserCli.run(
                new String[] {"read"},
                cs.out(), cs.err());

        assertEquals(2, exit);
    }

    @Test
    void run_writeMissingArgs_exitsTwo() {
        CaptureStreams cs = new CaptureStreams();
        int exit = StatusFieldParserCli.run(
                new String[] {"write", "only-path"},
                cs.out(), cs.err());

        assertEquals(2, exit);
    }

    /** Captures stdout/stderr for a single CLI invocation. */
    private static final class CaptureStreams {
        private final ByteArrayOutputStream outBuf =
                new ByteArrayOutputStream();
        private final ByteArrayOutputStream errBuf =
                new ByteArrayOutputStream();
        private final PrintStream out =
                new PrintStream(outBuf, true,
                        StandardCharsets.UTF_8);
        private final PrintStream err =
                new PrintStream(errBuf, true,
                        StandardCharsets.UTF_8);

        PrintStream out() {
            return out;
        }

        PrintStream err() {
            return err;
        }

        String stdout() {
            return outBuf.toString(
                    StandardCharsets.UTF_8);
        }

        String stderr() {
            return errBuf.toString(
                    StandardCharsets.UTF_8);
        }
    }
}
