import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { loadConfig } from "../../../src/config.js";
import { runPipeline } from "../../../src/assembler/pipeline.js";
import { verifyOutput } from "../../../src/verifier.js";
import {
  CONFIG_PROFILES,
  CONFIG_TEMPLATES_DIR,
  GOLDEN_DIR,
  RESOURCES_DIR,
  formatVerificationFailures,
  goldenExists,
} from "../../helpers/integration-constants.js";

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
        formatVerificationFailures(verification),
      ).toBe(true);
    },
  );
});
