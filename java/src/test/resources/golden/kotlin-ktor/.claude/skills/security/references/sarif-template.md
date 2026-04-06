# SARIF 2.1.0 Template

## Overview

The Static Analysis Results Interchange Format (SARIF) version 2.1.0 is the industry standard for security scan results. All security skills MUST produce SARIF-compliant output for GitHub Advanced Security and CI/CD integration.

## Required Schema Structure

Every SARIF output MUST include the following top-level fields:

```json
{
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/main/sarif-2.1/schema/sarif-schema-2.1.0.json",
  "version": "2.1.0",
  "runs": [
    {
      "tool": {
        "driver": {
          "name": "<scan-tool-name>",
          "version": "<scan-tool-version>",
          "informationUri": "<tool-documentation-url>",
          "rules": []
        }
      },
      "results": []
    }
  ]
}
```

## Rule Definition

Each rule referenced by results MUST be defined in `runs[].tool.driver.rules`:

```json
{
  "id": "SAST-001",
  "name": "SqlInjection",
  "shortDescription": {
    "text": "SQL Injection vulnerability detected"
  },
  "fullDescription": {
    "text": "User-controlled input flows into a SQL query without parameterization."
  },
  "helpUri": "https://owasp.org/Top10/A03_2021-Injection/",
  "defaultConfiguration": {
    "level": "error"
  },
  "properties": {
    "owasp-category": "A03",
    "cwe-id": "CWE-89"
  }
}
```

## Result Entry Structure

Each finding in `runs[].results` MUST follow this structure:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `ruleId` | String | Yes | Pattern: `SCAN-NNN` (e.g., `SAST-001`, `SCA-042`) |
| `level` | String | Yes | One of: `error`, `warning`, `note`, `none` |
| `message.text` | String | Yes | Human-readable description of the finding |
| `locations[].physicalLocation.artifactLocation.uri` | String | Yes | Relative file path |
| `locations[].physicalLocation.region.startLine` | int | Yes | Line number (> 0) |
| `properties.owasp-category` | String | No | OWASP category: `A01`-`A10` |
| `properties.cvss-score` | float | No | CVSS score: `0.0`-`10.0` |
| `properties.cwe-id` | String | No | CWE identifier: `CWE-NNN` |
| `properties.fix-recommendation` | String | No | Remediation guidance |

## SARIF Level to Severity Mapping

| Severity | SARIF Level | Weight |
|----------|-------------|--------|
| CRITICAL | `error` | 10 |
| HIGH | `error` | 5 |
| MEDIUM | `warning` | 2 |
| LOW | `note` | 1 |
| INFO | `none` | 0 |

> CRITICAL and HIGH both map to `error`. Differentiation is done via `properties.cvss-score` (CRITICAL >= 9.0, HIGH >= 7.0).

## Examples by Severity

### CRITICAL (level: error, CVSS >= 9.0)

```json
{
  "ruleId": "SAST-001",
  "level": "error",
  "message": {
    "text": "SQL Injection detected in query builder. User input concatenated directly into SQL string."
  },
  "locations": [
    {
      "physicalLocation": {
        "artifactLocation": {
          "uri": "src/main/java/com/example/dao/UserDao.java"
        },
        "region": {
          "startLine": 42,
          "startColumn": 12
        }
      }
    }
  ],
  "properties": {
    "owasp-category": "A03",
    "cvss-score": 9.8,
    "cwe-id": "CWE-89",
    "fix-recommendation": "Use parameterized queries or prepared statements instead of string concatenation."
  }
}
```

### HIGH (level: error, CVSS 7.0-8.9)

```json
{
  "ruleId": "SCA-012",
  "level": "error",
  "message": {
    "text": "Known vulnerability CVE-2024-1234 in dependency commons-text:1.9. Remote code execution via StringSubstitutor."
  },
  "locations": [
    {
      "physicalLocation": {
        "artifactLocation": {
          "uri": "pom.xml"
        },
        "region": {
          "startLine": 87
        }
      }
    }
  ],
  "properties": {
    "owasp-category": "A06",
    "cvss-score": 7.5,
    "cwe-id": "CWE-94",
    "fix-recommendation": "Upgrade commons-text to version 1.10.0 or later."
  }
}
```

