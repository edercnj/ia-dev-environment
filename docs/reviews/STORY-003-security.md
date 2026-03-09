# Security Review -- STORY-003: Models (Python to TypeScript Migration)

```
ENGINEER: Security
STORY: STORY-003
SCORE: 18/20
STATUS: Approved
---
PASSED:
- [3] Authentication checks (2/2) — N/A: pure data model layer, no auth surface. No auth bypass possible.
- [4] Authorization checks (2/2) — N/A: no access control decisions in scope. No privilege escalation vectors.
- [5] Sensitive data masking (2/2) — RESOLVED: `McpServerConfig.env` now has JSDoc warning on the `env` property and a `toJSON()` method that masks all env values with `"***"`. See src/models.ts:329-367.
- [7] Cryptography usage (2/2) — N/A: no cryptographic operations. No secrets, keys, or hashing involved.
- [8] Dependency vulnerabilities (2/2) — Zero new dependencies added. All 17 model classes use only TypeScript built-ins (string, number, boolean, Array, Record). No npm packages introduced.
- [9] CORS/CSP headers (2/2) — N/A: no HTTP endpoints, no server, no response headers.
- [10] Audit logging (2/2) — N/A: pure data model layer. No state mutations, no side effects, no operations requiring audit trail.

PARTIAL:
- [1] Input validation (1/2) — src/models.ts — Improvement: The `requireField` helper validates field presence but not field *type*. All `fromDict` methods use `as string`, `as boolean`, `as number` type assertions without runtime type checks. If a YAML config provides `name: 123` instead of `name: "some-string"`, the model silently accepts a number where a string is expected. For a config-parsing library, adding runtime type guards (e.g., `typeof value !== 'string'` checks) on required fields would prevent type confusion attacks in downstream consumers. [MEDIUM]
- [2] Output encoding (1/2) — src/models.ts:18-29 — Improvement: Error messages in `requireField` interpolate the `key` and `model` parameters directly into the string template. While these values come from hardcoded string literals in source code (not user input), if `fromDict` is ever called with dynamic model/key names derived from external input, this could expose internal structure. Current usage is safe but lacks defensive encoding. [LOW]
- [6] Error handling (no stack traces leaked) (1/2) — src/models.ts:18-29 — Improvement: Errors thrown by `requireField` expose internal model class names (e.g., "Missing required field 'name' in ProjectIdentity"). While acceptable for a CLI library, if these errors propagate to an API layer in a downstream consumer, they reveal internal implementation details. Consider documenting that consumers should catch and wrap these errors before exposing them externally. [LOW]
```

## Detailed Findings

### RESOLVED: McpServerConfig.env sensitive data protection

**Location:** `src/models.ts` -- `McpServerConfig` class (lines 326-367)

**Resolution:** The CRITICAL finding was addressed with two mitigations:
1. JSDoc warning on the `env` property: `/** May contain sensitive values (API keys, tokens). Use {@link toJSON} for safe serialization. */`
2. `toJSON()` method that replaces all env values with `"***"`, preventing accidental serialization of secrets

**Tests:** Three tests in `tests/node/models.test.ts` verify masking behavior: env values masked, empty env handled, non-sensitive fields preserved.

### MEDIUM: No runtime type validation in fromDict methods

**Location:** All `fromDict` methods across 14 model classes in `src/models.ts`

**Risk:** Type assertions (`as string`, `as boolean`, `as number`) are compile-time only. They provide zero runtime safety. If YAML input contains a number where a string is expected, or an object where a boolean is expected, the model will silently accept malformed data. This is a "trust the input" pattern that contradicts the "Zero Trust on Data" security principle.

**Impact:** Low for current usage (CLI tool parsing its own YAML), but this is a foundational library that will be consumed by STORY-004 and beyond. Type confusion in config parsing could cause unexpected behavior in downstream features.

**Recommendation:** Add runtime type checks in `requireField` or create typed variants (`requireString`, `requireBoolean`, `requireNumber`) that validate at runtime. This can be deferred to a follow-up story if agreed.

### LOW: Error messages expose internal class names

**Location:** `src/models.ts:24`

**Risk:** Error format `"Missing required field '${key}' in ${model}"` includes internal class names. Acceptable for a CLI library but should be documented as internal errors not suitable for external API responses.

## Summary

The changes are appropriate for a pure data model layer. The code is well-structured with `readonly` properties, consistent factory patterns, and thorough test coverage. The CRITICAL `McpServerConfig.env` finding has been resolved with `toJSON()` masking and JSDoc documentation. The remaining MEDIUM item (runtime type validation) is a defense-in-depth improvement that can be addressed in a follow-up story. Low-severity items relate to defensive practices for downstream consumers rather than immediate vulnerabilities.
