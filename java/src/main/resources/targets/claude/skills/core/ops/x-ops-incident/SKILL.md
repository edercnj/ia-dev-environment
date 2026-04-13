---
name: x-ops-incident
description: "Guides incident response with severity-based checklists, communication templates, and postmortem triggers. Interactive guide for SEV1-SEV4 incidents covering classification, response coordination, and action item tracking."
user-invocable: true
argument-hint: "[severity SEV1|SEV2|SEV3|SEV4] [--postmortem] [--notify]"
allowed-tools: Read, Write, Bash, Grep, Glob, Agent
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Incident Response

## Purpose

Provides an interactive incident response guide for {{PROJECT_NAME}} that walks the team through the complete process from detection to resolution. Classifies severity, loads severity-specific checklists, coordinates communication, triggers postmortems, and tracks action items.

## Triggers

- `/x-ops-incident` — start interactive severity classification
- `/x-ops-incident SEV1` — start SEV1 critical incident response
- `/x-ops-incident SEV2 --postmortem` — SEV2 incident with postmortem generation
- `/x-ops-incident SEV3 --notify` — SEV3 incident with communication templates
- `/x-ops-incident SEV1 --postmortem --notify` — full incident response workflow

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `severity` | positional | (interactive) | Severity level: `SEV1`, `SEV2`, `SEV3`, or `SEV4` |
| `--postmortem` | boolean | `false` | Generate postmortem document (auto-enabled for SEV1/SEV2) |
| `--notify` | boolean | `false` | Generate communication templates for all channels |

## Workflow

```
1. CLASSIFY     -> Analyze impact description and classify severity (SEV1-SEV4)
2. LOAD         -> Load severity-specific checklist from SRE Practices KP
3. GUIDE        -> Conduct team through Detection -> Triage -> Mitigation -> Resolution
4. COMMUNICATE  -> Generate communication templates (status page, Slack, email)
5. POSTMORTEM   -> Generate postmortem document from template (SEV1/SEV2 or --postmortem)
6. TRACK        -> Register action items with owners and deadlines
```

### Step 1 — Classify Severity

If severity is provided as argument, validate and use directly. If omitted, ask the user about the impact and suggest classification.

| Severity | Label | Criteria | Response Time | Update Frequency |
|----------|-------|----------|---------------|------------------|
| **SEV1** | Critical | Total service outage, significant financial loss, data breach affecting all users | 15 min | Every 30 min |
| **SEV2** | High | Major feature unavailable, severe performance degradation, large user impact | 30 min | Every 1 hour |
| **SEV3** | Medium | Minor feature impacted, workaround available, limited user impact | 4 hours | Every 4 hours |
| **SEV4** | Low | Cosmetic issue, minimal user impact, no business impact | Next business day | Daily |

**Severity Decision Criteria:**

- **User Impact**: How many users are affected? Is it all users or a subset?
- **Financial Impact**: Is there direct revenue loss or potential financial liability?
- **Data Impact**: Is there data loss, corruption, or unauthorized access?
- **Scope**: Is the issue isolated or widespread across regions/services?

### Step 2 — Load Checklist

Load the severity-specific checklist from the SRE Practices knowledge pack (`skills/sre-practices/`). Each severity level has different response requirements:

**SEV1 — Critical Checklist:**

- [ ] Incident Commander assigned within 5 minutes
- [ ] Response team assembled (engineering, on-call, management)
- [ ] Communication channel created (war room / bridge call)
- [ ] Initial status page update published
- [ ] Executive stakeholders notified within 15 minutes
- [ ] All hands on deck — pull in additional engineers as needed

**SEV2 — High Checklist:**

- [ ] On-call engineer acknowledged within 10 minutes
- [ ] Secondary engineer engaged if needed
- [ ] Communication channel created
- [ ] Status page update published within 30 minutes
- [ ] Engineering manager notified

**SEV3 — Medium Checklist:**

- [ ] On-call engineer acknowledged within 1 hour
- [ ] Workaround documented and communicated
- [ ] Ticket created for permanent fix
- [ ] Status page updated if user-facing

**SEV4 — Low Checklist:**

- [ ] Ticket created with appropriate priority
- [ ] Assigned to next sprint or backlog
- [ ] No immediate response required

### Step 3 — Guide Response

Conduct the team through the incident response flow. Use the `sre-engineer` agent via Agent tool for reliability expertise and validation.

#### Detection

- Identify the alert source (monitoring, user report, automated test)
- Record alert timestamp and affected service(s)
- Confirm initial severity assessment

#### Triage

- Determine root cause hypothesis
- Identify blast radius (affected services, users, regions)
- Assign roles: Incident Commander (SEV1/SEV2), Communication Lead, Technical Lead

#### Mitigation

- Apply immediate mitigation (rollback, feature flag, scaling, failover)
- Verify mitigation effectiveness
- Document mitigation steps taken

#### Resolution

- Implement permanent fix
- Verify resolution across all affected components
- Confirm service restoration to normal operation
- Update status page with resolution notice

### Step 4 — Communicate

Generate communication templates based on severity and channels. Frequency varies by severity level.

#### Status Page Template

