# Story Planning Report -- story-0034-0003

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0034-0003 |
| Epic ID | 0034 |
| Date | 2026-04-10 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story-0034-0003 is the **third and final atomic removal story** of epic-0034. It eliminates the generic "agents" target (`.agents/`), reducing `AssemblerTarget` to a singleton enum with only `CLAUDE`. Unlike stories 0001 and 0002 which deleted target-specific subdirectories under `java/src/main/resources/targets/`, story 0003 touches **no resource directory** — the Agents target shares source resources with Codex (already removed in story 0002) and has no dedicated `targets/agents/` directory.

The scope is bounded and well-defined:
- **2 main Java classes** (`AgentsAssembler`, `AgentsSelection`) + edits to `AssemblerFactory`
- **6 test classes + 1 fixture** (`Agents*Test*` + `AgentsTestFixtures`)
- **0 resource files** (no `targets/agents/` directory exists)
- **~2910 golden files** across 17 `.agents/` subdirectories (1 per profile)
- **4 shared-class edits**: `AssemblerTarget` (enum), `FileCategorizer`, `OverwriteDetector`, `AssemblerTargetTest`
- **Explicit NON-scope:** `PlatformFilter.java` is owned by story-0034-0004 (higienization); this story MUST NOT touch it

The **4 tasks** in the story match one-to-one with §8 of the story markdown, decomposed further by the multi-agent consolidation into augmented DoD criteria covering architecture correctness, test regression, security verification, quality gates, and PO validation.

Post-execution state: `AssemblerTarget.values().length == 1`, grep for `.agents/|CODEX_AGENTS|AgentsAssembler|AgentsSelection` in `java/src/main` returns zero, and `mvn clean verify` is green with coverage ≥ 95% line / ≥ 90% branch.

## Architecture Assessment

**Layers affected (by task):**

| Task | Layer | Change Type |
|------|-------|------------|
| TASK-001 | application.assembler + adapter.test | delete 2 main + 6 test + 1 fixture; edit `AssemblerFactory` |
| TASK-002 | domain (enum) + adapter.inbound (cli) + util + adapter.test | edit `AssemblerTarget`, `FileCategorizer`, `OverwriteDetector`, `AssemblerTargetTest` |
| TASK-003 | adapter.test | delete golden `.agents/` in 17 profiles |
| TASK-004 | cross-cutting | final verify + PR |

**Dependency direction verification:** All deletions preserve inward dependency flow. No domain → adapter leak introduced; `AssemblerTarget` (domain enum) shrinks to a single value, which does not add any external dependency. `PlatformFilter.java` is explicitly **not** touched — it remains as a shared class that may still have stale `.agents/` branches, to be cleaned by story-0034-0004.

**Critical classification (pre-planning):** The baseline (`plans/epic-0034/baseline-pre-epic.md`) reports 4 `*Agents*` Java main classes and 12 `*Agents*Test*` classes. Filesystem audit during planning (2026-04-10) classified each:

- **Story 0003 owns** 2 main (`AgentsAssembler`, `AgentsSelection`) + 6 tests + fixture
- **Story 0001 owns** `GithubAgentsAssembler` + 4 `GithubAgents*Test*` classes (already deleted by prior story)
- **Story 0002 owns** `CodexAgentsMdAssembler` + `CodexAgentsMdAssemblerTest` (already deleted by prior story)

This classification matches story §3.1/§3.2 exactly. No baseline delta requires escalation.

**Implementation order (strictly sequential):**
1. Delete test classes + fixture → prevents broken references to deleted main classes
2. Delete main classes + edit `AssemblerFactory`
3. Edit enum + cli + util + test
4. Delete golden subdirs
5. Final verify + PR

Tasks are sequential per story §8 and per TDD-compliant deletion pattern from stories 0001/0002.

## Test Strategy Summary

**TDD Phase:** This story is in VERIFY phase — a pure deletion/refactor story with no new production code. All test activity is regression + verification, not RED→GREEN→REFACTOR new cycles.

**Double-Loop TDD mapping:**

