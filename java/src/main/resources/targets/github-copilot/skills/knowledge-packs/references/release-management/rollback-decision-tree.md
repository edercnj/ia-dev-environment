# Rollback Decision Tree

## Quick Decision Guide

When a production issue is detected after deployment, use this decision tree to determine whether to roll back or fix forward.

## Step 1: Assess Severity

| Severity | Description | Examples |
|----------|------------|---------|
| Critical | Data loss, security breach, total outage | Database corruption, auth bypass, 500 on all endpoints |
| High | Significant feature broken, partial outage | Payment processing failed, search unavailable |
| Medium | Feature degraded, workaround available | Slow response times, minor UI bug |
| Low | Cosmetic issue, minimal user impact | Typo, styling issue, non-critical log noise |

**If Critical -> ROLLBACK IMMEDIATELY (do not proceed to Step 2)**

## Step 2: Assess Blast Radius

| Blast Radius | Description | Indicator |
|-------------|------------|-----------|
| Wide | All users affected | Error rate spike across all endpoints |
| Moderate | Specific user segment affected | Errors in specific region or user type |
| Narrow | Single feature or flow affected | Errors isolated to one endpoint |

## Step 3: Assess Time to Fix

| Estimate | Description |
|----------|------------|
| Quick (< 30 min) | Root cause known, fix is straightforward |
| Medium (30 min - 2 hours) | Root cause suspected, fix needs testing |
| Long (> 2 hours) | Root cause unknown or fix is complex |

## Decision Matrix

| Severity | Blast Radius | Time to Fix | Decision |
|----------|-------------|-------------|----------|
| Critical | Any | Any | **ROLLBACK** |
| High | Wide | Any | **ROLLBACK** |
| High | Moderate | Quick | Fix forward (with caution) |
| High | Moderate | Medium/Long | **ROLLBACK** |
| High | Narrow | Quick | Fix forward |
| High | Narrow | Medium | Fix forward (with monitoring) |
| High | Narrow | Long | **ROLLBACK** |
| Medium | Wide | Quick | Fix forward |
| Medium | Wide | Medium/Long | **ROLLBACK** |
| Medium | Moderate/Narrow | Any | Fix forward |
| Low | Any | Any | Fix forward |

## Rollback Methods

### Method 1: Revert Deployment (Fastest)

**When:** Infrastructure supports instant rollback (Kubernetes, blue-green, etc.)

```bash
# Kubernetes rollback
kubectl rollout undo deployment/<name>

# Verify rollback
kubectl rollout status deployment/<name>
```

**Time:** 1-5 minutes
**Risk:** Low (returns to known good state)

### Method 2: Redeploy Prior Version

**When:** Need to deploy a specific known-good version

```bash
# Deploy specific version
kubectl set image deployment/<name> \
  container=image:v1.2.2

# Or via Helm
helm rollback <release> <revision>
```

**Time:** 5-15 minutes
**Risk:** Low

### Method 3: Feature Flag Disable

**When:** Issue is isolated to a flagged feature

```bash
# Disable feature flag (platform-specific)
# LaunchDarkly, Unleash, or custom flag service
```

**Time:** Seconds to 1 minute
**Risk:** Very low (no deployment needed)

### Method 4: Git Revert + Deploy

**When:** Need audit trail of the rollback decision

```bash
# Revert the problematic commit(s)
git revert <commit-sha>

# Push and trigger CI/CD
git push origin main
```

**Time:** 15-30 minutes (includes CI/CD pipeline)
**Risk:** Medium (new deployment through full pipeline)

## Post-Rollback Checklist

- [ ] Verify service is healthy (metrics, logs, error rates)
- [ ] Notify stakeholders of rollback and status
- [ ] Update status page if applicable
- [ ] Create incident ticket with timeline
- [ ] Identify root cause of the issue
- [ ] Write fix with comprehensive tests
- [ ] Verify fix in staging before re-deployment
- [ ] Schedule postmortem if severity was High or Critical

## Database Rollback Considerations

### Safe to Roll Back

- Additive schema changes (new columns with defaults, new tables)
- Application code that works with both old and new schema (expand/contract)

### Unsafe to Roll Back

- Destructive schema changes (column drops, renames, type changes)
- Data migrations that transformed existing data
- Constraint additions that would fail on current data

### Coordination Strategy

1. **Before deployment:** Verify rollback script exists and is tested
2. **During rollback:** Execute database rollback BEFORE application rollback
3. **After rollback:** Verify data integrity with validation queries
4. **Prevention:** Always use expand/contract pattern for schema changes

## Escalation Triggers

Escalate to engineering leadership if:

- Rollback fails or introduces new issues
- Multiple rollback attempts needed within 24 hours
- Data loss or corruption is suspected
- Security breach is confirmed
- SLA violation is imminent or confirmed
