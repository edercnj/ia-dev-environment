---
name: finops
description: "FinOps practices: resource rightsizing, cost allocation, spot instances, reserved capacity, cost alerting, and cloud-specific cost optimization tools"
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: FinOps

## Purpose

Provides FinOps (Financial Operations) practices for cloud cost management in {{LANGUAGE}} {{FRAMEWORK}} projects. Covers resource rightsizing, cost allocation, spot/preemptible instances, reserved capacity, cost alerting, and cloud-specific optimization tools.

## Quick Reference

See `references/cost-optimization-checklist.md` for a layer-by-layer cost optimization checklist (compute, storage, network, database).

See `references/tagging-strategy-template.md` for a tagging strategy template with mandatory and optional tags.

## Resource Rightsizing

Align provisioned resources with actual usage to eliminate waste.

- **CPU/Memory Analysis**: Compare actual usage (P95/P99) against provisioned capacity; target 60-80% utilization
- **Recommendation Engines**: AWS Compute Optimizer, Azure Advisor, GCP Recommender provide automated rightsizing suggestions
- **Rightsizing Cadence**: Monthly review of resource utilization metrics; adjust after sustained usage patterns (minimum 14 days of data)
- **Container Resource Limits**: Set CPU requests/limits and memory requests/limits based on observed usage; avoid unbounded containers
- **Vertical vs Horizontal**: Prefer horizontal scaling (more small instances) over vertical scaling (fewer large instances) for cost efficiency

## Cost Allocation

Track and attribute cloud costs to teams, services, and environments.

- **Tagging Strategy**: Mandatory tags: `team`, `environment`, `service`, `cost-center`; optional: `project`, `owner`, `expiry`
- **Cost Centers and Ownership**: Each service must have a designated cost owner who reviews monthly spend
- **Showback vs Chargeback**: Showback (visibility into costs per team) as first step; chargeback (billing teams) for mature organizations
- **Untagged Resource Detection**: Automated policies to detect and flag untagged resources; enforce tagging at provisioning time
- **Budget Boundaries**: Define per-team and per-environment budgets; alert at 80% and 100% thresholds

## Spot/Preemptible Instances

Leverage discounted compute for fault-tolerant workloads.

- **Interruption Handling**: Implement graceful shutdown (SIGTERM handler), checkpointing for long-running tasks, and state externalization
- **Workload Suitability**: Best for stateless services, batch processing, CI/CD pipelines, and fault-tolerant workloads
- **Spot Fleet Strategies**: Diversify across instance types and availability zones to reduce interruption probability
- **Fallback to On-Demand**: Configure automatic fallback to on-demand instances when spot capacity is unavailable
- **Cost Savings**: Typical 60-90% discount compared to on-demand pricing

## Reserved Capacity

Commit to usage for predictable workloads to reduce costs.

- **RI vs Savings Plans**: Reserved Instances lock to instance type; Savings Plans offer flexibility across instance families
- **Break-Even Analysis**: Typically 7-9 months for 1-year reservations; calculate based on current on-demand spend
- **Commitment Period**: 1-year (lower discount, lower risk) vs 3-year (higher discount, higher commitment)
- **Coverage Recommendations**: Reserve 70-80% of baseline capacity; use on-demand/spot for variable load
- **Review Cadence**: Quarterly review of reservation utilization and coverage; adjust for workload changes

## Cost Alerting

Proactive monitoring and notification for cloud spend.

- **Budget Alerts**: Per team, per environment, per service; alert at 50%, 80%, 100% of budget
- **Anomaly Detection**: Detect unexpected cost spikes (>20% day-over-day or >50% week-over-week increase)
- **Billing Threshold Notifications**: Absolute threshold alerts for total account spend
- **Daily/Weekly Cost Reports**: Automated reports showing spend breakdown, trends, and top cost drivers
- **Forecast Alerts**: Notify when projected monthly spend exceeds budget based on current burn rate

## FinOps Practices

Organizational practices for cloud financial management.

- **Cost Review Cadence**: Weekly team review (15 min), monthly organization review (30 min), quarterly strategic review
- **Optimization Sprints**: Dedicate 10-20% of sprint capacity to cost reduction tasks
- **Unit Economics**: Track cost per transaction, cost per user, cost per request; set targets and monitor trends
- **Waste Elimination**: Identify and remove unused resources (unattached volumes, idle load balancers, orphaned snapshots)
- **Architecture Cost Reviews**: Include cost impact assessment in architecture decision records (ADRs)

## Cloud-Specific Cost Tools

### AWS

- **Cost Explorer**: Visualize and analyze spend patterns over time
- **Trusted Advisor**: Automated checks for cost optimization opportunities
- **Savings Plans**: Flexible commitment-based discounts across EC2, Lambda, Fargate
- **Compute Optimizer**: ML-based recommendations for EC2, EBS, Lambda right-sizing

### Azure

- **Cost Management**: Budget tracking, cost analysis, and optimization recommendations
- **Advisor**: Personalized best practices for cost, security, reliability, performance
- **Reservations**: Reserved VM Instances, SQL Database, Cosmos DB, and more

### GCP

- **Billing Console**: Cost breakdown, budgets, and alerts
- **Recommender**: ML-based recommendations for VM right-sizing and idle resource cleanup
- **Committed Use Discounts**: 1-year and 3-year commitments for compute and memory
