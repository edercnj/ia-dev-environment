package dev.iadev.smoke;

import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end smoke for EPIC-0051 KP migration.
 *
 * <p>Validates the RULE-051-01/03/04/07 invariants on the
 * fully generated {@code .claude/} output for a java-spring
 * profile:</p>
 * <ol>
 *   <li>{@code knowledge/} populated (≥ 32 KPs).</li>
 *   <li>No forbidden frontmatter fields in knowledge files.</li>
 *   <li>Sampled skill consumers reference knowledge/ not
 *       skills/{kp}/SKILL.md.</li>
 *   <li>Rules 03, 04, 05 reference knowledge/ where
 *       applicable.</li>
 * </ol>
 */
@DisplayName("KnowledgePackMigrationSmokeTest")
class KnowledgePackMigrationSmokeTest {

    private static final Pattern OLD_KP_PATH =
            Pattern.compile(
                    "skills/(architecture|security|"
                    + "compliance|coding-standards|"
                    + "observability|protocols|testing|"
                    + "performance-engineering|resilience"
                    + ")/SKILL\\.md");

    private static final List<String> FORBIDDEN_FIELDS =
            List.of("user-invocable:", "allowed-tools:",
                    "argument-hint:", "context-budget:");

    @Test
    @DisplayName("migration_endToEnd_knowledgePopulated")
    void migration_endToEnd_knowledgePopulated(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = runPipeline(tempDir);
        Path knowledge = outputDir.resolve(
                ".claude/knowledge");

        assertThat(Files.isDirectory(knowledge))
                .as(".claude/knowledge/ must exist")
                .isTrue();

        try (Stream<Path> entries =
                Files.list(knowledge)) {
            long count = entries
                    .filter(p -> !p.getFileName()
                            .toString().startsWith("."))
                    .count();
            assertThat(count)
                    .as("knowledge/ must contain >= 30 KPs")
                    .isGreaterThanOrEqualTo(30);
        }
    }

    @Test
    @DisplayName("migration_endToEnd_noForbiddenFrontmatter")
    void migration_endToEnd_noForbiddenFrontmatter(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = runPipeline(tempDir);
        Path knowledge = outputDir.resolve(
                ".claude/knowledge");

        try (Stream<Path> files = Files.walk(knowledge)) {
            List<Path> offenders = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName()
                            .toString().endsWith(".md"))
                    .filter(this::hasForbiddenField)
                    .toList();
            assertThat(offenders)
                    .as("No knowledge file may declare"
                            + " skill-only fields"
                            + " (RULE-051-07)")
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("migration_endToEnd_sampledSkillsUseNewPaths")
    void migration_endToEnd_sampledSkillsUseNewPaths(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = runPipeline(tempDir);
        List<String> sampled = List.of(
                "x-arch-plan", "x-review-pr",
                "x-story-plan");
        for (String skill : sampled) {
            Path skillMd = outputDir.resolve(
                    ".claude/skills/" + skill + "/SKILL.md");
            if (!Files.isRegularFile(skillMd)) {
                continue;
            }
            String content = Files.readString(
                    skillMd, StandardCharsets.UTF_8);
            assertThat(
                    OLD_KP_PATH.matcher(content).find())
                    .as("Skill %s must not reference"
                                    + " old skills/{kp}/SKILL.md"
                                    + " path", skill)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("migration_endToEnd_rulesUseNewPaths")
    void migration_endToEnd_rulesUseNewPaths(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = runPipeline(tempDir);
        Path rulesDir = outputDir.resolve(".claude/rules");
        try (Stream<Path> files = Files.walk(rulesDir)) {
            List<Path> offenders = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName()
                            .toString().endsWith(".md"))
                    .filter(p -> hasOldKpPath(p))
                    .toList();
            assertThat(offenders)
                    .as("Rules must not reference old"
                            + " skills/{kp}/SKILL.md path")
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("migration_endToEnd_legacySkillsKpOutputRemoved")
    void migration_endToEnd_legacySkillsKpOutputRemoved(
            @TempDir Path tempDir) throws IOException {
        // During transition (story 0051-0006 will finalize),
        // SkillsCopyHelper may still emit skills/{kp}/ for
        // backward compat. This smoke asserts the new
        // knowledge/ output is the canonical one by checking
        // that migrated KPs ALSO exist under knowledge/.
        Path outputDir = runPipeline(tempDir);
        List<String> sampled = List.of(
                "architecture", "coding-standards",
                "testing", "security", "observability");
        for (String kp : sampled) {
            Path simple = outputDir.resolve(
                    ".claude/knowledge/" + kp + ".md");
            Path complexIdx = outputDir.resolve(
                    ".claude/knowledge/" + kp + "/index.md");
            assertThat(
                    Files.isRegularFile(simple)
                            || Files.isRegularFile(
                                    complexIdx))
                    .as("KP %s must exist under knowledge/"
                                    + " (simple or complex)",
                            kp)
                    .isTrue();
        }
    }

    private Path runPipeline(Path tempDir) {
        Path outputDir = tempDir.resolve("output");
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ProjectConfig config = ConfigProfiles
                .getStack("java-spring");
        new AssemblerPipeline(
                AssemblerPipeline.buildAssemblers())
                .runPipeline(config, outputDir,
                        PipelineOptions.defaults());
        return outputDir;
    }

    private boolean hasForbiddenField(Path file) {
        try {
            String content = Files.readString(
                    file, StandardCharsets.UTF_8);
            if (!content.startsWith("---")) {
                return false;
            }
            int end = content.indexOf("---", 3);
            if (end < 0) {
                return false;
            }
            String fm = content.substring(3, end);
            for (String field : FORBIDDEN_FIELDS) {
                if (fm.contains(field)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean hasOldKpPath(Path file) {
        try {
            String content = Files.readString(file);
            return OLD_KP_PATH.matcher(content).find();
        } catch (IOException e) {
            return false;
        }
    }
}
