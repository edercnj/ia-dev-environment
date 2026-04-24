package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for EPIC-0055 foundation (stories 0055-0001 + 0055-0002).
 *
 * <p>Validates the infrastructure delivered by the foundation phase BEFORE
 * the 8 orchestrator retrofits (stories 0055-0003 through 0055-0010)
 * land. The per-orchestrator compliance checks (TaskCreate per phase,
 * gates PRE/POST invoked) will be added as retrofits complete.</p>
 *
 * <p>Scope of this test file today:
 * <ul>
 *   <li>Rule 25 is present in every generated profile with the expected
 *       canonical sections.</li>
 *   <li>{@code x-internal-phase-gate} skill is present and marked
 *       internal (Rule 22 visibility).</li>
 *   <li>Both Rule 25 hook scripts (verify-phase-gates.sh,
 *       enforce-phase-sequence.sh) are copied regardless of
 *       {@code config.telemetryEnabled()} — Rule 25 Layer 2/3
 *       enforcement is decoupled from observability toggle.</li>
 *   <li>Generated settings.json wires both hooks under the appropriate
 *       event matchers.</li>
 *   <li>CI audit scripts (audit-task-hierarchy.sh, audit-phase-gates.sh)
 *       exit 0 on the canonical repo tree, and correctly detect
 *       violations in synthetic SKILL.md fixtures.</li>
 *   <li>Runtime hooks fail-open on ambiguous / non-applicable payloads.</li>
 *   <li>Baseline file lists the 8 canonical orchestrators awaiting
 *       retrofit.</li>
 * </ul>
 * </p>
 *
 * @see SmokeTestBase
 * @see Epic0054CompressionSmokeTest
 */
@DisplayName("Epic0055FoundationSmokeTest — Rule 25 + phase-gate infra")
class Epic0055FoundationSmokeTest extends SmokeTestBase {

    private static final List<String> RULE_25_HOOK_FILES = List.of(
            "verify-phase-gates.sh",
            "enforce-phase-sequence.sh");

    private static final List<String> CANONICAL_ORCHESTRATORS = List.of(
            "x-epic-implement",
            "x-story-implement",
            "x-task-implement",
            "x-release",
            "x-epic-orchestrate",
            "x-review",
            "x-review-pr",
            "x-pr-merge-train");

    private static final List<String> RULE_25_CANONICAL_SECTIONS = List.of(
            "## Purpose",
            "## Scope",
            "## Invariants",
            "## `subject` Contract",
            "## Enforcement Layers",
            "## Audit Contract",
            "## Integration with Rule 24",
            "## Backward Compatibility");

