---
name: new-feature
description: >
  Orchestrates the complete feature implementation cycle: planning,
  layer-by-layer implementation, review, and PR creation.
---

# New Feature Implementation

Follow these steps to implement a new feature in **my-spring-hexagonal**.

## Prerequisites

- A story file (e.g., `plans/STORY-XXX.md`) with acceptance criteria
- All dependency stories completed

## Workflow

### Step 1 — Plan

Use the **x-dev-story-implement** skill to orchestrate the full cycle:

```
/x-dev-story-implement @plans/STORY-XXX.md
```

This skill handles: branch creation, architecture planning, task decomposition,
implementation, review, and PR creation.

### Step 2 — Implement

If you prefer manual control, use **x-dev-implement** for the coding phase:

```
/x-dev-implement @plans/STORY-XXX.md
```

This implements layer-by-layer following hexagonal architecture with
java 21 / spring-boot 3.x.

### Step 3 — Review

Run the parallel review with specialist engineers:

```
/x-review
```

This launches Security, QA, Performance, and other specialist reviewers in parallel.

### Step 4 — Push & PR

After review passes, push and create PR:

```
/x-git-push
```

## Agents Involved

- **java-developer** — Primary implementer
- **tech-lead** — Architecture decisions and final review

## Tips

- Always run the full lifecycle for story-level work
- Use x-dev-implement only for focused coding tasks
- Review findings with CRITICAL severity must be fixed before merge
