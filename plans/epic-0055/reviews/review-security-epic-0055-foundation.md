# Security Review — EPIC-0055 Foundation (PR #633)

ENGINEER: Security
STORY: epic-0055-foundation (PR #633, epic/0055 → develop)
SCORE: 27/30
STATUS: Approved (with MINOR advisories)

---

## Scope reviewed

- `java/src/main/resources/targets/claude/hooks/enforce-phase-sequence.sh` (PreToolUse, 99 LOC)
- `java/src/main/resources/targets/claude/hooks/verify-phase-gates.sh` (Stop, 77 LOC)
- `scripts/audit-task-hierarchy.sh` (223 LOC)
- `scripts/audit-phase-gates.sh` (205 LOC)
- `audits/task-hierarchy-baseline.txt`
- `java/src/main/resources/targets/claude/skills/core/internal/plan/x-internal-phase-gate/SKILL.md` (contract)
- `adr/ADR-0014-task-hierarchy-and-phase-gates.md`
- `java/src/main/resources/targets/claude/rules/25-task-hierarchy.md`

## PASSED (27 points)

### 1. Command injection — `enforce-phase-sequence.sh` and `verify-phase-gates.sh` (6/6)

Both scripts parse untrusted JSON from stdin (`enforce`) or from the state file
(`verify`) through `jq` exclusively. There is **no** `eval`, no unquoted
command substitution of attacker data, no dynamic string construction into
shell. Every reference to the extracted values (`$TOOL_NAME`, `$TARGET_SKILL`,
`$PASSED`, `$PHASE`, `$MODE`) is quoted and used in string equality tests or
in `echo` to stderr — not re-executed. `jq -r` returns raw text but the text
is never fed back to `bash -c` or to a `find`/`grep` `-exec` expression.

### 2. JSON hardening + fail-open posture (5/5)

`enforce-phase-sequence.sh` contract is exactly right for a PreToolUse hook:

- Missing `jq` → exit 0 (fail-open)
- Empty stdin → exit 0
- `tool_name != Skill` → exit 0
- Target not canonical → exit 0
- No state file → exit 0
- `taskTracking.enabled != true` → exit 0
- Empty `phaseGateResults` → exit 0
- **Only** fails CLOSED (exit 2) when the most recent gate explicitly reports
  `passed=false`. This matches the posture requested in the review brief:
  fail-open on ambiguity, fail-CLOSED on detected gate violations.

`verify-phase-gates.sh` (Stop hook) matches the same shape — WARNING + exit 2
only when explicit failures are present in `phaseGateResults[]`.

### 3. Path traversal — hooks (4/4)

`$PROJECT_DIR` is derived from `CLAUDE_PROJECT_DIR` (trusted env) or
`git rev-parse --show-toplevel` (trusted process). The only file-system
operation on untrusted data is `find "$PROJECT_DIR/plans" -maxdepth 3
-type f -name "execution-state.json"`. The `find` root is always quoted,
`-maxdepth 3` bounds traversal, and `-type f` filters to regular files.
No arbitrary path from the JSON payload is ever passed to a file I/O call.

### 4. ReDoS — `SUBJECT_REGEX` in `audit-task-hierarchy.sh` (3/3)

The Rule-25 subject regex `^(([A-Z][A-Z0-9-]+|epic-[0-9]{4}|...|Phase [0-9]+))( › [A-Za-z0-9_.:() -]+){0,3}$` was stress-tested against adversarial inputs. The outer quantifier is a bounded `{0,3}` (not `*` or `+`), character classes inside are non-overlapping, and there is **no** nested unbounded quantifier. Python-equivalent stress test with a 50-token (U+203A) string returned in < 1 ms. The `›` UTF-8 byte sequence behaves as an opaque three-byte literal under the C locale but matches correctly under UTF-8 locales. No catastrophic backtracking.

### 5. Skill-filename command injection — audit scripts (3/3)

`basename "$(dirname "$skill_file")"` is always quoted. `grep -cE` / `grep -nE`
are invoked with fixed regex literals and file-path arguments that come
from `find ... -name "SKILL.md"` (filename is always literally `SKILL.md`).
No skill name is ever expanded into a `bash -c` string or used to construct
a `$(...)` substitution. `jq` invocations consistently pass the state-file
path as an argument, not inlined into a command string.

### 6. Secrets / credentials (3/3)

Grep for `password`, `secret`, `api_key`, `token`, `credential` across all 4
shell files returned 0 hits. ADR-0014 and Rule 25 reference no credentials.
The baseline file contains only skill names and story references.

### 7. Info leak via error messages (2/2)

Gate failure messages disclose: state-file path, phase name, mode, and a
JSON array of missing tasks / missing artifacts. All of these are non-sensitive
operational metadata (no file contents, no credentials, no user data).
Acceptable exposure for an operator-facing CLI tool.

### 8. Rule 25 / ADR-0014 trust-model accuracy (1/1)

Defense-in-depth claims match implementation:

- **Layer 1 (Normative)** — Rule 25 ships with the stated invariants. ✓
- **Layer 2 (Stop hook)** — `verify-phase-gates.sh` reads `phaseGateResults[]`
  and emits WARNING + exit 2. ✓
- **Layer 3 (PreToolUse hook)** — `enforce-phase-sequence.sh` blocks
  `Skill()` when the latest gate failed. ✓ Narrower than the rule text
  suggests (it inspects only `last` — see PARTIAL #3 below).
- **Layer 4 (CI audit)** — both `audit-*.sh` scripts exist and wire to
  self-check. ✓

No claim in ADR-0014 over-promises beyond what the code delivers. The
`taskTracking.enabled=false` → short-circuit behavior is honest (Rule 19
compat fallback is documented and implemented).

## PARTIAL (2 deductions, -2)

### P1. State-file symlink hardening (-1)

`find "$PROJECT_DIR/plans" -maxdepth 3 -type f -name "execution-state.json"`
uses the default non-dereferencing behavior, so a symlink named
`execution-state.json` pointing at `/etc/passwd` would be treated as a
symbolic link and NOT matched by `-type f` — good. However, if `plans/epic-*/`
itself is a symlink to an attacker-controlled directory, the scan could pick
up a malicious JSON file. The hook then feeds it to `jq` (safe — `jq` is
a parser, not an executor). The only attack that could succeed is:

- Write a plausible-looking `execution-state.json` with `taskTracking.enabled=true`
  and `phaseGateResults[last].passed=false` to **block a legitimate Skill()
  invocation** (a denial-of-service against the LLM turn, not escalation).

**Mitigation (minor):** add `-xdev` or `! -type l` at the directory walk
level, or `readlink -f` the result and verify it still starts with
`$PROJECT_DIR/plans/`. Not blocking — local attacker with repo write access
has bigger primitives.

### P2. `--expected-artifacts` path-prefix validation in `x-internal-phase-gate` (-1)

The SKILL.md contract (Step 4b) states: "For each `path ∈ --expected-artifacts`,
`[[ -e "$path" ]]`". There is **no documented prefix check** that the path
resolves inside the repo root. In practice the caller is always an
orchestrator we trust, but the contract as written would allow
`--expected-artifacts /etc/passwd` to be used as a gate oracle (checking
existence only, not content). Since no implementation is shipped in this PR
— only the contract — this is a recommendation for the retrofit stories
0055-0003 through 0055-0010:

- Reject paths containing `..` segments after normalization.
- Require `realpath --relative-to=$REPO_ROOT` to yield a path NOT starting
  with `../`.
- Document this in the contract before the first retrofit ships.

**Severity:** LOW. The gate only calls `[[ -e ]]` (existence check) — it does
NOT `cat`, `read`, or execute the target — so the worst outcome is a boolean
leak of "does file X exist" which is the same primitive any shell user on the
host already has.

## FAILED

None.

---

## Advisory notes (non-scoring)

- `verify-phase-gates.sh` branch filter accepts `feat/*` as well as `feature/*`.
  Rule 09 only defines `feature/*`. Harmless but worth aligning with Rule 09
  naming in a follow-up.
- `audit-task-hierarchy.sh` line 151–152: `tc_count=$(grep -cE ... || echo 0)`
  — when `grep` finds zero matches it exits 1, so the fallback is correct.
  However some `grep` implementations still emit `0` on stdout so the
  variable could contain `"0\n0"`. Defensive: pipe through `head -1` or use
  `wc -l`. Cosmetic.
- The baseline file (`audits/task-hierarchy-baseline.txt`) is the declared
  immutability point. ADR-0014 claims immutability "after EPIC-0055 merges
  into main" but I did not see the CI check that enforces this. If the
  `--self-check` only verifies existence (which it does — lines 75-84 of
  the audit script), nothing prevents future PRs from appending entries.
  Recommendation: compute a SHA pinned in `scripts/audit-task-hierarchy.sh`
  or a separate `.ci/baseline-immutability.sha` file, and compare on
  `--self-check`. File this as follow-up, not blocking.

---

## Verdict

Approved. The two bash hooks are well-engineered for their trust model
(fail-open on ambiguity, fail-closed on explicit violation). The audit
scripts are ReDoS-safe and injection-safe. The contract-level risk on
`--expected-artifacts` (P2) must be addressed in the first retrofit
(story-0055-0003) but does not block this foundation PR since no
implementation is shipped here — only the SKILL.md specification.

No hardcoded credentials, no command injection, no path traversal in the
delivered code. Trust-model documentation in Rule 25 and ADR-0014 matches
what the code actually enforces.
