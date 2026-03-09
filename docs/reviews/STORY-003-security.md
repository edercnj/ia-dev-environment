# Security Review -- STORY-003: Models (Python to TypeScript Migration)

```
ENGINEER: Security
STORY: STORY-003
SCORE: 16/20
STATUS: Approved
---
PASSED:
- [3] Authentication checks (2/2) — N/A: pure data model layer, no auth surface. No auth bypass possible.
- [4] Authorization checks (2/2) — N/A: no access control decisions in scope. No privilege escalation vectors.
- [7] Cryptography usage (2/2) — N/A: no cryptographic operations. No secrets, keys, or hashing involved.
- [8] Dependency vulnerabilities (2/2) — Zero new dependencies added. All 17 model classes use only TypeScript built-ins (string, number, boolean, Array, Record). No npm packages introduced.
- [9] CORS/CSP headers (2/2) — N/A: no HTTP endpoints, no server, no response headers.
- [10] Audit logging (2/2) — N/A: pure data model layer. No state mutations, no side effects, no operations requiring audit trail.

PARTIAL:
- [1] Input validation (1/2) — src/models.ts:lines 1114-1125 — Improvement: The `requireField` helper validates field presence but not field *type*. All `fromDict` methods use `as string`, `as boolean`, `as number` type assertions without runtime type checks. If a YAML config provides `name: 123` instead of `name: "some-string"`, the model silently accepts a number where a string is expected. For a config-parsing library, adding runtime type guards (e.g., `typeof value !== 'string'` checks) on required fields would prevent type confusion attacks in downstream consumers. [MEDIUM]
- [2] Output encoding (1/2) — src/models.ts:line 1121 — Improvement: Error messages in `requireField` interpolate the `key` and `model` parameters directly into the string template. While these values come from hardcoded string literals in source code (not user input), if `fromDict` is ever called with dynamic model/key names derived from external input, this could expose internal structure. Current usage is safe but lacks defensive encoding. [LOW]
- [6] Error handling (no stack traces leaked) (1/2) — src/models.ts:lines 1114-1125 — Improvement: Errors thrown by `requireField` expose internal model class names (e.g., "Missing required field 'name' in ProjectIdentity"). While acceptable for a CLI library, if these errors propagate to an API layer in a downstream consumer, they reveal internal implementation details. Consider documenting that consumers should catch and wrap these errors before exposing them externally. [LOW]

FAILED:
- [5] Sensitive data masking (0/2) — src/models.ts:line 1447, tests/node/models.test.ts:line 2029 — Fix: `McpServerConfig.env` stores environment variables as `Record<string, string>` with no data classification or masking. The test fixture includes `env: { TOKEN: "abc" }` and `env: { API_KEY: "x" }`, demonstrating that this field will carry secrets (API keys, tokens). There is no protection against these values being logged, serialized, or exposed in error messages. The `env` field should be documented as RESTRICTED/PROHIBITED data, and the class should either: (a) implement a custom `toString`/`toJSON` that masks env values, or (b) at minimum, add a JSDoc warning that `env` may contain secrets and must not be logged. The test fixtures normalizing secret-like values (TOKEN, API_KEY) without comment reinforces the risk that consumers will treat this field carelessly. [CRITICAL]
```

## Detailed Findings

### CRITICAL: McpServerConfig.env lacks sensitive data protection

**Location:** `src/models.ts` -- `McpServerConfig` class, `env` property (line 1426)

**Risk:** The `env` field is typed as `Readonly<Record<string, string>>` and is designed to hold environment variables for MCP server connections. Environment variables commonly contain secrets (API keys, tokens, passwords, connection strings). The current implementation:

1. Stores secrets as plain strings with no classification
2. Has no `toString()` or `toJSON()` override to prevent accidental serialization
3. Would expose all env values if the object is logged, stringified, or included in an error message
4. Test fixtures (`{ TOKEN: "abc" }`, `{ API_KEY: "x" }`) confirm this field carries secret-class data

**Recommendation:** Add a `toJSON()` method that masks env values, or at minimum add JSDoc documentation marking `env` as potentially containing PROHIBITED data per the project's security principles (Rule 07). Example:

```typescript
/** WARNING: May contain secrets (API keys, tokens). Never log or serialize without masking. */
readonly env: Readonly<Record<string, string>>;
```

### MEDIUM: No runtime type validation in fromDict methods

**Location:** All `fromDict` methods across 14 model classes in `src/models.ts`

**Risk:** Type assertions (`as string`, `as boolean`, `as number`) are compile-time only. They provide zero runtime safety. If YAML input contains a number where a string is expected, or an object where a boolean is expected, the model will silently accept malformed data. This is a "trust the input" pattern that contradicts the "Zero Trust on Data" security principle.

**Impact:** Low for current usage (CLI tool parsing its own YAML), but this is a foundational library that will be consumed by STORY-004 and beyond. Type confusion in config parsing could cause unexpected behavior in downstream features.

**Recommendation:** Add runtime type checks in `requireField` or create typed variants (`requireString`, `requireBoolean`, `requireNumber`) that validate at runtime. This can be deferred to a follow-up story if agreed.

### LOW: Error messages expose internal class names

**Location:** `src/models.ts` line 1121

**Risk:** Error format `"Missing required field '${key}' in ${model}"` includes internal class names. Acceptable for a CLI library but should be documented as internal errors not suitable for external API responses.

## Summary

The changes are appropriate for a pure data model layer. The code is well-structured with `readonly` properties, consistent factory patterns, and thorough test coverage. The primary security concern is the `McpServerConfig.env` field carrying secrets without any protective measures. For a library that will be the foundation of config handling, adding basic type validation would strengthen defense-in-depth. The remaining items are low-severity and relate to defensive practices for downstream consumers rather than immediate vulnerabilities.
