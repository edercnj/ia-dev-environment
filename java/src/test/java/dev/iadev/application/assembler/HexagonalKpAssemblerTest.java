package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for hexagonal architecture KP assembly.
 *
 * <p>Covers Gherkin scenarios @GK-1 through @GK-6 from
 * story-0017-0002.</p>
 */
@Disabled("EPIC-0051 complete: SkillsAssembler no longer emits KP output under .claude/skills/{kp}/; replaced by KnowledgePackMigrationSmokeTest + KnowledgeAssemblerTest on the new .claude/knowledge/ layout. See ADR-0013.")
@DisplayName("Hexagonal KP Assembly")
class HexagonalKpAssemblerTest {

    @Nested
    @DisplayName("GK-1: config without architecture.style"
            + " does not generate KP hexagonal")
    class NoStyleDeclared {

        @Test
        @DisplayName("style microservice does not include"
                + " architecture-hexagonal in KP list")
        void select_noHexStyle_excludesHexKp() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("microservice")
                            .build();

            List<String> packs =
                    SkillsSelection.selectKnowledgePacks(
                            config);

            assertThat(packs)
                    .doesNotContain(
                            "architecture-hexagonal");
            assertThat(packs)
                    .contains("architecture");
        }
    }

    @Nested
    @DisplayName("GK-2: hexagonal style generates KP"
            + " with 4 sections")
    class HexagonalFourSections {

        @Test
        @DisplayName("hexagonal KP assembles with 4"
                + " mandatory sections")
        void assemble_hexagonal_hasFourSections(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("hexagonal")
                            .basePackage("com.example.myapp")
                            .build();

            SkillsAssembler assembler =
                    new SkillsAssembler();
            TemplateEngine engine = new TemplateEngine();
            assembler.assemble(config, engine, tempDir);

            Path kpFile = tempDir.resolve(
                    "skills/architecture-hexagonal/"
                            + "SKILL.md");
            assertThat(kpFile).exists();

            String content = Files.readString(
                    kpFile, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Canonical Package Structure")
                    .contains("Dependency Rules")
                    .contains("Port/Adapter")
                    .contains("ArchUnit");
        }

        @Test
        @DisplayName("hexagonal KP contains base package"
                + " in code examples")
        void assemble_hexagonal_containsBasePackage(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("hexagonal")
                            .basePackage("com.example.myapp")
                            .build();

            SkillsAssembler assembler =
                    new SkillsAssembler();
            TemplateEngine engine = new TemplateEngine();
            assembler.assemble(config, engine, tempDir);

            Path kpFile = tempDir.resolve(
                    "skills/architecture-hexagonal/"
                            + "SKILL.md");
            String content = Files.readString(
                    kpFile, StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("com.example.myapp");
            assertThat(content)
                    .doesNotContain("{{BASE_PACKAGE}}");
        }

        @Test
        @DisplayName("section 1 has package structure"
                + " diagram")
        void assemble_hexagonal_hasPackageStructure(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("hexagonal")
                            .basePackage("com.example.myapp")
                            .build();

            SkillsAssembler assembler =
                    new SkillsAssembler();
            TemplateEngine engine = new TemplateEngine();
            assembler.assemble(config, engine, tempDir);

            Path kpFile = tempDir.resolve(
                    "skills/architecture-hexagonal/"
                            + "SKILL.md");
            String content = Files.readString(
                    kpFile, StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("domain/")
                    .contains("adapter/")
                    .contains("application/")
                    .contains("config/");
        }

        @Test
        @DisplayName("section 2 has dependency violation"
                + " examples")
        void assemble_hexagonal_hasViolationExamples(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("hexagonal")
                            .basePackage("com.example.myapp")
                            .build();

            SkillsAssembler assembler =
                    new SkillsAssembler();
            TemplateEngine engine = new TemplateEngine();
            assembler.assemble(config, engine, tempDir);

            Path kpFile = tempDir.resolve(
                    "skills/architecture-hexagonal/"
                            + "SKILL.md");
            String content = Files.readString(
                    kpFile, StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("VIOLATION")
                    .contains("FORBIDDEN");
        }

        @Test
        @DisplayName("section 3 has compilable port"
                + " and adapter examples")
        void assemble_hexagonal_hasPortAdapterExamples(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("hexagonal")
                            .basePackage("com.example.myapp")
                            .build();

            SkillsAssembler assembler =
                    new SkillsAssembler();
            TemplateEngine engine = new TemplateEngine();
            assembler.assemble(config, engine, tempDir);

            Path kpFile = tempDir.resolve(
                    "skills/architecture-hexagonal/"
                            + "SKILL.md");
            String content = Files.readString(
                    kpFile, StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("ManageOrderUseCase")
                    .contains("OrderRepository")
                    .contains("ManageOrderService");
        }

        @Test
        @DisplayName("section 4 has 3+ ArchTest rules")
        void assemble_hexagonal_hasArchTestRules(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("hexagonal")
                            .basePackage("com.example.myapp")
                            .build();

            SkillsAssembler assembler =
                    new SkillsAssembler();
            TemplateEngine engine = new TemplateEngine();
            assembler.assemble(config, engine, tempDir);

            Path kpFile = tempDir.resolve(
                    "skills/architecture-hexagonal/"
                            + "SKILL.md");
            String content = Files.readString(
                    kpFile, StandardCharsets.UTF_8);

            long archTestCount = content.lines()
                    .filter(l -> l.contains("@ArchTest"))
                    .count();
            assertThat(archTestCount)
                    .as("Must have 3+ ArchTest rules")
                    .isGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("GK-6: layered style does not generate"
            + " hexagonal KP")
    class LayeredExcludesHexagonal {

        @Test
        @DisplayName("layered style does not produce"
                + " architecture-hexagonal directory")
        void assemble_layered_noHexKpGenerated(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("layered")
                            .build();

            SkillsAssembler assembler =
                    new SkillsAssembler();
            TemplateEngine engine = new TemplateEngine();
            assembler.assemble(config, engine, tempDir);

            Path kpDir = tempDir.resolve(
                    "skills/architecture-hexagonal");
            assertThat(kpDir).doesNotExist();
        }
    }
}
