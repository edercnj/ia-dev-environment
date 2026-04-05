package dev.iadev.application.assembler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory that instantiates the 33 assemblers in the
 * fixed order defined by RULE-005.
 *
 * <p>Extracted from {@link AssemblerPipeline} to keep
 * both classes under 250 lines per RULE-004.</p>
 *
 * @see AssemblerPipeline
 * @see AssemblerDescriptor
 */
public final class AssemblerFactory {

    private AssemblerFactory() {
        // utility class
    }

    /**
     * Builds assemblers with default options (backward
     * compatible).
     *
     * @return immutable ordered list of assembler descriptors
     */
    public static List<AssemblerDescriptor>
            buildAssemblers() {
        return buildAssemblers(PipelineOptions.defaults());
    }

    /**
     * Builds the ordered list of 33 assemblers per RULE-005.
     *
     * <p>Delegates to group builders by category:
     * constitution, core, github, docs, codex, cicd,
     * and readme. The options parameter controls
     * constitution preservation behavior.</p>
     *
     * @param options pipeline options controlling assembler
     *                behavior
     * @return immutable ordered list of assembler descriptors
     */
    public static List<AssemblerDescriptor>
            buildAssemblers(PipelineOptions options) {
        List<AssemblerDescriptor> all = new ArrayList<>();
        all.addAll(buildConstitutionAssemblers(options));
        all.addAll(buildClaudeRulesAssemblers());
        all.addAll(buildClaudeConfigAssemblers());
        all.addAll(buildGithubInputAssemblers());
        all.addAll(buildGithubOutputAssemblers());
        all.addAll(buildDocsAssemblers());
        all.addAll(buildCodexAssemblers());
        all.addAll(buildCicdAssemblers());
        return List.copyOf(all);
    }

    private static List<AssemblerDescriptor>
            buildConstitutionAssemblers(
                    PipelineOptions options) {
        return List.of(
                desc("ConstitutionAssembler",
                        AssemblerTarget.ROOT,
                        new ConstitutionAssembler(
                                resolveConstitutionResources(),
                                options
                                        .overwriteConstitution())));
    }

    private static Path resolveConstitutionResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(
                        "templates/constitution/"
                                + "CONSTITUTION.md", 3);
    }

    private static List<AssemblerDescriptor>
            buildClaudeRulesAssemblers() {
        return List.of(
                desc("RulesAssembler",
                        AssemblerTarget.CLAUDE,
                        new RulesAssembler()),
                desc("SkillsAssembler",
                        AssemblerTarget.CLAUDE,
                        new SkillsAssembler()),
                desc("AgentsAssembler",
                        AssemblerTarget.CLAUDE,
                        new AgentsAssembler()),
                desc("PatternsAssembler",
                        AssemblerTarget.CLAUDE,
                        new PatternsAssembler()));
    }

    private static List<AssemblerDescriptor>
            buildClaudeConfigAssemblers() {
        return List.of(
                desc("ProtocolsAssembler",
                        AssemblerTarget.CLAUDE,
                        new ProtocolsAssembler()),
                desc("HooksAssembler",
                        AssemblerTarget.CLAUDE,
                        new HooksAssembler()),
                desc("SettingsAssembler",
                        AssemblerTarget.CLAUDE,
                        new SettingsAssembler()));
    }

    private static List<AssemblerDescriptor>
            buildGithubInputAssemblers() {
        return List.of(
                desc("GithubInstructionsAssembler",
                        AssemblerTarget.GITHUB,
                        new GithubInstructionsAssembler()),
                desc("GithubMcpAssembler",
                        AssemblerTarget.GITHUB,
                        new GithubMcpAssembler()),
                desc("GithubSkillsAssembler",
                        AssemblerTarget.GITHUB,
                        new GithubSkillsAssembler()));
    }

    private static List<AssemblerDescriptor>
            buildGithubOutputAssemblers() {
        return List.of(
                desc("GithubAgentsAssembler",
                        AssemblerTarget.GITHUB,
                        new GithubAgentsAssembler()),
                desc("GithubHooksAssembler",
                        AssemblerTarget.GITHUB,
                        new GithubHooksAssembler()),
                desc("GithubPromptsAssembler",
                        AssemblerTarget.GITHUB,
                        new GithubPromptsAssembler()),
                desc("PrIssueTemplateAssembler",
                        AssemblerTarget.GITHUB,
                        new PrIssueTemplateAssembler()));
    }

    private static List<AssemblerDescriptor>
            buildDocsAssemblers() {
        return List.of(
                desc("DocsAssembler",
                        AssemblerTarget.ROOT,
                        new DocsAssembler()),
                desc("GrpcDocsAssembler",
                        AssemblerTarget.ROOT,
                        new GrpcDocsAssembler()),
                desc("RunbookAssembler",
                        AssemblerTarget.ROOT,
                        new RunbookAssembler()),
                desc("IncidentTemplatesAssembler",
                        AssemblerTarget.ROOT,
                        new IncidentTemplatesAssembler()),
                desc("ReleaseChecklistAssembler",
                        AssemblerTarget.ROOT,
                        new ReleaseChecklistAssembler()),
                desc("OperationalRunbookAssembler",
                        AssemblerTarget.ROOT,
                        new OperationalRunbookAssembler()),
                desc("SloSliTemplateAssembler",
                        AssemblerTarget.ROOT,
                        new SloSliTemplateAssembler()),
                desc("DocsContributingAssembler",
                        AssemblerTarget.ROOT,
                        new DocsContributingAssembler()),
                desc("DataMigrationPlanAssembler",
                        AssemblerTarget.ROOT,
                        new DataMigrationPlanAssembler()));
    }

    private static List<AssemblerDescriptor>
            buildCodexAssemblers() {
        return List.of(
                desc("CodexAgentsMdAssembler",
                        AssemblerTarget.ROOT,
                        new CodexAgentsMdAssembler()),
                desc("CodexConfigAssembler",
                        AssemblerTarget.CODEX,
                        new CodexConfigAssembler()),
                desc("CodexSkillsAssembler",
                        AssemblerTarget.CODEX_AGENTS,
                        new CodexSkillsAssembler()),
                desc("CodexRequirementsAssembler",
                        AssemblerTarget.CODEX,
                        new CodexRequirementsAssembler()),
                desc("CodexOverrideAssembler",
                        AssemblerTarget.ROOT,
                        new CodexOverrideAssembler()),
                desc("DocsAdrAssembler",
                        AssemblerTarget.ROOT,
                        new DocsAdrAssembler()));
    }

    private static List<AssemblerDescriptor>
            buildCicdAssemblers() {
        return List.of(
                desc("CicdAssembler",
                        AssemblerTarget.ROOT,
                        new CicdAssembler()),
                desc("EpicReportAssembler",
                        AssemblerTarget.ROOT,
                        new EpicReportAssembler()),
                desc("ReadmeAssembler",
                        AssemblerTarget.CLAUDE,
                        new ReadmeAssembler()));
    }

    private static AssemblerDescriptor desc(
            String name,
            AssemblerTarget target,
            Assembler assembler) {
        return new AssemblerDescriptor(
                name, target, assembler);
    }
}
