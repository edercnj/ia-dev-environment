# DevOps Review — EPIC-0055 Foundation (PR #633)

```
ENGINEER: DevOps
STORY: epic-0055-foundation (PR #633)
SCORE: 14/20
STATUS: Partial
```

---

## Scope Reviewed

- `scripts/audit-task-hierarchy.sh` (223 lines, exit 25)
- `scripts/audit-phase-gates.sh` (205 lines, exit 26)
- `java/src/main/resources/targets/claude/hooks/verify-phase-gates.sh`
- `java/src/main/resources/targets/claude/hooks/enforce-phase-sequence.sh`
- `HookConfigBuilder.java` / `HooksAssembler.java`
- `audits/task-hierarchy-baseline.txt`
- 10 regenerated golden `settings.json` files (3 spot-checked)
- `.github/workflows/*.yml` for CI wiring

---

## PASSED (14 pts)

### 1. Scripts self-contained and ergonomic (+3)

Both audit scripts are pure Bash with `jq` + `git` as the only external deps.
`--help`, `--self-check`, `--json`, and `--skills-root` / `--baseline`
override flags are present on both. `set -u` is set. Idempotent by
construction (read-only scans, no mutation of any artifact). `--self-check`
resolves OK for both scripts against the repo layout.

Evidence:

```
$ scripts/audit-task-hierarchy.sh --self-check  → self-check: OK, exit 0
$ scripts/audit-phase-gates.sh --self-check     → self-check: OK, exit 0
$ scripts/audit-task-hierarchy.sh --json        → {"exit_code":0,...} exit 0
```

### 2. JSON well-formed in all 10 goldens (+3)

Spot-checked 4 of 10 profiles (`java-quarkus`, `java-spring-fintech-pci`,
`java-spring-neo4j`, `java-spring-clickhouse`) — all parse with
`python3 -c "json.load(...)"`. Each carries exactly one reference to
`enforce-phase-sequence.sh` (under `PreToolUse[*]/hooks[1]`) AND one to
`verify-phase-gates.sh` (under `Stop[0]/hooks[2]`). Matcher `"*"` on
PreToolUse is correct — the hook filters on `tool_name == "Skill"`
internally, so wildcard is the right mechanical contract.

Timeout = 5 seconds, identical to sibling telemetry hooks. Appropriate
given the fail-open `jq`-only contract (documented P95 <50ms for
verify-phase-gates.sh).

### 3. HooksAssembler wiring correct (+2)

`HooksAssembler.TELEMETRY_SCRIPTS` (lines 59-70) includes
`verify-phase-gates.sh` and `enforce-phase-sequence.sh` alongside the
9 pre-existing telemetry scripts. Both source files exist under
`java/src/main/resources/targets/claude/hooks/`. `makeExecutable()` is
called post-copy (line 147), so the output in every generated
`.claude/hooks/` directory receives mode 755 regardless of source
permissions.

### 4. Script execute bit on goldens (+2)

Spot-checked `java-quarkus`, `java-spring-fintech-pci`, `java-spring-neo4j`
— all 3 goldens show `-rwxr-xr-x` (755) on both new hook scripts. The
`makeExecutable()` path from `HooksAssembler` works as designed and the
goldens committed on disk are correctly re-permissioned.

### 5. Fail-open contracts are correct (+2)

Both `verify-phase-gates.sh` and `enforce-phase-sequence.sh` implement
clean fail-open: missing `jq`, missing state file, `taskTracking.enabled
!= true`, empty `phaseGateResults`, or off-branch checkout → exit 0
silently. `CLAUDE_PHASE_GATE_DISABLED=1` global opt-out honored. Only
explicit detected failures emit exit 2 + human-readable stderr. This
aligns with the Rule 24 precedent (`verify-story-completion.sh`) and
avoids breaking legacy epics.

### 6. Baseline content disciplined (+2)

`audits/task-hierarchy-baseline.txt` lists exactly the 8 canonical
orchestrators, each tagged `# retirado-por: story-0055-000X`. Format
comment is clear and explicit. No ghost entries.

---

## PARTIAL (0 pts awarded — 4 deducted)

### 7. CI wiring to GitHub Actions absent (-3)

**Finding:** Neither `audit-task-hierarchy.sh` nor `audit-phase-gates.sh`
is invoked by any workflow in `.github/workflows/`. Grep across
`ci-release.yml` and `security-scan-weekly.yml` returns ONLY
`audit-model-selection.sh`.

**Impact:** Camada 4 (CI audit) of Rule 25 is de facto unarmed on PR
#633 and on all follow-up PRs until a separate commit wires the
scripts into CI. The rule text says "CI script ... scans every
SKILL.md ... fails the CI build with `TASK_HIERARCHY_VIOLATION` /
`PHASE_GATE_VIOLATION`" — that claim is currently false.

