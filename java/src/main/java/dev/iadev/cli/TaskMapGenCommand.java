package dev.iadev.cli;

import dev.iadev.application.taskmap.TaskImplementationMapGenerator;
import dev.iadev.domain.taskmap.exception.TaskMapGenerationException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * Picocli subcommand: {@code task-map-gen --plans-dir DIR --story story-XXXX-YYYY}.
 *
 * <p>Wraps {@link TaskImplementationMapGenerator} as a CLI entry point. Returns exit
 * code 0 on success and 1 on any error (validation, parsing, sort, I/O), printing a
 * single-line diagnostic to stderr that includes the TASK-IDs involved when relevant.</p>
 */
@Command(
        name = "task-map-gen",
        mixinStandardHelpOptions = true,
        description = "Generate a task-implementation-map for a story.")
public final class TaskMapGenCommand implements Callable<Integer> {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_FAILURE = 1;

    @Option(names = {"-s", "--story"}, required = true,
            description = "Story id, e.g. story-0038-0002")
    String storyId;

    @Option(names = {"-d", "--plans-dir"},
            description = "Plans directory containing task-TASK-*.md files "
                    + "(default: plans/epic-XXXX/plans/ derived from --story)")
    Path plansDir;

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();
        try {
            Path resolvedDir = plansDir != null ? plansDir : defaultPlansDir(storyId);
            Path output = TaskImplementationMapGenerator.generate(resolvedDir, storyId);
            out.println("wrote " + output);
            return EXIT_SUCCESS;
        } catch (TaskMapGenerationException e) {
            err.println("ERROR: " + e.getMessage());
            return EXIT_FAILURE;
        } catch (IllegalArgumentException | UncheckedIOException e) {
            err.println("ERROR: " + e.getMessage());
            return EXIT_FAILURE;
        }
    }

    private static Path defaultPlansDir(String storyId) {
        // story-XXXX-YYYY -> plans/epic-XXXX/plans/. Storage layout is always 4-digit ids;
        // malformed storyIds slip through here but fail downstream in StoryKey.parse with
        // a clear diagnostic.
        String epic = storyId.startsWith("story-") && storyId.length() >= 11
                ? storyId.substring(6, 10) : storyId;
        return Path.of("plans", "epic-" + epic, "plans");
    }
}