    // ---------------------------------------------------------------
    // Pipeline-backed assertions (run the generator for each profile)
    // ---------------------------------------------------------------

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName("Rule 25 is present with canonical sections")
    void smoke_rule25_existsWithCanonicalSections(String profile)
            throws IOException {
        runPipeline(profile);
        Path rule = getOutputDir(profile)
                .resolve(".claude/rules/25-task-hierarchy.md");

        assertThat(rule)
                .as("profile %s: Rule 25 file must exist", profile)
                .exists();

        String body = Files.readString(rule, StandardCharsets.UTF_8);
        for (String section : RULE_25_CANONICAL_SECTIONS) {
            assertThat(body)
                    .as("profile %s: Rule 25 must declare '%s'",
                            profile, section)
                    .contains(section);
        }
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName("x-internal-phase-gate skill is present "
            + "and marked internal")
    void smoke_phaseGateSkill_existsAsInternal(String profile)
            throws IOException {
        runPipeline(profile);
        Path skill = getOutputDir(profile)
                .resolve(".claude/skills/x-internal-phase-gate/SKILL.md");

        assertThat(skill)
                .as("profile %s: phase-gate skill must exist", profile)
                .exists();

        String body = Files.readString(skill, StandardCharsets.UTF_8);
        assertThat(body)
                .as("profile %s: frontmatter must set "
                        + "visibility: internal", profile)
                .contains("visibility: internal")
                .contains("user-invocable: false")
                .contains("model: haiku")
                .contains("🔒 **INTERNAL SKILL**");
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName("Rule 25 hook scripts are copied regardless of "
            + "telemetry status")
    void smoke_hookScripts_alwaysCopied(String profile)
            throws IOException {
        runPipeline(profile);
        Path hooksDir = getOutputDir(profile)
                .resolve(".claude/hooks");

        for (String hook : RULE_25_HOOK_FILES) {
            Path p = hooksDir.resolve(hook);
            assertThat(p)
                    .as("profile %s: hook %s must be copied",
                            profile, hook)
                    .exists();
            assertThat(Files.isExecutable(p))
                    .as("profile %s: hook %s must be executable",
                            profile, hook)
                    .isTrue();
        }
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName("settings.json wires both Rule 25 hooks under the "
            + "correct event matchers")
    void smoke_settingsJson_wiresRule25Hooks(String profile)
            throws IOException {
        runPipeline(profile);
        Path settings = getOutputDir(profile)
                .resolve(".claude/settings.json");

        assertThat(settings).exists();
        String body = Files.readString(settings, StandardCharsets.UTF_8);

        assertThat(body)
                .as("profile %s: settings.json must wire "
                        + "verify-phase-gates.sh under Stop",
                        profile)
                .contains("\"Stop\"")
                .contains("verify-phase-gates.sh");
        assertThat(body)
                .as("profile %s: settings.json must wire "
                        + "enforce-phase-sequence.sh under PreToolUse",
                        profile)
                .contains("\"PreToolUse\"")
                .contains("enforce-phase-sequence.sh");
    }

    // ---------------------------------------------------------------
    // Repo-level assertions (single-run, not per-profile)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("audit-task-hierarchy.sh --self-check exits 0 "
            + "on canonical repo tree")
    void smoke_audit_taskHierarchy_selfCheckOk() throws Exception {
        int exit = runScript(
                repoRoot().resolve("scripts/audit-task-hierarchy.sh"),
                repoRoot(),
                "--self-check");
        assertThat(exit).isZero();
    }

    @Test
    @DisplayName("audit-phase-gates.sh --self-check exits 0 "
            + "on canonical repo tree")
    void smoke_audit_phaseGates_selfCheckOk() throws Exception {
        int exit = runScript(
                repoRoot().resolve("scripts/audit-phase-gates.sh"),
                repoRoot(),
                "--self-check");
        assertThat(exit).isZero();
    }

    @Test
    @DisplayName("audit-task-hierarchy.sh (full scan) exits 0 "
            + "on canonical repo tree")
    void smoke_audit_taskHierarchy_fullScanOk() throws Exception {
        int exit = runScript(
                repoRoot().resolve("scripts/audit-task-hierarchy.sh"),
                repoRoot());
        assertThat(exit).isZero();
    }

    @Test
    @DisplayName("audit-task-hierarchy.sh detects missing TaskCreate "
            + "in a non-baselined canonical orchestrator")
    void smoke_audit_taskHierarchy_detectsMissingTaskCreate(
            @TempDir Path fixtureDir) throws Exception {
        // The audit only scans the 8 canonical orchestrator names —
        // so the fixture SKILL.md must live in a directory named like
        // one of them. Using `x-epic-implement`; the baseline override
        // points at an empty file so the canonical name is NOT
        // grandfathered, forcing the audit to evaluate the body.
        Path skillsRoot = fixtureDir.resolve("skills-root");
        Path skillDir = skillsRoot.resolve(
                "core/dev/x-epic-implement");
        Files.createDirectories(skillDir);
        Files.writeString(
                skillDir.resolve("SKILL.md"),
                """
                ## Phase 0 — Args
                Does not emit TaskCreate.

                ## Phase 1 — Plan
                Also missing.
                """,
                StandardCharsets.UTF_8);

        Path emptyBaseline = fixtureDir.resolve("empty-baseline.txt");
        Files.writeString(emptyBaseline, "");

        int exit = runScript(
                repoRoot().resolve("scripts/audit-task-hierarchy.sh"),
                repoRoot(),
                "--skills-root", skillsRoot.toString(),
                "--baseline", emptyBaseline.toString());
        assertThat(exit).isEqualTo(25);
    }

    @Test
    @DisplayName("audit-phase-gates.sh detects missing gates "
            + "in a non-baselined canonical orchestrator")
    void smoke_audit_phaseGates_detectsMissingGates(
            @TempDir Path fixtureDir) throws Exception {
        Path skillsRoot = fixtureDir.resolve("skills-root");
        Path skillDir = skillsRoot.resolve(
                "core/dev/x-epic-implement");
        Files.createDirectories(skillDir);
        // Phase declarations without any x-internal-phase-gate
        // invocation — the audit must surface this as two gate
        // violations (missing --mode pre + missing --mode post) per
        // phase.
        Files.writeString(
                skillDir.resolve("SKILL.md"),
                """
                ## Phase 0 — Args
                Body without phase-gate invocation.

                ## Phase 1 — Plan
                Body without phase-gate invocation.
                """,
                StandardCharsets.UTF_8);

        Path emptyBaseline = fixtureDir.resolve("empty-baseline.txt");
        Files.writeString(emptyBaseline, "");

        int exit = runScript(
                repoRoot().resolve("scripts/audit-phase-gates.sh"),
                repoRoot(),
                "--skills-root", skillsRoot.toString(),
                "--baseline", emptyBaseline.toString());
        assertThat(exit).isEqualTo(26);
    }

    @Test
    @DisplayName("enforce-phase-sequence.sh fails open on "
            + "non-Skill PreToolUse payload")
    void smoke_hook_enforcePhaseSequence_shortCircuitsOnNonSkill()
            throws Exception {
        Path hook = repoRoot().resolve(
                ".claude/hooks/enforce-phase-sequence.sh");
        assertThat(hook).exists();

        ProcessBuilder pb = new ProcessBuilder(
                "bash", hook.toString());
        pb.directory(repoRoot().toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getOutputStream().write(
                "{\"tool_name\":\"Read\",\"tool_input\":{}}"
                        .getBytes(StandardCharsets.UTF_8));
        p.getOutputStream().close();
        assertThat(p.waitFor(10, TimeUnit.SECONDS)).isTrue();
        assertThat(p.exitValue())
                .as("hook must exit 0 on non-Skill payload")
                .isZero();
    }

    @Test
    @DisplayName("verify-phase-gates.sh fails open when outside "
            + "an active epic/feat/fix branch")
    void smoke_hook_verifyPhaseGates_shortCircuitsOutsideActiveBranch(
            @TempDir Path fakeRepo) throws Exception {
        // Build a minimal git repo on a branch name that the hook should
        // skip ("main"). Copy the hook into it and run without a state
        // file — the hook must exit 0 without touching anything.
        runCommand(fakeRepo, "git", "init", "-q");
        runCommand(fakeRepo, "git",
                "-c", "user.email=t@t", "-c", "user.name=t",
                "commit", "--allow-empty", "-q", "-m", "init");
        runCommand(fakeRepo, "git", "checkout", "-q", "-b", "main");

        Path hookSrc = repoRoot().resolve(
                ".claude/hooks/verify-phase-gates.sh");
        Path hookDest = fakeRepo.resolve(
                ".claude/hooks/verify-phase-gates.sh");
        Files.createDirectories(hookDest.getParent());
        Files.copy(hookSrc, hookDest,
                StandardCopyOption.REPLACE_EXISTING);
        hookDest.toFile().setExecutable(true);

        int exit = runCommand(fakeRepo,
                "bash", hookDest.toString());
        assertThat(exit).isZero();
    }

    @Test
    @DisplayName("task-hierarchy baseline lists all 8 "
            + "canonical orchestrators")
    void smoke_baseline_listsCanonicalOrchestrators()
            throws IOException {
        Path baseline = repoRoot().resolve(
                "audits/task-hierarchy-baseline.txt");
        assertThat(baseline).exists();

        String body = Files.readString(baseline, StandardCharsets.UTF_8);
        for (String orchestrator : CANONICAL_ORCHESTRATORS) {
            assertThat(body)
                    .as("baseline must list '%s' "
                            + "(pending retrofit)", orchestrator)
                    .contains(orchestrator);
        }
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    /**
     * Resolves the repository root from the current working dir.
     * Smoke tests run from {@code java/}, so the repo root is the
     * parent.
     */
    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java")
                ? cwd.getParent()
                : cwd;
    }

    /**
     * Invokes a shell script with args and returns its exit code.
     * Redirects stderr to stdout and drains it so the process
     * doesn't block on a full pipe.
     */
    private int runScript(
            Path script, Path workdir, String... args)
            throws Exception {
        String[] cmd = new String[args.length + 2];
        cmd[0] = "bash";
        cmd[1] = script.toString();
        System.arraycopy(args, 0, cmd, 2, args.length);
        return runCommand(workdir, cmd);
    }

    private int runCommand(Path workdir, String... cmd)
            throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workdir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        // drain output to avoid pipe blocking
        p.getInputStream().readAllBytes();
        boolean finished = p.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException(
                    "Timeout running: " + String.join(" ", cmd));
        }
        return p.exitValue();
    }
}
