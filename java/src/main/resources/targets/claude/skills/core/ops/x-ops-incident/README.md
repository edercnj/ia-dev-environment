# x-ops-incident

> Guides incident response with severity-based checklists, communication templates, and postmortem triggers. Interactive guide for SEV1-SEV4 incidents covering classification, response coordination, and action item tracking.

| | |
|---|---|
| **Category** | Operations |
| **Invocation** | `/x-ops-incident [severity SEV1\|SEV2\|SEV3\|SEV4] [--postmortem] [--notify]` |
| **Reads** | sre-practices |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Provides an interactive incident response guide that walks the team through the complete process from detection to resolution. Classifies incident severity (SEV1-SEV4), loads severity-specific checklists, coordinates communication with status page, Slack, and email templates, triggers postmortem generation for SEV1/SEV2 incidents, and tracks action items with owners and deadlines.

## Usage

```
/x-ops-incident
/x-ops-incident SEV1
/x-ops-incident SEV2 --postmortem
/x-ops-incident SEV3 --notify
```

## Workflow

1. **Classify** -- Analyze impact and classify severity (SEV1-SEV4)
2. **Load** -- Load severity-specific checklist from SRE Practices knowledge pack
3. **Guide** -- Conduct team through Detection, Triage, Mitigation, and Resolution
4. **Communicate** -- Generate communication templates (status page, Slack, email)
5. **Postmortem** -- Generate postmortem document (SEV1/SEV2 or when --postmortem)
6. **Track** -- Register action items with owners, deadlines, and priority

## See Also

- [x-ops-troubleshoot](../x-ops-troubleshoot/) -- Diagnoses errors and failures that may escalate to incidents
- [x-perf-profile](../x-perf-profile/) -- Profiling for performance-related incidents
