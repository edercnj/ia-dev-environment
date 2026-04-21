package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Smoke tests for YAML frontmatter and artifact
 * structure validation.
 *
 * <p>Validates that generated skills have valid YAML
 * frontmatter with required fields, that the skill
 * directory structure follows conventions, and that rules
 * follow naming and content conventions.</p>
 *
 * <p>Runs against all 8 bundled profiles using the
 * pipeline in a temporary directory (RULE-006).</p>
 *
 * @see SmokeTestBase
 * @see FrontmatterParser
 */
@DisplayName("FrontmatterSmokeTest")
class FrontmatterSmokeTest extends SmokeTestBase {

    private static final Set<String> REQUIRED_FIELDS =
            Set.of("name", "description");

    private static final Pattern RULE_NAME_PATTERN =
            Pattern.compile("^\\d{2}-.*\\.md$");

    /**
     * The {@code patterns} directory contains an aggregated
     * content file without YAML frontmatter. This is by
     * design: patterns are concatenated content, not skill
     * definitions.
     */
    private static final String PATTERNS_DIR = "patterns";

    static Stream<String> profiles() {
        return SmokeProfiles.profiles();
    }

    @Nested
    @DisplayName("Skills frontmatter")
    class SkillsFrontmatter {

        @ParameterizedTest(name = "[{0}] all SKILL.md "
                + "files have valid YAML frontmatter")
        @MethodSource(
                "dev.iadev.smoke.FrontmatterSmokeTest"
                        + "#profiles")
        @DisplayName("all skills have valid frontmatter")
        void allSkills_haveValidFrontmatter_withRequiredFields(
                String profile) throws IOException {
            runPipeline(profile);
            Path skillsDir = getOutputDir(profile)
                    .resolve(".claude/skills");

            assertThat(skillsDir)
                    .as("Skills directory must exist for "
                            + "profile: %s", profile)
                    .isDirectory();

            List<Path> skillFiles = findSkillFiles(skillsDir);
            assertThat(skillFiles)
                    .as("At least one SKILL.md must exist "
                            + "for profile: %s", profile)
                    .isNotEmpty();

            List<String> violations = new ArrayList<>();
            for (Path skillFile : skillFiles) {
                validateSkillFrontmatter(
                        skillFile, skillsDir, violations);
            }

            if (!violations.isEmpty()) {
                fail("Frontmatter violations in profile "
                        + "'%s':\n  %s".formatted(
                        profile,
                        String.join("\n  ", violations)));
            }
        }

        @ParameterizedTest(name = "[{0}] all SKILL.md "
                + "have non-empty content after frontmatter")
        @MethodSource(
                "dev.iadev.smoke.FrontmatterSmokeTest"
                        + "#profiles")
        @DisplayName("skills have content after frontmatter")
        void allSkills_haveContentAfterFrontmatter(
                String profile) throws IOException {
            runPipeline(profile);
            Path skillsDir = getOutputDir(profile)
                    .resolve(".claude/skills");

            List<Path> skillFiles = findSkillFiles(skillsDir);
            List<String> emptyBodySkills = new ArrayList<>();

            for (Path skillFile : skillFiles) {
                String content = Files.readString(
                        skillFile, StandardCharsets.UTF_8);
                FrontmatterParser.Result result =
                        FrontmatterParser.parse(content);
                if (result.body().trim().isEmpty()) {
                    emptyBodySkills.add(
                            relativePath(skillFile,
                                    skillsDir));
                }
            }

            if (!emptyBodySkills.isEmpty()) {
                fail("Skills with empty body in profile "
                        + "'%s':\n  %s".formatted(
                        profile,
                        String.join("\n  ",
                                emptyBodySkills)));
            }
        }
    }

    @Nested
    @DisplayName("Skill directory structure")
    class SkillDirectoryStructure {

        @ParameterizedTest(name = "[{0}] each SKILL.md "
                + "is in skills/skillName/SKILL.md")
        @MethodSource(
                "dev.iadev.smoke.FrontmatterSmokeTest"
                        + "#profiles")
        @DisplayName("skill files follow directory convention")
        void allSkills_followDirectoryConvention(
                String profile) throws IOException {
            runPipeline(profile);
            Path skillsDir = getOutputDir(profile)
                    .resolve(".claude/skills");

            List<Path> skillFiles = findSkillFiles(skillsDir);
            List<String> violations = new ArrayList<>();

            for (Path skillFile : skillFiles) {
                Path parent = skillFile.getParent();
                if (parent == null
                        || !skillFile.getFileName()
                        .toString().equals("SKILL.md")) {
                    violations.add(
                            "Not named SKILL.md: "
                                    + skillFile);
                    continue;
                }
                String skillName = parent.getFileName()
                        .toString();
                if (skillName.isEmpty()
                        || skillName.startsWith(".")) {
                    violations.add(
                            "Invalid skill directory name: "
                                    + skillName);
                }
            }

            if (!violations.isEmpty()) {
                fail("Directory structure violations in "
                        + "profile '%s':\n  %s".formatted(
                        profile,
                        String.join("\n  ", violations)));
            }
        }

