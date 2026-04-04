package dev.iadev.assembler;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden file parity tests for
 * GithubInstructionsAssembler.
 */
@DisplayName("GithubInstructionsAssembler — golden")
class GithubInstructionsGoldenTest {

    @Test
    @DisplayName("copilot-instructions.md matches"
            + " golden file for rust-axum")
    void assemble_copilotInstructions_matchesGolden(
            @TempDir Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        GithubInstructionsAssembler assembler =
                new GithubInstructionsAssembler();
        ProjectConfig config =
                GithubInstructionsTestFixtures
                        .buildRustAxumConfig();

        assembler.assemble(
                config, new TemplateEngine(),
                outputDir);

        String expected = loadResource(
                "golden/rust-axum/.github/"
                        + "copilot-instructions.md");
        assertThat(expected)
                .as("Golden file must exist")
                .isNotEmpty();

        String actual = Files.readString(
                outputDir.resolve(
                        "copilot-instructions.md"),
                StandardCharsets.UTF_8);
        assertThat(actual)
                .as("copilot-instructions.md must"
                        + " match golden file"
                        + " byte-for-byte")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("domain.instructions.md matches"
            + " golden file for rust-axum")
    void assemble_domainInstructions_matchesGolden(
            @TempDir Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        GithubInstructionsAssembler assembler =
                new GithubInstructionsAssembler();
        ProjectConfig config =
                GithubInstructionsTestFixtures
                        .buildRustAxumConfig();

        assembler.assemble(
                config, new TemplateEngine(),
                outputDir);

        String expected = loadResource(
                "golden/rust-axum/.github/"
                        + "instructions/"
                        + "domain.instructions.md");
        assertThat(expected)
                .as("Golden file must exist")
                .isNotEmpty();

        String actual = Files.readString(
                outputDir.resolve(
                        "instructions/"
                                + "domain"
                                + ".instructions.md"),
                StandardCharsets.UTF_8);
        assertThat(actual)
                .as("domain.instructions.md must"
                        + " match golden file")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("coding-standards.instructions.md"
            + " matches golden for rust-axum")
    void assemble_codingStandards_matchesGolden(
            @TempDir Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        GithubInstructionsAssembler assembler =
                new GithubInstructionsAssembler();
        ProjectConfig config =
                GithubInstructionsTestFixtures
                        .buildRustAxumConfig();

        assembler.assemble(
                config, new TemplateEngine(),
                outputDir);

        String expected = loadResource(
                "golden/rust-axum/.github/"
                        + "instructions/coding"
                        + "-standards"
                        + ".instructions.md");
        assertThat(expected)
                .as("Golden file must exist")
                .isNotEmpty();

        String actual = Files.readString(
                outputDir.resolve(
                        "instructions/coding"
                                + "-standards"
                                + ".instructions.md"),
                StandardCharsets.UTF_8);
        assertThat(actual)
                .as("coding-standards.instructions"
                        + ".md must match golden")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("architecture.instructions.md"
            + " matches golden for rust-axum")
    void assemble_architecture_matchesGolden(
            @TempDir Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        GithubInstructionsAssembler assembler =
                new GithubInstructionsAssembler();
        ProjectConfig config =
                GithubInstructionsTestFixtures
                        .buildRustAxumConfig();

        assembler.assemble(
                config, new TemplateEngine(),
                outputDir);

        String expected = loadResource(
                "golden/rust-axum/.github/"
                        + "instructions/"
                        + "architecture"
                        + ".instructions.md");
        assertThat(expected)
                .as("Golden file must exist")
                .isNotEmpty();

        String actual = Files.readString(
                outputDir.resolve(
                        "instructions/"
                                + "architecture"
                                + ".instructions.md"),
                StandardCharsets.UTF_8);
        assertThat(actual)
                .as("architecture.instructions.md"
                        + " must match golden")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("quality-gates.instructions.md"
            + " matches golden for rust-axum")
    void assemble_qualityGates_matchesGolden(
            @TempDir Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);

        GithubInstructionsAssembler assembler =
                new GithubInstructionsAssembler();
        ProjectConfig config =
                GithubInstructionsTestFixtures
                        .buildRustAxumConfig();

        assembler.assemble(
                config, new TemplateEngine(),
                outputDir);

        String expected = loadResource(
                "golden/rust-axum/.github/"
                        + "instructions/"
                        + "quality-gates"
                        + ".instructions.md");
        assertThat(expected)
                .as("Golden file must exist")
                .isNotEmpty();

        String actual = Files.readString(
                outputDir.resolve(
                        "instructions/"
                                + "quality-gates"
                                + ".instructions.md"),
                StandardCharsets.UTF_8);
        assertThat(actual)
                .as("quality-gates.instructions.md"
                        + " must match golden")
                .isEqualTo(expected);
    }

    private String loadResource(String path) {
        var url = getClass().getClassLoader()
                .getResource(path);
        if (url == null) {
            return null;
        }
        try {
            return Files.readString(
                    Path.of(url.getPath()),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
