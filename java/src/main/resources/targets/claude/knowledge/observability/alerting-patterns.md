# Alerting Patterns and Anti-Patterns

## Alerting Patterns

### Symptom-Based Alerting

Alert on symptoms that users experience rather than on internal causes.

| Symptom | Alert On | Instead Of |
|---------|----------|------------|
| Service unavailable | HTTP 5xx rate > threshold | CPU usage > 80% |
| Slow responses | p99 latency > SLO target | Database query count high |
| Data loss | Error rate on writes > 0.1% | Disk usage > 90% |

**Principle**: Users care about symptoms; causes are for investigation, not alerting.

### Golden Signals Alerting

Alert on the four golden signals from the Google SRE book:

| Signal | What to Measure | Alert Threshold |
|--------|----------------|-----------------|
| **Latency** | Duration of successful and failed requests | p95 > target for 5 min |
| **Traffic** | Demand on the system (requests/sec) | Sudden drop > 50% from baseline |
| **Errors** | Rate of failed requests (explicit and implicit) | Error rate > SLO budget burn rate |
| **Saturation** | Fullness of the most constrained resource | > 80% of capacity for 10 min |

### Multi-Window Multi-Burn-Rate Alerting

Use multiple time windows and burn rates to balance detection speed with alert precision.

| Burn Rate | Short Window | Long Window | Severity |
|-----------|-------------|-------------|----------|
| 14.4x | 5 min | 1 hour | P1 — Page |
| 6x | 30 min | 6 hours | P2 — Ticket |
| 3x | 2 hours | 3 days | P3 — Notification |
| 1x | 6 hours | 3 days | Dashboard only |

**How it works**:
1. Short window detects if the issue is happening NOW
2. Long window confirms it is not a transient spike
3. Both windows must fire for the alert to trigger
4. Higher burn rates trigger faster with shorter windows

### Composite Alerting

Combine multiple signals into a single alert to reduce noise:

```
ALERT: ServiceDegraded
  IF error_rate > 1% AND latency_p99 > 500ms
  FOR 5 minutes
  SEVERITY: P2
  RUNBOOK: https://runbooks.example.com/service-degraded
```

## Anti-Patterns

### Alert on Every Metric

**Problem**: Creating alerts for every metric the system exposes leads to hundreds of alerts, most of which are not actionable.

**Symptoms**:
- On-call engineer receives 50+ alerts per shift
- Most alerts are ignored or silenced permanently
- Real incidents get lost in alert noise

**Solution**: Only alert on metrics that directly indicate user-facing impact. Use dashboards for everything else.

### Threshold-Only Alerting

**Problem**: Setting static thresholds (CPU > 80%, memory > 90%) that do not adapt to system behavior and do not correlate with user impact.

**Symptoms**:
- Alerts fire during normal traffic spikes
- No alerts during actual incidents (threshold not reached)
- Constant threshold tuning without improvement

**Solution**: Use burn-rate alerting based on SLOs. Alert when error budget consumption rate indicates SLO violation is likely.

### Missing Runbook Links

**Problem**: Alerts fire without any guidance on how to investigate or resolve the issue.

**Symptoms**:
- On-call engineer spends time figuring out what the alert means
- Different engineers take different actions for the same alert
- Mean time to resolution (MTTR) increases

**Solution**: Every alert MUST link to a runbook with:
1. What the alert means
2. How to verify the issue
3. Step-by-step mitigation actions
4. Escalation criteria

### Alert Without Context

**Problem**: Alerts contain only the metric name and value without surrounding context.

**Solution**: Include in every alert:
- Service name and environment
- Current value vs threshold/SLO
- Trend (getting better or worse)
- Link to dashboard and runbook
- Related recent changes (deploy, config change)

### Duplicate Alerts

**Problem**: Multiple alerts fire for the same underlying issue, overwhelming the on-call engineer.

**Solution**:
- Group related alerts by service and failure mode
- Use alert deduplication with a 5-minute window
- Implement alert correlation to link cause-effect chains
- Suppress downstream alerts when upstream is already alerting
