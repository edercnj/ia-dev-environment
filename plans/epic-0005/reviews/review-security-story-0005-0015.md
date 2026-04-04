# Security Review — story-0005-0015

```
ENGINEER: Security
STORY: story-0005-0015
SCORE: 20/20
STATUS: Approved
---
PASSED:
- [1] Input validation (2/2) — `checkExistingArtifacts()` validates that `outputDir` exists before iterating; `ARTIFACT_DIRS` is a hardcoded `as const` tuple with no user-controlled directory names; `existsSync` + `join` prevent injection. The CLI validates `--config`, `--output-dir`, and `--resources-dir` before use.
- [2] Output encoding (2/2) — `formatConflictMessage()` produces plain-text output to stderr only. Directory names in the conflict list come from the hardcoded `ARTIFACT_DIRS` constant, not from user input. No HTML, JSON, or template rendering is involved in error output.
- [3] Authentication checks (2/2) — Not applicable. This is a local CLI tool with no authentication surface. No network listeners, no API endpoints, no session management introduced.
- [4] Authorization checks (2/2) — Not applicable. The CLI operates under the invoking user's OS-level permissions. File system access uses `existsSync` which respects OS permission model. No privilege escalation paths introduced.
- [5] Sensitive data masking (2/2) — No sensitive data is handled in this change. The overwrite detector only checks for directory existence and reports directory names (`.claude/`, `.github/`, `docs/`). No credentials, tokens, PII, or secrets are read, logged, or returned. The error message contains only static directory names from the hardcoded constant.
- [6] Error handling — no stack traces (2/2) — `CliError` with code `OVERWRITE_CONFLICT` is thrown and caught by `handleKnownError()` in `cli.ts:110-118`, which outputs only `error.message` (the formatted conflict list). Stack traces are only shown when `--verbose` is explicitly enabled AND the error is an unknown type (not CliError), which is an intentional debug-only behavior for local CLI usage. The overwrite conflict path never exposes stack traces.
- [7] Cryptography usage (2/2) — Not applicable. No cryptographic operations introduced. No hashing, encryption, signing, or random number generation in the changed code.
- [8] Dependency vulnerabilities (2/2) — No new dependencies added. `package.json` and `package-lock.json` are unchanged. The new module uses only `node:fs` (`existsSync`) and `node:path` (`join`) from the Node.js standard library.
- [9] CORS/CSP headers (2/2) — Not applicable. This is a CLI tool with no HTTP server, no web interface, and no network-facing components. No headers to configure.
- [10] Audit logging (2/2) — Not applicable for a local CLI tool. The overwrite detection result is communicated to the user via stderr with a clear, actionable message listing conflicting directories and the `--force` remediation. This is appropriate for the CLI context.
```

## Detailed Analysis

### Files Reviewed

| File | Type | Verdict |
|------|------|---------|
| `src/overwrite-detector.ts` | New module | Clean |
| `src/cli.ts` | Modified (overwrite check integration) | Clean |
| `src/assembler/epic-report-assembler.ts` | Modified (removed `docs/epic/` output) | Clean |
| `tests/node/overwrite-detector.test.ts` | New test file | Clean |
| `tests/node/cli.test.ts` | Modified (overwrite protection tests) | Clean |
| `tests/node/integration/cli-integration.test.ts` | Modified (integration tests) | Clean |
| `tests/golden/*/docs/epic/` | Deleted golden files | Clean |
| `CHANGELOG.md` | Updated | Clean |

### Security Observations

1. **Hardcoded allow-list pattern**: `ARTIFACT_DIRS` is defined as `[".claude", ".github", "docs"] as const`. This is the correct approach — the detector only checks for known directories rather than scanning user-controlled input, eliminating directory traversal or injection vectors.

2. **Fail-secure design**: When conflicts are detected and `--force` is not provided, the CLI **denies** (exits with error code 1) rather than proceeding. This aligns with the "Fail Secure" principle from the security standards.

3. **Bypass hierarchy is correct**: `--dry-run` skips the overwrite check (correct — dry-run writes nothing), and `--force` explicitly opts in to overwrite. The check order (`!options.dryRun && !options.force`) ensures dry-run takes priority.

4. **No TOCTOU race**: The `existsSync` check and subsequent `runPipeline` are not atomic, but this is acceptable for a local CLI tool where the user controls the filesystem. A TOCTOU race here would require the user to create directories between the check and the write, which is a self-inflicted condition.

5. **Surface area reduction**: Removing the `docs/epic/` output path reduces the number of directories the tool writes to, which is a positive security change (less filesystem surface area).

6. **No path traversal**: `outputDir` is joined with hardcoded directory names via `path.join()`, which normalizes paths. The `ARTIFACT_DIRS` constant contains no `..` segments or absolute paths.
