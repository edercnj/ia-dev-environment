# Interactive Flow Walkthrough (story-0039-0015)

> End-to-end example sessions for the `x-release` skill post-EPIC-0039.
> Covers a normal release (`/x-release`) and a hotfix release
> (`/x-release --hotfix`). Each session shows the operator's input, every
> prompt shown by the skill, and the final terminal output.

This document is the operator-facing companion to the pure-technical
references (`auto-version-detection.md`, `prompt-flow.md`,
`state-file-schema.md`, `approval-gate-workflow.md`,
`backmerge-strategies.md`, `git-flow-cycle-explainer.md`). Read those for
formal semantics; read this for a realistic experience of the happy path
and its two or three common decision points.

## 1. Normal Release Walkthrough

### 1.1 Scenario

- Repository: `iadev/demo-service`
- Current branch: `develop`
- Last release tag: `v3.1.0`
- Commits since last tag (auto-detected):
  - 5 × `feat:`
  - 2 × `fix:`
  - 0 × breaking
- Expected bump: MINOR → next version `v3.2.0`
- Operator goal: cut `v3.2.0` interactively, merge via GitHub PR flow.

### 1.2 Invocation

```
$ /x-release
```

No positional argument, no `--version` flag → **auto-detect mode**
(story-0039-0001).

### 1.3 Phase 0 — RESUME-DETECT (silent on fresh start)

```
[0.1] gh CLI: OK (2.45.0)
[0.1] jq:    OK (1.7.1)
[0.1] gh auth: OK (edercnj)
[0.3] No state file at plans/release-state-3.2.0.json — fresh start.
```

No prompt. The skill proceeds to Phase 1.

### 1.4 Phase 1 — DETERMINE (auto-detect banner)

```
[1] Next version detected: 3.2.0 (MINOR) — 5 feat, 2 fix, 0 breaking since v3.1.0
[1] bumpType = minor (auto)
```

### 1.5 Phase 1.5 — PRE-FLIGHT Dashboard (story-0039-0009)

The skill renders the consolidated pre-flight dashboard before
branching:

```
╔══════════════════════════════════════════════════════════════╗
║   PRE-FLIGHT — Release v3.2.0                                ║
╠══════════════════════════════════════════════════════════════╣
║ Version                                                      ║
║   target:      3.2.0                                         ║
║   bump:        MINOR                                         ║
║   last tag:    v3.1.0 (age: 12d)                             ║
║                                                              ║
║ Commits since v3.1.0                                         ║
║   feat:        5                                             ║
║   fix:         2                                             ║
║   breaking:    0                                             ║
║   ignored:     14 (chore/docs/test/...)                      ║
║                                                              ║
║ CHANGELOG preview (first 10 lines)                           ║
║   ## [Unreleased]                                            ║
║   ### Added                                                  ║
║   - Auto-version detection (feat)                            ║
║   - Smart resume of orphaned state (feat)                    ║
║   - Pre-flight dashboard (feat)                              ║
║   ### Fixed                                                  ║
║   - Integrity report JSON escaping (fix)                     ║
║   (5 linhas omitidas)                                        ║
║                                                              ║
║ Integrity checks                                             ║
║   changelog_unreleased_non_empty: PASS                       ║
║   version_alignment:              PASS                       ║
║   no_new_todos:                   WARN (2 markers)           ║
║   overall:                        PASS                       ║
║                                                              ║
║ Execution plan                                               ║
║   2. VALIDATE-DEEP (build + 9 checks, parallel wave)         ║
║   3. BRANCH      release/3.2.0 from develop (worktree)       ║
║   4. UPDATE      pom.xml → 3.2.0 (strip -SNAPSHOT)           ║
║   5. CHANGELOG   move [Unreleased] → [3.2.0]                 ║
║   6. COMMIT      release: v3.2.0                             ║
║   7. OPEN-RELEASE-PR  release/3.2.0 → main                   ║
║   8. APPROVAL-GATE                                           ║
║   9. TAG         v3.2.0 on main                              ║
║   10. BACK-MERGE-DEVELOP  PR chore/backmerge-v3.2.0          ║
║   11. PUBLISH    push tag; optional GitHub Release           ║
║   12. CLEANUP    delete release/3.2.0 branch                 ║
║   13. SUMMARY    Git Flow diagram                            ║
╚══════════════════════════════════════════════════════════════╝

Prosseguir com release v3.2.0?
  [1] Sim, prosseguir
  [2] Editar versão (--version)
  [3] Abortar
> 1
```

Operator chooses `1`. Skill proceeds to VALIDATE-DEEP.

### 1.6 Phase 2 — VALIDATE-DEEP (parallel wave, story-0039-0004)

