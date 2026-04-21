## TDD Tags — Canonical Glossary

This glossary is the single source of truth for the Red-Green-Refactor tag set
used across TDD-oriented skills (`x-test-tdd`, `x-task-implement`,
`x-story-implement`). When any of those skills documents commit-footer
syntax or cycle semantics, link to this file instead of duplicating.

## Core Tags

| Tag | Meaning | When Emitted |
| :--- | :--- | :--- |
| `RED` | Failing test exists for the next behavior | First commit of a TDD cycle; test is added and is observed to fail |
| `GREEN` | Minimum implementation makes the RED test pass | Second commit of a TDD cycle; implementation is added, test is observed to pass |
| `REFACTOR` | Design improvement without behavior change | Optional third commit of a cycle; no new test added, all existing tests still pass |

## Variants

| Variant | Meaning | Consumer Skill |
| :--- | :--- | :--- |
| `RED_NOT_OBSERVED` | Tooling could not confirm the test failed before GREEN commit | `x-task-implement` aborts when this is detected (enforces TDD honesty) |
| `GREEN_FLAKY` | Test passed on retry but failed on first invocation | Logged as a warning; story-level review gate inspects |
| `REFACTOR_DEFERRED` | Cycle completed without a REFACTOR commit because no improvement was needed | Valid; not every cycle requires a refactor |
| `COALESCED` | Two or more logically atomic tasks committed together under RULE-TF-04 | `x-task-implement` emits `Coalesces-with: TASK-XXXX` footer |

## Conventional-Commits Footer Format

Every TDD commit carries one of the tags above as the **last line** of the
commit body, in the form:

```
<tag>: TASK-XXXX-YYYY-NNN
```

Example:

```
test(x-story-implement): add failing test for empty task list

RED: TASK-0047-0001-003
```

Multiple-tag commits (rare — only for COALESCED) use one tag per line.

## TPP Level Annotations

When a test is explicitly ordered per the Transformation Priority Premise, the
cycle commit MAY carry an additional footer line:

```
TPP-Level: <0-6>
```

| Level | Transformation |
| :--- | :--- |
| 0 | `{} -> nil` — no test to a test asserting the degenerate case |
| 1 | `nil -> constant` — replace nil with a concrete value |
| 2 | `constant -> constant+` — enumerate a second case |
| 3 | `unconditional -> conditional` — introduce branching |
| 4 | `scalar -> array / collection` | — introduce iteration |
| 5 | `statement -> recursion` — self-referential structure |
| 6 | `expression -> function call` — abstract into a named operation |

Skills consuming this glossary do not redefine these levels; they cite this
file when ordering their test plans.

## Forbidden

- Inventing per-skill tags that overlap with the core three (e.g., `TDD-RED`,
  `RED-START`). Use the canonical tags above verbatim.
- Omitting the tag footer on a TDD-cycle commit. A commit without a tag is
  treated as a non-TDD commit and fails the double-loop audit.
- Using lowercase (`red`, `green`). Tags are SHOUT_CASE for deterministic
  grep-ability.
