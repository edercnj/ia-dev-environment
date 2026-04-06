package dev.iadev.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Cross-profile consistency smoke test that validates
 * artifacts across all 12 bundled profiles.
 *
 * <p>Runs the pipeline for every profile once, then
 * compares outputs to detect:</p>
 * <ul>
 *   <li>Divergence in artifacts that should be identical
 *       across profiles (e.g., rules file names)</li>
 *   <li>Structural inconsistencies (different file counts
 *       or file names in directories that should be
 *       structurally consistent)</li>
 *   <li>Statistical outliers in artifact category
 *       counts</li>
 * </ul>
 *
 * <p>Legitimately different artifacts (project-identity,
 * domain rules, Dockerfiles) are excluded from identity
 * comparisons.</p>
 *
 * <p>RULE-001: Covers all 12 profiles.
 * RULE-002: Independent of golden files.
 * RULE-006: Output in {@code @TempDir}.</p>
 *
 * @see SmokeTestBase
 * @see SmokeProfiles
 */
@DisplayName("CrossProfileConsistencySmokeTest")
class CrossProfileConsistencySmokeTest {

    /**
     * Directories whose file names must be identical
     * across all profiles.
     */
    static final Set<String> STRUCTURALLY_CONSISTENT_DIRS =
            Set.of(
                    ".claude/rules",
                    ".github/instructions",
                    ".github/hooks",
                    ".github/prompts");

    /**
     * Files that are legitimately different per profile
     * and excluded from byte-identity checks.
     */
    static final Set<String> EXCLUSIONS = Set.of(
            ".claude/rules/01-project-identity.md",
            ".claude/rules/02-domain.md",
            ".claude/rules/03-coding-standards.md",
            "Dockerfile",
            "docker-compose.yml",
            "docker-compose.yaml",
            "k8s",
            ".claude/hooks");

    /**
     * Categories that legitimately vary to zero for some
     * profiles (e.g., hooks for interpreted languages,
     * k8s for CLI tools). Excluded from extreme deviation
     * detection.
     */
    static final Set<String> VARYING_CATEGORIES = Set.of(
            "claude-hooks",
            "contracts",
            "k8s",
            "github-top",
            "root-files");

    /**
     * Artifact categories that must have the same count
     * across all profiles.
     */
    static final Set<String> UNIFORM_CATEGORIES = Set.of(
            "claude-rules",
            "claude-settings",
            "github-hooks",
            "github-instructions",
            "github-issue-templates",
            "github-prompts",
            "tests");

    @TempDir
    private static Path sharedTempDir;

    /**
     * Map of profile name to its output directory.
     * Populated once before all tests.
     */
    private static Map<String, Path> profileOutputs;

    @BeforeAll
    static void runAllProfiles() {
        profileOutputs = new LinkedHashMap<>();
        List<String> profiles = SmokeProfiles.profileList();

        for (String profile : profiles) {
            Path outputDir =
                    sharedTempDir.resolve(profile);
            SmokeTestValidators
                    .createDirectoryQuietly(outputDir);
            profileOutputs.put(profile,
                    runPipelineForProfile(
                            profile, outputDir));
        }
    }

    @Nested
    @DisplayName("Structurally consistent directories")
    class StructuralConsistency {

        @Test
        @DisplayName("rules directory has same file "
                + "names across all profiles")
        void rulesDir_sameFileNames_allProfiles()
                throws IOException {
            assertSameFileNames(".claude/rules");
        }

        @Test
        @DisplayName("github instructions directory has "
                + "same file names across all profiles")
        void instructionsDir_sameFileNames_allProfiles()
                throws IOException {
            assertSameFileNames(".github/instructions");
        }

        @Test
        @DisplayName("github hooks directory has same "
                + "file names across all profiles")
        void hooksDir_sameFileNames_allProfiles()
                throws IOException {
            assertSameFileNames(".github/hooks");
        }

