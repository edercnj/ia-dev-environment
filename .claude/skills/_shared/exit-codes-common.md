## Common Exit Codes — Shared Families

This snippet documents the three exit-code families that recur across
orchestrating skills (`x-release`, `x-epic-implement`, `x-story-implement`, and
downstream dispatchers). Per-skill codes are documented in each skill's own
`SKILL.md`; this file owns the cross-skill families.

## DEP_* — Dependency Unresolved

| Code | Trigger | Recovery |
| :--- | :--- | :--- |
| `DEP_NOT_FOUND` | Upstream story / epic / task file does not exist on disk | Confirm ID; create the artifact if planning is missing |
| `DEP_NOT_COMPLETE` | Upstream story / task is referenced but has `Status != Concluida` | Complete the upstream first; do NOT force through with `--skip-deps` |
| `DEP_PR_UNMERGED` | Upstream PR is open; current work depends on its merge | Wait for merge; re-run the caller when `gh pr view --json state` reports MERGED |
| `DEP_CYCLIC` | A dependency graph cycle is detected between artifacts | Fix the planning error; one of the edges must be removed or inverted |

## STATE_* — Execution State Corruption

| Code | Trigger | Recovery |
| :--- | :--- | :--- |
| `STATE_FILE_MISSING` | `execution-state.json` expected but absent | Re-initialize from PR statuses via `gh pr view` per resume protocol |
| `STATE_FILE_CORRUPT` | JSON parse fails or required field missing | Archive the bad file; re-initialize; log the corruption for audit |
| `STATE_VERSION_MISMATCH` | `schemaVersion` does not match the caller's expectation | Run the migration for that version; do NOT silently down-convert |
| `STATE_TASK_UNKNOWN` | Resume references a TASK-ID not declared in the current task breakdown | Planning artifact drift; regenerate the task breakdown from the story |

## RULE_* — Policy Violations

| Code | Trigger | Recovery |
| :--- | :--- | :--- |
| `RULE_BRANCH_PROTECTED` | Attempted direct commit / force push to `main` or `develop` | Use a feature branch and a PR; Rule 09 forbids direct pushes |
| `RULE_COMMIT_NON_CONVENTIONAL` | Commit message does not match Conventional-Commits grammar | Rewrite the message; Rule 08 is non-negotiable |
| `RULE_COVERAGE_BELOW_THRESHOLD` | Line < 85% OR Branch < 80% at merge time | Add tests until thresholds are met; Rule 05 quality gate |
| `RULE_SCOPE_VIOLATION` | Change touches files outside the declared story / task File Footprint | Shrink the change or update the footprint intentionally |
| `RULE_SKIP_HOOK` | Commit invoked with `--no-verify` or equivalent bypass | Fix the underlying hook failure; hook bypasses are forbidden |

## Usage Contract

Skills consuming this snippet:

- Link to this file when the exit code is one of the families above.
- Do NOT redefine the same code with different semantics in their own docs.
- MAY extend with skill-specific codes (e.g., `x-release` owns `REL_*`) and
  document those in the skill itself.

## Exit-Code Invariants

- Exit code strings are **stable across versions** — CI pipelines and callers
  grep for these strings. Renaming requires a deprecation cycle.
- Exit codes are **uppercase with underscores** (`DEP_NOT_FOUND`). Lower-case
  or camelCase is not recognized by downstream classifiers.
- Each code SHOULD be accompanied by a human-readable message on the same
  line, formatted as `<CODE>: <message>`. Callers grep for `<CODE>:` and
  surface the trailing message to the operator.
