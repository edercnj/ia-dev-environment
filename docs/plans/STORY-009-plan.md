# STORY-009 Implementation Plan — Git & Troubleshooting Skills

## Affected Components

1. **Templates**: New `resources/github-skills-templates/git-troubleshooting/` directory with 2 templates
2. **Assembler**: Add `git-troubleshooting` group to `SKILL_GROUPS` in `github_skills_assembler.py`
3. **Tests**: Extend `test_github_skills_assembler.py` and `test_pipeline.py`
4. **Golden Files**: Regenerate via `scripts/generate_golden.py`

## New Files

| File | Purpose |
|------|---------|
| `resources/github-skills-templates/git-troubleshooting/x-git-push.md` | Git workflow template |
| `resources/github-skills-templates/git-troubleshooting/x-ops-troubleshoot.md` | Troubleshooting template |

## Modified Files

| File | Change |
|------|--------|
| `src/ia_dev_env/assembler/github_skills_assembler.py` | Add `git-troubleshooting` to `SKILL_GROUPS` |
| `tests/assembler/test_github_skills_assembler.py` | Add tests for git-troubleshooting group |
| `tests/test_pipeline.py` | Add `TestPipelineGitTroubleshootSkills` class |

## Implementation Order

1. Create template files (based on `.claude/skills/` originals)
2. Register group in `SKILL_GROUPS`
3. Add unit tests
4. Add pipeline tests
5. Regenerate golden files
6. Run full test suite

## Risk Assessment

- **Low risk**: Pattern is well-established from STORY-005/007/008
- Templates are direct copies of `.claude/skills/` with `{{PLACEHOLDER}}` syntax preserved
- No conditional filtering needed (unlike infrastructure skills)
