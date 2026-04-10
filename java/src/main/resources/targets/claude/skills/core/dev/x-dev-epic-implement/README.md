# x-dev-epic-implement — Execution Flow

> Visual reference for the complete orchestration flow, skill invocations, and decision points.

## 1. High-Level Orchestration Flow

```mermaid
flowchart TD
    START(["/x-dev-epic-implement EPIC-ID"]) --> P0

    subgraph P0["Phase 0 — Preparation (Inline)"]
        P0A[Parse args + flags] --> P0B[Prerequisites check]
        P0B --> P0C[Read IMPLEMENTATION-MAP.md]
        P0C --> P0D[Read EPIC-XXXX.md]
        P0D --> P0E[Discover story files]
        P0E --> P0F{--single-pr?}
        P0F -->|Yes| LEGACY[Legacy flow: epic branch + mega-PR]
        P0F -->|No| P0G{--dry-run?}
        P0G -->|Yes| DRYRUN([Print execution plan + STOP])
        P0G -->|No| P0H{--resume?}
        P0H -->|Yes| RESUME[Resume Workflow]
        P0H -->|No| P0I[Initialize execution state]
        RESUME --> P0I
    end

    P0I --> PHASELOOP

    subgraph PHASELOOP["Phase Loop (for each phase 0..N)"]
        direction TB
        PH05{--sequential?}
        PH05 -->|Yes| SKIP05[Skip pre-flight]
        PH05 -->|No| PREFLIGHT["Phase 0.5: Pre-flight Analysis"]
        SKIP05 --> CAPTURE
        PREFLIGHT --> CAPTURE

        CAPTURE["Capture mainShaBeforePhase"] --> EXEC

        subgraph EXEC["Story Execution"]
            GET[getExecutableStories] --> WAIT{Dependencies merged?}
            WAIT -->|Not yet| MERGEWAIT["PR Merge Wait Loop"]
            MERGEWAIT --> GET
            WAIT -->|Yes| DISPATCH{--sequential?}
            DISPATCH -->|Yes| SEQ["1.4: Sequential Dispatch"]
            DISPATCH -->|No| PAR["1.4a: Parallel Worktree Dispatch"]
            SEQ --> VALIDATE
            PAR --> VALIDATE
            VALIDATE["1.5: Validate SubagentResult"] --> CHECKPOINT
            CHECKPOINT["1.6: Update Checkpoint"] --> SYNC
            SYNC["1.6b: Markdown Status Sync"]
        end

        SYNC --> REBASE{Open PRs in phase?}
        REBASE -->|Yes| AUTOREBASE["1.4e: Auto-Rebase remaining PRs"]
        REBASE -->|No| GATE
        AUTOREBASE --> GATE

        GATE["Integrity Gate: compile + test + coverage + smoke"]
        GATE --> NEXTPHASE{More phases?}
        NEXTPHASE -->|Yes| PH05
    end

    NEXTPHASE -->|No| P2

    subgraph P2["Phase 2 — Epic Progress Report"]
        P2A[Generate PR_LINKS_TABLE] --> P2B[Resolve template placeholders]
        P2B --> P2C[Write epic-execution-report.md]
        P2C --> P2D[Finalize checkpoint]
    end

    P2 --> P3

    subgraph P3["Phase 3 — Verification"]
        P3A[Run full test suite on main] --> P3B[DoD Checklist]
        P3B --> P3C{All stories SUCCESS + PRs merged?}
        P3C -->|Yes| COMPLETE([COMPLETE])
        P3C -->|Critical path OK| PARTIAL([PARTIAL])
        P3C -->|Critical path failed| FAILED([FAILED])
    end

    style LEGACY fill:#e94560,color:#fff
    style DRYRUN fill:#533483,color:#fff
    style COMPLETE fill:#2d6a4f,color:#fff
    style PARTIAL fill:#e9c46a,color:#000
    style FAILED fill:#e94560,color:#fff
```

## 2. Per-Story Subagent Dispatch

