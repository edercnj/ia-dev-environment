---
name: disaster-recovery
description: >
  Knowledge Pack: Disaster Recovery -- DR strategies (active-active, active-passive,
  warm standby, pilot light), RPO/RTO definitions, failover automation, DR testing
  cadence, multi-region patterns, and recovery procedures for my-spring-service.
---

# Knowledge Pack: Disaster Recovery

## Summary

Disaster recovery patterns for my-spring-service using java 21 with spring-boot.

### DR Strategies

| Strategy | RPO | RTO | Cost | Description |
|----------|-----|-----|------|-------------|
| Active-Active | ~0 | ~0 | Very High | Multi-region active, all regions serve traffic |
| Active-Passive | Minutes | Minutes-Hours | High | Standby region activated on failure |
| Warm Standby | Minutes | Minutes | Medium | Scaled-down replica, fast activation |
| Pilot Light | Hours | Hours | Low | Minimal standby, provisioned on demand |

### RPO/RTO

- **RPO (Recovery Point Objective)**: Maximum acceptable data loss measured in time
- **RTO (Recovery Time Objective)**: Maximum acceptable downtime from failure to restoration
- Align with SLA commitments: 99.9% = ~8.7h/year, 99.99% = ~52min/year

### Failover Automation

- DNS failover with health checks (low TTL: 30-60s)
- Load balancer failover with connection draining
- Database failover: read replica promotion, point-in-time recovery
- Automated runbook execution with approval gates

### DR Testing

- **Tabletop** (quarterly): Walk through procedures, validate documentation
- **Simulation** (bi-annually): Partial failover, validate automation
- **Full Failover** (annually): Complete region switchover, highest confidence

### Multi-Region Patterns

- **Synchronous replication**: Zero data loss, higher latency
- **Asynchronous replication**: Low latency, potential data loss
- **Conflict resolution**: Last-Writer-Wins, merge logic, or CRDTs

### Recovery per Component

- **Application**: Container restart, scaling, redeployment from known-good image
- **Database**: Replica promotion, backup restore, point-in-time recovery
- **Cache**: Warmup from database, rebuild, fallback to database
- **Message Broker**: Dead letter replay, partition recovery, offset management

## References

- [AWS Well-Architected — Reliability Pillar](https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/) -- DR strategies and multi-region patterns
- [Google SRE Book — Managing Incidents](https://sre.google/sre-book/managing-incidents/) -- Incident management and communication
- [Azure Architecture — Business Continuity](https://learn.microsoft.com/en-us/azure/architecture/framework/resiliency/) -- RPO/RTO planning and DR testing
