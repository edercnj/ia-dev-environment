# Implementation Plan — STORY-013

## Affected Components

| Component | Action | Path |
|-----------|--------|------|
| README template | Extend | `resources/readme-template.md` |
| ReadmeAssembler | Extend | `src/claude_setup/assembler/readme_assembler.py` |
| Validation script | Create | `scripts/validate-github-structure.py` |
| Golden files | Regenerate | `tests/golden/*/README.md` |

## Implementation Groups

### G1: Extend README template
- Add `.github/` directory tree section
- Add `.claude/` ↔ `.github/` mapping table
- Add conventions per artifact type
- Add note about both being generated outputs
- Add new placeholders: `{{GITHUB_TREE}}`, `{{MAPPING_TABLE}}`, `{{GITHUB_SUMMARY}}`

### G2: Extend ReadmeAssembler
- Add `_count_github_*()` helper functions
- Add `_build_github_tree()` for directory tree
- Add `_build_mapping_table()` for equivalence
- Add `_build_github_summary()` for component counts
- Wire new placeholders in `_generate_readme()`

### G3: Create validation script
- `scripts/validate-github-structure.py`
- Validate instructions, skills, agents, prompts, hooks, MCP
- Produce Go/No-Go report with severity levels

### G4: Regenerate golden files & verify tests
- Run `python scripts/generate_golden.py`
- Run byte-for-byte tests

## Risk Assessment
- Low risk: template extension is additive
- Golden files must be regenerated for all 8 profiles