```mermaid
flowchart LR
    ORCH["Orchestrator<br/>(stays on main)"] -->|"metadata only<br/>(RULE-001)"| SUB

    subgraph SUB["Subagent (worktree or sequential)"]
        direction TB
        INVOKE["/x-dev-story-implement {storyId}"] --> LIFECYCLE
    end

    LIFECYCLE -->|SubagentResult JSON| ORCH

    subgraph RESULT["SubagentResult"]
        direction TB
        R1["status: SUCCESS | FAILED | PARTIAL"]
        R2["commitSha, findingsCount, summary"]
        R3["prUrl, prNumber"]
        R4["reviewScores: specialist + techLead"]
        R5["coverageLine, coverageBranch, tddCycles"]
    end

    SUB -.-> RESULT
```

## 3. x-dev-story-implement — Per-Story Execution (9 Phases)

This is what happens **inside each subagent** when a story is dispatched.

```mermaid
flowchart TD
    ENTRY(["/x-dev-story-implement {storyId}"]) --> PH0

    subgraph PH0["Phase 0 — Preparation"]
        PH0A[Read story file] --> PH0B[Create branch: feat/storyId-desc]
        PH0B --> PH0C[Scope Assessment]
        PH0C --> TIER{Tier?}
        TIER -->|SIMPLE| SIMPLE["Skip 1B-1E, 4"]
        TIER -->|STANDARD| STANDARD["All phases"]
        TIER -->|COMPLEX| COMPLEX["All phases + pause after 4"]
    end

    PH0 --> PH05C{Has API interfaces?}
    PH05C -->|Yes| PH05["Phase 0.5: API Contract Generation + Approval Gate"]
    PH05C -->|No| PH1
    PH05 --> PH1

    subgraph PH1["Phase 1 — Planning"]
        PH1A["/x-dev-architecture-plan"] --> PH1PARALLEL
        subgraph PH1PARALLEL["Parallel Planning (SINGLE message)"]
            PH1B["/x-test-plan"]
            PH1C["/x-lib-task-decomposer"]
            PH1D["Event Schema Design<br/>(if event-driven)"]
            PH1E["Compliance Assessment<br/>(if compliance)"]
        end
    end

    PH1 --> PH2

    subgraph PH2["Phase 2 — TDD Implementation"]
        PH2A["Write Acceptance Test (AT-N)"] --> PH2B
        PH2B["RED: Write failing unit test"] --> PH2C
        PH2C["GREEN: Minimum code to pass"] --> PH2D
        PH2D["REFACTOR: Improve design"] --> PH2E{More tests?}
        PH2E -->|Yes| PH2B
        PH2E -->|No| PH2F["Verify AT-N passes"]
    end

    PH2 --> PH3["Phase 3 — Documentation"]
    PH3 --> PH4

    subgraph PH4["Phase 4 — Specialist Review"]
        PH4A["/x-review"] --> PH4B
        subgraph PH4B["Parallel Specialists"]
            SEC["Security"]
            QA["QA"]
            PERF["Performance"]
            DB["Database"]
            OBS["Observability"]
            DEVOPS["DevOps"]
            API["API"]
            EVENT["Event"]
        end
    end

    PH4 --> PH5["Phase 5 — Fix review findings"]

    PH5 --> PH6

    subgraph PH6["Phase 6 — PR Creation"]
        PH6A["git push -u origin feat/storyId-desc"]
        PH6A --> PH6B["gh pr create --base main"]
        PH6B --> PH6C["Body: 'Part of EPIC-{epicId}'"]
    end

    PH6 --> PH7

    subgraph PH7["Phase 7 — Tech Lead Review"]
        PH7A["/x-review-pr"] --> PH7B{40/40?}
        PH7B -->|GO| PH8
        PH7B -->|NO-GO| PH7C["Fix + re-review<br/>(max 2 cycles)"]
        PH7C --> PH7A
    end

    PH7 --> PH8

    subgraph PH8["Phase 8 — Final Verification"]
        PH8A[Update IMPLEMENTATION-MAP status] --> PH8B
        PH8B[Update story file status] --> PH8C
        PH8C[Jira transition to Done] --> PH8D
        PH8D[DoD Checklist: 24+ items] --> PH8E
        PH8E["git checkout main"]
    end

    PH8 --> DONE(["Return SubagentResult"])

    style SIMPLE fill:#2d6a4f,color:#fff
    style STANDARD fill:#16213e,color:#fff
    style COMPLEX fill:#e94560,color:#fff
```

## 4. Complete Skills Dependency Graph

