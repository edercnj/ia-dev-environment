## Pre-Commit Chain — Canonical Error Matrix

This table is the single source of truth for error handling across the
pre-commit chain (`format -> lint -> compile -> commit`). It is referenced
from `x-git-commit`, `x-code-format`, and `x-code-lint`.

When any of those three skills documents error behavior, link to this file
instead of duplicating the rows.

| Step | Error Condition | Exit Code | Expected Skill Behavior |
| :--- | :--- | :--- | :--- |
| format | Formatter binary not installed | `FORMAT_TOOL_MISSING` | Abort with install hint; do not proceed to lint |
| format | Formatter run fails (non-zero exit, non-diff) | `FORMAT_FAILED` | Report stderr verbatim; do not auto-retry |
| format | Formatter rewrote files | `FORMAT_APPLIED` (soft) | Continue to lint; commit will include the rewrites |
| lint | Linter binary not installed | `LINT_TOOL_MISSING` | Abort with install hint; do not proceed to compile |
| lint | Linter reports errors at configured severity | `LINT_ERRORS` | Abort; surface the first N findings; suggest `--fix` if supported |
| lint | Linter warnings only (no errors) | `LINT_WARN` (soft) | Continue; include warnings in commit body if `--strict` |
| compile | Compiler / build tool not installed | `COMPILE_TOOL_MISSING` | Abort with stack-specific hint (`{{BUILD_TOOL}}`) |
| compile | Compilation fails | `COMPILE_FAILED` | Abort; surface compiler stderr; do NOT commit partial tree |
| compile | Compilation succeeds with warnings | `COMPILE_WARN` (soft) | Continue; treat as error only if Rule 05 mandates zero warnings |
| commit | Working tree has no changes | `NOTHING_TO_COMMIT` | Exit cleanly with "no changes" message; not a failure |
| commit | Conventional-Commits validation fails | `BAD_COMMIT_MESSAGE` | Abort; surface which rule failed (type / scope / subject length) |
| commit | Pre-commit hook other than this chain fails | `HOOK_FAILED` | Abort; never bypass with `--no-verify` (Rule 07) |

## Soft vs. Hard Exit Codes

- **Hard** (abort) codes force the chain to stop immediately and surface the
  error to the caller. The next step does NOT run.
- **Soft** codes (marked `(soft)` above) are informational and allow the chain
  to continue. They may be escalated to hard via `--strict` flags where
  documented.

## Forbidden

- Skipping hooks with `--no-verify` or `--no-gpg-sign` to "unblock" the chain.
  Rule 07 forbids this; fix the underlying failure instead.
- Catching one of these exit codes and mapping it to a generic `EXIT_1`. The
  caller (e.g., `x-story-implement`) distinguishes failure modes based on
  these codes; losing the distinction breaks retry/remediation logic.
- Hand-rolling an equivalent table in a specific skill's `SKILL.md` or
  `references/*.md`. The whole point of this snippet is single-source-of-truth.