### MEDIUM (level: warning, CVSS 4.0-6.9)

```json
{
  "ruleId": "SAST-015",
  "level": "warning",
  "message": {
    "text": "Sensitive data logged without masking. PII field 'email' written to application log."
  },
  "locations": [
    {
      "physicalLocation": {
        "artifactLocation": {
          "uri": "src/main/java/com/example/service/AuditService.java"
        },
        "region": {
          "startLine": 63
        }
      }
    }
  ],
  "properties": {
    "owasp-category": "A09",
    "cvss-score": 5.3,
    "cwe-id": "CWE-532",
    "fix-recommendation": "Apply PII masking before logging. Use structured logging with field-level redaction."
  }
}
```

### LOW (level: note)

```json
{
  "ruleId": "SAST-042",
  "level": "note",
  "message": {
    "text": "Missing security header: X-Content-Type-Options not set in response."
  },
  "locations": [
    {
      "physicalLocation": {
        "artifactLocation": {
          "uri": "src/main/java/com/example/config/SecurityConfig.java"
        },
        "region": {
          "startLine": 28
        }
      }
    }
  ],
  "properties": {
    "owasp-category": "A05",
    "cwe-id": "CWE-693",
    "fix-recommendation": "Add X-Content-Type-Options: nosniff header to all HTTP responses."
  }
}
```

### INFO (level: none)

```json
{
  "ruleId": "INFO-001",
  "level": "none",
  "message": {
    "text": "Dependency commons-lang3:3.14.0 has an available update to 3.15.0. No known vulnerabilities."
  },
  "locations": [
    {
      "physicalLocation": {
        "artifactLocation": {
          "uri": "pom.xml"
        },
        "region": {
          "startLine": 52
        }
      }
    }
  ],
  "properties": {
    "fix-recommendation": "Consider upgrading to latest version for bug fixes and improvements."
  }
}
```

## Complete SARIF Document Example

```json
{
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/main/sarif-2.1/schema/sarif-schema-2.1.0.json",
  "version": "2.1.0",
  "runs": [
    {
      "tool": {
        "driver": {
          "name": "ia-dev-security-scan",
          "version": "1.0.0",
          "informationUri": "https://github.com/edercnj/ia-dev-environment",
          "rules": [
            {
              "id": "SAST-001",
              "name": "SqlInjection",
              "shortDescription": {
                "text": "SQL Injection vulnerability"
              },
              "defaultConfiguration": {
                "level": "error"
              },
              "properties": {
                "owasp-category": "A03",
                "cwe-id": "CWE-89"
              }
            }
          ]
        }
      },
      "results": [
        {
          "ruleId": "SAST-001",
          "level": "error",
          "message": {
            "text": "SQL Injection detected in UserDao.findByName()"
          },
          "locations": [
            {
              "physicalLocation": {
                "artifactLocation": {
                  "uri": "src/main/java/com/example/dao/UserDao.java"
                },
                "region": {
                  "startLine": 42
                }
              }
            }
          ],
          "properties": {
            "owasp-category": "A03",
            "cvss-score": 9.8,
            "cwe-id": "CWE-89",
            "fix-recommendation": "Use parameterized queries."
          }
        }
      ]
    }
  ]
}
```

## Validation Rules

1. `$schema` MUST reference the official SARIF 2.1.0 schema URL
2. `version` MUST be `"2.1.0"`
3. `runs` MUST contain at least one run entry
4. Every `ruleId` in results MUST have a corresponding entry in `rules`
5. `locations` MUST contain at least one physical location with a valid URI
6. `startLine` MUST be a positive integer
7. `level` MUST be one of: `error`, `warning`, `note`, `none`
8. `properties.cvss-score` when present MUST be between 0.0 and 10.0
9. `properties.owasp-category` when present MUST match pattern `A[0-9]{2}`
10. `properties.cwe-id` when present MUST match pattern `CWE-[0-9]+`
