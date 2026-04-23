# Example: Values Template

### values.yaml with All Common Fields

```yaml
# charts/my-service/values.yaml

# -- Number of replicas (ignored when autoscaling is enabled)
replicaCount: 2

# -- Override chart name
nameOverride: ""
# -- Override full name
fullnameOverride: ""

# -- Labels to add to all resources
commonLabels: {}

image:
  # -- Container registry
  registry: registry.example.com
  # -- Image repository
  repository: my-service
  # -- Image tag (defaults to Chart.appVersion)
  tag: ""
  # -- Image pull policy
  pullPolicy: IfNotPresent

# -- Image pull secrets
imagePullSecrets: []

serviceAccount:
  # -- Create a ServiceAccount
  create: true
  # -- ServiceAccount name (auto-generated if empty)
  name: ""
  # -- Annotations for the ServiceAccount (e.g., IAM role)
  annotations: {}

# -- Pod-level security context
podSecurityContext:
  runAsNonRoot: true
  runAsUser: 10001
  runAsGroup: 10001
  fsGroup: 10001
  seccompProfile:
    type: RuntimeDefault

# -- Container-level security context
securityContext:
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  runAsNonRoot: true
  capabilities:
    drop:
      - ALL

service:
  # -- Service type
  type: ClusterIP
  # -- Service port
  port: 80
  # -- Container target port
  targetPort: 8080

ingress:
  # -- Enable Ingress
  enabled: false
  # -- Ingress class name
  className: nginx
  # -- Ingress annotations
  annotations: {}
  # -- Ingress hosts
  hosts:
    - host: my-service.example.com
      paths:
        - path: /
          pathType: Prefix
  # -- TLS configuration
  tls: []

resources:
  requests:
    cpu: 250m
    memory: 256Mi
  limits:
    cpu: 500m
    memory: 512Mi

# -- Environment variables
env: []
# -- Environment from ConfigMap/Secret references
envFrom: []

startupProbe:
  enabled: true
  path: /healthz
  initialDelaySeconds: 5
  periodSeconds: 5
  failureThreshold: 12

livenessProbe:
  enabled: true
  path: /healthz
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  enabled: true
  path: /readyz
  periodSeconds: 5
  failureThreshold: 3

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

pdb:
  enabled: true
  minAvailable: "50%"

strategy:
  maxSurge: 1
  maxUnavailable: 0

# -- Termination grace period in seconds
terminationGracePeriodSeconds: 30

# -- Pod annotations
podAnnotations:
  prometheus.io/scrape: "true"
  prometheus.io/port: "8080"
  prometheus.io/path: "/metrics"

# -- Pod labels (in addition to common labels)
podLabels: {}

# -- Topology spread constraints
topologySpreadConstraints:
  - maxSkew: 1
    topologyKey: topology.kubernetes.io/zone
    whenUnsatisfiable: DoNotSchedule
    labelSelector:
      matchLabels: {}

# -- Node selector
nodeSelector: {}
# -- Tolerations
tolerations: []

# -- Extra volumes
extraVolumes: []
# -- Extra volume mounts
extraVolumeMounts: []

# -- Dependency: PostgreSQL subchart
postgresql:
  enabled: false

# -- Dependency: Redis subchart (aliased as cache)
cache:
  enabled: false
```
