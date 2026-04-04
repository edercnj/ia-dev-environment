# Postmortem Report

## Incident Summary

| Field | Value |
|-------|-------|
| Incident ID | `INC-XXXX` |
| Severity | `SEV1` / `SEV2` / `SEV3` / `SEV4` |
| Date | `YYYY-MM-DD` |
| Duration | `X hours Y minutes` |
| Status | Resolved / Monitoring |
| Affected Services | `service-name` |
| Users Affected | `N users` / `N% of traffic` |
| Resolution | Brief description of resolution |

### Executive Summary

Provide a 2-3 sentence summary of the incident, its impact, and how it was resolved.

## Timeline

| Time (UTC) | Event | Details |
|------------|-------|---------|
| `YYYY-MM-DD HH:MM` | Detected | How the incident was first detected |
| `YYYY-MM-DD HH:MM` | Triaged | Severity assigned, responders engaged |
| `YYYY-MM-DD HH:MM` | Mitigated | Initial mitigation applied |
| `YYYY-MM-DD HH:MM` | Resolved | Full resolution confirmed |
| `YYYY-MM-DD HH:MM` | Post-check | Verification that resolution is stable |

## Root Cause Analysis

### 5 Whys

| Step | Question | Answer |
|------|----------|--------|
| Why #1 | Why did the incident occur? | `[Answer]` |
| Why #2 | Why did [answer from #1] happen? | `[Answer]` |
| Why #3 | Why did [answer from #2] happen? | `[Answer]` |
| Why #4 | Why did [answer from #3] happen? | `[Answer]` |
| Why #5 | Why did [answer from #4] happen? | `[Root cause]` |

### Root Cause Summary

Describe the root cause in one paragraph. Include the technical details of what went wrong and why.

## Impact Assessment

| Metric | Value |
|--------|-------|
| Total Downtime | `X hours Y minutes` |
| Users Affected | `N users` |
| Revenue Impact | `$X` or `N/A` |
| SLO Burn | `X% of monthly error budget` |
| Requests Failed | `N requests` |
| Data Loss | `None` / `Description` |

### Impact Details

Provide additional context on how the incident affected users, business operations, and SLOs.

## Contributing Factors

List the factors that contributed to the incident or extended its duration:

- [ ] **Technical Debt**: Describe any technical debt that contributed
- [ ] **Monitoring Gaps**: Were there missing alerts or dashboards?
- [ ] **Documentation Gaps**: Was runbook or documentation missing/outdated?
- [ ] **Process Gaps**: Were there missing processes or communication breakdowns?
- [ ] **Testing Gaps**: Were there missing test cases that would have caught this?
- [ ] **Capacity Planning**: Was the issue related to under-provisioned resources?

## Action Items

| # | Action | Owner | Deadline | Priority | Status |
|---|--------|-------|----------|----------|--------|
| 1 | `[Describe corrective action]` | `@owner` | `YYYY-MM-DD` | P0 | Open |
| 2 | `[Describe preventive action]` | `@owner` | `YYYY-MM-DD` | P1 | Open |
| 3 | `[Describe monitoring improvement]` | `@owner` | `YYYY-MM-DD` | P2 | Open |
| 4 | `[Describe documentation update]` | `@owner` | `YYYY-MM-DD` | P3 | Open |

### Priority Definitions

| Priority | Definition | SLA |
|----------|-----------|-----|
| P0 | Must fix immediately -- prevents recurrence of SEV1/SEV2 | 7 days |
| P1 | High priority -- significant risk reduction | 14 days |
| P2 | Medium priority -- improvement to monitoring or process | 30 days |
| P3 | Low priority -- nice-to-have improvements | 60 days |

## Lessons Learned

### What Went Well

- List things that worked well during the incident response

### What Could Be Improved

- List things that did not work well and need improvement

### Where We Got Lucky

- List factors that could have made the incident worse

## Prevention Measures

### Short-Term (1-2 weeks)

- [ ] Immediate fixes to prevent recurrence
- [ ] Additional monitoring and alerting

### Medium-Term (1-3 months)

- [ ] Architectural improvements
- [ ] Process improvements
- [ ] Automation of manual steps

### Long-Term (3-6 months)

- [ ] Systemic changes to prevent similar classes of incidents
- [ ] Infrastructure improvements
- [ ] Training and knowledge sharing