```mermaid
graph TD
    EPIC["/x-dev-epic-implement"] -->|per story| LIFE["/x-dev-story-implement"]

    LIFE -->|Phase 1A| ARCH["/x-dev-architecture-plan"]
    LIFE -->|Phase 1B| TEST["/x-test-plan"]
    LIFE -->|Phase 1C| TASK["/x-lib-task-decomposer"]
    LIFE -->|Phase 2| IMPL["/x-dev-implement<br/>(TDD implementation)"]
    LIFE -->|Phase 3| ARCHUP["/x-dev-arch-update"]
    LIFE -->|Phase 4| REVIEW["/x-review"]
    LIFE -->|Phase 6| PR["gh pr create<br/>(PR targeting main)"]
    LIFE -->|Phase 7| LTREVIEW["/x-review-pr"]
    LIFE -->|Phase 8| E2E["/run-e2e<br/>(smoke tests)"]

    REVIEW -->|parallel| S1["Security Engineer"]
    REVIEW -->|parallel| S2["QA Engineer"]
    REVIEW -->|parallel| S3["Performance Engineer"]
    REVIEW -->|parallel| S4["Database Engineer"]
    REVIEW -->|parallel| S5["Observability Engineer"]
    REVIEW -->|parallel| S6["DevOps Engineer"]
    REVIEW -->|parallel| S7["API Engineer"]
    REVIEW -->|parallel| S8["Data Modeling Engineer"]

    EPIC -->|integrity gate| GATE["Integrity Gate Subagent<br/>(compile + test + coverage + smoke)"]
    EPIC -->|conflict resolution| CONFLICT["Conflict Resolution Subagent<br/>(Section 1.4c)"]
    EPIC -->|report| REPORT["Epic Progress Report<br/>(Phase 2)"]
    EPIC -->|status sync| JIRA["Jira API<br/>(MCP Atlassian)"]

    classDef skill fill:#16213e,stroke:#0f3460,color:#fff
    classDef specialist fill:#533483,stroke:#e94560,color:#fff
    classDef infra fill:#1a1a2e,stroke:#e94560,color:#fff

    class EPIC,LIFE,ARCH,TEST,TASK,IMPL,ARCHUP,REVIEW,LTREVIEW,E2E skill
    class S1,S2,S3,S4,S5,S6,S7,S8 specialist
    class PR,GATE,CONFLICT,REPORT,JIRA infra
```

## 5. Flag Decision Matrix

```mermaid
flowchart TD
    FLAGS([Flags Received]) --> F1{--single-pr?}
    F1 -->|Yes| LEGACY["LEGACY MODE<br/>Epic branch + mega-PR<br/>Skip all per-story PR logic"]
    F1 -->|No| F2{--sequential?}

    F2 -->|Yes| SEQMODE["SEQUENTIAL MODE<br/>Skip Phase 0.5<br/>One story at a time<br/>Skip auto-rebase"]
    F2 -->|No| F3{--strict-overlap?}

    F3 -->|Yes| STRICT["STRICT MODE<br/>Pre-flight partitions stories<br/>High-overlap → sequential queue<br/>Low-overlap → parallel batch"]
    F3 -->|No| ADVISORY["DEFAULT (ADVISORY)<br/>Pre-flight warns only<br/>All stories in parallel<br/>Auto-rebase resolves conflicts"]

    FLAGS --> F4{--auto-merge?}
    F4 -->|Yes| AUTO["AUTO-MERGE<br/>gh pr merge after approval<br/>Critical path order"]
    F4 -->|No| POLL["POLLING<br/>Wait for manual merge<br/>60s interval, 24h timeout"]

    FLAGS --> F5{--skip-review?}
    F5 -->|Yes| NOREVIEW["SKIP REVIEWS<br/>x-dev-story-implement skips<br/>Phases 4 and 7"]
    F5 -->|No| FULLREVIEW["FULL REVIEWS<br/>Specialist (Phase 4)<br/>Tech Lead (Phase 7)"]

    FLAGS --> F6{--skip-smoke-gate?}
    F6 -->|Yes| NOSMOKE["SKIP SMOKE<br/>Integrity gate runs<br/>Steps 1-4 only"]
    F6 -->|No| FULLSMOKE["FULL GATE<br/>Steps 1-5<br/>Including smoke tests"]

    style LEGACY fill:#e94560,color:#fff
    style ADVISORY fill:#2d6a4f,color:#fff
    style STRICT fill:#e9c46a,color:#000
    style SEQMODE fill:#16213e,color:#fff
```

