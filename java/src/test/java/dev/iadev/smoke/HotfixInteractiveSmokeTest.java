package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.release.BumpType;
import dev.iadev.release.CommitCounts;
import dev.iadev.release.ConventionalCommitsParser;
import dev.iadev.release.GitTagReader;
import dev.iadev.release.HotfixInvalidCommitsException;
import dev.iadev.release.HotfixVersionNotPatchException;
import dev.iadev.release.ReleaseContext;
import dev.iadev.release.SemVer;
import dev.iadev.release.VersionBumper;
import dev.iadev.release.VersionDetector;
import dev.iadev.release.preflight.DashboardData;
import dev.iadev.release.preflight.PreflightDashboardRenderer;
import dev.iadev.release.resume.StateFileDetector;
import dev.iadev.release.summary.SummaryRenderer;
import dev.iadev.release.telemetry.ReleaseTelemetryWriter;
import dev.iadev.release.integrity.IntegrityReport;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * End-to-end smoke test for story-0039-0014 (hotfix parity).
 *
 * <p>Wires the real domain primitives ({@link VersionDetector},
 * {@link StateFileDetector}, {@link PreflightDashboardRenderer},
 * {@link SummaryRenderer}, {@link ReleaseTelemetryWriter})
 * against a temporary git fixture carrying one tag
 * {@code v3.1.0} and N Conventional Commits, simulating the
 * {@code /x-release --hotfix} flow non-interactively.</p>
 *
 * <p>Validates the 6 Gherkin scenarios in story §7:
 * degenerate (feat in hotfix), happy (PATCH only), error
 * (override not PATCH), and boundary (state file separate,
 * SUMMARY shows hotfix branch, telemetry releaseType=hotfix).</p>
 */
@DisplayName("Hotfix interactive — smoke (story-0039-0014)")
class HotfixInteractiveSmokeTest {

    @Test
    @DisplayName("smoke_hotfixWithFeatCommits_"
            + "abortsHotfixInvalidCommits")
    void smoke_hotfixFeatAborts(@TempDir Path tempDir)
            throws Exception {
        initRepo(tempDir);
        commit(tempDir, "seed.txt", "0", "chore: seed");
        run(tempDir, "git", "tag", "-a", "v3.1.0",
                "-m", "baseline");
        commit(tempDir, "a.txt", "a", "feat: new-feature");
        commit(tempDir, "b.txt", "b", "fix: something");

        assertThatThrownBy(() ->
                detectHotfix(tempDir))
                .isInstanceOf(
                        HotfixInvalidCommitsException.class)
                .satisfies(e -> assertThat(
                        ((HotfixInvalidCommitsException) e)
                                .code())
                        .isEqualTo(
                                "HOTFIX_INVALID_COMMITS"));
    }

    @Test
    @DisplayName("smoke_hotfixWithTwoFixes_"
            + "detectsPatchBumpTo311")
    void smoke_hotfixHappyPath(@TempDir Path tempDir)
            throws Exception {
        initRepo(tempDir);
        commit(tempDir, "seed.txt", "0", "chore: seed");
        run(tempDir, "git", "tag", "-a", "v3.1.0",
                "-m", "baseline");
        commit(tempDir, "a.txt", "a", "fix: one");
        commit(tempDir, "b.txt", "b", "fix: two");

        SemVer next = detectHotfix(tempDir);

        assertThat(next).hasToString("3.1.1");
    }

    @Test
    @DisplayName("smoke_hotfixOverrideMinor_"
            + "abortsHotfixVersionNotPatch")
    void smoke_hotfixOverrideMinorAborts(
            @TempDir Path tempDir) throws Exception {
        initRepo(tempDir);
        commit(tempDir, "seed.txt", "0", "chore: seed");
        run(tempDir, "git", "tag", "-a", "v3.1.0",
                "-m", "baseline");

        SemVer current = SemVer.parse("3.1.0");
        SemVer requested = SemVer.parse("3.2.0");

        assertThatThrownBy(() ->
                VersionDetector.validateOverride(
                        current, requested,
                        ReleaseContext.forHotfix()))
                .isInstanceOf(
                        HotfixVersionNotPatchException.class)
                .satisfies(e -> assertThat(
                        ((HotfixVersionNotPatchException) e)
                                .code())
                        .isEqualTo(
                                "HOTFIX_VERSION_NOT_PATCH"));
    }

