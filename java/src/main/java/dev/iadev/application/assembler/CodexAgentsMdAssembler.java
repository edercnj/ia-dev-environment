package dev.iadev.application.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.stack.StackResolver;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates {@code AGENTS.md} at the project root for
 * OpenAI Codex CLI.
 *
 * <p>Operates in 3 phases:
 * <ol>
 *   <li>Collect extended context by scanning agents,
 *       skills, and hooks from the output</li>
 *   <li>Build rendering context merging config fields
 *       with extended fields</li>
 *   <li>Render the Pebble template and write
 *       {@code AGENTS.md}</li>
 * </ol>
 *
 * <p>Scanning logic is in {@link CodexScanner}.</p>
 *
 * @see Assembler
 * @see CodexScanner
 * @see CodexShared
 */
public final class CodexAgentsMdAssembler
        implements Assembler {

    private static final String TEMPLATE_PATH =
            "targets/codex/templates/agents-md.md.njk";

    /** {@inheritDoc} */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        Path claudeDir = outputDir.getParent()
                .resolve(".claude");

        var scanned = scanClaudeDir(claudeDir);
        Map<String, Object> context =
                buildExtendedContext(
                        config, scanned.agents,
                        scanned.skills,
                        scanned.hookPresence);

        String rendered = engine.render(
                TEMPLATE_PATH, context);

        CopyHelpers.ensureDirectory(outputDir);
        Path dest = outputDir.resolve("AGENTS.md");
        CopyHelpers.writeFile(dest, rendered);
        return List.of(dest.toString());
    }

    private static ScannedOutput scanClaudeDir(
            Path claudeDir) {
        List<AgentInfo> agents = CodexScanner.scanAgents(
                claudeDir.resolve("agents"));
        List<SkillInfo> skills = CodexScanner.scanSkills(
                claudeDir.resolve("skills"));
        HookPresence hookPresence = HookPresence.of(
                CodexShared.detectHooks(
                        claudeDir.resolve("hooks")));
        return new ScannedOutput(
                agents, skills, hookPresence);
    }

    private record ScannedOutput(
            List<AgentInfo> agents,
            List<SkillInfo> skills,
            HookPresence hookPresence) {
    }

    /**
     * Scans a directory for agent {@code .md} files.
     * Delegates to {@link CodexScanner}.
     *
     * @param agentsDir the agents directory path
     * @return sorted list of {@link AgentInfo}
     */
    static List<AgentInfo> scanAgents(Path agentsDir) {
        return CodexScanner.scanAgents(agentsDir);
    }

    /**
     * Extracts description from the first meaningful line.
     * Delegates to {@link CodexScanner}.
     *
     * @param content the file content
     * @return the description string
     */
    static String extractDescription(String content) {
        return CodexScanner.extractDescription(content);
    }

    /**
     * Scans a directory for skill subdirs.
     * Delegates to {@link CodexScanner}.
     *
     * @param skillsDir the skills directory path
     * @return sorted list of {@link SkillInfo}
     */
    static List<SkillInfo> scanSkills(Path skillsDir) {
        return CodexScanner.scanSkills(skillsDir);
    }

    /**
     * Extracts raw YAML frontmatter block.
     * Delegates to {@link CodexScanner}.
     *
     * @param content the file content
     * @return Optional containing the YAML block
     */
    static java.util.Optional<String>
            extractFrontmatterBlock(String content) {
        return CodexScanner
                .extractFrontmatterBlock(content);
    }

    /**
     * Parses YAML frontmatter from SKILL.md.
     * Delegates to {@link CodexScanner}.
     *
     * @param content the SKILL.md file content
     * @param dirName the directory name as fallback
     * @return a {@link SkillInfo} with extracted metadata
     */
    static SkillInfo parseSkillFrontmatter(
            String content, String dirName) {
        return CodexScanner.parseSkillFrontmatter(
                content, dirName);
    }

    /**
     * Builds extended context for AGENTS.md template.
     *
     * @param config       the project configuration
     * @param agents       the scanned agents
     * @param skills       the scanned skills
     * @param hookPresence whether hooks were detected
     * @return the template context map
     */
    static Map<String, Object> buildExtendedContext(
            ProjectConfig config,
            List<AgentInfo> agents,
            List<SkillInfo> skills,
            HookPresence hookPresence) {
        var resolved = StackResolver.resolve(config);
        Map<String, Object> ctx =
                new LinkedHashMap<>(
                        ContextBuilder.buildContext(config));

        ctx.put("observability",
                config.observabilityTool());

        Map<String, String> resolvedStack =
                new LinkedHashMap<>();
        resolvedStack.put("buildCmd",
                resolved.buildCmd());
        resolvedStack.put("testCmd",
                resolved.testCmd());
        resolvedStack.put("compileCmd",
                resolved.compileCmd());
        resolvedStack.put("coverageCmd",
                resolved.coverageCmd());
        ctx.put("resolved_stack", resolvedStack);

        ctx.put("agents_list", agents);

        List<Map<String, Object>> templateSkills =
                new ArrayList<>();
        for (SkillInfo skill : skills) {
            Map<String, Object> entry =
                    new LinkedHashMap<>();
            entry.put("name", skill.name());
            entry.put("description", skill.description());
            entry.put("user_invocable",
                    skill.userInvocable());
            templateSkills.add(entry);
        }
        ctx.put("skills_list", templateSkills);

        ctx.put("has_hooks", hookPresence.hasHooks());
        ctx.put("mcp_servers",
                CodexShared.mapMcpServers(config));
        ctx.put("security_frameworks",
                new ArrayList<>(
                        config.security().frameworks()));
        ctx.put("model", CodexShared.DEFAULT_MODEL);
        ctx.put("approval_policy",
                CodexShared.deriveApprovalPolicy(
                        hookPresence));
        ctx.put("sandbox_mode",
                CodexShared.SANDBOX_WORKSPACE_WRITE);

        return ctx;
    }
}
