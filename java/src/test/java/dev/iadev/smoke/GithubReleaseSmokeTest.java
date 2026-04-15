package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Story-0039-0006 validation: x-release Phase 11 PUBLISH documents the
 * GitHub Release auto-creation flow with explicit operator confirmation
 * (RULE-007), the {@code --no-github-release} opt-out flag, and warn-only
 * error handling when {@code gh release create} fails.
 *
 * <p>This is a documentation smoke test — it validates that the SKILL.md
 * resource contains the contractually required blocks. Runtime behavior of
 * {@code gh release create} is not invoked here; the skill is interpreted by
 * Claude Code at runtime from the generated {@code .claude/skills/} output.
 */
class GithubReleaseSmokeTest {

    private static final Path SKILL_MD = Path.of(
            "src", "main", "resources", "targets", "claude", "skills",
            "core", "ops", "x-release", "SKILL.md");

    private static String read() throws IOException {
        return Files.readString(SKILL_MD, StandardCharsets.UTF_8);
    }

    @Nested
    class FlagDeclaration {

        @Test
        void argumentHint_includesNoGithubReleaseFlag() throws IOException {
            assertThat(read())
                    .as("argument-hint frontmatter must list --no-github-release")
                    .contains("--no-github-release");
        }

        @Test
        void parametersTable_documentsNoGithubReleaseFlag() throws IOException {
            String md = read();
            assertThat(md)
                    .contains("| `--no-github-release` | No |")
                    .contains("RULE-007")
                    .contains("no** silent auto-create mode");
        }
    }

    @Nested
    class PhaseBlock {

        @Test
        void step11_1_blockExists() throws IOException {
            assertThat(read())
                    .contains("#### Step 11.1 — GitHub Release")
                    .contains("story-0039-0006");
        }

        @Test
        void threePaths_allDocumented() throws IOException {
            String md = read();
            assertThat(md)
                    .contains("Path A: --no-github-release")
                    .contains("Path B: interactive Y")
                    .contains("Path C: interactive n");
        }

        @Test
        void changelogExtractor_referenced() throws IOException {
            assertThat(read())
                    .contains("dev.iadev.release.changelog.ChangelogBodyExtractor");
        }

        @Test
        void askUserQuestion_wired() throws IOException {
            String md = read();
            assertThat(md)
                    .contains("AskUserQuestion")
                    .contains("Create GitHub Release v")
                    .contains("Yes, create now")
                    .contains("No, skip");
        }

        @Test
        void ghInvocation_usesArgvNotShellInterpolation() throws IOException {
            String md = read();
            // Argv invocation pattern: gh release create "v${VERSION}" --title ... --notes ...
            assertThat(md)
                    .contains("gh release create \"v${VERSION}\"")
                    .contains("--title")
                    .contains("--notes");
        }
    }

    @Nested
    class StateContract {

        @Test
        void githubReleaseUrl_persistedToState() throws IOException {
            String md = read();
            assertThat(md)
                    .contains("githubReleaseUrl")
                    .contains("nullable");
        }

        @Test
        void allThreePaths_updateState() throws IOException {
            String md = read();
            // Path A + C set null; Path B sets URL or null on failure.
            assertThat(md)
                    .contains(".githubReleaseUrl = null")
                    .contains(".githubReleaseUrl = $url");
        }
    }

    @Nested
    class ErrorCatalog {

        @Test
        void publishGhReleaseFailed_warnOnly() throws IOException {
            String md = read();
            assertThat(md)
                    .contains("PUBLISH_GH_RELEASE_FAILED")
                    // warn-only => exit column is "—" (em dash) in the catalog row
                    .containsPattern("PUBLISH_GH_RELEASE_FAILED[^\\n]*—");
        }

        @Test
        void publishUnknownFlag_exit1() throws IOException {
            String md = read();
            assertThat(md)
                    .contains("PUBLISH_UNKNOWN_FLAG")
                    .containsPattern("PUBLISH_UNKNOWN_FLAG[^\\n]*\\| 1 \\|");
        }
    }

    @Nested
    class WorkflowDocumentation {

        @Test
        void phaseOverview_mentionsGithubRelease() throws IOException {
            assertThat(read())
                    .contains("11. PUBLISH")
                    .contains("GitHub Release");
        }

        @Test
        void dryRun_showsGithubReleasePrompt() throws IOException {
            String md = read();
            assertThat(md)
                    .contains("gh release create v2.3.0")
                    .contains("--no-github-release")
                    .contains("PUBLISH_GH_RELEASE_FAILED");
        }
    }
}
