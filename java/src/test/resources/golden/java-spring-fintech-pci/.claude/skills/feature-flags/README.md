# feature-flags

> Feature flags patterns: toggle types, lifecycle management, evaluation strategies, progressive delivery, cleanup policies, and hexagonal architecture integration.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-task-implement, x-story-implement, x-arch-plan, x-review, architect agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Toggle types: release, experiment, ops, permission toggles with lifecycle rules
- Toggle lifecycle: creation, testing, rollout (percentage-based), cleanup
- Evaluation strategies: server-side vs client-side, caching, context-based targeting
- Feature flag frameworks: OpenFeature (recommended), Unleash, LaunchDarkly, Flagsmith
- Progressive delivery: canary deployments, blue-green with flags, ring deployments, dark launches
- Hexagonal architecture integration: FeatureFlagPort in domain, adapter implementation
- Cleanup policies: stale flag detection, automated reminders, code removal strategy, audit log
- Anti-patterns: nested flags, flag-driven domain branching, permanent flags without expiry

## Key Concepts

This pack covers the complete feature flag lifecycle from creation through cleanup, with four toggle types each having distinct lifetimes and removal triggers. Evaluation strategies balance freshness against performance with server-side (consistent, secure) and client-side (fast, reduced load) options. The hexagonal architecture integration ensures domain purity by defining a FeatureFlagPort interface in the domain layer with concrete framework adapters in the outbound adapter layer. Progressive delivery patterns (canary, blue-green, ring, dark launches) enable safe production rollouts with instant rollback capability. Stale flag detection and automated cleanup enforcement prevent technical debt accumulation.

## See Also

- [ci-cd-patterns](../ci-cd-patterns/) — Pipeline integration for progressive delivery
- [architecture-hexagonal](../architecture-hexagonal/) — Port/Adapter patterns for flag integration
- [coding-standards](../coding-standards/) — Clean Code principles for flag implementation
