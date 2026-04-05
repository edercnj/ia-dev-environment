---
name: sre-practices
description: >
  Knowledge Pack: SRE Practices -- Error budgets, toil reduction, on-call practices,
  capacity planning, incident management process, and change management for my-spring-hexagonal.
---

# Knowledge Pack: SRE Practices

## Summary

SRE practices for my-spring-hexagonal using java 21 with spring-boot.

### Error Budgets

- Error budget = 1 - SLO target (e.g., 99.9% SLO = 0.1% budget = 43.8 min/month)
- Burn rate measures budget consumption speed (1.0x = normal, >14.4x = fast burn alert)
- Deploy freeze when budget exhausted; reduced velocity at 75% consumption
- Multi-window alerting: fast burn (1h/5m), slow burn (6h/30m)

### Toil Reduction

- Toil: manual, repetitive, automatable, tactical, no enduring value
- Maximum 50% of team time on toil (SRE standard)
- Prioritize automation by frequency x time x risk matrix
- Self-service tooling, runbook automation, policy-based auto-remediation

### On-Call Practices

- Rotation patterns: weekly, follow-the-sun, hybrid
- Escalation by severity: SEV-1 (5 min), SEV-2 (15 min), SEV-3 (30 min)
- Fatigue management: max 2 pages/hour, compensatory rest after SEV-1
- Shift handoff with log, context transfer, open incident review

### Capacity Planning

- Load testing: baseline, stress, soak, spike
- Minimum 30% headroom above observed peak
- Growth modeling: linear, exponential, seasonal adjustments
- Resource right-sizing based on 14-day P99 utilization

### Incident Management Process

- Detection: automated alerting, synthetic monitoring, user reports
- Response: acknowledge, classify severity, assign commander, open communication
- Mitigation: rollback, feature flags, traffic management, restart
- Postmortem: blameless, timeline, contributing factors, action items with owners

### Change Management

- Change freeze during holidays and major events
- Automatic rollback on 2x error rate or 3x latency baseline
- Canary analysis: 1-5% initial traffic, 15-30 min evaluation per stage
- DORA metrics: lead time, deploy frequency, change failure rate, MTTR

## References

- [Google SRE Book](https://sre.google/sre-book/table-of-contents/) -- Error budgets, toil, on-call, incident management
- [Google SRE Workbook](https://sre.google/workbook/table-of-contents/) -- Practical SRE implementations
- [PagerDuty Incident Response](https://response.pagerduty.com/) -- Incident management process and on-call best practices