```
[VALIDATE-DEEP] workdir_clean:       0s  PASS
[VALIDATE-DEEP] branch_correct:      0s  PASS
[VALIDATE-DEEP] changelog_unreleased 0s  PASS
[VALIDATE-DEEP] build_tests:        47s  PASS
[VALIDATE-DEEP] coverage_line:       1s  PASS (97.3%)
[VALIDATE-DEEP] coverage_branch:     1s  PASS (92.8%)
[VALIDATE-DEEP] golden_files:       12s  PASS
[VALIDATE-DEEP] hardcoded_version:   2s  PASS
[VALIDATE-DEEP] version_match:       1s  PASS
[VALIDATE-DEEP] generation_drift:    6s  PASS
[VALIDATE-DEEP] integrity_drift:     3s  PASS
[VALIDATE-DEEP] all checks complete in 49s (sequential estimate: 73s, -32%)
```

### 1.7 Phases 3-6 — BRANCH, UPDATE, CHANGELOG, COMMIT (non-interactive)

```
[3] Created worktree .claude/worktrees/release-3.2.0/ (branch release/3.2.0 from develop)
[4] pom.xml: 3.2.0-SNAPSHOT → 3.2.0
[5] CHANGELOG.md: moved [Unreleased] entries into [3.2.0] - 2026-04-15
[6] release: v3.2.0 — committed as 7a9c3b2 on release/3.2.0
```

### 1.8 Phase 7 — OPEN-RELEASE-PR

```
[7] Pushed release/3.2.0 to origin
[7] gh pr create --base main --head release/3.2.0
[7] → PR #412 opened: https://github.com/iadev/demo-service/pull/412
[7] Fire-and-forget x-review-pr dispatched in background (skip with --skip-review)
```

### 1.9 Phase 8 — APPROVAL-GATE (interactive prompt, story-0039-0007)

```
╔══════════════════════════════════════════════════════════════╗
║   APPROVAL GATE — Release v3.2.0                             ║
╠══════════════════════════════════════════════════════════════╣
║   PR:       #412 (https://github.com/.../pull/412)           ║
║   Status:   OPEN (review in progress)                        ║
║   Phase:    APPROVAL_PENDING                                 ║
║   State:    plans/release-state-3.2.0.json (persisted)       ║
╚══════════════════════════════════════════════════════════════╝

O que deseja fazer?
  [1] PR mergeado — continuar
  [2] Rodar /x-pr-fix PR#412
  [3] Sair e retomar depois
> 3
```

Operator chooses `3`. Skill exits cleanly; state is preserved:

```
[8] Exiting at APPROVAL_PENDING. Resume with:
        /x-release --continue-after-merge
```

The operator reviews the PR on GitHub with teammates, addresses any
comments via `/x-pr-fix 412`, and eventually merges via the GitHub UI.

### 1.10 Resume — `/x-release --continue-after-merge`

```
$ /x-release --continue-after-merge
[0.3] Loaded plans/release-state-3.2.0.json (schemaVersion=2, phase=APPROVAL_PENDING)
[0.3] MODE = RESUME — jumping to Phase 9 (RESUME-AND-TAG)
```

### 1.11 Phase 9 — RESUME-AND-TAG

```
[9] gh pr view 412: state=MERGED, mergedAt=2026-04-15T14:02:18Z
[9] git fetch origin
[9] git checkout main && git pull
[9] git tag -a v3.2.0 -m "release: v3.2.0"
[9] git push origin v3.2.0
[9] Tagged main at 8f2a94d
```

### 1.12 Phase 10 — BACK-MERGE-DEVELOP

```
[10] git checkout -b chore/backmerge-v3.2.0 main
[10] gh pr create --base develop --head chore/backmerge-v3.2.0
[10] → PR #413 opened: https://github.com/iadev/demo-service/pull/413

O que deseja fazer?
  [1] PR mergeado — continuar
  [2] Rodar /x-pr-fix PR#413
  [3] Sair e retomar depois
> 1
```

### 1.13 Phase 11 — PUBLISH (GitHub Release prompt, story-0039-0006)

```
╔══════════════════════════════════════════════════════════════╗
║   PUBLISH — Release v3.2.0                                   ║
╠══════════════════════════════════════════════════════════════╣
║   Tag v3.2.0 published to origin.                            ║
║   Deseja criar o GitHub Release?                             ║
║     [1] Sim, criar agora                                     ║
║     [2] Não, criar manualmente depois                        ║
╚══════════════════════════════════════════════════════════════╝
> 1
[11] gh release create v3.2.0 --title "v3.2.0" --notes-file <(awk '/## \[3.2.0\]/,/## \[/' CHANGELOG.md)
[11] → Release published: https://github.com/iadev/demo-service/releases/tag/v3.2.0
```

### 1.14 Phase 12 — CLEANUP

