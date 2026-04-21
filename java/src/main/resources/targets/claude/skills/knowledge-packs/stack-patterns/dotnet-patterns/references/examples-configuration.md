# Example: Configuration

### appsettings.json Structure

```json
{
  "Server": {
    "Urls": "http://0.0.0.0:8080"
  },
  "Database": {
    "ConnectionString": "Host=localhost;Database=mydb;Username=postgres;Password=secret",
    "MaxPoolSize": 25,
    "CommandTimeout": 30
  },
  "Auth": {
    "JwtSecret": "change-me-in-production",
    "Issuer": "example.com",
    "TokenExpirySecs": 86400
  },
  "Features": {
    "RequireApproval": false,
    "MaxMerchantsPerAccount": 100
  }
}
```

### Environment-Specific Overrides

```json
// appsettings.Development.json
{
  "Database": {
    "ConnectionString": "Host=localhost;Database=mydb_dev;Username=postgres;Password=dev"
  },
  "Logging": {
    "LogLevel": {
      "Default": "Debug"
    }
  }
}

// appsettings.Production.json
{
  "Database": {
    "MaxPoolSize": 50
  },
  "Logging": {
    "LogLevel": {
      "Default": "Warning"
    }
  }
}
```

### IConfiguration Direct Access

```csharp
// For simple one-off values (prefer IOptions<T> for groups)
var connectionString = builder.Configuration.GetConnectionString("Default");
var jwtSecret = builder.Configuration["Auth:JwtSecret"];
```

### User Secrets (Development)

```bash
# Initialize user secrets
dotnet user-secrets init

# Set secrets
dotnet user-secrets set "Database:ConnectionString" "Host=localhost;Database=mydb;Username=admin;Password=real-secret"
dotnet user-secrets set "Auth:JwtSecret" "my-dev-secret-key"
```

### Environment Variable Override

```bash
# Double underscore replaces colon for nested config
export Database__ConnectionString="Host=prod-db;Database=mydb;Username=app;Password=prod-secret"
export Auth__JwtSecret="production-jwt-secret"
```