        @Test
        @DisplayName("github prompts directory has same "
                + "file names across all profiles")
        void promptsDir_sameFileNames_allProfiles()
                throws IOException {
            assertSameFileNames(".github/prompts");
        }

        @Test
        @DisplayName("settings.json shares a common "
                + "subset of keys across all profiles")
        void settingsJson_commonKeys_allProfiles()
                throws IOException {
            assertCommonJsonKeysExist(
                    ".claude/settings.json",
                    Set.of("permissions"));
        }
    }

    @Nested
    @DisplayName("Uniform category counts")
    class UniformCategoryCounts {

        @Test
        @DisplayName("categories with uniform counts "
                + "are the same across all profiles")
        void uniformCategories_sameCounts_allProfiles()
                throws IOException {
            String manifestResource =
                    "smoke/expected-artifacts.json";
            var manifest =
                    ExpectedArtifacts.loadFromClasspath(
                            manifestResource);

            List<String> violations = new ArrayList<>();

            for (String category : UNIFORM_CATEGORIES) {
                Map<String, Integer> countsByProfile =
                        new TreeMap<>();

                for (String profile
                        : SmokeProfiles.profileList()) {
                    ProfileArtifacts expected =
                            manifest.getProfile(profile);
                    int count = expected
                            .getCategoryCount(category);
                    countsByProfile.put(profile, count);
                }

                Set<Integer> uniqueCounts =
                        new TreeSet<>(
                                countsByProfile.values());
                if (uniqueCounts.size() > 1) {
                    violations.add(formatCountViolation(
                            category, countsByProfile));
                }
            }

            if (!violations.isEmpty()) {
                fail("Uniform category count "
                        + "violations:\n%s"
                        .formatted(String.join(
                                "\n", violations)));
            }
        }
    }

    @Nested
    @DisplayName("Identical artifacts cross-profile")
    class IdenticalArtifacts {

        @Test
        @DisplayName("rules file names are identical "
                + "across all profiles")
        void rulesFileNames_identical_allProfiles()
                throws IOException {
            assertSameFileNames(".claude/rules");
        }

        @Test
        @DisplayName("github agents file names are "
                + "structurally consistent")
        void githubAgentsNames_consistent_allProfiles()
                throws IOException {
            assertSubsetFileNames(".github/agents");
        }

        @Test
        @DisplayName("github instructions content is "
                + "structurally present in all profiles")
        void githubInstructions_present_allProfiles()
                throws IOException {
            assertSameFileNames(".github/instructions");
        }
    }

    @Nested
    @DisplayName("Exclusion handling")
    class ExclusionHandling {

        @Test
        @DisplayName("project-identity rules differ "
                + "across profiles as expected")
        void projectIdentity_differsAcrossProfiles()
                throws IOException {
            String excludedFile =
                    ".claude/rules/"
                            + "01-project-identity.md";
            Map<String, String> contentByProfile =
                    new HashMap<>();

            for (var entry
                    : profileOutputs.entrySet()) {
                Path file = entry.getValue()
                        .resolve(excludedFile);
                if (Files.isRegularFile(file)) {
                    contentByProfile.put(
                            entry.getKey(),
                            Files.readString(file,
                                    StandardCharsets
                                            .UTF_8));
                }
            }

            assertThat(contentByProfile)
                    .as("01-project-identity.md must "
                            + "exist in all profiles")
                    .hasSize(
                            SmokeProfiles.profileList()
                                    .size());

            Set<String> uniqueContents =
                    new TreeSet<>(
                            contentByProfile.values());
            assertThat(uniqueContents.size())
                    .as("01-project-identity.md should "
                            + "differ across profiles "
                            + "(contains language, "
                            + "framework, build tool)")
                    .isGreaterThan(1);
        }

