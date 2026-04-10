# Story Planning Report -- story-0034-0005

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0034-0005 |
| Epic ID | 0034 |
| Date | 2026-04-10 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |
| Mode | multi-agent (inline synthesis by orchestrator) |

## Planning Summary

story-0034-0005 is the **closer story** for EPIC-0034 (Remove non-Claude targets from generator). It has no new production code. The scope is strictly documentation update + manifest regeneration + end-to-end verification + final PR creation.

Five tasks were consolidated from the proposals of 5 specialist agents, aligning with the pre-existing Section 8 task structure in the story file. Every task is `VERIFY` phase (no RED/GREEN cycles) because the story is a verification closer, not a feature implementation.

The story carries the responsibility of:

1. Writing the BREAKING CHANGE disclosure to CHANGELOG.md per Rule 08 SemVer requirements.
2. Regenerating `expected-artifacts.json` from ~9500 to ~830 entries per profile.
3. Validating 6 grep sanity checks, CLI smoke tests for rejected/accepted platforms, and invariants for RULE-003 (`.github/workflows/`) and RULE-004 (`resources/shared/templates/`).
4. Producing the final epic PR body with 5-story summary + metrics + migration + rollback.

## Architecture Assessment

**Layers affected:** none (cross-cutting documentation + test resources only).

**New components:** none.

**Modified components:**

- `CLAUDE.md` (repo root) - documentation
- `.claude/rules/*.md` - documentation (generated outputs; source-of-truth edits under `java/src/main/resources/targets/claude/rules/`)
- `README.md` (repo root) - documentation
- `docs/*` (if exists) - documentation
- `CHANGELOG.md` - release documentation
- `java/src/test/resources/smoke/expected-artifacts.json` - test manifest (regenerated)
- `java/src/test/resources/golden/**` - test fixtures (regenerated)

**Architecture contract validated:**

- `dev.iadev.smoke.ExpectedArtifactsGenerator` - main class for manifest regeneration
- `dev.iadev.golden.GoldenFileRegenerator` - main class for golden file regeneration
- Canonical invocation procedure documented in `README.md` §"Regenerating Golden Files" (~L820 per MEMORY.md)
- `mvn process-resources` is a mandatory prerequisite (per MEMORY.md; skipping causes stale output)
- Output paths hardcoded (SEC-003: no CWE-22 path traversal risk)

**RULE-004 invariant:** `java/src/main/resources/shared/templates/` must stay at exactly 57 files with empty `git diff origin/main`.

**RULE-003 invariant:** `.github/workflows/` golden file count must stay at exactly 95.

## Test Strategy Summary

**Acceptance tests (outer loop):** 9 Gherkin scenarios from story §7, all validated in TASK-004 with a PO-002 evidence matrix. Scenario 9 (PR mergeable) is deferred to TASK-005.

**Unit tests (inner loop):** none (no new production code).

**Integration / smoke tests:** 3 pre-existing smoke tests (`PlatformDirectorySmokeTest`, `AssemblerRegressionSmokeTest`, `CliModesSmokeTest`) must pass against the regenerated manifest. This is the primary automated gate for manifest correctness.

**Coverage target:**

- Line >= 95.00% absolute AND >= 93.69% (baseline 95.69% - 2pp per RULE-002)
- Branch >= 90.00% absolute AND >= 88.69% (baseline 90.69% - 2pp)

**TPP ordering applied to verification steps:**

1. nil: baseline measurements (wc -l, pre-edit grep)
2. constant: single-file edits (CLAUDE.md)
3. scalar: multi-file grep sweeps
4. collection: CLI smoke tests (iterated over rejected + accepted platforms)
5. conditional: coverage thresholds (line + branch, absolute + drift)
6. iteration: full E2E verification loop + PR mergeability

**CLI verification contract:** 3 reject tests + 2 accept tests + 1 file-count test = 6 runtime checks on the built jar.

## Security Assessment Summary

**OWASP Top 10 mapping for this story:**

- **A01 Broken Access Control:** N/A (no access control changes).
- **A02 Cryptographic Failures:** N/A (no cryptography).
- **A03 Injection:** N/A (documentation).
- **A04 Insecure Design:** N/A.
- **A05 Security Misconfiguration:** covered - CHANGELOG discloses BREAKING removal of CLI flags.
- **A06 Vulnerable Components:** N/A.
- **A07 Authentication Failures:** N/A.
- **A08 Software and Data Integrity Failures:** covered - `GoldenFileRegenerator` and `ExpectedArtifactsGenerator` are hardcoded-output tools, not user-controllable. SEC-003 verifies no CWE-22 path traversal.
- **A09 Logging Failures:** N/A.
- **A10 SSRF:** N/A.

**CWE mapping applied to individual DoD criteria:**

- **CWE-798 (Hardcoded Credentials):** grep for `password|secret|token|api_key|bearer` across all documentation edits (CLAUDE.md, rules, README, CHANGELOG). Zero true positives required.
- **CWE-209 (Error Message Information Disclosure):** CLI rejected-platform error message must not contain class names, stack traces, or file paths. Verified via TASK-004 Step 4 grep on stderr.
- **CWE-22 (Path Traversal):** `ExpectedArtifactsGenerator` output path must be a compile-time constant. Verified via TASK-003 Step 7 grep on the generator source.

**Compliance assessment:** Project compliance field is not set to a specific framework (per project identity: `library`). No regulatory mapping required.

**Risk level:** LOW. Documentation and verification story with no new code paths.

## Implementation Approach

