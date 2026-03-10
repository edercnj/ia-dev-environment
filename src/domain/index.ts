export interface DomainModule {
  readonly id: string;
}

export const DOMAIN_LAYER = "domain";

// --- STORY-006: Domain mappings and constants ---
export * from "./resolved-stack.js";
export * from "./stack-mapping.js";
export * from "./stack-pack-mapping.js";
export * from "./pattern-mapping.js";
export * from "./protocol-mapping.js";
export * from "./core-kp-routing.js";
export * from "./version-resolver.js";
