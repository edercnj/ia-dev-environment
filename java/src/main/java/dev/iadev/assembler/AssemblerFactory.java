package dev.iadev.assembler;

import java.util.List;

/**
 * Factory that instantiates the 23 assemblers in the
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
     * Builds the ordered list of 23 assemblers per RULE-005.
     *
     * <p>Order: Rules, Skills, Agents, Patterns, Protocols,
     * Hooks, Settings, GithubInstructions, GithubMcp,
     * GithubSkills, GithubAgents, GithubHooks,
     * GithubPrompts, Docs, GrpcDocs, Runbook,
     * CodexAgentsMd, CodexConfig, CodexSkills, DocsAdr,
     * Cicd, EpicReport, Readme</p>
     *
     * @return immutable ordered list of assembler descriptors
     */
    public static List<AssemblerDescriptor> buildAssemblers() {
        return List.of(
                new AssemblerDescriptor(
                        "RulesAssembler",
                        AssemblerTarget.CLAUDE,
                        new RulesAssembler()),
                new AssemblerDescriptor(
                        "SkillsAssembler",
                        AssemblerTarget.CLAUDE,
                        new SkillsAssembler()),
                new AssemblerDescriptor(
                        "AgentsAssembler",
                        AssemblerTarget.CLAUDE,
                        new AgentsAssembler()),
                new AssemblerDescriptor(
                        "PatternsAssembler",
                        AssemblerTarget.CLAUDE,
                        new PatternsAssembler()),
                new AssemblerDescriptor(
                        "ProtocolsAssembler",
                        AssemblerTarget.CLAUDE,
                        new ProtocolsAssembler()),
                new AssemblerDescriptor(
                        "HooksAssembler",
                        AssemblerTarget.CLAUDE,
                        new HooksAssembler()),
                new AssemblerDescriptor(
                        "SettingsAssembler",
                        AssemblerTarget.CLAUDE,
                        new SettingsAssembler()),
                new AssemblerDescriptor(
                        "GithubInstructionsAssembler",
                        AssemblerTarget.GITHUB,
                        new GithubInstructionsAssembler()),
                new AssemblerDescriptor(
                        "GithubMcpAssembler",
                        AssemblerTarget.GITHUB,
                        new GithubMcpAssembler()),
                new AssemblerDescriptor(
                        "GithubSkillsAssembler",
                        AssemblerTarget.GITHUB,
                        new GithubSkillsAssembler()),
                new AssemblerDescriptor(
                        "GithubAgentsAssembler",
                        AssemblerTarget.GITHUB,
                        new GithubAgentsAssembler()),
                new AssemblerDescriptor(
                        "GithubHooksAssembler",
                        AssemblerTarget.GITHUB,
                        new GithubHooksAssembler()),
                new AssemblerDescriptor(
                        "GithubPromptsAssembler",
                        AssemblerTarget.GITHUB,
                        new GithubPromptsAssembler()),
                new AssemblerDescriptor(
                        "DocsAssembler",
                        AssemblerTarget.DOCS,
                        new DocsAssembler()),
                new AssemblerDescriptor(
                        "GrpcDocsAssembler",
                        AssemblerTarget.DOCS,
                        new GrpcDocsAssembler()),
                new AssemblerDescriptor(
                        "RunbookAssembler",
                        AssemblerTarget.ROOT,
                        new RunbookAssembler()),
                new AssemblerDescriptor(
                        "CodexAgentsMdAssembler",
                        AssemblerTarget.ROOT,
                        new CodexAgentsMdAssembler()),
                new AssemblerDescriptor(
                        "CodexConfigAssembler",
                        AssemblerTarget.CODEX,
                        new CodexConfigAssembler()),
                new AssemblerDescriptor(
                        "CodexSkillsAssembler",
                        AssemblerTarget.CODEX_AGENTS,
                        new CodexSkillsAssembler()),
                new AssemblerDescriptor(
                        "DocsAdrAssembler",
                        AssemblerTarget.ROOT,
                        new DocsAdrAssembler()),
                new AssemblerDescriptor(
                        "CicdAssembler",
                        AssemblerTarget.ROOT,
                        new CicdAssembler()),
                new AssemblerDescriptor(
                        "EpicReportAssembler",
                        AssemblerTarget.ROOT,
                        new EpicReportAssembler()),
                new AssemblerDescriptor(
                        "ReadmeAssembler",
                        AssemblerTarget.CLAUDE,
                        new ReadmeAssembler()));
    }
}
