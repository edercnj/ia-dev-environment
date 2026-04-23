# x-task-plan — Full Protocol

Supplementary reference for `x-task-plan/SKILL.md`. The body of the skill carries the minimum viable contract; this document expands the full behavior envelope, corner cases, and historical context per ADR-0007 / EPIC-0047 carve-out policy.

## 1. Invocation Modes (Detailed)

### 1.1 Task-file-first mode (`--task-file <path>`) — EPIC-0038 canonical

Consumes a standalone `task-TASK-XXXX-YYYY-NNN.md` contract previously emitted by `x-story-plan` Phase 4a. The task file follows the `task-schema.md` specification (story-0038-0001) and is validated by `TaskFileParser`. Required sections:

- Header with `**Task ID:**`, `**Story:**`, `**Status:**`
- `## 1. Objetivo`
- `## 2. Contratos I/O` with `### 2.1 Inputs`, `### 2.2 Outputs`, `### 2.3 Testabilidade`
- `## 3. Definition of Done`
- `## 4. Dependências`
- `## 5. Plano de implementação` (filled by this skill)

Validation rules:

- **RULE-TF-01 Testability:** §2.3 must contain exactly one checked declaration among `[x] INDEPENDENT` / `[x] REQUIRES_MOCK` / `[x] COALESCED`. Empty or multi-selected aborts with exit 3.
- **RULE-TF-02 Outputs:** §2.2 must list at least one grep/assert/test-verifiable output.

### 1.2 Story-scoped mode — legacy (epics 0025-0037)

Reads the task from `## 8. Tasks` of a story file. Preserved for backward compatibility. The task ID is located by exact string match (`### TASK-XXXX-YYYY-NNN:`).

## 2. P1-P5 Lifecycle (EPIC-0049)

This skill adopts the canonical planning-versioning lifecycle (RULE-007). Placement inside the workflow:

| Step | When | Child skill | Behavior on `--no-commit` | Behavior on `--dry-run` |
|------|------|-------------|---------------------------|-------------------------|
| P1 — detect worktree | Start | `x-git-worktree detect-context` | Skip (orchestrator owns lifecycle) | Skip |
| P2 — ensure `epic/<ID>` branch | After P1 | `x-internal-epic-branch-ensure` | Skip | Skip |
| Phases 0-4 (original) | Middle | (inline) | Run unchanged | Run unchanged |
| Phase 5 (Write Plan) | Middle | (inline) | Run; file written | Run; file written |
| **Phase 5.4 — Planning Status Propagation** (alias Step P4) | After Phase 5 | `x-git-commit` (v1) / staged-only (v2 batch) | SKIP commit; log `"[no-commit] Plan written; commit deferred to caller"` | SKIP commit; log `"dry-run, skipping commit"` |
| P5 — push | End | `x-git-push` | Skip | Skip |

The P4 step is an **alias** over the pre-existing Phase 5.4 (Planning Status Propagation) which already orchestrates the commit. No additional P4 invocation is issued — adding one would double-commit. The alias section exists in the skill body purely so the P1-P5 naming is readable end-to-end.

## 3. `--no-commit` Contract (story-0049-0017)

| Aspect | `--no-commit=false` (default) | `--no-commit=true` (batch) |
|--------|-------------------------------|----------------------------|
| Plan file written to disk | Yes | Yes |
| Status flipped `Pendente -> Planejada` | Yes | Yes |
| `git add` of plan + task file | Yes | Yes |
| `x-git-commit` invoked | Yes | **NO** (deferred to caller) |
| Response `commitSha` | non-null SHA | `null` |
| Re-invocation semantics | Idempotent (staleness check) | Idempotent; flipping the flag between runs alternates commit behavior |

**Caller contract (e.g., `x-story-plan`):** when invoking N tasks with `--no-commit=true`, the caller MUST aggregate all written paths and issue ONE consolidated `x-planning-commit` call covering every plan + status update — producing a single commit per story instead of N commits.

**Backward compat:** absence of `--no-commit` (or explicit `--no-commit=false`) preserves pre-EPIC-0049 behavior byte-for-byte.

## 4. Failure Matrix

| Scenario | Exit code | Mitigation |
|----------|-----------|------------|
| `x-internal-epic-branch-ensure` fails (P2) | 5 | Repair remote; re-run |
| `StatusFieldParserCli` exit 20 | 20 | Story-0049-0010 integrity gate recovery |
| `StatusFieldParserCli` exit 40 | 40 | Invalid transition — check source markdown |
| `x-git-commit` failure in Phase 5.4 | propagate | Investigate pre-commit chain |
| `x-git-push` failure (P5) | 0 (soft-fail) | Operator pushes manually |

## 5. Telemetry

Phase markers emitted:

- `Phase-P1-Worktree-Detect`
- `Phase-P2-Epic-Branch-Ensure`
- Original Phase 1-5 markers
- `Phase-P5-Push`

P4 reuses the existing telemetry of Phase 5.4 (no new phase marker to avoid double-counting).
