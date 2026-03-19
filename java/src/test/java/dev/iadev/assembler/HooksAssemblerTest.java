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
 * Tests for HooksAssembler — the sixth assembler in the
 * pipeline, generating .claude/hooks/ directory with
 * post-compile hook scripts for compiled languages.
 */
@DisplayName("HooksAssembler")
class HooksAssemblerTest {

    @Nested
    @DisplayName("assemble — implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void isAssemblerInstance() {
            HooksAssembler assembler =
                    new HooksAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — hook generation")
    class HookGeneration {

        @Test
        @DisplayName("generates post-compile-check.sh"
                + " for kotlin-gradle")
        void generatesHookForKotlin(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            HooksAssembler assembler =
                    new HooksAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("kotlin", "2.0")
                            .framework("ktor", "")
                            .buildTool("gradle")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).hasSize(1);
            Path hookFile = outputDir.resolve(
                    "hooks/post-compile-check.sh");
            assertThat(hookFile).exists();
        }

        @Test
        @DisplayName("generates post-compile-check.sh"
                + " for java-maven")
        void generatesHookForJavaMaven(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            HooksAssembler assembler =
                    new HooksAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).hasSize(1);
            Path hookFile = outputDir.resolve(
                    "hooks/post-compile-check.sh");
            assertThat(hookFile).exists();
        }

        @Test
        @DisplayName("generates post-compile-check.sh"
                + " for typescript-npm")
        void generatesHookForTypescript(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            HooksAssembler assembler =
                    new HooksAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("typescript", "5")
                            .framework("nestjs", "10")
                            .buildTool("npm")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).hasSize(1);
        }

        @Test
        @DisplayName("returns empty list for python"
                + " (no hook template)")
        void returnsEmptyForPython(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            HooksAssembler assembler =
                    new HooksAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.115")
                            .buildTool("pip")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("assemble — hook content and permissions")
    class HookContent {

        @Test
        @DisplayName("hook script starts with shebang line")
        void hookStartsWithShebang(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            HooksAssembler assembler =
                    new HooksAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("kotlin", "2.0")
                            .framework("ktor", "")
                            .buildTool("gradle")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path hookFile = outputDir.resolve(
                    "hooks/post-compile-check.sh");
            String content = Files.readString(
                    hookFile, StandardCharsets.UTF_8);
            assertThat(content)
                    .startsWith("#!/usr/bin/env bash");
        }

        @Test
        @DisplayName("hook script has executable permission")
        void hookIsExecutable(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            HooksAssembler assembler =
                    new HooksAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("kotlin", "2.0")
                            .framework("ktor", "")
                            .buildTool("gradle")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path hookFile = outputDir.resolve(
                    "hooks/post-compile-check.sh");
            assertThat(Files.isExecutable(hookFile))
                    .isTrue();
        }

        @Test
        @DisplayName("kotlin hook contains compileKotlin"
                + " command")
        void kotlinHookContainsCompileKotlin(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            HooksAssembler assembler =
                    new HooksAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("kotlin", "2.0")
                            .framework("ktor", "")
                            .buildTool("gradle")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path hookFile = outputDir.resolve(
                    "hooks/post-compile-check.sh");
            String content = Files.readString(
                    hookFile, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("compileKotlin");
        }

        @Test
        @DisplayName("java-maven hook contains mvn compile")
        void javaMavenHookContainsMvnCompile(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            HooksAssembler assembler =
                    new HooksAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path hookFile = outputDir.resolve(
                    "hooks/post-compile-check.sh");
            String content = Files.readString(
                    hookFile, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("compile");
        }
    }

    @Nested
    @DisplayName("assemble — golden file parity")
    class GoldenFile {

        @Test
        @DisplayName("post-compile-check.sh matches"
                + " golden file for kotlin-ktor")
        void hookMatchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            HooksAssembler assembler =
                    new HooksAssembler();
            ProjectConfig config = buildKotlinKtorConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String goldenPath =
                    "golden/kotlin-ktor/.claude/hooks/"
                            + "post-compile-check.sh";
            String expected = loadResource(goldenPath);
            assertThat(expected)
                    .as("Golden file must exist")
                    .isNotNull();

            String actual = Files.readString(
                    outputDir.resolve(
                            "hooks/post-compile-check.sh"),
                    StandardCharsets.UTF_8);
            assertThat(actual)
                    .as("Hook must match golden file"
                            + " byte-for-byte")
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

    @Nested
    @DisplayName("assemble — edge cases")
    class EdgeCases {

        @Test
        @DisplayName("missing hook template returns"
                + " empty list")
        void missingTemplateReturnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path hooksDir = resourceDir.resolve(
                    "hooks-templates/exotic");
            Files.createDirectories(hooksDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            HooksAssembler assembler =
                    new HooksAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("kotlin", "2.0")
                            .framework("ktor", "")
                            .buildTool("gradle")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("unknown language returns empty list")
        void unknownLanguageReturnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            HooksAssembler assembler =
                    new HooksAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("unknown", "1.0")
                            .framework("unknown", "1.0")
                            .buildTool("unknown")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    static ProjectConfig buildKotlinKtorConfig() {
        return TestConfigBuilder.builder()
                .projectName("my-ktor-service")
                .purpose("Describe your service"
                        + " purpose here")
                .archStyle("microservice")
                .domainDriven(true)
                .eventDriven(true)
                .language("kotlin", "2.0")
                .framework("ktor", "")
                .buildTool("gradle")
                .nativeBuild(false)
                .container("docker")
                .orchestrator("kubernetes")
                .iac("terraform")
                .apiGateway("kong")
                .securityFrameworks("lgpd")
                .smokeTests(true)
                .contractTests(true)
                .performanceTests(true)
                .clearInterfaces()
                .addInterface("rest")
                .addInterface("websocket")
                .addInterface("event-consumer")
                .addInterface("event-producer")
                .build();
    }
}
