# Incident Response Guide

## Severity Classification

| Severity | Criteria | Impact | Response Time |
|----------|----------|--------|---------------|
| SEV1 | Critical -- Complete service outage, significant financial loss, or data breach affecting all users | Total loss of service or critical business function | 15 min |
| SEV2 | Major -- Partial service degradation affecting a large percentage of users, or significant feature unavailable | Major feature unavailable or severe performance degradation | 30 min |
| SEV3 | Moderate -- Minor feature degradation, workaround available, limited user impact | Minor feature impacted, workaround exists | 4 hours |
| SEV4 | Low -- Cosmetic issue, minimal user impact, no business impact | Minimal impact, no revenue or data loss | Next business day |

### Severity Decision Criteria

- **User Impact**: How many users are affected? Is it all users or a subset?
- **Financial Impact**: Is there direct revenue loss or potential financial liability?
- **Data Impact**: Is there data loss, corruption, or unauthorized access?
- **Scope**: Is the issue isolated or widespread across regions/services?

## Detection & Triage

### Initial Detection Checklist

- [ ] Alert source identified (monitoring, user report, automated test)
- [ ] Alert timestamp recorded
- [ ] Affected service(s) identified
- [ ] Initial severity assessed using classification table above
- [ ] On-call engineer acknowledged the alert

### Triage Steps

1. **Verify the alert**: Confirm the issue is real and not a false positive
2. **Check dashboards**: Review monitoring dashboards for anomalies
3. **Assess blast radius**: Determine which services, regions, and users are affected
4. **Check recent changes**: Review recent deployments, config changes, or infrastructure modifications
5. **Assign severity**: Use the severity classification table to assign an initial severity level

## Communication Plan

### Internal Communication

| Severity | Channel | Frequency | Audience |
|----------|---------|-----------|----------|
| SEV1 | Incident war room + Slack #incidents | Every 30 min | Engineering, Leadership, Support |
| SEV2 | Slack #incidents | Every 60 min | Engineering, Support |
| SEV3 | Slack #incidents | On resolution | Engineering team |
| SEV4 | Team channel | On resolution | Owning team |

### External Communication (Status Page)

**SEV1 Template:**
> We are currently investigating an issue affecting [SERVICE]. Our team is actively working on resolution. We will provide updates every 30 minutes.

**SEV2 Template:**
> We are aware of degraded performance in [SERVICE]. Our team is investigating. We will provide updates every 60 minutes.

### Stakeholder Notification

- [ ] Internal status page updated
- [ ] External status page updated (SEV1/SEV2)
- [ ] Customer support team notified
- [ ] Leadership notified (SEV1)

## Mitigation Steps

### General Mitigation Checklist

- [ ] **Rollback**: Can the issue be resolved by rolling back the last deployment?
- [ ] **Feature Flags**: Can the problematic feature be disabled via feature flag?
- [ ] **Scaling**: Does the service need horizontal or vertical scaling?
- [ ] **Traffic Diversion**: Can traffic be redirected to healthy instances or regions?
- [ ] **Cache Flush**: Does the cache need to be invalidated?
- [ ] **Restart**: Will restarting the service resolve the issue?
- [ ] **Database**: Are there database-level mitigations (query kill, connection pool reset)?

### Mitigation Priority Order

1. Rollback to last known good version
2. Disable problematic feature via feature flag
3. Scale up affected services
4. Divert traffic to healthy regions
5. Apply hotfix if rollback is not possible

## Escalation Matrix

| Severity | Primary Responder | Escalation Target | Response Time | Escalation Time |
|----------|-------------------|-------------------|---------------|-----------------|
| SEV1 | On-Call Engineer | Incident Commander | 15 min | 30 min |
| SEV2 | On-Call Engineer | Team Lead | 30 min | 60 min |
| SEV3 | On-Call Engineer | -- | 4 hours | -- |
| SEV4 | Owning Team | -- | Next business day | -- |

### Roles During Incident

- **Incident Commander**: Coordinates response, makes decisions, manages communication (SEV1/SEV2)
- **On-Call Engineer**: First responder, performs initial triage and mitigation
- **Subject Matter Expert**: Provides domain-specific expertise for affected systems
- **Communications Lead**: Manages external and stakeholder communications (SEV1)

## Resolution Verification

### Verification Checklist

- [ ] Primary metrics have returned to baseline values
- [ ] Error rates are within acceptable thresholds
- [ ] All monitoring alerts related to the incident are resolved
- [ ] Health check endpoints return successful responses
- [ ] End-to-end smoke tests pass
- [ ] Affected users have been notified of resolution
- [ ] Incident status page updated to "Resolved"

### Post-Resolution Monitoring

- Monitor affected services for at least 30 minutes after resolution (SEV1/SEV2)
- Verify no secondary issues emerged from the mitigation
- Confirm all rollback or scaling changes are stable

## Timeline Template

| Time (UTC) | Action | Owner | Notes |
|------------|--------|-------|-------|
| `YYYY-MM-DD HH:MM` | Alert triggered | Monitoring | Initial alert details |
| `YYYY-MM-DD HH:MM` | On-call acknowledged | `@engineer` | Severity classification |
| `YYYY-MM-DD HH:MM` | Triage completed | `@engineer` | Blast radius assessed |
| `YYYY-MM-DD HH:MM` | Escalation initiated | `@engineer` | Incident Commander assigned |
| `YYYY-MM-DD HH:MM` | Mitigation applied | `@engineer` | Description of mitigation |
| `YYYY-MM-DD HH:MM` | Resolution verified | `@engineer` | Metrics back to normal |
| `YYYY-MM-DD HH:MM` | Incident closed | `@commander` | All checks passed |
