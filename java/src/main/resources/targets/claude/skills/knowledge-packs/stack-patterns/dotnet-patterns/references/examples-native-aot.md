# Example: NativeAOT

### PublishAot Configuration

```xml
<!-- In .csproj -->
<PropertyGroup>
    <PublishAot>true</PublishAot>
    <InvariantGlobalization>true</InvariantGlobalization>
</PropertyGroup>
```

### JSON Source Generators

```csharp
[JsonSerializable(typeof(MerchantResponse))]
[JsonSerializable(typeof(CreateMerchantRequest))]
[JsonSerializable(typeof(PaginatedResponse<MerchantResponse>))]
[JsonSerializable(typeof(ProblemDetails))]
[JsonSerializable(typeof(ValidationProblemDetails))]
[JsonSourceGenerationOptions(
    PropertyNamingPolicy = JsonKnownNamingPolicy.CamelCase,
    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]
public partial class AppJsonSerializerContext : JsonSerializerContext
{
}

// Registration
builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.TypeInfoResolverChain.Insert(0, AppJsonSerializerContext.Default);
});
```

### Trimming Annotations

```csharp
// Mark types that must survive trimming
[DynamicallyAccessedMembers(DynamicallyAccessedMemberTypes.All)]
public class MerchantEntity { /* ... */ }

// Suppress trimming warnings when reflection is intentional
[UnconditionalSuppressMessage("Trimming", "IL2026",
    Justification = "Type is preserved via JsonSerializable")]
public static MerchantResponse ToResponse(this Merchant merchant) { /* ... */ }
```

### Reflection-Free Patterns

```csharp
// INSTEAD OF: Activator.CreateInstance<T>()
// USE: factory delegates or explicit construction
builder.Services.AddScoped<IMerchantService>(sp =>
    new MerchantService(
        sp.GetRequiredService<IMerchantRepository>(),
        sp.GetRequiredService<ILogger<MerchantService>>(),
        sp.GetRequiredService<IOptionsSnapshot<FeatureOptions>>()));

// INSTEAD OF: Assembly scanning for validators
// USE: explicit registration
builder.Services.AddScoped<IValidator<CreateMerchantRequest>, CreateMerchantRequestValidator>();
```

### Build Commands

```bash
# Standard build
dotnet build

# Run in development
dotnet run --environment Development

# Publish NativeAOT
dotnet publish -c Release -r linux-x64

# Publish self-contained (non-AOT)
dotnet publish -c Release -r linux-x64 --self-contained
```
