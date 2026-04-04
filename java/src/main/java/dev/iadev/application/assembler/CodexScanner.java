package dev.iadev.application.assembler;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Scans agents and skills directories for Codex
 * AGENTS.md generation.
 *
 * <p>Extracted from {@link CodexAgentsMdAssembler} to
 * keep both classes under 250 lines per RULE-004.</p>
 *
 * @see CodexAgentsMdAssembler
 */
final class CodexScanner {

    private CodexScanner() {
        // utility class
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
     * @return Optional containing the YAML block,
     *         or empty if not found
     */
    static Optional<String> extractFrontmatterBlock(
            String content) {
        String[] lines = content.split("\n");
        if (lines.length == 0
                || !"---".equals(lines[0].trim())) {
            return Optional.empty();
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
                return Optional.of(sb.toString());
            }
        }
        return Optional.empty();
    }

    /**
     * Parses YAML frontmatter from {@code SKILL.md}
     * content using SnakeYAML.
     *
     * @param content the SKILL.md file content
     * @param dirName the directory name as fallback
     * @return a {@link SkillInfo} with extracted metadata
     */
    @SuppressWarnings("unchecked")
    static SkillInfo parseSkillFrontmatter(
            String content, String dirName) {
        Optional<String> blockOpt =
                extractFrontmatterBlock(content);
        if (blockOpt.isEmpty()) {
            return new SkillInfo(dirName, "", true);
        }
        String block = blockOpt.orElseThrow();
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
        return new SkillInfo(name, description,
                userInvocable);
    }
}
