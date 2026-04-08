---
name: disaster-recovery
description: "Disaster recovery patterns: DR strategies, RPO/RTO, failover automation, DR testing, multi-region patterns, and recovery procedures"
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: Disaster Recovery

## Purpose

Provides comprehensive disaster recovery patterns for {{LANGUAGE}} {{FRAMEWORK}} services deployed in containerized environments. Covers DR strategy selection, RPO/RTO definitions, failover automation, testing cadence, multi-region data patterns, communication plans, and per-component recovery procedures.

## Quick Reference (always in context)

See `references/dr-strategy-decision-tree.md` for DR strategy selection based on RPO/RTO requirements. See `references/rpo-rto-calculator.md` for RPO/RTO calculation based on SLA alignment.

## Detailed References

Read these files for comprehensive disaster recovery guidance:

| Reference | Content |
|-----------|---------|
| `references/dr-strategy-decision-tree.md` | DR strategy selection flowchart based on RPO/RTO requirements, cost constraints, and complexity tolerance |
| `references/rpo-rto-calculator.md` | RPO/RTO calculation based on SLA alignment, business impact analysis, and cost justification |

## DR Strategies

| Strategy | RPO | RTO | Cost | Complexity | Description |
|----------|-----|-----|------|------------|-------------|
| Active-Active | ~0 | ~0 | Very High | Very High | Multi-region active, all regions serve traffic simultaneously |
| Active-Passive | Minutes | Minutes-Hours | High | High | Standby region activated on primary failure |
| Warm Standby | Minutes | Minutes | Medium | Medium | Scaled-down replica in standby, fast activation |
| Pilot Light | Hours | Hours | Low | Medium | Minimal resources in standby, activated and scaled on failure |

### Active-Active

- All regions serve production traffic simultaneously
- Data replicated synchronously across regions
- Lowest RPO/RTO but highest cost and complexity
- Requires conflict resolution for concurrent writes
- Best for: mission-critical, zero-downtime requirements

### Active-Passive

- Primary region serves all traffic; standby on hot standby
- Asynchronous data replication to standby
- Moderate RPO (minutes of data loss) and RTO (minutes to hours)
- Failover requires DNS switch and health check validation
- Best for: important services with moderate downtime tolerance

### Warm Standby

- Scaled-down replica running in secondary region
- Receives replicated data continuously
- Fast activation: scale up resources and switch traffic
- Best for: services needing fast recovery at moderate cost

### Pilot Light

- Minimal infrastructure in secondary region (database replicas only)
- Application infrastructure provisioned on demand during failover
- Longest RTO but lowest ongoing cost
- Best for: non-critical services with hours of acceptable downtime

## RPO/RTO Definitions

### Recovery Point Objective (RPO)

Maximum acceptable data loss measured in time. Defines how frequently backups or replication must occur.

- **RPO = 0**: Zero data loss (synchronous replication required)
- **RPO = 5 min**: Up to 5 minutes of transactions may be lost
- **RPO = 1 hour**: Hourly backup/replication acceptable
- **RPO = 24 hours**: Daily backup sufficient

### Recovery Time Objective (RTO)

Maximum acceptable downtime from failure detection to service restoration.

- **RTO < 1 min**: Automated failover required (active-active)
- **RTO < 15 min**: Automated failover with health checks (active-passive)
- **RTO < 1 hour**: Semi-automated with manual verification
- **RTO < 4 hours**: Manual failover procedures acceptable

### SLA Alignment

- Map RPO/RTO to SLA commitments (99.9% = ~8.7h/year downtime)
- Document gap between current capability and SLA requirement
- Cost-justify DR investment based on business impact analysis

## Failover Automation

### DNS Failover

- Health check-based DNS routing (Route53, CloudFlare, Azure Traffic Manager)
- TTL configuration: low TTL (30-60s) for fast failover
- Weighted routing for gradual traffic shift during planned failovers

### Load Balancer Failover

- Health check-based backend selection
- Cross-region load balancing with geographic affinity
- Connection draining during failover transitions

### Database Failover

- Read replica promotion to primary
- Multi-master replication with automatic leader election
- Point-in-time recovery for data corruption scenarios
- Connection string management (DNS-based endpoint or service discovery)

### Automated Runbook Execution

- Event-driven failover triggers (monitoring alerts)
- Step-by-step automated procedures with human approval gates
- Rollback automation for failed failover attempts
- Post-failover validation checks

## DR Testing Cadence

| Test Type | Frequency | Scope | Confidence |
|-----------|-----------|-------|------------|
| Tabletop Exercise | Quarterly | Communication and procedures | Low cost, validates communication |
| Simulation Test | Bi-annually | Partial failover of components | Medium cost, validates automation |
| Full Failover Test | Annually | Complete region switchover | High cost, highest confidence |

### Tabletop Exercise

Walk through DR procedures without executing them. Validate that documentation is current, roles are clear, and communication channels work.

### Simulation Test

Execute partial failover: fail over a single service or database. Validate that automation scripts work and recovery meets RTO/RPO targets.

### Full Failover Test

Complete region failover with production traffic. Maximum confidence but highest risk. Requires careful planning, monitoring, and rollback readiness.

## Multi-Region Patterns

### Data Replication

- **Synchronous**: Zero data loss, higher latency, limited by network distance
- **Asynchronous**: Low latency, potential data loss during failover
- **Semi-synchronous**: Acknowledge after at least one replica confirms

### Eventual Consistency

- Convergence guarantees: all replicas reach same state eventually
- Read-your-writes consistency: user sees their own updates immediately
- Causal consistency: operations that depend on each other are ordered

### Conflict Resolution

- **Last-Writer-Wins (LWW)**: Simple, may lose data, suitable for non-critical fields
- **Merge**: Application-level merge logic for concurrent updates
- **CRDT**: Conflict-free Replicated Data Types for automatic resolution

## Communication Plan

### Internal Communication

- **Engineering**: Real-time updates in incident channel, status page
- **Management**: Executive summary every 30 minutes during incident
- **Support**: Customer-facing FAQ and scripted responses

### External Communication

- **Customers**: Status page updates, email notifications for affected users
- **Partners**: Direct notification for API consumers with estimated recovery
- **Regulatory**: Incident reports per compliance requirements (timing, content)

### Templates per Severity

- **SEV1 (Critical)**: Immediate notification to all stakeholders, war room established
- **SEV2 (Major)**: Engineering and management notified, regular updates
- **SEV3 (Minor)**: Engineering notified, status page updated

## Recovery Procedures per Component

### Application

- Container restart with health check validation
- Horizontal scaling to handle backlog after recovery
- Redeployment from known-good image if corruption suspected
- Cache warmup before accepting full traffic

### Database

- Automated failover to read replica (promote to primary)
- Restore from backup with point-in-time recovery
- Data integrity validation post-recovery
- Connection pool reset and application restart

### Cache

- Cache warmup from database (lazy or eager loading)
- Cache rebuild from persistent storage if available
- Fallback to database during warmup period
- Monitor cache hit rate during recovery

### Message Broker

- Replay unprocessed messages from dead letter queue
- Partition recovery and rebalancing
- Consumer group offset management
- Message deduplication during replay

## Related Knowledge Packs

- `skills/sre-practices/` — incident management, error budgets, and change management
- `skills/infrastructure/` — Kubernetes manifests, health probes, and graceful shutdown
- `skills/data-management/` — backup/restore strategies and data replication patterns
