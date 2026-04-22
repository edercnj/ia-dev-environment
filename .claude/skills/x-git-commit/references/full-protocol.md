# x-git-commit — Full Protocol

> **Slim/Full split** per [ADR-0012 — Skill Body Slim-by-Default](../../../../../../../../../adr/ADR-0012-skill-body-slim-by-default.md).
> The `SKILL.md` sibling of this file carries the minimum viable contract
> (Triggers, Parameters, Output Contract, Error Envelope). This file holds
> the workflow detail, validation rules, examples, and diagnostic material.

## 1. Commit Message Format (RULE-016)

```
<type>(<TASK-XXXX-YYYY-NNN>): <subject> [TDD:TAG]

[optional body]

[optional footer]
```

### Examples

```
feat(TASK-0029-0005-001): add formatter detection logic [TDD:GREEN]

Implements language detection via {{LANGUAGE}} template variable.
Maps each language to its primary formatter with fallback support.

Refs: story-0029-0003
```

```
test(TASK-0029-0005-002): add unit tests for commit validation [TDD:RED]
```

```
refactor(TASK-0029-0005-003): extract validation into helper [TDD:REFACTOR]
```

```
docs(TASK-0029-0005-004): update README with commit format
```

## 2. TDD Tags (RULE-008)

| Tag | Meaning | When to Use |
|-----|---------|-------------|
| `[TDD]` | Generic TDD cycle | When specific phase does not apply |
| `[TDD:RED]` | Test fails — test written, implementation pending | After writing a failing test |
| `[TDD:GREEN]` | Test passes — minimal implementation done | After making the test pass |
| `[TDD:REFACTOR]` | Refactoring — no behavior change | After refactoring without changing tests |

- TDD tags are appended to the subject line after a space.
- When `--tdd` is not provided, no TDD tag is included.
- TDD tags are optional for non-TDD commits (docs, chore).

## 3. Workflow

```
1. VALIDATE    -> Check parameters (task ID, type, subject, tdd tag)
2. CHECK-STAGE -> Verify staged files exist
3. PRE-COMMIT  -> Run chain: x-code-format -> x-code-lint -> compile (unless --skip-chain)
4. BUILD-MSG   -> Construct commit message with task ID and TDD tag
5. COMMIT      -> Execute git commit (or git commit --amend)
6. REPORT      -> Output commit summary
```

### Step 1 — Validate Parameters

#### Task ID Validation

The task ID MUST match the pattern `TASK-XXXX-YYYY-NNN` where:
- `XXXX` = 4-digit epic number (zero-padded)
- `YYYY` = 4-digit story number (zero-padded)
- `NNN` = 3-digit task number (zero-padded)

```
Valid:   TASK-0029-0005-001, TASK-0001-0001-001
Invalid: TASK-29-5-1, TASK-0029-0005, task-0029-0005-001
```

If invalid: **ABORT** with message `"Task ID invalid -- expected format: TASK-XXXX-YYYY-NNN"`.

#### Subject Validation

- Maximum 72 characters.
- Imperative mood (detect and warn on common patterns: "adds" -> "add", "fixed" -> "fix", "updated" -> "update", "removed" -> "remove").
- No trailing period.

If subject exceeds 72 characters: **ABORT** with message `"Subject exceeds 72 characters (got: {length})"`.

#### Type Validation

Valid types: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `perf`.

If invalid: **ABORT** with message `"Invalid commit type: {type} -- valid types: feat, fix, test, refactor, docs, chore, perf"`.

#### TDD Tag Validation

Valid tags (case-insensitive input, uppercase output): `RED`, `GREEN`, `REFACTOR`, `TDD`.

If invalid: **ABORT** with message `"Invalid TDD tag: {tag} -- valid tags: RED, GREEN, REFACTOR, TDD"`.

### Step 2 — Check Staged Files

```bash
STAGED=$(git diff --cached --name-only)
if [ -z "$STAGED" ]; then
    echo "ABORT: No staged files for commit"
    exit 1
fi
echo "Staged files: $(echo "$STAGED" | wc -l)"
```

