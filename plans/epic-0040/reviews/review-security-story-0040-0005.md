# Specialist Review — Security

**Story:** story-0040-0005
**PR:** #413
**Branch:** feat/story-0040-0005-pii-scrubber
**Reviewer:** Security specialist (inline)
**Date:** 2026-04-16

ENGINEER: Security
STORY: story-0040-0005
SCORE: 28/30
STATUS: Partial

---

## PASSED

- [SEC-01] All 8 canonical PII patterns implemented with deterministic markers (2/2) — `TelemetryScrubber.DEFAULT_RULES`.
- [SEC-02] Rule 20 published as normative source of truth; matches implementation verbatim (2/2) — `20-telemetry-privacy.md`.
- [SEC-03] Metadata whitelist is a CLOSED list; non-allowed keys are REMOVED (not masked), log line `INFO telemetry.metadata.removed key=...` matches rule 20 §3 (2/2) — `TelemetryScrubber.scrubMetadata`.
- [SEC-04] Fail-open semantics explicit (WARN + original event) rather than fail-closed or silent drop; justified by the companion `PiiAudit` which runs as a CI gate (2/2) — `TelemetryScrubber.scrub` lines 132-140.
- [SEC-05] URL-with-credentials pattern tightened to also consume the host segment so the email regex cannot silently strip the `PASS_REDACTED@host.tld` leftover (2/2) — `TelemetryScrubber.DEFAULT_RULES` entry `url_credentials`.
- [SEC-06] Bearer-before-JWT ordering documented and covered by `PatternOrdering.scrub_bearerJwt_noNestedMarker` test, preventing nested `BEARER_REDACTED JWT_REDACTED` markers that would leak the fact that a JWT was present (2/2).
- [SEC-07] Fuzz corpus size guaranteed at class-load time via `static {}` sanity block; corrupted fixture fails loud before the parametrized runner starts (2/2) — `TelemetryScrubberFuzzTest` lines 204-217.
- [SEC-08] 100-string corpus covers all 8 blocked categories; aggregate scan proves zero false negatives; each category has 12-15 entries (well-distributed) (2/2).
- [SEC-09] `PiiAudit` uses the same rule set as the scrubber — single source of truth, no parallel regex implementation (Rule 20 §7 compliance) (2/2) — `PiiAudit` constructor defaults to `TelemetryScrubber.DEFAULT_RULES`.
- [SEC-10] Scrubber output is never written to disk by the scrubber itself; the writer remains the only I/O boundary (2/2).
- [SEC-11] No secret value ever appears in a log line (only markers and category names) (2/2).
- [SEC-12] CLI (`PiiAudit`) delegates to picocli and `System.exit` only in `main()`; library use-case goes through `audit(Path, PrintWriter)` which returns findings and never terminates the JVM (2/2).
- [SEC-13] Record fields are defensively copied (`Map.copyOf`) in `TelemetryEvent` constructor; `ScrubRule` is an immutable record; `MetadataWhitelist` uses `Set.copyOf` (2/2).

## PARTIAL

- [SEC-14] Scrubber mutates the `skill` field through `scrubString`, but `skill` is validated by `SKILL_KEBAB = ^[a-z0-9]+(?:-[a-z0-9]+)*$` in `TelemetryEvent`. A scrubbed output like `EMAIL_REDACTED` would fail that pattern and throw in the new-event constructor, triggering the fail-open path (return original). In the current rule set no PII pattern can match a kebab-case skill, so this is latent — but documenting the invariant in a comment would prevent future regressions (1/2) [MEDIUM] — `TelemetryScrubber.scrubUnchecked` lines 144-168. **Fix:** Add a one-line comment next to `scrubbedSkill` explaining the invariant or, better, short-circuit the scrub when the input matches `SKILL_KEBAB`.

- [SEC-15] `PiiAudit` reads entire files via `Files.readAllLines` — an attacker who can place a multi-gigabyte file under the scan root could cause an OOM in CI. Telemetry files are produced by this project and bounded by the writer, so the risk is low, but the audit tool is a generic scanner and might be pointed at untrusted trees (1/2) [LOW] — `PiiAudit.scanFile` line 161. **Fix:** Switch to streaming line-by-line via `Files.newBufferedReader` + `readLine` loop, keeping only the current line in memory.

## FAILED

(none)

## Severity Summary

CRITICAL: 0 | HIGH: 0 | MEDIUM: 1 | LOW: 1

## Notes

- **OWASP mapping**:
  - A02:2021 Cryptographic Failures → not applicable (no cryptography introduced).
  - A03:2021 Injection → regex patterns use compiled `Pattern` instances; no dynamic regex construction from user input.
  - A08:2021 Software and Data Integrity Failures → fail-open semantics documented and enforced with tests; no silent drop.
  - A09:2021 Security Logging and Monitoring Failures → `PiiAudit` provides the CI gate; findings are reportable.

- **CWE mapping**:
  - CWE-532 (Insertion of Sensitive Information into Log File) → mitigated by scrubber + rule 20.
  - CWE-200 (Exposure of Sensitive Information) → mitigated by metadata whitelist (removal, not just masking).

- **Rule 06 (Security Baseline) compliance**: No hardcoded secrets; no `Math.random()`; no string concatenation of PII into SQL / paths / URLs (which the scrubber itself prevents).
- **Rule 20 (new)**: implementation matches the normative matrix verbatim.
