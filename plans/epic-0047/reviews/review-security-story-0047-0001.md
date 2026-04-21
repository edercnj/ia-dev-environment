# Specialist Review — Security Engineer

> **Story ID:** story-0047-0001
> **Date:** 2026-04-21
> **Reviewer:** Security Specialist (post-hoc review)
> **Engineer Type:** Security
> **Template Version:** 1.0

## Review Scope

Rule 06 (Security Baseline) + Rule 12 (Java Security Anti-Patterns) applied to the new code paths in `SkillsAssembler.assembleShared` and related filesystem operations. Focus areas:

- Path traversal / directory escape (Rule 06: "All path inputs: canonicalize, then verify prefix").
- Symlink following (Rule 06 Forbidden: "Following symlinks in file operations without explicit opt-in").
- Hardcoded secrets / credentials (Rule 06 Forbidden, Rule 12 J4).
- Temp file / resource permissions.
- Error-message information disclosure (Rule 12 J7).

Reviewed files:
- `SkillsAssembler.java` (assembleShared + deleteStrictly).
- `_shared/` Markdown snippets (error-handling, tdd-tags-glossary, exit-codes-common).
- Every modified source `SKILL.md` + ADR-0011.

## Score Summary

28/30 | Status: Partial

## Passed Items

| # | Item | Notes |
| :--- | :--- | :--- |
| 1 | No user-controlled path input | `sharedSrc` is derived from `resourcesDir.resolve("targets/claude/skills/_shared")`. `resourcesDir` is set in the constructor either from classpath resolution or from a test-controlled `@TempDir`. No external user/HTTP input reaches path resolution. Rule 12 J6 not applicable. |
| 2 | No placeholder substitution on `_shared/` content | ADR-0011 explicitly rejected Option (a) on security grounds ("no new security surface"). The delivered Option (b) treats `_shared/` as a static Markdown tree — no template engine, no interpolation — so `{{}}`-injection in snippets cannot leak variable values. |
| 3 | No hardcoded secrets | All 3 snippet files + README are pure technical documentation (exit-code families, TDD tag glossary, pre-commit error matrix). Rule 12 J4 not triggered. ADR-0011 contains no credentials. |
| 4 | Safe copy API | `CopyHelpers.copyDirectory` (unchanged from prior work) uses `Files.walkFileTree` with `SimpleFileVisitor`. No `Runtime.exec`, no shell out. |
| 5 | No deserialization of untrusted content | `_shared/` is regular text; no `ObjectInputStream` (Rule 12 J3) or YAML-unsafe-constructor paths touched. |
| 6 | No unrestricted TLS / crypto | No network code added. Rule 12 J2, J5 not triggered. |
| 7 | No SQL or DB access | `database=none` profile; Rule 12 J1 not triggered. |
| 8 | No CORS / HTTP exposure | Pure CLI generator; no web layer. Rule 12 J8 not triggered. |
| 9 | No Math.random() usage | Search confirmed: no calls added. Rule 12 J2 clean. |
| 10 | Error messages sanitized | `deleteStrictly` and `pruneStaleSkills` throw `UncheckedIOException` with file paths — this IS expected for a dev-time CLI tool (not an HTTP-facing service). Rule 12 J7 applies to HTTP error handlers, not CLI/test-time exceptions. Information disclosure risk = none (developer already owns the filesystem). |
| 11 | Prune deletion is scoped | `pruneStaleSkills` only walks `outputDir.resolve("skills")` and deletes dirs NOT in `expected`. The `_shared/` dir name is added to `expected` via `assembleShared` return → prune cannot delete it by accident. |
| 12 | No dynamic classloading | `resolveClasspathResources()` uses `ResourceResolver` — pre-existing, unchanged. |
| 13 | ADR-0011 explicitly calls out the non-change | §Consequences / Neutral: "Option (a) must normalize the placeholder path and reject traversal outside `_shared/`. Option (b) has no such surface". Security reasoning documented, not implicit. |

## Failed Items

(none Critical / High / Medium)

## Partial Items

| # | Item | Status | Notes |
| :--- | :--- | :--- | :--- |
| 1 | Symlink handling in `CopyHelpers.copyDirectory` | Partial | Rule 06 Forbidden list: "Following symlinks in file operations without explicit opt-in". `assembleShared` delegates to `CopyHelpers.copyDirectory` which uses `Files.walkFileTree` with default options — on POSIX this follows symlinks. If a malicious actor added a symlink to `/etc/passwd` inside `_shared/` and the developer ran `mvn process-resources`, the content would be copied to the output tree. Threat model: low (developer controls their own working tree + build-time, not a network-exposed input), but the violation of the Rule is still formally present. Note: this is PRE-EXISTING behavior of `CopyHelpers.copyDirectory`, not introduced by this story. Severity: Low. Recommend filing a follow-up to pass `FOLLOW_LINKS` OFF / `NOFOLLOW_LINKS` in the shared copy helper. Not a blocker for this PR. |
| 2 | No CI lint asserting `_shared/*.md` absence of secrets | Partial | The three snippet files are today pure documentation. If future authors add a snippet containing (e.g.) a sample AWS access key in a code block, nothing currently scans `_shared/` for secrets. Mitigation: project-wide `x-security-dashboard` and `x-dependency-audit` run in CI; neither targets Markdown content for secret scanning. Severity: Low. Recommend coordinating with the security baseline team to add gitleaks/trufflehog to CI in a follow-up. Not a blocker. |

## Severity Summary

| Severity | Count |
| :--- | :--- |
| Critical | 0 |
| High | 0 |
| Medium | 0 |
| Low | 2 |
| **Total** | **2** |

## Recommendations

1. (Low, pre-existing) Harden `CopyHelpers.copyDirectory` to use `LinkOption.NOFOLLOW_LINKS` when walking trees, per Rule 06 ("Following symlinks... without explicit opt-in"). This is a project-wide concern, not story-0047-0001's responsibility.
2. (Low, future epic) Add gitleaks scan of every committed file including `_shared/` — complements the current Java-centric scanning.

## Verdict

**Approved.** No new attack surface introduced by this story. Option (b) was explicitly chosen (ADR-0011 §Rationale item 6) to avoid adding a path-resolution surface; the delivered code keeps that promise. Both Partial items are pre-existing or out-of-scope concerns whose follow-up is appropriate but not blocking.
