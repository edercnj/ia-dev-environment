# release-management

> Release management practices: semantic versioning, version lifecycle (alpha/beta/RC/GA/LTS/EOL), release branching strategies, artifact registry management, release signing and attestation, hotfix process, rollback procedures, and release communication.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | `x-release`, `x-git-push`, `x-changelog`, `tech-lead` agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Semantic versioning (MAJOR.MINOR.PATCH rules, pre-release identifiers, build metadata)
- Version lifecycle phases (Alpha, Beta, RC, GA, LTS, EOL) with transition criteria
- Release branching strategies (GitFlow, trunk-based, release branches)
- Artifact registry management (Maven Central, npm, PyPI, crates.io, container registries)
- Release signing and attestation (GPG, Sigstore/Cosign, SLSA provenance, GitHub attestations)
- Hotfix process (cherry-pick vs dedicated branch, version bumping rules)
- Rollback procedures (application, database, feature flags, decision criteria)
- Release communication (release notes, migration guides, deprecation notices)

## Key Concepts

This pack covers the complete release lifecycle from initial development through end-of-life, with strict SemVer rules and phase transition criteria. It recommends GitFlow as the default branching strategy with alternatives for specific team sizes and workflows. Artifact publishing patterns span multiple languages and container registries with signing and SLSA provenance requirements. The rollback section provides a decision matrix for choosing between rollback and fix-forward based on severity, blast radius, and time-to-fix.

## See Also

- [sre-practices](../sre-practices/) — Change management, canary analysis, and deployment velocity metrics
- [security](../security/) — Artifact signing, SBOM generation, and supply chain hardening
- [protocols](../protocols/) — API versioning strategies that impact release planning