        @ParameterizedTest(name = "[{0}] no orphan files "
                + "in skills directories")
        @MethodSource(
                "dev.iadev.smoke.FrontmatterSmokeTest"
                        + "#profiles")
        @DisplayName("no orphan files in skill dirs")
        void allSkillDirs_containSkillMd(
                String profile) throws IOException {
            runPipeline(profile);
            Path skillsDir = getOutputDir(profile)
                    .resolve(".claude/skills");

            if (!Files.isDirectory(skillsDir)) {
                return;
            }

            List<String> orphanDirs = new ArrayList<>();
            try (Stream<Path> dirs =
                         Files.list(skillsDir)) {
                dirs.filter(Files::isDirectory)
                        .forEach(dir -> {
                            Path skillMd =
                                    dir.resolve("SKILL.md");
                            if (!Files.isRegularFile(skillMd)) {
                                checkForOrphanDir(
                                        dir, skillsDir,
                                        orphanDirs);
                            }
                        });
            }

            if (!orphanDirs.isEmpty()) {
                fail("Skill directories without SKILL.md "
                        + "in profile '%s':\n  %s".formatted(
                        profile,
                        String.join("\n  ", orphanDirs)));
            }
        }
    }

    @Nested
    @DisplayName("Rules structure")
    class RulesStructure {

        @ParameterizedTest(name = "[{0}] rules follow "
                + "NN-name.md naming")
        @MethodSource(
                "dev.iadev.smoke.FrontmatterSmokeTest"
                        + "#profiles")
        @DisplayName("rules follow naming convention")
        void allRules_followNamingConvention(
                String profile) throws IOException {
            runPipeline(profile);
            Path rulesDir = getOutputDir(profile)
                    .resolve(".claude/rules");

            assertThat(rulesDir)
                    .as("Rules directory must exist for "
                            + "profile: %s", profile)
                    .isDirectory();

            List<Path> ruleFiles = findMdFiles(rulesDir);
            assertThat(ruleFiles)
                    .as("At least one rule must exist "
                            + "for profile: %s", profile)
                    .isNotEmpty();

            List<String> violations = new ArrayList<>();
            for (Path ruleFile : ruleFiles) {
                String fileName = ruleFile.getFileName()
                        .toString();
                if (!RULE_NAME_PATTERN.matcher(fileName)
                        .matches()) {
                    violations.add(
                            "Rule file does not match "
                                    + "NN-*.md pattern: "
                                    + fileName);
                }
            }

            if (!violations.isEmpty()) {
                fail("Naming violations in profile "
                        + "'%s':\n  %s".formatted(
                        profile,
                        String.join("\n  ", violations)));
            }
        }

        @ParameterizedTest(name = "[{0}] rules start with "
                + "# Rule or # Global")
        @MethodSource(
                "dev.iadev.smoke.FrontmatterSmokeTest"
                        + "#profiles")
        @DisplayName("rules start with expected headers")
        void allRules_startWithExpectedHeaders(
                String profile) throws IOException {
            runPipeline(profile);
            Path rulesDir = getOutputDir(profile)
                    .resolve(".claude/rules");

            List<Path> ruleFiles = findMdFiles(rulesDir);
            List<String> violations = new ArrayList<>();

            for (Path ruleFile : ruleFiles) {
                String content = Files.readString(
                        ruleFile, StandardCharsets.UTF_8)
                        .trim();
                if (!content.startsWith("# Rule")
                        && !content.startsWith("# Global")) {
                    violations.add(
                            "%s starts with: %s"
                                    .formatted(
                                            ruleFile
                                                    .getFileName(),
                                            firstLine(
                                                    content)));
                }
            }

            if (!violations.isEmpty()) {
                fail("Header violations in profile "
                        + "'%s':\n  %s".formatted(
                        profile,
                        String.join("\n  ", violations)));
            }
        }

        @ParameterizedTest(name = "[{0}] rules are not empty")
        @MethodSource(
                "dev.iadev.smoke.FrontmatterSmokeTest"
                        + "#profiles")
        @DisplayName("rules are not empty")
        void allRules_areNotEmpty(
                String profile) throws IOException {
            runPipeline(profile);
            Path rulesDir = getOutputDir(profile)
                    .resolve(".claude/rules");

            List<Path> ruleFiles = findMdFiles(rulesDir);
            List<String> emptyRules = new ArrayList<>();

            for (Path ruleFile : ruleFiles) {
                if (Files.size(ruleFile) == 0) {
                    emptyRules.add(
                            ruleFile.getFileName()
                                    .toString());
                }
            }

            if (!emptyRules.isEmpty()) {
                fail("Empty rules in profile '%s':\n  %s"
                        .formatted(
                                profile,
                                String.join("\n  ",
                                        emptyRules)));
            }
        }
    }

    @Nested
    @DisplayName("Patterns aggregated file")
    class PatternsAggregatedFile {

