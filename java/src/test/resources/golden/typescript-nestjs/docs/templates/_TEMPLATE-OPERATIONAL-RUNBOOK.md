# Operational Runbook — my-nestjs-service

## Scaling Procedures

### Horizontal Scaling (Replicas)

| Action | Command / Procedure | Validation |
|--------|-------------------|------------|
| Scale up | Increase replica count in deployment config | Verify new pods are running and healthy |
| Scale down | Decrease replica count (ensure minimum 2 for HA) | Verify traffic is redistributed |
| Emergency scale | Double current replicas | Monitor resource utilization |

### Vertical Scaling (Resources)

- [ ] Review current resource requests and limits
- [ ] Identify bottleneck (CPU, memory, or I/O)
- [ ] Update resource limits in deployment manifest
- [ ] Apply changes with rolling restart
- [ ] Monitor for OOM kills or CPU throttling

### Auto-Scaling Configuration

| Parameter | Description | Recommended Default |
|-----------|-------------|-------------------|
| Min replicas | Minimum number of instances | 2 |
| Max replicas | Maximum number of instances | 10 |
| CPU target | CPU utilization target for scaling | 70% |
| Memory target | Memory utilization target for scaling | 80% |
| Scale-up stabilization | Cooldown period after scale-up | 60s |
| Scale-down stabilization | Cooldown period after scale-down | 300s |

### Scaling Validation Checklist

- [ ] All new replicas pass health checks
- [ ] Load balancer distributes traffic evenly
- [ ] No increase in error rate after scaling
- [ ] Latency remains within SLO thresholds
- [ ] Dependent services can handle increased connections




## Message Broker Operations

### Topic and Queue Management

| Operation | Procedure | Validation |
|-----------|-----------|------------|
| Create topic | Use admin CLI or management console | Verify topic appears in topic list |
| Delete topic | Mark for deletion, verify no active consumers | Confirm topic removed after retention |
| Adjust partitions | Increase partition count (cannot decrease) | Verify consumer group rebalance |
| Update retention | Modify retention period via topic config | Confirm old messages are purged |

### Dead Letter Processing

- [ ] Monitor dead letter queue/topic size
- [ ] Investigate root cause of message failures
- [ ] Replay failed messages after fix is deployed
- [ ] Purge dead letter queue after successful replay
- [ ] Set up alerting for dead letter queue growth

### Consumer Lag Monitoring

| Metric | Healthy Range | Action When Exceeded |
|--------|--------------|---------------------|
| Consumer lag (messages) | < 1000 | Scale up consumers or investigate slow processing |
| Consumer lag (time) | < 5 minutes | Check consumer health and processing throughput |
| Rebalance frequency | < 1 per hour | Investigate consumer stability |
| Failed message rate | < 0.1% | Review error logs and fix processing logic |

### Broker Health Checks

- [ ] All broker nodes are reachable and in-sync
- [ ] Under-replicated partitions count is zero
- [ ] Controller election is stable
- [ ] Disk usage is below 80% on all brokers
- [ ] Network throughput is within expected bounds


## Certificate Rotation

### TLS Certificate Renewal

| Step | Action | Validation |
|------|--------|------------|
| 1 | Generate new certificate signing request (CSR) | Verify CSR contains correct SANs |
| 2 | Submit CSR to certificate authority | Confirm certificate issued |
| 3 | Deploy new certificate to all instances | Verify TLS handshake succeeds |
| 4 | Update trust stores if CA changed | Confirm no TLS errors in logs |
| 5 | Revoke old certificate after grace period | Verify old cert is no longer accepted |

### Mutual TLS Rotation

- [ ] Generate new client certificates for all services
- [ ] Deploy new certificates alongside existing ones (dual-cert period)
- [ ] Update trust stores to accept both old and new certs
- [ ] Roll out new client certs to all services
- [ ] Remove old certificates after all services are updated

### Secrets Rotation Schedule

| Secret Type | Rotation Frequency | Automated |
|-------------|-------------------|-----------|
| TLS certificates | 90 days | Yes |
| API keys | 180 days | Yes |
| Database credentials | 90 days | Yes |
| Service account tokens | 365 days | Depends |

### Validation Steps

- [ ] TLS certificate chain is valid and complete
- [ ] Certificate expiry is at least 30 days in the future
- [ ] No certificate warnings in application logs
- [ ] mTLS connections between services are functional
- [ ] External clients can connect without errors

## Dependency Failure Handling

### External API Failure Procedures

| Step | Action | Fallback |
|------|--------|----------|
| 1 | Detect dependency failure (timeout, 5xx, connection refused) | Circuit breaker opens automatically |
| 2 | Activate degraded mode | Return cached responses or default values |
| 3 | Monitor dependency health endpoint | Circuit breaker half-opens to test recovery |
| 4 | Restore normal operation when dependency recovers | Circuit breaker closes, full functionality restored |

### Circuit Breaker Manual Override

- [ ] Identify the failing dependency circuit breaker
- [ ] Force-open circuit breaker if auto-detection is delayed
- [ ] Verify fallback behavior is active
- [ ] Monitor for cascading failures
- [ ] Force-close circuit breaker when dependency is confirmed healthy

### Fallback Activation

| Dependency | Fallback Strategy | Data Staleness Tolerance |
|------------|-------------------|-------------------------|
| External API | Cached response | 5-15 minutes |
| Database read | Read from replica or cache | 1-5 minutes |
| Message broker | In-memory queue with disk spillover | Until broker recovers |
| Authentication service | Cached tokens with extended validity | 15-60 minutes |

### Degraded Mode Operations

- [ ] Identify which features are affected
- [ ] Communicate degraded status to users (status page)
- [ ] Monitor error rates and latency in degraded mode
- [ ] Prepare for traffic surge when dependency recovers
- [ ] Document degraded mode behavior for future reference

## Backup & Restore Procedures

### Backup Verification

| Check | Frequency | Procedure |
|-------|-----------|-----------|
| Backup completion | Daily | Verify backup job completed without errors |
| Backup integrity | Weekly | Restore backup to test environment and validate |
| Backup size trend | Monthly | Ensure backup size growth is within expectations |
| Retention compliance | Monthly | Verify oldest backup meets retention policy |

### Restore Testing Schedule

| Test Type | Frequency | Scope |
|-----------|-----------|-------|
| Table-level restore | Monthly | Restore single table to test environment |
| Full restore | Quarterly | Restore complete database to test environment |
| Point-in-time recovery | Quarterly | Restore to specific timestamp |
| Cross-region restore | Semi-annually | Restore backup in different region |

### Point-in-Time Recovery

1. Identify the target recovery timestamp
2. Stop application traffic to prevent further writes
3. Restore base backup closest to (but before) target time
4. Apply WAL/binlog files up to target timestamp
5. Verify data integrity at target timestamp
6. Resume application traffic

### Disaster Recovery Activation

| Step | Action | RTO Target |
|------|--------|------------|
| 1 | Declare disaster recovery event | 0 min |
| 2 | Activate DR environment | 15 min |
| 3 | Restore from latest backup | 30-60 min |
| 4 | Verify data integrity | 15 min |
| 5 | Switch DNS/traffic to DR environment | 5 min |
| 6 | Validate all services operational | 15 min |
