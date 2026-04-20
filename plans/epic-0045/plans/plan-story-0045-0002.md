# Implementation Plan — story-0045-0002

## Story
Add Rule 20 (CI-Watch) + regression audit

## Affected Layers and Components

| Layer | Component | Change Type |
|---|---|---|
| Config/Rules | `java/src/main/resources/targets/claude/rules/21-ci-watch.md` | NEW FILE |
| Application | `CoreRulesWriter.java` | VERIFY (auto-picks up via copyCoreRules) |
| Test | `RulesAssemblerCiWatchTest.java` | NEW FILE |
| Ops | `scripts/audit-rule-20.sh` | NEW FILE |
| Test | `dev/iadev/ci/Rule20AuditTest.java` | NEW FILE |
| Doc | `CLAUDE.md` | UPDATE |
| Golden | All golden rule dirs | UPDATE |

## Design Notes

### Rule numbering
Slot 20 is double-occupied by `20-interactive-gates.md` (EPIC-0043) and
`20-telemetry-privacy.md` (EPIC-0040). The next available numeric slot is **21**.
File will be named `21-ci-watch.md` to avoid collision. Story description was
written before those slots were occupied; using 21 is the safe, non-destructive choice.

### Rule auto-inclusion
`CoreRulesWriter.copyCoreRules()` iterates `targets/claude/rules/*.md` sorted.
No code change is needed to include `21-ci-watch.md` — just creating the file
in the correct directory is sufficient.

### Test coverage
- `RulesAssemblerCiWatchTest.java` — verifies `21-ci-watch.md` is assembled
  into output rules directory (uses real classpath, same pattern as
  `RulesAssemblerInteractiveGatesTest`)
- `Rule20AuditTest.java` — uses ProcessBuilder to invoke `scripts/audit-rule-20.sh`
  - Happy path: production codebase exits 0
  - Regression: mock a violating SKILL.md (x-pr-create without x-pr-watch-ci),
    verify exit 1
  - Opt-out: x-pr-create with --no-ci-watch, verify exit 0

### Audit script design
- Grep for `Skill(skill: "x-pr-create"` in SKILL.md files under `core/`
- For each file with a real invocation, check if same file has
  `Skill(skill: "x-pr-watch-ci"` OR `--no-ci-watch`
- Exit 0 = no violations, exit 1 = violations found

### CLAUDE.md update
Add EPIC-0045 to the "In progress" block.
