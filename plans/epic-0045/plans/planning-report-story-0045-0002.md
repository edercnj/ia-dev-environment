# Story Planning Report -- story-0045-0002

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0045-0002 |
| Epic ID | 0045 |
| Date | 2026-04-20 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story 0045-0002 delivers Rule 20 (CI-Watch) formalizing RULE-045-01 (v2 default) + RULE-045-02 (v1 no-op), `scripts/audit-rule-20.sh` grep-based regression guard, Java wrapper tests, RuleAssembler integration, and CLAUDE.md "In progress" update. 33 TASK_PROPOSAL entries consolidated into 28 final tasks.

## Architecture Assessment

- **Affected source files:** `rules/20-ci-watch.md` (NEW source-of-truth), `RulesAssembler.java` (register rule), `CoreRulesWriter.java` (if enumerated), `RulesAssemblerCiWatchTest.java` (NEW, mirrors Telemetry test), `scripts/audit-rule-20.sh` (NEW grep audit), `Rule20AuditTest.java` (NEW Java wrapper with @TempDir fixtures), goldens regen across targets, `CLAUDE.md` In-progress block.
- **Slot 20 coexistence:** `20-interactive-gates.md`, `20-telemetry-privacy.md`, and new `20-ci-watch.md` share slot 20 (documented precedent — alphabetical ordering within slot).
- **Dependency direction:** Assembler is application layer; scripts are cross-cutting tooling; Java wrapper is test-only.
- **Rule body outline:** Rule + Fallback Matrix (V1 × V2 × `--no-ci-watch`) + Rationale + Enforcement + Forbidden; explicit V1 no-op row closes Rule 19 compat.

## Test Strategy Summary

- **Outer loop:** Rule 20 golden diff IT (byte-identical assembly) + Rule20AuditMavenIT wiring into `mvn verify`.
- **Inner loop (TPP):** 6-level audit script test progression — nil (empty repo → 0) / constant (compliant → 0) / scalar (violator → 1) / conditional (opt-out → 0) / collection (mixed → enumerate all violators) / iteration (real repo smoke).
- **Fixtures:** `@TempDir` with synthetic SKILL.md; no mutation of real skills.
- **Coverage target:** ≥ 95% Line / ≥ 90% Branch on `Rule20AuditTest` harness.

## Security Assessment Summary

- **OWASP mapping:** CWE-78 (OS command injection), CWE-22 (path traversal), CWE-209 (info disclosure via stderr).
- **5 controls:** bash `set -euo pipefail` + no `eval`/backticks (TASK-017); `readonly SKILLS_DIR` + `grep -F` + no symlink follow (TASK-018); `ProcessBuilder` argv-list in Java wrapper (TASK-019); stderr truncation to relative paths + 8 KiB cap (TASK-020); no env/secret echo with canary-value test (TASK-021).
- **Risk level:** Low. Read-only script over tracked files; attack surface narrow.

## Implementation Approach

Tech Lead enforces: shellcheck-clean bash with documented exit codes (0/1/2); Rule 20 markdown matches Rule 13/19 scaffolding (5 sections, `> Related:` cross-ref, no emoji); assembler ordering preserves Rules 01–19 byte-identical; Rule 19 compat via explicit V1 no-op clause in Fallback Matrix; CLAUDE.md diff scoped to In-Progress block (≤ 10 lines); Conventional Commits per task (`docs(task-0045-0002-NNN)`, `feat(...)`, `test(...)`).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 28 |
| Architecture tasks (ARCH) | 5 (rule file, assembler, copy-verbatim test, audit script, CLAUDE.md) |
| Test tasks (QA) | 7 (assembler test + 6 audit TPP levels + Maven IT) |
| Security tasks (SEC) | 5 (bash hardening, path confinement, ProcessBuilder, stderr truncation, no-secret) |
| Quality gate tasks (TL) | 6 (shellcheck, rule format, assembler ordering, Rule 19 compat, CLAUDE.md scope, Conv Commits) |
| Validation tasks (PO) | 3 (RULE-045-01/02 trace, audit msg actionable, CLAUDE.md test) |
| Merged tasks | 2 (audit GREEN merged across QA+ARCH; goldens merged into ARCH lifecycle) |

## Consolidated Risk Matrix

| Risk | Source | Severity | Likelihood | Mitigation |
|------|--------|----------|------------|------------|
| Rule 19 regression (v1 epic breaks on Rule 20 enforcement) | TechLead | High | Low | TASK-025 explicit V1 no-op in Fallback Matrix + Forbidden section |
| Slot-20 collision breaks assembler ordering | Architect | Medium | Low | TASK-024 assembler ordering test; Rules 01–19 byte-identical baseline |
| Audit exit-code drift (bash quirks) | PO | Medium | Medium | TASK-022 documented exit codes 0/1/2 + Rule20AuditExitCodesTest |
| Stderr leaks absolute paths / CI secrets | Security | Medium | Low | TASK-020 relative path truncation + 8 KiB cap; TASK-021 canary value test |
| False negatives from grep matching prose mentions | QA | Medium | Medium | TASK-018 `grep -F` fixed-string + canonical `Skill(skill: "x-pr-create"` pattern; exclude Triggers/Examples sections |
| Opt-out abuse (silent regression disguised as `--no-ci-watch`) | PO | Low | Medium | Deferred: TASK-004 (from PO-004 proposal) not adopted yet; optionally require rationale comment via future story |

## DoR Status

See `dor-story-0045-0002.md`.
