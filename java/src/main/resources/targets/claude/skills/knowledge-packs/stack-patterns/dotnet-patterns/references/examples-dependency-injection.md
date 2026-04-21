# Example: Dependency Injection

### Built-In DI with IServiceCollection

```csharp
var builder = WebApplication.CreateBuilder(args);

// Scoped: one instance per HTTP request
builder.Services.AddScoped<IMerchantRepository, MerchantRepository>();
builder.Services.AddScoped<IMerchantService, MerchantService>();

// Singleton: one instance for application lifetime
builder.Services.AddSingleton<ITokenProvider, JwtTokenProvider>();

// Transient: new instance every time it is requested
builder.Services.AddTransient<IRequestValidator, RequestValidator>();

// Options pattern
builder.Services.Configure<DatabaseOptions>(
    builder.Configuration.GetSection("Database"));
builder.Services.Configure<AuthOptions>(
    builder.Configuration.GetSection("Auth"));

var app = builder.Build();
```

### Constructor Injection

```csharp
public class MerchantService : IMerchantService
{
    private readonly IMerchantRepository _repository;
    private readonly ILogger<MerchantService> _logger;
    private readonly IOptionsSnapshot<FeatureOptions> _featureOptions;

    public MerchantService(
        IMerchantRepository repository,
        ILogger<MerchantService> logger,
        IOptionsSnapshot<FeatureOptions> featureOptions)
    {
        _repository = repository;
        _logger = logger;
        _featureOptions = featureOptions;
    }

    public async Task<Merchant> CreateAsync(CreateMerchantCommand command, CancellationToken ct)
    {
        if (_featureOptions.Value.RequireApproval)
        {
            _logger.LogInformation("Merchant creation requires approval for MID {Mid}", command.Mid);
        }

        var merchant = Merchant.Create(command.Mid, command.Name, command.Type);
        await _repository.AddAsync(merchant, ct);
        return merchant;
    }
}
```

### IOptions Variants

| Interface | Lifetime | Reloads on Change | Use When |
|-----------|----------|-------------------|----------|
| `IOptions<T>` | Singleton | No | Static config, never changes |
| `IOptionsSnapshot<T>` | Scoped | Yes (per request) | Config that may change between requests |
| `IOptionsMonitor<T>` | Singleton | Yes (callback) | Singleton services needing live updates |

```csharp
public class FeatureOptions
{
    public const string SectionName = "Features";

    public bool RequireApproval { get; set; }
    public int MaxMerchantsPerAccount { get; set; } = 100;
    public TimeSpan CacheDuration { get; set; } = TimeSpan.FromMinutes(5);
}

// Registration
builder.Services.Configure<FeatureOptions>(
    builder.Configuration.GetSection(FeatureOptions.SectionName));

// Validation at startup
builder.Services.AddOptionsWithValidateOnStart<FeatureOptions>()
    .Bind(builder.Configuration.GetSection(FeatureOptions.SectionName))
    .ValidateDataAnnotations();
```

### FORBIDDEN

- **Service Locator pattern** — never inject `IServiceProvider` and call `GetService<T>()` manually; use constructor injection
- **`new` for DI-managed services** — never instantiate services with `new MerchantService(...)` when they should be resolved from the container
