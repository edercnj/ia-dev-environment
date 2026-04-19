package dev.iadev.ci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Validates that {@code scripts/audit-interactive-gates.sh} correctly enforces
 * Rule 20 — Interactive Gates Convention.
 *
 * <p>Tests follow the Gherkin scenarios in story-0043-0006 §7 and use
 * {@link ProcessBuilder} to run the script against synthetic fixture trees.
 * Naming convention: {@code [method]_[scenario]_[expectedBehavior]} per
 * Rule 05 / RULE-005.
 *
 * <p>Coverage note: JaCoCo cannot instrument bash; coverage from this file is
 * limited to the Java harness (ProcessBuilder setup, assertion helpers). Shell
 * logic is validated by scenario-level exit-code assertions. Exception
 * documented per story-0043-0006 DoD §4 ("wrapper may be below limit").
 *
 * @see <a href="../../../../../../../../../scripts/audit-interactive-gates.sh">
 *     audit-interactive-gates.sh</a>
 */
@DisplayName("InteractiveGatesAuditTest")
class InteractiveGatesAuditTest {

    /**
     * Path to the audit script, resolved relative to the Maven module root
     * ({@code java/}) so the test works regardless of working directory.
     */
    private static final Path SCRIPT =
            Paths.get("..").resolve("scripts/audit-interactive-gates.sh")
                    .normalize().toAbsolutePath();

    /**
     * Skills source root used when verifying that the production codebase is
     * clean (the real target for --baseline and strict-mode production runs).
     */
    private static final Path PRODUCTION_SKILLS_DIR =
            Paths.get("src/main/resources/targets/claude/skills")
                    .toAbsolutePath();

    @BeforeAll
    static void assumePrerequisites() {
        assumeTrue(Files.isRegularFile(SCRIPT),
                "Audit script not found at " + SCRIPT
                        + ". Run TASK-0043-0006-001 first.");
        assumeTrue(isBashAvailable(),
                "bash is not available on this platform");
    }

    // -----------------------------------------------------------------------
    // Happy path: production tree with --baseline exits 0
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("audit_withBaseline_returnsZero — "
            + "production codebase with --baseline exits 0")
    void audit_withBaseline_returnsZero() throws Exception {
        assumeTrue(Files.isDirectory(PRODUCTION_SKILLS_DIR),
                "Skills directory not found: " + PRODUCTION_SKILLS_DIR);

        ProcessResult result = runAudit("--baseline");

        assertThat(result.exitCode())
                .as("--baseline mode must exit 0 on a clean post-retrofit tree")
                .isZero();
    }

    // -----------------------------------------------------------------------
    // Happy path: strict mode also passes after all retrofits complete
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("audit_strictMode_returnsZero — "
            + "production codebase in strict mode exits 0 after EPIC-0043")
    void audit_strictMode_returnsZero() throws Exception {
        assumeTrue(Files.isDirectory(PRODUCTION_SKILLS_DIR),
                "Skills directory not found: " + PRODUCTION_SKILLS_DIR);

        ProcessResult result = runAudit();

        assertThat(result.exitCode())
                .as("Strict mode must exit 0 after all retrofits are done")
                .isZero();
    }

    // -----------------------------------------------------------------------
    // Error: injected HALT in SKILL.md without AskUserQuestion
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("audit_withInjectedHalt_returnsOne — "
            + "SKILL.md with HALT and no AskUserQuestion triggers Regex 1")
    void audit_withInjectedHalt_returnsOne(@TempDir Path tempRoot)
            throws Exception {

        Path devDir = buildEmptySkillsTree(tempRoot);

        // Inject HALT without AskUserQuestion.
        Path skillDir = devDir.resolve("x-task-implement");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"),
                "# Skill: x-task-implement\n"
                        + "\n"
                        + "## Phase 2\n"
                        + "\n"
                        + "HALT: run /x-task-implement to resume.\n"
                        + "\n"
                        + "Some additional text.\n",
                StandardCharsets.UTF_8);

        ProcessResult result = runAuditInTree(tempRoot);

