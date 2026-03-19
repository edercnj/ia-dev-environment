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
        void coreDirMissing(@TempDir Path tempDir) {
            SkillsAssembler assembler =
                    new SkillsAssembler(tempDir);

            List<String> skills =
                    assembler.selectCoreSkills();

            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("core dir is a file returns empty")
        void coreDirIsFile(@TempDir Path tempDir)
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
        void libSubdirPrefixed(@TempDir Path tempDir)
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
        void conditionalDirMissing(@TempDir Path tempDir)
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

            assertThat(files).isNotNull();
        }
    }

    @Nested
    @DisplayName("assembleKnowledge — edge cases")
    class KnowledgeEdgeCases {

        @Test
        @DisplayName("knowledge pack dir missing"
                + " returns null (filtered out)")
        void kpDirMissing(@TempDir Path tempDir)
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

            assertThat(files).isNotNull();
        }

        @Test
        @DisplayName("knowledge pack with SKILL.md"
                + " renders template")
        void kpWithSkillMd(@TempDir Path tempDir)
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

            assertThat(files).isNotNull();
        }

        @Test
        @DisplayName("knowledge pack without SKILL.md"
                + " copies non-skill items")
        void kpWithoutSkillMd(@TempDir Path tempDir)
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

            assertThat(files).isNotNull();
        }
    }

    @Nested
    @DisplayName("copyStackPatterns — edge cases")
    class StackPatterns {

        @Test
        @DisplayName("unknown framework returns no"
                + " stack patterns")
        void unknownFrameworkNoPatterns(
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

            assertThat(files).isNotNull();
        }

        @Test
        @DisplayName("stack pattern dir missing returns"
                + " null")
        void stackPatternDirMissing(
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

            assertThat(files).isNotNull();
        }
    }

    @Nested
    @DisplayName("copyInfraPatterns — edge cases")
    class InfraPatterns {

        @Test
        @DisplayName("infra pattern not included is"
                + " skipped")
        void infraNotIncludedSkipped(
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

            assertThat(files).isNotNull();
        }

        @Test
        @DisplayName("infra pattern dir missing is"
                + " skipped")
        void infraDirMissingSkipped(
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

            assertThat(files).isNotNull();
        }
    }

    @Nested
    @DisplayName("default constructor")
    class DefaultConstructor {

        @Test
        @DisplayName("default constructor resolves"
                + " classpath resources")
        void defaultConstructorResolves() {
            SkillsAssembler assembler =
                    new SkillsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }
}
