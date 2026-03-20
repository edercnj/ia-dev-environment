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
 * Tests for SettingsAssembler — the seventh assembler in the
 * pipeline, generating .claude/settings.json and
 * .claude/settings.local.json with permissions and hook
 * configurations.
 */
@DisplayName("SettingsAssembler")
class SettingsAssemblerTest {

    @Nested
    @DisplayName("assemble — implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            SettingsAssembler assembler =
                    new SettingsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — file generation")
    class FileGeneration {

        @Test
        @DisplayName("generates settings.json and"
                + " settings.local.json")
        void assemble_whenCalled_generatesBothFiles(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).hasSize(2);
            assertThat(outputDir.resolve("settings.json"))
                    .exists();
            assertThat(outputDir.resolve(
                    "settings.local.json"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("assemble — maven permissions")
    class MavenPermissions {

        @Test
        @DisplayName("settings.json contains maven commands"
                + " for java-quarkus")
        void assemble_whenCalled_containsMavenCommands(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Bash(mvn *)");
        }

        @Test
        @DisplayName("settings.json contains universal"
                + " git commands")
        void assemble_whenCalled_containsGitCommands(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Bash(git *)");
        }
    }

    @Nested
    @DisplayName("assemble — npm permissions")
    class NpmPermissions {

        @Test
        @DisplayName("settings.json contains npm commands"
                + " for typescript-nestjs")
        void assemble_whenCalled_containsNpmCommands(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("typescript", "5")
                            .framework("nestjs", "10")
                            .buildTool("npm")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Bash(npm *)")
                    .contains("Bash(npx *)")
                    .contains("Bash(node *)");
        }

        @Test
        @DisplayName("npm config does NOT contain maven"
                + " commands")
        void assemble_whenCalled_doesNotContainMavenCommands(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("typescript", "5")
                            .framework("nestjs", "10")
                            .buildTool("npm")
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .doesNotContain("Bash(mvn *)");
        }
    }

    @Nested
    @DisplayName("assemble — hooks configuration")
    class HooksConfig {

        @Test
        @DisplayName("settings.json contains PostToolUse"
                + " hooks for compiled language")
        void assemble_whenCalled_containsHooksForCompiledLang(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("kotlin", "2.0")
                            .framework("ktor", "")
                            .buildTool("gradle")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("PostToolUse");
            assertThat(content)
                    .contains("Write|Edit");
            assertThat(content)
                    .contains("post-compile-check.sh");
        }

        @Test
        @DisplayName("settings.json does NOT contain hooks"
                + " for python (no compile)")
        void assemble_noHooksForPython_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.115")
                            .buildTool("pip")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .doesNotContain("PostToolUse");
            assertThat(content)
                    .doesNotContain("hooks");
        }
    }

    @Nested
    @DisplayName("assemble — JSON validity")
    class JsonValidity {

        @Test
        @DisplayName("settings.json is valid JSON with"
                + " required keys")
        void assemble_settings_isValidJson(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("kotlin", "2.0")
                            .framework("ktor", "")
                            .buildTool("gradle")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("\"permissions\"");
            assertThat(content)
                    .contains("\"allow\"");
            // Verify it starts and ends as valid JSON
            assertThat(content.trim())
                    .startsWith("{");
            assertThat(content.trim())
                    .endsWith("}");
        }

        @Test
        @DisplayName("settings.local.json is valid JSON"
                + " with empty allow list")
        void assemble_settingsLocal_isValidJson(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.local.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("\"permissions\"");
            assertThat(content)
                    .contains("\"allow\": []");
        }
    }

    @Nested
    @DisplayName("buildSettingsJson — JSON structure")
    class BuildSettingsJson {

        @Test
        @DisplayName("without hooks produces permissions"
                + " only")
        void buildSettingsJson_withoutHooksPermissionsOnly_succeeds() {
            List<String> perms = List.of("Bash(git *)");
            String json = SettingsAssembler
                    .buildSettingsJson(perms, HookPresence.WITHOUT_HOOKS);

            assertThat(json)
                    .contains("\"permissions\"")
                    .contains("\"allow\"")
                    .contains("Bash(git *)")
                    .doesNotContain("hooks");
        }

        @Test
        @DisplayName("with hooks includes PostToolUse"
                + " section")
        void buildSettingsJson_withHooks_includesPostToolUse() {
            List<String> perms = List.of("Bash(git *)");
            String json = SettingsAssembler
                    .buildSettingsJson(perms, HookPresence.WITH_HOOKS);

            assertThat(json)
                    .contains("\"hooks\"")
                    .contains("\"PostToolUse\"")
                    .contains("\"Write|Edit\"")
                    .contains("post-compile-check.sh")
                    .contains("\"timeout\": 60")
                    .contains("Checking compilation...");
        }
    }

    @Nested
    @DisplayName("buildSettingsLocalJson — structure")
    class BuildSettingsLocalJson {

        @Test
        @DisplayName("produces empty permissions")
        void buildSettingsLocalJson_whenCalled_producesEmptyPermissions() {
            String json = SettingsAssembler
                    .buildSettingsLocalJson();

            assertThat(json)
                    .contains("\"permissions\"")
                    .contains("\"allow\": []");
        }
    }

    @Nested
    @DisplayName("parseJsonStringArray — parsing")
    class ParseJsonStringArray {

        @Test
        @DisplayName("parses simple JSON array")
        void parseJsonStringArray_whenCalled_parsesSimpleArray() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray(
                            "[\"a\", \"b\", \"c\"]");

            assertThat(result)
                    .containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("returns empty for empty array")
        void parseJsonStringArray_emptyForEmptyArray_succeeds() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray("[]");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty for non-array")
        void parseJsonStringArray_emptyForNonArray_succeeds() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray("{}");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("handles entries with parentheses")
        void parseJsonStringArray_whenCalled_handlesParentheses() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray(
                            "[\"Bash(git *)\","
                                    + " \"Bash(ls *)\"]");

            assertThat(result)
                    .containsExactly(
                            "Bash(git *)",
                            "Bash(ls *)");
        }

        @Test
        @DisplayName("handles entries with special chars")
        void parseJsonStringArray_whenCalled_handlesSpecialChars() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray(
                            "[\"WebFetch"
                                    + "(domain:github.com)\"]");

            assertThat(result)
                    .containsExactly(
                            "WebFetch(domain:github.com)");
        }
    }

    @Nested
    @DisplayName("deduplicate — removes duplicates")
    class Deduplicate {

        @Test
        @DisplayName("preserves order and removes"
                + " duplicates")
        void deduplicate_preservesOrder_removesDupes() {
            List<String> input = List.of(
                    "a", "b", "a", "c", "b");

            List<String> result =
                    SettingsAssembler.deduplicate(input);

            assertThat(result)
                    .containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("returns same list when no duplicates")
        void deduplicate_noDuplicatesUnchanged_succeeds() {
            List<String> input = List.of("a", "b", "c");

            List<String> result =
                    SettingsAssembler.deduplicate(input);

            assertThat(result)
                    .containsExactly("a", "b", "c");
        }
    }

    @Nested
    @DisplayName("collectPermissions — permission merging")
    class CollectPermissions {

        @Test
        @DisplayName("maven config includes base + maven"
                + " permissions")
        void collectPermissions_maven_includesBaseAndMaven(
                @TempDir Path tempDir)
                throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);

            SettingsAssembler assembler =
                    new SettingsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    assembler.collectPermissions(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(git *)")
                    .contains("Bash(mvn *)");
        }

        @Test
        @DisplayName("docker container adds docker"
                + " permissions")
        void collectPermissions_docker_addsDockerPerms(
                @TempDir Path tempDir)
                throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);

            SettingsAssembler assembler =
                    new SettingsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .container("docker")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    assembler.collectPermissions(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(docker build *)");
        }

        @Test
        @DisplayName("kubernetes orchestrator adds k8s"
                + " permissions")
        void collectPermissions_k8s_addsK8sPerms(
                @TempDir Path tempDir)
                throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);

