import {
  existsSync,
  readFileSync,
  readdirSync,
  statSync,
} from "node:fs";
import { join, relative } from "node:path";
import { FileDiff, VerificationResult } from "./models.js";

export const BINARY_DIFF_MESSAGE = "<binary files differ>";
export const MAX_DIFF_LINES = 200;
const BINARY_PROBE_BYTES = 8192;

/** @internal Validate that a path exists and is a directory. */
export function validateDirectory(
  dirPath: string,
  name: string,
): void {
  if (!existsSync(dirPath)) {
    throw new Error(`${name} does not exist: ${dirPath}`);
  }
  if (!statSync(dirPath).isDirectory()) {
    throw new Error(`${name} is not a directory: ${dirPath}`);
  }
}

/** @internal Walk directory recursively, return sorted relative paths. */
export function collectRelativePaths(
  baseDir: string,
): string[] {
  const entries = readdirSync(baseDir, {
    recursive: true,
    withFileTypes: true,
  });
  const paths: string[] = [];
  for (const entry of entries) {
    if (!entry.isFile()) continue;
    const parentDir = entry.parentPath ?? entry.path;
    const fullPath = join(parentDir, entry.name);
    const relPath = relative(baseDir, fullPath)
      .split("\\").join("/");
    paths.push(relPath);
  }
  return paths.sort();
}

function isBinaryBuffer(buffer: Buffer): boolean {
  for (let i = 0; i < Math.min(buffer.length, BINARY_PROBE_BYTES); i++) {
    if (buffer[i] === 0) return true;
  }
  return false;
}

function generateTextDiff(
  actualBuf: Buffer,
  refBuf: Buffer,
  relativePath: string,
): string {
  if (isBinaryBuffer(actualBuf) || isBinaryBuffer(refBuf)) {
    return BINARY_DIFF_MESSAGE;
  }
  const actualLines = actualBuf.toString("utf-8").split("\n");
  const refLines = refBuf.toString("utf-8").split("\n");
  return buildUnifiedDiff(
    refLines, actualLines, relativePath,
  );
}

function buildUnifiedDiff(
  refLines: string[],
  actualLines: string[],
  relativePath: string,
): string {
  const header = [
    `--- reference/${relativePath}`,
    `+++ actual/${relativePath}`,
  ];
  const diffLines = computeDiffLines(refLines, actualLines);
  const limited = diffLines.slice(0, MAX_DIFF_LINES);
  return [...header, ...limited].join("\n");
}

function computeDiffLines(
  refLines: string[],
  actualLines: string[],
): string[] {
  const lines: string[] = [];
  const maxLen = Math.max(refLines.length, actualLines.length);
  for (let i = 0; i < maxLen; i++) {
    const refLine = i < refLines.length ? refLines[i] : undefined;
    const actLine = i < actualLines.length ? actualLines[i] : undefined;
    if (refLine === actLine) {
      lines.push(` ${refLine ?? ""}`);
    } else {
      if (refLine !== undefined) lines.push(`-${refLine}`);
      if (actLine !== undefined) lines.push(`+${actLine}`);
    }
  }
  return lines;
}

function compareFiles(
  actualFile: string,
  referenceFile: string,
  relativePath: string,
): FileDiff | null {
  const actualBuf = readFileSync(actualFile);
  const refBuf = readFileSync(referenceFile);
  if (actualBuf.equals(refBuf)) return null;
  const diffText = generateTextDiff(
    actualBuf, refBuf, relativePath,
  );
  return new FileDiff(
    relativePath, diffText,
    actualBuf.length, refBuf.length,
  );
}

function findMismatches(
  actualDir: string,
  referenceDir: string,
  commonPaths: string[],
): FileDiff[] {
  const mismatches: FileDiff[] = [];
  for (const relPath of commonPaths) {
    const result = compareFiles(
      join(actualDir, relPath),
      join(referenceDir, relPath),
      relPath,
    );
    if (result !== null) mismatches.push(result);
  }
  return mismatches;
}

function computeSetDifference(
  source: Set<string>,
  target: Set<string>,
): string[] {
  const diff: string[] = [];
  for (const item of source) {
    if (!target.has(item)) diff.push(item);
  }
  return diff.sort();
}

function computeSetIntersection(
  setA: Set<string>,
  setB: Set<string>,
): string[] {
  const common: string[] = [];
  for (const item of setA) {
    if (setB.has(item)) common.push(item);
  }
  return common.sort();
}

/**
 * Compare two directory trees byte-for-byte.
 *
 * @param actualDir - Directory produced by the TypeScript CLI
 * @param referenceDir - Golden reference directory
 * @throws Error if either directory does not exist or is not a directory
 */
export function verifyOutput(
  actualDir: string,
  referenceDir: string,
): VerificationResult {
  validateDirectory(actualDir, "actualDir");
  validateDirectory(referenceDir, "referenceDir");
  const actualPaths = new Set(collectRelativePaths(actualDir));
  const refPaths = new Set(collectRelativePaths(referenceDir));
  const missing = computeSetDifference(refPaths, actualPaths);
  const extra = computeSetDifference(actualPaths, refPaths);
  const common = computeSetIntersection(refPaths, actualPaths);
  const totalFiles = common.length + missing.length + extra.length;
  const mismatches = findMismatches(
    actualDir, referenceDir, common,
  );
  const success = mismatches.length === 0
    && missing.length === 0 && extra.length === 0;
  return new VerificationResult(
    success, totalFiles, mismatches, missing, extra,
  );
}
