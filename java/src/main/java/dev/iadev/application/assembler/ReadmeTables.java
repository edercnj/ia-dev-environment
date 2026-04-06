package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;
import dev.iadev.domain.model.ProjectConfig;

import java.nio.file.Path;
import java.util.Set;

/**
 * Coordinates markdown table builders for README.md.
 *
 * <p>Delegates to three specialized builders:
 * <ul>
 *   <li>{@link SummaryTableBuilder} — generation summary
 *       and settings section</li>
 *   <li>{@link MappingTableBuilder} — platform mapping
 *       table</li>
 *   <li>{@link SkillsTableBuilder} — skills, agents, rules,
 *       knowledge packs, and hooks tables</li>
 * </ul>
 *
 * <p>All public methods are static for backward compatibility
 * with {@link ReadmeAssembler}.</p>
 *
 * @see ReadmeAssembler
 * @see SummaryTableBuilder
 * @see MappingTableBuilder
 * @see SkillsTableBuilder
 */
public final class ReadmeTables {

    private static final SkillsTableBuilder SKILLS =
            new SkillsTableBuilder();
    private static final MappingTableBuilder MAPPING =
            new MappingTableBuilder();
    private static final SummaryTableBuilder SUMMARY =
            new SummaryTableBuilder();

    private ReadmeTables() {
        // utility class
    }

    /**
     * Builds markdown table of rules.
     *
     * @param outputDir the .claude/ output directory
     * @return formatted markdown table
     */
    public static String buildRulesTable(Path outputDir) {
        return SKILLS.buildRulesTable(outputDir);
    }

    /**
     * Builds markdown table of skills, excluding knowledge
     * packs.
     *
     * @param outputDir the .claude/ output directory
     * @return formatted markdown table
     */
    public static String buildSkillsTable(Path outputDir) {
        return SKILLS.buildSkillsTable(outputDir);
    }

    /**
     * Builds markdown table of agents.
     *
     * @param outputDir the .claude/ output directory
     * @return formatted markdown table
     */
    public static String buildAgentsTable(Path outputDir) {
        return SKILLS.buildAgentsTable(outputDir);
    }

    /**
     * Builds markdown table of knowledge packs.
     *
     * @param outputDir the .claude/ output directory
     * @return formatted markdown table
     */
    public static String buildKnowledgePacksTable(
            Path outputDir) {
        return SKILLS.buildKnowledgePacksTable(outputDir);
    }

    /**
     * Builds hooks documentation section.
     *
     * @param config the project configuration
     * @return formatted hooks section
     */
    public static String buildReadmeHooksSection(
            ProjectConfig config) {
        return SKILLS.buildReadmeHooksSection(config);
    }

    /**
     * Builds static settings section content.
     *
     * @return formatted settings section
     */
    public static String buildSettingsSection() {
        return SUMMARY.buildSettingsSection();
    }

    /**
     * Builds the platform mapping table.
     *
     * @param outputDir the .claude/ output directory
     * @return formatted mapping table with totals
     */
    public static String buildMappingTable(Path outputDir) {
        return MAPPING.build(outputDir);
    }

    /**
     * Builds a platform-filtered mapping table.
     *
     * @param outputDir the .claude/ output directory
     * @param platforms the active platforms (empty = all)
     * @return formatted mapping table, or empty string
     */
    public static String buildMappingTable(
            Path outputDir, Set<Platform> platforms) {
        return MAPPING.build(outputDir, platforms);
    }

    /**
     * Builds the generation summary table.
     *
     * @param outputDir the .claude/ output directory
     * @param config    the project configuration
     * @return formatted generation summary
     */
    public static String buildGenerationSummary(
            Path outputDir, ProjectConfig config) {
        return SUMMARY.buildGenerationSummary(outputDir);
    }

    /**
     * Builds a platform-filtered generation summary.
     *
     * @param outputDir the .claude/ output directory
     * @param config    the project configuration
     * @param platforms the active platforms (empty = all)
     * @return formatted generation summary
     */
    public static String buildGenerationSummary(
            Path outputDir,
            ProjectConfig config,
            Set<Platform> platforms) {
        return SUMMARY.buildGenerationSummary(
                outputDir, platforms);
    }
}
