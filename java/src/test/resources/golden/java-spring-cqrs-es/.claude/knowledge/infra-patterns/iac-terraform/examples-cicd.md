# Example: CI/CD

### Plan on PR, Apply on Merge

```yaml
# .github/workflows/terraform.yaml
name: Terraform
on:
  pull_request:
    paths:
      - 'terraform/**'
  push:
    branches: [main]
    paths:
      - 'terraform/**'

permissions:
  id-token: write   # OIDC authentication
  contents: read
  pull-requests: write

jobs:
  detect-changes:
    runs-on: ubuntu-latest
    outputs:
      environments: ${{ steps.changes.outputs.environments }}
    steps:
      - uses: actions/checkout@v4
      - name: Detect changed environments
        id: changes
        run: |
          ENVS=$(git diff --name-only ${{ github.event.before }} ${{ github.sha }} \
            | grep '^terraform/environments/' \
            | cut -d'/' -f3 \
            | sort -u \
            | jq -R -s -c 'split("\n") | map(select(. != ""))')
          echo "environments=${ENVS}" >> "$GITHUB_OUTPUT"

  plan:
    needs: detect-changes
    if: github.event_name == 'pull_request'
    runs-on: ubuntu-latest
    strategy:
      matrix:
        environment: ${{ fromJson(needs.detect-changes.outputs.environments) }}
    steps:
      - uses: actions/checkout@v4

      - uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: "1.7.x"

      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789012:role/terraform-plan
          aws-region: us-east-1

      - name: Terraform Init
        working-directory: terraform/environments/${{ matrix.environment }}
        run: terraform init -input=false

      - name: Terraform Validate
        working-directory: terraform/environments/${{ matrix.environment }}
        run: terraform validate

      - name: Terraform Plan
        working-directory: terraform/environments/${{ matrix.environment }}
        run: terraform plan -input=false -out=tfplan

      - name: Post plan to PR
        uses: borchero/terraform-plan-comment@v2
        with:
          working-directory: terraform/environments/${{ matrix.environment }}
          plan-file: tfplan

  apply:
    needs: detect-changes
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    strategy:
      max-parallel: 1
      matrix:
        environment: ${{ fromJson(needs.detect-changes.outputs.environments) }}
    environment: ${{ matrix.environment }}
    steps:
      - uses: actions/checkout@v4

      - uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: "1.7.x"

      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789012:role/terraform-apply
          aws-region: us-east-1

      - name: Terraform Init
        working-directory: terraform/environments/${{ matrix.environment }}
        run: terraform init -input=false

      - name: Terraform Apply
        working-directory: terraform/environments/${{ matrix.environment }}
        run: terraform apply -input=false -auto-approve
```
