# Tagging Strategy Template

Standardized tagging strategy for cloud resource cost allocation and governance.

## Mandatory Tags

Every cloud resource MUST have these tags applied at provisioning time.

| Tag Key | Description | Example Values | Enforcement |
|---------|-------------|----------------|-------------|
| `team` | Owning team name | `platform`, `payments`, `search` | Block creation without tag |
| `environment` | Deployment environment | `production`, `staging`, `development` | Block creation without tag |
| `service` | Service or application name | `order-api`, `user-service`, `batch-processor` | Block creation without tag |
| `cost-center` | Finance cost center code | `CC-1001`, `CC-2050` | Block creation without tag |

## Recommended Tags

Strongly recommended for improved cost visibility and governance.

| Tag Key | Description | Example Values |
|---------|-------------|----------------|
| `project` | Project or initiative name | `checkout-v2`, `migration-2025` |
| `owner` | Individual or team email | `alice@company.com`, `team-platform@company.com` |
| `expiry` | Resource expiration date | `2025-12-31`, `never` |
| `managed-by` | Provisioning tool | `terraform`, `pulumi`, `manual` |
| `data-classification` | Data sensitivity level | `public`, `internal`, `confidential` |

## Optional Tags

Additional context tags for specific use cases.

| Tag Key | Description | Example Values |
|---------|-------------|----------------|
| `version` | Application version | `1.2.3`, `latest` |
| `compliance` | Compliance framework | `pci-dss`, `hipaa`, `sox` |
| `backup-policy` | Backup schedule tier | `daily`, `weekly`, `none` |
| `on-call` | On-call rotation | `team-platform-oncall` |

## Enforcement Rules

- **Provisioning Gate**: Reject resource creation if mandatory tags are missing
- **Drift Detection**: Weekly scan for resources with missing or non-compliant tags
- **Remediation SLA**: 48 hours to tag or justify untagged resources
- **Cost Attribution**: Untagged resources charged to a shared "unallocated" cost center
- **Audit Trail**: Log all tag changes for compliance and cost forensics

## Naming Conventions

- Tag keys: lowercase, hyphen-separated (`cost-center`, not `CostCenter`)
- Tag values: lowercase where possible, consistent across all resources
- Maximum tag key length: 128 characters
- Maximum tag value length: 256 characters
- No leading/trailing whitespace in tag keys or values
