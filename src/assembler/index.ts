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
