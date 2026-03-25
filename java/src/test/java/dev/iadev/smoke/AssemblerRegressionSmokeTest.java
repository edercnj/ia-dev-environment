package dev.iadev.smoke;

import dev.iadev.assembler.AssemblerDescriptor;
import dev.iadev.assembler.AssemblerPipeline;
import dev.iadev.assembler.AssemblerResult;
import dev.iadev.assembler.AssemblerTarget;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.model.PipelineResult;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test validating that all 25 assemblers execute
 * and contribute output for every registered profile.
 *
 * <p>Detects assembler regressions:</p>
 * <ul>
 *   <li>Assembler added/removed without test update</li>
 *   <li>Assembler silently skips (zero output)</li>
 *   <li>Assembler order changed from RULE-005</li>
 *   <li>Conditional assembler misbehaves</li>
 *   <li>Output pattern violations (extensions, naming)</li>
 * </ul>
 *
 * <p>RULE-001: Parametrized for all 8 profiles.</p>
 * <p>RULE-002: Independent of golden files.</p>
 * <p>RULE-006: Output in {@code @TempDir}.</p>
 *
 * @see SmokeTestBase
 * @see SmokeProfiles
 */
@DisplayName("AssemblerRegressionSmokeTest")
class AssemblerRegressionSmokeTest extends SmokeTestBase {

    static final int EXPECTED_ASSEMBLER_COUNT = 25;

    static final List<String> EXPECTED_ORDER = List.of(
            "RulesAssembler",
            "SkillsAssembler",
            "AgentsAssembler",
            "PatternsAssembler",
            "ProtocolsAssembler",
            "HooksAssembler",
            "SettingsAssembler",
            "GithubInstructionsAssembler",
            "GithubMcpAssembler",
            "GithubSkillsAssembler",
            "GithubAgentsAssembler",
            "GithubHooksAssembler",
            "GithubPromptsAssembler",
            "DocsAssembler",
            "GrpcDocsAssembler",
            "RunbookAssembler",
            "CodexAgentsMdAssembler",
            "CodexConfigAssembler",
            "CodexSkillsAssembler",
            "CodexRequirementsAssembler",
            "CodexOverrideAssembler",
            "DocsAdrAssembler",
            "CicdAssembler",
            "EpicReportAssembler",
            "ReadmeAssembler");

    /**
     * Profiles that include a grpc interface, activating
     * GrpcDocsAssembler.
     */
    static final Set<String> GRPC_PROFILES = Set.of(
            "java-spring",
            "java-quarkus",
            "go-gin",
            "rust-axum");

    /**
     * Profiles that have MCP servers configured, activating
     * GithubMcpAssembler.
     */
    static final Set<String> MCP_PROFILES = Set.of(
            "java-quarkus");

    /**
     * Output areas that every profile must produce files in.
     * Keyed by directory path relative to output root.
     */
    static final Map<String, String> MANDATORY_OUTPUT_AREAS =
            Map.ofEntries(
                    Map.entry(".claude/rules",
                            "RulesAssembler"),
                    Map.entry(".claude/skills",
                            "SkillsAssembler"),
                    Map.entry(".claude/agents",
                            "AgentsAssembler"),
                    Map.entry(".github/instructions",
                            "GithubInstructionsAssembler"),
                    Map.entry(".github/skills",
                            "GithubSkillsAssembler"),
                    Map.entry(".github/agents",
                            "GithubAgentsAssembler"),
                    Map.entry(".github/hooks",
                            "GithubHooksAssembler"),
                    Map.entry(".github/prompts",
                            "GithubPromptsAssembler"),
                    Map.entry(".codex",
                            "CodexConfigAssembler"),
                    Map.entry(".agents/skills",
                            "CodexSkillsAssembler"),
                    Map.entry("docs",
                            "DocsAssembler"));

    static Stream<String> profiles() {
        return SmokeProfiles.profiles();
    }

    @Nested
    @DisplayName("Assembler registration")
    class AssemblerRegistration {

        @Test
        @DisplayName("factory returns exactly 25 "
                + "assemblers")
        void buildAssemblers_returnsExactCount() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            assertThat(descriptors)
                    .as("Pipeline must have exactly %d "
                            + "assemblers per RULE-005",
                            EXPECTED_ASSEMBLER_COUNT)
                    .hasSize(EXPECTED_ASSEMBLER_COUNT);
        }