        @Test
        @DisplayName("excluded files are not compared "
                + "for byte identity")
        void exclusions_documentedAndValid() {
            assertThat(EXCLUSIONS)
                    .as("Exclusion set must not be empty")
                    .isNotEmpty();

            assertThat(EXCLUSIONS)
                    .as("Exclusions must include "
                            + "project-identity")
                    .contains(
                            ".claude/rules/"
                                    + "01-project-identity"
                                    + ".md");

            assertThat(EXCLUSIONS)
                    .as("Exclusions must include "
                            + "domain rule")
                    .contains(
                            ".claude/rules/02-domain.md");
        }
    }

    @Nested
    @DisplayName("Statistical outlier detection")
    class StatisticalOutliers {

        @Test
        @DisplayName("no profile produces drastically "
                + "different total file counts")
        void totalFileCounts_noOutliers_allProfiles()
                throws IOException {
            Map<String, Long> countsByProfile =
                    new TreeMap<>();

            for (var entry
                    : profileOutputs.entrySet()) {
                long count = SmokeTestValidators
                        .countFiles(entry.getValue());
                countsByProfile.put(
                        entry.getKey(), count);
            }

            double mean = countsByProfile.values()
                    .stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0);

            double stdDev = calculateStdDev(
                    countsByProfile.values()
                            .stream()
                            .mapToLong(Long::longValue)
                            .toArray(),
                    mean);

            List<String> outliers = new ArrayList<>();
            double threshold = 3.0;

            for (var entry
                    : countsByProfile.entrySet()) {
                double zScore =
                        (entry.getValue() - mean) / stdDev;
                if (Math.abs(zScore) > threshold) {
                    outliers.add(
                            "  %s: %d files "
                                    + "(z-score=%.2f, "
                                    + "mean=%.0f, "
                                    + "stddev=%.1f)"
                                    .formatted(
                                            entry.getKey(),
                                            entry.getValue(),
                                            zScore,
                                            mean,
                                            stdDev));
                }
            }

            if (!outliers.isEmpty()) {
                fail("Statistical outlier profiles "
                        + "detected (|z-score| > %.1f):"
                        + "\n%s\nAll counts: %s"
                        .formatted(
                                threshold,
                                String.join("\n",
                                        outliers),
                                countsByProfile));
            }
        }

