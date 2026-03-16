# Deploy Runbook — my-quarkus-service

## 1. Service Info

| Field | Value |
|-------|-------|
| Service Name | my-quarkus-service |
| Language | java 21 |
| Framework | quarkus |
| Build Tool | maven |
| Container | docker |
| Orchestrator | kubernetes |

## 2. Pre-conditions

- [ ] All CI checks passing on the target branch
- [ ] Code review approved and merged
- [ ] Artifacts built and pushed to registry


- [ ] Docker image built and tagged


- [ ] Kubernetes manifests updated with new image tag


## 3. Deploy Procedure


1. Update image tag in Kubernetes deployment manifest
2. Apply manifests: `kubectl apply -f k8s/`
3. Monitor rollout: `kubectl rollout status deployment/my-quarkus-service`


## 4. Post-Deploy Verification

- [ ] Health endpoint responds with 200
- [ ] Application logs show successful startup
- [ ] No error spikes in monitoring dashboards


## 5. Rollback Procedure


1. Roll back deployment: `kubectl rollout undo deployment/my-quarkus-service`
2. Verify rollback: `kubectl rollout status deployment/my-quarkus-service`

3. Verify health endpoint responds with 200
4. Notify team of rollback

## 6. Troubleshooting

| Symptom | Possible Cause | Action |
|---------|---------------|--------|
| Health check fails | Application not started | Check application logs |
| Connection refused | Port mismatch | Verify port configuration |
| High error rate | Bad deployment | Initiate rollback procedure |


## 7. Contacts

| Role | Contact |
|------|---------|
| On-call Engineer | TBD |
| Team Lead | TBD |
| DevOps | TBD |
