# finops

> FinOps practices: resource rightsizing, cost allocation, spot instances, reserved capacity, cost alerting, and cloud-specific cost optimization tools.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-dev-architecture-plan, x-review (DevOps specialist), devops-engineer agent, architect agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Resource rightsizing: CPU/memory analysis, recommendation engines, container resource limits
- Cost allocation: tagging strategy, cost centers, showback vs chargeback, budget boundaries
- Spot/preemptible instances: interruption handling, workload suitability, fleet strategies, fallback
- Reserved capacity: RI vs Savings Plans, break-even analysis, commitment periods, coverage recommendations
- Cost alerting: budget alerts, anomaly detection, forecast alerts, daily/weekly reports
- FinOps practices: review cadence, optimization sprints, unit economics, waste elimination
- Cloud-specific tools: AWS (Cost Explorer, Compute Optimizer), Azure (Cost Management, Advisor), GCP (Recommender, Committed Use Discounts)

## Key Concepts

This pack provides FinOps (Financial Operations) practices for cloud cost management, enabling teams to track, allocate, and optimize cloud spend. Resource rightsizing targets 60-80% utilization by comparing actual usage (P95/P99) against provisioned capacity with monthly review cycles. Cost allocation enforces mandatory tagging (team, environment, service, cost-center) with automated detection of untagged resources. Spot instances offer 60-90% savings for fault-tolerant workloads with graceful shutdown handling. The pack establishes organizational practices including weekly team cost reviews, quarterly strategic reviews, and unit economics tracking (cost per transaction, cost per user).

## See Also

- [infrastructure](../infrastructure/) — Docker builds, Kubernetes manifests, and resource management
- [disaster-recovery](../disaster-recovery/) — Multi-region patterns affecting cost structure
- [ci-cd-patterns](../ci-cd-patterns/) — Pipeline optimization for build cost reduction