        @Test
        @DisplayName("no profile has a category count "
                + "deviating more than 50 percent "
                + "from the median")
        void categoryCounts_noExtremeDeviation()
                throws IOException {
            String manifestResource =
                    "smoke/expected-artifacts.json";
            var manifest =
                    ExpectedArtifacts.loadFromClasspath(
                            manifestResource);

            Set<String> allCategories = new TreeSet<>();
            for (String profile
                    : SmokeProfiles.profileList()) {
                allCategories.addAll(
                        manifest.getProfile(profile)
                                .categories().keySet());
            }

            allCategories.removeAll(VARYING_CATEGORIES);

            List<String> violations = new ArrayList<>();

            for (String category : allCategories) {
                List<Integer> counts = new ArrayList<>();
                Map<String, Integer> profileCounts =
                        new TreeMap<>();

                for (String profile
                        : SmokeProfiles.profileList()) {
                    int count = manifest
                            .getProfile(profile)
                            .getCategoryCount(category);
                    counts.add(count);
                    profileCounts.put(profile, count);
                }

                double median = calculateMedian(counts);
                if (median == 0) {
                    continue;
                }

                for (var entry
                        : profileCounts.entrySet()) {
                    double deviation = Math.abs(
                            entry.getValue() - median)
                            / median;
                    if (deviation > 0.5) {
                        violations.add(
                                ("  %s in %s: %d "
                                        + "(median=%.0f, "
                                        + "deviation="
                                        + "%.0f%%)")
                                        .formatted(
                                                category,
                                                entry.getKey(),
                                                entry.getValue(),
                                                median,
                                                deviation
                                                        * 100));
                    }
                }
            }

            if (!violations.isEmpty()) {
                fail("Category counts with >50%% "
                        + "deviation from median:\n%s"
                        .formatted(String.join(
                                "\n", violations)));
            }
        }
    }

    @Nested
    @DisplayName("Profile naming conventions")
    class NamingConventions {

        @Test
        @DisplayName("all profiles produce .claude "
                + "directory")
        void allProfiles_produceClaudeDir() {
            for (var entry
                    : profileOutputs.entrySet()) {
                assertThat(
                        entry.getValue()
                                .resolve(".claude"))
                        .as(".claude dir must exist for "
                                + "profile: %s",
                                entry.getKey())
                        .isDirectory();
            }
        }

        @Test
        @DisplayName("all profiles produce .github "
                + "directory")
        void allProfiles_produceGithubDir() {
            for (var entry
                    : profileOutputs.entrySet()) {
                assertThat(
                        entry.getValue()
                                .resolve(".github"))
                        .as(".github dir must exist for "
                                + "profile: %s",
                                entry.getKey())
                        .isDirectory();
            }
        }

        @Test
        @DisplayName("all profiles produce AGENTS.md "
                + "at root")
        void allProfiles_produceAgentsMd() {
            for (var entry
                    : profileOutputs.entrySet()) {
                assertThat(
                        entry.getValue()
                                .resolve("AGENTS.md"))
                        .as("AGENTS.md must exist for "
                                + "profile: %s",
                                entry.getKey())
                        .isRegularFile();
            }
        }

        @Test
        @DisplayName("skill directories follow "
                + "kebab-case naming in all profiles")
        void skillDirs_kebabCase_allProfiles()
                throws IOException {
            List<String> violations = new ArrayList<>();

            for (var entry
                    : profileOutputs.entrySet()) {
                Path skillsDir = entry.getValue()
                        .resolve(".agents/skills");
                if (!Files.isDirectory(skillsDir)) {
                    continue;
                }
                try (Stream<Path> dirs =
                             Files.list(skillsDir)) {
                    dirs.filter(Files::isDirectory)
                            .forEach(dir -> {
                                String name =
                                        dir.getFileName()
                                                .toString();
                                if (!isKebabCase(name)) {
                                    violations.add(
                                            "  %s: %s"
                                                    .formatted(
                                                            entry.getKey(),
                                                            name));
                                }
                            });
                }
            }

            if (!violations.isEmpty()) {
                fail("Skill dirs not in kebab-case:\n%s"
                        .formatted(String.join(
                                "\n", violations)));
            }
        }
    }

    // -- assertion helpers --

    private static void assertSameFileNames(
            String relativeDir) throws IOException {
        Map<String, Set<String>> filesByProfile =
                new TreeMap<>();

        for (var entry : profileOutputs.entrySet()) {
            Path dir = entry.getValue()
                    .resolve(relativeDir);
            Set<String> fileNames = new TreeSet<>();
            if (Files.isDirectory(dir)) {
                try (Stream<Path> list =
                             Files.list(dir)) {
                    list.filter(Files::isRegularFile)
                            .forEach(f -> fileNames.add(
                                    f.getFileName()
                                            .toString()));
                }
            }
            filesByProfile.put(
                    entry.getKey(), fileNames);
        }

        Set<Set<String>> uniqueSets =
                new TreeSet<>(
                        (a, b) -> a.equals(b)
                                ? 0
                                : a.toString()
                                .compareTo(
                                        b.toString()));
        uniqueSets.addAll(filesByProfile.values());

        if (uniqueSets.size() > 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("File names in '%s' differ "
                    .formatted(relativeDir));
            sb.append("across profiles:\n");
            for (var entry
                    : filesByProfile.entrySet()) {
                sb.append("  %s: %s\n"
                        .formatted(
                                entry.getKey(),
                                entry.getValue()));
            }
            fail(sb.toString());
        }
    }

    private static void assertCommonJsonKeysExist(
            String relativeFile,
            Set<String> requiredKeys)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<String> violations = new ArrayList<>();

        for (var entry : profileOutputs.entrySet()) {
            Path file = entry.getValue()
                    .resolve(relativeFile);
            if (!Files.isRegularFile(file)) {
                violations.add(
                        "  %s: file not found"
                                .formatted(
                                        entry.getKey()));
                continue;
            }
            String content = Files.readString(
                    file, StandardCharsets.UTF_8);
            JsonNode root = mapper.readTree(content);
            for (String key : requiredKeys) {
                if (!root.has(key)) {
                    violations.add(
                            "  %s: missing key '%s'"
                                    .formatted(
                                            entry.getKey(),
                                            key));
                }
            }
        }

        if (!violations.isEmpty()) {
            fail("Missing required keys in '%s':\n%s"
                    .formatted(relativeFile,
                            String.join("\n",
                                    violations)));
        }
    }

    private static void assertSubsetFileNames(
            String relativeDir) throws IOException {
        Map<String, Set<String>> filesByProfile =
                new TreeMap<>();

        for (var entry : profileOutputs.entrySet()) {
            Path dir = entry.getValue()
                    .resolve(relativeDir);
            Set<String> fileNames = new TreeSet<>();
            if (Files.isDirectory(dir)) {
                try (Stream<Path> list =
                             Files.list(dir)) {
                    list.filter(Files::isRegularFile)
                            .forEach(f -> fileNames.add(
                                    f.getFileName()
                                            .toString()));
                }
            }
            filesByProfile.put(
                    entry.getKey(), fileNames);
        }

        Set<String> commonFiles = null;
        for (Set<String> files
                : filesByProfile.values()) {
            if (commonFiles == null) {
                commonFiles = new TreeSet<>(files);
            } else {
                commonFiles.retainAll(files);
            }
        }

        assertThat(commonFiles)
                .as("At least some files in '%s' must "
                        + "be common across all profiles",
                        relativeDir)
                .isNotNull()
                .isNotEmpty();
    }

    private static String formatCountViolation(
            String category,
            Map<String, Integer> countsByProfile) {
        StringBuilder sb = new StringBuilder();
        sb.append("  %s:".formatted(category));
        for (var entry : countsByProfile.entrySet()) {
            sb.append(" %s=%d"
                    .formatted(
                            entry.getKey(),
                            entry.getValue()));
        }
        return sb.toString();
    }

    // -- utility methods --

    private static Path runPipelineForProfile(
            String profile, Path outputDir) {
        var config = dev.iadev.config.ConfigProfiles
                .getStack(profile);
        var pipeline =
                new dev.iadev.application.assembler.AssemblerPipeline(
                        dev.iadev.application.assembler
                                .AssemblerPipeline
                                .buildAssemblers());
        var options =
                new dev.iadev.application.assembler.PipelineOptions(
                        false, true, false, null);

        var result = pipeline.runPipeline(
                config, outputDir, options);
        assertThat(result.success())
                .as("Pipeline must succeed for profile: "
                        + "%s", profile)
                .isTrue();

        return outputDir;
    }

    private static double calculateStdDev(
            long[] values, double mean) {
        double sumSquares = 0;
        for (long value : values) {
            double diff = value - mean;
            sumSquares += diff * diff;
        }
        return Math.sqrt(sumSquares / values.length);
    }

    private static double calculateMedian(
            List<Integer> values) {
        List<Integer> sorted =
                values.stream().sorted().toList();
        int size = sorted.size();
        if (size == 0) {
            return 0;
        }
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1)
                    + sorted.get(size / 2)) / 2.0;
        }
        return sorted.get(size / 2);
    }

    private static boolean isKebabCase(String name) {
        return name.matches(
                "^[a-z][a-z0-9]*(-[a-z0-9]+)*$");
    }
}
