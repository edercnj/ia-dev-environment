# OpenFeature Setup Guide

## Overview

OpenFeature provides a vendor-neutral, community-driven API for feature flag evaluation. The provider pattern allows swapping backends (Unleash, LaunchDarkly, Flagsmith) without changing application code.

## Architecture

```
Application Code
    |
    v
OpenFeature API (vendor-neutral)
    |
    v
Provider (pluggable backend)
    |
    v
Flag Management System (Unleash, LaunchDarkly, Flagsmith, etc.)
```

## SDK Initialization

### Java

```java
// Add dependency: dev.openfeature:sdk
OpenFeatureAPI api = OpenFeatureAPI.getInstance();
api.setProviderAndWait(new UnleashProvider(unleashConfig));

Client client = api.getClient();
boolean enabled = client.getBooleanValue("release.checkout-v2.enabled", false);
```

### Go

```go
// Add dependency: github.com/open-feature/go-sdk
openfeature.SetProvider(unleashProvider)

client := openfeature.NewClient("my-app")
enabled, _ := client.BooleanValue(ctx, "release.checkout-v2.enabled", false, openfeature.EvaluationContext{})
```

### TypeScript / JavaScript

```typescript
// Add dependency: @openfeature/server-sdk
OpenFeature.setProvider(new UnleashProvider(config));

const client = OpenFeature.getClient();
const enabled = await client.getBooleanValue('release.checkout-v2.enabled', false);
```

### Python

```python
# Add dependency: openfeature-sdk
from openfeature import api

api.set_provider(UnleashProvider(config))
client = api.get_client()
enabled = client.get_boolean_value("release.checkout-v2.enabled", False)
```

### Rust

```rust
// Add dependency: open-feature
let mut api = OpenFeature::singleton_mut().await;
api.set_provider(UnleashProvider::new(config)).await;

let client = api.create_client();
let enabled = client.get_bool_value("release.checkout-v2.enabled", None, None).await;
```

## Evaluation Context

The evaluation context carries targeting attributes for flag evaluation:

```java
EvaluationContext context = new ImmutableContext();
context.add("userId", new Value("user-123"));
context.add("email", new Value("user@example.com"));
context.add("tier", new Value("premium"));
context.add("environment", new Value("production"));

boolean enabled = client.getBooleanValue(
    "experiment.new-pricing.variant-a",
    false,
    context
);
```

### Context Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| `userId` | String | Unique user identifier for sticky targeting |
| `email` | String | User email for domain-based targeting |
| `tier` | String | Subscription tier (free, basic, premium) |
| `environment` | String | Deployment environment (dev, staging, prod) |
| `country` | String | ISO country code for geo-targeting |
| `version` | String | Application version for version-gating |

## Hooks

Hooks execute before/after flag evaluation for cross-cutting concerns:

### Logging Hook

```java
public class LoggingHook implements Hook {
    @Override
    public void after(HookContext ctx, FlagEvaluationDetails details, Map hints) {
        logger.info("Flag {} evaluated to {} for user {}",
            ctx.getFlagKey(),
            details.getValue(),
            ctx.getCtx().getTargetingKey());
    }
}

// Register globally
api.addHooks(new LoggingHook());
```

### Metrics Hook

```java
public class MetricsHook implements Hook {
    @Override
    public void after(HookContext ctx, FlagEvaluationDetails details, Map hints) {
        metrics.counter("feature_flag_evaluation",
            Tags.of("flag", ctx.getFlagKey(),
                     "value", String.valueOf(details.getValue())));
    }
}
```

## Error Handling

```java
try {
    boolean enabled = client.getBooleanValue("release.feature-x.enabled", false);
} catch (OpenFeatureError e) {
    // Default value returned automatically on error
    // Log error for observability
    logger.warn("Flag evaluation failed: {}", e.getMessage());
}
```

### Error Codes

| Code | Description | Action |
|------|-------------|--------|
| `PROVIDER_NOT_READY` | Provider not initialized | Use default value, log warning |
| `FLAG_NOT_FOUND` | Flag key does not exist | Use default value, check flag name |
| `TYPE_MISMATCH` | Expected type differs from flag type | Fix flag configuration |
| `GENERAL` | Unexpected error | Use default value, investigate |

## Hexagonal Architecture Integration

### Domain Port

```java
// domain/port/FeatureFlagPort.java
public interface FeatureFlagPort {
    boolean isEnabled(String flagName, Map<String, String> context);
}
```

### Adapter Implementation

```java
// adapter/outbound/OpenFeatureAdapter.java
public class OpenFeatureAdapter implements FeatureFlagPort {
    private final Client client;

    public OpenFeatureAdapter(Client client) {
        this.client = client;
    }

    @Override
    public boolean isEnabled(String flagName, Map<String, String> context) {
        EvaluationContext evalCtx = buildContext(context);
        return client.getBooleanValue(flagName, false, evalCtx);
    }
}
```

### Test Adapter

```java
// test/InMemoryFeatureFlagAdapter.java
public class InMemoryFeatureFlagAdapter implements FeatureFlagPort {
    private final Map<String, Boolean> flags = new HashMap<>();

    public void setFlag(String name, boolean enabled) {
        flags.put(name, enabled);
    }

    @Override
    public boolean isEnabled(String flagName, Map<String, String> context) {
        return flags.getOrDefault(flagName, false);
    }
}
```

## Testing Strategies

### Unit Tests (In-Memory Adapter)

```java
@Test
void processOrder_featureEnabled_usesNewFlow() {
    InMemoryFeatureFlagAdapter flags = new InMemoryFeatureFlagAdapter();
    flags.setFlag("release.checkout-v2.enabled", true);

    OrderService service = new OrderService(flags);
    OrderResult result = service.processOrder(order);

    assertThat(result.flow()).isEqualTo("v2");
}

@Test
void processOrder_featureDisabled_usesLegacyFlow() {
    InMemoryFeatureFlagAdapter flags = new InMemoryFeatureFlagAdapter();
    flags.setFlag("release.checkout-v2.enabled", false);

    OrderService service = new OrderService(flags);
    OrderResult result = service.processOrder(order);

    assertThat(result.flow()).isEqualTo("v1");
}
```

### Integration Tests (Real Provider)

```java
@Test
void adapter_evaluatesFlag_withRealProvider() {
    // Use test container or test provider
    OpenFeatureAdapter adapter = new OpenFeatureAdapter(testClient);

    boolean result = adapter.isEnabled(
        "release.checkout-v2.enabled",
        Map.of("userId", "test-user"));

    assertThat(result).isFalse(); // default value
}
```

## Provider Comparison

| Feature | OpenFeature | Unleash | LaunchDarkly | Flagsmith |
|---------|------------|---------|--------------|-----------|
| Vendor Lock-in | None | Low | High | Low |
| Self-hosted | Via provider | Yes | No | Yes |
| Pricing | Free (API) | Free/Paid | Paid | Free/Paid |
| SDK Languages | 7+ | 15+ | 25+ | 10+ |
| Local Evaluation | Via provider | Yes | Yes | Yes |
| A/B Testing | Via provider | Basic | Advanced | Basic |
