# Scope Assessment Reference

> **Context:** This reference details SIMPLE/STANDARD/COMPLEX classification.
> Part of x-story-implement skill.

## Scope Assessment

Analyze the story content to classify its scope tier. The classification determines which phases execute:

**Classification Criteria:**

| Criterion | How to detect |
|-----------|--------------|
| Components affected | Count distinct `.java`/`.kt`/`.py`/`.ts`/`.go`/`.rs` file mentions in tech description |
| New endpoints | Count `POST/GET/PUT/DELETE/PATCH /path` patterns in data contracts |
| Schema changes | Presence of "migration script", "ALTER TABLE", "CREATE TABLE", "DROP TABLE", "ADD COLUMN" |
| Compliance | `compliance:` field with value other than "none" |
| Dependents | Count stories that depend on this one (from IMPLEMENTATION-MAP) |

**Tier Classification:**

| Tier | Criteria | Phase Behavior |
|------|----------|---------------|
| SIMPLE | <=1 component, 0 endpoints, 0 schema changes, no compliance | Skip phases 1B, 1C, 1D, 1E |
| STANDARD | 2-3 components OR 1-2 new endpoints | All phases execute normally |
| COMPLEX | >=4 components OR schema changes OR compliance requirement | All phases + stakeholder review after Phase 2 |

**Elevation Rules:**
- Compliance **always** elevates to COMPLEX regardless of other criteria
- Schema changes **always** elevate to at least COMPLEX
- A single COMPLEX criterion is sufficient for COMPLEX classification

**Display the assessment before proceeding:**

```
Scope Assessment: [TIER]
> [Phases that will execute]
> Rationale: [justification]
> [Override instruction if SIMPLE]
```

**`--full-lifecycle` Flag:**
If the user passes `--full-lifecycle`, force full execution regardless of tier:
- All phases execute (equivalent to STANDARD)
- Display: "Scope override: running full lifecycle as requested"

**SIMPLE Execution Flow:**
Phases 0 (Prepare) > 1A (Architecture) > 2 (Task Execution Loop) > 3 (Documentation) > 6 (Commit + PR) > 8 (Final Verification). Skips 1B (Impl Plan), 1C (Task Decomposer), 1D (Event Schema), 1E (Security), and the heavyweight review pipeline (Phases 4, 5, 7).

**COMPLEX Execution Flow:**
All phases execute normally (0 through 8). After **Phase 4 (Specialist Review)**, **pause** with:
"Scope COMPLEX -- stakeholder review required. Review all specialist findings and confirm to proceed with Phase 5 (Remediation), Phase 6 (PR), Phase 7 (Tech Lead Review), and Phase 8 (Final Verification)."
Wait for developer confirmation before executing Phase 5.

**Default Behavior:**
If scope assessment cannot be performed (e.g., story content unavailable), default to STANDARD (all phases execute, no pause). No error is raised.
