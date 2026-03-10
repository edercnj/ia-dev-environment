import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import * as os from "node:os";
import {
  auditRulesContext,
  MAX_FILE_COUNT,
  MAX_TOTAL_BYTES,
} from "../../../src/assembler/auditor.js";

let tmpDir: string;

beforeEach(() => {
  tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "auditor-"));
});

afterEach(() => {
  fs.rmSync(tmpDir, { recursive: true, force: true });
});

function createMdFile(name: string, sizeBytes: number): void {
  const content = "x".repeat(sizeBytes);
  fs.writeFileSync(path.join(tmpDir, name), content);
}

describe("auditRulesContext", () => {
  it("auditRulesContext_nonExistentDir_returnsEmpty", () => {
    const result = auditRulesContext("/no/such/directory");
    expect(result.totalFiles).toBe(0);
    expect(result.totalBytes).toBe(0);
    expect(result.fileSizes).toEqual([]);
    expect(result.warnings).toEqual([]);
  });

  it("auditRulesContext_emptyDir_returnsZeroCounts", () => {
    const result = auditRulesContext(tmpDir);
    expect(result.totalFiles).toBe(0);
    expect(result.totalBytes).toBe(0);
    expect(result.warnings).toEqual([]);
  });

  it("auditRulesContext_withinThresholds_noWarnings", () => {
    createMdFile("01-rule.md", 100);
    createMdFile("02-rule.md", 200);

    const result = auditRulesContext(tmpDir);

    expect(result.totalFiles).toBe(2);
    expect(result.totalBytes).toBe(300);
    expect(result.warnings).toHaveLength(0);
  });

  it("auditRulesContext_exceedsFileCount_warnsAboutFiles", () => {
    for (let i = 0; i < 12; i++) {
      createMdFile(`rule-${String(i).padStart(2, "0")}.md`, 10);
    }

    const result = auditRulesContext(tmpDir);

    expect(result.totalFiles).toBe(12);
    expect(result.warnings).toHaveLength(1);
    expect(result.warnings[0]).toContain("12 rule files");
    expect(result.warnings[0]).toContain(
      `${MAX_FILE_COUNT}`,
    );
  });

  it("auditRulesContext_exceedsTotalBytes_warnsAboutSize", () => {
    createMdFile("big-rule.md", 60_000);

    const result = auditRulesContext(tmpDir);

    expect(result.totalFiles).toBe(1);
    expect(result.totalBytes).toBe(60_000);
    expect(result.warnings).toHaveLength(1);
    expect(result.warnings[0]).toContain("KB total rules");
    expect(result.warnings[0]).toContain("50KB");
  });

  it("auditRulesContext_exceedsBothThresholds_twoWarnings", () => {
    for (let i = 0; i < 11; i++) {
      createMdFile(`rule-${String(i).padStart(2, "0")}.md`, 5_000);
    }

    const result = auditRulesContext(tmpDir);

    expect(result.totalFiles).toBe(11);
    expect(result.totalBytes).toBe(55_000);
    expect(result.warnings).toHaveLength(2);
  });

  it("auditRulesContext_fileSizesSortedDescending_largestFirst", () => {
    createMdFile("small.md", 50);
    createMdFile("large.md", 500);
    createMdFile("medium.md", 200);

    const result = auditRulesContext(tmpDir);

    expect(result.fileSizes[0]![0]).toBe("large.md");
    expect(result.fileSizes[1]![0]).toBe("medium.md");
    expect(result.fileSizes[2]![0]).toBe("small.md");
  });

  it("auditRulesContext_ignoresNonMdFiles_onlyCountsMd", () => {
    createMdFile("rule.md", 100);
    fs.writeFileSync(
      path.join(tmpDir, "notes.txt"),
      "ignored",
    );

    const result = auditRulesContext(tmpDir);

    expect(result.totalFiles).toBe(1);
  });

  it("auditRulesContext_exactlyAtThreshold_noWarnings", () => {
    for (let i = 0; i < MAX_FILE_COUNT; i++) {
      createMdFile(`rule-${i}.md`, 10);
    }

    const result = auditRulesContext(tmpDir);

    expect(result.totalFiles).toBe(MAX_FILE_COUNT);
    expect(result.warnings).toHaveLength(0);
  });
});
