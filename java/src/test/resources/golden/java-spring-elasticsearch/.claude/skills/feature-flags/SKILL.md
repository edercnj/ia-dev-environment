---
name: feature-flags
description: "Feature flags patterns: toggle types, lifecycle management, evaluation strategies, progressive delivery, cleanup policies, and hexagonal architecture integration."
---

# Knowledge Pack: Feature Flags

## Purpose

Provides comprehensive feature flag patterns for {{LANGUAGE}} {{FRAMEWORK}} projects, enabling progressive delivery, toggle lifecycle management, evaluation strategies, cleanup policies, and hexagonal architecture integration. Covers toggle types (release, experiment, ops, permission), flag lifecycle (creation through cleanup), server-side and client-side evaluation, framework recommendations (OpenFeature, Unleash, LaunchDarkly, Flagsmith), and anti-patterns.

## Quick Reference (always in context)

See `references/openfeature-setup.md` for OpenFeature setup guide with provider pattern and `references/progressive-delivery-patterns.md` for progressive delivery patterns with examples.

## Detailed References

Read these files for comprehensive feature flags guidance:

| Reference | Content |
|-----------|---------|
| `references/openfeature-setup.md` | OpenFeature setup guide: provider pattern, SDK initialization, evaluation context, hooks, error handling, and testing strategies per language |
| `references/progressive-delivery-patterns.md` | Progressive delivery patterns: canary deployment with flags, blue-green with instant rollback, ring deployments, dark launches, and monitoring gates |

## Toggle Types

### Release Toggles

- Short-lived, binary (on/off), removed after full rollout
- Maximum age: 2 sprints
- Default value: off (disabled in production until rollout begins)
- Removal trigger: full rollout completed and verified stable
- Owner: feature team lead responsible for rollout

### Experiment Toggles

- A/B testing, multivariate support, data-driven decision making
- Lifetime: experiment duration (typically 1-4 weeks)
- Removal trigger: data-driven decision made and winner selected
- Variants: control group + one or more treatment groups
- Metrics: define success metrics before experiment starts

### Ops Toggles

- Circuit breaker, kill switch, manual override for operational control
- May be long-lived with documented justification and periodic review
- Default value: feature enabled (toggle disables for emergency)
- Use cases: graceful degradation, load shedding, dependency isolation
- Requires runbook documentation for on-call engineers

### Permission Toggles

- Role-based feature access, premium features, internal tools
- Lifecycle tied to product decisions (not technical debt)
- Targeting: user roles, subscription tiers, internal groups
- Evaluation: check user context attributes against targeting rules
- Removal: only when feature becomes universally available or deprecated

## Toggle Lifecycle

### Creation

- Define: flag name, type, default value, owner, expiry date
- Naming convention: `{type}.{feature}.{variant}` (e.g., `release.checkout-v2.enabled`)
- Register in flag management system with metadata
- Create corresponding feature branch and test fixtures

### Testing

- Validate both flag-on and flag-off code paths
- Ensure no coupling between flags (no nested flag evaluation)
- Unit tests: in-memory flag adapter with explicit state
- Integration tests: verify behavior with flag toggled in each state

### Rollout

- Percentage-based rollout: 1% -> 10% -> 50% -> 100%
- Monitor error rates, latency, and business metrics at each stage
- Minimum soak time at each percentage before advancing
- Rollback plan: toggle flag off immediately if regression detected

### Cleanup

- Remove flag evaluation code and dead code path
- Remove test fixtures specific to the flag
- Verify no references remain (grep for flag name across codebase)
- Maximum age enforcement per toggle type (CI check)
- Single PR for complete flag removal

## Evaluation Strategies

### Server-Side Evaluation

- Consistent evaluation across all clients
- Secure: flag logic and targeting rules not exposed to clients
- Single source of truth for flag state
- Higher network latency for client applications

### Client-Side Evaluation

- Faster evaluation: no network round-trip per check
- Reduced server load for high-frequency flag checks
- Requires flag data synchronization (polling or streaming)
- Risk: stale flag state during sync intervals

### Caching

- Local cache with TTL: balance freshness vs performance
- Stale-while-revalidate: serve cached value while fetching update
- Cache invalidation on flag change via webhook or streaming
- Cache key: flag name + evaluation context hash

### Context-Based Targeting

- User attributes: ID, email, role, subscription tier
- Environment: production, staging, development
- Percentage rollout: sticky hashing (user ID + flag name)
- User segment targeting: predefined groups (beta testers, internal)
- Consistent bucket assignment: same user always gets same variant

## Feature Flag Frameworks

### OpenFeature (RECOMMENDED)

