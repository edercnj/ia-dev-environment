package dev.iadev.lifecycle.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.iadev.application.lifecycle.LifecycleReconciler;
import dev.iadev.application.lifecycle.StatusSyncException;
import dev.iadev.application.lifecycle.StatusTransitionInvalidException;
import dev.iadev.domain.lifecycle.Divergence;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin CLI wrapper around {@link LifecycleReconciler} — the
 * Java entry point invoked by the {@code x-status-reconcile}
 * skill (story-0046-0006 TASK-003). I/O is abstracted through
 * the {@code stdout} / {@code stderr} writers so smoke tests
 * can assert on captured output without forking a process.
 *
 * <p>Exit codes map to the story's §5.2 data contract:</p>
 * <ul>
 *   <li>0 — SUCCESS (no divergence OR diagnose mode)</li>
 *   <li>10 — APPLIED (apply succeeded — caller creates the
 *       commit via {@code x-git-commit} inside the skill; the
 *       CLI returns the list of rewritten files)</li>
 *   <li>20 — STATUS_SYNC_FAILED (markdown write failure)</li>
 *   <li>30 — STATE_FILE_INVALID (missing / malformed
 *       execution-state.json)</li>
 *   <li>40 — STATUS_TRANSITION_INVALID (suspicious transition
 *       — markdown was ahead of state.json)</li>
 *   <li>50 — USER_ABORTED (operator chose ABORT at gate;
 *       produced by the skill layer, not the CLI)</li>
 * </ul>
 *
 * <p>The CLI itself never invokes {@code git commit}. The
 * skill layer composes the commit via Rule 13 INLINE-SKILL
 * after inspecting the final JSON emitted by this class.</p>
 */
public final class StatusReconcileCli {

    public static final int SUCCESS = 0;
    public static final int APPLIED = 10;
    public static final int STATUS_SYNC_FAILED = 20;
    public static final int STATE_FILE_INVALID = 30;
    public static final int STATUS_TRANSITION_INVALID = 40;
    public static final int USER_ABORTED = 50;
    public static final int USAGE_ERROR = 2;

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    private final LifecycleReconciler reconciler;
    private final PrintWriter out;
    private final PrintWriter err;

    public StatusReconcileCli(LifecycleReconciler rec,
            PrintWriter out, PrintWriter err) {
        this.reconciler = rec;
        this.out = out;
        this.err = err;
    }

    /**
     * Runs the CLI against the given options and returns the
     * process exit code. The {@code plansRoot} parameter is
     * the directory that contains {@code epic-XXXX/} children
     * — typically {@code plans/} at the repo root; tests pass
     * a temp dir.
     */
    public int run(Options o, Path plansRoot) {
        if ((o.epicId == null || o.epicId.isBlank())
                && (o.storyId == null
                        || o.storyId.isBlank())) {
            err.println("ERROR: must provide --epic or "
                    + "--story");
            return USAGE_ERROR;
        }
        String epicId = resolveEpicId(o);
        Path epicDir = plansRoot.resolve(
                "epic-" + epicId);
        if (!Files.isDirectory(epicDir)) {
            err.println("ERROR: epic dir not found: "
                    + epicDir);
            return STATE_FILE_INVALID;
        }

        // Rule 19 — legacy v1 skip.
        try {
            if (reconciler.isLegacyV1(epicDir)) {
                out.println("legacy epic; skipping per "
                        + "Rule 19");
                writeFinalJson(out, "SUCCESS", epicId,
                        List.of(), null, "diagnose");
                return SUCCESS;
            }
        } catch (StatusSyncException ex) {
            err.println("ERROR: " + ex.getMessage());
            return STATE_FILE_INVALID;
        }

        // Diagnose.
        List<Divergence> divergences;
        try {
            divergences = reconciler.diff(epicDir);
        } catch (StatusSyncException ex) {
            err.println("ERROR: " + ex.getMessage());
            return STATE_FILE_INVALID;
        }
        divergences = filterByStoryScope(divergences, o);
        printReport(out, epicId, divergences);

        boolean diagnoseOnly = !o.apply || o.dryRun;
        if (diagnoseOnly) {
            writeFinalJson(out, "SUCCESS", epicId,
                    divergences, null, "diagnose");
            return SUCCESS;
        }

        // Apply path (gate handled by the skill layer; CLI
        // always proceeds when invoked with --apply).
        if (divergences.isEmpty()) {
            writeFinalJson(out, "SUCCESS", epicId,
                    divergences, null, "apply");
            return SUCCESS;
        }
        try {
            reconciler.apply(divergences);
        } catch (StatusTransitionInvalidException ex) {
            err.println("ERROR: "
                    + ex.getMessage());
            return STATUS_TRANSITION_INVALID;
        } catch (StatusSyncException ex) {
            err.println("ERROR: " + ex.getMessage());
            return STATUS_SYNC_FAILED;
        }
        writeFinalJson(out, "APPLIED", epicId,
                divergences, null, "apply");
        return APPLIED;
    }

    private static String resolveEpicId(Options o) {
        if (o.epicId != null && !o.epicId.isBlank()) {
            return o.epicId;
        }
        // story-XXXX-YYYY → epicId=XXXX
        String[] p = o.storyId.split("-");
        if (p.length < 3) {
            throw new IllegalArgumentException(
                    "unparseable story id: " + o.storyId);
        }
        return p[1];
    }

    private static List<Divergence> filterByStoryScope(
            List<Divergence> src, Options o) {
        if (o.storyId == null || o.storyId.isBlank()) {
            return src;
        }
        List<Divergence> keep = new ArrayList<>();
        for (Divergence d : src) {
            if (d.artifactId().equals(o.storyId)) {
                keep.add(d);
            }
        }
        return keep;
    }

    private static void printReport(PrintWriter out,
            String epicId, List<Divergence> divergences) {
        out.println("Divergence report for epic "
                + epicId + ":");
        for (Divergence d : divergences) {
            out.println("  " + d.artifactId() + ": "
                    + labelOrUnset(d.from()) + " \u2192 "
                    + d.to().label());
        }
        out.println("Total divergences: "
                + divergences.size());
    }

    private static String labelOrUnset(
            dev.iadev.domain.lifecycle.LifecycleStatus s) {
        return s == null ? "<unset>" : s.label();
    }

    private static void writeFinalJson(PrintWriter out,
            String status, String epicId,
            List<Divergence> divergences, String commitSha,
            String mode) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("status", status);
        root.put("epicId", epicId);
        ArrayNode arr = root.putArray("divergences");
        for (Divergence d : divergences) {
            ObjectNode item = arr.addObject();
            item.put("artifact", d.artifactId());
            item.put("from",
                    d.from() == null
                            ? null : d.from().label());
            item.put("to", d.to().label());
        }
        root.put("divergenceCount", divergences.size());
        if (commitSha == null) {
            root.putNull("commitSha");
        } else {
            root.put("commitSha", commitSha);
        }
        root.put("mode", mode);
        out.println(root.toString());
        out.flush();
    }

    /** Plain record for CLI arguments (populated by skill). */
    public static final class Options {
        public String epicId;
        public String storyId;
        public boolean apply;
        public boolean dryRun;
        public boolean nonInteractive;
    }
}
