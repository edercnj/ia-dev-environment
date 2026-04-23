# Artifact Publishing Matrix

## Registry Configuration Per Language

### Java (Maven Central via Sonatype OSSRH)

**Prerequisites:**
- Sonatype OSSRH account with namespace (group ID) claimed
- GPG key for artifact signing
- `settings.xml` with OSSRH credentials

**Publish Commands:**

```bash
# Deploy snapshot
mvn deploy -DaltDeploymentRepository=ossrh::https://s01.oss.sonatype.org/content/repositories/snapshots

# Deploy release (stages to OSSRH)
mvn deploy -P release

# Release from staging
mvn nexus-staging:release -DstagingRepositoryId=<id>
```

**CI Integration (GitHub Actions):**

```yaml
- name: Publish to Maven Central
  run: mvn deploy -P release
  env:
    MAVEN_USERNAME: ${{ secrets.OSSRH_USERNAME }}
    MAVEN_PASSWORD: ${{ secrets.OSSRH_TOKEN }}
    MAVEN_GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
```

### TypeScript / JavaScript (npm)

**Prerequisites:**
- npm account with publish access
- Scoped package configured (optional)
- Access token with publish permission

**Publish Commands:**

```bash
# Publish release
npm publish

# Publish scoped package
npm publish --access public

# Publish pre-release
npm publish --tag beta

# Publish with provenance (npm >= 9.5)
npm publish --provenance
```

**CI Integration (GitHub Actions):**

```yaml
- name: Publish to npm
  run: npm publish --provenance
  env:
    NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}
```

### Python (PyPI)

**Prerequisites:**
- PyPI account or trusted publisher configured
- `twine` installed for upload
- `build` for creating distributions

**Publish Commands:**

```bash
# Build distributions
python -m build

# Upload to PyPI
twine upload dist/*

# Upload to Test PyPI
twine upload --repository testpypi dist/*
```

**CI Integration (GitHub Actions):**

```yaml
- name: Publish to PyPI
  uses: pypa/gh-action-pypi-publish@release/v1
  with:
    password: ${{ secrets.PYPI_API_TOKEN }}
```

### Rust (crates.io)

**Prerequisites:**
- crates.io account with API token
- `Cargo.toml` metadata complete (description, license, repository)

**Publish Commands:**

```bash
# Dry run (verify before publish)
cargo publish --dry-run

# Publish to crates.io
cargo publish

# Yank a version (mark as not recommended)
cargo yank --version 1.0.0
```

**CI Integration (GitHub Actions):**

```yaml
- name: Publish to crates.io
  run: cargo publish
  env:
    CARGO_REGISTRY_TOKEN: ${{ secrets.CRATES_TOKEN }}
```

### Go (Go Module Proxy)

**Prerequisites:**
- Module path matches repository URL
- Semantic version tags (e.g., `v1.2.3`)

**Publish Commands:**

```bash
# Tag and push (Go modules use git tags)
git tag v1.2.3
git push origin v1.2.3

# Verify module is available
GOPROXY=proxy.golang.org go list -m example.com/module@v1.2.3
```

**Note:** Go modules are published via git tags. The Go module proxy automatically indexes tagged versions.

## Container Image Publishing

### Docker Hub

```bash
# Build and tag
docker build -t org/image:v1.2.3 .
docker tag org/image:v1.2.3 org/image:latest

# Push
docker push org/image:v1.2.3
docker push org/image:latest
```

### GitHub Container Registry (GHCR)

```bash
# Login
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin

# Build and push
docker build -t ghcr.io/org/image:v1.2.3 .
docker push ghcr.io/org/image:v1.2.3
```

### Multi-Architecture Builds

```bash
# Create builder
docker buildx create --use

# Build and push multi-arch
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t org/image:v1.2.3 \
  --push .
```

## Version Tag Conventions

| Tag | Purpose | Mutable |
|-----|---------|---------|
| `v1.2.3` | Exact version | No (immutable) |
| `v1.2` | Latest patch in minor | Yes |
| `v1` | Latest minor in major | Yes |
| `latest` | Most recent stable | Yes |
| `sha-abc123` | Specific commit | No (immutable) |
| `v1.2.3-rc.1` | Release candidate | No (immutable) |

## Retention Policies

| Artifact Type | Keep | Expiry |
|--------------|------|--------|
| Release versions | All | Never (or per compliance) |
| Pre-release versions | Last 10 | 90 days |
| Snapshot/dev builds | Last 5 | 30 days |
| Container images (release) | All | Never |
| Container images (dev) | Last 10 | 14 days |
