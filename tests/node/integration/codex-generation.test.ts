/**
 * Codex end-to-end integration tests — validates AGENTS.md and config.toml
 * generation across diverse config fixtures, plus regression and determinism.
 */
import { describe, it, expect, beforeAll, afterAll } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { loadConfig } from "../../../src/config.js";
import { runPipeline } from "../../../src/assembler/pipeline.js";
import type { PipelineResult } from "../../../src/models.js";
import { RESOURCES_DIR } from "../../helpers/integration-constants.js";
import {
  assertAgentsMdContains,
  assertAgentsMdNotContains,
  assertValidToml,
  assertDirsIdentical,
} from "../../helpers/codex-helpers.js";

const FIXTURES_DIR = path.resolve(
  __dirname, "../../fixtures/codex",
);

function fixtureConfigPath(name: string): string {
  return path.join(FIXTURES_DIR, `${name}.yaml`);
}

describe("Codex generation integration", { timeout: 60000 }, () => {
  describe("codex-full config", () => {
    let tmpDir: string;
    let result: PipelineResult;
    let agentsMd: string;
    let configToml: string;

    beforeAll(async () => {
      tmpDir = fs.mkdtempSync(
        path.join(tmpdir(), "codex-full-"),
      );
      const config = loadConfig(fixtureConfigPath("codex-full"));
      const outputDir = path.join(tmpDir, "output");
      result = await runPipeline(
        config, RESOURCES_DIR, outputDir, false,
      );
      agentsMd = fs.readFileSync(
        path.join(outputDir, "AGENTS.md"), "utf-8",
      );
      configToml = fs.readFileSync(
        path.join(outputDir, ".codex", "config.toml"), "utf-8",
      );
    });

    afterAll(() => {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it("pipelineSucceeds_codexFull", () => {
      expect(result.success).toBe(true);
    });

    it("agentsMdContainsDomainSection_codexFull", () => {
      assertAgentsMdContains(agentsMd, "## Domain");
    });

    it("agentsMdContainsSecuritySection_codexFull", () => {
      assertAgentsMdContains(agentsMd, "lgpd");
    });

    it("agentsMdContainsArchitectureSection_codexFull", () => {
      assertAgentsMdContains(agentsMd, "## Architecture");
    });

    it("agentsMdContainsTechStackSection_codexFull", () => {
      assertAgentsMdContains(agentsMd, "## Tech Stack");
    });

    it("agentsMdContainsCommandsSection_codexFull", () => {
      assertAgentsMdContains(agentsMd, "## Commands");
    });

    it("agentsMdContainsCodingStandards_codexFull", () => {
      assertAgentsMdContains(agentsMd, "## Coding Standards");
    });

    it("agentsMdContainsQualityGates_codexFull", () => {
      assertAgentsMdContains(agentsMd, "## Quality Gates");
    });

    it("agentsMdContainsConventions_codexFull", () => {
      assertAgentsMdContains(agentsMd, "## Conventions");
    });

    it("agentsMdContainsSkillsSection_codexFull", () => {
      assertAgentsMdContains(agentsMd, "## Available Skills");
    });

    it("agentsMdContainsAgentPersonas_codexFull", () => {
      assertAgentsMdContains(agentsMd, "## Agent Personas");
    });

    it("configTomlIsValidToml_codexFull", () => {
      assertValidToml(configToml);
    });

    it("configTomlContainsMcpServers_codexFull", () => {
      const parsed = assertValidToml(configToml);
      expect(parsed).toHaveProperty("mcp_servers");
    });

    it("configTomlContainsTestServer_codexFull", () => {
      expect(configToml).toContain("test-server");
    });

    it("configTomlApprovalOnRequest_codexFull", () => {
      expect(configToml).toContain('approval_policy = "on-request"');
    });

    it("filesGeneratedIncludesCodexArtifacts_codexFull", () => {
      const codexFiles = result.filesGenerated.filter(
        (f) => f.includes(".codex"),
      );
      expect(codexFiles.length).toBeGreaterThanOrEqual(1);
      const rootAgentsMd = result.filesGenerated.filter(
        (f) => f.endsWith("AGENTS.md") && !f.includes(".codex"),
      );
      expect(rootAgentsMd.length).toBe(1);
    });
  });

  describe("codex-minimal config", () => {
    let tmpDir: string;
    let result: PipelineResult;
    let agentsMd: string;
    let configToml: string;

    beforeAll(async () => {
      tmpDir = fs.mkdtempSync(
        path.join(tmpdir(), "codex-minimal-"),
      );
      const config = loadConfig(
        fixtureConfigPath("codex-minimal"),
      );
      const outputDir = path.join(tmpDir, "output");
      result = await runPipeline(
        config, RESOURCES_DIR, outputDir, false,
      );
      agentsMd = fs.readFileSync(
        path.join(outputDir, "AGENTS.md"), "utf-8",
      );
      configToml = fs.readFileSync(
        path.join(outputDir, ".codex", "config.toml"), "utf-8",
      );
    });

    afterAll(() => {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it("pipelineSucceeds_codexMinimal", () => {
      expect(result.success).toBe(true);
    });

    it("agentsMdNotContainsDomain_codexMinimal", () => {
      assertAgentsMdNotContains(agentsMd, "## Domain");
    });

    it("agentsMdNotContainsSecurity_codexMinimal", () => {
      assertAgentsMdNotContains(agentsMd, "## Security");
    });

    it("agentsMdContainsArchitecture_codexMinimal", () => {
      assertAgentsMdContains(agentsMd, "## Architecture");
    });

    it("configTomlIsValid_codexMinimal", () => {
      assertValidToml(configToml);
    });

    it("configTomlNoMcpServers_codexMinimal", () => {
      expect(configToml).not.toContain("[mcp_servers");
    });

    it("configTomlApprovalOnRequest_codexMinimal", () => {
      expect(configToml).toContain('approval_policy = "on-request"');
    });
  });

  describe("codex-java-spring config", () => {
    let tmpDir: string;
    let agentsMd: string;
    let configToml: string;

    beforeAll(async () => {
      tmpDir = fs.mkdtempSync(
        path.join(tmpdir(), "codex-java-spring-"),
      );
      const config = loadConfig(
        fixtureConfigPath("codex-java-spring"),
      );
      const outputDir = path.join(tmpDir, "output");
      await runPipeline(
        config, RESOURCES_DIR, outputDir, false,
      );
      agentsMd = fs.readFileSync(
        path.join(outputDir, "AGENTS.md"), "utf-8",
      );
      configToml = fs.readFileSync(
        path.join(outputDir, ".codex", "config.toml"), "utf-8",
      );
    });

    afterAll(() => {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it("agentsMdContainsCommands_codexJavaSpring", () => {
      assertAgentsMdContains(agentsMd, "## Commands");
    });

    it("agentsMdContainsGradleOrMaven_codexJavaSpring", () => {
      const hasGradle = agentsMd.includes("gradle");
      const hasMaven = agentsMd.includes("mvn");
      expect(hasGradle || hasMaven).toBe(true);
    });

    it("agentsMdContainsDomain_codexJavaSpring", () => {
      assertAgentsMdContains(agentsMd, "## Domain");
    });

    it("configTomlIsValid_codexJavaSpring", () => {
      assertValidToml(configToml);
    });

    it("configTomlApprovalOnRequest_codexJavaSpring", () => {
      expect(configToml).toContain('approval_policy = "on-request"');
    });
  });

  describe("codex-python-fastapi config", () => {
    let tmpDir: string;
    let configToml: string;

    beforeAll(async () => {
      tmpDir = fs.mkdtempSync(
        path.join(tmpdir(), "codex-python-fastapi-"),
      );
      const config = loadConfig(
        fixtureConfigPath("codex-python-fastapi"),
      );
      const outputDir = path.join(tmpDir, "output");
      await runPipeline(
        config, RESOURCES_DIR, outputDir, false,
      );
      configToml = fs.readFileSync(
        path.join(outputDir, ".codex", "config.toml"), "utf-8",
      );
    });

    afterAll(() => {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it("configTomlIsValid_codexPythonFastapi", () => {
      assertValidToml(configToml);
    });

    it("configTomlContainsMcpServers_codexPythonFastapi", () => {
      expect(configToml).toContain("firecrawl");
    });
  });

  describe("codex-no-hooks config", () => {
    let tmpDir: string;
    let configToml: string;

    beforeAll(async () => {
      tmpDir = fs.mkdtempSync(
        path.join(tmpdir(), "codex-no-hooks-"),
      );
      const config = loadConfig(
        fixtureConfigPath("codex-no-hooks"),
      );
      const outputDir = path.join(tmpDir, "output");
      await runPipeline(
        config, RESOURCES_DIR, outputDir, false,
      );
      configToml = fs.readFileSync(
        path.join(outputDir, ".codex", "config.toml"), "utf-8",
      );
    });

    afterAll(() => {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it("configTomlApprovalUntrusted_codexNoHooks", () => {
      expect(configToml).toContain('approval_policy = "untrusted"');
    });

    it("configTomlIsValid_codexNoHooks", () => {
      assertValidToml(configToml);
    });
  });

  describe("determinism — repeated execution", () => {
    let tmpDir: string;
    let codex1: string;
    let codex2: string;

    beforeAll(async () => {
      tmpDir = fs.mkdtempSync(
        path.join(tmpdir(), "codex-determinism-"),
      );
      const config = loadConfig(fixtureConfigPath("codex-full"));
      const out1 = path.join(tmpDir, "run1");
      const out2 = path.join(tmpDir, "run2");
      await runPipeline(config, RESOURCES_DIR, out1, false);
      await runPipeline(config, RESOURCES_DIR, out2, false);
      codex1 = path.join(out1, ".codex");
      codex2 = path.join(out2, ".codex");
    });

    afterAll(() => {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it("repeatedExecution_producesIdenticalCodexOutput", () => {
      assertDirsIdentical(codex1, codex2);
    });
  });

  describe("regression — .claude/ and .github/ unaffected", () => {
    let tmpDir: string;
    let claude1: string;
    let claude2: string;
    let github1: string;
    let github2: string;

    beforeAll(async () => {
      tmpDir = fs.mkdtempSync(
        path.join(tmpdir(), "codex-regression-"),
      );
      const config = loadConfig(fixtureConfigPath("codex-full"));
      const out1 = path.join(tmpDir, "run1");
      const out2 = path.join(tmpDir, "run2");
      await runPipeline(config, RESOURCES_DIR, out1, false);
      await runPipeline(config, RESOURCES_DIR, out2, false);
      claude1 = path.join(out1, ".claude");
      claude2 = path.join(out2, ".claude");
      github1 = path.join(out1, ".github");
      github2 = path.join(out2, ".github");
    });

    afterAll(() => {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    });

    it("claudeOutput_identicalBetweenRuns", () => {
      assertDirsIdentical(claude1, claude2);
    });

    it("githubOutput_identicalBetweenRuns", () => {
      assertDirsIdentical(github1, github2);
    });
  });
});
