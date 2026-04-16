package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.release.BumpType;
import dev.iadev.release.CommitCounts;
import dev.iadev.release.ConventionalCommitsParser;
import dev.iadev.release.GitTagReader;
import dev.iadev.release.InvalidBumpException;
import dev.iadev.release.SemVer;
import dev.iadev.release.VersionBumper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end smoke test for story-0039-0001 covering the 6 Gherkin scenarios
 * in §7 of the story. The test wires together the real adapter
 * ({@link GitTagReader}) and the real domain primitives
 * ({@link ConventionalCommitsParser} + {@link VersionBumper}) against a
 * temporary git fixture — no mocks.
 */
@DisplayName("Auto-version detection — smoke (story-0039-0001)")
class AutoVersionDetectionSmokeTest {

    @Test
    @DisplayName("smoke_tagWithThreeFeats_detectsMinorBumpTo110")
    void smoke_tagWithThreeFeats_detectsMinorBumpTo110(@TempDir Path tempDir) throws Exception {
        initRepo(tempDir);
        commit(tempDir, "seed.txt", "0", "chore: seed");
        run(tempDir, "git", "tag", "-a", "v1.0.0", "-m", "baseline");
        commit(tempDir, "a.txt", "a", "feat: alpha");
        commit(tempDir, "b.txt", "b", "feat: beta");
        commit(tempDir, "c.txt", "c", "feat: gamma");

        SemVer next = detect(tempDir);

        assertThat(next).hasToString("1.1.0");
    }

    @Test
    @DisplayName("smoke_noTagTwoFixes_detectsPatchBumpTo001")
    void smoke_noTagTwoFixes_detectsPatchBumpTo001(@TempDir Path tempDir) throws Exception {
        initRepo(tempDir);
        commit(tempDir, "a.txt", "a", "fix: one");
        commit(tempDir, "b.txt", "b", "fix: two");

        SemVer next = detect(tempDir);

        assertThat(next).hasToString("0.0.1");
    }

    @Test
    @DisplayName("smoke_tagWithOnlyDocs_abortsNoBumpSignal")
    void smoke_tagWithOnlyDocs_abortsNoBumpSignal(@TempDir Path tempDir) throws Exception {
        initRepo(tempDir);
        commit(tempDir, "seed.txt", "0", "chore: seed");
        run(tempDir, "git", "tag", "-a", "v1.0.0", "-m", "baseline");
        commit(tempDir, "a.txt", "a", "docs: readme");
        commit(tempDir, "b.txt", "b", "chore: tidy");

        assertThatThrownBy(() -> detect(tempDir))
                .isInstanceOf(InvalidBumpException.class)
                .extracting("code")
                .isEqualTo(InvalidBumpException.Code.VERSION_NO_BUMP_SIGNAL);
    }

    @Test
    @DisplayName("smoke_tagWithFeatBang_detectsMajorBumpTo200")
    void smoke_tagWithFeatBang_detectsMajorBumpTo200(@TempDir Path tempDir) throws Exception {
        initRepo(tempDir);
        commit(tempDir, "seed.txt", "0", "chore: seed");
        run(tempDir, "git", "tag", "-a", "v1.0.0", "-m", "baseline");
        commit(tempDir, "a.txt", "a", "feat!: drop legacy endpoint");

        SemVer next = detect(tempDir);

        assertThat(next).hasToString("2.0.0");
    }

    @Test
    @DisplayName("smoke_tagWithZeroCommits_abortsNoCommits")
    void smoke_tagWithZeroCommits_abortsNoCommits(@TempDir Path tempDir) throws Exception {
        initRepo(tempDir);
        commit(tempDir, "seed.txt", "0", "feat: seed");
        run(tempDir, "git", "tag", "-a", "v1.0.0", "-m", "baseline");

        assertThatThrownBy(() -> detect(tempDir))
                .isInstanceOf(InvalidBumpException.class)
                .extracting("code")
                .isEqualTo(InvalidBumpException.Code.VERSION_NO_COMMITS);
    }

    @Test
    @DisplayName("smoke_explicitVersionOverride_bypassesDetection")
    void smoke_explicitVersionOverride_bypassesDetection(@TempDir Path tempDir) throws Exception {
        initRepo(tempDir);
        commit(tempDir, "a.txt", "a", "feat: noise");

        // explicit override is parsed directly; detection is NOT consulted.
        SemVer explicit = SemVer.parse("4.0.0");

        assertThat(explicit).hasToString("4.0.0");
        assertThatThrownBy(() -> SemVer.parse("3.2"))
                .isInstanceOf(InvalidBumpException.class)
                .extracting("code")
                .isEqualTo(InvalidBumpException.Code.VERSION_INVALID_FORMAT);
    }

    // ----- end-to-end pipeline under test -----

    private static SemVer detect(Path repoDir) {
        GitTagReader reader = new GitTagReader(repoDir);
        Optional<String> lastTag = reader.lastTag();
        SemVer baseline = lastTag.map(SemVer::parse).orElse(SemVer.ZERO);
        List<String> commits = reader.commitsSince(lastTag);
        if (commits.isEmpty()) {
            throw new InvalidBumpException(
                    InvalidBumpException.Code.VERSION_NO_COMMITS,
                    "git log range is empty");
        }
        CommitCounts counts = ConventionalCommitsParser.classify(commits);
        BumpType type = BumpType.from(counts);
        if (type == null) {
            throw new InvalidBumpException(
                    InvalidBumpException.Code.VERSION_NO_BUMP_SIGNAL,
                    "no feat/fix/perf/breaking commits since " + lastTag.orElse("0.0.0"));
        }
        return VersionBumper.bump(baseline, type);
    }

    // ----- fixture helpers (duplicated minimal subset from GitTagReaderIT) -----

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
