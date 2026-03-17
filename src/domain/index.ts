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

// --- STORY-007: Domain validator, resolver & skill registry ---
export * from "./validator.js";
export * from "./resolver.js";
export * from "./skill-registry.js";

// --- STORY-0005-0004: Implementation Map Parser ---
export * from "./implementation-map/index.js";

// --- STORY-0005-0012: Dry-run planner and formatter ---
export * from "./dry-run/index.js";
