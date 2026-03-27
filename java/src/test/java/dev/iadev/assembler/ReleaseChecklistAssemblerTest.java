package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ReleaseChecklistAssembler -- generates
 * docs/templates/_TEMPLATE-RELEASE-CHECKLIST.md from a
 * Pebble template with conditional sections for container,
 * native build, and contract tests.
 */
@DisplayName("ReleaseChecklistAssembler")
class ReleaseChecklistAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssembler() {
            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble -- generates release checklist")
    class AssembleChecklist {

        @Test
        @DisplayName("generates release checklist in"
                + " docs/templates/ subdirectory")
        void assemble_whenCalled_generatesChecklistFile(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            Path expected = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            assertThat(expected).exists();
        }

        @Test
        @DisplayName("creates docs/templates/ subdirectory")
        void assemble_whenCalled_createsTemplatesSubdir(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(
                    outputDir.resolve("docs/templates"))
                    .exists()
                    .isDirectory();
        }

        @Test
        @DisplayName("returns file path in result list")
        void assemble_whenCalled_returnsFilePath(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            assertThat(files.get(0)).endsWith(
                    "_TEMPLATE-RELEASE-CHECKLIST.md");
        }

        @Test
        @DisplayName("resolves project_name variable")
        void assemble_whenCalled_resolvesProjectName(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("api-pagamentos")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("api-pagamentos");
        }

