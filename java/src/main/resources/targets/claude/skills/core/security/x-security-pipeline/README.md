# x-security-pipeline

> Generates CI/CD pipeline configurations with conditional security stages based on SecurityConfig flags. Supports GitHub Actions, GitLab CI, and Azure DevOps with minimal and full stage modes.

| | |
|---|---|
| **Category** | Security |
| **Invocation** | `/x-security-pipeline [--ci github\|gitlab\|azure] [--stages all\|minimal] [--trigger push\|pr\|schedule] [--fail-on-findings true\|false] [--severity-threshold CRITICAL\|HIGH\|MEDIUM]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Generates CI/CD pipeline configuration files with up to 9 conditional security stages. Each stage is included only when its corresponding SecurityConfig flag is enabled. Supports three CI platforms (GitHub Actions, GitLab CI, Azure DevOps), two stage modes (minimal with 3 stages, full with 9 stages), and configurable severity thresholds for pipeline failure. References atomic scanning skills instead of duplicating scan logic.

## Usage

```
/x-security-pipeline
/x-security-pipeline --ci gitlab --stages minimal
/x-security-pipeline --ci azure --trigger schedule
/x-security-pipeline --fail-on-findings false --severity-threshold CRITICAL
```

## Workflow

1. Read SecurityConfig flags and infrastructure settings from project configuration
2. Evaluate stage conditions against config flags
3. Filter stages by mode (minimal/all) and condition results
4. Render platform-specific YAML from stage definitions
5. Validate generated YAML structure (actionlint/yamllint)
6. Write pipeline file to platform-specific path
7. Summarize enabled/disabled stages in generation report

## Outputs

| Artifact | Path |
|----------|------|
| GitHub Actions | `.github/workflows/security.yml` |
| GitLab CI | `.gitlab-ci-security.yml` |
| Azure DevOps | `azure-pipelines-security.yml` |

## See Also

- [x-owasp-scan](../x-owasp-scan/) -- OWASP Top 10 verification (referenced as pipeline stage)
- [x-dependency-audit](../x-dependency-audit/) -- Dependency audit (always-enabled baseline stage)
- [x-hardening-eval](../x-hardening-eval/) -- Hardening evaluation (optional pipeline stage)
- [x-security-dashboard](../x-security-dashboard/) -- Aggregated security posture view from scan results
