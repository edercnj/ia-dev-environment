# Rule 20 — Telemetry Privacy (RULE-TELEMETRY-PRIVACY-01)

> **Scope:** Every skill, hook, and Java class that emits, persists, or exports
> telemetry events under `plans/epic-*/telemetry/`.
> **Ownership:** Security / Platform Team.
> **Related:** Rule 06 (Security Baseline). EPIC-0040 (Telemetria de Execução de
> Skills). Story-0040-0005 (PII scrubbing).

## 1. Rule

All telemetry events MUST be scrubbed through
`dev.iadev.telemetry.TelemetryScrubber` (or the equivalent shell regex chain
in `telemetry-emit.sh`) BEFORE being written to `events.ndjson`. The scrubbed
payload is the only form that MAY be committed to version control.

The intent of Rule 20 is: *any NDJSON committed to a public repository must be
safe to republish without further review*. If a value could conceivably
identify a person, authenticate a service, or authorize access, it MUST be
either removed or replaced by a deterministic redaction marker.

## 2. Blocked Patterns (normative)

The scrubber MUST mask the following patterns wherever they appear in string
fields (`failureReason`, `phase`, `tool`, `skill`, and `metadata` values). Each
pattern maps to a deterministic replacement marker so downstream analytics can
count occurrences without recovering the secret.

| # | Category | Regex | Replacement |
| - | :--- | :--- | :--- |
| 1 | AWS Access Key ID | `AKIA[0-9A-Z]{16}` | `AWS_KEY_REDACTED` |
| 2 | AWS Secret Key | `(?i)aws_secret[^=\s]*\s*=\s*\S+` | `AWS_SECRET_REDACTED` |
| 3 | JWT (three dot-separated base64url segments) | `eyJ[A-Za-z0-9_-]+\.eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+` | `JWT_REDACTED` |
| 4 | Bearer token | `(?i)bearer\s+[A-Za-z0-9._-]+` | `BEARER_REDACTED` |
| 5 | GitHub token (`ghp_`, `gho_`, `ghu_`, `ghs_`, `ghr_`) | `gh[pousr]_[A-Za-z0-9]{36,}` | `GITHUB_TOKEN_REDACTED` |
| 6 | Email address | `[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}` | `EMAIL_REDACTED` |
| 7 | CPF (Brazilian ID, formatted) | `\d{3}\.\d{3}\.\d{3}-\d{2}` | `CPF_REDACTED` |
| 8 | URL with embedded credentials | `://[^:/\s]+:[^@/\s]+@` | `://USER:PASS_REDACTED@` |

Rules 1 through 8 are the **minimum set**. Implementations MAY add further
patterns (e.g., RSA private key headers, Slack tokens) provided the mapping is
documented in the same rule file AND the corresponding test vector is added to
the fuzz corpus (`java/src/test/resources/fixtures/telemetry/pii-corpus.txt`).

### Application order

Scrubber application order matters because later patterns may match substrings
of earlier replacements. The canonical order is: **Bearer → JWT → AWS key →
AWS secret → GitHub token → URL-with-credentials → email → CPF**. This order
guarantees that a header like `Bearer eyJ...` is first collapsed to
`BEARER_REDACTED`, avoiding a second pass that would produce nested markers.

## 3. Metadata Whitelist

The `metadata` map on `TelemetryEvent` accepts ONLY the keys below. Any other
key MUST be removed (not masked) with an `INFO` log line
`"telemetry.metadata.removed key={key}"`.

| Key | Type | Intent |
| :--- | :--- | :--- |
| `retryCount` | `Integer` | Number of retries inside a tool invocation |
| `commitSha` | `String` (7-40 hex chars) | SHA of a commit produced by the event |
| `filesChanged` | `Integer` | Aggregate count (no filenames) |
| `linesAdded` | `Integer` | Aggregate diff stat |
| `linesDeleted` | `Integer` | Aggregate diff stat |
| `exitCode` | `Integer` | POSIX exit code from a Bash tool call |
| `toolAttempt` | `Integer` | Current attempt (1..N) of a retry loop |
| `phaseNumber` | `Integer` | Phase index (1..N) inside a skill |

The whitelist is intentionally short. Adding a new key requires an ADR and an
update to both this rule and `MetadataWhitelist.ALLOWED_KEYS`.

## 4. Rotation & Retention

- Scrubbed NDJSON is retained indefinitely by default — it contains no secrets
  and no PII.
- Removing historical events from `events.ndjson` requires a manual curator
  script (not auto-emitted) so that provenance stays explicit.
- `PiiAudit` (CLI under `dev.iadev.telemetry.PiiAudit`) MUST be run as a CI
  gate on any branch that touches `plans/epic-*/telemetry/`; a non-zero exit
  MUST block the merge.

## 5. Fail-Open Semantics

Scrubber robustness is preferred over scrubber strictness at emit-time. When
the regex engine throws (e.g., `PatternSyntaxException` at JVM start-up), the
scrubber MUST log WARN and return the original event unchanged. The audit
pipeline (`PiiAudit`) catches anything the emit-time scrubber missed — a
misconfigured regex never silently drops events.

## 6. Enforcement

- `TelemetryScrubberTest` + `TelemetryScrubberFuzzTest`: 100+ fixture strings,
  zero false negatives.
- `MetadataWhitelistTest`: each allowed key round-trips, each non-allowed key
  is removed.
- `PiiAuditSmokeIT`: end-to-end scan over a polluted NDJSON fixture exits with
  code 1 and reports ≥ 3 matches.
- `RulesAssemblerTelemetryTest`: verifies this rule file is copied verbatim
  into `.claude/rules/20-telemetry-privacy.md`.

## 7. Forbidden

- Emitting telemetry without passing the event through the scrubber.
- Adding a new `metadata` key without updating BOTH this rule AND
  `MetadataWhitelist.ALLOWED_KEYS`.
- Mutating a fuzz-corpus entry without re-running the scrubber test.
- Implementing a parallel scrubber outside `dev.iadev.telemetry` (single
  source of truth).
- Changing a replacement marker (e.g., renaming `AWS_KEY_REDACTED`) without a
  coordinated downgrade of consumer dashboards.