            SettingsAssembler assembler =
                    new SettingsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .container("none")
                            .orchestrator("kubernetes")
                            .smokeTests(false)
                            .build();

            List<String> perms =
                    assembler.collectPermissions(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(kubectl get *)");
        }

        @Test
        @DisplayName("smoke tests add newman permissions")
        void collectPermissions_whenCalled_smokeTestsAddNewman(
                @TempDir Path tempDir)
                throws IOException {
            Path templatesDir =
                    setupTemplatesDir(tempDir);

            SettingsAssembler assembler =
                    new SettingsAssembler(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("quarkus", "3.17")
                            .buildTool("maven")
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(true)
                            .build();

            List<String> perms =
                    assembler.collectPermissions(
                            config, templatesDir);

            assertThat(perms)
                    .contains("Bash(newman *)");
        }

        private Path setupTemplatesDir(Path tempDir)
                throws IOException {
            Path templatesDir = tempDir.resolve(
                    "settings-templates");
            Files.createDirectories(templatesDir);
            Files.writeString(
                    templatesDir.resolve("base.json"),
                    "[\"Bash(git *)\"]",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    templatesDir.resolve(
                            "java-maven.json"),
                    "[\"Bash(mvn *)\"]",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    templatesDir.resolve("docker.json"),
                    "[\"Bash(docker build *)\"]",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    templatesDir.resolve(
                            "kubernetes.json"),
                    "[\"Bash(kubectl get *)\"]",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    templatesDir.resolve(
                            "testing-newman.json"),
                    "[\"Bash(newman *)\"]",
                    StandardCharsets.UTF_8);
            return templatesDir;
        }
    }

