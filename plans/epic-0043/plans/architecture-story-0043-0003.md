# Architecture Plan — story-0043-0003

## Change Scope: Simplified

## 1. Affected Components

| File | Change |
|------|--------|
| `java/src/main/resources/targets/claude/skills/core/dev/x-story-implement/SKILL.md` | Rewrite Phase 0.5 gate + Phase 2.2.9 gate + CLI args + execution-state schema |
| `java/src/main/resources/targets/claude/skills/core/dev/x-epic-implement/SKILL.md` | Add `--non-interactive` to all `x-story-implement` invocations |
| All `java/src/test/resources/golden/*/.claude/skills/x-story-implement/SKILL.md` | Regen (19 golden profiles) |
| All `java/src/test/resources/golden/*/.claude/skills/x-epic-implement/SKILL.md` | Regen (19 golden profiles) |

## 2. Key Design Decisions

1. **Invert default**: menu replaces auto-approve as the default; `--non-interactive` becomes the opt-out.
2. **REJECT vs FIX-PR**: Phase 0.5 gate has no PR → uses REJECT variant (slot 2) per Rule 20 §Canonical.
3. **ABORT semantics for task PR gate**: `status = PR_CREATED` (not FAILED), making it resume-able.
4. **PAUSE absorption**: old PAUSE option absorbed into ABORT with resume-able state.
5. **Deprecation**: `--manual-contract-approval` and `--manual-task-approval` become no-ops with warning.
6. **`allowed-tools` update**: add `AskUserQuestion` to x-story-implement frontmatter.

## 3. Architecture Compliance

- All gate menus use exactly 3 slots per Rule 20 §Canonical Option Menu
- FIX-PR handler uses Rule 13 Pattern 1 INLINE-SKILL
- `--non-interactive` preserves backward compatibility for CI/automation
