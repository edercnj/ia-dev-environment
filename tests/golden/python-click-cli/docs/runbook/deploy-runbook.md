# Deploy Runbook — my-cli-tool

## 1. Service Info

| Field | Value |
|-------|-------|
| Service Name | my-cli-tool |
| Language | python 3.9 |
| Framework | click |
| Build Tool | pip |
| Container | docker |
| Orchestrator | none |

## 2. Pre-conditions

- [ ] All CI checks passing on the target branch
- [ ] Code review approved and merged
- [ ] Artifacts built and pushed to registry


- [ ] Docker image built and tagged



## 3. Deploy Procedure


1. Pull latest Docker image
2. Stop existing container: `docker compose down`
3. Start updated container: `docker compose up -d`


## 4. Post-Deploy Verification

- [ ] Health endpoint responds with 200
- [ ] Application logs show successful startup
- [ ] No error spikes in monitoring dashboards


## 5. Rollback Procedure


1. Stop current container: `docker compose down`
2. Redeploy previous image tag: `docker compose up -d`

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
