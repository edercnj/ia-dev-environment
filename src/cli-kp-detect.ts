/**
 * Knowledge pack detection — determines if a skill file
 * belongs to a knowledge pack by reading SKILL.md content.
 *
 * Extracted from cli-display.ts to keep modules under 250 lines.
 *
 * @module
 */
import { existsSync, readFileSync } from "node:fs";
import { basename, dirname, join } from "node:path";

const SKILL_MD_FILENAME = "SKILL.md";
const KP_MARKER_INVOCABLE = "user-invocable: false";
const KP_MARKER_HEADING = "# Knowledge Pack";

/**
 * Resolve the SKILL.md path for a given file path.
 *
 * @param filePath - Path to a file within a skill directory.
 * @returns The resolved SKILL.md path, or undefined if not found.
 */
function resolveSkillMdPath(filePath: string): string | undefined {
  if (!existsSync(filePath)) {
    return undefined;
  }
  if (basename(filePath) === SKILL_MD_FILENAME) {
    return filePath;
  }
  const candidate = join(dirname(filePath), SKILL_MD_FILENAME);
  return existsSync(candidate) ? candidate : undefined;
}

/**
 * Check whether a SKILL.md content indicates a knowledge pack.
 *
 * @param content - The SKILL.md file content.
 * @returns true if the content matches knowledge pack markers.
 */
function isKnowledgePackContent(content: string): boolean {
  if (content.includes(KP_MARKER_INVOCABLE)) {
    return true;
  }
  return content.trimStart().startsWith(KP_MARKER_HEADING);
}

/**
 * Check whether a skill file belongs to a knowledge pack.
 *
 * If the file is not SKILL.md itself, looks for SKILL.md in the same directory.
 * Returns true if SKILL.md contains "user-invocable: false"
 * or starts with "# Knowledge Pack".
 *
 * @param filePath - Absolute or relative path to a file within a skill directory.
 * @param cache - Optional cache to avoid redundant SKILL.md reads.
 * @returns true if the file belongs to a knowledge pack, false otherwise.
 */
export function isKnowledgePackFile(
  filePath: string,
  cache?: Map<string, boolean>,
): boolean {
  const skillMdPath = resolveSkillMdPath(filePath);
  if (skillMdPath === undefined) {
    return false;
  }

  if (cache !== undefined) {
    const cached = cache.get(skillMdPath);
    if (cached !== undefined) {
      return cached;
    }
  }

  let content: string;
  try {
    content = readFileSync(skillMdPath, "utf-8");
  } catch {
    if (cache !== undefined) {
      cache.set(skillMdPath, false);
    }
    return false;
  }
  const result = isKnowledgePackContent(content);

  if (cache !== undefined) {
    cache.set(skillMdPath, result);
  }

  return result;
}
