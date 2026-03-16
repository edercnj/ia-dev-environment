/**
 * Shared helpers for markdown content parsing in skill template tests.
 *
 * Extracts sections and YAML frontmatter from SKILL.md files.
 * Used by any test that validates skill template structure.
 */

/**
 * Extract the body of a `## heading` section from markdown content.
 * Returns the text between the target heading and the next `## ` heading
 * (but not `### ` subheadings). Returns empty string when the heading
 * is not found or content is empty.
 */
export function extractSection(
  content: string,
  heading: string,
): string {
  const parts = content.split(`## ${heading}`);
  if (parts.length < 2) return "";
  return parts[1]!.split(/\n## (?!#)/)[0]!;
}

/**
 * Extract YAML frontmatter (the text between the opening and closing
 * `---` delimiters). Returns empty string when frontmatter is absent
 * or malformed.
 */
export function extractFrontmatter(content: string): string {
  const match = content.match(/^---\n([\s\S]*?)\n---/);
  return match ? match[1]! : "";
}
