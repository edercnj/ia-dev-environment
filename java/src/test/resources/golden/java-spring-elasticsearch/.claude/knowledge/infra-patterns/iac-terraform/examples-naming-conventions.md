# Example: Naming Conventions

### Resource Naming Pattern

```
{provider_prefix}_{resource_type}.{project}_{environment}_{component}
```

### Rules

| Element | Convention | Example |
|---|---|---|
| **Resources** | `snake_case`, descriptive | `aws_vpc.main`, `aws_subnet.private` |
| **Variables** | `snake_case`, noun-based | `vpc_cidr`, `instance_class` |
| **Outputs** | `snake_case`, prefixed by resource | `vpc_id`, `private_subnet_ids` |
| **Locals** | `snake_case` | `common_tags`, `subnet_cidrs` |
| **Modules** | `snake_case`, short | `module.vpc`, `module.database` |
| **Files** | `main.tf`, `variables.tf`, `outputs.tf`, `versions.tf`, `locals.tf` | Standard file set |
| **Cloud resources** | `{project}-{env}-{component}` | `myapp-prod-vpc`, `myapp-prod-rds` |

### Variable Definition Pattern

```hcl
# variables.tf
variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be one of: dev, staging, prod."
  }
}

variable "project" {
  description = "Project name used for resource naming"
  type        = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,20}$", var.project))
    error_message = "Project must be lowercase alphanumeric with hyphens, 3-21 characters."
  }
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"

  validation {
    condition     = can(cidrhost(var.vpc_cidr, 0))
    error_message = "Must be a valid CIDR block."
  }
}

variable "tags" {
  description = "Additional tags to apply to all resources"
  type        = map(string)
  default     = {}
}
```

### Output Definition Pattern

```hcl
# outputs.tf
output "vpc_id" {
  description = "ID of the VPC"
  value       = aws_vpc.main.id
}

output "private_subnet_ids" {
  description = "IDs of the private subnets"
  value       = aws_subnet.private[*].id
}

output "database_endpoint" {
  description = "Connection endpoint for the database"
  value       = aws_db_instance.main.endpoint
  sensitive   = true
}
```
