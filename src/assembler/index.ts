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
