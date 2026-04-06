package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Smoke tests for content integrity validation across all
 * 8 bundled profiles.
 *
 * <p>Validates that pipeline output is usable and free of
 * template artifacts. Organized into nested test classes
 * per validation category.</p>
 *
 * <p>RULE-001: Parametrized for all 8 profiles.
 * RULE-002: Independent of golden files.
 * RULE-006: Output in {@code @TempDir}.</p>
 *
 * @see SmokeTestBase
 * @see SmokeTestValidators
 * @see SmokeProfiles
 */
@DisplayName("ContentIntegritySmokeTest")
class ContentIntegritySmokeTest extends SmokeTestBase {

    /** Prefix for template files excluded from checks. */
    static final String TEMPLATE_FILE_PREFIX = "_TEMPLATE";

    private static final String RULES_DIR = "rules";

    @Nested
    @DisplayName("Empty files")
    class EmptyFiles {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke.SmokeProfiles#profiles")
        @DisplayName(
                "No empty files in generated output")
        void noEmptyFiles_output_allHaveContent(
                String profile) throws IOException {
            runPipeline(profile);
            SmokeTestValidators.assertNoEmptyFiles(
                    getOutputDir(profile));
        }
    }

    @Nested
    @DisplayName("Pebble placeholders")
    class PebblePlaceholders {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke.SmokeProfiles#profiles")
        @DisplayName("No unresolved Pebble placeholders "
                + "(excluding allowlist)")
        void noUnresolved_excludingAllowlist_noneFound(
                String profile) throws IOException {
            runPipeline(profile);
            Path outputDir = getOutputDir(profile);

            List<String> violations = new ArrayList<>();
            Files.walkFileTree(outputDir,
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(
                                Path file,
                                BasicFileAttributes a)
                                throws IOException {
                            if (isTemplateFile(file)) {
                                return FileVisitResult
                                        .CONTINUE;
                            }
                            PlaceholderScanner
                                    .scanPebble(
                                            file,
                                            outputDir,
                                            violations);
                            return FileVisitResult
                                    .CONTINUE;
                        }
                    });

