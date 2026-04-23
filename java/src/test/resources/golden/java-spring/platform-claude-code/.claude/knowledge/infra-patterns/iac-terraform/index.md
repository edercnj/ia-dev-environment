---
name: iac-terraform
description: "Terraform patterns: module structure, remote state, naming conventions, CI/CD workflows, drift detection, and common infrastructure modules."
---

# Pattern: Terraform Patterns

## Purpose

Provides production-grade Terraform patterns for infrastructure-as-code including project structure, state management, CI/CD integration, and reusable module design across AWS, GCP, and Azure.

## Supplements

Supplements `infrastructure` knowledge pack with Terraform-specific IaC patterns.

## Stack Compatibility

- **Terraform:** ≥ 1.6 (`required_version` enforced); provider-agnostic examples (AWS / GCP / Azure provider blocks substitute cleanly)
- **State backend:** S3 + DynamoDB lock table (or GCS + Cloud Storage lock, or Azure Blob + native locks) — chosen per deployment target
- **terraform-docs:** ≥ 0.17 (CI documentation step)
- **tflint / tfsec / checkov:** optional CI quality gates
- **Atlantis:** ≥ 0.27 (PR-driven apply workflow, optional)

## Patterns Index

| Pattern | Use case | File |
| :--- | :--- | :--- |
| Module Structure | Canonical layout (`main.tf`, `variables.tf`, `outputs.tf`, `versions.tf`), root vs child modules | [`references/examples-module-structure.md`](references/examples-module-structure.md) |
| Remote State | S3 + DynamoDB backend, state locking, state encryption, `terraform_remote_state` data source | [`references/examples-remote-state.md`](references/examples-remote-state.md) |
| Naming Conventions | Resource naming, tags strategy, environment labels, cost allocation | [`references/examples-naming-conventions.md`](references/examples-naming-conventions.md) |
| CI/CD | `terraform fmt/validate/plan/apply` pipeline, PR checks, approval gates, artifact storage | [`references/examples-cicd.md`](references/examples-cicd.md) |
| Drift Detection | Scheduled `terraform plan` comparison, Atlantis drift-check, notification hooks | [`references/examples-drift-detection.md`](references/examples-drift-detection.md) |
| Common Modules | VPC / network, IAM / service accounts, object storage, managed K8s, ALB/NLB patterns | [`references/examples-common-modules.md`](references/examples-common-modules.md) |

## When to Open an Example File

Open a specific `references/examples-<pattern>.md` only when you are about to implement that pattern. The Patterns Index is enough to decide *which* file applies; each example file carries the complete `.tf` snippets.

## References

All pattern examples live under `references/examples-*.md` next to this SKILL.md. The naming convention is `examples-<slug>.md` where `<slug>` matches the third column of the Patterns Index above.
