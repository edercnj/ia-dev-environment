/**
 * Codex integration test helpers — assertion utilities for AGENTS.md,
 * config.toml validation, and directory comparison.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import { expect } from "vitest";
import { parse as parseToml } from "smol-toml";

/** Assert that AGENTS.md content contains a markdown heading section. */
export function assertAgentsMdContains(
  content: string, section: string,
): void {
  expect(content).toContain(section);
}

/** Assert that AGENTS.md content does NOT contain a section. */
export function assertAgentsMdNotContains(
  content: string, section: string,
): void {
  expect(content).not.toContain(section);
}

/** Parse TOML content and return the result. Throws on invalid TOML. */
export function assertValidToml(
  content: string,
): Record<string, unknown> {
  const parsed = parseToml(content);
  expect(parsed).toBeDefined();
  return parsed as Record<string, unknown>;
}

/** Recursively collect all file paths relative to a base directory. */
function collectFiles(dir: string, base: string): string[] {
  const result: string[] = [];
  if (!fs.existsSync(dir)) return result;
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isFile()) {
      result.push(path.relative(base, full));
    } else if (entry.isDirectory()) {
      result.push(...collectFiles(full, base));
    }
  }
  return result.sort();
}

/** Assert that two directories are byte-for-byte identical. */
export function assertDirsIdentical(
  dir1: string, dir2: string,
): void {
  const files1 = collectFiles(dir1, dir1);
  const files2 = collectFiles(dir2, dir2);
  expect(files1).toEqual(files2);
  for (const rel of files1) {
    const content1 = fs.readFileSync(
      path.join(dir1, rel), "utf-8",
    );
    const content2 = fs.readFileSync(
      path.join(dir2, rel), "utf-8",
    );
    expect(content1, `File mismatch: ${rel}`).toBe(content2);
  }
}
