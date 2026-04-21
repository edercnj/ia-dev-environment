package dev.iadev.adapter.inbound.cli;

import dev.iadev.application.lifecycle.LifecycleTransitionMatrix;
import dev.iadev.application.lifecycle.StatusFieldParser;
import dev.iadev.application.lifecycle.StatusSyncException;
import dev.iadev.application.lifecycle.StatusTransitionInvalidException;
import dev.iadev.domain.lifecycle.LifecycleStatus;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Thin command-line bridge exposing
 * {@link StatusFieldParser} and
 * {@link LifecycleTransitionMatrix} to SKILL.md retrofits that
 * run under Bash. Designed for the story-0046-0002 contract
 * (see CLI wrapper {@code read}/{@code write} with exit codes
 * {@code 0}, {@code 20}, {@code 40}).
 *
 * <p>Business-logic-free adapter: all lifecycle rules live in
 * {@code application.lifecycle}. This class only parses argv
 * and maps exceptions to POSIX exit codes.</p>
 */
public final class StatusFieldParserCli {

    /** Successful read / write. */
    public static final int EXIT_OK = 0;

    /** Argv parse error (usage, unknown sub-command). */
    public static final int EXIT_USAGE = 2;

    /** Fail-loud: missing file, regex miss, I/O failure. */
    public static final int EXIT_STATUS_SYNC_FAILED = 20;

    /** Transition forbidden by the matrix. */
    public static final int EXIT_STATUS_TRANSITION_INVALID =
            40;

    private StatusFieldParserCli() {
        // Adapter — not instantiable.
    }

    /**
     * JVM entry point. Delegates to
     * {@link #run(String[], PrintStream, PrintStream)} so
     * tests can capture stdout / stderr without touching the
     * global JVM streams.
     */
    public static void main(String[] args) {
        int code = run(args, System.out, System.err);
        System.exit(code);
    }

    /**
     * Testable entry point: returns the exit code instead of
     * calling {@link System#exit(int)}.
     */
    public static int run(String[] args,
            PrintStream out, PrintStream err) {
        if (args == null || args.length == 0) {
            return printUsage(err);
        }
        String sub = args[0];
        if ("read".equals(sub)) {
            return doRead(args, out, err);
        }
        if ("write".equals(sub)) {
            return doWrite(args, out, err);
        }
        return printUsage(err);
    }

    private static int doRead(String[] args,
            PrintStream out, PrintStream err) {
        if (args.length < 2) {
            return printUsage(err);
        }
        Path file = Path.of(args[1]);
        try {
            Optional<LifecycleStatus> status =
                    StatusFieldParser.readStatus(file);
            out.println(
                    status.map(LifecycleStatus::label)
                            .orElse("NONE"));
            return EXIT_OK;
        } catch (StatusSyncException e) {
            err.println("STATUS_SYNC_FAILED: "
                    + e.getMessage());
            return EXIT_STATUS_SYNC_FAILED;
        }
    }

    private static int doWrite(String[] args,
            PrintStream out, PrintStream err) {
        if (args.length < 3) {
            return printUsage(err);
        }
        Path file = Path.of(args[1]);
        String label = args[2];
        Optional<LifecycleStatus> maybeTarget =
                LifecycleStatus.fromLabel(label);
        if (maybeTarget.isEmpty()) {
            err.println(
                    "STATUS_SYNC_FAILED: unknown label '"
                            + label + "' for " + file);
            return EXIT_STATUS_SYNC_FAILED;
        }
        LifecycleStatus target = maybeTarget.get();
        try {
            Optional<LifecycleStatus> current =
                    StatusFieldParser.readStatus(file);
            if (current.isEmpty()) {
                err.println(
                        "STATUS_SYNC_FAILED: no Status line"
                                + " in " + file);
                return EXIT_STATUS_SYNC_FAILED;
            }
            LifecycleTransitionMatrix.validateOrThrow(
                    current.get(), target);
            StatusFieldParser.writeStatus(file, target);
            out.println("OK");
            return EXIT_OK;
        } catch (StatusTransitionInvalidException e) {
            err.println(
                    "STATUS_TRANSITION_INVALID: "
                            + e.getMessage());
            return EXIT_STATUS_TRANSITION_INVALID;
        } catch (StatusSyncException e) {
            err.println("STATUS_SYNC_FAILED: "
                    + e.getMessage());
            return EXIT_STATUS_SYNC_FAILED;
        }
    }

    private static int printUsage(PrintStream err) {
        err.println("Usage: StatusFieldParserCli "
                + "read <file>");
        err.println("       StatusFieldParserCli "
                + "write <file> <status-label>");
        err.println("Status labels: Pendente | Planejada"
                + " | Em Andamento | Concluída | Falha"
                + " | Bloqueada");
        return EXIT_USAGE;
    }

    /** Expose charset used for stream defaults (tests). */
    static java.nio.charset.Charset defaultCharset() {
        return StandardCharsets.UTF_8;
    }
}
