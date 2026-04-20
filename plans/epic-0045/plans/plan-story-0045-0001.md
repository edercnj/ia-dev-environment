# Implementation Plan — story-0045-0001

**Story:** Criar skill `x-pr-watch-ci` com polling de CI + detecção de Copilot review
**Epic:** EPIC-0045

## 1. Affected Layers and Components

- `adapter/pr/` (new) — `PrWatchExitCode.java`, `PrWatchStatusClassifier.java`
- `skills/core/pr/x-pr-watch-ci/` (new) — `SKILL.md`, `README.md`
- Golden files — all target profiles

## 2. New Classes to Create

| Class | Package | Layer |
|-------|---------|-------|
| `PrWatchExitCode` | `dev.iadev.adapter.pr` | adapter |
| `PrWatchStatusClassifier` | `dev.iadev.adapter.pr` | adapter |

## 3. Method Signatures

### PrWatchExitCode (enum)
```java
package dev.iadev.adapter.pr;

public enum PrWatchExitCode {
    SUCCESS(0),
    CI_PENDING_PROCEED(10),
    CI_FAILED(20),
    TIMEOUT(30),
    PR_ALREADY_MERGED(40),
    NO_CI_CONFIGURED(50),
    PR_CLOSED(60),
    PR_NOT_FOUND(70);

    private final int code;
    PrWatchExitCode(int code) { this.code = code; }
    public int code() { return code; }
}
```

### PrWatchStatusClassifier
```java
package dev.iadev.adapter.pr;

public final class PrWatchStatusClassifier {
    public PrWatchExitCode classify(
        List<CheckResult> checks,
        CopilotReviewResult copilot,
        String prState,
        boolean mergedAt,
        long elapsedSeconds,
        ClassifierConfig cfg
    )
}
```

## 4. TDD Strategy

- TASK-0045-0001-004 (test first): @ParameterizedTest with 8 rows covering all exit codes
- TASK-0045-0001-003 (implementation): `PrWatchStatusClassifier.classify()` pure method
- TASK-0045-0001-001/002: SKILL.md base + polling logic
- TASK-0045-0001-005: README + golden regen

## 5. Task Order (respecting dependency declarations)

Wave 1 (parallel): TASK-0045-0001-001, TASK-0045-0001-004 (no deps)
Wave 2: TASK-0045-0001-002 (dep: 001), TASK-0045-0001-003 (dep: 004)
Wave 3: TASK-0045-0001-005 (dep: 001, 002)
