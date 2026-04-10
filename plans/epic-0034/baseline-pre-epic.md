# EPIC-0034 Baseline — Pre-Execution Snapshot

> **Captured:** 2026-04-10
> **Branch:** `feature/epic-0034-remove-non-claude-targets` (fresh from `develop`)
> **Develop SHA:** `6e7e3aae0` (post PR #266 merge)
> **Purpose:** Reference snapshot for measuring epic impact. The DoD in `epic-0034.md` §3 requires this baseline to exist before dispatching the first story.

## Baseline Validation

| Item | Status | Notes |
|------|--------|-------|
| `mvn clean verify` green | YES | Validated on `fix/baseline-manifest-golden-drift` (PR #266, merged to develop) |
| Total tests | 837 passing | 0 failures, 0 errors, 0 skipped |
| Coverage — line | 95.69% (8610/8998) | Above Rule 05 threshold (≥95%) |
| Coverage — branch | 90.69% (2562/2825) | Above Rule 05 threshold (≥90%) |
| JaCoCo classes analyzed | 300 | — |
| Build time | 6:01 min | Reference for regression detection |

## Source Code Counts (Java Main)

| Target | Path | Java classes to delete |
|--------|------|------------------------|
| GitHub Copilot | `java/src/main/java/**/*Github*`, `**/*Copilot*` | **8** |
| Codex | `java/src/main/java/**/*Codex*` | **7** |
| Agents (generic) | `java/src/main/java/**/*Agents*` (excluding `*Parent*`) | **4** |
| **Subtotal (to delete)** | | **19** |
| Claude Code (PROTECTED) | `java/src/main/java/**/*Claude*` | kept |

> Note: epic doc §1 declares "18 classes Java (8 GitHub/Copilot + 7 Codex + 2 Agents + 1 `ReadmeGithubCounter`)". The actual count of 4 `*Agents*` classes includes the `ReadmeGithubCounter` counterpart and possibly abstract parents. Story 0003 will classify each of the 4 individually.

## Source Code Counts (Java Tests)

| Target | Pattern | Test classes |
|--------|---------|--------------|
| GitHub Copilot tests | `*Github*Test*` or `*Copilot*Test*` | **16** |
| Codex tests | `*Codex*Test*` | **6** |
| Agents tests | `*Agents*Test*` | **12** |
| **Subtotal** | | **34** |

> Note: epic doc §1 declares "29 classes de teste + 2 fixtures". Actual: 34 test classes (+5 vs. epic). Story-level planning must reconcile this delta — some may be abstract bases or shared helpers that remain after target removal.

## Resources (Generator Source)

| Target | Path | File count |
|--------|------|------------|
| GitHub Copilot resources | `java/src/main/resources/targets/github-copilot/` | **131** |
| Codex resources | `java/src/main/resources/targets/codex/` | **15** |
| Claude resources | `java/src/main/resources/targets/claude/` | **395** (kept) |
| Agents resources | — | **0** (no dedicated source dir) |
| Shared templates (PROTECTED) | `java/src/main/resources/shared/templates/` | **57** |

## Golden Files (Test Fixtures)

| Subdirectory | Count | Action per EPIC-0034 |
|--------------|-------|----------------------|
| `.github/` (non-workflows) | **2324** | DELETE (story-0034-0001) |
| `.github/workflows/` | **95** | **PROTECTED (RULE-003)** — never touch |
| `.codex/` | **2944** | DELETE (story-0034-0002) |
| `.agents/` | **2910** | DELETE (story-0034-0003) |
| `.claude/` | **5684** | KEEP (post-epic target) |
| **Other (root files)** | **328** | KEEP |
| **TOTAL** | **14285** | — |

**Projected post-epic golden count:** 14285 − 2324 − 2944 − 2910 = **6107** files (~57% reduction).

## Documentation

| File | Current size | Projected post-epic |
|------|--------------|---------------------|
| `CLAUDE.md` (root) | 283 lines | ~80 lines (story-0034-0005 promises −200 lines) |
| `.claude/rules/` committed output | 10 files | 10 files (unchanged, target drift correction only) |

## Protected Areas Inventory (RULE-003, RULE-004)

| Path | Reason | Files |
|------|--------|-------|
| `java/src/test/resources/golden/**/.github/workflows/` | CI/CD pipelines (RULE-003) | 95 |
| `java/src/main/resources/shared/templates/` | Claude Code templates (RULE-004) | 57 |

Any story that reduces these counts must halt and report a rule violation.

## Generator Tools Verified

| Tool | Location | Purpose |
|------|----------|---------|
| `GoldenFileRegenerator` | `dev.iadev.golden.GoldenFileRegenerator` | Rebuilds golden profiles from source resources |
| `ExpectedArtifactsGenerator` | `dev.iadev.smoke.ExpectedArtifactsGenerator` | Rebuilds `expected-artifacts.json` manifest |
| Canonical regen procedure | README.md §"Regenerating Golden Files" (~L820) | `mvn compile test-compile` → run regenerator → `mvn test` |

> Story 0034-0005 must use both tools in sequence to produce the final clean state.

## Grep Sanity Baseline

Before the epic starts, the following greps return non-zero (target code still exists):

```
grep -rn "GithubInstructionsAssembler" java/src/main/java  # expected: hits in 8 classes
grep -rn "CodexConfigAssembler"        java/src/main/java  # expected: hits in 7 classes
grep -rn "AgentsAssembler"             java/src/main/java  # expected: hits in 4 classes
grep -rn "Platform.COPILOT"            java/src/main/java  # expected: hits in enum + references
grep -rn "Platform.CODEX"              java/src/main/java  # expected: hits in enum + references
grep -rn "AssemblerTarget.GITHUB"      java/src/main/java  # expected: hits in enum + references
grep -rn "AssemblerTarget.CODEX"       java/src/main/java  # expected: hits in enum + references
grep -rn "AssemblerTarget.CODEX_AGENTS" java/src/main/java # expected: hits in enum + references
```

After story-0034-0005 completes, ALL of the above MUST return zero (epic DoD §3 "Grep Sanity Check").
