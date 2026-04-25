# Example: Dependencies

### Subchart Management in Chart.yaml

```yaml
dependencies:
  # Conditionally enabled subchart
  - name: postgresql
    version: "15.5.x"
    repository: "oci://registry-1.docker.io/bitnamicharts"
    condition: postgresql.enabled

  # Aliased subchart (use different name in values.yaml)
  - name: redis
    version: "19.6.x"
    repository: "oci://registry-1.docker.io/bitnamicharts"
    condition: cache.enabled
    alias: cache

  # Import specific values from subchart
  - name: common
    version: "2.x.x"
    repository: "oci://registry-1.docker.io/bitnamicharts"
    tags:
      - common
```

### Subchart Configuration in values.yaml

```yaml
# Enable/disable subcharts
postgresql:
  enabled: true
  auth:
    database: myservice
    username: myservice
    existingSecret: my-service-db-credentials
  primary:
    persistence:
      size: 10Gi
    resources:
      requests:
        cpu: 250m
        memory: 256Mi

cache:
  enabled: true
  architecture: standalone
  auth:
    enabled: true
    existingSecret: my-service-cache-credentials
  master:
    persistence:
      size: 1Gi
    resources:
      requests:
        cpu: 100m
        memory: 128Mi
```
