# Supply Chain Hardening Guide

## Overview

Supply chain hardening protects the integrity of software from source code through build, distribution, and deployment. This guide covers SLSA framework compliance, artifact signing with Sigstore, provenance attestation, and dependency integrity verification.

## SLSA Framework

Supply-chain Levels for Software Artifacts (SLSA, pronounced "salsa") is a security framework for increasing supply chain integrity.

### Level 1 — Build Provenance

**Requirements:**
- Automated build process (no manual builds for releases)
- Build provenance document generated for every artifact
- Provenance includes: builder identity, source repo, build instructions

**Implementation:**
- Use CI/CD system (GitHub Actions, GitLab CI, Jenkins) for all builds
- Generate provenance metadata as part of the build pipeline
- Store provenance alongside artifacts

### Level 2 — Hosted Build

**Requirements:**
- Build runs on a hosted build service (not developer machines)
- Provenance is authenticated (signed by the build service)
- Source and build configuration are version-controlled

**Implementation:**
- Use hosted CI runners (GitHub-hosted, GitLab SaaS runners)
- Enable provenance signing via OIDC identity
- Store build configuration in the repository (no out-of-band config)

### Level 3 — Hardened Builds

**Requirements:**
- Hardened build platform with isolation between builds
- Non-falsifiable provenance (build service generates, not user)
- Ephemeral build environments (fresh environment per build)
- Isolated builds (no shared state between build jobs)

**Implementation:**
- Use ephemeral runners (auto-scaling, destroyed after use)
- Disable self-hosted runners for production builds
- Use SLSA GitHub Generator or equivalent for provenance

### Level 4 — Two-Party Review

**Requirements:**
- Two-person review of all source changes before build
- Hermetic builds (no network access during build)
- Reproducible builds (same inputs produce identical outputs)
- All build inputs are fully declared and version-locked

**Implementation:**
- Enforce branch protection with minimum 2 reviewers
- Use hermetic build systems (Bazel, Buck2)
- Verify build reproducibility in CI
- Pin all build tool versions

## Sigstore Setup

Sigstore provides keyless signing and verification for software artifacts.

### Components

| Component | Purpose |
|-----------|---------|
| cosign | Sign and verify container images and blobs |
| Rekor | Transparency log for signatures |
| Fulcio | Certificate authority for keyless signing |

### Container Image Signing

```bash
# Install cosign
go install github.com/sigstore/cosign/v2/cmd/cosign@latest

# Sign a container image (keyless, uses OIDC)
cosign sign --yes ${IMAGE_REF}@${DIGEST}

# Verify a signature
cosign verify \
  --certificate-identity="https://github.com/org/repo/.github/workflows/release.yml@refs/tags/v1.0.0" \
  --certificate-oidc-issuer="https://token.actions.githubusercontent.com" \
  ${IMAGE_REF}@${DIGEST}
```

### GitHub Actions Integration

```yaml
permissions:
  id-token: write
  packages: write
  contents: read

steps:
  - name: Install cosign
    uses: sigstore/cosign-installer@v3

  - name: Sign container image
    run: cosign sign --yes ${{ env.IMAGE }}@${{ steps.build.outputs.digest }}

  - name: Attest SBOM
    run: |
      cosign attest --yes \
        --predicate sbom.json \
        --type cyclonedx \
        ${{ env.IMAGE }}@${{ steps.build.outputs.digest }}
```

### Binary Signing

```bash
# Sign a binary blob
cosign sign-blob --yes --output-signature=binary.sig binary

# Verify
cosign verify-blob \
  --certificate-identity="..." \
  --certificate-oidc-issuer="..." \
  --signature=binary.sig binary
```

## Provenance Attestation

### in-toto Attestation Format

```json
{
  "_type": "https://in-toto.io/Statement/v1",
  "subject": [
    {
      "name": "my-app",
      "digest": { "sha256": "abc123..." }
    }
  ],
  "predicateType": "https://slsa.dev/provenance/v1",
  "predicate": {
    "buildDefinition": {
      "buildType": "https://github.com/actions/runner",
      "externalParameters": {
        "workflow": ".github/workflows/release.yml"
      }
    },
    "runDetails": {
      "builder": {
        "id": "https://github.com/actions/runner"
      },
      "metadata": {
        "invocationId": "https://github.com/org/repo/actions/runs/123"
      }
    }
  }
}
```

### SLSA GitHub Generator

```yaml
provenance:
  needs: build
  permissions:
    actions: read
    id-token: write
    contents: write
  uses: slsa-framework/slsa-github-generator/.github/workflows/generator_generic_slsa3.yml@v2.1.0
  with:
    base64-subjects: ${{ needs.build.outputs.digest }}
```

## Dependency Integrity Verification

### Lock File Verification

| Build Tool | Verification Command |
|-----------|---------------------|
| npm | `npm ci` (fails if lock file mismatch) |
| pnpm | `pnpm install --frozen-lockfile` |
| yarn | `yarn install --frozen-lockfile` |
| Maven | `mvn dependency:resolve --fail-at-end` |
| Gradle | `gradle --write-locks` then verify |
| Go | `go mod verify` |
| Cargo | `cargo verify-project` |
| pip | `pip install --require-hashes -r requirements.txt` |

### Hash Pinning

Where supported, pin dependency hashes for integrity verification:

```
# npm (package-lock.json includes integrity hashes automatically)
# pip (requirements.txt with hashes)
flask==3.1.0 --hash=sha256:abc123...

# Go (go.sum contains hashes for all modules)
# Cargo (Cargo.lock includes checksums)
```

## Continuous Monitoring

| Practice | Frequency | Tool |
|----------|-----------|------|
| Dependency vulnerability scan | Every PR, daily on main | Dependabot, Renovate, Snyk |
| SBOM generation | Every release, every container build | CycloneDX, SPDX |
| Provenance verification | Every deployment | cosign verify |
| Lock file audit | Every PR | CI pipeline check |
| License compliance check | Every PR, weekly on main | FOSSA, license-checker |