            if (!violations.isEmpty()) {
                fail(("Found %d unresolved "
                        + "placeholder(s) in "
                        + "profile '%s':\n%s")
                        .formatted(
                                violations.size(),
                                profile,
                                String.join("\n",
                                        violations)));
            }
        }
    }

    @Nested
    @DisplayName("Data placeholders")
    class DataPlaceholders {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke.SmokeProfiles#profiles")
        @DisplayName("No unresolved data placeholders "
                + "in non-template files")
        void noData_nonTemplate_noneFound(
                String profile) throws IOException {
            runPipeline(profile);
            Path outputDir = getOutputDir(profile);

            List<String> violations = new ArrayList<>();
            Files.walkFileTree(outputDir,
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(
                                Path file,
                                BasicFileAttributes a)
                                throws IOException {
                            if (isTemplateFile(file)
                                    || isRulesFile(
                                    file, outputDir)
                                    || isSkillDocFile(
                                    file, outputDir)) {
                                return FileVisitResult
                                        .CONTINUE;
                            }
                            PlaceholderScanner
                                    .scanData(
                                            file,
                                            outputDir,
                                            violations);
                            return FileVisitResult
                                    .CONTINUE;
                        }
                    });

            if (!violations.isEmpty()) {
                fail(("Found %d data "
                        + "placeholder(s) in "
                        + "profile '%s':\n%s")
                        .formatted(
                                violations.size(),
                                profile,
                                String.join("\n",
                                        violations)));
            }
        }
    }

    @Nested
    @DisplayName("JSON validity")
    class JsonValidity {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke.SmokeProfiles#profiles")
        @DisplayName(
                "All JSON files are valid and parseable")
        void allJson_parseable_noErrors(
                String profile) throws IOException {
            runPipeline(profile);
            Path outputDir = getOutputDir(profile);

            List<Path> files = FileCollector.byExtension(
                    outputDir, Set.of(".json"));

            assertThat(files)
                    .as("Expected .json files for '%s'",
                            profile)
                    .isNotEmpty();

            for (Path f : files) {
                SmokeTestValidators.assertValidJson(f);
            }
        }
    }

    @Nested
    @DisplayName("YAML validity")
    class YamlValidity {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke.SmokeProfiles#profiles")
        @DisplayName("All YAML files are valid and "
                + "parseable")
        void allYaml_parseable_noErrors(
                String profile) throws IOException {
            runPipeline(profile);
            Path outputDir = getOutputDir(profile);

            List<Path> files = FileCollector.byExtension(
                    outputDir,
                    Set.of(".yaml", ".yml"));

            assertThat(files)
                    .as("Expected .yaml files for '%s'",
                            profile)
                    .isNotEmpty();

            for (Path f : files) {
                SmokeTestValidators.assertValidYaml(f);
            }
        }
    }

    @Nested
    @DisplayName("Markdown headers")
    class MarkdownHeaders {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke.SmokeProfiles#profiles")
        @DisplayName("All Markdown files start with "
                + "#, ---, or <!--")
        void allMd_validHeaders_noErrors(
                String profile) throws IOException {
            runPipeline(profile);
            Path outputDir = getOutputDir(profile);

            List<Path> files = FileCollector.byExtension(
                    outputDir, Set.of(".md"));

            assertThat(files)
                    .as("Expected .md files for '%s'",
                            profile)
                    .isNotEmpty();

            List<String> violations = new ArrayList<>();
            for (Path f : files) {
                checkHeader(f, outputDir, violations);
            }

            if (!violations.isEmpty()) {
                fail(("Found %d markdown file(s) "
                        + "without valid header in "
                        + "profile '%s':\n%s")
                        .formatted(
                                violations.size(),
                                profile,
                                String.join("\n",
                                        violations)));
            }
        }

        private static void checkHeader(
                Path file, Path outputDir,
                List<String> violations)
                throws IOException {
            String content = Files.readString(
                    file, StandardCharsets.UTF_8);
            String trimmed = content.stripLeading();

            boolean valid =
                    trimmed.startsWith("#")
                            || trimmed.startsWith("---")
                            || trimmed.startsWith("<!--");

            if (!valid) {
                String rel = SmokeTestValidators
                        .relativizePosix(outputDir, file);
                violations.add(
                        ("  %s -- does not start "
                                + "with '#', '---', "
                                + "or '<!--'")
                                .formatted(rel));
            }
        }
    }

    // -- shared helpers --

    private static boolean isTemplateFile(Path file) {
        return file.getFileName().toString()
                .startsWith(TEMPLATE_FILE_PREFIX);
    }

    private static boolean isRulesFile(
            Path file, Path outputDir) {
        String rel = SmokeTestValidators
                .relativizePosix(outputDir, file);
        return rel.contains("/" + RULES_DIR + "/")
                || rel.startsWith(RULES_DIR + "/");
    }

    private static boolean isSkillDocFile(
            Path file, Path outputDir) {
        String rel = SmokeTestValidators
                .relativizePosix(outputDir, file);
        return rel.contains("/skills/")
                && rel.endsWith(".md");
    }

    /**
     * Collects files by extension from a directory tree.
     */
    static final class FileCollector {

        private FileCollector() {
        }

        static List<Path> byExtension(
                Path dir, Set<String> extensions)
                throws IOException {
            List<Path> result = new ArrayList<>();
            Files.walkFileTree(dir,
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(
                                Path f,
                                BasicFileAttributes a) {
                            String n = f.getFileName()
                                    .toString()
                                    .toLowerCase();
                            for (String e : extensions) {
                                if (n.endsWith(e)) {
                                    result.add(f);
                                    break;
                                }
                            }
                            return FileVisitResult
                                    .CONTINUE;
                        }
                    });
            return result;
        }
    }

    /**
     * Scans files for placeholder patterns.
     */
    static final class PlaceholderScanner {

        /**
         * Skill placeholders resolved at runtime by the
         * AI agent or CI system.
         */
        static final Set<String> ALLOWED = Set.of(
                "LANGUAGE", "LANGUAGE_VERSION",
                "LANGUAGE_EXT",
                "FRAMEWORK", "ARCHITECTURE",
                "ARCH_STYLE",
                "PROJECT_NAME", "project_name",
                "DB_TYPE", "BUILD_FILE", "BUILD_TOOL",
                "DATABASE_TYPE", "MIGRATION_TOOL",
                "LANGUAGE_NAME", "FRAMEWORK_NAME",
                "language_name", "architecture_style",
                "TDD_COMPLIANCE_TABLE", "TDD_SUMMARY",
                "language_version",
                "ORCHESTRATOR", "PORT", "EXT",
                "BUILD_COMMAND", "TEST_COMMAND",
                "COMPILE_COMMAND", "COVERAGE_COMMAND",
                "SMOKE_COMMAND",
                "EPIC_ID", "BRANCH", "PLACEHOLDER",
                "COMMIT_LOG", "PR_LINK",
                "STARTED_AT", "FINISHED_AT",
                "STORIES_COMPLETED", "STORIES_FAILED",
                "STORIES_BLOCKED", "STORIES_TOTAL",
                "COMPLETION_PERCENTAGE",
                "PHASE_TIMELINE_TABLE",
                "STORY_STATUS_TABLE",
                "FINDINGS_SUMMARY",
                "UNRESOLVED_ISSUES",
                "COVERAGE_BEFORE", "COVERAGE_AFTER",
                "COVERAGE_DELTA",
                "entity", "EntityName", "name", "item",
                "author", "dep", "schema", "table_name",
                "resource_path", "placeholders",
                "events_per_snapshot",
                ".", "...");

        private static final Pattern PEBBLE =
                Pattern.compile(
                        "\\{\\{\\s*([^}]+?)\\s*\\}\\}");

        private static final List<Pattern> CI_PATS =
                List.of(
                        Pattern.compile(
                                "github\\.[a-z_.]+"),
                        Pattern.compile(
                                "matrix\\.[a-z_.]+"),
                        Pattern.compile(
                                "secrets\\.[A-Z_]+"),
                        Pattern.compile(
                                "vars\\.[A-Z_a-z]+"),
                        Pattern.compile(
                                "env\\.[A-Z_a-z]+"),
                        Pattern.compile(
                                "runner\\.[a-z_]+"),
                        Pattern.compile(
                                "steps\\.[a-z_-]+"
                                        + "\\.outputs"
                                        + "\\.[a-z_]+"),
                        Pattern.compile(
                                "needs\\.[a-z_-]+"
                                        + "\\.outputs"
                                        + "\\.[a-z_]+"),
                        Pattern.compile(
                                "\\$labels\\.[a-z_]+"),
                        Pattern.compile("\\$value"),
                        Pattern.compile(
                                "fromJson\\(.*\\)"),
                        Pattern.compile(
                                "hashFiles\\(.*\\)"),
                        Pattern.compile(
                                ".*\\|\\s*default"
                                        + "\\(.*\\)"));

        private static final Pattern DATA =
                Pattern.compile(
                        "(?<!\\{)\\{("
                                + "DOMAIN_NAME"
                                + "|DOMAIN_OVERVIEW"
                                + "|WHAT_IT_RECEIVES"
                                + "|WHAT_IT_PROCESSES"
                                + "|WHAT_IT_RETURNS"
                                + "|WHAT_IT_PERSISTS"
                                + "|ENTITIES_TABLE"
                                + "|VALUE_OBJECTS"
                                + "|AGGREGATES"
                                + "|RULE_ID_\\d+"
                                + "|RULE_NAME_\\d+"
                                + "|RULE_\\d+"
                                + "_DESCRIPTION"
                                + "|STATE_MACHINES"
                                + "|PROTOCOLS"
                                + "|SENSITIVE_DATA_TABLE"
                                + "|DATA_HANDLING_RULES"
                                + "|UNIT_TEST_SCENARIOS"
                                + "|INTEGRATION_TEST"
                                + "_SCENARIOS"
                                + "|DOMAIN_ANTI_PATTERNS"
                                + "|GLOSSARY"
                                + ")\\}(?!\\})");

        private PlaceholderScanner() {
        }

        static void scanPebble(
                Path file, Path outputDir,
                List<String> violations)
                throws IOException {
            String content = Files.readString(
                    file, StandardCharsets.UTF_8);
            String rel = SmokeTestValidators
                    .relativizePosix(outputDir, file);

            List<String> lines =
                    content.lines().toList();
            for (int i = 0; i < lines.size(); i++) {
                Matcher m = PEBBLE.matcher(lines.get(i));
                while (m.find()) {
                    String inner = m.group(1).trim();
                    if (!isAllowed(inner)) {
                        violations.add(
                                "  %s:%d -- %s"
                                        .formatted(
                                                rel,
                                                i + 1,
                                                m.group()
                                        ));
                    }
                }
            }
        }

        static void scanData(
                Path file, Path outputDir,
                List<String> violations)
                throws IOException {
            String content = Files.readString(
                    file, StandardCharsets.UTF_8);
            String rel = SmokeTestValidators
                    .relativizePosix(outputDir, file);

            List<String> lines =
                    content.lines().toList();
            for (int i = 0; i < lines.size(); i++) {
                Matcher m = DATA.matcher(lines.get(i));
                while (m.find()) {
                    violations.add(
                            "  %s:%d -- %s"
                                    .formatted(
                                            rel,
                                            i + 1,
                                            m.group()));
                }
            }
        }

        private static boolean isAllowed(String inner) {
            if (ALLOWED.contains(inner)) {
                return true;
            }
            for (Pattern p : CI_PATS) {
                if (p.matcher(inner).matches()) {
                    return true;
                }
            }
            return false;
        }
    }
}
