package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.stack.StackResolver;
import dev.iadev.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
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
 *   <li>Collect extended context by scanning agents, skills,
 *       and hooks from the {@code .claude/} output</li>
 *   <li>Build rendering context merging the 25 flat config
 *       fields with extended fields</li>
 *   <li>Render the Pebble template and write
 *       {@code AGENTS.md}</li>
 * </ol>
 *
 * <p>This is the seventeenth assembler in the pipeline
 * (position 17 of 23 per RULE-005). Its target is
 * {@link AssemblerTarget#ROOT}.</p>
 *
 * @see Assembler
 * @see CodexShared
 */
public final class CodexAgentsMdAssembler
        implements Assembler {

    private static final String TEMPLATE_PATH =
            "codex-templates/agents-md.md.njk";

    /**
     * {@inheritDoc}
     *
     * <p>Generates {@code AGENTS.md} by scanning
     * {@code .claude/} output and rendering the codex
     * agents template.</p>
     */
    @Override
    public List<String> assemble(
            ProjectConfig config,
            TemplateEngine engine,
            Path outputDir) {
        List<String> warnings = new ArrayList<>();

        Path claudeDir = outputDir.getParent()
                .resolve(".claude");

        List<AgentInfo> agents = scanAgents(
                claudeDir.resolve("agents"));
        List<SkillInfo> skills = scanSkills(
                claudeDir.resolve("skills"));
        HookPresence hookPresence = HookPresence.of(
                CodexShared.detectHooks(
                        claudeDir.resolve("hooks")));

        if (agents.isEmpty()) {
            warnings.add(
                    "No agents found in output directory");
        }
        if (skills.isEmpty()) {
            warnings.add(
                    "No skills found in output directory");
        }

        Map<String, Object> context =
                buildExtendedContext(
                        config, agents, skills,
                        hookPresence);

        String rendered = engine.render(
                TEMPLATE_PATH, context);

        CopyHelpers.ensureDirectory(outputDir);
        Path dest = outputDir.resolve("AGENTS.md");
        CopyHelpers.writeFile(dest, rendered);

        List<String> result = new ArrayList<>();
        result.add(dest.toString());
        return result;
    }

    /**
     * Scans a directory for agent {@code .md} files.
     *
     * @param agentsDir the agents directory path
     * @return sorted list of {@link AgentInfo}
     */
    static List<AgentInfo> scanAgents(Path agentsDir) {
        if (!CodexShared.isAccessibleDirectory(agentsDir)) {
            return List.of();
        }
        List<AgentInfo> agents = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(
                             agentsDir, "*.md")) {
            List<Path> sorted = new ArrayList<>();
            stream.forEach(sorted::add);
            sorted.sort((a, b) -> a.getFileName().toString()
                    .compareTo(b.getFileName().toString()));
            for (Path file : sorted) {
                String name = file.getFileName().toString()
                        .replaceFirst("\\.md$", "");
                String content = Files.readString(
                        file, StandardCharsets.UTF_8);
                String description =
                        extractDescription(content);
                agents.add(
                        new AgentInfo(name, description));
            }
        } catch (IOException e) {
            return List.of();
        }
        return agents;
    }

    /**
     * Extracts description from the first meaningful line
     * of content.
     *
     * @param content the file content
     * @return the description string
     */
    static String extractDescription(String content) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim();
            }
            return trimmed;
        }
        return "";
    }

    /**
     * Scans a directory for skill subdirs containing
     * {@code SKILL.md}.
     *
     * @param skillsDir the skills directory path
     * @return sorted list of {@link SkillInfo}
     */
    static List<SkillInfo> scanSkills(Path skillsDir) {
        if (!CodexShared.isAccessibleDirectory(skillsDir)) {
            return List.of();
        }
        List<SkillInfo> skills = new ArrayList<>();
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(skillsDir)) {
            List<Path> sorted = new ArrayList<>();
            stream.forEach(sorted::add);
            sorted.sort((a, b) -> a.getFileName().toString()
                    .compareTo(b.getFileName().toString()));
            for (Path entry : sorted) {
                if (!Files.isDirectory(entry)) {
                    continue;
                }
                Path skillMd =
                        entry.resolve("SKILL.md");
                if (!Files.exists(skillMd)) {
                    continue;
                }
                String content = Files.readString(
                        skillMd, StandardCharsets.UTF_8);
                skills.add(parseSkillFrontmatter(
                        content,
                        entry.getFileName().toString()));
            }
        } catch (IOException e) {
            return List.of();
        }
        return skills;
    }

    /**
     * Extracts raw YAML frontmatter block between
     * {@code ---} delimiters.
     *
     * @param content the file content
     * @return the YAML block or null if not found
     */
    static String extractFrontmatterBlock(String content) {
        String[] lines = content.split("\n");
        if (lines.length == 0
                || !"---".equals(lines[0].trim())) {
            return null;
        }
        for (int i = 1; i < lines.length; i++) {
            if ("---".equals(lines[i].trim())) {
                StringBuilder sb = new StringBuilder();
                for (int j = 1; j < i; j++) {
                    if (j > 1) {
                        sb.append("\n");
                    }
                    sb.append(lines[j]);
                }
                return sb.toString();
            }
        }
        return null;
    }

    /**
     * Parses YAML frontmatter from {@code SKILL.md} content
     * using SnakeYAML.
     *
     * @param content the SKILL.md file content
     * @param dirName the directory name as fallback
     * @return a {@link SkillInfo} with extracted metadata
     */
    @SuppressWarnings("unchecked")
    static SkillInfo parseSkillFrontmatter(
            String content, String dirName) {
        String block = extractFrontmatterBlock(content);
        if (block == null) {
            return new SkillInfo(dirName, "", true);
        }
        Yaml yaml = new Yaml(new SafeConstructor(
                new LoaderOptions()));
        Object parsed = yaml.load(block);
        if (!(parsed instanceof Map)) {
            return new SkillInfo(dirName, "", true);
        }
        Map<String, Object> map =
                (Map<String, Object>) parsed;

        String name = map.get("name") instanceof String
                ? (String) map.get("name") : dirName;
        Object rawDesc = map.get("description");
        String description = rawDesc instanceof String
                ? ((String) rawDesc).trim() : "";
        Object rawInvocable = map.get("user-invocable");
        boolean userInvocable =
                !Boolean.FALSE.equals(rawInvocable);
        return new SkillInfo(name, description, userInvocable);
    }

    /**
     * Builds extended context for {@code AGENTS.md} template
     * rendering.
     *
     * <p>Merges the 25 flat config fields with extended
     * fields: resolved_stack, agents_list, skills_list,
     * has_hooks, mcp_servers, security_frameworks,
     * observability, model, approval_policy,
     * sandbox_mode.</p>
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
        resolvedStack.put("buildCmd", resolved.buildCmd());
        resolvedStack.put("testCmd", resolved.testCmd());
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
