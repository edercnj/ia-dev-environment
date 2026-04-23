# Release Branching Guide

> **Cross-reference:** See Rule 09 (`rules/09-branching-model.md`) for mandatory branching conventions and branch naming rules.

## Default Recommendation

**GitFlow is the recommended branching strategy for all projects.** It provides clear separation between development and production, supports parallel release tracks, and enables formal release processes. Choose an alternative only when specific criteria justify it.

## Decision Flowchart

Use this guide to validate whether an alternative to GitFlow is appropriate.

## Step 1: Assess Release Frequency

| Frequency | Description |
|-----------|------------|
| Continuous | Multiple deploys per day; CI/CD fully automated |
| Regular | Weekly or biweekly releases; semi-automated |
| Scheduled | Monthly or quarterly releases; planned cycles |

## Step 2: Assess Team Size

| Size | Engineers | Characteristics |
|------|----------|----------------|
| Small | 1-4 | Single team, direct communication |
| Medium | 5-10 | Multiple sub-teams, some coordination |
| Large | 10+ | Multiple teams, formal coordination required |

## Step 3: Assess Compliance Requirements

| Level | Description |
|-------|------------|
| Low | Internal tools, experimental projects |
| Medium | Customer-facing products, SLA commitments |
| High | Regulated industries (finance, healthcare, government) |

## Decision Matrix

| Scenario | Strategy | Rationale |
|----------|----------|-----------|
| Default for all new projects | GitFlow (Recommended) | Formal process, clear separation, scalable |
| Large team, scheduled releases | GitFlow (Recommended) | Maximum control and traceability |
| Compliance/audit requirements | GitFlow (Mandatory) | Formal release process required |
| Open-source projects | GitFlow (Recommended) | Community contribution workflow |
| Small team, continuous deployment | Trunk-based (Alternative) | Maximum velocity when discipline exists |
| Small team, low compliance | Trunk-based (Alternative) | Minimal overhead, fast feedback |
| Medium team, periodic stabilization | Release branches (Alternative) | Focused stabilization periods |
| Rapid prototyping | Trunk-based (Alternative) | Speed over process |

## Strategy Details

### GitFlow (Recommended)

```
main ────●────────────●────────── (production releases)
          \          /
develop ──●──●──●──●──●──●──●──
           \      /
            feature/x
```

**Rules:**
- `main` reflects production state
- `develop` is the integration branch
- Feature branches from `develop`
- Release branches from `develop` for stabilization
- Hotfix branches from `main` for urgent fixes

> See Rule 09 for branch naming conventions: `feature/*`, `release/*`, `hotfix/*`, `bugfix/*`.

### Trunk-Based Development (Alternative)

```
main ──●──●──●──●──●──●──●──●── (continuous deployment)
        \       \
         tag    tag
        v1.0.0  v1.1.0
```

**Rules:**
- All developers commit to `main` (or short-lived feature branches < 1 day)
- Feature flags gate incomplete work
- Every commit is a release candidate
- Tags mark specific releases for traceability

### Release Branches (Alternative)

```
main ──●──●──●──●──●──●──●──●──
        \           \
         release/1.0 release/1.1
         ●──●──●     ●──●
         (stabilize)  (stabilize)
```

**Rules:**
- Branch from `main` at feature freeze
- Only bug fixes allowed on release branch
- Cherry-pick fixes back to `main`
- Delete release branch after EOL

## Migration Between Strategies

### Trunk-Based -> GitFlow

1. Create `develop` branch from `main`
2. Redirect feature branches to branch from `develop`
3. Introduce release branches for stabilization
4. Keep `main` as production-only; merge via release branches

### GitFlow -> Trunk-Based

1. Reduce release branch lifetime progressively
2. Introduce feature flags for incomplete work
3. Increase CI/CD automation and test coverage
4. Eventually merge `develop` into `main` workflow; tag releases directly