        @Test
        @DisplayName("resolves build_tool variable")
        void assemble_whenCalled_resolvesBuildTool(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .buildTool("gradle")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content).contains("gradle");
        }
    }

    @Nested
    @DisplayName("assemble -- mandatory sections present")
    class MandatorySections {

        @Test
        @DisplayName("contains Pre-Release Validation"
                + " section")
        void assemble_whenCalled_containsPreReleaseValidation(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("Pre-Release Validation");
        }

        @Test
        @DisplayName("contains Version & Changelog section")
        void assemble_whenCalled_containsVersionChangelog(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("Version & Changelog");
        }

        @Test
        @DisplayName("contains Artifact Build section")
        void assemble_whenCalled_containsArtifactBuild(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("Artifact Build");
        }

        @Test
        @DisplayName("contains Quality Gate section")
        void assemble_whenCalled_containsQualityGate(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("Quality Gate");
        }

        @Test
        @DisplayName("contains Publish section")
        void assemble_whenCalled_containsPublish(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content).contains("Publish");
        }

        @Test
        @DisplayName("contains Post-Release section")
        void assemble_whenCalled_containsPostRelease(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("Post-Release");
        }

        @Test
        @DisplayName("contains all 6 mandatory sections")
        void assemble_whenCalled_containsAllMandatorySections(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            for (String section
                    : ReleaseChecklistAssembler
                    .MANDATORY_SECTIONS) {
                assertThat(content)
                        .as("Missing section: %s", section)
                        .contains(section);
            }
        }
    }

    @Nested
    @DisplayName("assemble -- CLI without container"
            + " (degenerate case)")
    class CliWithoutContainer {

        @Test
        @DisplayName("excludes container image items when"
                + " container=none")
        void assemble_noContainer_excludesContainerItems(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .contractTests(false)
                            .nativeBuild(false)
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content)
                    .doesNotContain(
                            "Container image built");
            assertThat(content)
                    .doesNotContain(
                            "Container image pushed");
        }

        @Test
        @DisplayName("excludes native image items when"
                + " native_build=false")
        void assemble_noNative_excludesNativeItems(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .contractTests(false)
                            .nativeBuild(false)
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content)
                    .doesNotContain(
                            "Native image built");
        }

        @Test
        @DisplayName("excludes contract test items when"
                + " contract_tests=false")
        void assemble_noContract_excludesContractItems(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .contractTests(false)
                            .nativeBuild(false)
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content)
                    .doesNotContain(
                            "Contract tests pass");
        }

        @Test
        @DisplayName("still contains mandatory sections"
                + " for CLI config")
        void assemble_cliConfig_containsMandatorySections(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .contractTests(false)
                            .nativeBuild(false)
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("Pre-Release Validation");
            assertThat(content)
                    .contains("Artifact Build");
            assertThat(content)
                    .contains("Quality Gate");
        }
    }

    @Nested
    @DisplayName("assemble -- microservice with container")
    class MicroserviceWithContainer {

        @Test
        @DisplayName("includes container image built item"
                + " when container=docker")
        void assemble_docker_includesContainerBuiltItem(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .contractTests(true)
                            .nativeBuild(false)
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content).contains(
                    "Container image built and tagged"
                            + " with version");
        }

        @Test
        @DisplayName("includes container image latest tag"
                + " when container=docker")
        void assemble_docker_includesContainerLatestTag(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content).contains(
                    "Container image tagged with `latest`");
        }

        @Test
        @DisplayName("includes container push item when"
                + " container=docker")
        void assemble_docker_includesContainerPush(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content).contains(
                    "Container image pushed to registry");
        }

        @Test
        @DisplayName("includes contract tests item when"
                + " contract_tests=true")
        void assemble_contractEnabled_includesContractItem(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .contractTests(true)
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content)
                    .contains("Contract tests pass");
        }
    }

    @Nested
    @DisplayName("assemble -- native build enabled")
    class NativeBuildEnabled {

        @Test
        @DisplayName("includes native image item when"
                + " native_build=true")
        void assemble_nativeEnabled_includesNativeItem(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .nativeBuild(true)
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content).contains(
                    "Native image built and smoke-tested");
        }

        @Test
        @DisplayName("excludes native image item when"
                + " native_build=false")
        void assemble_nativeDisabled_excludesNativeItem(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .nativeBuild(false)
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content)
                    .doesNotContain(
                            "Native image built");
        }
    }

    @Nested
    @DisplayName("assemble -- coverage thresholds")
    class CoverageThresholds {

        @Test
        @DisplayName("renders coverage_line threshold")
        void assemble_whenCalled_rendersCoverageLineThreshold(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content).contains("95%");
        }

        @Test
        @DisplayName("renders coverage_branch threshold")
        void assemble_whenCalled_rendersCoverageBranchThreshold(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content).contains("90%");
        }
    }

    @Nested
    @DisplayName("assemble -- no Pebble artifacts in output")
    class NoPebbleArtifacts {

        @Test
        @DisplayName("output does not contain Pebble tags")
        void assemble_whenCalled_noPebbleTagsInOutput(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content).doesNotContain("{%");
            assertThat(content).doesNotContain("{{");
        }
    }

    @Nested
    @DisplayName("assemble -- graceful no-op")
    class GracefulNoOp {

        @Test
        @DisplayName("returns empty list when template"
                + " file absent")
        void assemble_whenCalled_returnsEmptyWhenAbsent(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler(
                            resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("does not create output directory"
                + " when template absent")
        void assemble_whenCalled_doesNotCreateOutputDir(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler(
                            resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(outputDir).doesNotExist();
        }
    }

    @Nested
    @DisplayName("assemble -- profile comparisons")
    class ProfileComparisons {

        @Test
        @DisplayName("python-click-cli excludes all"
                + " conditional sections")
        void assemble_pythonCli_excludesAllConditionals(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("click", "8.1")
                            .buildTool("pip")
                            .container("none")
                            .contractTests(false)
                            .nativeBuild(false)
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content)
                    .doesNotContain("Container image");
            assertThat(content)
                    .doesNotContain("Native image");
            assertThat(content)
                    .doesNotContain("Contract tests");
        }

        @Test
        @DisplayName("java-spring includes container and"
                + " contract sections")
        void assemble_javaSpring_includesContainerAndContract(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("spring-boot", "3.4")
                            .buildTool("maven")
                            .container("docker")
                            .contractTests(true)
                            .nativeBuild(false)
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content).contains(
                    "Container image built");
            assertThat(content).contains(
                    "Container image pushed");
            assertThat(content).contains(
                    "Contract tests pass");
            assertThat(content)
                    .doesNotContain("Native image");
        }

        @Test
        @DisplayName("java-quarkus includes container,"
                + " native, and contract sections")
        void assemble_javaQuarkus_includesAllConditionals(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            ReleaseChecklistAssembler assembler =
                    new ReleaseChecklistAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .container("docker")
                            .contractTests(true)
                            .nativeBuild(true)
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            Path file = outputDir.resolve(
                    "docs/templates/"
                            + "_TEMPLATE-RELEASE-CHECKLIST.md");
            String content = readFile(file);
            assertThat(content).contains(
                    "Container image built");
            assertThat(content).contains(
                    "Native image built and smoke-tested");
            assertThat(content).contains(
                    "Contract tests pass");
        }
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(
                    path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read: " + path, e);
        }
    }
}
