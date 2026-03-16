export interface Assembler {
  assemble(): Promise<void>;
}

export const ASSEMBLER_LAYER = "assembler";

// --- STORY-008: Assembler helpers ---
export * from "./auditor.js";
export * from "./conditions.js";
export * from "./consolidator.js";
export * from "./copy-helpers.js";

// --- STORY-009: RulesAssembler ---
export * from "./rules-assembler.js";
export * from "./rules-identity.js";
export * from "./rules-conditionals.js";

// --- STORY-010: SkillsAssembler ---
export * from "./skills-selection.js";
export * from "./skills-assembler.js";

// --- STORY-011: AgentsAssembler ---
export * from "./agents-selection.js";
export * from "./agents-assembler.js";

// --- STORY-012: PatternsAssembler + ProtocolsAssembler ---
export * from "./patterns-assembler.js";
export * from "./protocols-assembler.js";

// --- STORY-013: HooksAssembler + SettingsAssembler ---
export * from "./hooks-assembler.js";
export * from "./settings-assembler.js";

// --- STORY-014: GitHub Assemblers ---
export * from "./github-hooks-assembler.js";
export * from "./github-mcp-assembler.js";
export * from "./github-prompts-assembler.js";
export * from "./github-agents-assembler.js";
export * from "./github-skills-assembler.js";
export * from "./github-instructions-assembler.js";

// --- STORY-015: ReadmeAssembler ---
export * from "./readme-assembler.js";

// --- STORY-022 + STORY-023: Codex assemblers ---
export * from "./codex-shared.js";
export * from "./codex-agents-md-assembler.js";
export * from "./codex-config-assembler.js";
export * from "./codex-skills-assembler.js";

// --- STORY-0004-0011: CicdAssembler ---
export * from "./cicd-assembler.js";

// --- STORY-016: Pipeline Orchestrator ---
export * from "./pipeline.js";
