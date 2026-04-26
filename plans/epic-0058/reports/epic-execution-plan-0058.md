# Epic Execution Plan — EPIC-0058: Audit Scripts Lifecycle & Generation

**Generated:** 2026-04-26T00:00:00Z
**Epic ID:** 0058
**Mode:** sequential
**Story Count:** 8
**Flow Version:** 2

---

## Kahn Phases (Execution Order)

| Phase | Stories | Parallelism |
| :--- | :--- | :--- |
| 0 | story-0058-0001 | Sequential (1 story) |
| 1 | story-0058-0002, story-0058-0003, story-0058-0004, story-0058-0005 | Sequential (4 stories) |
| 2 | story-0058-0006 | Sequential (1 story) |
| 3 | story-0058-0007 | Sequential (1 story) |
| 4 | story-0058-0008 | Sequential (1 story) |

---

## Dependency Graph

```
story-0058-0001 (root)
├── story-0058-0002 (leaf)
├── story-0058-0003
│   └── story-0058-0006
│       └── story-0058-0007
│           └── story-0058-0008
├── story-0058-0004
│   └── story-0058-0006 (above)
└── story-0058-0005
    └── story-0058-0006 (above)
```

---

## Critical Path

`story-0058-0001 → story-0058-0003 → story-0058-0006 → story-0058-0007 → story-0058-0008`

Critical path length: **5 stories**

---

## Story Summary

| Story | Title | Phase | Blocked By | Status |
| :--- | :--- | :--- | :--- | :--- |
| story-0058-0001 | Formalizar Rule 25 "Audit Gate Lifecycle" + ADR | 0 | — | Pendente |
| story-0058-0002 | Publicar catálogo canônico `docs/audit-gates-catalog.md` | 1 | 0058-0001 | Pendente |
| story-0058-0003 | Implementar `audit-flow-version.sh` (Rule 19) | 1 | 0058-0001 | Pendente |
| story-0058-0004 | Implementar `audit-epic-branches.sh` (Rule 21) | 1 | 0058-0001 | Pendente |
| story-0058-0005 | Implementar `audit-skill-visibility.sh` (Rule 22) | 1 | 0058-0001 | Pendente |
| story-0058-0006 | Criar `ScriptsAssembler` + source-of-truth | 2 | 0058-0003, 0058-0004, 0058-0005 | Pendente |
| story-0058-0007 | Regenerar golden files e asserts GoldenFileTest | 3 | 0058-0006 | Pendente |
| story-0058-0008 | Workflow CI `audit.yml` + sub-assembler | 4 | 0058-0007 | Pendente |

---

## Overlap Matrix

`null` (sequential mode — overlap analysis not computed)

---

## Execution Envelope

```json
{"epicId":"0058","mode":"sequential","phases":[{"index":0,"stories":["story-0058-0001"]},{"index":1,"stories":["story-0058-0002","story-0058-0003","story-0058-0004","story-0058-0005"]},{"index":2,"stories":["story-0058-0006"]},{"index":3,"stories":["story-0058-0007"]},{"index":4,"stories":["story-0058-0008"]}],"overlapMatrix":null,"overlapSeverity":null,"criticalPath":["story-0058-0001","story-0058-0003","story-0058-0006","story-0058-0007","story-0058-0008"],"planPath":"plans/epic-0058/reports/epic-execution-plan-0058.md","storyCount":8,"strictOverlap":false}
```