## 6. Integrity Gate Flow

```mermaid
flowchart TD
    TRIGGER(["All phase stories complete<br/>+ All PRs merged"]) --> PRE

    PRE["Preconditions:<br/>1. mainShaBeforePhase captured<br/>2. git checkout main && git pull"] --> S1

    S1["Step 1: Compile<br/>{{COMPILE_COMMAND}}"] --> S1R{Pass?}
    S1R -->|No| FAIL1(["FAIL — compilation error"])
    S1R -->|Yes| S2

    S2["Step 2: Test<br/>{{TEST_COMMAND}} (full suite)"] --> S2R{Pass?}
    S2R -->|No| REGRESS["Regression Diagnosis:<br/>correlate with phase commits"]
    S2R -->|Yes| S3

    REGRESS --> REGSRC{Source found?}
    REGSRC -->|Yes| REVERT["git revert commitSha<br/>Mark story FAILED<br/>Block dependents"]
    REGSRC -->|No| PAUSE(["PAUSE — manual investigation"])

    S3["Step 3: Coverage<br/>line >= 95%, branch >= 90%"] --> S3R{Pass?}
    S3R -->|No| FAIL3(["FAIL — coverage below threshold"])
    S3R -->|Yes| S4

    S4{--skip-smoke-gate?}
    S4 -->|Yes| SKIP(["SKIP — smokeGate.status = SKIP<br/>Gate: PASS"])
    S4 -->|No| S5

    S5["Step 5: Smoke Tests<br/>{{SMOKE_COMMAND}}"] --> S5R{Pass?}
    S5R -->|No| SMOKEFAIL["Smoke Gate FAIL<br/>Correlate with phase stories"]
    S5R -->|Yes| PASS(["PASS — advance to next phase"])

    SMOKEFAIL --> OPERATOR(["Operator decides:<br/>--resume after fix<br/>or --skip-smoke-gate"])

    style PASS fill:#2d6a4f,color:#fff
    style FAIL1 fill:#e94560,color:#fff
    style FAIL3 fill:#e94560,color:#fff
    style PAUSE fill:#e9c46a,color:#000
    style SKIP fill:#533483,color:#fff
```

## 7. Auto-Rebase Flow (Section 1.4e)

```mermaid
flowchart TD
    TRIGGER(["PR merged to main"]) --> CHECK{Other open PRs<br/>in this phase?}
    CHECK -->|No| DONE(["No rebase needed"])
    CHECK -->|Yes| SEQCHECK{--sequential?}
    SEQCHECK -->|Yes| SKIP(["Skip — sequential mode"])
    SEQCHECK -->|No| ORDER["Sort remaining PRs<br/>by critical path priority"]

    ORDER --> LOOP

    subgraph LOOP["For each remaining PR"]
        FETCH["git fetch origin main"] --> REBASE
        REBASE["git rebase origin/main"] --> CONFLICT{Conflicts?}
        CONFLICT -->|No| PUSH["git push --force-with-lease"]
        CONFLICT -->|Yes| RESOLVE["Dispatch Conflict Resolution<br/>Subagent (Section 1.4c)"]
        RESOLVE --> RESOLVED{Resolved?}
        RESOLVED -->|Yes| CONTINUE["git rebase --continue"] --> PUSH
        RESOLVED -->|No| RETRY{attempts < 3?}
        RETRY -->|Yes| ABORT1["git rebase --abort<br/>Retry on next merge"]
        RETRY -->|No| ABORT2["git rebase --abort<br/>Mark FAILED<br/>Close PR"]
    end

    PUSH --> NEXT{More PRs?}
    NEXT -->|Yes| LOOP
    NEXT -->|No| DONE2(["Rebase complete"])

    style DONE fill:#2d6a4f,color:#fff
    style DONE2 fill:#2d6a4f,color:#fff
    style SKIP fill:#533483,color:#fff
    style ABORT2 fill:#e94560,color:#fff
```

## 8. Resume Workflow

