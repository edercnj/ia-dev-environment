```
ENGINEER: Security
STORY: STORY-0003-0010
SCORE: 20/20
STATUS: Approved
---
PASSED:
- [1] Input validation (2/2) — N/A — no code changes; all modifications are static markdown content in skill templates and golden files
- [2] Output encoding (2/2) — N/A — no code changes; no dynamic output rendering introduced
- [3] Authentication checks (2/2) — N/A — no code changes; no authentication logic affected
- [4] Authorization checks (2/2) — N/A — no code changes; no authorization logic affected
- [5] Sensitive data masking (2/2) — N/A — no code changes; added content contains no secrets, credentials, tokens, or PII
- [6] Error handling / no stack traces (2/2) — N/A — no code changes; no error handling paths modified
- [7] Cryptography usage (2/2) — N/A — no code changes; no cryptographic operations introduced or modified
- [8] Dependency vulnerabilities (2/2) — N/A — no dependencies added, removed, or updated; no package.json or lock file changes
- [9] CORS/CSP headers (2/2) — N/A — no code changes; no HTTP configuration affected
- [10] Audit logging (2/2) — N/A — no code changes; no audit-relevant operations introduced
FAILED:
(none)
PARTIAL:
(none)
```

## Review Notes

**Scope:** 26 markdown files modified across 3 categories:
- 2 source templates (`resources/skills-templates/`, `resources/github-skills-templates/`)
- 24 golden file copies (`tests/golden/{profile}/.agents/`, `.claude/`, `.github/`)

**Change summary:** Each file receives the same 8 lines of markdown content:
1. **Step 2 addition (5 lines):** TDD cross-cutting rules block (Red-Green-Refactor, Atomic TDD Commits, Gherkin Completeness)
2. **Step 4 addition (2 lines):** TDD Compliance and Double-Loop TDD as Definition of Done criteria

**Security assessment:** This changeset poses zero security risk. All modifications are static instructional markdown consumed by AI agents during story decomposition. No executable code, no configuration changes, no dependency changes, no secrets, and no user-facing input/output paths are affected.
