# Deploy Runbook — my-fastapi-timescale

## 1. Service Info

| Field | Value |
|-------|-------|
| Name | my-fastapi-timescale |
| Version | `vX.Y.Z` |
| Environment | `production` |
| Date | `YYYY-MM-DD` |

## 2. Pre-conditions

- [ ] All tests passing in CI
- [ ] Artifact built and published
- [ ] Configuration reviewed and updated
- [ ] Monitoring dashboards accessible


## 3. Deploy Procedure

### Step-by-step

1. Verify pre-conditions checklist above
2. Notify the team in the operations channel



### Docker Deployment

```bash
# Pull the new image
docker compose pull

# Deploy with zero downtime
docker compose up -d --remove-orphans

# Verify container is running
docker compose ps
```



3. Verify deployment status
4. Run post-deploy verification

## 4. Post-Deploy Verification

### Health Checks

- [ ] Application health endpoint returns `200 OK`
- [ ] All dependent services are reachable
- [ ] No error spikes in logs

### Smoke Tests

- [ ] Core business flow validated
- [ ] API responses match expected format
- [ ] Latency within acceptable thresholds

## 5. Rollback Procedure

### When to Rollback

- Health checks failing after deployment
- Error rate exceeds acceptable threshold
- Critical business flow broken

### Rollback Steps

1. Notify the team about rollback decision



### Docker Rollback

```bash
# Rollback to previous image
docker compose down
docker compose up -d --remove-orphans

# Verify container is running
docker compose ps
```




2. Verify rollback was successful
3. Investigate root cause

## 6. Troubleshooting

| Problem | Cause | Solution |
|---------|-------|----------|
| Pods not starting | Image pull error | Verify image tag and registry credentials |
| Health check failing | Application crash | Check application logs and configuration |
| High latency | Resource constraints | Scale up or check resource limits |
| Connection refused | Network policy | Verify service endpoints and network config |

## 7. Contacts

| Role | Contact |
|------|---------|
| Oncall Engineer | `@oncall` |
| Team Lead | `@team-lead` |
| Escalation | `@engineering-manager` |
