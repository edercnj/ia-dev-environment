package dev.iadev.parallelism.cli;

import dev.iadev.parallelism.ParallelismEvaluator;
import dev.iadev.parallelism.ParallelismEvaluator.Report;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * Picocli command: {@code parallel-eval --scope=epic|story|task ...}.
 *
 * <p>Implements the CLI surface documented in the
 * {@code /x-parallel-eval} skill contract. Produces
 * Markdown by default and JSON under {@code --format=json}.</p>
 *
 * <p>Exit codes (story-0041-0004 §5.2):</p>
 * <ul>
 *   <li>0 — no conflicts</li>
 *   <li>1 — warnings only (e.g., missing footprints)</li>
 *   <li>2 — hard or regen conflicts detected</li>
 * </ul>
 */
@Command(
        name = "parallel-eval",
        mixinStandardHelpOptions = true,
        description = "Evaluate parallelism collisions for "
                + "an epic or a pair of stories/tasks.")
public final class ParallelEvalCli implements Callable<Integer> {

    enum Scope { EPIC, STORY, TASK }

    enum Format { MARKDOWN, JSON }

    @Option(names = "--scope", required = true,
            description = "Analysis scope: epic|story|task")
    Scope scope;

    @Option(names = "--epic",
            description = "Path to the epic directory "
                    + "(required when --scope=epic).")
    Path epicDir;

    @Option(names = "--a",
            description = "First ID (story or task).")
    String idA;

    @Option(names = "--b",
            description = "Second ID (story or task).")
    String idB;

    @Option(names = "--out",
            description = "Output file path; stdout if "
                    + "omitted.")
    Path outPath;

    @Option(names = "--format",
            defaultValue = "MARKDOWN",
            description = "Output format: markdown|json "
                    + "(default: markdown)")
    Format format;

    @Option(names = "--map",
            description = "Optional implementation-map path. "
                    + "When supplied, its declared phases "
                    + "override the topological derivation.")
    Path mapPath;

    @Option(names = "--include-soft",
            description = "Include SOFT (read-only) overlaps "
                    + "in the output; default false.")
    boolean includeSoft;

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        ParallelismEvaluator evaluator =
                new ParallelismEvaluator(
                        new dev.iadev.parallelism
                                .CollisionDetector(),
                        includeSoft);
        Report report;
        String scopeLabel;
        switch (scope) {
            case EPIC -> {
                if (epicDir == null) {
                    err("Missing --epic for --scope=epic");
                    return 2;
                }
                report = evaluator.evaluateEpic(epicDir);
                scopeLabel = epicDir.getFileName()
                        .toString().toUpperCase();
            }
            case STORY -> {
                requirePair();
                Path ed = deriveEpicDirFromStory(idA);
                report = evaluator.evaluateStoryPair(
                        ed, idA, idB);
                scopeLabel = idA + " vs " + idB;
            }
            case TASK -> {
                requirePair();
                report = evaluateTaskPair(idA, idB);
                scopeLabel = idA + " vs " + idB;
            }
            default -> {
                err("Unsupported scope");
                return 2;
            }
        }
        String rendered = (format == Format.JSON)
                ? new JsonRenderer()
                        .render(scopeLabel, report)
                : new MarkdownRenderer()
                        .render(scopeLabel, report);
        write(rendered);
        return report.exitCode();
    }

    private void requirePair() {
        if (idA == null || idB == null) {
            throw new IllegalArgumentException(
                    "Missing --a/--b for pair scope");
        }
    }

    private Path deriveEpicDirFromStory(String id) {
        // story-XXXX-YYYY -> plans/epic-XXXX
        String[] parts = id.split("-");
        if (parts.length < 3) {
            throw new IllegalArgumentException(
                    "Unparseable story id: " + id);
        }
        return Path.of(
                "plans", "epic-" + parts[1]);
    }

    private Report evaluateTaskPair(
            String taskA, String taskB) throws IOException {
        // Task scope: locate plan files and parse their
        // ## File Footprint directly.
        Path epicDir = deriveEpicDirFromTask(taskA);
        Path plansDir = epicDir.resolve("plans");
        var fpA = readTaskFootprint(plansDir, taskA);
        var fpB = readTaskFootprint(plansDir, taskB);
        java.util.List<dev.iadev.parallelism.Collision>
                collisions = new java.util.ArrayList<>();
        new dev.iadev.parallelism.CollisionDetector()
                .detect(taskA, fpA, taskB, fpB, includeSoft)
                .ifPresent(collisions::add);
        java.util.List<String> warnings =
                new java.util.ArrayList<>();
        if (fpA.isEmpty()) {
            warnings.add("footprint missing for " + taskA);
        }
        if (fpB.isEmpty()) {
            warnings.add("footprint missing for " + taskB);
        }
        return new Report("task", 2, collisions,
                java.util.List.of(
                        java.util.List.of(taskA, taskB)),
                warnings, java.util.Map.of());
    }

    private Path deriveEpicDirFromTask(String taskId) {
        // TASK-XXXX-YYYY-NNN -> plans/epic-XXXX
        String[] parts = taskId.split("-");
        if (parts.length < 4) {
            throw new IllegalArgumentException(
                    "Unparseable task id: " + taskId);
        }
        return Path.of("plans", "epic-" + parts[1]);
    }

    private dev.iadev.parallelism.FileFootprint
            readTaskFootprint(Path plansDir, String taskId)
            throws IOException {
        Path exact = plansDir.resolve(
                "plan-" + taskId + ".md");
        if (Files.isRegularFile(exact)) {
            return dev.iadev.parallelism
                    .FileFootprintParser.parse(
                            Files.readString(exact));
        }
        // Try task-TASK-XXXX.md
        Path alt = plansDir.resolve(
                "task-" + taskId + ".md");
        if (Files.isRegularFile(alt)) {
            return dev.iadev.parallelism
                    .FileFootprintParser.parse(
                            Files.readString(alt));
        }
        return dev.iadev.parallelism.FileFootprint.EMPTY;
    }

    private void write(String content) throws IOException {
        if (outPath != null) {
            Files.createDirectories(
                    outPath.toAbsolutePath().getParent());
            Files.writeString(outPath, content);
        } else {
            PrintWriter w = spec.commandLine().getOut();
            w.print(content);
            w.flush();
        }
    }

    private void err(String msg) {
        spec.commandLine().getErr().println(msg);
    }

    /** Entry-point used by tests and adapters. */
    public static int main(String... args) {
        return new CommandLine(new ParallelEvalCli())
                .execute(args);
    }
}
