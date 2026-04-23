# x-internal-report-write — Full Protocol

Carve-out companion for [`../SKILL.md`](../SKILL.md) per ADR-0007. The
main SKILL.md is the normative contract; this file is reserved for
future expansion (extended workflow pipelines, jq recipes,
troubleshooting matrix). At the time story-0049-0007 landed the main
SKILL.md was already comprehensive, so this page exists primarily to
satisfy the `SkillSizeLinter` references-sibling requirement while
keeping a clear seam for follow-up stories to migrate detail out of
the 500-line window.

## §1 — Renderer pipeline (reference)

The canonical rendering order is:

1. Read template body from disk (`.claude/templates/<name>` or
   absolute path).
2. Parse `--data` as inline JSON or `@path` file.
3. Expand all `{{#each <collection>}} … {{/each}}` blocks inside-out.
4. Substitute remaining `{{<dot-path>}}` placeholders via `jq -r`.
5. Write rendered body to the target path via atomic `mv` (overwrite
   mode) OR merge by `## ID:` marker (`--append=true`).

The canonical Bash skeleton lives in SKILL.md §Workflow — this section
is reserved for future expansion into full jq recipes and
troubleshooting matrix.

## §2 — Relationship with sibling internal skills

| Skill | Shared behaviour | Distinct behaviour |
| :--- | :--- | :--- |
| `x-internal-status-update` (pilot, 0049-0005) | Convention anchors, `visibility: internal`, Bash-only toolset | Mutates markdown Status fields; no template rendering |
| `x-internal-args-normalize` (0049-0007) | Convention anchors, stdout JSON envelope, no file writes (this one DOES write files) | Parses argv against schema; no template rendering |

## §3 — Follow-up carve-out opportunities

Candidate sections to migrate out of the main SKILL.md in a future
story if/when it grows past 500 lines again:

- The 6 worked examples (§Examples 1–7).
- The full Outputs / Error Handling tables.
- The Testing scenario catalogue (10 scenarios).

Until then, the main SKILL.md remains the single source of truth for
the skill's contract; orchestrators MUST read only SKILL.md for
invocation semantics.