**Chosen approach:** strict sequential execution of 5 tasks aligned with story Section 8. No parallelism (each task depends on the previous state).

**Rationale:**

- TASK-001 (CLAUDE.md) before TASK-002 (rules + CHANGELOG): allows consistent voice propagation.
- TASK-002 before TASK-003 (manifest regen): if CLAUDE.md is part of the source tree driving output, regen must run after edits settle.
- TASK-003 before TASK-004 (E2E verify): verification runs against the final state.
- TASK-004 before TASK-005 (PR creation): PR body references concrete evidence from verification.

**Quality gates enforced per task:** Rule 05 (coverage), RULE-002 (non-degradation), RULE-003 (workflows preserved), RULE-004 (shared templates preserved), Rule 08 (Conventional Commits + CHANGELOG + BREAKING CHANGE footer + rollback), Rule 09 (target branch = develop).

**Tech Lead alternative considered:** running TASK-001 and TASK-002 in parallel. Rejected because CHANGELOG rollback note and migration text depend on the exact set of changes finalized in CLAUDE.md.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 5 |
| Architecture tasks (pure) | 0 (ARCH-001, ARCH-002 merged into TASK-003 and TASK-004) |
| Test tasks | 0 RED/GREEN pairs; 9 QA VERIFY tasks (QA-001..QA-009) all merged into TASK-004 |
| Security tasks | 0 standalone; SEC-001..SEC-004 all augmented into TASK-001, TASK-002, TASK-003, TASK-004 |
| Quality gate tasks | 0 standalone; TL-001..TL-005 merged into TASK-001, TASK-002, TASK-004, TASK-005 |
| Validation tasks (PO) | 0 standalone; PO-001..PO-004 merged into TASK-001, TASK-002, TASK-004, TASK-005 |
| Merged tasks (multi-source) | 5 of 5 (100%) |
| Augmented tasks | 4 of 5 (TASK-001 through TASK-004 received security criteria injection) |
| Parallelizable tasks | 0 |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| `CLAUDE.md` or `.claude/rules/*.md` are generated outputs; direct edits will be overwritten | Architect | High | High | Escalation notes in tasks breakdown explicitly flag this. Edit source under `java/src/main/resources/targets/claude/` and regenerate. |
| Canonical regen command (per MEMORY.md) buried in README.md ~L820 | Architect | Medium | High | TASK-003 Step 1 mandates reading README.md §"Regenerating Golden Files" BEFORE executing any generator. |
| `mvn process-resources` skipped, producing stale regen output (per MEMORY.md) | Architect | High | Medium | TASK-003 Step 3 is explicit and non-skippable. |
| Coverage degradation beyond 2pp from baseline 95.69% line / 90.69% branch | QA | High | Low | TASK-004 Step 2 extracts concrete JaCoCo numbers and asserts thresholds. RULE-002 enforcement. |
| `.github/workflows/` accidentally deleted during golden regeneration (RULE-003 breach) | Security + Tech Lead | Critical | Low | TASK-003 Step 4 and TASK-004 Step 5 both count workflows files == 95 post-regen. |
| `resources/shared/templates/` touched (RULE-004 breach) | Tech Lead | Critical | Very Low | TASK-004 Step 6 runs `git diff origin/main -- shared/templates/` expecting empty. |
| CHANGELOG `BREAKING CHANGE:` footer missing from commit; SemVer parser fails to bump MAJOR | Tech Lead | High | Medium | TASK-002 Step 8 provides explicit commit template with footer. TASK-005 Step 1 greps for non-conforming commits. |
| CLI error message for rejected platforms contains stack trace (CWE-209 breach) | Security | Medium | Low | TASK-004 Step 4 greps stderr for `Exception`, `at dev.iadev`, `at java.`, `.java:NN` patterns. Zero matches required. |
| `ExpectedArtifactsGenerator` output path is user-controllable (CWE-22 breach) | Security | High | Very Low | TASK-003 Step 7 verifies the path is a compile-time constant in the generator source. |
| PR merge conflicts with `develop` due to unrelated merges during the epic | Tech Lead | Medium | Medium | TASK-005 Step 2 checks up-to-dateness, rebases if needed, re-runs verification. |
| PR body placeholders left unfilled | Product Owner | Low | Medium | TASK-005 Step 3 explicitly substitutes values from TASK-004 evidence. Reviewer catches as rework. |
| Story promise of ~180 line delta on CLAUDE.md not met (e.g. file already shorter) | Product Owner | Low | Medium | DoD threshold softened to delta >= 150 in TASK-001; if file is already minimal, document actual delta in PR body. |

## DoR Status

**Verdict: READY** (10/10 mandatory checks passed; 0/0 applicable conditional checks)

All mandatory checks passed:

1. Architecture plan exists (cross-cutting assessment + architecture contracts for regenerators)
2. Test plan with 9 AT scenarios in TPP order (UT not required - no new production code)
3. Security assessment with OWASP and CWE mappings (CWE-798, CWE-209, CWE-22)
4. 5 tasks >= 4 minimum
5. Every task has multiple DoD criteria
6. Linear dependency chain, no cycles
7. 9 Gherkin scenarios >= 4 minimum
8. Data contracts present (Documentation Contract + Verification Contract + Error Codes)
9. Implementation plan in 5 individual task plan files + planning report Implementation Approach section
10. Planning report exists with all required sections

Conditional checks (compliance, contract tests): N/A for this project.

---

## DoR Validation Results

See `plans/epic-0034/plans/dor-story-0034-0005.md` for the full checklist.
