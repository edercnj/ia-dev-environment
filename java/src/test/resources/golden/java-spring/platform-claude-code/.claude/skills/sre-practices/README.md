# sre-practices

> SRE practices: error budgets, toil reduction, on-call practices, capacity planning, incident management process, and change management. Covers SLO-based release gates, burn rate calculation, automation prioritization, rotation patterns, load testing methodology, blameless postmortems, and canary analysis.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | `x-ops-troubleshoot`, `x-ops-incident`, `x-review` (DevOps specialist), `devops-engineer` agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Error budgets (SLO-based release gates, burn rate calculation, exhaustion policy)
- Toil reduction (identification criteria, automation prioritization matrix, elimination strategies)
- On-call practices (rotation patterns, escalation policies, handoff procedures, fatigue management)
- Capacity planning (load testing methodology, growth modeling, headroom targets, resource right-sizing)
- Incident management (detection, response, mitigation, resolution, blameless postmortems)
- Change management (change freeze policies, rollback criteria, canary analysis, deployment velocity)

## Key Concepts

This pack provides the complete SRE operational framework covering six domains. Error budget management ties directly to deployment gates, freezing releases when the budget is exhausted. Toil reduction targets below 30% of team time with a prioritization matrix based on frequency, time cost, and risk. On-call practices include rotation patterns for different team sizes, escalation severity levels with response time SLAs, and fatigue management limits. The incident management process covers the full lifecycle from detection through blameless postmortems with action item tracking.

## See Also

- [observability](../observability/) — SLO/SLI framework, error budget calculation, and alerting strategy
- [resilience](../resilience/) — Circuit breakers, fallback patterns, and chaos engineering
- [release-management](../release-management/) — Release branching, hotfix process, and rollback procedures
- [infrastructure](../infrastructure/) — Health probes, graceful shutdown, and resource management
