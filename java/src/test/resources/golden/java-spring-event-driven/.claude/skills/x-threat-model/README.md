# x-threat-model

> Generate threat models using STRIDE analysis: identify components, map data flows, analyze threats per category, classify severity, suggest mitigations, and produce threat model document.

| | |
|---|---|
| **Category** | Security |
| **Invocation** | `/x-threat-model [architecture-plan-path] [--format stride\|pasta\|linddun] [--output results/security/]` |
| **Reads** | `skills/security/` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Generates automated threat models by analyzing the project architecture using STRIDE, PASTA, or LINDDUN methodologies. Identifies system components (services, databases, APIs, brokers), maps data flows and trust boundaries, evaluates all 6 STRIDE categories per component, classifies threats by severity, and suggests concrete mitigations referencing the security knowledge pack. Works with an architecture plan or falls back to codebase analysis.

## Usage

```
/x-threat-model
/x-threat-model steering/plan.md
/x-threat-model --format pasta
/x-threat-model --output results/security/
```

## Workflow

1. Read architecture plan or discover components from codebase (fallback)
2. Extract system components (services, databases, APIs, brokers, caches)
3. Map data flows, trust boundaries, and communication protocols
4. Apply STRIDE analysis per component across all 6 categories
5. Classify each threat by severity using impact x probability
6. Suggest mitigations referencing the security knowledge pack
7. Generate threat model document with threat matrix

## Outputs

| Artifact | Path |
|----------|------|
| Threat model | `results/security/threat-model.md` |

## See Also

- [x-owasp-scan](../x-owasp-scan/) -- OWASP Top 10 verification with ASVS mapping
- [x-hardening-eval](../x-hardening-eval/) -- Application hardening posture evaluation
- [x-security-dashboard](../x-security-dashboard/) -- Aggregated security posture view from all scanning skills