    @Nested
    @DisplayName("assemble — golden file parity")
    class GoldenFile {

        @Test
        @DisplayName("settings.json matches golden file"
                + " for kotlin-ktor")
        void assemble_settings_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    HooksAssemblerTest
                            .buildKotlinKtorConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String goldenPath =
                    "golden/kotlin-ktor/.claude/"
                            + "settings.json";
            String expected = loadResource(goldenPath);
            assertThat(expected)
                    .as("Golden file must exist")
                    .isNotNull();

            String actual = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(actual)
                    .as("settings.json must match golden"
                            + " file byte-for-byte")
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("settings.local.json matches golden"
                + " file for kotlin-ktor")
        void assemble_settingsLocal_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    HooksAssemblerTest
                            .buildKotlinKtorConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String goldenPath =
                    "golden/kotlin-ktor/.claude/"
                            + "settings.local.json";
            String expected = loadResource(goldenPath);
            assertThat(expected)
                    .as("Golden file must exist")
                    .isNotNull();

            String actual = Files.readString(
                    outputDir.resolve(
                            "settings.local.json"),
                    StandardCharsets.UTF_8);
            assertThat(actual)
                    .as("settings.local.json must match"
                            + " golden file byte-for-byte")
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
        @DisplayName("unknown language still includes"
                + " base permissions")
        void assemble_unknownLanguage_includesBase(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("unknown", "1.0")
                            .framework("unknown", "1.0")
                            .buildTool("unknown")
                            .container("none")
                            .orchestrator("none")
                            .smokeTests(false)
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Bash(git *)");
        }

        @Test
        @DisplayName("podman container adds docker"
                + " permissions")
        void assemble_podman_addsDockerPerms(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("podman")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Bash(docker build *)");
        }

        @Test
        @DisplayName("docker-compose orchestrator adds"
                + " compose permissions")
        void assemble_compose_addsComposePerms(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            SettingsAssembler assembler =
                    new SettingsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .orchestrator("docker-compose")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("settings.json"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Bash(docker compose *)");
        }
    }
}
