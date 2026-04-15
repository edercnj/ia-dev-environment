# Story Planning Report -- story-0039-0008

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0039-0008 |
| Epic ID | 0039 |
| Title | Smart Resume de state files |
| Date | 2026-04-15 |
| Schema Version | v1 (legacy) |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story 0039-0008 replaces the current `STATE_CONFLICT` abort with an actionable 3-option prompt (Retomar / Abortar / Iniciar nova) when a release state file exists. Planning consolidated 6 tasks across 2 domain components (`StateFileDetector`, `SmartResumeOrchestrator`), one documentation task (SKILL.md Step 0.5), one smoke test, and 2 verification tasks (quality gate + AC validation). Dependencies on story-0039-0001 (auto-version detect) and story-0039-0002 (state file schema v2) are satisfied at the epic level.

## Architecture Assessment

- **Affected layers:** `domain` (StateFileDetector — pure age calc + file listing), `application` (SmartResumeOrchestrator — use case orchestration), `config` (SKILL.md doc).
- **New components:** `dev.iadev.release.resume.StateFileDetector`, `dev.iadev.release.resume.SmartResumeOrchestrator`.
- **Dependency direction:** Domain → zero external imports; Application depends on domain port only. Rule 04 compliant.
- **Integration:** Orchestrator invoked from the existing `/x-release` entry point (Step 0.5 inserted before auto-detect S01).
- **Implementation order:** TASK-001 (domain) → TASK-002 (application) → TASK-003 (docs) / TASK-004 (smoke) in parallel → TASK-005 → TASK-006.

## Test Strategy Summary

- **Acceptance tests (AT):** 6 derived from Gherkin scenarios (§7): no-state-degenerate, happy retomar, boundary iniciar-nova, boundary no-new-commits-hides-iniciar-nova, error --no-prompt, boundary COMPLETED-ignored.
- **Unit tests (TPP order):**
  - Level 1 (nil): empty `plans/` directory → Optional.empty
  - Level 2 (constant): single state file, phase=APPROVAL_PENDING → returns it
  - Level 3 (scalar): age calculation "2h 14min" from `lastPhaseCompletedAt`
  - Level 4 (collection): multiple state files → returns most recent non-COMPLETED
  - Level 5 (conditional): `--no-prompt` branch vs interactive branch
  - Level 6 (iteration): all 3 options (Retomar/Abortar/Iniciar nova) exercised
- **Integration/Smoke:** 1 smoke test (TASK-004) validates detect → prompt → resume path end-to-end.
- **Coverage target:** ≥ 95% line, ≥ 90% branch for `dev.iadev.release.resume` package.

## Security Assessment Summary

- **OWASP mapping:**
  - **A03 Injection / A01 Broken Access Control:** state file path read must canonicalize + verify prefix is `plans/` (CWE-22 path traversal).
  - **A08 Software & Data Integrity:** state file is trusted input from same repo; still validate `phase` enum, parse JSON strictly.
  - **A09 Security Logging & Monitoring:** no sensitive data in state file; version strings only. No PII.
- **Controls needed:**
  - Path normalization in `StateFileDetector` (reject `..`, symlinks).
  - Strict JSON parsing (fail on malformed state — do not auto-upgrade schema per RULE-003).
  - No exception message leakage to user prompt (generic error text).
- **Risk level:** LOW (local filesystem, trusted authoring boundary).

## Implementation Approach

Tech Lead selected the **orchestrator + detector split** (ARCH proposed monolithic detector; TL moved prompting concern to application layer). Rationale: domain purity (Rule 04) requires StateFileDetector to have no I/O of prompts; orchestrator owns the AskUserQuestion integration. Matches existing patterns in `dev.iadev.release.*` module (sibling use cases follow same split).

Quality gates:
1. Rule 03: method ≤ 25 lines, class ≤ 250 lines, ≤ 4 parameters.
2. Rule 04: domain has zero adapter/framework imports.
3. Rule 05: coverage ≥ 95% line, ≥ 90% branch.
4. Rule 12 J6: path traversal prevention in file read.
5. Rule 13: skill invocation protocol respected in SKILL.md doc (no bare-slash in delegation contexts).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 6 |
| Architecture tasks | 2 (TASK-001, TASK-002) |
| Test tasks | 1 (TASK-004) |
| Security tasks | 0 standalone (augmented into TASK-001, TASK-002) |
| Quality gate tasks | 1 (TASK-005) |
| Validation tasks | 1 (TASK-006) |
| Documentation tasks | 1 (TASK-003) |
| Merged tasks | 2 (TASK-001, TASK-002 merged ARCH+QA+SEC) |
| Augmented tasks | 2 (TASK-001, TASK-002 received SEC criteria) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| State file path traversal via crafted filename | Security | HIGH | LOW | Canonicalize path + prefix verify `plans/` base dir |
| Race condition: state file deleted between detect and read | QA | MEDIUM | LOW | Read-once pattern; NoSuchFileException → treat as "no state" |
| Non-TTY environments incorrectly treated as interactive | TL | MEDIUM | MEDIUM | Explicit `--no-prompt` flag + TTY detection; fallback to STATE_CONFLICT |
| Ambiguous "no new commits since tag" calculation | PO | LOW | MEDIUM | Define "new commits" as `git log <previousVersion>..HEAD --oneline` count > 0 |
| Schema drift: state v1 silently read as v2 | Security | HIGH | LOW | Enforce RULE-003 — hard-fail with STATE_SCHEMA_VERSION on v1 input |

## DoR Status

See `dor-story-0039-0008.md`. Verdict: READY (see Phase 5 output).
