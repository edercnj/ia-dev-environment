# x-ops-troubleshoot

> Diagnoses errors, stacktraces, build failures, and unexpected behavior. Systematic approach: reproduce, locate, understand, fix, verify. Use whenever something fails: compilation errors, test failures, runtime exceptions, coverage gaps, or performance issues.

| | |
|---|---|
| **Category** | Operations |
| **Invocation** | `/x-ops-troubleshoot [error-description or test-name]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Provides a systematic approach to diagnose and fix issues across the project. Covers compilation errors, test failures, build failures, runtime exceptions, and performance issues. Always follows the fix-first-test pattern: write a test that reproduces the bug before fixing it, ensuring the bug-reproducing test prevents regressions.

## Usage

```
/x-ops-troubleshoot
/x-ops-troubleshoot "NullPointerException in TransactionService"
/x-ops-troubleshoot testCalculateTotal_negativeAmount
```

## Workflow

1. **Reproduce** -- Get the exact error (stacktrace, build log, test output)
2. **Locate** -- Find where the error originates in the codebase
3. **Understand** -- Determine why it is failing (expected vs actual behavior)
4. **Fix** -- Write a test that reproduces the bug, then fix the code
5. **Verify** -- Run the full test suite to ensure no regressions

## See Also

- [x-ops-incident](../x-ops-incident/) -- Escalate to incident response when issues affect production
- [x-test-run](../x-test-run/) -- Run tests with coverage after troubleshooting
- [x-story-implement](../x-story-implement/) -- References this skill during Phase 4 (fixes)
