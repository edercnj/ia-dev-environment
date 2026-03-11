import { mkdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";

/**
 * Create a file tree from a mapping of relative paths to content.
 * Automatically creates intermediate directories.
 */
export function createFileTree(
  baseDir: string,
  files: Record<string, string>,
): void {
  for (const [relativePath, content] of Object.entries(files)) {
    const fullPath = join(baseDir, relativePath);
    mkdirSync(dirname(fullPath), { recursive: true });
    writeFileSync(fullPath, content, "utf-8");
  }
}

/**
 * Create a binary file at the given path.
 * Automatically creates intermediate directories.
 */
export function createBinaryFile(
  filePath: string,
  data: Buffer,
): void {
  mkdirSync(dirname(filePath), { recursive: true });
  writeFileSync(filePath, data);
}
