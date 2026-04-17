# Pre-flight Conflict Analysis — Phase 0

## File Overlap Matrix

No implementation plan files exist for Phase 0 stories (foundation phase).
Artifact analysis from IMPLEMENTATION-MAP.md Section 6 used as proxy:

| Story A | Story B | Overlapping Files | Classification |
|---------|---------|-------------------|----------------|
| story-0022-0001 | story-0022-0002 | — | no-overlap |
| story-0022-0001 | story-0022-0004 | — | no-overlap |
| story-0022-0001 | story-0022-0016 | — | no-overlap |
| story-0022-0001 | story-0022-0017 | — | no-overlap |
| story-0022-0001 | story-0022-0025 | — | no-overlap |
| story-0022-0001 | story-0022-0027 | — | no-overlap |
| story-0022-0002 | story-0022-0004 | — | no-overlap |
| story-0022-0002 | story-0022-0016 | — | no-overlap |
| story-0022-0002 | story-0022-0017 | — | no-overlap |
| story-0022-0002 | story-0022-0025 | — | no-overlap |
| story-0022-0002 | story-0022-0027 | — | no-overlap |
| story-0022-0004 | story-0022-0016 | — | no-overlap |
| story-0022-0004 | story-0022-0017 | — | no-overlap |
| story-0022-0004 | story-0022-0025 | — | no-overlap |
| story-0022-0004 | story-0022-0027 | — | no-overlap |
| story-0022-0016 | story-0022-0017 | — | no-overlap |
| story-0022-0016 | story-0022-0025 | — | no-overlap |
| story-0022-0016 | story-0022-0027 | — | no-overlap |
| story-0022-0017 | story-0022-0025 | — | no-overlap |
| story-0022-0017 | story-0022-0027 | — | no-overlap |
| story-0022-0025 | story-0022-0027 | — | no-overlap |

## Adjusted Execution Plan

### Parallel Batch
- story-0022-0001 (Config Model — java/src/.../SecurityConfig.java, ScanningConfig.java, QualityGateConfig.java, SkillsSelection.java)
- story-0022-0002 (SARIF+Scoring — security/references/sarif-template.md, security-scoring.md)
- story-0022-0004 (ASVS KP — knowledge-packs/owasp-asvs/)
- story-0022-0016 (AppSec Agent — agents/conditional/appsec-engineer.md)
- story-0022-0017 (DevSecOps Agent — agents/conditional/devsecops-engineer.md)
- story-0022-0025 (Crypto Ref — security/references/cryptography.md)
- story-0022-0027 (Anti-Patterns — rules/conditional/12-security-anti-patterns.md)

### Sequential Queue (after parallel batch)
(none — all stories classified as no-overlap)

## Warnings
- No implementation plan files found for any Phase 0 story
- Artifact analysis derived from implementation-map-0022.md Section 6 (Detalhamento por Fase)
- All 7 stories operate on distinct, non-overlapping file sets — parallel dispatch safe
