# Task Plan -- TASK-0034-0005-001

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0005-001 |
| Story ID | story-0034-0005 |
| Epic ID | 0034 |
| Source Agent | merged(TechLead, ProductOwner, Security) |
| Type | documentation |
| TDD Phase | VERIFY |
| TPP Level | N/A |
| Layer | cross-cutting |
| Estimated Effort | M |
| Date | 2026-04-10 |

## Objective

Update the root `CLAUDE.md` file to reflect the Claude-only generator scope. Remove ~180 lines referencing the removed GitHub Copilot, Codex, and Agents targets. Ensure the resulting file has a single coherent voice, zero residual multi-target references (except legitimate `.github/workflows/` CI/CD context), and no secrets leakage.

## Implementation Guide

### Step 1 - Identify the authoritative file

1. Run `wc -l /Users/edercnj/workspaces/ia-dev-environment/CLAUDE.md` to confirm the current line count.
2. Compare with the 283-line baseline from `plans/epic-0034/baseline-pre-epic.md`.
3. If the repo-root `CLAUDE.md` is already short (~80 lines executive summary) and the 283-line target is actually `.claude/README.md`, read the Escalation Note in `tasks-story-0034-0005.md` for TASK-001 and pivot accordingly.
4. IMPORTANT: If the file is **generated** from `java/src/main/resources/targets/claude/`, edit the source template under resources, not the generated file. Then regenerate via `ia-dev-env generate` (or the equivalent GoldenFileRegenerator flow if CLAUDE.md is a golden output).

### Step 2 - Record pre-edit measurements

```bash
wc -l CLAUDE.md > /tmp/claude-md-pre-edit.txt
grep -ciE 'copilot|codex|\.github/|\.codex/|\.agents/' CLAUDE.md >> /tmp/claude-md-pre-edit.txt
```

### Step 3 - Delete sections

Per story §3.1:

1. Delete section `### .github/ (GitHub Copilot)` (lines ~33-43 in the pre-edit state).
2. Delete columns `.github/` and `.codex/` from the mapping table `.claude/ <-> .github/ <-> .codex/ Mapping` (keep `.claude/` column only; simplify title to `.claude/ Structure` or equivalent).
3. Delete line `**Total .github/ artifacts: 52**`.
4. Delete all rows in the `Generation Summary` table that reference `.github`, Copilot, Codex, or Agents. Keep only `.claude/` rows.
5. Update the prose descriptions that introduce the mapping table and Generation Summary to drop multi-target language.
6. Search for any residual prose mention (body paragraphs) of "GitHub Copilot", "Codex", ".agents", ".github/" (excluding `.github/workflows/`) and rewrite or delete.

### Step 4 - Update counts in Generation Summary

Post-deletion, the Generation Summary should report only `.claude/` artifacts:

- Rules (.claude)
- Skills (.claude)
- Knowledge Packs (.claude)
- Agents (.claude)
- Hooks (.claude)
- Settings (.claude)
- Plan Templates (.claude)

Delete rows for Instructions (.github), Skills (.github), Agents (.github), Prompts (.github), Hooks (.github), Plan Templates (.github), MCP (.github), and any Codex/Agents entries.

### Step 5 - Grep validation

```bash
grep -iE 'copilot|codex|\.github/' CLAUDE.md \
  | grep -v '\.github/workflows/'
# Expected: 0 matches (exit 1, empty output)

grep -iE 'password|secret|token|api[-_]?key|bearer' CLAUDE.md
# Expected: 0 sensitive matches (only placeholder/example text allowed)
```

### Step 6 - Line count delta

```bash
wc -l CLAUDE.md > /tmp/claude-md-post-edit.txt
# Expected: delta >= 150 lines removed; story promises ~180
# Absolute post-edit target: <= 130 lines
```

### Step 7 - Manual read-through

Read the post-edit CLAUDE.md front to back. Confirm:

- Single coherent voice (no "GitHub Copilot" or "Codex" fragments left).
- Single-target language throughout ("the Claude Code generator", not "one of the targets").
- No dangling "## See .github/" cross-references.
- Table of contents still matches actual sections.

### Step 8 - Commit

Use Conventional Commits per Rule 08:

```
docs(claude): reduce CLAUDE.md to claude-only scope (EPIC-0034)

Remove ~180 lines referencing GitHub Copilot, Codex, and Agents targets
(removed in stories 0034-0001/0002/0003). Update Generation Summary to
report only .claude/ artifacts. Retain .github/workflows/ references in
CI/CD context per RULE-003.

Ref: EPIC-0034
```

## Definition of Done

- [ ] Pre-edit `wc -l CLAUDE.md` recorded (baseline for delta calculation)
- [ ] Section `### .github/ (GitHub Copilot)` deleted
- [ ] Mapping table columns `.github/` and `.codex/` deleted (or entire table rewritten as `.claude/`-only)
- [ ] Line `**Total .github/ artifacts: 52**` deleted
- [ ] Generation Summary rows for `.github`, Copilot, Codex, Agents deleted
- [ ] `grep -iE 'copilot|codex|\.github/' CLAUDE.md` returns 0 (except `.github/workflows/` CI context)
- [ ] `grep -iE 'password|secret|token|api[-_]?key|bearer' CLAUDE.md` returns 0 sensitive matches (CWE-798)
- [ ] Post-edit `wc -l CLAUDE.md` <= 130 (delta >= 150 lines)
- [ ] Manual read-through confirms coherent single voice
- [ ] Conventional Commit message follows format `docs(claude): ...`
- [ ] Pre-commit hooks pass (format, lint, compile -- compile is no-op for markdown)

## Dependencies

| Depends On | Reason |
|-----------|--------|
| -- | First task of the story |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| CLAUDE.md at repo root is generated output (not hand-edited) | Medium | High | Before editing, check if the file is regenerated by `ia-dev-env generate` from `java/src/main/resources/targets/claude/`. If yes, edit the source template and regenerate. Direct hand-edits will be lost. |
| Delta <150 lines (below story promise of ~180) | Low | Low | If the file was already partially cleaned, document the actual delta in the PR body. Story success metric tolerates the effective reduction; the promise is indicative not contractual. |
| Residual prose references after column/row deletion | Medium | Medium | Step 3.6 explicitly searches prose mentions. Step 5 grep is the final gate. |
| Secret accidentally introduced via paste (CWE-798) | Low | High | Step 5 grep for credentials-like tokens. Step 8 conventional commit triggers pre-commit hook which may include secret scanners. |
