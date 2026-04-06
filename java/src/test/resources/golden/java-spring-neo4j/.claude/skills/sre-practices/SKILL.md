---
name: sre-practices
description: "SRE practices: error budgets, toil reduction, on-call practices, capacity planning, incident management process, and change management. Covers SLO-based release gates, burn rate calculation, automation prioritization, rotation patterns, load testing methodology, blameless postmortems, and canary analysis."
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: SRE Practices

## Purpose

Provides comprehensive Site Reliability Engineering practices for {{LANGUAGE}} {{FRAMEWORK}} services, enabling teams to maintain reliability, reduce operational toil, and manage incidents effectively. Covers error budget management, toil reduction strategies, on-call operations, capacity planning, incident response, and change management processes.

## Quick Reference (always in context)

See `references/error-budget-calculator.md` for SLO targets, burn rate formulas, and budget exhaustion thresholds.

See `references/on-call-handbook.md` for on-call rotation, escalation procedures, and page response workflow.

See `references/capacity-planning-template.md` for load testing methodology, growth projections, and resource sizing.

## Error Budgets

### SLO-Based Release Gates

Error budgets quantify acceptable unreliability derived from Service Level Objectives. When the budget is exhausted, new deployments are frozen until the budget recovers.

| SLO Target | Error Budget (monthly) | Allowed Downtime |
|-----------|----------------------|-----------------|
| 99.9% | 0.1% | 43.8 minutes |
| 99.95% | 0.05% | 21.9 minutes |
| 99.99% | 0.01% | 4.38 minutes |

**Release gate rule:** Deploy only when remaining error budget exceeds the estimated risk of the deployment.

### Burn Rate Calculation

Burn rate measures how fast the error budget is being consumed relative to the budget period.

- **Burn rate = 1**: Budget consumed evenly over the period (normal)
- **Burn rate > 1**: Budget consumed faster than expected (alert)
- **Fast burn alert**: Burn rate > 14.4 over 1 hour (budget exhausted in ~5 days)
- **Slow burn alert**: Burn rate > 6 over 6 hours (budget exhausted in ~12 days)

Formula: `burn_rate = (error_rate / error_budget_rate)`

### Budget Exhaustion Policy

| Consumption | Action |
|------------|--------|
| 50% consumed | Warning alert to SRE team; review recent changes |
| 75% consumed | Escalation to engineering lead; freeze non-critical deploys |
| 100% consumed | Full deploy freeze; all engineering effort on reliability |

### Error Budget Allocation

- Allocate budgets per calendar month or per rolling 30-day window
- Split budget across services proportionally to their SLO criticality
- Reserve 10% of budget for planned maintenance windows
- Track budget consumption in real-time dashboards

## Toil Reduction

### Toil Identification Criteria

Toil is work that is manual, repetitive, automatable, tactical, devoid of enduring value, and scales linearly with service growth.

| Criterion | Description |
|----------|-------------|
| Manual | Requires human intervention to complete |
| Repetitive | Performed more than once with the same steps |
| Automatable | Could be handled by software with existing technology |
| Tactical | Interrupt-driven rather than strategy-driven |
| No enduring value | Does not permanently improve the system |

### Automation Prioritization Matrix

Prioritize automation based on frequency, time cost, and risk:

| Priority | Frequency | Time per Occurrence | Risk if Manual |
|---------|-----------|-------------------|---------------|
| P0 (Critical) | Daily+ | > 30 minutes | High (outage risk) |
| P1 (High) | Weekly | > 15 minutes | Medium |
| P2 (Medium) | Monthly | > 1 hour | Low |
| P3 (Low) | Quarterly | > 2 hours | Minimal |

### Toil Budget

- Maximum 50% of team time on toil (SRE standard)
- Target: reduce toil to below 30% within 6 months
- Track toil hours per engineer per sprint
- Automate the highest-frequency toil items first

### Elimination Strategies

1. **Self-service tooling**: Provide developers with CLI tools and dashboards
2. **Runbook automation**: Convert manual runbooks to executable scripts
3. **Policy-based automation**: Auto-remediate known failure patterns
4. **Proactive elimination**: Fix root causes instead of treating symptoms

## On-Call Practices

### Rotation Patterns

| Pattern | Use Case | Pros | Cons |
|--------|----------|------|------|
| Weekly | Small teams (3-5) | Simple scheduling | Long shifts |
| Follow-the-sun | Distributed teams | No night pages | Handoff overhead |
| Hybrid | Medium teams (5-10) | Balanced load | Complex scheduling |

### Escalation Policies

| Severity | Initial Response | Escalation After | Commander |
|---------|-----------------|------------------|-----------|
| SEV-1 (Critical) | 5 minutes | 15 minutes | VP Engineering |
| SEV-2 (Major) | 15 minutes | 30 minutes | Engineering Manager |
| SEV-3 (Minor) | 30 minutes | 2 hours | Team Lead |
| SEV-4 (Low) | Next business day | N/A | On-call engineer |

