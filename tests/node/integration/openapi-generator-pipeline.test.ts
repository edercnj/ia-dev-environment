import { describe, it, expect, beforeAll, afterAll } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { loadConfig } from "../../../src/config.js";
import { runPipeline } from "../../../src/assembler/pipeline.js";
import type { PipelineResult } from "../../../src/models.js";
import {
  CONFIG_TEMPLATES_DIR,
  RESOURCES_DIR,
} from "../../helpers/integration-constants.js";

const REST_PROFILES = [
  "go-gin", "java-quarkus", "java-spring",
  "kotlin-ktor", "python-fastapi", "rust-axum",
  "typescript-nestjs",
] as const;

const NON_REST_PROFILE = "python-click-cli";

const REF_FILE = "skills/x-dev-lifecycle/references/openapi-generator.md";

describe("openapi-generator pipeline integration", { timeout: 60000 }, () => {
  describe("REST profiles include openapi-generator reference", () => {
    for (const profile of REST_PROFILES) {
      describe(`profile: ${profile}`, () => {
        let tmpDir: string;
        let result: PipelineResult;

        beforeAll(async () => {
          tmpDir = fs.mkdtempSync(
            path.join(tmpdir(), `openapi-pipe-${profile}-`),
          );
          const configPath = path.join(
            CONFIG_TEMPLATES_DIR,
            `setup-config.${profile}.yaml`,
          );
          const config = loadConfig(configPath);
          const outputDir = path.join(tmpDir, "output");
          result = await runPipeline(
            config, RESOURCES_DIR, outputDir, false,
          );
        });

        afterAll(() => {
          fs.rmSync(tmpDir, { recursive: true, force: true });
        });

        it(`pipelineSuccess_${profile}`, () => {
          expect(result.success).toBe(true);
        });

        it(`claudeOutput_includesOpenapiGenerator_${profile}`, () => {
          const outputDir = path.join(tmpDir, "output");
          const filePath = path.join(outputDir, ".claude", REF_FILE);
          expect(fs.existsSync(filePath)).toBe(true);
        });

        it(`agentsOutput_includesOpenapiGenerator_${profile}`, () => {
          const outputDir = path.join(tmpDir, "output");
          const filePath = path.join(outputDir, ".agents", REF_FILE);
          expect(fs.existsSync(filePath)).toBe(true);
        });

        it(`githubOutput_includesOpenapiGenerator_${profile}`, () => {
          const outputDir = path.join(tmpDir, "output");
          const filePath = path.join(outputDir, ".github", REF_FILE);
          expect(fs.existsSync(filePath)).toBe(true);
        });

        it(`placeholdersResolved_noUnresolvedSingleBrace_${profile}`, () => {
          const outputDir = path.join(tmpDir, "output");
          const filePath = path.join(outputDir, ".claude", REF_FILE);
          const content = fs.readFileSync(filePath, "utf-8");
          expect(content).not.toMatch(/\{framework_name\}/);
          expect(content).not.toMatch(/\{language_name\}/);
          expect(content).not.toMatch(/\{project_name\}/);
        });
      });
    }
  });

  describe("non-REST profile includes template with runtime skip", () => {
    let tmpDir: string;
    let result: PipelineResult;

    beforeAll(async () => {
      tmpDir = fs.mkdtempSync(
        path.join(tmpdir(), "openapi-pipe-cli-"),
      );
      const configPath = path.join(
        CONFIG_TEMPLATES_DIR,
        `setup-config.${NON_REST_PROFILE}.yaml`,
      );
      const config = loadConfig(configPath);
      const outputDir = path.join(tmpDir, "output");
      result = await runPipeline(
        config, RESOURCES_DIR, outputDir, false,
      );
    });

    afterAll(() => {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it("pipelineSuccess_pythonClickCli", () => {
      expect(result.success).toBe(true);
    });

    it("claudeOutput_includesTemplate_coreSkillUnconditional", () => {
      const outputDir = path.join(tmpDir, "output");
      const filePath = path.join(outputDir, ".claude", REF_FILE);
      expect(fs.existsSync(filePath)).toBe(true);
    });

    it("templateContent_containsSkipInstruction_runtimeGuard", () => {
      const outputDir = path.join(tmpDir, "output");
      const filePath = path.join(outputDir, ".claude", REF_FILE);
      const content = fs.readFileSync(filePath, "utf-8");
      expect(content).toMatch(/[Ss]kip/);
      expect(content).toMatch(/rest|REST/);
    });
  });
});
