---
name: feature-flags
description: >
  Knowledge Pack: Feature Flags -- Toggle types, lifecycle management, evaluation strategies,
  progressive delivery, cleanup policies, and hexagonal architecture integration for ia-dev-environment.
---

# Knowledge Pack: Feature Flags

## Summary

Feature flag patterns for ia-dev-environment using typescript 5 with commander.

### Toggle Types

- Release toggles: short-lived, binary (on/off), max age 2 sprints, removed after full rollout
- Experiment toggles: A/B testing, multivariate, data-driven decision, removed after conclusion
- Ops toggles: circuit breaker, kill switch, manual override, long-lived with documented justification
- Permission toggles: role-based access, premium features, lifecycle tied to product decisions

### Toggle Lifecycle

- Creation: define flag name, type, default value, owner, expiry date
- Testing: validate both flag-on and flag-off paths, ensure no coupling between flags
- Rollout: percentage-based (1% -> 10% -> 50% -> 100%), monitoring at each stage
- Cleanup: remove flag + dead code path, verify no references, max age enforcement per type

### Evaluation Strategies

- Server-side: consistent, secure, no client exposure of flag logic
- Client-side: faster, reduced network calls, requires flag data sync
- Caching: local cache with TTL, stale-while-revalidate, invalidation on change
- Context-based targeting: user attributes, environment, percentage rollout, segments
- Percentage rollout: sticky hashing (user ID + flag name), consistent bucket assignment

### Feature Flag Frameworks

- OpenFeature (RECOMMENDED): vendor-neutral API, provider pattern, hooks, evaluation context
- Unleash: open-source, self-hosted, activation strategies, client SDKs
- LaunchDarkly: SaaS, targeting rules, experiments, analytics
- Flagsmith: open-source, self-hosted or cloud, segment-based targeting

### Progressive Delivery

- Canary with flags: route traffic based on flag evaluation, percentage-based rollout
- Blue-green with flags: new version behind flag, instant rollback via toggle
- Ring deployments: internal -> beta -> general availability, per-ring monitoring
- Dark launches: deploy disabled, shadow traffic validation, test in production

### Integration with Architecture

- Hexagonal port: `FeatureFlagPort` interface in domain layer
- Domain NEVER depends on flag framework: adapter-layer integration only
- Port contract: `boolean isEnabled(String flagName, EvaluationContext context)`
- Adapter: wraps specific framework SDK in adapter/outbound package
- Testing: in-memory implementation for unit tests, no mock of flag framework

### Cleanup Policies

- Stale flag detection: automated scan for flags older than max age per type
- CI enforcement: fail build on expired flags, PR comment with cleanup instructions
- Code removal: flag eval + dead path + test fixtures in single PR
- Flag audit log: creation, modification, evaluation frequency, last evaluated

### Anti-Patterns

- Nested flags: flag depending on another flag (combinatorial explosion)
- Flag-driven branching in domain: use strategy pattern via port instead
- Permanent flags without expiry: all flags MUST have expiry or documented justification
- Testing only flagged-on path: both paths MUST be tested
- Flag naming without convention: use `{type}.{feature}.{variant}` format

## References

- `references/openfeature-setup.md` -- OpenFeature setup with provider pattern and SDK examples
- `references/progressive-delivery-patterns.md` -- Progressive delivery patterns with examples
