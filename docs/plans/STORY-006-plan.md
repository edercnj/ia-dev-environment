# Implementation Plan — STORY-006: Testing Skills

## Affected Components

1. `resources/github-skills-templates/testing/` — 6 new template files
2. `src/claude_setup/assembler/github_skills_assembler.py` — Add testing group to SKILL_GROUPS
3. `tests/assembler/test_github_skills_assembler.py` — Add testing skill tests
4. `tests/golden/*/github/skills/{testing-skills}/SKILL.md` — 48 golden files (6 skills × 8 profiles)

## Templates to Create

| Template | Source Reference | Key Keywords |
|----------|-----------------|--------------|
| x-test-plan.md | .claude/skills/x-test-plan/SKILL.md | test plan, coverage, scenarios, categories |
| x-test-run.md | .claude/skills/x-test-run/SKILL.md | test, coverage, threshold, 95%, 90% |
| run-e2e.md | .claude/skills/run-e2e/SKILL.md | end-to-end, container, database, integration |
| run-smoke-api.md | .claude/skills/run-smoke-api/SKILL.md | smoke, Newman, Postman, health |
| run-contract-tests.md | .claude/skills/run-contract-tests/SKILL.md | contract, Pact, Spring Cloud Contract, consumer |
| run-perf-test.md | .claude/skills/run-perf-test/SKILL.md | performance, latency, throughput, load |

## Assembler Change

Add to SKILL_GROUPS dict:
```python
"testing": (
    "x-test-plan",
    "x-test-run",
    "run-e2e",
    "run-smoke-api",
    "run-contract-tests",
    "run-perf-test",
),
```

## Test Changes

- Add TESTING_SKILLS tuple from SKILL_GROUPS["testing"]
- ALL_SKILLS automatically includes testing skills (20 total)
- Add TestGenerateTestingGroup (count == 6)
- Add TestTestingSkillContent (parametrized, references + language)
- Add TestTestingSkillDescriptionKeywords (keyword validation per skill)

## Risk Assessment

- Low risk: Pattern is identical to STORY-004 (dev) and STORY-005 (review)
- No new assembler logic needed — just add group entry
- Templates must NOT duplicate .claude/skills/ content — use references only