```
[12] x-git-worktree remove --id release-3.2.0
[12] git push origin --delete release/3.2.0
[12] git branch -D release/3.2.0
[12] Cleanup complete.
```

### 1.15 Phase 13 — SUMMARY (story-0039-0005)

```
╔══════════════════════════════════════════════════════════════╗
║   Git Flow Cycle — v3.1.0 → v3.2.0                           ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║   develop ──●──────●───────────────●───►  (back-merge)       ║
║              \                     ↑                         ║
║               \                    │                         ║
║   release/3.2  ●───●───●───●───●───┘                         ║
║                           │                                  ║
║   main    ────────────────●────────●───►  (tag v3.2.0)       ║
║                                                              ║
║   PR release:    #412  MERGED                                ║
║   PR backmerge:  #413  MERGED                                ║
║   GitHub Release: v3.2.0                                     ║
║                                                              ║
║   Duração total: 2h 14min (incluindo APPROVAL halt)          ║
║   Telemetria:    plans/release-metrics.jsonl                 ║
║                                                              ║
║   Top 3 fases mais lentas (vs. mediana histórica):           ║
║     2. VALIDATE-DEEP      49s   (+8% vs mediana)             ║
║     7. OPEN-RELEASE-PR    12s   (−5% vs mediana)             ║
║     4. UPDATE              3s   (+0% vs mediana)             ║
║                                                              ║
║   Release concluída com sucesso. 🎉                          ║
╚══════════════════════════════════════════════════════════════╝
```

Skill exits 0. Total operator-visible prompts: **4**
(pre-flight, approval gate, backmerge gate, GitHub Release).

---

## 2. Hotfix Release Walkthrough

### 2.1 Scenario

- Repository: `iadev/demo-service`
- Current branch: `main`
- Last release tag: `v3.2.0`
- Incident: critical NPE in `PaymentController` reported as P1 at 02:14 UTC.
- A single commit lands on `main` via hotfix cherry-pick:
  - 1 × `fix(payment): null-check on missing idempotency key`
- Operator goal: cut `v3.2.1` as a hotfix, back-merge to `develop`.

### 2.2 Invocation

```
$ /x-release --hotfix
```

The `--hotfix` flag engages the hotfix context
(`ReleaseContext.forHotfix()`, story-0039-0014). Auto-detect is
restricted to PATCH only.

### 2.3 Phase 0 — RESUME-DETECT

```
[0.1] gh / jq / gh auth: OK
[0.3] No state file at plans/release-state-hotfix-3.2.1.json — fresh start.
[0.3] ReleaseContext = HOTFIX (base=main, state=release-state-hotfix-*)
```

### 2.4 Phase 1 — DETERMINE (hotfix auto-detect)

```
[1] --hotfix mode: PATCH only (VersionDetector.forHotfix)
[1] Next version detected: 3.2.1 (PATCH) — 0 feat, 1 fix, 0 breaking since v3.2.0
[1] bumpType = patch (auto, hotfix)
```

If the commit window had contained a `feat:` or a breaking change, the
skill would have aborted with `HOTFIX_INVALID_COMMITS` (story-0039-0014
§5.3).

### 2.5 Phase 1.5 — PRE-FLIGHT (hotfix banner)

```
╔══════════════════════════════════════════════════════════════╗
║   PRE-FLIGHT — Hotfix v3.2.1 (modo HOTFIX, base=main,        ║
║                              bump=PATCH)                     ║
╠══════════════════════════════════════════════════════════════╣
║   (remaining sections identical to normal release)           ║
╚══════════════════════════════════════════════════════════════╝

Prosseguir com hotfix v3.2.1?
  [1] Sim, prosseguir
  [2] Editar versão (--version)
  [3] Abortar
> 1
```

### 2.6 Phase 2 — VALIDATE-DEEP

Same 10 checks as a normal release. Check 2 (correct branch) expects
`main` (not `develop`). Everything passes in ~51s.

### 2.7 Phase 3 — BRANCH (hotfix worktree)

```
[3] Created worktree .claude/worktrees/hotfix-3-2-1/ (branch hotfix/3.2.1 from main)
[3] HOTFIX_SLUG validated against ^[a-z0-9][a-z0-9-]{0,62}$
```

### 2.8 Phases 4-6

```
[4] pom.xml: 3.2.0 → 3.2.1 (no SNAPSHOT — hotfix skips SNAPSHOT advance)
[5] CHANGELOG.md: moved [Unreleased] entries into [3.2.1] - 2026-04-15
[6] fix: v3.2.1 (hotfix) — committed as 4b71e03 on hotfix/3.2.1
```

### 2.9 Phase 7 — OPEN-RELEASE-PR (base=main)

```
[7] Pushed hotfix/3.2.1 to origin
[7] gh pr create --base main --head hotfix/3.2.1 --title "fix: v3.2.1 (hotfix)"
[7] → PR #418 opened: https://github.com/iadev/demo-service/pull/418
```

