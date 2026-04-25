package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke integration test for KnowledgeAssembler wiring.
 *
 * <p>Verifies that the assembler is registered in the
 * factory pipeline and can assemble synthetic fixtures
 * end-to-end without error.</p>
 */
@DisplayName("KnowledgeAssemblerSmokeIT")
class KnowledgeAssemblerSmokeIT {

    @Test
    @DisplayName("assemblerRegistered_inAssemblerFactory")
    void assemblerRegistered_inAssemblerFactory() {
        List<AssemblerDescriptor> descriptors =
                AssemblerFactory.buildAssemblers();

        boolean found = descriptors.stream()
                .anyMatch(d ->
                        "KnowledgeAssembler".equals(d.name()));

        assertThat(found)
                .as("KnowledgeAssembler must be registered"
                        + " in AssemblerFactory")
                .isTrue();
    }

    @Test
    @DisplayName("assemblerOrdering_rulesBeforeKnowledgeBeforeSkills")
    void assemblerOrdering_rulesBeforeKnowledgeBeforeSkills() {
        List<AssemblerDescriptor> descriptors =
                AssemblerFactory.buildAssemblers();

        List<String> names = descriptors.stream()
                .map(AssemblerDescriptor::name)
                .toList();

        int rulesIdx = names.indexOf("RulesAssembler");
        int knowledgeIdx = names.indexOf("KnowledgeAssembler");
        int skillsIdx = names.indexOf("SkillsAssembler");

        assertThat(rulesIdx).isLessThan(knowledgeIdx);
        assertThat(knowledgeIdx).isLessThan(skillsIdx);
    }

    @Test
    @DisplayName("assemble_withSyntheticFixtures_copiesFilesVerbatim")
    void assemble_withSyntheticFixtures_copiesFilesVerbatim(
            @TempDir Path tempDir) throws IOException {
        Path sourceDir = tempDir.resolve("knowledge-src");
        Path targetDir = tempDir.resolve("output");
        Files.createDirectories(sourceDir);
        String expectedContent = "# Testing\nContent.\n";
        Files.writeString(
                sourceDir.resolve("testing.md"),
                expectedContent,
                StandardCharsets.UTF_8);

        KnowledgeAssembler assembler =
                new KnowledgeAssembler();
        assembler.assemble(sourceDir, targetDir);

        Path copied = targetDir.resolve("testing.md");
        assertThat(copied).exists();
        assertThat(Files.readString(
                copied, StandardCharsets.UTF_8))
                .isEqualTo(expectedContent);
    }

    @Test
    @DisplayName("assemble_viaAssemblerInterface_returnsGeneratedPaths")
    void assemble_viaAssemblerInterface_returnsGeneratedPaths(
            @TempDir Path tempDir) throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        ProjectConfig config = TestConfigBuilder.minimal();
        KnowledgeAssembler assembler =
                new KnowledgeAssembler();

        List<String> files = assembler.assemble(
                config, new TemplateEngine(), outputDir);

        assertThat(outputDir.resolve("knowledge")).exists();
        assertThat(files).isNotNull();
    }
}
