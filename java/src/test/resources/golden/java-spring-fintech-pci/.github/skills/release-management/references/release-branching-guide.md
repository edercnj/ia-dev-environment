# Release Branching Guide

## Decision Flowchart

Use this guide to select the optimal branching strategy for your project.

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

| Release Frequency | Team Size | Compliance | Strategy | Rationale |
|------------------|-----------|------------|----------|-----------|
| Continuous | Small | Low | Trunk-based | Maximum velocity, minimal overhead |
| Continuous | Small | Medium | Trunk-based + release tags | Traceability via tags |
| Continuous | Medium | Low | Trunk-based | Feature flags for isolation |
| Continuous | Medium | Medium | Trunk-based + release tags | Tags for audit trail |
| Continuous | Large | Low-Medium | Trunk-based + feature flags | Flags prevent integration conflicts |
| Regular | Small | Low | Trunk-based | Simple, effective |
| Regular | Medium | Low | Trunk-based + release tags | Release traceability |
| Regular | Medium | Medium | Release branches | Stabilization period |
| Regular | Large | Medium | Release branches | Parallel stabilization |
| Scheduled | Any | Low-Medium | Release branches | Planned stabilization |
| Scheduled | Medium | High | GitFlow | Formal release process |
| Scheduled | Large | High | GitFlow | Maximum control and traceability |

## Strategy Details

### Trunk-Based Development

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

### Release Branches

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

### GitFlow

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

## Migration Between Strategies

### Trunk-Based -> Release Branches

1. Continue committing to `main`
2. When ready to stabilize, create `release/x.y` from `main`
3. Only bug fixes on release branch; cherry-pick to `main`
4. Maintain both workflows during transition

### Release Branches -> Trunk-Based

1. Shorten release branch lifetime progressively
2. Introduce feature flags for incomplete work
3. Increase CI/CD automation and test coverage
4. Eventually eliminate release branches; tag releases from `main`