        assertThat(result.exitCode())
                .as("HALT without AskUserQuestion must cause exit 1;\n"
                        + "output was:\n" + result.stdout())
                .isEqualTo(1);
        assertThat(result.stdout())
                .as("Output must mention HALT violation")
                .contains("HALT without AskUserQuestion");
    }

    // -----------------------------------------------------------------------
    // Error: deprecated flag in delegation context
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("audit_withDeprecatedFlag_returnsOne — "
            + "--manual-task-approval in skill body triggers Regex 2")
    void audit_withDeprecatedFlag_returnsOne(@TempDir Path tempRoot)
            throws Exception {

        Path devDir = buildEmptySkillsTree(tempRoot);

        // Deprecated flag in an operational Skill(...) invocation.
        Path skillDir = devDir.resolve("x-story-implement");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"),
                "# Skill: x-story-implement\n"
                        + "\n"
                        + "## Phase 2.2.9\n"
                        + "\n"
                        + "    Skill(skill: \"x-foo\","
                        + " args: \"--manual-task-approval\")\n"
                        + "\n"
                        + "AskUserQuestion(question: \"Proceed?\","
                        + " options: [...])\n",
                StandardCharsets.UTF_8);

        ProcessResult result = runAuditInTree(tempRoot);

        assertThat(result.exitCode())
                .as("Deprecated --manual-task-approval in delegation "
                        + "context must cause exit 1;\n"
                        + "output was:\n" + result.stdout())
                .isEqualTo(1);
        assertThat(result.stdout())
                .as("Output must mention 'deprecated flag'")
                .contains("deprecated flag");
    }

    // -----------------------------------------------------------------------
    // Boundary: ## Triggers section is allowlisted
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("audit_triggersSection_isAllowlisted — "
            + "deprecated flag inside ## Triggers is not flagged")
    void audit_triggersSection_isAllowlisted(@TempDir Path tempRoot)
            throws Exception {

        Path devDir = buildEmptySkillsTree(tempRoot);

        Path skillDir = devDir.resolve("x-story-implement");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"),
                "# Skill: x-story-implement\n"
                        + "\n"
                        + "## Triggers\n"
                        + "\n"
                        + "/x-story-implement --interactive\n"
                        + "/x-story-implement --manual-task-approval story-0001-0002\n"
                        + "\n"
                        + "## Phase 2\n"
                        + "\n"
                        + "AskUserQuestion(question: \"Proceed?\", options: [...])\n",
                StandardCharsets.UTF_8);

        ProcessResult result = runAuditInTree(tempRoot);

        assertThat(result.exitCode())
                .as("Deprecated flags inside ## Triggers must NOT be reported;\n"
                        + "output was:\n" + result.stdout())
                .isZero();
    }

    // -----------------------------------------------------------------------
    // Boundary: --interactive-merge is NOT a false positive
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("audit_interactiveMergeFlag_isNotFalsePositive — "
            + "--interactive-merge does not match deprecated --interactive")
    void audit_interactiveMergeFlag_isNotFalsePositive(
            @TempDir Path tempRoot) throws Exception {

        Path devDir = buildEmptySkillsTree(tempRoot);

        Path skillDir = devDir.resolve("x-epic-implement");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"),
                "# Skill: x-epic-implement\n"
                        + "\n"
                        + "## Phase 1\n"
                        + "\n"
                        + "    Skill(skill: \"x-epic-implement\","
                        + " args: \"--interactive-merge\")\n"
                        + "\n"
                        + "AskUserQuestion(question: \"Merge?\", options: [...])\n",
                StandardCharsets.UTF_8);

        ProcessResult result = runAuditInTree(tempRoot);

        assertThat(result.exitCode())
                .as("--interactive-merge must NOT trigger deprecated "
                        + "--interactive pattern (Regex 2 tokenised lookahead);\n"
                        + "output was:\n" + result.stdout())
                .isZero();
    }

    // -----------------------------------------------------------------------
    // Boundary: core/lib is out of scope
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("audit_coreLib_isOutOfScope — "
            + "HALT in core/lib SKILL.md is not scanned")
    void audit_coreLib_isOutOfScope(@TempDir Path tempRoot)
            throws Exception {

        buildEmptySkillsTree(tempRoot);

        // Put HALT in lib (out-of-scope); no in-scope violations.
        Path libSkillDir = tempRoot.resolve(
                "skills/core/lib/x-lib-group-verifier");
        Files.createDirectories(libSkillDir);
        Files.writeString(libSkillDir.resolve("SKILL.md"),
                "# Skill: x-lib-group-verifier\n"
                        + "\n"
                        + "| MISSING_DEPENDENCY | HALT pipeline |\n"
                        + "| BUILD_ERROR        | HALT pipeline |\n",
                StandardCharsets.UTF_8);

        ProcessResult result = runAuditInTree(tempRoot);

        assertThat(result.exitCode())
                .as("HALT in core/lib must NOT be scanned (out of scope);\n"
                        + "output was:\n" + result.stdout())
                .isZero();
    }

    // -----------------------------------------------------------------------
    // Boundary: heading in references/*.md is documentational
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("audit_referencesHeadingMention_isIgnored — "
            + "'## Interactive Workflow (--interactive)' heading in "
            + "references/*.md is not flagged by Regex 2")
    void audit_referencesHeadingMention_isIgnored(@TempDir Path tempRoot)
            throws Exception {

        buildEmptySkillsTree(tempRoot);

        // Create a refs file with --interactive only in a heading (prose).
        Path releaseDir = tempRoot.resolve("skills/core/ops/x-release");
        Files.createDirectories(releaseDir.resolve("references"));
        Files.writeString(releaseDir.resolve("SKILL.md"),
                "# Skill: x-release\n"
                        + "\n"
                        + "## Phase 8\n"
                        + "\n"
                        + "AskUserQuestion(question: \"Proceed?\","
                        + " options: [...])\n",
                StandardCharsets.UTF_8);
        Files.writeString(releaseDir.resolve(
                "references/approval-gate-workflow.md"),
                "# Approval Gate Workflow\n"
                        + "\n"
                        + "## Interactive Workflow (--interactive)\n"
                        + "\n"
                        + "When --interactive was set the skill used AskUserQuestion.\n"
                        + "Without --interactive there was no menu.\n",
                StandardCharsets.UTF_8);

        ProcessResult result = runAuditInTree(tempRoot);

        assertThat(result.exitCode())
                .as("Heading '## Interactive Workflow (--interactive)' in "
                        + "references/*.md must NOT trigger Regex 2;\n"
                        + "output was:\n" + result.stdout())
                .isZero();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds an empty in-scope directory tree at {@code root/skills/core/}.
     * Returns the {@code core/dev/} path.
     */
    private static Path buildEmptySkillsTree(Path root) throws IOException {
        Path devDir = root.resolve("skills/core/dev");
        Files.createDirectories(devDir);
        Files.createDirectories(root.resolve("skills/core/ops"));
        Files.createDirectories(root.resolve("skills/core/review"));
        return devDir;
    }

    /**
     * Runs the audit script against a synthetic tree by passing
     * {@code --skills-dir <treeRoot>/skills} as an override.
     */
    private static ProcessResult runAuditInTree(Path treeRoot)
            throws Exception {
        Path skillsDir = treeRoot.resolve("skills").toAbsolutePath();
        return exec(SCRIPT.toString(), "--skills-dir",
                skillsDir.toString());
    }

    /**
     * Runs the audit script against the production skills directory with
     * optional extra args. The working directory is set to the module root
     * so that relative paths inside the script resolve correctly.
     */
    private static ProcessResult runAudit(String... extraArgs)
            throws Exception {
        List<String> args = new ArrayList<>();
        args.add(SCRIPT.toString());
        for (String a : extraArgs) {
            args.add(a);
        }
        // Run from the repo root (one level above the java/ module).
        Path repoRoot = Paths.get("..").toAbsolutePath().normalize();
        return exec(repoRoot, args.toArray(new String[0]));
    }

    private static ProcessResult exec(String... args) throws Exception {
        return exec(null, args);
    }

    private static ProcessResult exec(Path workDir, String... args)
            throws Exception {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        if (workDir != null) {
            pb.directory(workDir.toFile());
        }
        Process p = pb.start();

        String stdout;
        try (InputStream is = p.getInputStream()) {
            stdout = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        boolean done = p.waitFor(60, TimeUnit.SECONDS);
        assertThat(done)
                .as("Script must finish within 60 s")
                .isTrue();
        return new ProcessResult(p.exitValue(), stdout);
    }

    private static boolean isBashAvailable() {
        try {
            Process p = new ProcessBuilder(
                    "bash", "-c", "command -v bash >/dev/null 2>&1")
                    .start();
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Immutable holder for the process exit code and captured stdout. */
    private record ProcessResult(int exitCode, String stdout) {}
}
