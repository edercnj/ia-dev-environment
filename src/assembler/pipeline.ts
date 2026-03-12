/**
 * Pipeline Orchestrator — coordinates all 16 assemblers in RULE-008 order.
 *
 * Migrated from Python `assembler/__init__.py`.
 * Supports real mode (atomic output) and dry-run mode (temp dir, discard).
 *
 * @module
 */
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, relative, resolve } from "node:path";
import type { ProjectConfig } from "../models.js";
import { PipelineResult } from "../models.js";
import { PipelineError } from "../exceptions.js";
import { atomicOutput } from "../utils.js";
import { TemplateEngine } from "../template-engine.js";
import type { AssembleResult } from "./rules-assembler.js";
import { RulesAssembler } from "./rules-assembler.js";
import { SkillsAssembler } from "./skills-assembler.js";
import { AgentsAssembler } from "./agents-assembler.js";
import { PatternsAssembler } from "./patterns-assembler.js";
import { ProtocolsAssembler } from "./protocols-assembler.js";
import { HooksAssembler } from "./hooks-assembler.js";
import { SettingsAssembler } from "./settings-assembler.js";
import { GithubInstructionsAssembler } from "./github-instructions-assembler.js";
import { GithubMcpAssembler } from "./github-mcp-assembler.js";
import { GithubSkillsAssembler } from "./github-skills-assembler.js";
import { GithubAgentsAssembler } from "./github-agents-assembler.js";
import { GithubHooksAssembler } from "./github-hooks-assembler.js";
import { GithubPromptsAssembler } from "./github-prompts-assembler.js";
import { ReadmeAssembler } from "./readme-assembler.js";
import { CodexAgentsMdAssembler } from "./codex-agents-md-assembler.js";
import { CodexConfigAssembler } from "./codex-config-assembler.js";

/** Warning appended to dry-run results. */
export const DRY_RUN_WARNING = "Dry run -- no files written";

/** Target output directory for an assembler. */
export type AssemblerTarget = "claude" | "github" | "codex";

/** Pairs a display name with an assembler instance and its target directory. */
export interface AssemblerDescriptor {
  readonly name: string;
  readonly target: AssemblerTarget;
  readonly assembler: {
    assemble(
      config: ProjectConfig,
      outputDir: string,
      resourcesDir: string,
      engine: TemplateEngine,
    ): string[] | AssembleResult;
  };
}

/** Aggregated files and warnings from assembler execution. */
interface NormalizedResult {
  files: string[];
  warnings: string[];
}

/** Normalize assembler return value to { files, warnings }. */
export function normalizeResult(
  result: string[] | AssembleResult,
): NormalizedResult {
  if (Array.isArray(result)) {
    return { files: [...result], warnings: [] };
  }
  return { files: [...result.files], warnings: [...result.warnings] };
}

/** Build the ordered list of 16 assemblers per RULE-008. */
export function buildAssemblers(): readonly AssemblerDescriptor[] {
  return [
    { name: "RulesAssembler", target: "claude", assembler: new RulesAssembler() },
    { name: "SkillsAssembler", target: "claude", assembler: new SkillsAssembler() },
    { name: "AgentsAssembler", target: "claude", assembler: new AgentsAssembler() },
    { name: "PatternsAssembler", target: "claude", assembler: new PatternsAssembler() },
    { name: "ProtocolsAssembler", target: "claude", assembler: new ProtocolsAssembler() },
    { name: "HooksAssembler", target: "claude", assembler: new HooksAssembler() },
    { name: "SettingsAssembler", target: "claude", assembler: new SettingsAssembler() },
    { name: "GithubInstructionsAssembler", target: "github", assembler: new GithubInstructionsAssembler() },
    { name: "GithubMcpAssembler", target: "github", assembler: new GithubMcpAssembler() },
    { name: "GithubSkillsAssembler", target: "github", assembler: new GithubSkillsAssembler() },
    { name: "GithubAgentsAssembler", target: "github", assembler: new GithubAgentsAssembler() },
    { name: "GithubHooksAssembler", target: "github", assembler: new GithubHooksAssembler() },
    { name: "GithubPromptsAssembler", target: "github", assembler: new GithubPromptsAssembler() },
    { name: "ReadmeAssembler", target: "claude", assembler: new ReadmeAssembler() },
    { name: "CodexAgentsMdAssembler", target: "codex", assembler: new CodexAgentsMdAssembler() },
    { name: "CodexConfigAssembler", target: "codex", assembler: new CodexConfigAssembler() },
  ];
}

/** Execute assemblers sequentially, aggregating files and warnings. */
export function executeAssemblers(
  assemblers: readonly AssemblerDescriptor[],
  config: ProjectConfig,
  outputDir: string,
  resourcesDir: string,
  engine: TemplateEngine,
): NormalizedResult {
  const claudeDir = join(outputDir, ".claude");
  const githubDir = join(outputDir, ".github");
  const codexDir = join(outputDir, ".codex");
  const files: string[] = [];
  const warnings: string[] = [];
  for (const { name, target, assembler } of assemblers) {
    try {
      const targetDir = target === "github"
        ? githubDir
        : target === "codex"
          ? codexDir
          : claudeDir;
      const raw = assembler.assemble(
        config, targetDir, resourcesDir, engine,
      );
      const normalized = normalizeResult(raw);
      files.push(...normalized.files);
      warnings.push(...normalized.warnings);
    } catch (error: unknown) {
      if (error instanceof PipelineError) throw error;
      const reason = error instanceof Error
        ? error.message
        : String(error);
      throw new PipelineError(name, reason);
    }
  }
  return { files, warnings };
}

/** Execute assemblers in a temporary directory, then clean up. */
async function runDry(
  config: ProjectConfig,
  resourcesDir: string,
): Promise<NormalizedResult> {
  const tempDir = await mkdtemp(
    join(tmpdir(), "ia-dev-env-dry-"),
  );
  try {
    const engine = new TemplateEngine(resourcesDir, config);
    const result = executeAssemblers(
      buildAssemblers(), config, tempDir, resourcesDir, engine,
    );
    result.warnings.push(DRY_RUN_WARNING);
    return result;
  } finally {
    await rm(tempDir, { recursive: true, force: true });
  }
}

/** Execute pipeline with atomic output to destination directory. */
async function runReal(
  config: ProjectConfig,
  resourcesDir: string,
  outputDir: string,
): Promise<NormalizedResult> {
  const resolvedDest = resolve(outputDir);
  const { tempDir, files, warnings } = await atomicOutput(
    outputDir,
    async (tempDir) => {
      const engine = new TemplateEngine(resourcesDir, config);
      const result = executeAssemblers(
        buildAssemblers(), config, tempDir, resourcesDir, engine,
      );
      return { tempDir, ...result };
    },
  );
  return {
    files: files.map((f) => join(resolvedDest, relative(tempDir, f))),
    warnings,
  };
}

/** Orchestrate all assemblers with atomic output or dry-run. */
export async function runPipeline(
  config: ProjectConfig,
  resourcesDir: string,
  outputDir: string,
  dryRun: boolean,
): Promise<PipelineResult> {
  const start = performance.now();
  const result = dryRun
    ? await runDry(config, resourcesDir)
    : await runReal(config, resourcesDir, outputDir);
  const durationMs = Math.round(performance.now() - start);
  return new PipelineResult(
    true, outputDir, result.files, result.warnings, durationMs,
  );
}
