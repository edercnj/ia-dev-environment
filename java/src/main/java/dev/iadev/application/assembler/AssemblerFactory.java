package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Factory that instantiates the 22 assemblers in the
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
     * Builds the ordered list of 22 assemblers per RULE-005.
     *
     * <p>Delegates to group builders by category:
     * constitution, core, docs, cicd,
     * and readme. The options parameter controls
     * constitution preservation behavior.</p>
     *
     * @param options pipeline options controlling assembler
     *                behavior
     * @return immutable ordered list of assembler descriptors
     */
    public static List<AssemblerDescriptor>
            buildAssemblers(PipelineOptions options) {
        List<AssemblerDescriptor> all =
                buildAllAssemblers(options);
        return PlatformFilter.filter(
                all, options.platforms());
    }

    /**
     * Builds ALL 22 assemblers without platform filtering.
     *
     * <p>Use this when you need the complete list for
     * verbose/dry-run comparison against filtered list.</p>
     *
     * @param options pipeline options controlling assembler
     *                behavior (platforms field is ignored)
     * @return immutable ordered list of all 22 assembler
     *         descriptors
     */
    public static List<AssemblerDescriptor>
            buildAllAssemblers(PipelineOptions options) {
        List<AssemblerDescriptor> all = new ArrayList<>();
        all.addAll(buildConstitutionAssemblers(options));
        all.addAll(buildClaudeRulesAssemblers());
        all.addAll(buildClaudeConfigAssemblers());
        all.addAll(buildDocsAssemblers());
        all.addAll(buildCicdAssemblers());
        return List.copyOf(all);
    }

    private static List<AssemblerDescriptor>
            buildConstitutionAssemblers(
                    PipelineOptions options) {
        return List.of(
                desc("ConstitutionAssembler",
                        AssemblerTarget.ROOT,
                        Set.of(Platform.SHARED),
                        new ConstitutionAssembler(
                                resolveConstitutionResources(),
                                options
                                        .overwriteConstitution())));
    }

    private static Path resolveConstitutionResources() {
        return dev.iadev.util.ResourceResolver
                .resolveResourcesRoot(
                        "shared/templates/constitution/"
                                + "CONSTITUTION.md", 4);
    }

    private static List<AssemblerDescriptor>
            buildClaudeRulesAssemblers() {
        Set<Platform> claude = Set.of(Platform.CLAUDE_CODE);
        return List.of(
                desc("RulesAssembler",
                        AssemblerTarget.CLAUDE,
                        claude,
                        new RulesAssembler()),
                desc("SkillsAssembler",
                        AssemblerTarget.CLAUDE,
                        claude,
                        new SkillsAssembler()),
                desc("AgentsAssembler",
                        AssemblerTarget.CLAUDE,
                        claude,
                        new AgentsAssembler()),
                desc("PatternsAssembler",
                        AssemblerTarget.CLAUDE,
                        claude,
                        new PatternsAssembler()));
    }

    private static List<AssemblerDescriptor>
            buildClaudeConfigAssemblers() {
        Set<Platform> claude = Set.of(Platform.CLAUDE_CODE);
        return List.of(
                desc("ProtocolsAssembler",
                        AssemblerTarget.CLAUDE,
                        claude,
                        new ProtocolsAssembler()),
                desc("HooksAssembler",
                        AssemblerTarget.CLAUDE,
                        claude,
                        new HooksAssembler()),
                desc("SettingsAssembler",
                        AssemblerTarget.CLAUDE,
                        claude,
                        new SettingsAssembler()));
    }

    private static List<AssemblerDescriptor>
            buildDocsAssemblers() {
        Set<Platform> shared = Set.of(Platform.SHARED);
        return List.of(
                desc("DocsAssembler",
                        AssemblerTarget.ROOT,
                        shared,
                        new DocsAssembler()),
                desc("DocsAdrAssembler",
                        AssemblerTarget.ROOT,
                        shared,
                        new DocsAdrAssembler()),
                desc("GrpcDocsAssembler",
                        AssemblerTarget.ROOT,
                        shared,
                        new GrpcDocsAssembler()),
                desc("RunbookAssembler",
                        AssemblerTarget.ROOT,
                        shared,
                        new RunbookAssembler()),
                desc("IncidentTemplatesAssembler",
                        AssemblerTarget.ROOT,
                        shared,
                        new IncidentTemplatesAssembler()),
                desc("ReleaseChecklistAssembler",
                        AssemblerTarget.ROOT,
                        shared,
                        new ReleaseChecklistAssembler()),
                desc("OperationalRunbookAssembler",
                        AssemblerTarget.ROOT,
                        shared,
                        new OperationalRunbookAssembler()),
                desc("SloSliTemplateAssembler",
                        AssemblerTarget.ROOT,
                        shared,
                        new SloSliTemplateAssembler()),
                desc("DocsContributingAssembler",
                        AssemblerTarget.ROOT,
                        shared,
                        new DocsContributingAssembler()),
                desc("DataMigrationPlanAssembler",
                        AssemblerTarget.ROOT,
                        shared,
                        new DataMigrationPlanAssembler()));
    }

    private static List<AssemblerDescriptor>
            buildCicdAssemblers() {
        Set<Platform> shared = Set.of(Platform.SHARED);
        return List.of(
                desc("CicdAssembler",
                        AssemblerTarget.ROOT,
                        shared,
                        new CicdAssembler()),
                desc("EpicReportAssembler",
                        AssemblerTarget.ROOT,
                        shared,
                        new EpicReportAssembler()),
                desc("PlanTemplatesAssembler",
                        AssemblerTarget.ROOT,
                        shared,
                        new PlanTemplatesAssembler()),
                desc("ReadmeAssembler",
                        AssemblerTarget.CLAUDE,
                        Set.of(Platform.CLAUDE_CODE),
                        new ReadmeAssembler()));
    }

    private static AssemblerDescriptor desc(
            String name,
            AssemblerTarget target,
            Set<Platform> platforms,
            Assembler assembler) {
        return new AssemblerDescriptor(
                name, target, platforms, assembler);
    }
}
