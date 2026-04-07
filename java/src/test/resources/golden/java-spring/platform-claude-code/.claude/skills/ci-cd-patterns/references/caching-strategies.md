# Caching Strategies

## Language-Specific Caching

### Java (Maven)

```yaml
- uses: actions/cache@v4
  with:
    path: ~/.m2/repository
    key: maven-${{ hashFiles('**/pom.xml') }}
    restore-keys: maven-
```

- Cache `~/.m2/repository` for dependency resolution
- Key on `pom.xml` hash for cache invalidation
- JaCoCo coverage reports cached separately
- Multi-module builds benefit from incremental compilation

### Java (Gradle)

```yaml
- uses: actions/cache@v4
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
    restore-keys: gradle-
```

- Cache `~/.gradle/caches` and `~/.gradle/wrapper`
- Enable Gradle build cache for incremental builds
- Use `--build-cache` flag in CI
- KSP annotation processing cached via Gradle cache

### Python (pip/poetry)

```yaml
- uses: actions/cache@v4
  with:
    path: ~/.cache/pip
    key: pip-${{ hashFiles('**/requirements*.txt') }}
    restore-keys: pip-

# Poetry
- uses: actions/cache@v4
  with:
    path: ~/.cache/pypoetry
    key: poetry-${{ hashFiles('**/poetry.lock') }}
```

- Cache `~/.cache/pip` or `~/.cache/pypoetry`
- Virtual environment caching for faster activation
- Tox environments cached per Python version
- Use `--no-deps` for deterministic installs

### Go

```yaml
- uses: actions/cache@v4
  with:
    path: |
      ~/go/pkg/mod
      ~/.cache/go-build
    key: go-${{ hashFiles('**/go.sum') }}
    restore-keys: go-
```

- Cache `~/go/pkg/mod` for module dependencies
- Cache `~/.cache/go-build` for compilation
- Cross-compilation matrix uses OS-specific cache keys
- Module proxy speeds up cold builds

### Rust (Cargo)

```yaml
- uses: actions/cache@v4
  with:
    path: |
      ~/.cargo/bin/
      ~/.cargo/registry/index/
      ~/.cargo/registry/cache/
      ~/.cargo/git/db/
      target/
    key: cargo-${{ hashFiles('**/Cargo.lock') }}
    restore-keys: cargo-
```

- Cache cargo registry and compiled dependencies
- Incremental compilation via `target/` cache
- Use `sccache` for distributed compilation cache
- Separate cache keys for debug vs release builds

### TypeScript (npm/pnpm)

```yaml
# npm
- uses: actions/cache@v4
  with:
    path: ~/.npm
    key: npm-${{ hashFiles('**/package-lock.json') }}

# pnpm
- uses: actions/cache@v4
  with:
    path: ~/.pnpm-store
    key: pnpm-${{ hashFiles('**/pnpm-lock.yaml') }}
```

- Cache `~/.npm` or `~/.pnpm-store` (not `node_modules`)
- Workspace builds share dependency cache
- Use `--frozen-lockfile` for deterministic installs
- Turbo cache for monorepo incremental builds

### Kotlin (Gradle)

- Same Gradle caching strategy as Java
- KSP annotation processing benefits from build cache
- Kotlin compiler daemon cached between builds
- Use `--parallel` for multi-module projects

### C# (.NET)

```yaml
- uses: actions/cache@v4
  with:
    path: ~/.nuget/packages
    key: nuget-${{ hashFiles('**/*.csproj', '**/packages.lock.json') }}
    restore-keys: nuget-
```

- Cache `~/.nuget/packages` for NuGet dependencies
- Multi-target framework builds cached per TFM
- Use `dotnet restore --locked-mode` for determinism

## Cache Key Strategies

| Strategy | Key Pattern | Use Case |
|----------|------------|----------|
| Exact match | `lang-${{ hashFiles('lockfile') }}` | Deterministic builds |
| Prefix restore | `lang-` | Partial cache hit |
| Branch-scoped | `lang-${{ github.ref }}-${{ hashFiles('lockfile') }}` | Branch isolation |
| OS-scoped | `lang-${{ runner.os }}-${{ hashFiles('lockfile') }}` | Matrix builds |

## Cache Invalidation Patterns

- Lockfile change: automatic via hash-based keys
- Major dependency update: clear cache manually
- Build tool upgrade: include tool version in key
- Periodic refresh: use date-based key suffix weekly

## Docker Layer Caching

```yaml
- uses: docker/build-push-action@v6
  with:
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

- Use GitHub Actions cache backend for Docker layers
- Multi-stage builds cache intermediate stages
- Base image layers shared across builds
- Use `--cache-from` for registry-based caching
