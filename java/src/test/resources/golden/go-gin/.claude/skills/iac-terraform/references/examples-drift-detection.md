# Example: Drift Detection

### Scheduled Plan with Notification

```yaml
# .github/workflows/drift-detection.yaml
name: Terraform Drift Detection
on:
  schedule:
    - cron: '0 6 * * 1-5'  # Mon-Fri at 6:00 UTC
  workflow_dispatch: {}

jobs:
  drift:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        environment: [dev, staging, prod]
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

      - name: Terraform Plan (detect drift)
        id: plan
        working-directory: terraform/environments/${{ matrix.environment }}
        run: |
          terraform plan -input=false -detailed-exitcode -out=tfplan 2>&1 | tee plan-output.txt
          EXIT_CODE=${PIPESTATUS[0]}
          echo "exit_code=${EXIT_CODE}" >> "$GITHUB_OUTPUT"
          # Exit code 0 = no changes, 1 = error, 2 = changes detected
        continue-on-error: true

      - name: Notify on drift
        if: steps.plan.outputs.exit_code == '2'
        run: |
          curl -X POST "${{ secrets.SLACK_WEBHOOK_URL }}" \
            -H 'Content-Type: application/json' \
            -d "{
              \"text\": \"Terraform drift detected in *${{ matrix.environment }}* environment. Review the plan output and reconcile.\",
              \"blocks\": [
                {
                  \"type\": \"section\",
                  \"text\": {
                    \"type\": \"mrkdwn\",
                    \"text\": \"*Terraform Drift Detected*\nEnvironment: \`${{ matrix.environment }}\`\nRun: ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}\"
                  }
                }
              ]
            }"

      - name: Fail on error
        if: steps.plan.outputs.exit_code == '1'
        run: exit 1
```