    @Test
    @DisplayName("smoke_stateFileSeparate_noCollision")
    void smoke_stateFileSeparate(@TempDir Path tempDir)
            throws Exception {
        Path plansDir = tempDir.resolve("plans");
        Files.createDirectories(plansDir);
        Path existingReleaseState = plansDir.resolve(
                "release-state-3.2.0.json");
        Files.writeString(existingReleaseState,
                "{\"phase\":\"BRANCH\","
                        + "\"version\":\"3.2.0\"}",
                StandardCharsets.UTF_8);

        Path hotfixStatePath =
                StateFileDetector.resolveStatePath(
                        plansDir, "3.1.1",
                        ReleaseContext.forHotfix());

        assertThat(hotfixStatePath).isNotEqualTo(
                existingReleaseState);
        assertThat(hotfixStatePath.getFileName().toString())
                .isEqualTo(
                        "release-state-hotfix-3.1.1.json");
        assertThat(Files.exists(existingReleaseState))
                .isTrue();
    }

    @Test
    @DisplayName("smoke_summaryDiagram_containsHotfixBranch")
    void smoke_summaryDiagramHotfix() {
        String summary = SummaryRenderer.render(
                "3.1.0", "3.1.1", 42,
                ReleaseContext.forHotfix());

        assertThat(summary)
                .contains("hotfix/3.1.1")
                .contains("main:")
                .contains("develop:");
    }

    @Test
    @DisplayName("smoke_telemetryReleaseType_hotfix")
    void smoke_telemetryHotfix() {
        String line = ReleaseTelemetryWriter.format(
                "DETERMINE", "3.1.1",
                "2026-04-15T10:00:00Z",
                ReleaseContext.forHotfix());

        assertThat(line)
                .contains("\"releaseType\":\"hotfix\"")
                .contains("\"version\":\"3.1.1\"")
                .contains("\"phase\":\"DETERMINE\"");
    }

    @Test
    @DisplayName("smoke_preflightBanner_"
            + "modoHotfixRendered")
    void smoke_preflightBannerHotfix() {
        DashboardData data = new DashboardData(
                new SemVer(3, 1, 1, null),
                Optional.of(new SemVer(3, 1, 0, null)),
                5L,
                new CommitCounts(0, 2, 0, 0, 1),
                BumpType.PATCH,
                List.of("- fix: bump"),
                IntegrityReport.aggregate(List.of()),
                "main");

        String rendered =
                PreflightDashboardRenderer.render(
                        data, 10,
                        ReleaseContext.forHotfix());

        assertThat(rendered)
                .contains("modo HOTFIX, base=main")
                .contains("bump=PATCH");
    }

    // ----- end-to-end hotfix detection -----

    private static SemVer detectHotfix(Path repoDir) {
        GitTagReader reader = new GitTagReader(repoDir);
        Optional<String> lastTag = reader.lastTag();
        SemVer baseline = lastTag.map(SemVer::parse)
                .orElse(SemVer.ZERO);
        List<String> commits =
                reader.commitsSince(lastTag);
        CommitCounts counts =
                ConventionalCommitsParser.classify(commits);
        BumpType type = VersionDetector.detectBump(
                counts, ReleaseContext.forHotfix());
        return VersionBumper.bump(baseline, type);
    }

    // ----- fixture helpers -----

    private static void initRepo(Path dir) throws Exception {
        run(dir, "git", "init", "-q", "-b", "main");
        run(dir, "git", "config", "user.email",
                "test@iadev.dev");
        run(dir, "git", "config", "user.name",
                "Test User");
        run(dir, "git", "config", "commit.gpgsign",
                "false");
        run(dir, "git", "config", "tag.gpgsign",
                "false");
    }

    private static void commit(
            Path dir, String file, String content,
            String message) throws Exception {
        Files.writeString(dir.resolve(file), content,
                StandardCharsets.UTF_8);
        run(dir, "git", "add", file);
        run(dir, "git", "commit", "-q", "-m", message);
    }

    private static void run(Path dir, String... argv)
            throws Exception {
        ProcessBuilder pb = new ProcessBuilder(argv)
                .directory(dir.toFile())
                .redirectErrorStream(true);
        pb.environment().put("GIT_TERMINAL_PROMPT", "0");
        Process process = pb.start();
        String output = new String(
                process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException(
                    "command failed (" + exit + "): "
                            + String.join(" ", argv)
                            + "\n" + output);
        }
    }
}