**Outer loop (Acceptance Tests — from story §7 Gherkin):**

| ID | Scenario | Verification |
|----|----------|-------------|
| AT-1 | Build verde após remoção | `mvn clean verify` green; coverage thresholds |
| AT-2 | AssemblerTarget só tem CLAUDE | `AssemblerTargetTest` asserts singleton |
| AT-3 | Zero referências a `.agents/` | grep returns 0 |
| AT-4 | Golden `.agents/` removidos | `find` returns 0 |
| AT-5 | CLI claude-code funciona | smoke test with `--platform claude-code` |
| AT-6 | Degenerate: values() singleton | test asserts `length == 1` |

Total: **6 Gherkin scenarios** (meets DoR check #7 minimum of 4).

**Inner loop (Unit Tests — TPP ordered):**

| TPP Level | Type | Test Location | Purpose |
|-----------|------|--------------|---------|
| 1 (nil/degenerate) | regression | `AssemblerTargetTest` (edited in TASK-002) | single-element enum |
| 2 (constant) | regression | `AssemblerTargetTest` | `CLAUDE.directoryName == ".claude"` |
| 3 (scalar) | regression | `AssemblerTargetTest` | `valueOf("CODEX_AGENTS")` throws |
| 4 (collection) | regression | `AssemblerFactoryTest` (if exists) | remaining assemblers list |
| 5/6 | N/A | — | no new conditionals or iterations |

**RULE-006 TDD compliance (proportional removal):** The 6 deleted Agents* test classes exclusively cover the 2 deleted main classes. Removal is proportional — coverage should not degrade because removed tests were only exercising removed code.

**Coverage impact projection:** Net impact ≈ 0 pp. Baseline 95.69% line / 90.69% branch, projected post-story 95.69% / 90.69% (within 2pp degradation tolerance per RULE-002).

**Contract tests:** Project config declares `contract_tests: false` — DoR check #12 is N/A.

**Estimated coverage %:** ≥ 95% line / ≥ 90% branch (thresholds maintained).

## Security Assessment Summary

**OWASP Top 10 mapping (minimal):**

| Category | Relevance | Control |
|----------|-----------|---------|
| A04:2021 Insecure Design | LOW | Verify no partial orphaned references leaving dangling code paths |
| A06:2021 Vulnerable Components | LOW (improved) | Removing dead code reduces attack surface |
| A08:2021 Software & Data Integrity | LOW | No new deserialization; grep verifies clean removal |

**Security controls (task-level):**

- **SEC-001** (TASK-001): grep for residuals — `AgentsAssembler`/`AgentsSelection`/`CODEX_AGENTS` post-delete
- **SEC-002** (TASK-003): secrets scan in `.agents/` golden files before delete (`password|secret|token|api_?key`)
- **SEC-003** (TASK-003, CWE-22): symlink check before bulk delete (`find -type l`)
- **SEC-004** (TASK-001): orphan import detection in test tree
- **SEC-005** (TASK-004): final secrets scan under `java/src/main/resources/targets/`

**Risk level:** LOW. This is a deletion story with no new user input, no new secrets, no auth changes, no network code. Primary risk is orphan references — mitigated by compile verification at every task.

**Compliance:** Project config declares `compliance: none` — DoR check #11 is N/A.

**Secrets management:** No new credentials introduced. Defensive scans added to TASK-003 and TASK-004 DoD.

## Implementation Approach

**Chosen approach (Tech Lead):** Match the exact atomic removal pattern established by story-0034-0001 (see `tasks-story-0034-0001.md`), adapted for the smaller scope:

1. **Atomic deletion per task type** — tests first, main classes second, shared classes third, golden files fourth, verify last
2. **Strict sequential task ordering** — no parallelism (cleaner review, smaller blast radius)
3. **Conventional Commits with BREAKING CHANGE footers** on enum-touching commits
4. **Grep-driven verification** at every step — no trust in compile alone

**Quality gates defined:**

| Gate ID | Rule | Threshold | Task |
|---------|------|-----------|------|
| TL-001 | RULE-001 Build always green | `mvn clean verify` OK | TASK-004 |
| TL-002 | RULE-002 Coverage | line ≥ 95%, branch ≥ 90%, Δ ≤ 2pp | TASK-004 |
| TL-003 | Grep sanity | 0 residual refs in `java/src/main` | TASK-002, TASK-004 |
| TL-004 | Golden boundary | 0 `.agents/` dirs in golden tree | TASK-003, TASK-004 |
| TL-005 | Class length | AssemblerFactory ≤ 250 lines | TASK-001 |
| TL-006 | Conventional Commits | all 4 commits compliant | all tasks |
| TL-007 | Scope boundary | `PlatformFilter.java` untouched | TASK-002, TASK-004 |
| TL-008 | Enum singleton | `AssemblerTarget.values().length == 1` | TASK-002 |

**Coding standards compliance:** Maintained. Enum remains a single-file Java class (≤ 25 lines). `AssemblerFactory.java` class length check included in TASK-001 DoD. No new methods, no new cyclomatic complexity.

**Resolved considered alternative:** Single-commit megacommit vs 4-commit atomic sequence. Chose 4-commit sequence to enable granular rollback per task and match the pattern from story-0034-0001. Each commit is individually valid (except possible transient compile-time state between TASK-001 and TASK-002 if `AssemblerTargetTest` still references `CODEX_AGENTS` — acceptable because same story, same PR).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 4 |
| Architecture tasks | 2 (TASK-001, TASK-002) |
| Test tasks (delete/edit) | 2 (TASK-001 test deletions, TASK-003 golden deletions) |
| Security verification tasks | 1 (TASK-004) — security controls augmented across TASK-001/003/004 |
| Quality gate tasks | 1 (TASK-004) |
| Validation tasks (PO) | 1 (TASK-004) |
| Merged tasks (cross-agent consolidation) | 4 (all tasks merge contributions from multiple agents) |
| Augmented tasks (security criteria injected) | 3 (TASK-001, TASK-003, TASK-004) |
| Parallelizable tasks | 0 (strictly sequential per story §8) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Stories 0001/0002 not merged before 0003 starts | TL | High | Medium | TASK-001 pre-check grep for `GithubAgents*`/`CodexAgentsMd*`; HALT if found |
| `AssemblerFactory` has non-obvious registration (reflection/annotation scan) | Architect | Medium | Low | Thorough grep + compile check; investigate any unknown consumer |
| `AssemblerTargetTest` parametrized over enum values breaks with 1-element source | QA | Medium | Medium | Read full file before edit; adjust parametrization as needed |
| Developer accidentally edits `PlatformFilter.java` while cleaning `.agents/` refs | TL | High (scope boundary) | Medium | Explicit `git diff --name-only` check pre-commit; revert if found |
| Coverage drops below threshold despite proportional test removal | QA | High (blocks DoD) | Low | 6 removed test classes exclusively cover 2 removed main classes — net impact ≈ 0 pp |
| Bulk golden delete finds unexpected symlinks | Security | Medium (CWE-22) | Very Low | Pre-delete `find -type l` check in TASK-003 |
| CLI smoke fails because `claude-code` has hidden `.agents/` dependency | QA | High | Low | Pre-run smoke locally before commit; `x-ops-troubleshoot` on failure |
| Coverage/cumulative metrics discrepancy in PR body (8178 vs 8273 projected) | PO | Low (cosmetic) | Certain | Document arithmetic in PR body; 95-file delta = `.github/workflows/` protected |
| FileCategorizer.java is already clean (no-op edit mistakenly treated as failure) | Architect | Low | Medium | Pre-edit grep; document as no-op in commit body |
| `java/src/main/resources/targets/agents/` unexpectedly exists at runtime | Architect | Low | Very Low | Defensive `test -d && rm -rf` in TASK-003 |

## DoR Status

**Verdict:** **READY** — 10/10 mandatory checks passed, 0/0 applicable conditional checks (compliance N/A, contract tests N/A). See `dor-story-0034-0003.md` for full checklist.
