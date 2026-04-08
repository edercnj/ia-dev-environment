---
name: security
description: "Complete security reference: OWASP Top 10, security headers, secrets management, input validation, cryptography (TLS, hashing, key management), and pentest readiness checklist. Read during security reviews or when implementing security-sensitive features."
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: Security

## Purpose

Provides comprehensive security guidelines covering application security, cryptography, and pentest readiness.

## Quick Reference (always in context)

See `rules/06-security-baseline.md` for secure defaults, forbidden patterns, and defensive coding requirements.

## Detailed References

| Reference | Content |
|-----------|---------|
| `references/security-principles.md` | Data classification, input validation, secure error handling, credentials storage |
| `references/application-security.md` | OWASP Top 10, CSP, security headers, secrets management, dependency security |
| `references/cryptography.md` | TLS requirements, cipher suites, hashing algorithms, key management, field-level encryption |
| `references/pentest-readiness.md` | Pre-pentest hardening checklist, common vulnerabilities, remediation (if enabled) |
| `references/sbom-generation-guide.md` | SBOM generation with CycloneDX and SPDX formats, tools by language, CI integration |
| `references/supply-chain-hardening.md` | SLSA framework levels, Sigstore/cosign setup, provenance attestation |
| `references/sarif-template.md` | SARIF 2.1.0 template with required fields, custom properties, and examples per severity |
| `references/security-scoring.md` | Security scoring model: formula, grades A-F, severity weights, output conventions |
| `references/security-skill-template.md` | Canonical structure for security scanning skills, CI integration snippets, error handling conventions |

## Supply Chain Security

### SBOM Generation

A Software Bill of Materials (SBOM) enumerates every component in a software artifact, enabling vulnerability tracking and license compliance at scale.

**Preferred Formats:**

| Format | Standard | Use Case |
|--------|----------|----------|
| CycloneDX | OWASP standard | Security-focused, supports VEX, services, and ML/AI components |
| SPDX | ISO/IEC 5962:2021 | License-focused, broad ecosystem adoption, ISO standard |

**Generation Tools by Language:**

| Language | CycloneDX Tool | SPDX Tool |
|----------|---------------|-----------|
| Java (Maven) | `cyclonedx-maven-plugin` | `spdx-maven-plugin` |
| Java (Gradle) | `cyclonedx-gradle-plugin` | `spdx-gradle-plugin` |
| Node.js | `@cyclonedx/cdxgen` | `spdx-sbom-generator` |
| Python | `cyclonedx-bom` | `spdx-sbom-generator` |
| Go | `cyclonedx-gomod` | `spdx-sbom-generator` |
| Rust | `cyclonedx-rust-cargo` | `spdx-sbom-generator` |
| .NET | `CycloneDX` (dotnet tool) | `spdx-sbom-generator` |

**CycloneDX output must include:**
- All direct dependencies with exact versions
- All transitive dependencies with dependency graph
- License information per component (SPDX identifier)
- Package URL (purl) for each component
- Hash digests (SHA-256 minimum) for integrity verification

### Artifact Signing

Sign all release artifacts to guarantee authenticity and integrity.

**Sigstore/cosign (recommended for container images and binaries):**
- Keyless signing with OIDC identity (GitHub Actions, GitLab CI)
- Signature stored alongside artifact in OCI registry
- Verification via `cosign verify` with certificate identity and issuer
- Supports signing container images, blobs, and in-toto attestations

**Signing requirements:**
- All container images MUST be signed before deployment
- All release binaries MUST be signed
- Signatures MUST be verified in deployment pipelines
- Key material MUST never be stored in source control

### SLSA Framework

Supply-chain Levels for Software Artifacts (SLSA) defines incremental security levels for build integrity.

| Level | Name | Requirements |
|-------|------|-------------|
| Level 1 | Build provenance | Automated build process, provenance document generated |
| Level 2 | Hosted build | Build runs on hosted service, authenticated provenance |
| Level 3 | Hardened builds | Hardened build platform, non-falsifiable provenance, isolated builds |
| Level 4 | Two-party review | Two-person review of all changes, hermetic and reproducible builds |

**Minimum target:** SLSA Level 2 for all production artifacts.

**Provenance attestation:**
- Use in-toto attestation format for build provenance
- Include: builder identity, source repository, build instructions, input/output digests
- Store provenance alongside artifacts in registry or transparency log

### Dependency Pinning

Lock file integrity is the first line of defense against supply chain attacks.

**Requirements:**
- All dependencies MUST be pinned to exact versions in lock files
- Lock files MUST be committed to version control
- CI MUST verify lock file integrity (reject unpinned or modified lock files)
- Hash pinning (integrity checksums) SHOULD be used where supported
- Reproducible builds: same source + same lock file = identical artifact

**CI enforcement:**
- Reject PRs that add dependencies without updating lock file
- Reject PRs with unpinned dependency ranges in production manifests
- Automated lock file refresh via Dependabot, Renovate, or equivalent

## Software Composition Analysis

### SCA Tools

Software Composition Analysis tools detect known vulnerabilities and license issues in third-party dependencies.

**Recommended tools by language:**

| Language | Primary Tool | Alternative |
|----------|-------------|-------------|
| Java / Kotlin | OWASP Dependency-Check | Snyk, Grype |
| JavaScript / TypeScript | npm audit, Snyk | Grype, Socket |
| Python | pip-audit, Safety | Snyk, Grype |
| Go | govulncheck | Snyk, Grype |
| Rust | cargo-audit | Snyk, Grype |
| .NET | dotnet list package --vulnerable | Snyk, Grype |
| Containers | Grype, Trivy | Snyk Container |

**CI integration requirements:**
- SCA scan MUST run on every PR and merge to main
- CRITICAL and HIGH vulnerabilities MUST block the build
- Results MUST be reported in SARIF format for GitHub Advanced Security integration
- Scan both application dependencies and base container images

### License Compliance

Track and enforce license compatibility across all dependencies.

**SPDX license identifiers** (use standardized identifiers):

| Category | Licenses | Risk |
|----------|----------|------|
| Permissive | MIT, Apache-2.0, BSD-2-Clause, BSD-3-Clause, ISC | Low |
| Weak copyleft | LGPL-2.1, LGPL-3.0, MPL-2.0, EPL-2.0 | Medium — review required |
| Strong copyleft | GPL-2.0, GPL-3.0, AGPL-3.0 | High — legal review required |
| Proprietary | Commercial, custom | High — explicit approval required |

**License compatibility enforcement:**
- Maintain an allow-list of approved licenses in CI configuration
- Block PRs introducing strong copyleft licenses in proprietary projects
- Generate license attribution report for every release
- Review weak copyleft licenses for linking compatibility

### Transitive Dependency Risk

Transitive dependencies are the hidden attack surface of modern software.

**Risk categories:**
- **Depth risk:** Dependencies deeper than 3 levels are harder to audit
- **Phantom dependencies:** Dependencies used at runtime but not declared directly
- **Typosquatting:** Malicious packages with names similar to popular libraries
- **Maintainer risk:** Single-maintainer packages with broad install base

**Mitigation strategies:**
- Limit transitive dependency depth where tooling supports it
- Audit new transitive dependencies introduced by version updates
- Use lock files to freeze the full dependency tree
- Monitor dependency health metrics (maintainer count, update frequency, known vulnerabilities)
- Prefer dependencies with active maintenance and security disclosure policies

## Related Knowledge Packs

- `skills/compliance/` — regulatory framework requirements (GDPR, HIPAA, PCI-DSS)
- `skills/infrastructure/` — container security, Kubernetes security context
- `skills/observability/` — security event logging and audit trail patterns
