package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.cli.TaskMapGenCommand;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * End-to-end smoke for the task-implementation-map pipeline (TASK-0038-0002-007):
 * copies the 7-task fixture into a temp directory, invokes the CLI, and asserts the
 * generated map matches the golden file byte-for-byte and finishes within 200 ms.
 */
class TaskMapGenerationE2ESmokeTest {

    private static final Path FIXTURE_DIR = Path.of(
            "..", "plans", "epic-0038", "examples", "story-fixture-0038-0002");

    private static final Path GOLDEN_PATH = Path.of(
            "src", "test", "resources", "smoke", "epic-0038",
            "task-implementation-map-STORY-0038-0002.md");

    @Test
    void cliAgainstFixture_matchesGoldenByteForByteAndIsFastEnough(@TempDir Path tmp)
            throws IOException {
        copyFixture(FIXTURE_DIR, tmp);

        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        long start = System.nanoTime();
        int exit = new CommandLine(new TaskMapGenCommand())
                .setOut(new PrintWriter(out))
                .setErr(new PrintWriter(err))
                .execute("--story", "story-0038-0002", "--plans-dir", tmp.toString());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        assertThat(exit)
                .as("CLI must exit 0 (stderr=%s)", err)
                .isZero();

        Path generated = tmp.resolve("task-implementation-map-STORY-0038-0002.md");
        assertThat(generated).exists();

        byte[] actual = Files.readAllBytes(generated);
        byte[] golden = Files.readAllBytes(GOLDEN_PATH);
        assertThat(actual)
                .as("generated map must match golden byte-for-byte")
                .isEqualTo(golden);

        assertThat(elapsedMs)
                .as("generation must complete within 2000 ms (took %d ms)", elapsedMs)
                .isLessThan(2000L);
    }

    private static void copyFixture(Path src, Path dst) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(src, "task-TASK-*.md")) {
            for (Path p : stream) {
                Files.copy(p, dst.resolve(p.getFileName()),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    @Test
    void goldenFileReadable_andHasExpectedFormatMarkers() throws IOException {
        String md = Files.readString(GOLDEN_PATH, StandardCharsets.UTF_8);
        assertThat(md)
                .startsWith("# Task Implementation Map — story-0038-0002")
                .contains("## Dependency Graph")
                .contains("```mermaid")
                .contains("## Execution Order")
                .contains("## Coalesced Groups")
                .contains("## Parallelism Analysis")
                .contains("- Total tasks: 7");
    }
}