- Vendor-neutral API: standard interface, swap providers without code changes
- Provider pattern: pluggable backends (Unleash, LaunchDarkly, Flagsmith)
- Hooks: before/after evaluation for logging, metrics, validation
- Evaluation context: typed context object for targeting rules
- SDK availability: Java, Go, JavaScript, Python, .NET, Rust, PHP

### Unleash

- Open-source, self-hosted option available
- Activation strategies: gradual rollout, user ID, IP, custom
- Client SDKs with local evaluation and periodic sync
- Admin API for programmatic flag management

### LaunchDarkly

- SaaS platform with enterprise features
- Targeting rules: multi-condition targeting, scheduling
- Experiments: A/B testing with statistical analysis
- Analytics: flag evaluation metrics and impact analysis

### Flagsmith

- Open-source with cloud option
- Segment-based targeting with flexible rules
- Remote config: key-value configuration alongside flags
- Audit log: full history of flag changes

## Progressive Delivery

### Canary Deployments with Flags

- Route traffic based on flag evaluation result
- Percentage-based: 1% canary, monitor, increase gradually
- Automatic rollback: toggle flag off on error rate threshold
- Metrics comparison: canary vs baseline for latency, errors

### Blue-Green with Flags

- New version behind feature flag, both versions deployed
- Instant rollback: toggle flag off reverts to old behavior
- Zero-downtime transition: no deployment needed for rollback

### Ring Deployments

- Concentric rings: internal -> beta -> general availability
- Each ring maps to a user segment in flag targeting
- Progressive expansion with monitoring gates between rings

### Dark Launches

- Deploy feature to production disabled (flag off)
- Test in production without user impact
- Shadow traffic: duplicate requests to new code path for validation
- Measure performance and correctness before enabling

## Integration with Architecture

### Hexagonal Port for Feature Flags

- Domain port interface: `FeatureFlagPort` in domain layer
- Port contract: `boolean isEnabled(String flagName, EvaluationContext context)`
- Domain NEVER depends on flag framework: adapter-layer integration only
- Use case layer calls port interface, not concrete implementation

### Adapter Implementation

- Wraps specific framework SDK (OpenFeature, Unleash, etc.)
- Lives in `adapter/outbound/` package
- Implements `FeatureFlagPort` interface from domain
- Handles SDK initialization, caching, and error handling

### Testing

- In-memory implementation for unit tests: `InMemoryFeatureFlagAdapter`
- No mock of flag framework in domain tests
- Test both flag-on and flag-off paths in every test scenario
- Integration tests: verify adapter behavior with real SDK (test container)

## Cleanup Policies

### Stale Flag Detection

- Automated scan for flags older than maximum age per type
- CI pipeline check: fail build on expired flags
- Dashboard: flag inventory with age, type, owner, last evaluated

### Automated Cleanup Reminders

- CI check for expired flags: PR comment with cleanup instructions
- Scheduled job: weekly report of flags approaching expiry
- Owner notification: email or Slack alert for stale flags

### Code Removal Strategy

- Remove flag evaluation + dead code path + test fixtures in single PR
- Verify no references remain: grep codebase for flag name
- Update documentation: remove flag from feature docs
- Archive flag in management system (do not delete for audit trail)

### Flag Audit Log

- Track: creation, modification, evaluation frequency, last evaluated
- Retention: keep audit log for compliance period (minimum 1 year)
- Alerting: flag unused for 30 days triggers cleanup review

## Anti-Patterns

### Nested Flags

- Flag evaluation depending on another flag's result
- Causes combinatorial explosion of test scenarios
- Fix: flatten flag logic, one flag per feature decision

### Flag-Driven Branching in Domain Logic

- Business rules should not branch on flags directly
- Fix: use strategy pattern, inject behavior via port
- Domain receives behavior, not flag state

### Permanent Flags Without Expiry

- All flags MUST have an expiry date or documented justification
- CI enforcement: reject flag creation without expiry metadata
- Exception: ops toggles with documented review schedule

### Testing Only Flagged-On Path

- Both flag-on and flag-off paths MUST be tested
- Coverage gate: flag-related code must have branch coverage >= 90%
- Test matrix: enumerate all flag combinations for critical paths

### Flag Naming Without Convention

- Use format: `{type}.{feature}.{variant}` (e.g., `release.checkout-v2.enabled`)
- Enforce naming via validation in flag management system
- Reject non-conforming names in CI pipeline

## Related Knowledge Packs

- `skills/architecture/` — hexagonal port pattern for feature flag integration
- `skills/release-management/` — release branching and rollback strategies that use feature flags
- `skills/ci-cd-patterns/` — CI/CD pipeline integration for flag lifecycle management
