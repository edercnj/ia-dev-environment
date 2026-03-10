/**
 * Template file and directory copy helpers with placeholder replacement.
 *
 * @remarks
 * All operations use synchronous `node:fs` by design. This module is consumed
 * exclusively by CLI assemblers that run sequentially. Sync I/O simplifies
 * control flow and matches the Python predecessor's behavior for output parity.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { TemplateEngine } from "../template-engine.js";

/**
 * Copy a single template file with placeholder replacement.
 */
export function copyTemplateFile(
  src: string,
  dest: string,
  engine: TemplateEngine,
): string {
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  const content = fs.readFileSync(src, "utf-8");
  const replaced = engine.replacePlaceholders(content);
  fs.writeFileSync(dest, replaced, "utf-8");
  return dest;
}

/**
 * Copy a template file if source exists. Returns null if not found.
 */
export function copyTemplateFileIfExists(
  src: string,
  dest: string,
  engine: TemplateEngine,
): string | null {
  if (!fs.existsSync(src)) {
    return null;
  }
  return copyTemplateFile(src, dest, engine);
}

/**
 * Copy a directory tree with placeholder replacement in .md files.
 */
export function copyTemplateTree(
  src: string,
  dest: string,
  engine: TemplateEngine,
): string {
  fs.cpSync(src, dest, { recursive: true });
  replacePlaceholdersInDir(dest, engine);
  return dest;
}

/**
 * Copy a directory tree if source exists. Returns null if not found.
 */
export function copyTemplateTreeIfExists(
  src: string,
  dest: string,
  engine: TemplateEngine,
): string | null {
  if (!fs.existsSync(src)) {
    return null;
  }
  return copyTemplateTree(src, dest, engine);
}

/**
 * Replace placeholders in all .md files in directory recursively.
 */
export function replacePlaceholdersInDir(
  directory: string,
  engine: TemplateEngine,
): void {
  walkAndReplace(directory, engine);
}

function walkAndReplace(
  currentDir: string,
  engine: TemplateEngine,
): void {
  const entries = fs.readdirSync(currentDir, {
    withFileTypes: true,
  });

  for (const entry of entries) {
    const fullPath = path.join(currentDir, entry.name);
    if (entry.isDirectory()) {
      walkAndReplace(fullPath, engine);
      continue;
    }
    if (!entry.isFile() || !entry.name.endsWith(".md")) continue;
    const content = fs.readFileSync(fullPath, "utf-8");
    const replaced = engine.replacePlaceholders(content);
    fs.writeFileSync(fullPath, replaced, "utf-8");
  }
}
