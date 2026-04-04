package dev.iadev.application.assembler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Metadata extraction utilities for README generation.
 *
 * <p>Extracts rule numbers, scopes, skill descriptions,
 * and knowledge pack detection from file content.</p>
 *
 * <p>Extracted from {@link ReadmeUtils} to keep both
 * classes under 250 lines per RULE-004.</p>
 *
 * @see ReadmeUtils
 * @see ReadmeTables
 */
public final class ReadmeMetadata {

    private ReadmeMetadata() {
        // utility class
    }

    /**
     * Returns true if the SKILL.md flags a knowledge pack.
     *
     * @param skillMdPath path to SKILL.md
     * @return true if it is a knowledge pack
     */
    public static boolean isKnowledgePack(
            Path skillMdPath) {
        try {
            String text = Files.readString(
                    skillMdPath, StandardCharsets.UTF_8);
            if (text.contains("user-invocable: false")) {
                return true;
            }
            for (String line : text.split("\n")) {
                if (line.startsWith("# Knowledge Pack")) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read SKILL.md: "
                            + skillMdPath, e);
        }
    }

    /**
     * Extracts leading digits from a rule filename.
     *
     * @param filename the rule filename
     * @return the leading digits, or empty string
     */
    public static String extractRuleNumber(
            String filename) {
        var matcher = java.util.regex.Pattern
                .compile("^(\\d+)")
                .matcher(filename);
        return matcher.find() ? matcher.group(1) : "";
    }

    /**
     * Strips leading number+hyphen and {@code .md}
     * extension, then replaces hyphens with spaces.
     *
     * @param filename the rule filename
     * @return the extracted scope
     */
    public static String extractRuleScope(
            String filename) {
        String name = filename.replaceFirst("^\\d+-", "");
        name = name.replaceFirst("\\.md$", "");
        return name.replace('-', ' ');
    }

    /**
     * Reads SKILL.md and extracts the description value.
     *
     * @param skillMdPath path to SKILL.md
     * @return the description, or empty string
     */
    public static String extractSkillDescription(
            Path skillMdPath) {
        try {
            String text = Files.readString(
                    skillMdPath, StandardCharsets.UTF_8);
            for (String line : text.split("\n")) {
                if (line.startsWith("description:")) {
                    String[] parts = line.split(":", 2);
                    String desc = parts.length > 1
                            ? parts[1].trim() : "";
                    return desc.replaceAll(
                            "^[\"']|[\"']$", "");
                }
            }
            return "";
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read SKILL.md: "
                            + skillMdPath, e);
        }
    }
}
