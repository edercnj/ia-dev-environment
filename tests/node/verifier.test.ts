import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import {
  verifyOutput,
  validateDirectory,
  collectRelativePaths,
} from "../../src/verifier.js";
import { createFileTree, createBinaryFile } from "../helpers/file-tree.js";

let tmpDir: string;

beforeEach(() => {
  tmpDir = fs.mkdtempSync(path.join(tmpdir(), "verifier-test-"));
});

afterEach(() => {
  fs.rmSync(tmpDir, { recursive: true, force: true });
});

describe("verifyOutput", () => {
  it("verifyOutput_identicalDirs_returnsSuccess", () => {
    const actualDir = path.join(tmpDir, "actual");
    const refDir = path.join(tmpDir, "reference");
    const files = { "a.txt": "hello", "b.txt": "world" };
    createFileTree(actualDir, files);
    createFileTree(refDir, files);

    const result = verifyOutput(actualDir, refDir);

    expect(result.success).toBe(true);
  });

  it("verifyOutput_identicalDirs_totalFilesCorrect", () => {
    const actualDir = path.join(tmpDir, "actual");
    const refDir = path.join(tmpDir, "reference");
    const files = { "a.txt": "a", "b.txt": "b", "c.txt": "c" };
    createFileTree(actualDir, files);
    createFileTree(refDir, files);

    const result = verifyOutput(actualDir, refDir);

    expect(result.totalFiles).toBe(3);
  });

  it("verifyOutput_mismatchDetected_returnsFailure", () => {
    const actualDir = path.join(tmpDir, "actual");
    const refDir = path.join(tmpDir, "reference");
    createFileTree(actualDir, { "a.txt": "new" });
    createFileTree(refDir, { "a.txt": "old" });

    const result = verifyOutput(actualDir, refDir);

    expect(result.success).toBe(false);
    expect(result.mismatches).toHaveLength(1);
  });

  it("verifyOutput_mismatchContainsDiffString", () => {
    const actualDir = path.join(tmpDir, "actual");
    const refDir = path.join(tmpDir, "reference");
    createFileTree(actualDir, { "a.txt": "new\n" });
    createFileTree(refDir, { "a.txt": "old\n" });

    const result = verifyOutput(actualDir, refDir);
    const diffText = result.mismatches[0]!.diff;

    expect(diffText).toContain("---");
    expect(diffText).toContain("+++");
  });

  it("verifyOutput_mismatchContainsFileSizes", () => {
    const actualDir = path.join(tmpDir, "actual");
    const refDir = path.join(tmpDir, "reference");
    createFileTree(actualDir, { "a.txt": "short" });
    createFileTree(refDir, { "a.txt": "much longer text" });

    const result = verifyOutput(actualDir, refDir);
    const mismatch = result.mismatches[0]!;

    expect(mismatch.pythonSize).toBe(5);
    expect(mismatch.referenceSize).toBe(16);
  });

  it("verifyOutput_missingFileDetected", () => {
    const actualDir = path.join(tmpDir, "actual");
    const refDir = path.join(tmpDir, "reference");
    createFileTree(actualDir, { "a.txt": "a" });
    createFileTree(refDir, { "a.txt": "a", "b.txt": "b" });

    const result = verifyOutput(actualDir, refDir);

    expect(result.success).toBe(false);
    expect(result.missingFiles).toContain("b.txt");
  });

  it("verifyOutput_extraFileDetected", () => {
    const actualDir = path.join(tmpDir, "actual");
    const refDir = path.join(tmpDir, "reference");
    createFileTree(actualDir, { "a.txt": "a", "c.txt": "c" });
    createFileTree(refDir, { "a.txt": "a" });

    const result = verifyOutput(actualDir, refDir);

    expect(result.success).toBe(false);
    expect(result.extraFiles).toContain("c.txt");
  });

  it("verifyOutput_nestedDirectoriesCompared", () => {
    const actualDir = path.join(tmpDir, "actual");
    const refDir = path.join(tmpDir, "reference");
    const files = { "sub/dir/file.txt": "content" };
    createFileTree(actualDir, files);
    createFileTree(refDir, files);

    const result = verifyOutput(actualDir, refDir);

    expect(result.success).toBe(true);
    expect(result.totalFiles).toBe(1);
  });

  it("verifyOutput_emptyDirs_returnsSuccess", () => {
    const actualDir = path.join(tmpDir, "actual");
    const refDir = path.join(tmpDir, "reference");
    fs.mkdirSync(actualDir);
    fs.mkdirSync(refDir);

    const result = verifyOutput(actualDir, refDir);

    expect(result.success).toBe(true);
    expect(result.totalFiles).toBe(0);
  });

  it("verifyOutput_binaryFileMismatch_handled", () => {
    const actualDir = path.join(tmpDir, "actual");
    const refDir = path.join(tmpDir, "reference");
    fs.mkdirSync(actualDir);
    fs.mkdirSync(refDir);
    createBinaryFile(
      path.join(actualDir, "img.bin"),
      Buffer.from([0x00, 0x01, 0x02]),
    );
    createBinaryFile(
      path.join(refDir, "img.bin"),
      Buffer.from([0x00, 0x01, 0xff]),
    );

    const result = verifyOutput(actualDir, refDir);

    expect(result.success).toBe(false);
    expect(result.mismatches).toHaveLength(1);
    expect(result.mismatches[0]!.diff.toLowerCase()).toContain("binary");
  });

  it("verifyOutput_whitespaceDifferenceDetected", () => {
    const actualDir = path.join(tmpDir, "actual");
    const refDir = path.join(tmpDir, "reference");
    createFileTree(actualDir, { "a.txt": "hello " });
    createFileTree(refDir, { "a.txt": "hello" });

    const result = verifyOutput(actualDir, refDir);

    expect(result.success).toBe(false);
    expect(result.mismatches).toHaveLength(1);
  });

  it("verifyOutput_mixedFailures_allReported", () => {
    const actualDir = path.join(tmpDir, "actual");
    const refDir = path.join(tmpDir, "reference");
    createFileTree(actualDir, {
      "common.txt": "changed",
      "extra.txt": "extra",
    });
    createFileTree(refDir, {
      "common.txt": "original",
      "missing.txt": "miss",
    });

    const result = verifyOutput(actualDir, refDir);

    expect(result.success).toBe(false);
    expect(result.mismatches).toHaveLength(1);
    expect(result.missingFiles).toContain("missing.txt");
    expect(result.extraFiles).toContain("extra.txt");
  });
});

