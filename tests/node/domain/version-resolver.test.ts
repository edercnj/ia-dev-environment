import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { mkdirSync, rmSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { findVersionDir } from "../../../src/domain/version-resolver.js";

describe("findVersionDir", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = join(tmpdir(), `version-test-${Date.now()}`);
    mkdirSync(tmpDir, { recursive: true });
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("exactMatch_returnsExactDir", () => {
    mkdirSync(join(tmpDir, "java-21.0.1"));
    const result = findVersionDir(tmpDir, "java", "21.0.1");
    expect(result).toBe(join(tmpDir, "java-21.0.1"));
  });

  it("noExactMatch_fallbackToMajorX", () => {
    mkdirSync(join(tmpDir, "java-21.x"));
    const result = findVersionDir(tmpDir, "java", "21.0.1");
    expect(result).toBe(join(tmpDir, "java-21.x"));
  });

  it("bothExist_prefersExactMatch", () => {
    mkdirSync(join(tmpDir, "java-21.0.1"));
    mkdirSync(join(tmpDir, "java-21.x"));
    const result = findVersionDir(tmpDir, "java", "21.0.1");
    expect(result).toBe(join(tmpDir, "java-21.0.1"));
  });

  it("neitherExists_returnsUndefined", () => {
    const result = findVersionDir(tmpDir, "java", "21.0.1");
    expect(result).toBeUndefined();
  });

  it("singleVersionNumber_fallbackWorksCorrectly", () => {
    mkdirSync(join(tmpDir, "python-3.x"));
    const result = findVersionDir(tmpDir, "python", "3.11");
    expect(result).toBe(join(tmpDir, "python-3.x"));
  });

  it("versionWithoutDot_usesFullVersionAsMajor", () => {
    mkdirSync(join(tmpDir, "go-1.x"));
    const result = findVersionDir(tmpDir, "go", "1");
    // exact "go-1" doesn't exist, fallback "go-1.x" does
    expect(result).toBe(join(tmpDir, "go-1.x"));
  });
});
