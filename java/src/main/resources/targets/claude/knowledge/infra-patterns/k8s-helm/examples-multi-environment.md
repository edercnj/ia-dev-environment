# Example: Multi-Environment

### values-dev.yaml

```yaml
# charts/my-service/values-dev.yaml
replicaCount: 1

image:
  tag: "latest"

resources:
  requests:
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 250m
    memory: 256Mi

autoscaling:
  enabled: false

pdb:
  enabled: false

env:
  - name: LOG_LEVEL
    value: "debug"
  - name: ENABLE_TRACING
    value: "false"

ingress:
  enabled: true
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-staging
  hosts:
    - host: my-service.dev.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: my-service-dev-tls
      hosts:
        - my-service.dev.example.com
```

### values-staging.yaml

```yaml
# charts/my-service/values-staging.yaml
replicaCount: 2

resources:
  requests:
    cpu: 250m
    memory: 256Mi
  limits:
    cpu: 500m
    memory: 512Mi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 5

env:
  - name: LOG_LEVEL
    value: "info"
  - name: ENABLE_TRACING
    value: "true"

ingress:
  enabled: true
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-staging
  hosts:
    - host: my-service.staging.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: my-service-staging-tls
      hosts:
        - my-service.staging.example.com
```

### values-prod.yaml

```yaml
# charts/my-service/values-prod.yaml
replicaCount: 3

resources:
  requests:
    cpu: 500m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 15

env:
  - name: LOG_LEVEL
    value: "warn"
  - name: ENABLE_TRACING
    value: "true"

ingress:
  enabled: true
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
  hosts:
    - host: my-service.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: my-service-prod-tls
      hosts:
        - my-service.example.com

serviceAccount:
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/my-service-prod
```
