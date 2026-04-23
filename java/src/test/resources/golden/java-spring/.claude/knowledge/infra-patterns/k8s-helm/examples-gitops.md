# Example: GitOps Integration

### ArgoCD Application

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: my-service-prod
  namespace: argocd
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: my-project
  source:
    repoURL: https://github.com/example/platform-charts.git
    targetRevision: main
    path: charts/my-service
    helm:
      valueFiles:
        - values.yaml
        - values-prod.yaml
  destination:
    server: https://kubernetes.default.svc
    namespace: my-service-prod
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
      - ServerSideApply=true
    retry:
      limit: 3
      backoff:
        duration: 5s
        maxDuration: 3m
        factor: 2
```

### ArgoCD ApplicationSet (Multi-Environment)

```yaml
apiVersion: argoproj.io/v1alpha1
kind: ApplicationSet
metadata:
  name: my-service
  namespace: argocd
spec:
  generators:
    - list:
        elements:
          - env: dev
            cluster: https://dev-cluster.example.com
            namespace: my-service-dev
          - env: staging
            cluster: https://staging-cluster.example.com
            namespace: my-service-staging
          - env: prod
            cluster: https://prod-cluster.example.com
            namespace: my-service-prod
  template:
    metadata:
      name: "my-service-{{env}}"
      namespace: argocd
    spec:
      project: my-project
      source:
        repoURL: https://github.com/example/platform-charts.git
        targetRevision: main
        path: charts/my-service
        helm:
          valueFiles:
            - values.yaml
            - "values-{{env}}.yaml"
      destination:
        server: "{{cluster}}"
        namespace: "{{namespace}}"
      syncPolicy:
        automated:
          prune: true
          selfHeal: true
        syncOptions:
          - CreateNamespace=true
```

### Flux HelmRelease

```yaml
apiVersion: helm.toolkit.fluxcd.io/v2beta2
kind: HelmRelease
metadata:
  name: my-service
  namespace: my-service-prod
spec:
  interval: 10m
  timeout: 5m
  chart:
    spec:
      chart: charts/my-service
      version: "0.1.x"
      sourceRef:
        kind: GitRepository
        name: platform-charts
        namespace: flux-system
      interval: 5m
  values:
    replicaCount: 3
  valuesFrom:
    - kind: ConfigMap
      name: my-service-values
      valuesKey: values-prod.yaml
  upgrade:
    remediation:
      retries: 3
  rollback:
    cleanupOnFail: true
  test:
    enable: true
```
