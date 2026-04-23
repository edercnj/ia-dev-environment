# Example: Testing

### Post-Deployment Connection Test

```yaml
# templates/tests/test-connection.yaml
apiVersion: v1
kind: Pod
metadata:
  name: "{{ include "my-service.fullname" . }}-test-connection"
  labels:
    {{- include "my-service.labels" . | nindent 4 }}
  annotations:
    "helm.sh/hook": test
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  restartPolicy: Never
  securityContext:
    runAsNonRoot: true
    runAsUser: 10001
    seccompProfile:
      type: RuntimeDefault
  containers:
    - name: wget
      image: busybox:1.36
      command: ['wget']
      args:
        - '--spider'
        - '--timeout=5'
        - 'http://{{ include "my-service.fullname" . }}:{{ .Values.service.port }}/healthz'
      securityContext:
        allowPrivilegeEscalation: false
        readOnlyRootFilesystem: true
        capabilities:
          drop:
            - ALL
      resources:
        limits:
          cpu: 50m
          memory: 32Mi
```

### Comprehensive API Test

```yaml
# templates/tests/test-api.yaml
apiVersion: v1
kind: Pod
metadata:
  name: "{{ include "my-service.fullname" . }}-test-api"
  annotations:
    "helm.sh/hook": test
    "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
spec:
  restartPolicy: Never
  securityContext:
    runAsNonRoot: true
    runAsUser: 10001
    seccompProfile:
      type: RuntimeDefault
  containers:
    - name: test
      image: curlimages/curl:8.7.1
      command: ["/bin/sh", "-c"]
      args:
        - |
          set -e
          BASE_URL="http://{{ include "my-service.fullname" . }}:{{ .Values.service.port }}"

          echo "Testing health endpoint..."
          curl -sf "${BASE_URL}/healthz" || exit 1

          echo "Testing readiness endpoint..."
          curl -sf "${BASE_URL}/readyz" || exit 1

          echo "Testing API response..."
          STATUS=$(curl -sf -o /dev/null -w "%{http_code}" "${BASE_URL}/api/v1/status")
          [ "$STATUS" = "200" ] || exit 1

          echo "All tests passed."
      securityContext:
        allowPrivilegeEscalation: false
        readOnlyRootFilesystem: true
        capabilities:
          drop:
            - ALL
      resources:
        limits:
          cpu: 50m
          memory: 32Mi
```