        @Test
        @DisplayName("assembler order matches RULE-005")
        void buildAssemblers_orderMatchesRule005() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            List<String> actualNames = descriptors.stream()
                    .map(AssemblerDescriptor::name)
                    .toList();

            assertThat(actualNames)
                    .as("Assembler order must match "
                            + "RULE-005 specification")
                    .isEqualTo(EXPECTED_ORDER);
        }

        @Test
        @DisplayName("detects unexpected assembler if "
                + "added")
        void buildAssemblers_detectsNewAssembler() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            List<String> actualNames = descriptors.stream()
                    .map(AssemblerDescriptor::name)
                    .toList();

            List<String> unexpected = actualNames.stream()
                    .filter(name ->
                            !EXPECTED_ORDER.contains(name))
                    .toList();

            assertThat(unexpected)
                    .as("Unexpected assemblers found. "
                            + "Update EXPECTED_ORDER if "
                            + "intentional: %s", unexpected)
                    .isEmpty();
        }

        @Test
        @DisplayName("detects removed assembler if "
                + "missing")
        void buildAssemblers_detectsRemovedAssembler() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            List<String> actualNames = descriptors.stream()
                    .map(AssemblerDescriptor::name)
                    .toList();

            List<String> missing = EXPECTED_ORDER.stream()
                    .filter(name ->
                            !actualNames.contains(name))
                    .toList();

            assertThat(missing)
                    .as("Missing assemblers detected. "
                            + "Update EXPECTED_ORDER if "
                            + "intentional: %s", missing)
                    .isEmpty();
        }

        @Test
        @DisplayName("each assembler has non-null "
                + "implementation")
        void buildAssemblers_allNonNull() {
            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            for (AssemblerDescriptor desc : descriptors) {
                assertThat(desc.assembler())
                        .as("Assembler for %s must not "
                                + "be null", desc.name())
                        .isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("Per-assembler contribution")
    class PerAssemblerContribution {

        @ParameterizedTest
        @MethodSource("dev.iadev.smoke."
                + "AssemblerRegressionSmokeTest#profiles")
        @DisplayName("mandatory output areas have files")
        void pipeline_mandatoryAreasHaveFiles_forProfile(
                String profile) throws IOException {
            runPipeline(profile);
            Path outputDir = getOutputDir(profile);

            List<String> emptyAreas = new ArrayList<>();

            for (var entry
                    : MANDATORY_OUTPUT_AREAS.entrySet()) {
                String area = entry.getKey();
                String assembler = entry.getValue();
                Path areaPath = outputDir.resolve(area);

                if (!Files.isDirectory(areaPath)
                        || countFiles(areaPath) == 0) {
                    emptyAreas.add("%s (from %s)"
                            .formatted(area, assembler));
                }
            }

            assertThat(emptyAreas)
                    .as("Output areas with zero files "
                            + "for profile %s", profile)
                    .isEmpty();
        }

        @ParameterizedTest
        @MethodSource("dev.iadev.smoke."
                + "AssemblerRegressionSmokeTest#profiles")
        @DisplayName("pipeline produces non-zero total "
                + "file count")
        void pipeline_producesFiles_forProfile(
                String profile) {
            PipelineResult result = runPipeline(profile);

            assertThat(result.filesGenerated())
                    .as("Pipeline must produce files for "
                            + "profile %s", profile)
                    .isNotEmpty();
        }

        @ParameterizedTest
        @MethodSource("dev.iadev.smoke."
                + "AssemblerRegressionSmokeTest#profiles")
        @DisplayName("no assembler breaks other "
                + "assemblers' output areas")
        void pipeline_allAreasCoexist_forProfile(
                String profile) throws IOException {
            runPipeline(profile);
            Path outputDir = getOutputDir(profile);

            Map<String, Long> areaCounts =
                    new LinkedHashMap<>();
            for (String area
                    : MANDATORY_OUTPUT_AREAS.keySet()) {
                Path areaPath = outputDir.resolve(area);
                long count = Files.isDirectory(areaPath)
                        ? countFiles(areaPath) : 0;
                areaCounts.put(area, count);
            }

            long areasWithFiles = areaCounts.values()
                    .stream()
                    .filter(c -> c > 0)
                    .count();

            assertThat(areasWithFiles)
                    .as("All %d mandatory areas must "
                            + "have files for %s. "
                            + "Counts: %s",
                            MANDATORY_OUTPUT_AREAS.size(),
                            profile, areaCounts)
                    .isEqualTo(
                            MANDATORY_OUTPUT_AREAS.size());
        }
    }

    @Nested
    @DisplayName("Conditional assemblers")
    class ConditionalAssemblers {

        @ParameterizedTest
        @MethodSource("dev.iadev.smoke."
                + "AssemblerRegressionSmokeTest#profiles")
        @DisplayName("GrpcDocsAssembler produces output "
                + "only for grpc profiles")
        void grpcAssembler_respectsCondition_forProfile(
                String profile) {
            ProjectConfig config =
                    ConfigProfiles.getStack(profile);
            TemplateEngine engine = new TemplateEngine();

            int grpcFileCount =
                    executeSingleAssembler(
                            "GrpcDocsAssembler", config,
                            engine);

            if (GRPC_PROFILES.contains(profile)) {
                assertThat(grpcFileCount)
                        .as("GrpcDocsAssembler should "
                                + "produce output for grpc "
                                + "profile: %s", profile)
                        .isGreaterThan(0);
            } else {
                assertThat(grpcFileCount)
                        .as("GrpcDocsAssembler should "
                                + "produce zero output for "
                                + "non-grpc profile: %s",
                                profile)
                        .isZero();
            }
        }

        @ParameterizedTest
        @MethodSource("dev.iadev.smoke."
                + "AssemblerRegressionSmokeTest#profiles")
        @DisplayName("GithubMcpAssembler produces output "
                + "only for MCP profiles")
        void mcpAssembler_respectsCondition_forProfile(
                String profile) {
            ProjectConfig config =
                    ConfigProfiles.getStack(profile);
            TemplateEngine engine = new TemplateEngine();

            int mcpFileCount =
                    executeSingleAssembler(
                            "GithubMcpAssembler", config,
                            engine);

            if (MCP_PROFILES.contains(profile)) {
                assertThat(mcpFileCount)
                        .as("GithubMcpAssembler should "
                                + "produce output for MCP "
                                + "profile: %s", profile)
                        .isGreaterThan(0);
            } else {
                assertThat(mcpFileCount)
                        .as("GithubMcpAssembler should "
                                + "produce zero output for "
                                + "non-MCP profile: %s",
                                profile)
                        .isZero();
            }
        }

        @ParameterizedTest
        @MethodSource("dev.iadev.smoke."
                + "AssemblerRegressionSmokeTest#profiles")
        @DisplayName("conditional assembler skip is "
                + "graceful")
        void conditionalSkip_isGraceful_forProfile(
                String profile) {
            ProjectConfig config =
                    ConfigProfiles.getStack(profile);
            TemplateEngine engine = new TemplateEngine();

            List<String> conditionalNames = List.of(
                    "GrpcDocsAssembler",
                    "GithubMcpAssembler");

            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            for (AssemblerDescriptor desc : descriptors) {
                if (!conditionalNames.contains(
                        desc.name())) {
                    continue;
                }
                Path asmDir = createAssemblerTempDir(
                        desc.name() + "-graceful-"
                                + profile);
                Path targetDir =
                        desc.target().resolve(asmDir);
                createDirectorySilently(targetDir);

                AssemblerResult result =
                        desc.assembler()
                                .assembleWithResult(
                                        config, engine,
                                        targetDir);

                assertThat(result)
                        .as("Result for %s on %s must "
                                + "not be null",
                                desc.name(), profile)
                        .isNotNull();
                assertThat(result.files())
                        .as("Files list for %s on %s "
                                + "must not be null",
                                desc.name(), profile)
                        .isNotNull();
            }
        }

        @ParameterizedTest
        @MethodSource("dev.iadev.smoke."
                + "AssemblerRegressionSmokeTest#profiles")
        @DisplayName("grpc output area exists only for "
                + "grpc profiles after full pipeline")
        void grpcOutput_matchesProfile_afterPipeline(
                String profile) throws IOException {
            runPipeline(profile);
            Path outputDir = getOutputDir(profile);
            Path grpcDocs = outputDir.resolve(
                    "docs/api/grpc-reference.md");

            if (GRPC_PROFILES.contains(profile)) {
                assertThat(grpcDocs)
                        .as("grpc-reference.md must exist"
                                + " for grpc profile: %s",
                                profile)
                        .exists();
            } else {
                assertThat(grpcDocs)
                        .as("grpc-reference.md must not "
                                + "exist for non-grpc "
                                + "profile: %s", profile)
                        .doesNotExist();
            }
        }
    }

    @Nested
    @DisplayName("Output patterns")
    class OutputPatterns {

        @ParameterizedTest
        @MethodSource("dev.iadev.smoke."
                + "AssemblerRegressionSmokeTest#profiles")
        @DisplayName("all generated files have recognized"
                + " extensions")
        void pipeline_filesHaveValidExtensions_forProfile(
                String profile) throws IOException {
            runPipeline(profile);
            Path outputDir = getOutputDir(profile);

            Set<String> validExtensions = Set.of(
                    ".md", ".json", ".yaml", ".yml",
                    ".sh", ".toml");
            Set<String> extensionlessFiles = Set.of(
                    "Dockerfile", "docker-compose.yaml");
            List<String> violations = new ArrayList<>();

            Files.walkFileTree(outputDir,
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(
                                Path file,
                                BasicFileAttributes attrs) {
                            String name = file.getFileName()
                                    .toString();
                            boolean valid = validExtensions
                                    .stream()
                                    .anyMatch(name::endsWith)
                                    || extensionlessFiles
                                    .contains(name);
                            if (!valid) {
                                String rel = outputDir
                                        .relativize(file)
                                        .toString()
                                        .replace('\\', '/');
                                violations.add(rel);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });

            assertThat(violations)
                    .as("Files with unrecognized "
                            + "extensions for %s", profile)
                    .isEmpty();
        }

        @ParameterizedTest
        @MethodSource("dev.iadev.smoke."
                + "AssemblerRegressionSmokeTest#profiles")
        @DisplayName("assembler targets are consistent "
                + "across builds")
        void assemblerTargets_areConsistent_forProfile(
                String profile) {
            List<AssemblerDescriptor> descriptors =
                    AssemblerPipeline.buildAssemblers();

            Map<String, AssemblerTarget> nameToTarget =
                    new LinkedHashMap<>();
            for (AssemblerDescriptor desc : descriptors) {
                nameToTarget.put(
                        desc.name(), desc.target());
            }

            assertThat(nameToTarget.get("RulesAssembler"))
                    .isEqualTo(AssemblerTarget.CLAUDE);
            assertThat(nameToTarget.get("SkillsAssembler"))
                    .isEqualTo(AssemblerTarget.CLAUDE);
            assertThat(nameToTarget.get("AgentsAssembler"))
                    .isEqualTo(AssemblerTarget.CLAUDE);
            assertThat(nameToTarget.get(
                    "GithubInstructionsAssembler"))
                    .isEqualTo(AssemblerTarget.GITHUB);
            assertThat(nameToTarget.get(
                    "GithubSkillsAssembler"))
                    .isEqualTo(AssemblerTarget.GITHUB);
            assertThat(nameToTarget.get("DocsAssembler"))
                    .isEqualTo(AssemblerTarget.DOCS);
            assertThat(nameToTarget.get(
                    "CodexConfigAssembler"))
                    .isEqualTo(AssemblerTarget.CODEX);
            assertThat(nameToTarget.get(
                    "CodexSkillsAssembler"))
                    .isEqualTo(AssemblerTarget.CODEX_AGENTS);
            assertThat(nameToTarget.get(
                    "ReadmeAssembler"))
                    .isEqualTo(AssemblerTarget.CLAUDE);
            assertThat(nameToTarget.get(
                    "CicdAssembler"))
                    .isEqualTo(AssemblerTarget.ROOT);
        }
    }

    // --- Helper methods ---

    private int executeSingleAssembler(
            String assemblerName,
            ProjectConfig config,
            TemplateEngine engine) {
        List<AssemblerDescriptor> descriptors =
                AssemblerPipeline.buildAssemblers();

        for (AssemblerDescriptor desc : descriptors) {
            if (desc.name().equals(assemblerName)) {
                Path asmDir = createAssemblerTempDir(
                        assemblerName + "-single");
                Path targetDir =
                        desc.target().resolve(asmDir);
                createDirectorySilently(targetDir);

                AssemblerResult result =
                        desc.assembler()
                                .assembleWithResult(
                                        config, engine,
                                        targetDir);
                return result.files().size();
            }
        }
        return 0;
    }

    private Path createAssemblerTempDir(String name) {
        Path dir = tempDir.resolve("asm-" + name);
        createDirectorySilently(dir);
        return dir;
    }

    private static void createDirectorySilently(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(
                    "Failed to create directory: " + dir,
                    e);
        }
    }

    private static long countFiles(Path dir)
            throws IOException {
        long[] count = {0};
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attrs) {
                count[0]++;
                return FileVisitResult.CONTINUE;
            }
        });
        return count[0];
    }
}
