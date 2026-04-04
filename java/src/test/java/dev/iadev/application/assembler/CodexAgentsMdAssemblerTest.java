package dev.iadev.assembler;

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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CodexAgentsMdAssembler — generates AGENTS.md
 * at the project root for OpenAI Codex CLI.
 */
@DisplayName("CodexAgentsMdAssembler")
class CodexAgentsMdAssemblerTest {

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            assertThat(new CodexAgentsMdAssembler())
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("scanAgents")
    class ScanAgents {

        @Test
        @DisplayName("extracts agent name and description"
                + " from .md files")
        void assemble_whenCalled_extractsAgentInfo(
                @TempDir Path tempDir) throws IOException {
            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);
            Files.writeString(
                    agentsDir.resolve("architect.md"),
                    "# Architecture Expert\n\nDetails...",
                    StandardCharsets.UTF_8);
            Files.writeString(
                    agentsDir.resolve("qa-engineer.md"),
                    "# QA Specialist\n\nMore...",
                    StandardCharsets.UTF_8);

            List<AgentInfo> agents =
                    CodexAgentsMdAssembler.scanAgents(
                            agentsDir);

            assertThat(agents).hasSize(2);
            assertThat(agents.get(0).name())
                    .isEqualTo("architect");
            assertThat(agents.get(0).description())
                    .isEqualTo("Architecture Expert");
            assertThat(agents.get(1).name())
                    .isEqualTo("qa-engineer");
        }

        @Test
        @DisplayName("returns empty list for missing dir")
        void assemble_forMissing_returnsEmpty(
                @TempDir Path tempDir) {
            List<AgentInfo> agents =
                    CodexAgentsMdAssembler.scanAgents(
                            tempDir.resolve("nope"));
            assertThat(agents).isEmpty();
        }

        @Test
        @DisplayName("returns sorted list")
        void assemble_whenCalled_returnsSorted(
                @TempDir Path tempDir) throws IOException {
            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);
            Files.writeString(
                    agentsDir.resolve("z-agent.md"),
                    "# Z Agent", StandardCharsets.UTF_8);
            Files.writeString(
                    agentsDir.resolve("a-agent.md"),
                    "# A Agent", StandardCharsets.UTF_8);

            List<AgentInfo> agents =
                    CodexAgentsMdAssembler.scanAgents(
                            agentsDir);

