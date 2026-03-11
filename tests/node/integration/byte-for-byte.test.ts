import { describe, it, expect, beforeAll, afterAll } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { loadConfig } from "../../../src/config.js";
import { runPipeline } from "../../../src/assembler/pipeline.js";
import { verifyOutput } from "../../../src/verifier.js";
import type { VerificationResult } from "../../../src/models.js";
import type { PipelineResult } from "../../../src/models.js";

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

function formatMismatches(result: VerificationResult): string {
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

describe("byte-for-byte parity", { timeout: 60000 }, () => {
  describe.each(CONFIG_PROFILES)("profile: %s", (profileName) => {
    let tmpDir: string;
    let pipelineResult: PipelineResult;
    let verification: VerificationResult | undefined;
    const hasGolden = goldenExists(profileName);

    beforeAll(async () => {
      tmpDir = fs.mkdtempSync(
        path.join(tmpdir(), "parity-test-"),
      );
      const configPath = path.join(
        CONFIG_TEMPLATES_DIR,
        `setup-config.${profileName}.yaml`,
      );
      const config = loadConfig(configPath);
      const outputDir = path.join(tmpDir, "output");
      pipelineResult = await runPipeline(
        config, RESOURCES_DIR, outputDir, false,
      );
      if (hasGolden) {
        const goldenPath = path.join(GOLDEN_DIR, profileName);
        verification = verifyOutput(outputDir, goldenPath);
      }
    });

    afterAll(() => {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it(`pipelineSuccessForProfile_${profileName}`, () => {
      expect(pipelineResult.success).toBe(true);
    });

    it.skipIf(!hasGolden)(
      `pipelineMatchesGoldenFiles_${profileName}`,
      () => {
        expect(
          verification!.success,
          formatMismatches(verification!),
        ).toBe(true);
      },
    );

    it.skipIf(!hasGolden)(
      `noMissingFiles_${profileName}`,
      () => {
        expect(verification!.missingFiles).toEqual([]);
      },
    );

    it.skipIf(!hasGolden)(
      `noExtraFiles_${profileName}`,
      () => {
        expect(verification!.extraFiles).toEqual([]);
      },
    );

    it.skipIf(!hasGolden)(
      `totalFilesGreaterThanZero_${profileName}`,
      () => {
        expect(verification!.totalFiles).toBeGreaterThan(0);
      },
    );
  });
});
