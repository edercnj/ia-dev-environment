package dev.iadev.application.taskmap;

import dev.iadev.domain.taskfile.ParsedTaskFile;
import dev.iadev.domain.taskfile.TaskFileParser;
import dev.iadev.domain.taskmap.RawTask;
import dev.iadev.domain.taskmap.TaskGraph;
import dev.iadev.domain.taskmap.TopologicalSorter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Application use case: orchestrate {@link TaskFileParser} →
 * {@link TopologicalSorter} → {@link TaskMapMarkdownWriter} to produce a
 * {@code task-implementation-map-STORY-XXXX-YYYY.md} from a directory of per-task files.
 *
 * <p>Side effect: writes the resulting markdown to the same plans directory and returns
 * the resolved path. Stateless; thread-safe.</p>
 */
public final class TaskImplementationMapGenerator {

    private static final Pattern TASK_FILENAME = Pattern.compile(
            "^task-(TASK-(\\d{4})-(\\d{4})-\\d{3})\\.md$");

    private TaskImplementationMapGenerator() {
        // static-only
    }

    /**
     * @param plansDir story plans directory containing per-task files (e.g.
     *                 {@code plans/epic-0038/plans/}); also where the output is written
     * @param storyId  story id to filter task files (e.g. {@code "story-0038-0002"})
     * @return path of the written map file
     */
    public static Path generate(Path plansDir, String storyId) {
        Objects.requireNonNull(plansDir, "plansDir");
        Objects.requireNonNull(storyId, "storyId");
        StoryKey key = StoryKey.parse(storyId);
        List<Path> taskFiles = listTaskFiles(plansDir, key);
        if (taskFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "no task files found for " + storyId + " in " + plansDir);
        }
        List<RawTask> rawTasks = parseAll(taskFiles);
        TaskGraph graph = TopologicalSorter.sort(rawTasks);
        String markdown = TaskMapMarkdownWriter.write(storyId, graph);
        Path output = plansDir.resolve(
                "task-implementation-map-STORY-" + key.epic() + "-" + key.story() + ".md");
        writeAtomically(output, markdown);
        return output;
    }

    private static List<Path> listTaskFiles(Path plansDir, StoryKey key) {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(plansDir, "task-TASK-*.md")) {
            for (Path p : stream) {
                Matcher m = TASK_FILENAME.matcher(p.getFileName().toString());
                if (m.matches() && m.group(2).equals(key.epic())
                        && m.group(3).equals(key.story())) {
                    files.add(p);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        files.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return files;
    }

    private static List<RawTask> parseAll(List<Path> files) {
        List<RawTask> raws = new ArrayList<>();
        for (Path file : files) {
            String md = readString(file);
            ParsedTaskFile p = TaskFileParser.parse(md);
            String taskId = p.taskId().orElseThrow(() -> new IllegalArgumentException(
                    "missing **ID:** in " + file.getFileName()));
            String title = inferTitle(md);
            raws.add(new RawTask(
                    taskId, title, p.dependencies(),
                    p.testabilityCheckedKinds().size() == 1
                            ? p.testabilityCheckedKinds().get(0) : null,
                    p.testabilityReferenceIds()));
        }
        return raws;
    }

    private static String inferTitle(String markdown) {
        for (String line : markdown.split("\\r?\\n", -1)) {
            if (line.startsWith("# Task: ")) {
                return line.substring("# Task: ".length()).trim();
            }
        }
        return "(untitled)";
    }

    private static String readString(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read " + file, e);
        }
    }

    private static void writeAtomically(Path output, String content) {
        Path dir = output.toAbsolutePath().getParent();
        Path tmp = null;
        try {
            tmp = Files.createTempFile(dir, ".task-map-", ".md.tmp");
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, output,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException atomicUnsupported) {
                Files.move(tmp, output, StandardCopyOption.REPLACE_EXISTING);
            }
            tmp = null;
        } catch (IOException e) {
            throw new UncheckedIOException("failed to write " + output, e);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            }
        }
    }

    private record StoryKey(String epic, String story) {

        static StoryKey parse(String storyId) {
            Pattern p = Pattern.compile("^story-(\\d{4})-(\\d{4})$");
            Matcher m = p.matcher(storyId);
            if (!m.matches()) {
                throw new IllegalArgumentException(
                        "storyId must match story-XXXX-YYYY, got '" + storyId + "'");
            }
            return new StoryKey(m.group(1), m.group(2));
        }
    }
}
