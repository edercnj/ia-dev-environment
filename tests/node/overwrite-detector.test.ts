import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { mkdtemp, rm, mkdir } from "node:fs/promises";
import { writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import {
  checkExistingArtifacts,
  formatConflictMessage,
} from "../../src/overwrite-detector.js";

let tempDir: string;

beforeEach(async () => {
  tempDir = await mkdtemp(
    join(tmpdir(), "overwrite-detector-test-"),
  );
});

afterEach(async () => {
  await rm(tempDir, { recursive: true, force: true });
});

describe("checkExistingArtifacts", () => {
  it("checkExistingArtifacts_emptyDir_returnsNoConflicts", () => {
    const result = checkExistingArtifacts(tempDir);

    expect(result.hasConflicts).toBe(false);
    expect(result.conflictDirs).toEqual([]);
  });

  it("checkExistingArtifacts_withClaudeOnly_returnsOneConflict", async () => {
    await mkdir(join(tempDir, ".claude"), { recursive: true });

    const result = checkExistingArtifacts(tempDir);

    expect(result.hasConflicts).toBe(true);
    expect(result.conflictDirs).toEqual([".claude/"]);
  });

  it("checkExistingArtifacts_withClaudeAndGithub_returnsBothConflicts", async () => {
    await mkdir(join(tempDir, ".claude"), { recursive: true });
    await mkdir(join(tempDir, ".github"), { recursive: true });

    const result = checkExistingArtifacts(tempDir);

    expect(result.hasConflicts).toBe(true);
    expect(result.conflictDirs).toHaveLength(2);
    expect(result.conflictDirs).toContain(".claude/");
    expect(result.conflictDirs).toContain(".github/");
  });

  it("checkExistingArtifacts_withNonArtifactFiles_returnsNoConflicts", async () => {
    await mkdir(join(tempDir, "src"), { recursive: true });
    writeFileSync(
      join(tempDir, "package.json"), "{}", "utf-8",
    );

    const result = checkExistingArtifacts(tempDir);

    expect(result.hasConflicts).toBe(false);
    expect(result.conflictDirs).toEqual([]);
  });

  it("checkExistingArtifacts_withAllArtifactDirs_returnsAllConflicts", async () => {
    await mkdir(join(tempDir, ".claude"), { recursive: true });
    await mkdir(join(tempDir, ".github"), { recursive: true });
    await mkdir(join(tempDir, ".codex"), { recursive: true });
    await mkdir(join(tempDir, ".agents"), { recursive: true });
    await mkdir(join(tempDir, "docs"), { recursive: true });

    const result = checkExistingArtifacts(tempDir);

    expect(result.hasConflicts).toBe(true);
    expect(result.conflictDirs).toHaveLength(5);
    expect(result.conflictDirs).toContain(".claude/");
    expect(result.conflictDirs).toContain(".github/");
    expect(result.conflictDirs).toContain(".codex/");
    expect(result.conflictDirs).toContain(".agents/");
    expect(result.conflictDirs).toContain("docs/");
  });

  it("checkExistingArtifacts_nonexistentDir_returnsNoConflicts", () => {
    const nonexistent = join(tempDir, "does-not-exist");

    const result = checkExistingArtifacts(nonexistent);

    expect(result.hasConflicts).toBe(false);
    expect(result.conflictDirs).toEqual([]);
  });
});

describe("formatConflictMessage", () => {
  it("formatConflictMessage_withConflictDirs_returnsFormattedMessage", () => {
    const message = formatConflictMessage([
      ".claude/", ".github/", "docs/",
    ]);

    expect(message).toContain(
      "Output directory contains existing generated artifacts:",
    );
    expect(message).toContain("  - .claude/ (exists)");
    expect(message).toContain("  - .github/ (exists)");
    expect(message).toContain("  - docs/ (exists)");
    expect(message).toContain(
      "Use --force to overwrite existing files",
    );
    expect(message).toContain(
      "or specify a different --output-dir.",
    );
  });

  it("formatConflictMessage_withSingleDir_returnsFormattedMessage", () => {
    const message = formatConflictMessage([".claude/"]);

    expect(message).toContain(
      "Output directory contains existing generated artifacts:",
    );
    expect(message).toContain("  - .claude/ (exists)");
    expect(message).toContain("--force");
  });
});
