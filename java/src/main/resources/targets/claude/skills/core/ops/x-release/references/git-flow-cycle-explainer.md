# Git Flow Cycle Explainer — Summary Template

This template is consumed by Phase 13 (SUMMARY) of the `x-release` skill. It
is read verbatim and each placeholder is replaced with the corresponding
value from the release state file (see `state-file-schema.md`). Substitution
is **literal string replacement only** — placeholders MUST NOT be evaluated
as any template language (security RULE-003: treat state data as untrusted
input).

## Placeholders (exactly 6)

| Placeholder             | Source (state file)               | Fallback |
| :---------------------- | :-------------------------------- | :------- |
| `{{LAST_TAG}}`          | `previousVersion` (prefixed `v`)  | `—`      |
| `{{NEW_TAG}}`           | `version` (prefixed `v`)          | `—`      |
| `{{NEXT_SNAPSHOT}}`     | computed: next minor + `-SNAPSHOT`| `—`      |
| `{{RELEASE_PR}}`        | `prNumber` (prefixed `#`)         | `—`      |
| `{{BACKMERGE_PR}}`      | `backmergePrNumber` (prefixed `#`)| `—`      |
| `{{GITHUB_RELEASE_URL}}`| `githubReleaseUrl`                | (omit block) |

When a field is missing or `null`, the renderer substitutes the fallback
symbol above (em-dash `—` for simple values). The `{{GITHUB_RELEASE_URL}}`
block is entirely skipped when the URL is empty (no `-` placeholder line is
rendered). Every rendered line MUST remain ≤80 columns on an 80-col
terminal.

## Template body (rendered verbatim)

```
=== RELEASE {{NEW_TAG}} COMPLETED ===

main:     {{LAST_TAG}} ──────────── {{NEW_TAG}} ──
                                          ↑
                                (PR {{RELEASE_PR}} merged)
                                          │
release:                          release/{{NEW_TAG}}
                                          │   ↓ back-merge
develop:  ──●──●──●──●──●──●──●──●──●──●──●──●──
            {{LAST_TAG}}-SNAPSHOT     {{NEXT_SNAPSHOT}}

Por que main e develop divergem:
  main    = última release publicada ({{NEW_TAG}})
  develop = próxima release em desenvolvimento ({{NEXT_SNAPSHOT}})
  Os back-merges ({{BACKMERGE_PR}}) propagam fixes de release para
  develop, mas develop acumula novas features destinadas à próxima
  release. Divergência é esperada e saudável no Git Flow.

Artefatos criados:
  - Tag:             {{NEW_TAG}}
  - GitHub Release:  {{GITHUB_RELEASE_URL}}
  - Release PR:      {{RELEASE_PR}}
  - Back-merge PR:   {{BACKMERGE_PR}}
```

## Rendering contract

- Substitution is a single pass over the template; placeholders are not
  recursively expanded.
- The renderer MUST treat state-file values as literal strings (no shell
  expansion, no Markdown escaping, no template evaluation).
- When `githubReleaseUrl` is `null` or empty, the renderer SHOULD omit the
  `GitHub Release:` line entirely (not print `—`) to keep the output
  focused.
- When any of `previousVersion`, `version`, `prNumber`,
  `backmergePrNumber` is `null`, substitute `—`.
- All output goes to stdout. Phase 13 is read-only and has no side effects.

## Skip flag

Phase 13 is skipped when `--no-summary` is passed to `/x-release`
(CI-friendly, suppresses the verbose block). See SKILL.md §Phase 13.
