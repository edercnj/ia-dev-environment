# Tech Lead Review — STORY-007

**Decision:** GO
**Score:** 37/40
**Critical:** 0 | **Medium:** 2 | **Low:** 3

## Rubric Scores

| Section | Score | Max | Notes |
|---------|-------|-----|-------|
| A. Code Hygiene | 8 | 8 | No unused imports, no dead code, no warnings. Named constants (`JAVA_17_MINIMUM`, `PYTHON_310_MINOR`, etc.) replace all magic numbers. Clean method signatures throughout. |
| B. Naming | 4 | 4 | Intention-revealing names: `extractMajor`, `validateLanguageFramework`, `deriveProjectType`, `buildInfraPackRules`. No disinformation. Meaningful distinctions between `checkJava17Requirement` and `checkJavaFrameworkVersion`. |
| C. Functions | 5 | 5 | All functions under 25 lines. Single responsibility per function. Max 3 parameters (`checkJava17Requirement` has 3). No boolean flag parameters. `validateStack` delegates to focused sub-validators. |
| D. Vertical Formatting | 4 | 4 | `validator.ts`: 186 lines, `resolver.ts`: 137 lines, `skill-registry.ts`: 43 lines — all under 250. Newspaper Rule followed: public exports at top/bottom, private helpers in the middle. Blank lines separate logical blocks. |
| E. Design | 3 | 3 | DRY — `extractInterfaceTypes` reused in `deriveProjectType` and `deriveProtocols`. CQS respected — validators return error arrays (queries), resolver returns frozen object (query). No Law of Demeter violations (max 2 dots). |
| F. Error Handling | 2 | 3 | Error strings carry context (framework name, version, language). No null returns — uses `undefined` with early returns. However, `resolveDockerImage` has an unreachable `catch` block (line 54-56): `String.replace` never throws, making this dead error handling. |
| G. Architecture | 5 | 5 | Domain layer imports only `node:fs`/`node:path` (standard library) and own domain modules. No framework or adapter imports. Dependencies match the plan exactly. `index.ts` re-exports all three new modules correctly. |
| H. Framework & Infra | 3 | 4 | `Object.freeze()` on `ResolvedStack` and `CORE_KNOWLEDGE_PACKS` ensures immutability. No DI needed (pure functions). However, `validator.ts` imports `statSync` from `node:fs` directly — a minor coupling that could be abstracted via a port for testability (currently tested with real temp dirs, which is acceptable but not ideal). |
| I. Tests | 3 | 3 | Line coverage: validator 100%, resolver 98%, skill-registry 100%. Branch coverage: validator 95.08%, resolver 92.85%, skill-registry 100%. All exceed thresholds. 145 tests total with extensive parametrized coverage. Edge cases (empty versions, unknown languages, alpha versions) covered. |
| J. Security & Production | 0 | 1 | Error messages embed user-supplied config values (`framework name`, `language name`, `version`) without encoding. While this is a CLI tool (low risk), it was flagged in the security review and remains unaddressed. |
| **Total** | **37** | **40** | |

## Findings

### Critical
None

### Medium
1. **Unreachable catch block in `resolveDockerImage`** (`resolver.ts:54-56`): `String.prototype.replace()` with string arguments never throws. The `try/catch` is dead code inherited from the Python migration. Remove the try/catch and call `template.replace(...)` directly.
2. **QA finding not addressed: shared fixture duplication** — `validator.test.ts` and `resolver.test.ts` both alias `aValidationTestConfig` as `buildConfig` at module scope (line 14 and line 6 respectively). While the underlying fixture is shared (`project-config.fixture.ts`), the alias pattern is duplicated. Consider importing `aValidationTestConfig` directly without the alias, or accept the alias as a readability choice and document the decision.

### Low
1. **`verifyCrossReferences` unused `_config` parameter** (`validator.ts:172`): The `_config` parameter is never read. It exists for future extensibility but currently violates YAGNI. Consider removing it until needed, or add a JSDoc comment explaining the planned use.
2. **`resolveCommands` manually copies all fields** (`resolver.ts:38-46`): The function extracts every field from `LanguageCommandSet` into a plain `Record<string, string>`. It could return the `LanguageCommandSet` directly (or spread it), reducing maintenance burden when fields are added.
3. **Security review finding (output encoding)**: Error messages include user-supplied values without encoding. Low risk for a CLI tool, but noted for consistency with security review.

## Specialist Review Verification
- Security findings addressed: Partial — [2] output encoding remains LOW risk, accepted for CLI context
- QA findings addressed: Partial — [9] fixture alias duplication remains, but underlying fixture is centralized
- Performance findings addressed: Yes — all 13 items passed (26/26)

## Cross-File Consistency
- **Imports**: All cross-file imports use `.js` extension consistently (ESM convention). No circular dependencies detected.
- **Exports in `index.ts`**: All three new modules (`validator.js`, `resolver.js`, `skill-registry.js`) are re-exported. Matches public API surface.
- **Type consistency**: `resolver.ts` correctly uses `INTERFACE_SPEC_PROTOCOL_MAP` (not `INTERFACE_PROTOCOL_MAP`) as noted in the plan's risk assessment.
- **No repeated patterns needing abstraction**: Each file has distinct responsibilities with no duplicated logic across source files.
