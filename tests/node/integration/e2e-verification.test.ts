import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { loadConfig } from "../../../src/config.js";
import { runPipeline } from "../../../src/assembler/pipeline.js";
import { verifyOutput } from "../../../src/verifier.js";
import type { VerificationResult } from "../../../src/models.js";

const CONFIG_PROFILES = [
  "go-gin",
  "java-quarkus",
  "java-spring",
  "kotlin-ktor",
  "python-click-cli",
  "python-fastapi",
  "rust-axum",
  "typescript-nestjs",
] as const;

const PROJECT_ROOT = path.resolve(__dirname, "../../..");
const CONFIG_TEMPLATES_DIR = path.join(
  PROJECT_ROOT, "resources", "config-templates",
);
const GOLDEN_DIR = path.join(PROJECT_ROOT, "tests", "golden");
const RESOURCES_DIR = path.join(PROJECT_ROOT, "resources");

function formatFailures(result: VerificationResult): string {
  const lines: string[] = [];
  for (const m of result.mismatches) {
    lines.push(
      `MISMATCH: ${m.path} (actual=${m.pythonSize}B, ref=${m.referenceSize}B)`,
    );
    lines.push(m.diff.slice(0, 500));
  }
  for (const p of result.missingFiles) {
    lines.push(`MISSING: ${p}`);
  }
  for (const p of result.extraFiles) {
    lines.push(`EXTRA: ${p}`);
  }
  return lines.join("\n");
}

function goldenExists(profileName: string): boolean {
  return fs.existsSync(path.join(GOLDEN_DIR, profileName));
}

describe("E2E verification", { timeout: 60000 }, () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "e2e-verify-"),
    );
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it.each(CONFIG_PROFILES)(
    "fullFlowForProfile_%s",
    async (profileName) => {
      if (!goldenExists(profileName)) return;
      const configPath = path.join(
        CONFIG_TEMPLATES_DIR,
        `setup-config.${profileName}.yaml`,
      );
      const config = loadConfig(configPath);
      const outputDir = path.join(tmpDir, "output");
      const pipelineResult = await runPipeline(
        config, RESOURCES_DIR, outputDir, false,
      );
      expect(pipelineResult.success).toBe(true);
      const goldenPath = path.join(GOLDEN_DIR, profileName);
      const verification = verifyOutput(outputDir, goldenPath);
      expect(
        verification.success,
        formatFailures(verification),
      ).toBe(true);
    },
  );
});
