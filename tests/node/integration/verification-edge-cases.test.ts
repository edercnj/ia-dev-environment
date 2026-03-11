import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { runPipeline } from "../../../src/assembler/pipeline.js";
import { verifyOutput } from "../../../src/verifier.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
} from "../../../src/models.js";
import { createFileTree } from "../../helpers/file-tree.js";

const PROJECT_ROOT = path.resolve(__dirname, "../../..");
const RESOURCES_DIR = path.join(PROJECT_ROOT, "resources");

function makeMinimalConfig(): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("minimal-tool", "Minimal CLI tool"),
    new ArchitectureConfig("library"),
    [new InterfaceConfig("cli")],
    new LanguageConfig("python", "3.9"),
    new FrameworkConfig("click", "8.1", "pip"),
  );
}

describe("verification edge cases", { timeout: 30000 }, () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "edge-case-test-"),
    );
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("MinimalConfig", () => {
    it("minimalConfig_producesOutput", async () => {
      const config = makeMinimalConfig();
      const outputDir = path.join(tmpDir, "output");
      const result = await runPipeline(
        config, RESOURCES_DIR, outputDir, false,
      );
      expect(result.success).toBe(true);
      expect(result.filesGenerated.length).toBeGreaterThan(0);
    });

    it("minimalConfig_verifiesAgainstSelf", async () => {
      const config = makeMinimalConfig();
      const dirA = path.join(tmpDir, "a");
      const dirB = path.join(tmpDir, "b");
      await runPipeline(config, RESOURCES_DIR, dirA, false);
      await runPipeline(config, RESOURCES_DIR, dirB, false);
      const result = verifyOutput(dirA, dirB);
      expect(result.success).toBe(true);
    });
  });

  describe("Idempotency", () => {
    it("pipeline_isIdempotent", async () => {
      const config = makeMinimalConfig();
      const dir1 = path.join(tmpDir, "run1");
      const dir2 = path.join(tmpDir, "run2");
      await runPipeline(config, RESOURCES_DIR, dir1, false);
      await runPipeline(config, RESOURCES_DIR, dir2, false);
      const result = verifyOutput(dir1, dir2);
      expect(result.success).toBe(true);
    });
  });

  describe("EmptyReference", () => {
    it("allFiles_reportedAsExtra", async () => {
      const config = makeMinimalConfig();
      const actualDir = path.join(tmpDir, "output");
      const refDir = path.join(tmpDir, "empty_ref");
      await runPipeline(
        config, RESOURCES_DIR, actualDir, false,
      );
      fs.mkdirSync(refDir);
      const result = verifyOutput(actualDir, refDir);
      expect(result.success).toBe(false);
      expect(result.extraFiles.length).toBeGreaterThan(0);
      expect(result.missingFiles).toEqual([]);
    });
  });

  describe("EmptyOutput", () => {
    it("allFiles_reportedAsMissing", () => {
      const refDir = path.join(tmpDir, "reference");
      const actualDir = path.join(tmpDir, "empty_output");
      createFileTree(refDir, {
        "a.txt": "a", "b.txt": "b",
      });
      fs.mkdirSync(actualDir);
      const result = verifyOutput(actualDir, refDir);
      expect(result.success).toBe(false);
      expect(result.missingFiles).toHaveLength(2);
      expect(result.extraFiles).toEqual([]);
    });
  });

  describe("InvalidDirectories", () => {
    it("nonexistentActualDir_throwsError", () => {
      const refDir = path.join(tmpDir, "ref");
      fs.mkdirSync(refDir);
      expect(() =>
        verifyOutput(path.join(tmpDir, "nope"), refDir),
      ).toThrow(/does not exist/);
    });

    it("nonexistentReferenceDir_throwsError", () => {
      const actDir = path.join(tmpDir, "act");
      fs.mkdirSync(actDir);
      expect(() =>
        verifyOutput(actDir, path.join(tmpDir, "nope")),
      ).toThrow(/does not exist/);
    });

    it("fileAsActualDir_throwsError", () => {
      const filePath = path.join(tmpDir, "not_a_dir");
      fs.writeFileSync(filePath, "content");
      const refDir = path.join(tmpDir, "ref");
      fs.mkdirSync(refDir);
      expect(() =>
        verifyOutput(filePath, refDir),
      ).toThrow(/not a directory/);
    });

    it("fileAsReferenceDir_throwsError", () => {
      const actDir = path.join(tmpDir, "act");
      fs.mkdirSync(actDir);
      const filePath = path.join(tmpDir, "not_a_dir");
      fs.writeFileSync(filePath, "content");
      expect(() =>
        verifyOutput(actDir, filePath),
      ).toThrow(/not a directory/);
    });
  });
});