```mermaid
flowchart TD
    RESUME(["--resume flag"]) --> READ["Read execution-state.json"]

    READ --> RECLASS["Step 1: Reclassify Statuses"]

    subgraph RECLASS_RULES["Status Reclassification"]
        direction LR
        R1["IN_PROGRESS → PENDING"]
        R2["SUCCESS → SUCCESS ✓"]
        R3["PR_CREATED → verify gh pr view"]
        R4["PR_MERGED → SUCCESS"]
        R5["FAILED (retries < 2) → PENDING"]
        R6["FAILED (retries >= 2) → FAILED"]
        R7["PARTIAL → PENDING"]
        R8["BLOCKED → reevaluate"]
    end

    RECLASS --> PRCHECK["Step 1b: Verify PR states<br/>gh pr view for each prNumber"]
    PRCHECK --> BLOCKED["Step 2: Reevaluate BLOCKED<br/>Check if blockers now SUCCESS+MERGED"]
    BLOCKED --> FAILCLOSE["Close PRs of FAILED stories<br/>gh pr close --comment"]
    FAILCLOSE --> FEED["Step 3: Feed to getExecutableStories()"]
    FEED --> CONTINUE(["Continue execution loop"])

    style CONTINUE fill:#2d6a4f,color:#fff
```

## 9. Summary Table — Skills Chain

| Layer | Skill | Invoked By | Purpose |
|-------|-------|------------|---------|
| **Orchestrator** | `/x-dev-epic-implement` | User | Epic-level orchestration, phase management |
| **Per-Story** | `/x-dev-story-implement` | Epic orchestrator subagent | Full 9-phase development cycle per story |
| **Planning** | `/x-dev-architecture-plan` | Lifecycle Phase 1A | Architecture plan with diagrams + ADRs |
| **Planning** | `/x-test-plan` | Lifecycle Phase 1B | Double-Loop TDD test plan with TPP |
| **Planning** | `/x-lib-task-decomposer` | Lifecycle Phase 1C | Task breakdown from test plan |
| **Implementation** | `/x-dev-implement` | Lifecycle Phase 2 | TDD Red-Green-Refactor per scenario |
| **Documentation** | `/x-dev-arch-update` | Lifecycle Phase 3 | Update architecture document |
| **Review** | `/x-review` | Lifecycle Phase 4 | 8 specialist engineers in parallel |
| **Review** | `/x-review-pr` | Lifecycle Phase 7 | Tech Lead 40-point holistic review |
| **Testing** | `/run-e2e` | Lifecycle Phase 8 | Smoke tests / post-deploy verification |
| **Infra** | Integrity Gate | Epic orchestrator | Compile + test + coverage + smoke between phases |
| **Infra** | Conflict Resolution | Epic orchestrator (1.4c) | Resolve rebase conflicts automatically |
| **Infra** | Jira API | Epic orchestrator (1.6b) | Transition stories/epic to Done |

## 10. Per-Story PR Flow (Default Model)

```
Orchestrator (main)          Story Subagent (worktree)          GitHub
─────────────────           ──────────────────────────         ────────
                                                                
  dispatch story ──────────►  /x-dev-story-implement                  
                              │                                 
                              ├─ Phase 0: create branch         
                              ├─ Phase 1: plan                  
                              ├─ Phase 2: implement (TDD)       
                              ├─ Phase 3: document              
                              ├─ Phase 4: /x-review ───────────► specialist reviews
                              ├─ Phase 5: fix findings          
                              ├─ Phase 6: gh pr create ────────► PR #N (→ main)
                              │           "Part of EPIC-{id}"   
                              ├─ Phase 7: /x-review-pr ────────► tech lead 40/40
                              ├─ Phase 8: finalize              
                              │                                 
  ◄─── SubagentResult ──────  return {prUrl, prNumber, ...}     
  │                                                             
  ├─ update checkpoint                                          
  ├─ update story markdown                                      
  ├─ update IMPLEMENTATION-MAP                                  
  │                                                             
  ├─ wait for PR merge ────────────────────────────────────────► PR merged ✓
  │  (auto-merge or polling)                                    
  │                                                             
  ├─ auto-rebase other PRs                                      
  │  (Section 1.4e)                                             
  │                                                             
  └─ integrity gate                                             
     (compile + test + coverage + smoke on main)                
```
