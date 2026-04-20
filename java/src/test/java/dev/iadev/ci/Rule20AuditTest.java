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
 * Validates that {@code scripts/audit-rule-20.sh} correctly
 * enforces Rule 21 — CI-Watch (RULE-045-01).
 *
 * <p>Tests follow the Gherkin scenarios in story-0045-0002 §7
 * and use {@link ProcessBuilder} to run the script against
 * synthetic fixture trees.
 * Naming convention:
 * {@code [method]_[scenario]_[expectedBehavior]} per Rule 05.
 *
 * <p>Coverage note: JaCoCo cannot instrument bash; coverage from
 * this file is limited to the Java harness (ProcessBuilder setup,
 * assertion helpers). Shell logic is validated by
 * scenario-level exit-code assertions.
 *
 * @see <a href=
 *     "../../../../../../../../../scripts/audit-rule-20.sh">
 *     audit-rule-20.sh</a>
 */
@DisplayName("Rule20AuditTest")
class Rule20AuditTest {

    /**
     * Path to the audit script, resolved relative to the Maven
     * module root ({@code java/}) so the test works regardless
     * of working directory.
     */
    private static final Path SCRIPT =
            Paths.get("..").resolve("scripts/audit-rule-20.sh")
                    .normalize().toAbsolutePath();

    @BeforeAll
    static void assumePrerequisites() {
        assumeTrue(Files.isRegularFile(SCRIPT),
                "Audit script not found at " + SCRIPT
                        + ". Ensure scripts/audit-rule-20.sh"
                        + " exists (story-0045-0002).");
        assumeTrue(isBashAvailable(),
                "bash is not available on this platform");
    }

    // -----------------------------------------------------------
    // Happy path: compliant SKILL.md exits 0
    // -----------------------------------------------------------

    @Test
    @DisplayName("audit_compliantSkill_returnsZero — "
            + "SKILL.md with x-pr-create AND x-pr-watch-ci"
            + " exits 0")
    void audit_compliantSkill_returnsZero(
            @TempDir Path tempRoot) throws Exception {

        buildSkillWithBothCalls(tempRoot,
                "x-story-implement");

        ProcessResult result = runAuditInTree(tempRoot);

        assertThat(result.exitCode())
                .as("Compliant SKILL.md must exit 0;\n"
                        + "output was:\n"
                        + result.stdout())
                .isZero();
    }

    // -----------------------------------------------------------
    // Happy path: opt-out via --no-ci-watch exits 0
    // -----------------------------------------------------------

    @Test
    @DisplayName("audit_optOutExplicit_returnsZero — "
            + "SKILL.md with x-pr-create and --no-ci-watch"
            + " exits 0")
    void audit_optOutExplicit_returnsZero(
            @TempDir Path tempRoot) throws Exception {

        buildSkillWithPrCreateAndOptOut(tempRoot,
                "x-task-implement");

        ProcessResult result = runAuditInTree(tempRoot);

        assertThat(result.exitCode())
                .as("--no-ci-watch opt-out must exit 0;\n"
                        + "output was:\n"
                        + result.stdout())
                .isZero();
    }

    // -----------------------------------------------------------
    // Error: x-pr-create without x-pr-watch-ci exits 1
    // -----------------------------------------------------------

    @Test
    @DisplayName("audit_missingCiWatch_returnsOne — "
            + "SKILL.md with x-pr-create but no x-pr-watch-ci"
            + " exits 1")
    void audit_missingCiWatch_returnsOne(
            @TempDir Path tempRoot) throws Exception {

        buildSkillWithPrCreateOnly(tempRoot,
                "x-story-implement");

        ProcessResult result = runAuditInTree(tempRoot);

        assertThat(result.exitCode())
                .as("Missing x-pr-watch-ci must cause exit 1;"
                        + "\noutput was:\n"
                        + result.stdout())
                .isEqualTo(1);
        assertThat(result.stdout())
                .as("Output must mention x-pr-create violation")
                .contains("x-pr-create");
    }

    // -----------------------------------------------------------
    // Boundary: file without x-pr-create invocation exits 0
    // -----------------------------------------------------------

    @Test
    @DisplayName("audit_noPrCreate_returnsZero — "
            + "SKILL.md without x-pr-create invocation"
            + " exits 0")
    void audit_noPrCreate_returnsZero(
            @TempDir Path tempRoot) throws Exception {

        buildSkillWithoutPrCreate(tempRoot, "x-test-tdd");

        ProcessResult result = runAuditInTree(tempRoot);

        assertThat(result.exitCode())
                .as("File with no x-pr-create must exit 0;\n"
                        + "output was:\n"
                        + result.stdout())
                .isZero();
    }

