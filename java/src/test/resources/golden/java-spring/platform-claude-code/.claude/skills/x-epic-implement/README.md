# x-epic-implement — Execution Flow (EPIC-0049 refactor)

> Visual reference for the thin-orchestrator flow delivered by story-0049-0018.
> The main SKILL.md is now ~460 lines and delegates every substantive
> responsibility to six specialized sub-skills. This README illustrates the
> delegation topology; prose specifics live in `SKILL.md` and
> `references/full-protocol.md`.

## 1. High-Level Orchestration Flow (new default)

```mermaid
flowchart TD
    START(["/x-epic-implement EPIC-ID"]) --> P0

    P0["Phase 0 — Args<br/>x-internal-args-normalize"] --> FLOW{flowVersion?}
    FLOW -->|"1 (legacy)"| P1L["Phase 1 (legacy) — Load & Plan<br/>x-internal-epic-build-plan --mode sequential"]
    FLOW -->|"2 (default)"| P1N["Phase 1 — Load & Plan<br/>x-internal-epic-build-plan"]

    P1L --> P3L
    P1N --> P2["Phase 2 — Branch Setup<br/>x-internal-epic-branch-ensure<br/>(creates epic/XXXX)"]
    P2 --> P3N

    subgraph P3N["Phase 3 — Execution Loop (v2)"]
        P3N_SEQ{"Parallel?"}
        P3N_SEQ -->|"No (default)"| SEQ["Sequential: one story at a time<br/>x-story-implement --target-branch epic/XXXX --auto-merge-strategy=merge"]
        P3N_SEQ -->|"--parallel"| PAR["Parallel within phase batch<br/>(siblings in ONE assistant message)"]
    end

    subgraph P3L["Phase 3 — Execution Loop (legacy)"]
        P3L_SEQ["Sequential<br/>x-story-implement --target-branch develop"]
    end

    P3N --> P4["Phase 4 — Integrity Gate + Report<br/>x-internal-epic-integrity-gate +<br/>x-internal-report-write"]
    P3L --> P4

    P4 --> P4B{"--skip-pr-comments?"}
    P4B -->|"No"| P4C["Phase 4b — PR-comment remediation<br/>x-pr-fix-epic"]
    P4B -->|"Yes"| FLOW2{"flowVersion?"}
    P4C --> FLOW2

    FLOW2 -->|"2"| P5N["Phase 5 — Final PR<br/>x-git-merge develop→epic/XXXX +<br/>x-pr-create (no auto-merge)"]
    FLOW2 -->|"1"| SKIPFINAL["Skip Phase 5<br/>(legacy: stories already in develop)"]

    P5N --> DONE(["Return envelope"])
    SKIPFINAL --> DONE

    style P0 fill:#16213e,color:#fff
    style P1N fill:#16213e,color:#fff
    style P2 fill:#16213e,color:#fff
    style P3N fill:#16213e,color:#fff
    style P4 fill:#16213e,color:#fff
    style P5N fill:#2d6a4f,color:#fff
    style P1L fill:#533483,color:#fff
    style P3L fill:#533483,color:#fff
    style SKIPFINAL fill:#533483,color:#fff
    style DONE fill:#2d6a4f,color:#fff
```

## 2. Delegation Map

```mermaid
graph TD
    EPIC["x-epic-implement<br/>(thin orchestrator, ~460 lines)"] -->|Phase 0| ARGS["x-internal-args-normalize"]
    EPIC -->|Phase 1| PLAN["x-internal-epic-build-plan"]
    EPIC -->|Phase 2| BRANCH["x-internal-epic-branch-ensure"]
    EPIC -->|Phase 3 per story| STORY["x-story-implement"]
    EPIC -->|Phase 4| GATE["x-internal-epic-integrity-gate"]
    EPIC -->|Phase 4 report| REPORT["x-internal-report-write"]
    EPIC -->|Phase 5.1| MERGE["x-git-merge<br/>(sync develop→epic)"]
    EPIC -->|Phase 5.2| PR["x-pr-create<br/>(final PR, no auto-merge)"]
    EPIC -->|all phases| STATE["x-internal-status-update<br/>(atomic state writes)"]
    EPIC -->|Phase 4b optional| PRFIX["x-pr-fix-epic"]

    BRANCH -->|delegates creation| GITBRANCH["x-git-branch"]
    PLAN -->|renders via| REPORT
    PLAN -->|collision eval| PAREV["x-parallel-eval"]
    STORY -->|per task| TDD["x-test-tdd + x-pr-create + x-git-commit"]

    classDef thin fill:#16213e,stroke:#0f3460,color:#fff
    classDef internal fill:#533483,stroke:#e94560,color:#fff
    classDef primitive fill:#2d6a4f,stroke:#1b4332,color:#fff

    class EPIC,STORY thin
    class ARGS,PLAN,BRANCH,GATE,REPORT,STATE internal
    class MERGE,PR,GITBRANCH,PAREV,PRFIX,TDD primitive
```

