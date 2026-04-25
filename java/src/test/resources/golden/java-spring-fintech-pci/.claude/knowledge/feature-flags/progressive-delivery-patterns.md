# Progressive Delivery Patterns

## Overview

Progressive delivery extends continuous delivery by gradually exposing new features to users through feature flags, canary deployments, and ring-based rollouts. This approach reduces blast radius and enables data-driven rollout decisions.

## Canary Deployment with Feature Flags

### Pattern

Route a small percentage of traffic to the new code path using feature flag evaluation. Monitor metrics and gradually increase the percentage.

### Rollout Stages

| Stage | Percentage | Duration | Gate Criteria |
|-------|-----------|----------|---------------|
| Canary | 1% | 30 minutes | Error rate < baseline + 0.1% |
| Early Adopters | 10% | 2 hours | p99 latency < baseline * 1.1 |
| Expanded | 50% | 4 hours | Business metrics within 5% of baseline |
| Full Rollout | 100% | 24 hours | All metrics stable |

### Implementation

```
// Flag configuration
{
  "key": "release.checkout-v2.enabled",
  "type": "release",
  "rollout": {
    "percentage": 1,
    "sticky": true,
    "hashAttribute": "userId"
  }
}
```

### Monitoring Gates

At each stage, verify:
- Error rate: compare flagged-on vs flagged-off cohorts
- Latency: p50, p95, p99 for both cohorts
- Business metrics: conversion rate, revenue per session
- Infrastructure: CPU, memory, connection pool utilization

### Rollback Procedure

1. Toggle flag to 0% (instant, no deployment needed)
2. Verify error rates return to baseline within 5 minutes
3. Investigate root cause with flag-off as safe state
4. Fix, re-test, restart rollout from canary stage

## Blue-Green with Feature Flags

### Pattern

Both old and new versions are deployed simultaneously. A feature flag controls which code path executes, enabling instant rollback without deployment.

### Advantages Over Traditional Blue-Green

| Aspect | Traditional | Flag-Based |
|--------|------------|-----------|
| Rollback speed | Minutes (redeploy) | Seconds (toggle) |
| Granularity | All-or-nothing | Per-user or percentage |
| Testing in prod | Difficult | Easy (dark launch) |
| Resource cost | 2x infrastructure | Shared infrastructure |

### Implementation Flow

```
1. Deploy new code path behind feature flag (disabled)
2. Enable flag for internal users (ring 0)
3. Expand to beta users (ring 1)
4. Gradual percentage rollout (ring 2+)
5. Full rollout -> cleanup flag and old code path
```

### Instant Rollback

```
// Before: flag enabled at 50%
featureFlagService.updateFlag("release.checkout-v2.enabled", 0);
// After: all traffic uses old code path immediately
// No deployment, no restart, no downtime
```

## Ring Deployments

### Concentric Rings

| Ring | Audience | Size | Purpose |
|------|----------|------|---------|
| Ring 0 | Internal team | ~50 users | Dogfooding, catch obvious bugs |
| Ring 1 | Beta testers | ~500 users | Early feedback, edge cases |
| Ring 2 | Early adopters | ~5,000 users | Broader validation, performance |
| Ring 3 | General availability | All users | Full rollout |

### Mapping Rings to Flag Targeting

```
// Ring 0: Internal employees
targeting: { "attribute": "email", "endsWith": "@company.com" }

// Ring 1: Beta program members
targeting: { "segment": "beta-testers" }

// Ring 2: 10% of all users
targeting: { "percentage": 10, "hashAttribute": "userId" }

// Ring 3: Everyone
targeting: { "percentage": 100 }
```

### Promotion Criteria

Each ring promotion requires:
- Minimum soak time (Ring 0: 1 day, Ring 1: 2 days, Ring 2: 1 week)
- Zero critical bugs reported from current ring
- Metrics within acceptable thresholds
- Explicit approval from feature owner

### Ring-Based Monitoring

```
// Alert configuration per ring
{
  "ring0": { "errorBudget": "99.99%", "alertDelay": "5m" },
  "ring1": { "errorBudget": "99.9%", "alertDelay": "15m" },
  "ring2": { "errorBudget": "99.9%", "alertDelay": "30m" },
  "ring3": { "errorBudget": "99.5%", "alertDelay": "1h" }
}
```

## Dark Launches

### Pattern

Deploy feature to production completely disabled. Exercise the new code path with shadow traffic or synthetic requests to validate correctness and performance without affecting real users.

### Shadow Traffic

```
// Pseudo-code for shadow traffic
incomingRequest -> {
    // Real response from current code path
    Response actual = currentHandler.handle(request);

    // Shadow execution (async, fire-and-forget)
    if (featureFlag.isEnabled("ops.shadow-checkout-v2.enabled")) {
        async {
            Response shadow = newHandler.handle(request.clone());
            compareAndLog(actual, shadow);
        }
    }

    return actual; // Always return current response
}
```

### Validation Metrics

| Metric | Description | Threshold |
|--------|-------------|-----------|
| Response equivalence | Shadow matches actual | > 99% |
| Latency overhead | Shadow execution time | < 2x actual |
| Error rate | Shadow errors | Track only, no alert |
| Resource usage | CPU/memory delta | < 10% increase |

### Dark Launch Lifecycle

1. **Deploy**: Ship new code path behind disabled flag
2. **Shadow**: Enable shadow traffic, compare responses
3. **Validate**: Verify correctness and performance
4. **Canary**: Enable for 1% of real users
5. **Rollout**: Progressive percentage increase
6. **Cleanup**: Remove flag and old code path

## Monitoring Integration

### Metrics to Track

```
// Per-flag metrics
feature_flag_evaluation_total{flag="release.checkout-v2.enabled", value="true"}
feature_flag_evaluation_total{flag="release.checkout-v2.enabled", value="false"}
feature_flag_evaluation_duration_seconds{flag="release.checkout-v2.enabled"}

// Per-cohort comparison
http_request_duration_seconds{flag_value="true", quantile="0.99"}
http_request_duration_seconds{flag_value="false", quantile="0.99"}
http_errors_total{flag_value="true"}
http_errors_total{flag_value="false"}
```

### Dashboard Layout

1. **Flag Status Panel**: Current percentage, toggle history
2. **Cohort Comparison**: Side-by-side metrics (flagged vs unflagged)
3. **Error Rate Delta**: Difference between cohorts over time
4. **Latency Percentiles**: p50, p95, p99 per cohort
5. **Business Metrics**: Conversion, revenue per cohort

### Automated Rollback Triggers

| Condition | Action | Notification |
|-----------|--------|-------------|
| Error rate > 2x baseline | Toggle to 0% | PagerDuty alert |
| p99 latency > 3x baseline | Toggle to 0% | Slack notification |
| Business metric drop > 10% | Hold current percentage | Email to owner |
| Flag evaluation errors > 1% | Toggle to 0% | PagerDuty alert |

## Cleanup Checklist

After successful full rollout:

- [ ] Flag at 100% for minimum 1 week with stable metrics
- [ ] Remove flag evaluation from application code
- [ ] Remove dead code path (old implementation)
- [ ] Remove flag-specific test fixtures
- [ ] Update documentation
- [ ] Archive flag in management system
- [ ] Verify no remaining references (grep codebase)
- [ ] Merge cleanup PR with reviewer approval
