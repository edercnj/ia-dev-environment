# On-Call Handbook

## Before Your Shift

### Preparation Checklist

- [ ] Verify VPN and access to production environments
- [ ] Confirm alerting tool notifications are enabled (PagerDuty, OpsGenie, etc.)
- [ ] Review shift log from previous on-call engineer
- [ ] Check current incident status and any ongoing mitigation
- [ ] Verify access to runbooks and documentation
- [ ] Test escalation contacts are reachable

### Required Tools

| Tool | Purpose | Access Verification |
|------|---------|-------------------|
| Monitoring dashboard | Real-time system health | Login and check dashboards load |
| Log aggregator | Investigate errors and patterns | Run sample query |
| Alerting platform | Receive and manage pages | Verify notification settings |
| Communication channel | Incident coordination | Join on-call channel |
| Deployment pipeline | Rollback capability | Verify pipeline access |
| Runbook repository | Step-by-step procedures | Confirm latest version available |

## Page Response Workflow

### Step 1: Acknowledge (within SLA)

| Severity | Acknowledgment SLA |
|---------|-------------------|
| SEV-1 | 5 minutes |
| SEV-2 | 15 minutes |
| SEV-3 | 30 minutes |
| SEV-4 | Next business day |

### Step 2: Assess

1. Read the alert description and context
2. Check monitoring dashboards for scope of impact
3. Determine if this is a known issue (check runbooks)
4. Classify severity based on user impact

### Step 3: Mitigate

1. Apply known fix from runbook if available
2. If no runbook exists, attempt standard mitigations:
   - Rollback recent deployment
   - Restart affected service instances
   - Toggle feature flags for the affected feature
   - Scale up resources if capacity-related
3. If mitigation fails, escalate immediately

### Step 4: Communicate

- Update incident channel with status every 15 minutes
- Notify stakeholders based on severity
- Update status page for user-facing issues
- Document all actions taken with timestamps

### Step 5: Resolve

- Verify the issue is resolved via monitoring
- Confirm with affected teams or users
- Document resolution in the incident ticket
- Set up monitoring for recurrence

## Escalation Procedures

### When to Escalate

- Unable to mitigate within 30 minutes
- Impact is broader than initially assessed
- Required access or expertise not available
- Multiple simultaneous incidents

### Escalation Path

```
On-Call Engineer
  -> Team Lead (15 min without resolution)
    -> Engineering Manager (30 min without resolution)
      -> VP Engineering (SEV-1 only, 1 hour without resolution)
```

### Cross-Team Escalation

When the issue spans multiple services:

1. Identify the owning team via service catalog
2. Page the on-call engineer for that team
3. Establish a bridge call for multi-team coordination
4. Designate one incident commander

## Post-Incident Actions

### Immediate (within 24 hours)

- [ ] Write incident summary (what happened, impact, resolution)
- [ ] Create action items for preventing recurrence
- [ ] Update runbooks if the issue was not documented
- [ ] Schedule postmortem if severity >= SEV-2

### Follow-Up (within 1 week)

- [ ] Complete blameless postmortem document
- [ ] Present findings at team postmortem review
- [ ] Assign and track action items
- [ ] Update alerting thresholds if too noisy or too quiet

## Self-Care

### During On-Call

- Take breaks between non-critical incidents
- Eat regular meals and stay hydrated
- If woken up at night, take compensatory rest
- Delegate or escalate if overwhelmed

### After High-Stress Shifts

- Minimum 8 hours rest after SEV-1 incidents
- Debrief with team lead about the experience
- Request shift swap if consecutive stressful rotations
- Utilize company support resources if needed

### Workload Balance

| Metric | Healthy Threshold | Action if Exceeded |
|--------|------------------|-------------------|
| Pages per shift | < 2 per hour average | Tune alerting thresholds |
| Night pages per week | < 2 total | Review alert severity routing |
| SEV-1 per month | < 1 per engineer | Investigate systemic issues |
| On-call hours per quarter | < 168 hours | Add to rotation pool |
