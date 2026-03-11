import { describe, it, expect, beforeAll, afterAll } from "vitest";
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
  describe.each(CONFIG_PROFILES)("profile: %s", (profileName) => {
    let tmpDir: string;
    const hasGolden = goldenExists(profileName);

    beforeAll(async () => {
      tmpDir = fs.mkdtempSync(
        path.join(tmpdir(), "e2e-verify-"),
      );
    });

    afterAll(() => {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it.skipIf(!hasGolden)("fullFlowForProfile", async () => {
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
    });
  });
});