        @ParameterizedTest(name = "[{0}] patterns SKILL.md "
                + "exists and is non-empty")
        @MethodSource(
                "dev.iadev.smoke.FrontmatterSmokeTest"
                        + "#profiles")
        @DisplayName("patterns file exists and has content")
        void patterns_existAndHaveContent(
                String profile) throws IOException {
            runPipeline(profile);
            Path patternsFile = getOutputDir(profile)
                    .resolve(
                            ".claude/skills/patterns/SKILL.md");

            assertThat(patternsFile)
                    .as("Patterns SKILL.md must exist "
                            + "for profile: %s", profile)
                    .isRegularFile();
            assertThat(Files.size(patternsFile))
                    .as("Patterns SKILL.md must not be "
                            + "empty for profile: %s",
                            profile)
                    .isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Frontmatter name consistency")
    class FrontmatterNameConsistency {

        @ParameterizedTest(name = "[{0}] frontmatter name "
                + "matches directory name")
        @MethodSource(
                "dev.iadev.smoke.FrontmatterSmokeTest"
                        + "#profiles")
        @DisplayName("skill name matches directory name")
        void allSkills_nameMatchesDirectory(
                String profile) throws IOException {
            runPipeline(profile);
            Path skillsDir = getOutputDir(profile)
                    .resolve(".claude/skills");

            List<Path> skillFiles = findSkillFiles(skillsDir);
            List<String> mismatches = new ArrayList<>();

            for (Path skillFile : skillFiles) {
                String content = Files.readString(
                        skillFile, StandardCharsets.UTF_8);
                FrontmatterParser.Result result =
                        FrontmatterParser.parse(content);

                if (!result.hasFrontmatter()) {
                    continue;
                }

                String dirName = skillFile.getParent()
                        .getFileName().toString();
                result.getField("name").ifPresent(name -> {
                    if (!name.equals(dirName)) {
                        mismatches.add(
                                "Directory '%s' has "
                                        + "name='%s'"
                                        .formatted(
                                                dirName,
                                                name));
                    }
                });
            }

            if (!mismatches.isEmpty()) {
                fail("Name mismatches in profile "
                        + "'%s':\n  %s".formatted(
                        profile,
                        String.join("\n  ", mismatches)));
            }
        }
    }

    private void validateSkillFrontmatter(
            Path skillFile, Path skillsDir,
            List<String> violations) throws IOException {
        String content = Files.readString(
                skillFile, StandardCharsets.UTF_8);
        FrontmatterParser.Result result =
                FrontmatterParser.parse(content);
        String relative = relativePath(skillFile, skillsDir);

        if (!result.hasFrontmatter()) {
            violations.add(
                    "%s: missing YAML frontmatter"
                            .formatted(relative));
            return;
        }

        for (String field : REQUIRED_FIELDS) {
            if (result.getField(field).isEmpty()) {
                violations.add(
                        "%s: missing or empty field '%s'"
                                .formatted(relative, field));
            }
        }
    }

    private static List<Path> findSkillFiles(Path skillsDir)
            throws IOException {
        if (!Files.isDirectory(skillsDir)) {
            return List.of();
        }
        List<Path> result = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(skillsDir)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString()
                            .equals("SKILL.md"))
                    .filter(p -> !isAggregatedContentFile(
                            p, skillsDir))
                    .forEach(result::add);
        }
        return result;
    }

    private static boolean isAggregatedContentFile(
            Path file, Path skillsDir) {
        Path relative = skillsDir.relativize(file);
        return relative.getNameCount() == 2
                && relative.getName(0).toString()
                .equals(PATTERNS_DIR);
    }

    private static List<Path> findMdFiles(Path dir)
            throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<Path> result = new ArrayList<>();
        try (Stream<Path> list = Files.list(dir)) {
            list.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString()
                            .endsWith(".md"))
                    .forEach(result::add);
        }
        return result;
    }

    private static final java.util.Set<String>
            KNOWLEDGE_PACK_DIRS = java.util.Set.of(
                    "database-patterns",
                    "knowledge-packs",
                    "_shared");

    private static void checkForOrphanDir(
            Path dir, Path skillsDir,
            List<String> orphanDirs) {
        String dirName = dir.getFileName().toString();
        if (KNOWLEDGE_PACK_DIRS.contains(dirName)) {
            // Knowledge pack containers and the _shared/
            // directory (story-0047-0001) intentionally have
            // no SKILL.md at their root — they hold
            // reference material only.
            return;
        }
        boolean hasNestedSkills;
        try (Stream<Path> walk = Files.walk(dir)) {
            hasNestedSkills = walk
                    .filter(Files::isRegularFile)
                    .anyMatch(p -> p.getFileName()
                            .toString().equals("SKILL.md"));
        } catch (IOException e) {
            hasNestedSkills = false;
        }
        if (!hasNestedSkills) {
            orphanDirs.add(
                    skillsDir.relativize(dir).toString());
        }
    }

    private static String relativePath(
            Path file, Path base) {
        return base.relativize(file).toString()
                .replace('\\', '/');
    }

    private static String firstLine(String content) {
        int newline = content.indexOf('\n');
        if (newline < 0) {
            return content.length() > 60
                    ? content.substring(0, 60) + "..."
                    : content;
        }
        String first = content.substring(0, newline);
        return first.length() > 60
                ? first.substring(0, 60) + "..."
                : first;
    }
}
