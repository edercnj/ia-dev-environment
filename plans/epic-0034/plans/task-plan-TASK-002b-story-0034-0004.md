# Task Plan -- TASK-0034-0004-002b

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0004-002b |
| Story ID | story-0034-0004 |
| Epic ID | 0034 |
| Source Agent | merged(Architect, Tech Lead, Product Owner) |
| Type | implementation (edit) + refactor |
| TDD Phase | GREEN + REFACTOR |
| Layer | application |
| Estimated Effort | S (~1.5-2 hours) |
| Date | 2026-04-10 |

## Objective

Collapse `PlatformContextBuilder` and `PlatformFilter` to their single-platform form. After this task, the README template context exposes a single boolean `hasClaude` and one platforms-list entry; `PlatformFilter` becomes a near-passthrough whose public API is preserved but whose body reduces to the minimum meaningful form.

## Implementation Guide

1. **PlatformContextBuilder.java**:
   - Remove the `boolean copilot = ...` and `boolean codex = ...` locals.
   - Remove `flags.put("hasCopilot", copilot);` and `flags.put("hasCodex", codex);`.
   - Remove the `countActive(...)` private helper and the `flags.put("isMultiPlatform", ...)` line.
   - `resolveEffective()`: if `Platform.allUserSelectable()` has been reduced to `{CLAUDE_CODE}` only by story 0001, this method becomes a one-liner. Inline or keep — document the choice.
   - `buildCliNames()`: filter on `p != Platform.SHARED` is now a formality; keep for defensive programming.
   - Class-level Javadoc: delete mentions of `hasCopilot, hasCodex, isMultiPlatform`; describe only `hasClaude` and `platforms`.
2. **PlatformFilter.java**:
   - Read the current body.
   - Since `Platform.allUserSelectable()` returns `{CLAUDE_CODE}`, `shouldSkipFilter({CLAUDE_CODE}) == true` and `shouldSkipFilter(Set.of()) == true`. Every call site receives back the original descriptor list.
   - Decision point (Tech Lead rules): **keep the class** as a future extension point. Reduce the body to `return descriptors;` OR retain the stream pipeline and let it become a no-op at runtime (both are correct; the former is clearer).
   - Chosen approach: reduce to `return descriptors;` and delete `buildEffectiveSet`, `hasIntersection` private helpers.
   - Class-level Javadoc: update to reflect the reduced behavior.
3. **Tests**:
   - `PlatformContextBuilderTest`: delete any test asserting on `hasCopilot`, `hasCodex`, `isMultiPlatform` keys; keep tests asserting on `hasClaude` and `platforms`.
   - `PlatformFilterTest`: delete any multi-platform intersection tests; keep single-platform passthrough and empty-input tests.
4. Compile + test: `mvn -pl java compile test -Dtest=PlatformContextBuilderTest,PlatformFilterTest`.
5. Commit.

## Definition of Done

- [ ] `PlatformContextBuilder.buildPlatformFlags()` returns a map with exactly 2 keys: `hasClaude`, `platforms`
- [ ] `countActive()` private method deleted
- [ ] `PlatformFilter.filter()` body simplified (passthrough form); private helpers `buildEffectiveSet`, `hasIntersection` deleted
- [ ] Both files <= 60 lines each; no method > 25 lines
- [ ] Class-level Javadoc updated on both files (no mentions of Copilot/Codex/isMultiPlatform)
- [ ] `PlatformContextBuilderTest` and `PlatformFilterTest` updated and green
- [ ] `mvn -pl java compile test` green for impacted tests
- [ ] Commit body documents the Tech-Lead-wins decision (keep PlatformFilter class, don't inline)
- [ ] Conventional commit with scope `task-0034-0004-002b` in trailer

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0004-002 | Previous task removed upstream callers of the multi-platform form from the tables; this task can safely collapse the context builder without breaking those call sites. |

## Estimated Effort

- PlatformContextBuilder edit: 20 min
- PlatformFilter edit + decision documentation: 25 min
- Test updates: 30 min
- Compile + test run: 10 min
- Commit: 5 min
- **Total: ~1h 30 min**

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| A README template file still references `{{hasCopilot}}` or `{{hasCodex}}` tokens | Medium | Medium (broken template rendering) | Grep `resources/shared/templates/` and `resources/targets/claude/` for `{{hasCopilot}}` and `{{hasCodex}}` before commit. If found, those templates must be fixed in this task too. **CRITICAL**: if the grep hit is under `resources/shared/templates/`, the file is PROTECTED by RULE-004 — the correct fix is to simply leave the template alone; the token will just resolve to empty at runtime, which is the intended behavior |
| `PlatformFilter` has tests that exercise multi-platform intersection with Platform.SHARED | High | Low | Simplify tests to single-platform + SHARED assertions; document in commit |
| Reducing `PlatformFilter` to `return descriptors;` changes coverage of a previously-executed branch | Medium | Low | Branch coverage delta is absorbed by task 006's JaCoCo verify step; no action needed unless degradation exceeds 2pp |
