import { resolve } from "node:path";

export interface RuntimePaths {
  readonly cwd: string;
  readonly outputDir: string;
  readonly resourcesDir: string;
}

const DEFAULT_OUTPUT_DIR = "dist";
const DEFAULT_RESOURCES_DIR = "resources";

export function createRuntimePaths(cwd: string = process.cwd()): RuntimePaths {
  return Object.freeze({
    cwd,
    outputDir: resolve(cwd, DEFAULT_OUTPUT_DIR),
    resourcesDir: resolve(cwd, DEFAULT_RESOURCES_DIR),
  });
}