### 2.10 Phase 8 — APPROVAL-GATE

```
O que deseja fazer?
  [1] PR mergeado — continuar
  [2] Rodar /x-pr-fix PR#418
  [3] Sair e retomar depois
> 1
```

Operator chooses `1` immediately because the hotfix is time-critical and
the on-call reviewer is already on the PR.

### 2.11 Phase 9 — RESUME-AND-TAG

```
[9] gh pr view 418: state=MERGED, mergedAt=2026-04-15T02:41:02Z
[9] git tag -a v3.2.1 -m "fix: v3.2.1 (hotfix)"
[9] git push origin v3.2.1
```

### 2.12 Phase 10 — BACK-MERGE (hotfix variant, story-0039-0014)

If an active `release/*` branch is open, the hotfix is back-merged to
both `develop` AND the active release. In this example there is no
active release:

```
[10] Active release branch scan: none found
[10] gh pr create --base develop --head chore/backmerge-v3.2.1
[10] → PR #419 opened
[10] No SNAPSHOT advance (hotfix mode)

O que deseja fazer?
  [1] PR mergeado — continuar
  [2] Rodar /x-pr-fix PR#419
  [3] Sair e retomar depois
> 1
```

### 2.13 Phase 11 — PUBLISH

```
Deseja criar o GitHub Release?
  [1] Sim, criar agora
  [2] Não, criar manualmente depois
> 1
[11] → Release published: https://github.com/iadev/demo-service/releases/tag/v3.2.1
```

### 2.14 Phase 12 — CLEANUP

```
[12] x-git-worktree remove --id hotfix-3-2-1
[12] git push origin --delete hotfix/3.2.1
```

### 2.15 Phase 13 — SUMMARY (hotfix diagram variant)

```
╔══════════════════════════════════════════════════════════════╗
║   Git Flow Cycle — v3.2.0 → v3.2.1 (HOTFIX)                  ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║   main   ────●──────────●────────●───►  (tag v3.2.1)         ║
║               \         ↑                                    ║
║                \        │                                    ║
║   hotfix/3.2.1  ●───────┘                                    ║
║                 │                                            ║
║   develop ─────●┴──────────────●───►  (back-merge)           ║
║                                ↑                             ║
║                                │ PR #419                     ║
║                                                              ║
║   PR hotfix:     #418  MERGED                                ║
║   PR backmerge:  #419  MERGED                                ║
║   GitHub Release: v3.2.1                                     ║
║                                                              ║
║   Duração total: 28min (incident response window)            ║
║   releaseType: hotfix                                        ║
║                                                              ║
║   Hotfix concluído com sucesso. 🩹                           ║
╚══════════════════════════════════════════════════════════════╝
```

Total operator-visible prompts: **5** (pre-flight, approval gate,
backmerge gate, GitHub Release; add any `x-pr-fix` handoff if used).

---

## 3. Things Worth Noting

- **Smart Resume.** If a previous `release/3.2.0` state file is left
  orphaned for hours/days, the operator can simply re-invoke
  `/x-release`; the Smart Resume prompt (story-0039-0008) detects the
  stale state, shows age and options `[1] Resume | [2] Abort | [3]
  Start new` (Start-new only offered when new commits have landed).
  See `references/prompt-flow.md` → Smart Resume section.
- **CI path.** In automation, use `--no-prompt` (story-0039-0007) +
  `--continue-after-merge` + `--no-github-release`. No prompts are
  shown; every halt point exits cleanly with textual resume
  instructions and state preserved.
- **Interactive dry-run.** `/x-release --dry-run --interactive` pauses
  before each of the 13 phases and performs zero side effects
  (story-0039-0013). Used for onboarding / skill validation.
- **Telemetry.** Every phase appends a JSONL line to
  `plans/release-metrics.jsonl` (story-0039-0012). Phase 13 renders a
  Top-3 slowest-vs-median benchmark from that file. Disable with
  `--telemetry off`.
- **Abort.** `/x-release --abort` performs double-confirmation cleanup
  of open PRs, local/remote branches, and the state file
  (story-0039-0010).

## 4. Reference Index

| Reference | Purpose |
|---|---|
| `auto-version-detection.md` | Conventional Commits → SemVer bump algorithm |
| `prompt-flow.md` | `PromptEngine` halt points, Smart Resume |
| `state-file-schema.md` | `release-state-*.json` v2 schema, atomic writes |
| `approval-gate-workflow.md` | Phase 8 state machine |
| `backmerge-strategies.md` | Phase 10 clean vs conflict flow |
| `git-flow-cycle-explainer.md` | Phase 13 SUMMARY renderer |
| `interactive-flow-walkthrough.md` | _(this file)_ end-to-end example sessions |
