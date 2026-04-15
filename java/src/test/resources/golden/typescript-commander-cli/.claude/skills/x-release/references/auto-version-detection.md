# Auto-Version Detection — Conventional Commits

> **Scope.** This reference documents how `/x-release` computes the next version
> when no positional argument and no `--version` flag are provided. The
> algorithm is implemented in the `dev.iadev.release` package
> (story-0039-0001) and consumed by the `DETERMINE` phase of the skill.

## 1. Algorithm Overview

```
1. Probe last tag:
     git describe --tags --abbrev=0 --match 'v*'
   If no matching tag → previousVersion := 0.0.0 (implicit baseline)

2. List commits since last tag:
     git log <last-tag>..HEAD --no-merges --format=%s%n%b
   If range is empty → abort VERSION_NO_COMMITS

3. Classify each commit via ConventionalCommitsParser.classify(...)
   → CommitCounts{feat, fix, perf, breaking, ignored}

4. Select BumpType via BumpType.from(counts)
   → MAJOR if breaking > 0
   → MINOR if feat > 0
   → PATCH if fix + perf > 0
   → null otherwise → abort VERSION_NO_BUMP_SIGNAL

5. Next version := VersionBumper.bump(previousVersion, bumpType)
```

## 2. Classification Table

| Subject pattern | CommitCounts field | Contributes to bump |
|-----------------|--------------------|---------------------|
| `feat: ...` / `feat(scope): ...` | `feat` | MINOR |
| `feat!: ...` / `feat(scope)!: ...` | `feat`, `breaking` | MAJOR |
| `fix: ...` / `fix(scope): ...` | `fix` | PATCH |
| `fix!: ...` / `fix(scope)!: ...` | `fix`, `breaking` | MAJOR |
| `perf: ...` / `perf(scope): ...` | `perf` | PATCH |
| `docs:`, `chore:`, `test:`, `refactor:`, `style:`, `build:`, `ci:` | `ignored` | (none) |
| Any commit body containing `BREAKING CHANGE: ...` | `breaking` | MAJOR |
| Unrecognised subject | `ignored` | (none) |

> A commit that is both a feature and breaking contributes to BOTH `feat` AND
> `breaking` (orthogonal dimensions for the banner). The selected bump is
> still MAJOR — breaking wins over feat.

## 3. Bump Rules

| Base → | MAJOR | MINOR | PATCH |
|--------|-------|-------|-------|
| `3.1.0` | `4.0.0` | `3.2.0` | `3.1.1` |
| `0.0.0` | `1.0.0` | `0.1.0` | `0.0.1` |
| `1.2.3-rc.1` | `2.0.0` | `1.3.0` | `1.2.4` |

Pre-release suffixes on the base version are dropped after a bump (the next
version is always a stable release). Callers that need a pre-release must
append the suffix explicitly after `VersionBumper.bump` returns.

## 4. Banner Format

```
Próxima versão detectada: <target> (<BUMP>) — <feat> feat, <fix> fix, <breaking> breaking desde <previous>
```

Examples:

- `Próxima versão detectada: 3.2.0 (MINOR) — 7 feat, 2 fix, 0 breaking desde v3.1.0`
- `Próxima versão detectada: 0.1.0 (MINOR) — 5 feat, 0 fix, 0 breaking desde 0.0.0 (no prior tag)`

Override banner:

```
Versão explícita: 4.0.0
```

## 5. Edge Cases

| Scenario | Behaviour |
|----------|-----------|
| No prior tag, commits present | Baseline `0.0.0`; bump applies normally |
| No prior tag, no commits | `VERSION_NO_COMMITS` |
| Prior tag, zero commits in range | `VERSION_NO_COMMITS` |
| Prior tag, only docs/chore commits | `VERSION_NO_BUMP_SIGNAL` with hint: `Use --version X.Y.Z for releases without feat/fix/perf commits.` |
| `--version 3.2.0` supplied | Detection skipped; `bumpType = explicit`, banner reports explicit |
| `--version 3.2` supplied | `VERSION_INVALID_FORMAT` |
| `--last-tag v9.9.9` supplied but tag absent | `VERSION_TAG_NOT_FOUND` |
| Commit subject lacks Conventional Commits prefix | Counted as `ignored` (no bump pressure) |

## 6. Error Codes (exit 1)

| Code | Meaning |
|------|---------|
| `VERSION_NO_BUMP_SIGNAL` | All commits since last tag are ignored types. Use `--version` to release anyway. |
| `VERSION_NO_COMMITS` | `git log <tag>..HEAD` is empty. Nothing to release. |
| `VERSION_INVALID_FORMAT` | `--version` argument does not match `^\d+\.\d+\.\d+(-[a-z0-9.]+)?$`. |
| `VERSION_TAG_NOT_FOUND` | `--last-tag` value does not resolve in the current repository. |

These codes correspond one-to-one with `dev.iadev.release.InvalidBumpException.Code`
for the three domain-layer codes; `VERSION_TAG_NOT_FOUND` is raised by the
CLI adapter before invoking the domain.

## 7. Security Posture (Rule 06 / OWASP A03)

- All `git` invocations use `ProcessBuilder` with a fixed argv — no shell
  expansion and no concatenation of operator-supplied strings.
- `--version` is parsed via `SemVer.parse(...)` BEFORE any filesystem or
  `git` I/O (fail-fast).
- `--last-tag` is validated against `^v?\d+\.\d+\.\d+` before being passed
  to `git describe`/`git log`.
- Commit body parsing uses a pinned `(?m)^BREAKING CHANGE:\s` regex with no
  unbounded backtracking (ReDoS-safe).
- Error messages surfaced to operators do NOT include absolute paths or
  stack traces (Rule 06 — no internal path disclosure).

## 8. Fixture Table (used by `AutoVersionDetectionSmokeTest`)

| # | Fixture | Expected |
|---|---------|----------|
| 1 | tag `v1.0.0` + 3 `feat:` | `1.1.0` (MINOR) |
| 2 | no tag + 2 `fix:` | `0.0.1` (PATCH) |
| 3 | tag `v1.0.0` + only `docs:` | `VERSION_NO_BUMP_SIGNAL` |
| 4 | tag `v1.0.0` + 1 `feat!:` | `2.0.0` (MAJOR) |
| 5 | tag `v1.0.0` + 0 commits | `VERSION_NO_COMMITS` |
| 6 | `--version 4.0.0` (any state) | `4.0.0` (EXPLICIT) |

## 9. Implementation Map

| Concern | Type | Location |
|---------|------|----------|
| Subject + body classification | domain | `dev.iadev.release.ConventionalCommitsParser` |
| Bump arithmetic | domain | `dev.iadev.release.VersionBumper` |
| SemVer parse + validation | domain | `dev.iadev.release.SemVer` |
| Outbound git access | adapter | `dev.iadev.release.GitTagReader` implements `TagReader` |
| Error vocabulary | domain | `dev.iadev.release.InvalidBumpException.Code` |
