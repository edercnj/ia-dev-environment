package dev.iadev.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("GitTagReader — integration against a temporary git repository")
class GitTagReaderIT {

    @Test
    @DisplayName("lastTag_noTagsInRepo_returnsEmpty")
    void lastTag_noTagsInRepo_returnsEmpty(@TempDir Path tempDir) throws Exception {
        initRepo(tempDir);
        commit(tempDir, "initial.txt", "hello", "chore: seed repo");

        GitTagReader reader = new GitTagReader(tempDir);

        assertThat(reader.lastTag()).isEmpty();
    }

    @Test
    @DisplayName("lastTag_afterTagCreation_returnsTagName")
    void lastTag_afterTagCreation_returnsTagName(@TempDir Path tempDir) throws Exception {
        initRepo(tempDir);
        commit(tempDir, "a.txt", "a", "feat: first");
        run(tempDir, "git", "tag", "-a", "v1.0.0", "-m", "first release");

        GitTagReader reader = new GitTagReader(tempDir);

        assertThat(reader.lastTag()).contains("v1.0.0");
    }

    @Test
    @DisplayName("commitsSince_withTag_returnsCommitsAfterTag")
    void commitsSince_withTag_returnsCommitsAfterTag(@TempDir Path tempDir) throws Exception {
        initRepo(tempDir);
        commit(tempDir, "a.txt", "a", "feat: first");
        run(tempDir, "git", "tag", "-a", "v1.0.0", "-m", "first release");
        commit(tempDir, "b.txt", "b", "feat: second");
        commit(tempDir, "c.txt", "c", "fix: third");

        GitTagReader reader = new GitTagReader(tempDir);
        List<String> commits = reader.commitsSince(Optional.of("v1.0.0"));

        assertThat(commits).hasSize(2);
        assertThat(commits.get(0)).startsWith("fix: third");
        assertThat(commits.get(1)).startsWith("feat: second");
    }

    @Test
    @DisplayName("commitsSince_noRef_returnsAllCommits")
    void commitsSince_noRef_returnsAllCommits(@TempDir Path tempDir) throws Exception {
        initRepo(tempDir);
        commit(tempDir, "a.txt", "a", "feat: first");
        commit(tempDir, "b.txt", "b", "fix: second");

        GitTagReader reader = new GitTagReader(tempDir);
        List<String> commits = reader.commitsSince(Optional.empty());

        assertThat(commits).hasSize(2);
    }

    @Test
    @DisplayName("commitsSince_emptyRange_returnsEmptyList")
    void commitsSince_emptyRange_returnsEmptyList(@TempDir Path tempDir) throws Exception {
        initRepo(tempDir);
        commit(tempDir, "a.txt", "a", "feat: first");
        run(tempDir, "git", "tag", "-a", "v1.0.0", "-m", "first release");

        GitTagReader reader = new GitTagReader(tempDir);

        assertThat(reader.commitsSince(Optional.of("v1.0.0"))).isEmpty();
    }

    @Test
    @DisplayName("commitsSince_unsafeRef_rejectsInput")
    void commitsSince_unsafeRef_rejectsInput(@TempDir Path tempDir) throws Exception {
        initRepo(tempDir);
        commit(tempDir, "a.txt", "a", "feat: first");

        GitTagReader reader = new GitTagReader(tempDir);

        assertThatThrownBy(() -> reader.commitsSince(Optional.of("v1.0.0; rm -rf /")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsafe characters");
    }

    @Test
    @DisplayName("new_pathIsNotDirectory_rejected")
    void new_pathIsNotDirectory_rejected(@TempDir Path tempDir) throws IOException {
        Path file = Files.writeString(tempDir.resolve("file.txt"), "x");

        assertThatThrownBy(() -> new GitTagReader(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ----- fixture helpers -----

    private static void initRepo(Path dir) throws Exception {
        run(dir, "git", "init", "-q", "-b", "main");
        run(dir, "git", "config", "user.email", "test@iadev.dev");
        run(dir, "git", "config", "user.name", "Test User");
        run(dir, "git", "config", "commit.gpgsign", "false");
        run(dir, "git", "config", "tag.gpgsign", "false");
    }

    private static void commit(Path dir, String file, String content, String message)
            throws Exception {
        Files.writeString(dir.resolve(file), content, StandardCharsets.UTF_8);
        run(dir, "git", "add", file);
        run(dir, "git", "commit", "-q", "-m", message);
    }

    private static void run(Path dir, String... argv) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(argv)
                .directory(dir.toFile())
                .redirectErrorStream(true);
        pb.environment().put("GIT_TERMINAL_PROMPT", "0");
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException(
                    "command failed (" + exit + "): " + String.join(" ", argv) + "\n" + output);
        }
    }
}