    // -----------------------------------------------------------
    // Boundary: mention in prose (not Skill call) is not counted
    // -----------------------------------------------------------

    @Test
    @DisplayName("audit_proseOnlyMention_returnsZero — "
            + "x-pr-create mentioned in prose/table is not"
            + " flagged as invocation")
    void audit_proseOnlyMention_returnsZero(
            @TempDir Path tempRoot) throws Exception {

        Path skillDir = createSkillDir(tempRoot,
                "x-epic-implement");
        Files.writeString(skillDir.resolve("SKILL.md"),
                "# Skill: x-epic-implement\n"
                        + "\n"
                        + "## Integration Notes\n"
                        + "\n"
                        + "| Skill | Relationship |\n"
                        + "| x-pr-create | creates PRs |\n"
                        + "\n"
                        + "See x-pr-create for details.\n",
                StandardCharsets.UTF_8);

        ProcessResult result = runAuditInTree(tempRoot);

        assertThat(result.exitCode())
                .as("Prose mention of x-pr-create must NOT "
                        + "trigger violation;\n"
                        + "output was:\n"
                        + result.stdout())
                .isZero();
    }

    // -----------------------------------------------------------
    // Helpers — fixture builders
    // -----------------------------------------------------------

    private static Path createSkillDir(
            Path root, String skillName) throws IOException {
        Path dir = root.resolve("skills/core/dev/"
                + skillName);
        Files.createDirectories(dir);
        return dir;
    }

    private static void buildSkillWithBothCalls(
            Path root, String skillName) throws IOException {
        Path dir = createSkillDir(root, skillName);
        Files.writeString(dir.resolve("SKILL.md"),
                "# Skill: " + skillName + "\n"
                        + "\n"
                        + "## Phase 2.2.7\n"
                        + "\n"
                        + "    Skill(skill: \"x-pr-create\","
                        + " args: \"TASK-0001\")\n"
                        + "\n"
                        + "## Phase 2.2.7b\n"
                        + "\n"
                        + "    Skill(skill: \"x-pr-watch-ci\","
                        + " args: \"--pr-number 42\")\n",
                StandardCharsets.UTF_8);
    }

    private static void buildSkillWithPrCreateAndOptOut(
            Path root, String skillName) throws IOException {
        Path dir = createSkillDir(root, skillName);
        Files.writeString(dir.resolve("SKILL.md"),
                "# Skill: " + skillName + "\n"
                        + "\n"
                        + "## Phase 2.2.7\n"
                        + "\n"
                        + "    Skill(skill: \"x-pr-create\","
                        + " args: \"TASK-0001"
                        + " --no-ci-watch\")\n",
                StandardCharsets.UTF_8);
    }

    private static void buildSkillWithPrCreateOnly(
            Path root, String skillName) throws IOException {
        Path dir = createSkillDir(root, skillName);
        Files.writeString(dir.resolve("SKILL.md"),
                "# Skill: " + skillName + "\n"
                        + "\n"
                        + "## Phase 2.2.7\n"
                        + "\n"
                        + "    Skill(skill: \"x-pr-create\","
                        + " args: \"TASK-0001\")\n",
                StandardCharsets.UTF_8);
    }

    private static void buildSkillWithoutPrCreate(
            Path root, String skillName) throws IOException {
        Path dir = createSkillDir(root, skillName);
        Files.writeString(dir.resolve("SKILL.md"),
                "# Skill: " + skillName + "\n"
                        + "\n"
                        + "## Phase 1\n"
                        + "\n"
                        + "Run tests and compile.\n",
                StandardCharsets.UTF_8);
    }

    // -----------------------------------------------------------
    // Helpers — script runners
    // -----------------------------------------------------------

    private static ProcessResult runAuditInTree(
            Path treeRoot) throws Exception {
        Path skillsDir = treeRoot.resolve("skills")
                .toAbsolutePath();
        return exec(SCRIPT.toString(),
                "--skills-dir", skillsDir.toString());
    }

    private static ProcessResult exec(String... args)
            throws Exception {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        String stdout;
        try (InputStream is = p.getInputStream()) {
            stdout = new String(
                    is.readAllBytes(),
                    StandardCharsets.UTF_8);
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
                    "bash", "-c",
                    "command -v bash >/dev/null 2>&1")
                    .start();
            return p.waitFor(5, TimeUnit.SECONDS)
                    && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /** Immutable holder for process exit code and stdout. */
    private record ProcessResult(int exitCode,
            String stdout) {}
}
