---
name: finops
description: >
  Knowledge Pack: FinOps -- Resource rightsizing, cost allocation, spot/preemptible
  instances, reserved capacity, cost alerting, and cloud-specific cost optimization
  tools for {project_name}.
---

# Knowledge Pack: FinOps

## Summary

FinOps practices for cloud cost management in {project_name} using {language_name} {language_version} with {framework_name}.

### Resource Rightsizing

- Compare actual usage (P95/P99) against provisioned capacity; target 60-80% utilization
- Monthly review of resource utilization metrics; adjust after sustained usage patterns (minimum 14 days)
- Set CPU/memory requests and limits based on observed usage; avoid unbounded containers

### Cost Allocation

- Mandatory tags: `team`, `environment`, `service`, `cost-center`
- Each service must have a designated cost owner who reviews monthly spend
- Automated policies to detect and flag untagged resources

### Spot/Preemptible Instances

- Implement graceful shutdown (SIGTERM handler) and checkpointing for long-running tasks
- Best for stateless services, batch processing, CI/CD pipelines
- Diversify across instance types and availability zones to reduce interruption probability

### Reserved Capacity

- Reserved Instances lock to instance type; Savings Plans offer flexibility across families
- Break-even typically 7-9 months for 1-year reservations
- Reserve 70-80% of baseline capacity; use on-demand/spot for variable load

### Cost Alerting

- Budget alerts per team, per environment, per service at 50%, 80%, 100% thresholds
- Anomaly detection for unexpected cost spikes (>20% day-over-day)
- Automated daily/weekly reports showing spend breakdown and trends

## References

- [FinOps Foundation](https://www.finops.org/) -- FinOps framework and best practices
- [AWS Cost Optimization](https://docs.aws.amazon.com/wellarchitected/latest/cost-optimization-pillar/) -- AWS Well-Architected cost pillar
- [Azure Cost Management](https://learn.microsoft.com/en-us/azure/cost-management-billing/) -- Azure cost management documentation
- [GCP Cost Management](https://cloud.google.com/billing/docs) -- GCP billing and cost management