## 3. Flow Version Decision

```mermaid
flowchart TD
    START(["--resume OR first run"]) --> STATE{"execution-state.json<br/>exists?"}
    STATE -->|"No"| FLAGCHK{"--legacy-flow<br/>on argv?"}
    STATE -->|"Yes"| READVER["Read flowVersion field"]

    READVER --> VERCHK{"flowVersion == 1<br/>or absent?"}
    VERCHK -->|"Yes"| FORCELEG["Force --legacy-flow<br/>warn operator"]
    VERCHK -->|"No (== 2)"| NEWFLOW["flowVersion=2"]

    FLAGCHK -->|"Yes"| LEGACY["flowVersion=1"]
    FLAGCHK -->|"No"| NEWFLOW

    FORCELEG --> LEGACY
    LEGACY --> RUNLEGACY(["Run legacy flow<br/>(no epic branch, no final PR)"])
    NEWFLOW --> RUNNEW(["Run new flow<br/>(epic/XXXX + final PR)"])

    style RUNLEGACY fill:#533483,color:#fff
    style RUNNEW fill:#2d6a4f,color:#fff
```

## 4. Default-change summary

| Aspect | EPIC-0042 | EPIC-0049 (this refactor) |
|--------|-----------|---------------------------|
| SKILL.md size | ~2000 lines | ~460 lines (77% drop) |
| References size | ~1300 lines | ~280 lines |
| Parallelism | default on (`--sequential` opts out) | default off (`--parallel` opts in) |
| Auto-merge target | `develop` | `epic/<EPIC-ID>` |
| Final PR | N/A (every story merges to develop) | `epic/<EPIC-ID> → develop` (manual gate) |
| Inline `git`/`gh`/`jq`/`mvn` | present | **0** (only `Read`/`Glob` + `Skill`) |
| Backward compat | — | `--legacy-flow` + `flowVersion` auto-detect |

## 5. Error Code Catalogue

| Exit | Code | Phase | Cause |
|------|------|-------|-------|
| 1 | `ARGS_INVALID` | 0 | Normalizer rejects argv |
| 2 | `EPIC_DIR_MISSING` | 0 | `plans/epic-XXXX/` absent |
| 3 | `STORY_FAILED` | 3 | One or more stories FAILED |
| 4 | `INTEGRITY_GATE_FAILED` | 4 | Gate failed twice after recovery |
| 5 | `FINAL_PR_CONFLICTS` | 5 | `x-git-merge` sync conflicted |
| 6 | `BRANCH_ENSURE_FAILED` | 2 | `epic/<ID>` could not be ensured |
| 7 | `PLAN_BUILD_FAILED` | 1 | Plan build failed (non-cyclic) |
| 8 | `CYCLIC_DEPENDENCY` | 1 | DAG cycle detected |

## 6. References

- Main skill body: [`SKILL.md`](SKILL.md)
- Full protocol + retry/circuit-breaker/legacy semantics: [`references/full-protocol.md`](references/full-protocol.md)
- Args schema consumed by `x-internal-args-normalize`: [`references/args-schema.json`](references/args-schema.json)
- Parent story: `plans/epic-0049/story-0049-0018.md`
- ADR-0006 (file-conflict-aware parallelism), ADR-0010 (interactive gates), ADR-0012 (thin-skill pattern)
