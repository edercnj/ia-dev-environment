package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Content-assertion tests for x-git-cleanup-branches SKILL.md.
 *
 * <p>Validates that the generated skill encodes the destructive-
 * cleanup invariants: literal protected set, flag mutual
 * exclusion, worktree-context guard, and the HEAD-switch
 * fallback required when HEAD is a candidate branch.
 */
@DisplayName("x-git-cleanup-branches — protection + flags + guards")
class GitCleanupBranchesSkillContentTest {

    private static final String SKILL_PATH =
            "skills/x-git-cleanup-branches/SKILL.md";

    private String generateSkillContent(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler = new SkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), outputDir);
        return Files.readString(
                outputDir.resolve(SKILL_PATH),
                StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("frontmatter declares correct metadata")
    class Frontmatter {

        @Test
        @DisplayName("name matches skill identifier")
        void assemble_frontmatter_declaresName(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content)
                    .contains("name: x-git-cleanup-branches");
        }

        @Test
        @DisplayName("user-invocable is true")
        void assemble_frontmatter_userInvocableTrue(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content)
                    .contains("user-invocable: true");
        }

        @Test
        @DisplayName("allowed-tools limited to Bash and Read")
        void assemble_frontmatter_allowedToolsMinimal(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content)
                    .contains("allowed-tools: Bash, Read");
        }

        @Test
        @DisplayName("argument-hint advertises both flags")
        void assemble_frontmatter_argumentHintFlags(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content)
                    .contains("[--dry-run] [--yes]");
        }
    }

    @Nested
    @DisplayName("protected-branch invariant is literal")
    class ProtectedSet {

        @Test
        @DisplayName("regex matches exactly main/master/develop")
        void assemble_protection_literalRegex(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content).contains(
                    "^(main|master|develop)$");
        }

        @Test
        @DisplayName("documentation lists the three protected names")
        void assemble_protection_documentsNames(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content).contains("`main`");
            assertThat(content).contains("`master`");
            assertThat(content).contains("`develop`");
        }
    }

    @Nested
    @DisplayName("flag parsing enforces mutual exclusion")
    class FlagParsing {

        @Test
        @DisplayName("rejects --dry-run combined with --yes")
        void assemble_flags_rejectsCombination(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content).contains(
                    "--dry-run and --yes are mutually exclusive");
        }

        @Test
        @DisplayName("exit code 2 for usage errors")
        void assemble_flags_exitCodeTwo(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            String parseSection = extractSection(
                    content,
                    "### Step 1 — Parse Flags",
                    "### Step 2 —");
            assertThat(parseSection).contains("exit 2");
        }
    }

    @Nested
    @DisplayName("worktree-context guard aborts inside worktrees")
    class WorktreeContextGuard {

        @Test
        @DisplayName("emits IN_WORKTREE_UNSAFE code")
        void assemble_guard_emitsUnsafeCode(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content).contains("IN_WORKTREE_UNSAFE");
        }

        @Test
        @DisplayName("inlines detect_worktree_context snippet")
        void assemble_guard_inlinesDetect(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content)
                    .contains("detect_worktree_context()");
            assertThat(content)
                    .contains("/\\.claude/worktrees/");
        }
    }

    @Nested
    @DisplayName("destructive git operations are specified")
    class GitOperations {

        @Test
        @DisplayName("fetch uses --prune origin")
        void assemble_fetch_hasPruneFlag(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content)
                    .contains("git fetch --prune origin");
        }

        @Test
        @DisplayName("worktree removal uses --force")
        void assemble_worktree_hasForceFlag(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content)
                    .contains("git worktree remove --force");
        }

        @Test
        @DisplayName("branch deletion uses -D (forced)")
        void assemble_branch_hasForceDelete(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content).contains("git branch -D");
        }

        @Test
        @DisplayName("invokes git worktree prune after removal")
        void assemble_worktree_callsPrune(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content).contains("git worktree prune");
        }
    }

    @Nested
    @DisplayName("HEAD-switch fallback protects deletion")
    class HeadSwitchFallback {

        @Test
        @DisplayName("switches to develop when HEAD is candidate")
        void assemble_switch_prefersDevelop(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            String switchSection = extractSection(
                    content,
                    "### Step 9 — Switch Away",
                    "### Step 10 —");
            assertThat(switchSection)
                    .contains("git checkout develop");
        }

        @Test
        @DisplayName("falls back to main when develop missing")
        void assemble_switch_fallsBackToMain(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            String switchSection = extractSection(
                    content,
                    "### Step 9 — Switch Away",
                    "### Step 10 —");
            assertThat(switchSection)
                    .contains("git checkout main");
        }

        @Test
        @DisplayName("aborts when neither develop nor main exist")
        void assemble_switch_abortsWithoutFallback(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content)
                    .contains("NO_SAFE_FALLBACK_BRANCH");
        }
    }

    @Nested
    @DisplayName("confirmation gate guards destructive default")
    class ConfirmationGate {

        @Test
        @DisplayName("prompts y/N when --yes not set")
        void assemble_gate_promptsYesNo(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            assertThat(content).contains("[y/N]");
        }

        @Test
        @DisplayName("gate skipped on dry-run")
        void assemble_gate_dryRunExitsEarly(
                @TempDir Path tempDir) throws IOException {
            String content = generateSkillContent(tempDir);
            String planSection = extractSection(
                    content,
                    "### Step 7 — Print Plan",
                    "### Step 8 —");
            assertThat(planSection)
                    .contains("Dry-run complete");
        }
    }

    private static String extractSection(
            String content, String start, String end) {
        int startIdx = content.indexOf(start);
        if (startIdx < 0) {
            return "";
        }
        if (end == null) {
            return content.substring(startIdx);
        }
        int endIdx = content.indexOf(end, startIdx);
        if (endIdx < 0) {
            return content.substring(startIdx);
        }
        return content.substring(startIdx, endIdx);
    }
}