            assertThat(agents).hasSize(2);
            assertThat(agents.get(0).name())
                    .isEqualTo("a-agent");
            assertThat(agents.get(1).name())
                    .isEqualTo("z-agent");
        }

        @Test
        @DisplayName("extracts first non-empty line"
                + " when no heading")
        void assemble_whenCalled_extractsFirstLine(
                @TempDir Path tempDir) throws IOException {
            Path agentsDir = tempDir.resolve("agents");
            Files.createDirectories(agentsDir);
            Files.writeString(
                    agentsDir.resolve("dev.md"),
                    "Developer specialist",
                    StandardCharsets.UTF_8);

            List<AgentInfo> agents =
                    CodexAgentsMdAssembler.scanAgents(
                            agentsDir);

            assertThat(agents.get(0).description())
                    .isEqualTo("Developer specialist");
        }
    }

    @Nested
    @DisplayName("extractDescription")
    class ExtractDescription {

        @Test
        @DisplayName("extracts heading text without #")
        void assemble_whenCalled_extractsHeading() {
            assertThat(
                    CodexAgentsMdAssembler
                            .extractDescription(
                                    "# My Agent\n\nBody"))
                    .isEqualTo("My Agent");
        }

        @Test
        @DisplayName("returns first non-empty line")
        void assemble_whenCalled_returnsFirstLine() {
            assertThat(
                    CodexAgentsMdAssembler
                            .extractDescription(
                                    "\n\nSome text"))
                    .isEqualTo("Some text");
        }

        @Test
        @DisplayName("returns empty for empty content")
        void assemble_forEmpty_returnsEmpty() {
            assertThat(
                    CodexAgentsMdAssembler
                            .extractDescription(""))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("scanSkills")
    class ScanSkills {

        @Test
        @DisplayName("extracts skill info from frontmatter")
        void assemble_whenCalled_extractsSkillInfo(
                @TempDir Path tempDir) throws IOException {
            Path skillsDir = tempDir.resolve("skills");
            Path skill1 = skillsDir.resolve("my-skill");
            Files.createDirectories(skill1);
            Files.writeString(
                    skill1.resolve("SKILL.md"),
                    "---\nname: my-skill\ndescription:"
                            + " A cool skill\n"
                            + "user-invocable: true\n"
                            + "---\n\nBody",
                    StandardCharsets.UTF_8);

            List<SkillInfo> skills =
                    CodexAgentsMdAssembler.scanSkills(
                            skillsDir);

            assertThat(skills).hasSize(1);
            assertThat(skills.get(0).name())
                    .isEqualTo("my-skill");
            assertThat(skills.get(0).description())
                    .isEqualTo("A cool skill");
            assertThat(skills.get(0).userInvocable())
                    .isTrue();
        }

        @Test
        @DisplayName("uses dir name when no frontmatter")
        void assemble_whenCalled_usesDirNameFallback(
                @TempDir Path tempDir) throws IOException {
            Path skillsDir = tempDir.resolve("skills");
            Path skill1 = skillsDir.resolve("fallback");
            Files.createDirectories(skill1);
            Files.writeString(
                    skill1.resolve("SKILL.md"),
                    "No frontmatter here",
                    StandardCharsets.UTF_8);

            List<SkillInfo> skills =
                    CodexAgentsMdAssembler.scanSkills(
                            skillsDir);

            assertThat(skills.get(0).name())
                    .isEqualTo("fallback");
        }

        @Test
        @DisplayName("returns empty for missing dir")
        void assemble_forMissing_returnsEmpty(
                @TempDir Path tempDir) {
            List<SkillInfo> skills =
                    CodexAgentsMdAssembler.scanSkills(
                            tempDir.resolve("nope"));
            assertThat(skills).isEmpty();
        }

        @Test
        @DisplayName("skips dirs without SKILL.md")
        void assemble_withoutSkillMd_skipsDirs(
                @TempDir Path tempDir) throws IOException {
            Path skillsDir = tempDir.resolve("skills");
            Files.createDirectories(
                    skillsDir.resolve("no-skill"));

            List<SkillInfo> skills =
                    CodexAgentsMdAssembler.scanSkills(
                            skillsDir);

            assertThat(skills).isEmpty();
        }
    }

    @Nested
    @DisplayName("extractFrontmatterBlock")
    class ExtractFrontmatterBlock {

        @Test
        @DisplayName("extracts YAML between --- delimiters")
        void assemble_whenCalled_extractsYaml() {
            String content =
                    "---\nname: test\n---\nBody";
            assertThat(CodexAgentsMdAssembler
                    .extractFrontmatterBlock(content))
                    .isPresent()
                    .hasValue("name: test");
        }

        @Test
        @DisplayName("returns empty when no frontmatter")
        void assemble_whenNoFrontmatter_returnsEmpty() {
            assertThat(CodexAgentsMdAssembler
                    .extractFrontmatterBlock(
                            "No frontmatter"))
                    .isEmpty();
        }

        @Test
        @DisplayName("returns empty for unclosed"
                + " frontmatter")
        void assemble_forUnclosed_returnsEmpty() {
            assertThat(CodexAgentsMdAssembler
                    .extractFrontmatterBlock(
                            "---\nname: test\nNo close"))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("parseSkillFrontmatter")
    class ParseSkillFrontmatter {

        @Test
        @DisplayName("parses name, description,"
                + " user-invocable")
        void assemble_whenCalled_parsesAll() {
            String content =
                    "---\nname: x-review\n"
                            + "description: Code review\n"
                            + "user-invocable: true\n"
                            + "---\n";
            SkillInfo info =
                    CodexAgentsMdAssembler
                            .parseSkillFrontmatter(
                                    content, "fallback");
            assertThat(info.name()).isEqualTo("x-review");
            assertThat(info.description())
                    .isEqualTo("Code review");
            assertThat(info.userInvocable()).isTrue();
        }

        @Test
        @DisplayName("defaults user-invocable to true")
        void assemble_whenCalled_defaultsUserInvocable() {
            String content =
                    "---\nname: kp\n---\n";
            SkillInfo info =
                    CodexAgentsMdAssembler
                            .parseSkillFrontmatter(
                                    content, "kp");
            assertThat(info.userInvocable()).isTrue();
        }

        @Test
        @DisplayName("user-invocable false"
                + " when set to false")
        void assemble_whenCalled_userInvocableFalse() {
            String content =
                    "---\nname: hidden\n"
                            + "user-invocable: false\n"
                            + "---\n";
            SkillInfo info =
                    CodexAgentsMdAssembler
                            .parseSkillFrontmatter(
                                    content, "hidden");
            assertThat(info.userInvocable()).isFalse();
        }

        @Test
        @DisplayName("uses dir name when name absent")
        void assemble_whenCalled_usesDirName() {
            String content = "---\ndescription: D\n---\n";
            SkillInfo info =
                    CodexAgentsMdAssembler
                            .parseSkillFrontmatter(
                                    content, "my-dir");
            assertThat(info.name()).isEqualTo("my-dir");
        }
    }

    @Nested
    @DisplayName("buildExtendedContext")
    class BuildExtendedContext {

        @Test
        @DisplayName("contains 25 base fields plus"
                + " extended fields")
        void assemble_whenCalled_containsBaseAndExtended() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            List<AgentInfo> agents = List.of(
                    new AgentInfo("arch", "Architect"));
            List<SkillInfo> skills = List.of(
                    new SkillInfo(
                            "x-review", "Review", true));

            Map<String, Object> ctx =
                    CodexAgentsMdAssembler
                            .buildExtendedContext(
                                    config, agents,
                                    skills,
                                    HookPresence.WITH_HOOKS);

            // Base fields
            assertThat(ctx).containsKey("project_name");
            assertThat(ctx).containsKey("language_name");

            // Extended fields
            assertThat(ctx).containsKey("resolved_stack");
            assertThat(ctx).containsKey("agents_list");
            assertThat(ctx).containsKey("skills_list");
            assertThat(ctx).containsKey("has_hooks");
            assertThat(ctx).containsKey("mcp_servers");
            assertThat(ctx).containsKey(
                    "security_frameworks");
            assertThat(ctx).containsKey("model");
            assertThat(ctx).containsKey("approval_policy");
            assertThat(ctx).containsKey("sandbox_mode");
        }

        @Test
        @DisplayName("model is o4-mini")
        void assemble_whenCalled_modelIsDefault() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            Map<String, Object> ctx =
                    CodexAgentsMdAssembler
                            .buildExtendedContext(
                                    config, List.of(),
                                    List.of(), HookPresence.WITHOUT_HOOKS);

            assertThat(ctx.get("model"))
                    .isEqualTo("o4-mini");
        }

        @Test
        @DisplayName("approval_policy is on-request"
                + " with hooks")
        void assemble_withHooks_approvalPolicy() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            Map<String, Object> ctx =
                    CodexAgentsMdAssembler
                            .buildExtendedContext(
                                    config, List.of(),
                                    List.of(), HookPresence.WITH_HOOKS);

            assertThat(ctx.get("approval_policy"))
                    .isEqualTo("on-request");
        }

        @Test
        @DisplayName("skills_list maps to"
                + " snake_case format")
        void assemble_whenCalled_skillsListSnakeCase() {
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            List<SkillInfo> skills = List.of(
                    new SkillInfo(
                            "x-test", "Tester", true));

            Map<String, Object> ctx =
                    CodexAgentsMdAssembler
                            .buildExtendedContext(
                                    config, List.of(),
                                    skills,
                                    HookPresence.WITHOUT_HOOKS);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> skillsList =
                    (List<Map<String, Object>>)
                            ctx.get("skills_list");
            assertThat(skillsList).hasSize(1);
            assertThat(skillsList.get(0))
                    .containsKey("user_invocable");
        }
    }

    @Nested
    @DisplayName("assemble — generates AGENTS.md")
    class Assemble {

        @Test
        @DisplayName("generates AGENTS.md at root")
        void assemble_whenCalled_generatesAgentsMd(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            setupClaudeDir(outputDir);

            CodexAgentsMdAssembler assembler =
                    new CodexAgentsMdAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("kotlin", "2.0")
                            .framework("ktor", "")
                            .buildTool("gradle")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).hasSize(1);
            Path agentsMd = outputDir.resolve("AGENTS.md");
            assertThat(agentsMd).exists();
            String content = Files.readString(
                    agentsMd, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("test-project");
        }

        @Test
        @DisplayName("AGENTS.md includes agent info")
        void assemble_whenCalled_includesAgentInfo(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            setupClaudeDir(outputDir);

            CodexAgentsMdAssembler assembler =
                    new CodexAgentsMdAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path agentsMd = outputDir.resolve("AGENTS.md");
            String content = Files.readString(
                    agentsMd, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("architect");
            assertThat(content)
                    .contains("Agent Personas");
        }

        @Test
        @DisplayName("AGENTS.md includes skill info")
        void assemble_whenCalled_includesSkillInfo(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            setupClaudeDir(outputDir);

            CodexAgentsMdAssembler assembler =
                    new CodexAgentsMdAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path agentsMd = outputDir.resolve("AGENTS.md");
            String content = Files.readString(
                    agentsMd, StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("Available Skills");
            assertThat(content)
                    .contains("x-review");
        }

        @Test
        @DisplayName("AGENTS.md includes expanded security baseline")
        void assemble_whenCalled_includesExpandedSecurityBaseline(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("output");
            setupClaudeDir(outputDir);

            CodexAgentsMdAssembler assembler =
                    new CodexAgentsMdAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .securityFrameworks("owasp")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve("AGENTS.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("## Security Baseline")
                    .contains("### OWASP Top 10")
                    .contains("### Secrets Management")
                    .contains("### Input Validation")
                    .contains("### Security Headers")
                    .contains("### Cryptography");
        }

        private void setupClaudeDir(Path outputDir)
                throws IOException {
            Path claudeDir = outputDir.getParent()
                    .resolve(".claude");
            setupAgentsDir(claudeDir);
            setupSkillsDir(claudeDir);
            setupHooksDir(claudeDir);
        }

        private void setupAgentsDir(Path claudeDir)
                throws IOException {
            Path agentsDir =
                    claudeDir.resolve("agents");
            Files.createDirectories(agentsDir);
            Files.writeString(
                    agentsDir.resolve("architect.md"),
                    "# Architecture Expert",
                    StandardCharsets.UTF_8);
        }

        private void setupSkillsDir(Path claudeDir)
                throws IOException {
            Path skillDir = claudeDir
                    .resolve("skills/x-review");
            Files.createDirectories(skillDir);
            Files.writeString(
                    skillDir.resolve("SKILL.md"),
                    "---\nname: x-review\n"
                            + "description: Code review\n"
                            + "user-invocable: true\n"
                            + "---\nBody",
                    StandardCharsets.UTF_8);
        }

        private void setupHooksDir(Path claudeDir)
                throws IOException {
            Path hooksDir =
                    claudeDir.resolve("hooks");
            Files.createDirectories(hooksDir);
            Files.writeString(
                    hooksDir.resolve("hook.sh"),
                    "#!/bin/bash",
                    StandardCharsets.UTF_8);
        }
    }
}
