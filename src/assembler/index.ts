export interface Assembler {
  assemble(): Promise<void>;
}

export const ASSEMBLER_LAYER = "assembler";
