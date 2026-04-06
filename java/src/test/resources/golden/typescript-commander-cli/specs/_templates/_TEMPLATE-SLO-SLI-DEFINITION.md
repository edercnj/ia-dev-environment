# SLO/SLI Definitions — ia-dev-environment

## Service Overview

| Field | Value |
| :--- | :--- |
| **Service Name** | ia-dev-environment |
| **Role** | _Describe the service role in the ecosystem_ |
| **Stakeholders** | _Engineering, Product, SRE_ |
| **Criticality** | _Tier 1 (Critical) / Tier 2 (Important) / Tier 3 (Standard)_ |

## SLI Definitions

| SLI | Metric | Method | Source |
| :--- | :--- | :--- | :--- |
| **Availability** | Uptime ratio (successful requests / total requests) | Server-side measurement via HTTP status codes | Load balancer access logs, application metrics |
| **Latency** | Response time distribution (p50, p95, p99) | Server-side measurement from request receipt to response sent | Application histogram metrics (OpenTelemetry) |
| **Throughput** | Requests per second at stable load | Server-side counter of completed requests | Application counter metrics, APM dashboard |
| **Error Rate** | 5xx responses / total responses | Server-side classification of response status codes | Application error counter, load balancer logs |

### SLI Measurement Notes

- **Availability**: Exclude planned maintenance windows from calculation
- **Latency**: Measure at the application boundary (after TLS termination, before response serialization)
- **Throughput**: Measure sustained throughput, not burst capacity
- **Error Rate**: Include only server errors (5xx); client errors (4xx) are excluded from SLO

## SLO Targets

| SLI | Target | Window | Consequence of Violation |
| :--- | :--- | :--- | :--- |
| **Availability** | 99.9% | Rolling 30 days | Freeze non-critical deploys, trigger incident review |
| **Latency (p50)** | < 100ms | Rolling 30 days | Performance optimization sprint |
| **Latency (p95)** | < 500ms | Rolling 30 days | Investigation and optimization plan |
| **Latency (p99)** | < 1000ms | Rolling 30 days | Mandatory performance review |
| **Throughput** | >= 1000 req/s | Rolling 30 days | Capacity planning review |
| **Error Rate** | < 0.1% | Rolling 30 days | Root cause analysis required |

### Target Adjustment Process

1. Review SLO targets quarterly based on actual performance data
2. Propose changes via ADR with justification
3. Communicate target changes to all stakeholders at least 2 weeks in advance
4. Update monitoring and alerting configurations to match new targets

## Error Budget Policy

| Budget Consumed | Action | Responsible |
| :--- | :--- | :--- |
| **< 50%** | Normal operations; deploy at will | Engineering Team |
| **50% consumed** | Review recent changes; increase monitoring | Tech Lead |
| **75% consumed** | Freeze non-critical deployments; prioritize reliability work | Engineering Manager |
| **100% consumed** | Freeze all deployments; incident response mode; mandatory postmortem | SRE + Engineering Manager |

### Error Budget Calculation

```
Error Budget = (1 - SLO Target) x Time Window

Example for 99.9% availability over 30 days:
  Budget = (1 - 0.999) x 30 days = 0.001 x 43,200 minutes = 43.2 minutes of downtime allowed
```

### Budget Reset

- Error budgets reset at the start of each rolling window
- Carry-over of unused budget is NOT permitted
- Budget consumption rate is tracked daily

## Burn Rate Alerting Configuration

### Fast Burn Alert (Critical)

| Parameter | Value |
| :--- | :--- |
| **Burn Rate** | 14.4x (consumes 100% budget in 1 hour) |
| **Short Window** | 5 minutes |
| **Long Window** | 1 hour |
| **Severity** | P1 — Page (PagerDuty) |
| **Channel** | PagerDuty on-call rotation |
| **Response Time** | Immediate (< 5 min acknowledgment) |

### Slow Burn Alert (Warning)

| Parameter | Value |
| :--- | :--- |
| **Burn Rate** | 6x (consumes 100% budget in 5 hours) |
| **Short Window** | 30 minutes |
| **Long Window** | 6 hours |
| **Severity** | P2 — Ticket (Slack + Jira) |
| **Channel** | Slack #sre-alerts + Jira ticket auto-creation |
| **Response Time** | Within 30 minutes during business hours |

### Alert Configuration Checklist

- [ ] Fast burn alert configured for each SLI
- [ ] Slow burn alert configured for each SLI
- [ ] Alert routing verified (PagerDuty, Slack, email)
- [ ] Runbook linked to each alert
- [ ] Alert deduplication and grouping configured
- [ ] Silencing rules for planned maintenance windows

## Dashboard Requirements

### Mandatory Dashboard Panels

| Panel | Metric | Visualization |
| :--- | :--- | :--- |
| **Remaining Error Budget** | (Budget - Consumed) / Budget x 100% | Gauge with red/yellow/green zones |
| **Burn Rate (Current)** | Current error rate / allowed error rate | Time series with threshold lines |
| **SLI Trend (7d/30d)** | SLI value over time | Line chart with SLO target line |
| **Budget Exhaustion Forecast** | Projected date of budget exhaustion at current burn rate | Single stat with conditional formatting |
| **Error Budget Consumption** | Cumulative budget consumed over window | Area chart with 50%/75%/100% markers |
| **SLO Compliance History** | Monthly SLO met/missed status | Status timeline (green/red) |

### Dashboard Access

- **SRE Team**: Full access to all dashboards
- **Engineering Team**: Read access to service-specific dashboards
- **Management**: Read access to SLO compliance summary dashboard

## Review Cadence

### Monthly Review

| Activity | Frequency | Participants |
| :--- | :--- | :--- |
| SLO compliance check | Monthly | SRE + Tech Lead |
| Error budget consumption analysis | Monthly | SRE + Engineering Manager |
| Alert noise review (false positive rate) | Monthly | SRE |
| Dashboard accuracy verification | Monthly | SRE |

### Quarterly Adjustment

| Activity | Frequency | Participants |
| :--- | :--- | :--- |
| SLO target review and adjustment | Quarterly | SRE + Product + Engineering |
| SLI definition review | Quarterly | SRE + Tech Lead |
| Error budget policy review | Quarterly | SRE + Engineering Manager |
| Alerting threshold tuning | Quarterly | SRE |

### Escalation Process

1. **SLO violated for 1 month**: Tech Lead reviews with SRE; action plan created
2. **SLO violated for 2 consecutive months**: Engineering Manager escalation; reliability sprint scheduled
3. **SLO violated for 3 consecutive months**: VP Engineering review; dedicated reliability team assigned
