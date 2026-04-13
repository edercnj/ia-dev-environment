# Release Notes — EPIC-0036: Skill Taxonomy and Naming Refactor

> **Status:** Pending publication with the next minor release.
> **Authoritative decision record:** [ADR-0003 — Skill Taxonomy and Naming Refactor](../../adr/ADR-0003-skill-taxonomy-and-naming.md)
> **Migration staging document:** [`plans/epic-0036/skill-renames.md`](../../plans/epic-0036/skill-renames.md)

## Summary

EPIC-0036 reorganises the skill source-of-truth tree under
`java/src/main/resources/targets/claude/skills/` into ten category
subfolders (`plan/`, `dev/`, `test/`, `review/`, `security/`,
`code/`, `git/`, `pr/`, `ops/`, `jira/`) and renames **19 skills**
to a uniform `x-{subject}-{action}` scheme. The user-facing
invocation surface (the flat layout under `.claude/skills/`) is
preserved, but every skill name listed below has changed.

There is **no backward compatibility layer** (per RULE-005, "Hard
Rename without Aliases"). Pipelines, scripts, prompts, agent
templates, and any external integrations that invoke skills by
name MUST be updated mechanically using the migration table in
the next section.

A CI guard (`scripts/check-old-skill-names.sh`, wired into
`.github/workflows/skill-name-guard.yml`) blocks any reintroduction
of an old name on `develop` or `main`.

## Breaking Change Notice

> **Breaking change.** All 19 renamed skills have been removed.
> The old names no longer resolve. Consumers that invoke skills
> by name MUST migrate to the new names listed below before
> upgrading to the next minor release.

## Migration table (19 renames)

### Primary cluster (STORY-0036-0004)

| #  | Old name                  | New name              | Category |
|----|---------------------------|-----------------------|----------|
| 1  | `x-story-epic`            | `x-epic-create`       | plan     |
| 2  | `x-story-epic-full`       | `x-epic-decompose`    | plan     |
| 3  | `x-story-map`             | `x-epic-map`          | plan     |
| 4  | `x-epic-plan`             | `x-epic-orchestrate`  | plan     |
| 5  | `x-dev-implement`         | `x-task-implement`    | dev      |
| 6  | `x-dev-story-implement`   | `x-story-implement`   | dev      |
| 7  | `x-dev-epic-implement`    | `x-epic-implement`    | dev      |
| 8  | `x-dev-architecture-plan` | `x-arch-plan`         | plan     |
| 9  | `x-dev-arch-update`       | `x-arch-update`       | plan     |
| 10 | `x-dev-adr-automation`    | `x-adr-generate`      | plan     |

### `run-*` unification (STORY-0036-0005)

| #  | Old name              | New name              | Category |
|----|-----------------------|-----------------------|----------|
| 11 | `run-e2e`             | `x-test-e2e`          | test     |
| 12 | `run-smoke-api`       | `x-test-smoke-api`    | test     |
| 13 | `run-smoke-socket`    | `x-test-smoke-socket` | test     |
| 14 | `run-contract-tests`  | `x-test-contract`     | test     |
| 15 | `run-perf-test`       | `x-test-perf`         | test     |

### Pointwise simplifications (STORY-0036-0005)

| #  | Old name                  | New name              | Category |
|----|---------------------------|-----------------------|----------|
| 16 | `x-pr-fix-comments`       | `x-pr-fix`            | pr       |
| 17 | `x-pr-fix-epic-comments`  | `x-pr-fix-epic`       | pr       |
| 18 | `x-runtime-protection`    | `x-runtime-eval`      | security |
| 19 | `x-security-secret-scan`  | `x-security-secrets`  | security |

## Migration recipe

For each downstream consumer:

1. Replace every literal occurrence of an old name from column
   "Old name" with the corresponding "New name".
2. Re-run `scripts/check-old-skill-names.sh` locally to confirm
   no reintroductions slipped in.
3. Re-generate any cached `.claude/` outputs (`ia-dev-env`
   regeneration) and verify your invocation paths still resolve.

A simple `sed` script over a consumer repository is sufficient:

```bash
sed -i \
    -e 's/x-story-epic-full/x-epic-decompose/g' \
    -e 's/x-story-epic/x-epic-create/g' \
    -e 's/x-story-map/x-epic-map/g' \
    -e 's/x-epic-plan/x-epic-orchestrate/g' \
    -e 's/x-dev-implement/x-task-implement/g' \
    -e 's/x-dev-story-implement/x-story-implement/g' \
    -e 's/x-dev-epic-implement/x-epic-implement/g' \
    -e 's/x-dev-architecture-plan/x-arch-plan/g' \
    -e 's/x-dev-arch-update/x-arch-update/g' \
    -e 's/x-dev-adr-automation/x-adr-generate/g' \
    -e 's/run-e2e/x-test-e2e/g' \
    -e 's/run-smoke-api/x-test-smoke-api/g' \
    -e 's/run-smoke-socket/x-test-smoke-socket/g' \
    -e 's/run-contract-tests/x-test-contract/g' \
    -e 's/run-perf-test/x-test-perf/g' \
    -e 's/x-pr-fix-epic-comments/x-pr-fix-epic/g' \
    -e 's/x-pr-fix-comments/x-pr-fix/g' \
    -e 's/x-runtime-protection/x-runtime-eval/g' \
    -e 's/x-security-secret-scan/x-security-secrets/g' \
    <files...>
```

> Order matters: the longer prefixes (`x-story-epic-full`,
> `x-pr-fix-epic-comments`) MUST be substituted before their
> shorter counterparts so the longer match is not consumed first.

## CI guard

A new GitHub Actions workflow
(`.github/workflows/skill-name-guard.yml`) executes
`scripts/check-old-skill-names.sh` on every push and PR targeting
`develop` or `main`. The guard fails the build if any of the 19
old names appears in a tracked file outside the allow-list
(`plans/`, `adr/`, `CHANGELOG.md`, `docs/release-notes/`,
`.claude/`, `java/src/test/resources/golden/`, build output
directories, and the guard machinery itself).

## References

- [ADR-0003 — Skill Taxonomy and Naming Refactor](../../adr/ADR-0003-skill-taxonomy-and-naming.md)
- [`plans/epic-0036/epic-0036.md`](../../plans/epic-0036/epic-0036.md) — epic
  scope and rationale
- [`plans/epic-0036/skill-renames.md`](../../plans/epic-0036/skill-renames.md) —
  full migration staging checklist
- [`scripts/check-old-skill-names.sh`](../../scripts/check-old-skill-names.sh) —
  guard implementation
- [`tests/guard/test-skill-name-guard.sh`](../../tests/guard/test-skill-name-guard.sh) —
  guard regression tests
