# Rule 21 — CI-Watch (RULE-045-01)

> **Related:** Rule 13 (Skill Invocation Protocol — CI-Watch invoker MUST use Pattern 1
> INLINE-SKILL). Rule 19 (Backward Compatibility — CI-Watch is no-op for schema v1
> epics). Applies to orchestrating skills that open PRs when
> `planningSchemaVersion == "2.0"`; v1 epics are fully exempt.

## Rule

Every orchestrating skill that invokes `x-pr-create` under schema v2 MUST invoke
`x-pr-watch-ci` immediately after the PR is created and before presenting the
APPROVAL GATE (EPIC-0042 / EPIC-0043). CI-Watch is the **default** behaviour for
schema v2 — no flag is needed to enable it. The only sanctioned opt-out is the
`--no-ci-watch` flag, which suppresses the watch and proceeds directly to the
APPROVAL GATE. This opt-out exists exclusively for CI/automation pipelines where a
human reviewer is not present and the external CI result is irrelevant.

## Fallback Matrix

| `planningSchemaVersion` | `--no-ci-watch` present | Behaviour |
| :--- | :--- | :--- |
| absent / `"1.0"` | — | **V1 no-op.** `x-pr-watch-ci` is NOT invoked. Respects Rule 19. |
| `"2.0"` | absent | **V2 active (default).** `x-pr-watch-ci` invoked after every `x-pr-create`. |
| `"2.0"` | present | **V2 skipped (opt-out).** `x-pr-watch-ci` skipped; APPROVAL GATE presented immediately. |
| any other value | — | **V1 no-op fallback.** Treated as unknown version; see Rule 19 `SCHEMA_VERSION_INVALID_VALUE`. |

The `SchemaVersionResolver` (story-0038-0008) is the single source of truth for
version detection. Orchestrators MUST NOT implement their own version detection
logic — delegate to the resolver.

## Rationale

EPIC-0045 identified that orchestrating skills transitioned from `x-pr-create`
directly to the APPROVAL GATE without waiting for: (a) remote CI checks (`gh pr
checks`) to complete, and (b) the `copilot-pull-request-reviewer[bot]` to post its
review. Typical Copilot latency is 30–180 s, meaning the APPROVAL GATE's FIX-PR
option (EPIC-0043) fired before any automated review was available. The operator
either had to wait and re-invoke manually, or proceed blind.

CI-Watch encapsulates polling and detection in a single reusable primitive
(`x-pr-watch-ci`), making it straightforward for all current and future orchestrating
skills to wait for CI before acting. Making it the **default** in schema v2 ensures
new orchestrators cannot accidentally skip it.

## Enforcement

- `scripts/audit-rule-20.sh` (EPIC-0045): grep-based CI guard that locates every
  real `Skill(skill: "x-pr-create"` invocation in
  `java/src/main/resources/targets/claude/skills/core/**/SKILL.md` and verifies that
  the same file also contains `Skill(skill: "x-pr-watch-ci"` OR declares
  `--no-ci-watch`. Exit 0 = clean; exit 1 = violation list on stderr.
- `Rule20AuditTest.java` invokes the audit script: happy path exits 0; regression
  fixture (SKILL.md with `x-pr-create` but no `x-pr-watch-ci`) exits 1.
- `RulesAssemblerCiWatchTest.java`: verifies `21-ci-watch.md` is copied into the
  generated `.claude/rules/` output by `RulesAssembler`.

## Forbidden

- Invoking an orchestrator that opens a PR without passing the `x-pr-watch-ci`
  exit code to the APPROVAL GATE (EPIC-0043) when `planningSchemaVersion == "2.0"`.
- Creating a new orchestrating skill that calls `x-pr-create` without immediately
  calling `x-pr-watch-ci` (or declaring `--no-ci-watch` with explicit rationale).
- Hard-failing when `planningSchemaVersion` is absent or `"1.0"` — CI-Watch must
  remain a silent no-op for v1 epics (Rule 19 RULE-045-02).
- Implementing custom CI polling inside an orchestrating skill instead of delegating
  to `x-pr-watch-ci`.
- Using bare-slash (`/x-pr-watch-ci`) in delegation contexts — use Rule 13 Pattern 1
  INLINE-SKILL: `Skill(skill: "x-pr-watch-ci", args: "--pr-number N")`.

## Audit Command

```bash
# Standard run (zero violations expected on clean codebase):
scripts/audit-rule-20.sh

# Run from the java/ module directory (for Maven integration):
../scripts/audit-rule-20.sh
```

**Exit codes:** 0 = AUDIT PASSED, 1 = AUDIT FAILED (violations listed), 2 = execution error.

The script enforces: for every file containing a real `Skill(skill: "x-pr-create"` call,
the same file must contain `Skill(skill: "x-pr-watch-ci"` OR the string `--no-ci-watch`.
