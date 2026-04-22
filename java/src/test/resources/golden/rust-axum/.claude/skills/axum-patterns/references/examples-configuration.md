# Example: Configuration

### Layered Config with config Crate

```rust
use config::{Config, ConfigError, Environment, File};
use serde::Deserialize;

#[derive(Debug, Deserialize, Clone)]
pub struct AppConfig {
    pub server: ServerConfig,
    pub database: DatabaseConfig,
    pub auth: AuthConfig,
    pub log: LogConfig,
}

#[derive(Debug, Deserialize, Clone)]
pub struct ServerConfig {
    pub host: String,
    pub port: u16,
}

#[derive(Debug, Deserialize, Clone)]
pub struct DatabaseConfig {
    pub url: String,
    pub max_connections: u32,
    pub min_connections: u32,
    pub acquire_timeout_secs: u64,
    pub idle_timeout_secs: u64,
}

#[derive(Debug, Deserialize, Clone)]
pub struct AuthConfig {
    pub jwt_secret: String,
    pub token_expiry_secs: u64,
}

#[derive(Debug, Deserialize, Clone)]
pub struct LogConfig {
    pub level: String,
    pub format: String,
}

impl AppConfig {
    pub fn load() -> Result<Self, ConfigError> {
        let run_mode = std::env::var("RUN_MODE").unwrap_or_else(|_| "dev".into());

        let config = Config::builder()
            // Start with default values
            .add_source(File::with_name("config/default"))
            // Layer environment-specific file
            .add_source(File::with_name(&format!("config/{}", run_mode)).required(false))
            // Override with environment variables (APP__SERVER__PORT -> server.port)
            .add_source(
                Environment::with_prefix("APP")
                    .separator("__")
                    .try_parsing(true),
            )
            .build()?;

        config.try_deserialize()
    }
}
```

### Default Config File (config/default.toml)

```toml
[server]
host = "0.0.0.0"
port = 8080

[database]
url = "postgres://user:pass@localhost:5432/mydb"
max_connections = 10
min_connections = 2
acquire_timeout_secs = 5
idle_timeout_secs = 300

[auth]
jwt_secret = "change-me"
token_expiry_secs = 86400

[log]
level = "info"
format = "json"
```

### dotenvy for .env Files

```rust
fn main() {
    // Load .env file if present (non-fatal if missing)
    dotenvy::dotenv().ok();

    let config = AppConfig::load().expect("failed to load configuration");

    // ...
}
```