```markdown
**[INVESTIGATING/IDENTIFIED/MONITORING/RESOLVED]** — {{PROJECT_NAME}}

**Impact:** [Description of user-facing impact]
**Affected Services:** [List of affected services]
**Current Status:** [What we know and what we are doing]
**Next Update:** [Time of next update based on severity]
```

#### Slack/Teams Internal Update Template

```markdown
:rotating_light: **Incident Update — [SEV level]**
**Service:** [affected service]
**Impact:** [description]
**Status:** [Investigating/Mitigating/Resolved]
**IC:** [Incident Commander name]
**Timeline:**
- [HH:MM] [Event description]
**Next Steps:** [what happens next]
**Next Update:** [time]
```

#### Email Stakeholder Notification Template

```markdown
Subject: [SEV level] Incident — [Brief description]

Dear Stakeholders,

We are currently experiencing [description of the issue]
affecting [scope of impact].

**Severity:** [SEV1/SEV2/SEV3/SEV4]
**Impact:** [User-facing impact description]
**Status:** [Current status]
**ETA for Resolution:** [Estimate or "Under investigation"]

We will provide updates every [frequency based on severity].

Best regards,
[Team Name]
```

**Update Frequency by Severity:**

| Severity | Update Frequency | Channels |
|----------|-----------------|----------|
| SEV1 | Every 30 min | Status Page, Slack, Email |
| SEV2 | Every 1 hour | Status Page, Slack, Email |
| SEV3 | Every 4 hours | Status Page, Slack |
| SEV4 | Daily | Slack |

### Step 5 — Postmortem

If `--postmortem` flag is provided, or severity is SEV1 or SEV2, generate a postmortem document from the `_TEMPLATE-POSTMORTEM.md` template.

**Postmortem is triggered when:**

- Severity is SEV1 (always)
- Severity is SEV2 (always)
- `--postmortem` flag is explicitly provided (any severity)

**Postmortem document is pre-filled with:**

- Incident timeline from the response flow
- Severity classification and impact assessment
- Mitigation and resolution steps taken
- Participants and their roles
- Action items identified during the response

If the postmortem template is not available, generate an inline postmortem with the basic structure:

```markdown
# Postmortem — [Incident Title]

## Incident Summary
| Field | Value |
|-------|-------|
| Severity | [SEV level] |
| Date | [Date] |
| Duration | [Duration] |
| Affected Services | [Services] |

## Timeline
[Pre-filled from incident response]

## Root Cause Analysis
[To be completed]

## Action Items
[Pre-filled from tracked items]

## Lessons Learned
[To be completed]
```

### Step 6 — Track Action Items

Register all action items identified during the incident response. Each action item includes:

| Field | Description |
|-------|-------------|
| ID | Sequential identifier (AI-001, AI-002, ...) |
| Description | What needs to be done |
| Owner | Person responsible |
| Deadline | Target completion date |
| Priority | HIGH / MEDIUM / LOW |
| Status | OPEN / IN_PROGRESS / DONE |

**Output format:**

```markdown
## Action Items

| ID | Description | Owner | Deadline | Priority | Status |
|----|-------------|-------|----------|----------|--------|
| AI-001 | [description] | [owner] | [date] | HIGH | OPEN |
| AI-002 | [description] | [owner] | [date] | MEDIUM | OPEN |
```

## Severity Definitions

| Severity | Label | Description | Response Time | Update Frequency | Postmortem Required |
|----------|-------|-------------|---------------|------------------|---------------------|
| **SEV1** | Critical | Total service outage, significant financial loss, data breach | 15 min | 30 min | Yes (always) |
| **SEV2** | High | Major feature unavailable, degraded experience for many users | 30 min | 1 hour | Yes (always) |
| **SEV3** | Medium | Minor feature impacted, workaround available | 4 hours | 4 hours | Only if --postmortem |
| **SEV4** | Low | Cosmetic issue, minimal impact | Next business day | Daily | Only if --postmortem |

## Communication Templates

See Step 4 above for complete templates per channel (Status Page, Slack/Teams, Email) and update frequency per severity level.

## Error Handling

| Scenario | Action |
|----------|--------|
| Severity not provided | Ask the user about the impact and suggest classification based on description |
| Invalid severity (e.g., SEV5) | Reject with message: "Invalid severity. Use SEV1, SEV2, SEV3, or SEV4" |
| `--postmortem` without template | Generate inline postmortem with basic structure |
| No Incident Commander available | Assign the requesting user as IC and recommend finding a replacement |
| Incomplete information | Proceed with available data, note gaps in postmortem |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-ops-troubleshoot | called-by | Escalates to this skill when an issue becomes a production incident |
| sre-engineer (agent) | calls | Delegates reliability expertise and checklist validation via Agent tool |
| sre-practices (KP) | reads | References `skills/sre-practices/` for incident management processes |

- Uses `_TEMPLATE-POSTMORTEM.md` for postmortem document generation. Fallback: inline postmortem with basic structure when template is absent.
- Uses `_TEMPLATE-INCIDENT-RESPONSE.md` for severity classification reference
- Can be used standalone or as part of on-call response workflow
