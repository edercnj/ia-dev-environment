---
name: dotnet-patterns
description: ".NET patterns: dependency injection, Entity Framework Core, ASP.NET Core Web APIs, typed configuration, xUnit + WebApplicationFactory testing, and NativeAOT build."
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Pattern: .NET

## Purpose

Provides .NET (8/9) patterns for building ASP.NET Core services: constructor injection, EF Core with the repository pattern, Minimal APIs + MVC controllers, typed options, xUnit + WebApplicationFactory integration tests, and NativeAOT compilation. Agents reference this pack when producing C# service code, DI registrations, EF migrations, and endpoint handlers.

## Supplements

Supplements `architecture` and `layer-templates` knowledge packs with .NET-specific idioms (records, `IOptions<T>`, `IAsyncDisposable`).

## Stack Compatibility

- **.NET:** ≥ 8 (LTS) — NativeAOT examples target .NET 8+
- **ASP.NET Core:** ≥ 8.0
- **EF Core:** ≥ 8.0 (migrations, `DbContext` scoped lifetime)
- **xUnit:** ≥ 2.6
- **WebApplicationFactory:** `Microsoft.AspNetCore.Mvc.Testing` ≥ 8.0
- **Serilog / `ILogger<T>`:** structured logging via message templates

## Patterns Index

| Pattern | Use case | File |
| :--- | :--- | :--- |
| Dependency Injection | Constructor injection, service lifetimes (singleton / scoped / transient), `IServiceCollection` extensions | [`references/examples-dependency-injection.md`](references/examples-dependency-injection.md) |
| Entity Framework Core | `DbContext` configuration, migrations, repository pattern, `Include()` + projection | [`references/examples-entity-framework-core.md`](references/examples-entity-framework-core.md) |
| Web APIs | Minimal APIs vs controllers, model binding, `ProblemDetails` (RFC 7807), OpenAPI generation | [`references/examples-web-apis.md`](references/examples-web-apis.md) |
| Configuration | `IOptions<T>`, `IOptionsSnapshot<T>`, `IOptionsMonitor<T>`, env overlay, validation with `ValidateDataAnnotations` | [`references/examples-configuration.md`](references/examples-configuration.md) |
| Testing | xUnit, `WebApplicationFactory<TProgram>`, `IClassFixture`, in-memory EF provider, Testcontainers | [`references/examples-testing.md`](references/examples-testing.md) |
| NativeAOT | `PublishAot=true`, trimming-safe code, source generators, `JsonSerializerContext` | [`references/examples-native-aot.md`](references/examples-native-aot.md) |

## When to Open an Example File

Open a specific `references/examples-<pattern>.md` only when you are about to implement that pattern. The Patterns Index is enough to decide *which* file applies; each example file carries the complete C# snippets.

## References

All pattern examples live under `references/examples-*.md` next to this SKILL.md. The naming convention is `examples-<slug>.md` where `<slug>` matches the third column of the Patterns Index above.

## Anti-Patterns (.NET-Specific)

- **`async void`** — always use `async Task`; `async void` swallows exceptions and cannot be awaited, causing silent failures and crashes
- **`Task.Result` / `.Wait()` deadlocks** — never block on async code synchronously; this causes thread pool starvation and deadlocks in ASP.NET Core; always use `await`
- **EF Core lazy loading in APIs (N+1)** — avoid `virtual` navigation properties with lazy loading proxies; use `Include()` for eager loading or projection with `Select()`
- **Missing `IDisposable` / `IAsyncDisposable`** — types holding unmanaged resources or `DbContext` references must implement dispose; use `await using` for async disposal
- **Service Locator via `IServiceProvider`** — never inject `IServiceProvider` to resolve services manually; use constructor injection exclusively
- **`static` for stateful services** — static classes cannot participate in DI and make testing difficult; use scoped/singleton DI registrations instead
- **Missing `CancellationToken` propagation** — always accept and forward `CancellationToken` through async call chains to support request cancellation
- **`string` interpolation in log messages** — use structured logging with message templates: `_logger.LogInformation("Created merchant {Mid}", mid)` not `_logger.LogInformation($"Created merchant {mid}")`
- **Capturing `DbContext` in singletons** — `DbContext` is scoped; injecting it into a singleton causes it to outlive its intended scope and leads to concurrency issues