### Handoff Procedures

- **Shift log**: Document all events, actions taken, and pending items
- **Context transfer**: 15-minute handoff call at rotation change
- **Open incidents**: Transfer ownership explicitly with status summary
- **Pending alerts**: Review suppressed or snoozed alerts

### Fatigue Management

- Maximum 2 pages per on-call hour (sustained)
- Compensatory rest after high-page shifts (> 10 pages)
- Post-incident rest period: minimum 8 hours after SEV-1
- Regular review of page frequency to reduce noise
- On-call compensation: time-off or financial per organization policy

## Capacity Planning

### Load Testing Methodology

| Test Type | Purpose | Duration |
|----------|---------|----------|
| Baseline | Establish normal performance | 1 hour at expected load |
| Stress | Find breaking point | Ramp until failure |
| Soak | Detect memory leaks and degradation | 8-24 hours at peak load |
| Spike | Validate auto-scaling | Sudden 10x load burst |

### Growth Modeling

- **Linear growth**: Steady user acquisition; plan for monthly growth rate
- **Exponential growth**: Viral features; plan for 2x capacity per quarter
- **Seasonal patterns**: Holiday peaks; pre-provision based on prior year data

### Headroom Targets

- Minimum 30% headroom above observed peak load
- Auto-scaling trigger at 70% resource utilization
- Database connections: maximum 80% of pool at peak
- Storage: provision when 60% utilized (lead time for procurement)

### Resource Right-Sizing

| Resource | Monitoring Period | Right-Size Trigger |
|---------|------------------|-------------------|
| CPU | 14-day p99 | < 30% utilization sustained |
| Memory | 14-day p99 | < 40% utilization sustained |
| Storage | 30-day trend | Growth rate < 5% monthly |
| Network | 7-day peak | < 50% bandwidth utilized |

## Incident Management Process

### Detection

| Method | Response Time | Reliability |
|--------|-------------|-------------|
| Automated alerting | Seconds to minutes | High (if thresholds correct) |
| Synthetic monitoring | 1-5 minutes | High (proactive) |
| User reports | Minutes to hours | Variable |
| Log analysis | Minutes | Medium (requires correlation) |

### Response

1. **Acknowledge**: Confirm the alert within SLA response time
2. **Classify severity**: Use predefined severity matrix
3. **Assign commander**: Incident commander owns coordination
4. **Open communication**: Dedicated channel per incident
5. **Notify stakeholders**: Status page update within 15 minutes

### Mitigation

| Strategy | When to Use | Risk |
|---------|------------|------|
| Rollback | Bad deployment identified | Low (known good state) |
| Feature flags | Feature-specific issue | Low (granular control) |
| Traffic management | Capacity-related issue | Medium (partial service) |
| Restart | Transient state corruption | Medium (data loss risk) |

### Resolution

- Identify and fix root cause (not just symptoms)
- Verify fix in staging before production
- Monitor for recurrence for 24 hours post-fix
- Update runbooks with new failure pattern

### Postmortem

- **Blameless culture**: Focus on systems, not individuals
- **Timeline**: Reconstruct minute-by-minute event sequence
- **Contributing factors**: Identify all factors, not just the trigger
- **Action items**: Assign owners and deadlines for each item
- **Review cadence**: Weekly postmortem review meeting
- **Follow-up**: Track action item completion rate (target: 100% within 30 days)

## Change Management

### Change Freeze Policies

| Period | Policy | Exception Process |
|--------|--------|------------------|
| Holiday season | Full freeze (2 weeks) | VP approval required |
| Major events | Feature freeze (1 week) | Rollback-safe changes only |
| Quarterly close | Reduced deployments | Business-critical fixes only |

### Rollback Criteria

Automatic rollback triggers:

- Error rate exceeds 2x baseline for 5 minutes
- P99 latency exceeds 3x baseline for 5 minutes
- Availability drops below SLO for 2 minutes
- Crash rate exceeds 1% for any endpoint

### Canary Analysis

| Parameter | Recommended Value |
|----------|------------------|
| Initial traffic | 1-5% |
| Evaluation window | 15-30 minutes per stage |
| Stage progression | 5% -> 10% -> 25% -> 50% -> 100% |
| Success criteria | Error rate delta < 0.1% vs control |
| Abort threshold | Error rate delta > 0.5% vs control |

### Deployment Velocity Metrics

- **Lead time**: Time from commit to production (target: < 1 day)
- **Deploy frequency**: Deployments per day (target: multiple per day)
- **Change failure rate**: Percentage of deploys causing incidents (target: < 5%)
- **MTTR**: Mean time to recover from failures (target: < 1 hour)
