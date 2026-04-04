package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
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
 * Additional coverage tests for SkillsAssembler —
 * targeting uncovered branches and edge cases.
 */
@DisplayName("SkillsAssembler — coverage")
class SkillsAssemblerCoverageTest {

    @Nested
    @DisplayName("selectCoreSkills — edge cases")
    class SelectCoreSkills {

        @Test
        @DisplayName("core dir missing returns empty")
        void assemble_whenCalled_coreDirMissing(@TempDir Path tempDir) {
            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);

            List<String> skills =
                    assembler.selectCoreSkills();

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("core dir is a file returns empty")
        void assemble_coreDir_isFile(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(
                    tempDir.resolve("skills-templates"));
            Files.writeString(
                    tempDir.resolve(
                            "skills-templates/core"),
                    "not a dir");

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);

            List<String> skills =
                    assembler.selectCoreSkills();

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("core dir with lib subdirectory"
                + " prefixes with lib/")
        void assemble_whenCalled_libSubdirPrefixed(@TempDir Path tempDir)
                throws IOException {
            Path core = tempDir.resolve(
                    "skills-templates/core");
            Files.createDirectories(
                    core.resolve("lib/x-lib-tool"));
            Files.createDirectories(
                    core.resolve("x-review"));

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);

            List<String> skills =
                    assembler.selectCoreSkills();

            assertThat(skills)
                    .contains("lib/x-lib-tool")
                    .contains("x-review");
        }
    }

    @Nested
    @DisplayName("assembleConditional — edge cases")
    class ConditionalEdgeCases {

        @Test
        @DisplayName("conditional skill dir missing"
                + " returns null (filtered out)")
        void assembleConditional_whenCalled_conditionalDirMissing(@TempDir Path tempDir)
                throws IOException {
            Path core = tempDir.resolve(
                    "skills-templates/core");
            Files.createDirectories(core);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("assembleKnowledge — edge cases")
    class KnowledgeEdgeCases {

        @Test
        @DisplayName("knowledge pack dir missing"
                + " returns null (filtered out)")
        void assembleKnowledge_whenCalled_kpDirMissing(@TempDir Path tempDir)
                throws IOException {
            Path core = tempDir.resolve(
                    "skills-templates/core");
            Files.createDirectories(core);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("knowledge pack with SKILL.md"
                + " renders template")
        void assembleKnowledge_whenCalled_kpWithSkillMd(@TempDir Path tempDir)
                throws IOException {
            Path kpDir = tempDir.resolve(
                    "skills-templates/knowledge-packs"
                            + "/architecture");
            Files.createDirectories(kpDir);
            Files.writeString(
                    kpDir.resolve("SKILL.md"),
                    "name: architecture\n"
                            + "user-invocable: false\n",
                    StandardCharsets.UTF_8);
            Path refsDir = kpDir.resolve("references");
            Files.createDirectories(refsDir);
            Files.writeString(
                    refsDir.resolve("arch.md"),
                    "Arch content",
                    StandardCharsets.UTF_8);

            Path core = tempDir.resolve(
                    "skills-templates/core");
            Files.createDirectories(core);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
            assertThat(files).anyMatch(
                    f -> f.contains("architecture"));
        }

        @Test
        @DisplayName("knowledge pack without SKILL.md"
                + " copies non-skill items")
        void assembleKnowledge_whenCalled_kpWithoutSkillMd(@TempDir Path tempDir)
                throws IOException {
            Path kpDir = tempDir.resolve(
                    "skills-templates/knowledge-packs"
                            + "/testing");
            Files.createDirectories(kpDir);
            Files.writeString(
                    kpDir.resolve("some-file.md"),
                    "test content",
                    StandardCharsets.UTF_8);

            Path core = tempDir.resolve(
                    "skills-templates/core");
            Files.createDirectories(core);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("copyStackPatterns — edge cases")
    class StackPatterns {

        @Test
        @DisplayName("unknown framework returns no"
                + " stack patterns")
        void copyStackPatterns_whenCalled_unknownFrameworkNoPatterns(
                @TempDir Path tempDir) throws IOException {
            Path core = tempDir.resolve(
                    "skills-templates/core");
            Files.createDirectories(core);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("unknown", "1.0")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("stack pattern dir missing returns"
                + " null")
        void copyStackPatterns_whenCalled_stackPatternDirMissing(
                @TempDir Path tempDir) throws IOException {
            Path core = tempDir.resolve(
                    "skills-templates/core");
            Files.createDirectories(core);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .framework("quarkus", "3.17")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("copyInfraPatterns — edge cases")
    class InfraPatterns {

        @Test
        @DisplayName("infra pattern not included is"
                + " skipped")
        void copyInfraPatterns_whenCalled_infraNotIncludedSkipped(
                @TempDir Path tempDir) throws IOException {
            Path core = tempDir.resolve(
                    "skills-templates/core");
            Files.createDirectories(core);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("infra pattern dir missing is"
                + " skipped")
        void copyInfraPatterns_whenCalled_infraDirMissingSkipped(
                @TempDir Path tempDir) throws IOException {
            Path core = tempDir.resolve(
                    "skills-templates/core");
            Files.createDirectories(core);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("docker")
                            .orchestrator("kubernetes")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("default constructor")
    class DefaultConstructor {

        @Test
        @DisplayName("default constructor resolves"
                + " classpath resources")
        void constructor_withDefaults_resolvesCorrectly() {
            SkillsAssembler assembler =
                    new SkillsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }
}
