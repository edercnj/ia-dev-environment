# Error Budget Calculator

## SLO to Error Budget Conversion

Error budget is the inverse of the SLO target, expressed as allowed failures within a time period.

**Formula:** `error_budget = 1 - SLO_target`

| SLO Target | Error Budget | Monthly Downtime | Annual Downtime |
|-----------|-------------|-----------------|----------------|
| 99% | 1% | 7.31 hours | 3.65 days |
| 99.5% | 0.5% | 3.65 hours | 1.83 days |
| 99.9% | 0.1% | 43.8 minutes | 8.77 hours |
| 99.95% | 0.05% | 21.9 minutes | 4.38 hours |
| 99.99% | 0.01% | 4.38 minutes | 52.6 minutes |
| 99.999% | 0.001% | 26.3 seconds | 5.26 minutes |

## Burn Rate Calculation

Burn rate indicates how fast the error budget is being consumed.

**Formula:** `burn_rate = actual_error_rate / (error_budget / period_duration)`

### Examples

Given SLO = 99.9% (error budget = 0.1% per 30 days):

| Actual Error Rate | Burn Rate | Budget Exhaustion |
|------------------|-----------|-------------------|
| 0.1% | 1.0x | 30 days (normal) |
| 0.5% | 5.0x | 6 days |
| 1.0% | 10.0x | 3 days |
| 1.44% | 14.4x | ~2 days |
| 5.0% | 50.0x | ~14 hours |

## Multi-Window Alert Strategy

Use multiple windows to balance alert sensitivity and specificity:

| Alert Type | Long Window | Short Window | Burn Rate | Detection Time |
|-----------|------------|-------------|-----------|---------------|
| Fast burn | 1 hour | 5 minutes | > 14.4x | ~2 minutes |
| Slow burn | 6 hours | 30 minutes | > 6x | ~15 minutes |
| Steady burn | 3 days | 6 hours | > 1x | ~3 hours |

## Remaining Budget Calculation

**Formula:** `remaining_budget = error_budget - consumed_budget`

**Formula:** `consumed_budget = sum(error_minutes) / total_minutes_in_period`

### Tracking Example (30-day period)

```
Total minutes in period:   43,200
Error budget (99.9%):      43.2 minutes
Consumed (incidents):      15.0 minutes
Remaining budget:          28.2 minutes (65.3%)
Days remaining in period:  18 days
Projected burn rate:       0.83x (within budget)
```

## Budget Recovery

When budget is exhausted, recovery happens as the measurement window advances:

- **Fixed window**: Full budget restored at period boundary
- **Rolling window**: Budget recovers as old incidents age out
- **Hybrid**: Use rolling window with minimum fixed recovery at month start

## Decision Framework

| Remaining Budget | Deployment Decision |
|-----------------|-------------------|
| > 50% | Normal deployment velocity |
| 25-50% | Require extra testing; canary mandatory |
| 10-25% | Critical fixes only; extended canary |
| < 10% | Deploy freeze; reliability work only |
| 0% (exhausted) | Full freeze until budget recovers |
