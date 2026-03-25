---
name: run-smoke
description: "Skill: Smoke Tests — Runs the smoke test suite on-demand and produces a structured report with totals, category breakdown, and failure details."
allowed-tools: Read, Bash, Grep, Glob
argument-hint: "[profile: java-quarkus|go-gin|...|all] [category: pipeline|content|structure|cli|cross-profile|assembler|all]"
---

## Global Output Policy

- **Language**: English ONLY. (Ignore input language, always respond in English).
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.
- **Preservation**: All existing technical constraints below must be followed strictly.

# Skill: Smoke Tests (/run-smoke)

## Description

Executes the smoke test suite on-demand for the ia-dev-env Java CLI application. Smoke tests validate that the generation pipeline produces correct artifacts in quantity, structure, and content for all 8 bundled profiles. Results are presented as a structured markdown report per RULE-007.

**Condition**: This skill applies when you need to validate generation integrity without running the full test suite.

## Arguments

| Argument   | Format | Default | Description                                                                                      |
|------------|--------|---------|--------------------------------------------------------------------------------------------------|
| `profile`  | string | `all`   | Target profile: `java-quarkus`, `java-spring`, `go-gin`, `kotlin-ktor`, `python-click-cli`, `python-fastapi`, `rust-axum`, `typescript-nestjs`, or `all` |
| `category` | string | `all`   | Test category: `pipeline`, `content`, `structure`, `cli`, `cross-profile`, `assembler`, or `all` |

## Execution Flow

### Step 1 — Parse Arguments

Parse the user's input to determine `profile` and `category`. Defaults are `all` for both.

Examples:
- `/run-smoke` → profile=all, category=all
- `/run-smoke java-quarkus` → profile=java-quarkus, category=all
- `/run-smoke all pipeline` → profile=all, category=pipeline
- `/run-smoke java-quarkus content` → profile=java-quarkus, category=content

### Step 2 — Build Maven Command

Construct the Maven command based on the parsed arguments.

**Base command:**
```bash
cd java && mvn verify -P integration-tests -Dgroups=smoke
```

**With profile filter:**
```bash
cd java && mvn verify -P integration-tests -Dgroups=smoke -Dsmoke.profile=<profile>
```

**With category filter:**
```bash
cd java && mvn verify -P integration-tests -Dgroups=smoke -Dsmoke.category=<category>
```

**With both filters:**
```bash
cd java && mvn verify -P integration-tests -Dgroups=smoke -Dsmoke.profile=<profile> -Dsmoke.category=<category>
```

### Step 3 — Execute Tests

Run the constructed Maven command using Bash. Capture:
- Full stdout/stderr output
- Exit code (0 = all tests passed, non-zero = failures exist)

Set a generous timeout (300 seconds / 5 minutes) since smoke tests run the full generation pipeline.

### Step 4 — Parse Test Results

Extract results from the Maven/Surefire/Failsafe output. Look for:

1. **Summary line** — Pattern: `Tests run: N, Failures: N, Errors: N, Skipped: N`
2. **Per-class results** — Each test class maps to a category:
   - `PipelineSmokeTest` → pipeline
   - `ContentIntegritySmokeTest` → content
   - `FrontmatterSmokeTest` → structure
   - `CliModesSmokeTest` → cli
   - `CrossProfileConsistencySmokeTest` → cross-profile
   - `AssemblerRegressionSmokeTest` → assembler
3. **Failure details** — For any failed test, capture:
   - Test class and method name
   - Assertion message or exception
   - Relevant stack trace (first 5 lines)
4. **Execution time** — From `Total time:` in Maven output

If Surefire/Failsafe XML reports are available under `java/target/failsafe-reports/`, read them for precise per-test data.

### Step 5 — Produce Structured Report

Output the following markdown report (RULE-007 compliant):

```markdown
## Smoke Test Report

**Status:** PASS | FAIL
**Profile:** <profile> (<count> profiles)
**Category:** <category>
**Duration:** <time>

### Summary
| Category      | Tests | Passed | Failed | Skipped |
|---------------|-------|--------|--------|---------|
| Pipeline      | N     | N      | N      | N       |
| Content       | N     | N      | N      | N       |
| Structure     | N     | N      | N      | N       |
| CLI           | N     | N      | N      | N       |
| Cross-Profile | N     | N      | N      | N       |
| Assembler     | N     | N      | N      | N       |
| **Total**     | **N** | **N**  | **N**  | **N**   |

### Failed Tests
(none)
```

If there are failures, the "Failed Tests" section should list each failure:

```markdown
### Failed Tests

#### 1. PipelineSmokeTest.generatesExpectedFileCount_javaQuarkus
- **Category:** Pipeline
- **Profile:** java-quarkus
- **Error:** Expected 52 files but found 51
- **Details:**
  ```
  org.opentest4j.AssertionFailedError: Expected 52 files but found 51
    at dev.iadev.smoke.PipelineSmokeTest.generatesExpectedFileCount(PipelineSmokeTest.java:45)
  ```
```

## Category-to-Class Mapping

| Category      | Test Class                          | Validates                                    |
|---------------|-------------------------------------|----------------------------------------------|
| pipeline      | `PipelineSmokeTest`                 | File counts, directory structure per profile  |
| content       | `ContentIntegritySmokeTest`         | No empty files, no unresolved placeholders    |
| structure     | `FrontmatterSmokeTest`              | YAML frontmatter validity, required fields    |
| cli           | `CliModesSmokeTest`                 | dry-run, force, verbose, help modes           |
| cross-profile | `CrossProfileConsistencySmokeTest`  | Consistent structure across all 8 profiles    |
| assembler     | `AssemblerRegressionSmokeTest`      | Assembler output matches expected patterns    |

## Usage Examples

```
/run-smoke
/run-smoke java-quarkus
/run-smoke all pipeline
/run-smoke java-quarkus content
/run-smoke kotlin-ktor assembler
```

## Error Handling

- If Maven is not installed or `java/` directory does not exist, report the error clearly and stop.
- If the Maven command fails with a compilation error (not test failure), report it as a build error distinct from test failures.
- If no tests match the filter criteria, report "0 tests found" rather than treating it as a failure.

## Review Checklist

- [ ] Correct Maven command constructed based on arguments
- [ ] All 6 smoke test categories represented in report
- [ ] Per-category breakdown is accurate (not just totals)
- [ ] Failed tests include class name, method, error message, and stack trace
- [ ] Duration reported from Maven output
- [ ] Status is PASS only when 0 failures AND 0 errors
- [ ] Filtered runs correctly show only the requested profile/category
- [ ] Report format matches RULE-007 structured report requirements
