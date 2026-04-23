# Example: Helmfile

### Multi-Chart Orchestration

```yaml
# helmfile.yaml
helmDefaults:
  wait: true
  timeout: 300
  createNamespace: true
  atomic: true

environments:
  dev:
    values:
      - environments/dev/defaults.yaml
  staging:
    values:
      - environments/staging/defaults.yaml
  prod:
    values:
      - environments/prod/defaults.yaml

repositories:
  - name: bitnami
    url: https://charts.bitnami.com/bitnami
  - name: ingress-nginx
    url: https://kubernetes.github.io/ingress-nginx
  - name: cert-manager
    url: https://charts.jetstack.io

releases:
  # Infrastructure components
  - name: ingress-nginx
    namespace: ingress-nginx
    chart: ingress-nginx/ingress-nginx
    version: 4.10.x
    values:
      - charts/ingress-nginx/values.yaml
      - charts/ingress-nginx/values-{{ .Environment.Name }}.yaml
    installed: true

  - name: cert-manager
    namespace: cert-manager
    chart: cert-manager/cert-manager
    version: 1.15.x
    values:
      - charts/cert-manager/values.yaml
    set:
      - name: installCRDs
        value: "true"
    installed: true

  # Application services
  - name: my-service
    namespace: my-service-{{ .Environment.Name }}
    chart: ./charts/my-service
    values:
      - charts/my-service/values.yaml
      - charts/my-service/values-{{ .Environment.Name }}.yaml
    needs:
      - ingress-nginx/ingress-nginx
      - cert-manager/cert-manager
    installed: true

  - name: worker-service
    namespace: my-service-{{ .Environment.Name }}
    chart: ./charts/worker-service
    values:
      - charts/worker-service/values.yaml
      - charts/worker-service/values-{{ .Environment.Name }}.yaml
    needs:
      - my-service-{{ .Environment.Name }}/my-service
    installed: true
```

### Usage Commands

```bash
# Diff changes before applying
helmfile -e prod diff

# Apply to specific environment
helmfile -e prod apply

# Apply a specific release
helmfile -e prod -l name=my-service apply

# Destroy all releases in an environment
helmfile -e dev destroy
```
