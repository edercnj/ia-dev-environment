# disaster-recovery

> Disaster recovery patterns: DR strategies, RPO/RTO definitions, failover automation, DR testing cadence, multi-region patterns, communication plans, and per-component recovery procedures.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-arch-plan, x-ops-incident, x-ops-troubleshoot, devops-engineer agent, architect agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- DR strategies: Active-Active, Active-Passive, Warm Standby, Pilot Light
- RPO/RTO definitions and SLA alignment
- Failover automation: DNS, load balancer, database, automated runbook execution
- DR testing cadence: tabletop exercises, simulation tests, full failover tests
- Multi-region patterns: data replication (sync/async/semi-sync), eventual consistency, conflict resolution (LWW, merge, CRDT)
- Communication plan: internal/external templates per severity level
- Recovery procedures per component: application, database, cache, message broker

## Key Concepts

This pack provides a structured approach to disaster recovery planning with four strategies ranging from Active-Active (near-zero RPO/RTO, highest cost) to Pilot Light (hours-level RPO/RTO, lowest cost). RPO and RTO are defined with concrete targets aligned to SLA commitments, guiding the selection of replication and backup strategies. Failover automation covers DNS-based routing, database replica promotion, and automated runbook execution with human approval gates. DR testing follows a progressive cadence from quarterly tabletop exercises through annual full failover tests. Per-component recovery procedures address application restart, database failover, cache warmup, and message broker replay.

## See Also

- [data-management](../data-management/) — Backup/restore strategies and data governance
- [infrastructure](../infrastructure/) — Kubernetes manifests and deployment patterns
- [compliance](../compliance/) — Incident response and regulatory reporting requirements
