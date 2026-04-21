package dev.iadev.adapter.inbound.cli;

import dev.iadev.application.lifecycle.LifecycleAuditRunner;
import dev.iadev.application.lifecycle.Violation;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Thin command-line bridge exposing
 * {@link LifecycleAuditRunner} as a standalone program.
 *
 * <p>Usage: {@code scan [--skills-root <path>] [--json]}</p>
 * <p>Exit codes: 0 (no violations), 11 (violations found),
 * 2 (usage error).</p>
 */
public final class LifecycleAuditCli {

    /** Successful audit with no violations. */
    public static final int EXIT_OK = 0;

    /** Usage / argv parse error. */
    public static final int EXIT_USAGE = 2;

    /** Violations found. */
    public static final int EXIT_VIOLATIONS = 11;

    private LifecycleAuditCli() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * Executes the audit with the supplied argv and returns a
     * POSIX exit code.
     *
     * @param argv command-line arguments
     * @param out  stdout sink
     * @param err  stderr sink
     * @return exit code
     */
    public static int run(String[] argv, PrintStream out,
            PrintStream err) {
        if (argv == null || argv.length == 0
                || !"scan".equals(argv[0])) {
            err.println("Usage: scan [--skills-root <path>] "
                    + "[--json]");
            return EXIT_USAGE;
        }
        Path root = Path.of("java/src/main/resources/targets/"
                + "claude/skills");
        boolean json = false;
        for (int i = 1; i < argv.length; i++) {
            switch (argv[i]) {
                case "--skills-root":
                    if (i + 1 >= argv.length) {
                        err.println(
                                "--skills-root requires a value");
                        return EXIT_USAGE;
                    }
                    root = Path.of(argv[++i]);
                    break;
                case "--json":
                    json = true;
                    break;
                default:
                    err.println("Unknown arg: " + argv[i]);
                    return EXIT_USAGE;
            }
        }
        List<Violation> violations =
                new LifecycleAuditRunner().scan(root);
        if (violations.isEmpty()) {
            out.println("Lifecycle integrity audit: 0 "
                    + "violations");
            return EXIT_OK;
        }
        if (json) {
            out.println("[");
            for (int i = 0; i < violations.size(); i++) {
                Violation v = violations.get(i);
                out.println("  {\"dimension\":\""
                        + v.dimension() + "\",\"file\":\""
                        + v.file() + "\",\"line\":" + v.line()
                        + ",\"detail\":\""
                        + v.detail().replace("\"", "\\\"")
                        + "\"}"
                        + (i < violations.size() - 1 ? "," : ""));
            }
            out.println("]");
        } else {
            err.println("LIFECYCLE_AUDIT_REGRESSION: "
                    + violations.size() + " violation(s)");
            for (Violation v : violations) {
                err.println("  " + v.dimension() + " "
                        + v.file() + ":" + v.line() + " — "
                        + v.detail());
            }
        }
        return EXIT_VIOLATIONS;
    }
}
