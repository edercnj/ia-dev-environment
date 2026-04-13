# x-release Skill

Orchestrates Git Flow release with approval gate and PR-flow.

## Quick Start

### Standard release (minor bump)
```
/x-release minor
# ... skill runs phases 0-8 ...
# ... halts at APPROVAL-GATE, prints PR URL ...

# [User merges PR in GitHub UI]

/x-release 2.3.0 --continue-after-merge
# ... skill runs phases 9-11, tags main, opens back-merge PR ...
```

### Hotfix release
```
/x-release --hotfix
# ... same flow but from main, patch only ...
```

### Dry-run (preview without changes)
```
/x-release minor --dry-run
```

### Interactive mode (no halt, inline pause)
```
/x-release minor --interactive
# ... skill uses AskUserQuestion to pause instead of exit ...
```

## Phases

| # | Phase | Description | Halts? |
|---|---|---|---|
| 0 | RESUME_DETECTION | Load/create state, verify deps | -- |
| 1 | DETERMINE | Parse bump type, compute version | -- |
| 2 | VALIDATE_DEEP | 8 checks (tests, coverage, golden, consistency) | -- |
| 3 | BRANCH | Create release/* or hotfix/* | -- |
| 4 | UPDATE | Update version in pom.xml etc. | -- |
| 5 | CHANGELOG | Delegate to x-release-changelog | -- |
| 6 | COMMIT | Create release commit | -- |
| 7 | OPEN_RELEASE_PR | gh pr create --base main | -- |
| **8** | **APPROVAL_GATE** | **Persist state, halt** | **HUMAN** |
| 9 | RESUME_AND_TAG | Verify merged, tag main | -- |
| 10 | BACK_MERGE_DEVELOP | gh pr create --base develop | -- |
| 11 | PUBLISH | Push tag to remote | -- |
| 12 | CLEANUP | Delete branch and state file | -- |

## State File

The skill persists its progress in `plans/release-state-<X.Y.Z>.json`.
See `references/state-file-schema.md` for the full schema.

## References

- `references/approval-gate-workflow.md` -- How the human approval pause works
- `references/state-file-schema.md` -- State file JSON schema and phase transitions
- `references/backmerge-strategies.md` -- Clean vs conflict back-merge handling

## Dependencies

- `gh` CLI >= 2.0 (authenticated via `gh auth login`)
- `jq` for JSON parsing
- `git` >= 2.30

## See Also

- `x-release-changelog` -- invoked in Phase 5
- `x-git-push` -- Conventional Commits reference
- `x-review-pr` -- optional integration in Phase 7
