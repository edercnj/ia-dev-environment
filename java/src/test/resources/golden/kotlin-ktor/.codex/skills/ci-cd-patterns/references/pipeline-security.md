# Pipeline Security

## SLSA Compliance

### Supply Chain Levels

| Level | Requirements |
|-------|-------------|
| SLSA 1 | Build process documented, provenance generated |
| SLSA 2 | Hosted build service, authenticated provenance |
| SLSA 3 | Hardened build platform, non-falsifiable provenance |
| SLSA 4 | Hermetic and reproducible builds |

### Provenance Generation

```yaml
- uses: slsa-framework/slsa-github-generator/.github/workflows/generator_generic_slsa3.yml@v2.1.0
  with:
    base64-subjects: ${{ needs.build.outputs.digest }}
```

## Artifact Signing

- Sign container images with cosign (keyless via OIDC)
- Sign build artifacts with sigstore
- Verify signatures before deployment
- Store signatures alongside artifacts in registry

```yaml
- name: Sign container image
  run: cosign sign --yes ${{ env.IMAGE_REF }}@${{ steps.build.outputs.digest }}
```

## OIDC Authentication

- Use OIDC tokens instead of long-lived credentials
- Configure trust policies per environment
- Rotate OIDC provider certificates regularly
- Audit OIDC token claims in deployment logs

```yaml
permissions:
  id-token: write
  contents: read

- uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::role/deploy
    aws-region: us-east-1
```

## Least-Privilege Permissions

```yaml
permissions:
  contents: read
  packages: write
  id-token: write
  security-events: write
```

- Default to `permissions: {}` (no permissions)
- Grant only required permissions per job
- Use read-only checkout for build jobs
- Separate deploy permissions into dedicated jobs

## Dependency Pinning

- Pin all action versions to full SHA (not tags)
- Use Dependabot or Renovate for automated updates
- Verify action integrity via checksums
- Avoid using `@main` or `@latest` references

```yaml
# Pinned to SHA for security
- uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683
```

## Secret Scanning

- Enable GitHub secret scanning on all repositories
- Configure custom secret patterns for internal tokens
- Block pushes containing detected secrets
- Rotate compromised secrets immediately upon detection

## Dependency Scanning

```yaml
- name: Dependency review
  uses: actions/dependency-review-action@v4
  with:
    fail-on-severity: high
    deny-licenses: GPL-3.0

- name: SAST scan
  uses: github/codeql-action/analyze@v3
  with:
    languages: ${{ matrix.language }}
```

## Container Scanning

```yaml
- name: Scan container image
  uses: aquasecurity/trivy-action@0.28.0
  with:
    image-ref: ${{ env.IMAGE_REF }}
    format: sarif
    output: trivy-results.sarif
    severity: CRITICAL,HIGH
```