**Recommendation:** Add a step to `ci-release.yml` after the
`audit-model-selection.sh` step:

```yaml
- name: Audit task hierarchy (Rule 25)
  run: scripts/audit-task-hierarchy.sh
- name: Audit phase gates (Rule 25)
  run: scripts/audit-phase-gates.sh
```

This is a FOUNDATION PR and the scripts currently pass (0 violations),
so the omission is low-risk for THIS PR — but the foundation is
incomplete until CI wiring lands. Flag for story-0055-0003 scope or
a fast-follow patch.

### 8. Baseline immutability claim NOT enforced (-1)

**Finding:** `audits/task-hierarchy-baseline.txt` declares itself
"IMMUTABLE after EPIC-0055 merges into main" and the rule text says
"CI rejects additions post-merge (separate immutability check in
`audit-task-hierarchy.sh --self-check`)". However `self_check()` in
the script (lines 73-84) only verifies that the file *exists* — it
does not compare contents against a pinned hash, does not diff
against `HEAD~1`, and does not refuse additions.

**Impact:** The 8-orchestrator exemption list can be silently extended
by any future PR author with no audit regression signal. This erodes
the deprecation-window guarantee.

**Recommendation:** Either (a) add a byte-level digest check to
`--self-check` (pin SHA-256 post-merge), or (b) remove the
"immutability" claim from the file header and the rule text until a
real check ships. Also applies to the identical claim structure in
`execution-integrity-baseline.txt` (EPIC-0052).

---

## FAILED (0 pts — 2 deducted)

### 9. Source hook scripts lack execute bit (-2)

**Finding:** `java/src/main/resources/targets/claude/hooks/verify-phase-gates.sh`
and `enforce-phase-sequence.sh` are mode **644** (`-rw-r--r--`) in the
repo working tree.

Evidence:

```
-rw-r--r--  verify-phase-gates.sh
-rw-r--r--  enforce-phase-sequence.sh
-rwxr-xr-x  scripts/audit-task-hierarchy.sh
-rwxr-xr-x  scripts/audit-phase-gates.sh
```

**Impact:** In practice the `HooksAssembler.makeExecutable()` path
fixes permissions at copy time, so end-users get 755 in their
`.claude/hooks/`. BUT: developers who run the source scripts directly
for debugging (e.g. `bash java/src/.../verify-phase-gates.sh` piped
from stdin) hit a usability wall; they must prefix `bash`. The 9
pre-existing telemetry scripts follow the same pattern in this repo,
so this is a consistency-preserving defect rather than a regression
— which is why I'm deducting 2 rather than 4. Request either
(a) `chmod +x` on both source files AND all sibling telemetry
scripts, or (b) an explicit DevOps note acknowledging the pattern.

---

## Cross-Profile Consistency Check

Spot-checked `java-quarkus`, `java-spring-fintech-pci`, `java-spring-neo4j`,
`java-spring-clickhouse`:

| Profile | JSON valid | PreToolUse wires enforce | Stop wires verify-phase-gates | Execute bit |
| :--- | :--- | :--- | :--- | :--- |
| `java-quarkus` | OK | OK | OK | 755 |
| `java-spring-fintech-pci` | OK | OK | OK | 755 |
| `java-spring-neo4j` | OK | OK | OK | 755 |
| `java-spring-clickhouse` | OK | OK | OK | 755 |

Consistent shape across all 4 spot-checked profiles. The 10-profile fan-out
is mechanically correct — each profile received an identical 10-line
diff to `settings.json` plus the 2 hook scripts.

---

## Summary

| Dimension | Pts |
| :--- | ---: |
| Scripts self-contained + ergonomic | 3 |
| Golden JSON well-formed | 3 |
| HooksAssembler wiring | 2 |
| Execute bit on goldens | 2 |
| Fail-open contracts | 2 |
| Baseline discipline | 2 |
| CI workflow wiring | −3 |
| Baseline immutability | −1 |
| Source script perms | −2 |
| **Total** | **14/20** |

## Verdict

**Partial.** The code is structurally correct and will not regress
anything merged today. Three DevOps gaps prevent an "Approved":

1. The CI audit contract described in Rule 25 §Enforcement Layers is
   not wired into any workflow.
2. The "immutable baseline" invariant is asserted but not checked.
3. Source hook files are not executable (mirror of an existing repo
   antipattern rather than a new regression).

None are blocking for PR #633 as a foundation commit, but each
should be tracked in a follow-up before story-0055-0003 closes.