### Step 3 — Pre-Commit Chain (RULE-007)

Unless `--skip-chain` is provided, execute the chain sequentially.

#### 3a. Format (x-code-format)

Invoke the `x-code-format` skill via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-code-format", args: "")

If `x-code-format` modifies any files that were staged, **re-stage them automatically**:

```bash
# Record staged files before format (NUL-delimited for safety)
STAGED_BEFORE=$(git diff --cached --name-only -z)

# Run formatter (language-specific)
# For {{LANGUAGE}}: {{COMPILE_COMMAND}} equivalent
# See x-code-format skill for language-specific commands

# Re-stage files that were staged and modified by formatter
while IFS= read -r -d '' file; do
    if git diff --name-only -z | grep -zFx "$file" > /dev/null; then
        git add "$file"
        echo "Re-staged after format: $file"
    fi
done <<< "$STAGED_BEFORE"
```

If format fails: **ABORT** with message `"Pre-commit chain failed at step 'x-code-format': {error output}"`.

#### 3b. Lint (x-code-lint)

Invoke the `x-code-lint` skill via the Skill tool (Rule 13 — INLINE-SKILL pattern):

    Skill(skill: "x-code-lint", args: "")

Only ERROR-level findings block the commit.

If lint finds ERRORs: **ABORT** with message `"Pre-commit chain failed at step 'x-code-lint': {error count} errors found"`.

#### 3c. Compile

Execute the project compile command:

| Language | Build Tool | Compile Command |
|----------|-----------|-----------------|
| java | maven | `mvn compile -q` |
| java | gradle | `./gradlew compileJava -q` |
| kotlin | maven | `mvn compile -q` |
| kotlin | gradle | `./gradlew compileKotlin -q` |
| typescript | npm | `npx tsc --noEmit` |
| python | pip/poetry | `python -m py_compile` (or skip) |
| go | go | `go build ./...` |
| rust | cargo | `cargo check` |

The compile command for this project is: `{{COMPILE_COMMAND}}`.

If compile fails: **ABORT** with message `"Pre-commit chain failed at step 'compile': {error output}"`.

#### Skip Chain Warning

When `--skip-chain` is used:

```
WARNING: Pre-commit chain skipped -- emergency use only.
Format, lint, and compile checks were NOT executed.
Ensure code quality before pushing.
```

### Step 4 — Build Commit Message

Construct the commit message:

```
# With TDD tag:
{type}({task-id}): {subject} [TDD:{tag}]

# Without TDD tag:
{type}({task-id}): {subject}
```

If `--body` is provided, append it after a blank line:

```
{type}({task-id}): {subject} [TDD:{tag}]

{body}
```

### Step 5 — Execute Commit

```bash
# Normal commit
git commit -m "{message}"

# Amend commit
git commit --amend -m "{message}"
```

When `--amend` is used, warn:

```
NOTE: Amending last commit. Previous commit message will be replaced.
```

### Step 6 — Report

```
x-git-commit complete:
  Task:     {task-id}
  Type:     {type}
  Subject:  {subject}
  TDD:      {tag | none}
  Chain:    {passed | skipped}
  Commit:   {short-sha}
  Message:  {full commit message first line}
```

## 4. Rationale

The slim `SKILL.md` intentionally omits the step-by-step workflow and shell
snippets above. Runtime LLM invocations in the happy path do not need the
detail — they need to know which arguments are valid, what output to expect,
and which error codes to surface. When an operator hits an edge case (chain
failure during `--amend`, task-ID format disagreement with a hand-created
branch), this full-protocol file is the diagnostic reference.

See [ADR-0012 — Skill Body Slim-by-Default](../../../../../../../../../adr/ADR-0012-skill-body-slim-by-default.md)
for the full rationale and [ADR-0011 — Shared Snippets Inclusion Strategy](../../../../../../../../../adr/ADR-0011-shared-snippets-inclusion-strategy.md)
for how `_shared/error-handling-pre-commit.md` composes with this split.