describe("collectRelativePaths", () => {
  it("collectRelativePaths_returnsSortedPaths", () => {
    createFileTree(tmpDir, {
      "c.txt": "c", "a.txt": "a", "b.txt": "b",
    });

    const paths = collectRelativePaths(tmpDir);

    expect(paths).toEqual(["a.txt", "b.txt", "c.txt"]);
  });

  it("collectRelativePaths_includesNestedFiles", () => {
    createFileTree(tmpDir, {
      "root.txt": "r", "sub/nested.txt": "n",
    });

    const paths = collectRelativePaths(tmpDir);

    expect(paths).toContain("sub/nested.txt");
  });

  it("collectRelativePaths_emptyDir_returnsEmpty", () => {
    const paths = collectRelativePaths(tmpDir);

    expect(paths).toEqual([]);
  });
});

describe("validateDirectory", () => {
  it("validateDirectory_nonexistentDir_throwsError", () => {
    const refDir = path.join(tmpDir, "ref");
    fs.mkdirSync(refDir);

    expect(() => verifyOutput(path.join(tmpDir, "nope"), refDir))
      .toThrow(/does not exist/);
  });

  it("validateDirectory_nonexistentReferenceDir_throwsError", () => {
    const actDir = path.join(tmpDir, "act");
    fs.mkdirSync(actDir);

    expect(() => verifyOutput(actDir, path.join(tmpDir, "nope")))
      .toThrow(/does not exist/);
  });

  it("validateDirectory_fileNotDir_throwsError", () => {
    const filePath = path.join(tmpDir, "not_a_dir");
    fs.writeFileSync(filePath, "content");
    const refDir = path.join(tmpDir, "ref");
    fs.mkdirSync(refDir);

    expect(() => verifyOutput(filePath, refDir))
      .toThrow(/not a directory/);
  });

  it("validateDirectory_existingDir_noError", () => {
    const dir = path.join(tmpDir, "valid");
    fs.mkdirSync(dir);

    expect(() => validateDirectory(dir, "test")).not.toThrow();
  });
});
