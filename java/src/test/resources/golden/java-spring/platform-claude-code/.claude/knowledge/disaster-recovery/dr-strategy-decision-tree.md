# DR Strategy Decision Tree

Use this decision tree to select the appropriate disaster recovery strategy based on RPO/RTO requirements and budget constraints.

## Decision Flow

```
RPO/RTO Requirements
|
+-- RPO < 1 min AND RTO < 1 min?
|   +-- YES --> Active-Active
|   +-- NO
|       +-- RPO < 15 min AND RTO < 1 hour?
|           +-- YES --> Active-Passive
|           +-- NO
|               +-- RPO < 1 hour AND RTO < 4 hours?
|                   +-- YES --> Warm Standby
|                   +-- NO --> Pilot Light
```

## Strategy Comparison

| Criteria | Active-Active | Active-Passive | Warm Standby | Pilot Light |
|----------|--------------|----------------|--------------|-------------|
| RPO | ~0 | Minutes | Minutes | Hours |
| RTO | ~0 | Minutes-Hours | Minutes | Hours |
| Cost (relative) | 200% | 150% | 120% | 110% |
| Complexity | Very High | High | Medium | Medium |
| Data Replication | Synchronous | Asynchronous | Asynchronous | Periodic backup |
| Failover Type | Automatic | Semi-automatic | Semi-automatic | Manual |
| Best For | Mission-critical | Important services | Standard services | Non-critical |

## Selection Criteria

### Choose Active-Active When

- Zero downtime is a business requirement
- SLA requires 99.99% or higher availability
- Revenue loss per minute of downtime exceeds DR infrastructure cost
- Geographic distribution of users requires low-latency access

### Choose Active-Passive When

- Minutes of downtime are acceptable
- RPO of a few minutes is tolerable
- Budget allows ~50% additional infrastructure cost
- Automated failover is desired but zero-downtime is not required

### Choose Warm Standby When

- Recovery within minutes is needed but budget is limited
- Service is important but not mission-critical
- Some data loss (minutes) is acceptable
- Fast scale-up capability exists in secondary region

### Choose Pilot Light When

- Hours of downtime are acceptable during disaster
- Service is not revenue-critical
- Budget constraints limit standby infrastructure
- On-demand provisioning can meet RTO requirements
