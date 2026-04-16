package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        void instanceOf_whenCreated_implementsAssemblerInterface() {
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
        void assemble_whenCalled_generatesHookForKotlin(
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
                            .telemetryEnabled(false)
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
        void assemble_whenCalled_generatesHookForJavaMaven(
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
                            .telemetryEnabled(false)
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
        void assemble_whenCalled_generatesHookForTypescript(
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
                            .telemetryEnabled(false)
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).hasSize(1);
        }

        @Test
        @DisplayName("returns empty list for python"
                + " (no hook template)")
        void assemble_whenCalled_returnsEmptyForPython(
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
                            .telemetryEnabled(false)
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
        void assemble_whenCalled_hookStartsWithShebang(
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
        void assemble_hook_isExecutable(
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
        void assemble_kotlinHook_containsCompileKotlin(
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
        void assemble_javaMavenHook_containsMvnCompile(
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
        void assemble_hook_matchesGolden(
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
                    .isNotEmpty();

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
        void assemble_missingTemplate_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path hooksDir = resourceDir.resolve(
                    "targets/claude/hooks/exotic");
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
                            .telemetryEnabled(false)
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("unknown language returns empty list")
        void assemble_unknownLanguage_returnsEmpty(
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
                            .telemetryEnabled(false)
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("assemble — telemetry hooks (story-0040-0004)")
    class TelemetryHooks {

        private static final String[] TELEMETRY_FILES = {
                "telemetry-emit.sh",
                "telemetry-lib.sh",
                "telemetry-session.sh",
                "telemetry-pretool.sh",
                "telemetry-posttool.sh",
                "telemetry-subagent.sh",
                "telemetry-stop.sh"
        };

        @Test
        @DisplayName("telemetryEnabled=true copies all 7"
                + " telemetry scripts")
        void assemble_telemetryEnabled_copiesAllScripts(
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
                            .telemetryEnabled(true)
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path hooksDir = outputDir.resolve("hooks");
            for (String name : TELEMETRY_FILES) {
                Path f = hooksDir.resolve(name);
                assertThat(f)
                        .as("missing %s", name)
                        .exists();
            }
        }

        @Test
        @DisplayName("telemetryEnabled=false copies zero"
                + " telemetry scripts")
        void assemble_telemetryDisabled_skipsScripts(
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
                            .telemetryEnabled(false)
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path hooksDir = outputDir.resolve("hooks");
            if (!Files.exists(hooksDir)) {
                return;
            }
            for (String name : TELEMETRY_FILES) {
                assertThat(hooksDir.resolve(name))
                        .as("telemetry script %s must NOT"
                                + " be copied when disabled",
                                name)
                        .doesNotExist();
            }
        }

        @Test
        @DisplayName("telemetry scripts are executable")
        void assemble_telemetryEnabled_scriptsExecutable(
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
                            .telemetryEnabled(true)
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path hooksDir = outputDir.resolve("hooks");
            for (String name : TELEMETRY_FILES) {
                Path f = hooksDir.resolve(name);
                assertThat(Files.isExecutable(f))
                        .as("%s must be executable", name)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("telemetry coexists with"
                + " post-compile-check.sh")
        void assemble_telemetryWithCompiledLang_coexist(
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
                            .telemetryEnabled(true)
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path hooksDir = outputDir.resolve("hooks");
            assertThat(hooksDir.resolve(
                    "post-compile-check.sh")).exists();
            for (String name : TELEMETRY_FILES) {
                assertThat(hooksDir.resolve(name))
                        .as("missing %s", name).exists();
            }
            assertThat(files.size())
                    .isGreaterThanOrEqualTo(
                            TELEMETRY_FILES.length + 1);
        }

        @Test
        @DisplayName("missing telemetry source aborts with"
                + " AssemblerException citing path")
        void assemble_missingTelemetryFile_throws(
                @TempDir Path tempDir)
                throws IOException {
            // Resource dir with hooks/ folder but NO
            // telemetry-*.sh files present.
            Path resourceDir = tempDir.resolve("res");
            Path hooksSrc = resourceDir.resolve(
                    "targets/claude/hooks");
            Files.createDirectories(hooksSrc);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            HooksAssembler assembler =
                    new HooksAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.115")
                            .buildTool("pip")
                            .telemetryEnabled(true)
                            .build();

            assertThatThrownBy(() -> assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(
                            "telemetry-emit.sh");
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
