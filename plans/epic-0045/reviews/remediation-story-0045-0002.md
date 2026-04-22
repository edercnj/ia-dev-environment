# Review Remediation Tracker — story-0045-0002

**5 findings pending remediation** (all LOW severity — no CRITICAL/HIGH/MEDIUM)

## Findings Tracker

| Finding ID | Engineer | Severity | Description | Status | Fix Commit SHA |
|------------|----------|----------|-------------|--------|----------------|
| FIND-001 | DevOps | LOW | Pre-existing: Dockerfile eclipse-temurin tags not pinned to digest (floating :21-jdk-alpine, :21-jre-alpine) | Open | |
| FIND-002 | DevOps | LOW | No K8s resource limits defined (N/A — tool not deployed to K8s) | Open | |
| FIND-003 | DevOps | LOW | No K8s health probes (N/A — HEALTHCHECK present in Dockerfile; K8s not applicable) | Open | |
| FIND-004 | DevOps | LOW | .dockerignore not verified/updated in this story's scope | Open | |
| FIND-005 | QA | LOW | Minor: RulesAssemblerCiWatchTest could consolidate content assertions into @ParameterizedTest | Open | |
| FIND-006 | Tech Lead | LOW | Rule20AuditTest.java: unused ArrayList/List imports | Fixed | 56cc3d755* |

*FIND-006 fixed inline during Tech Lead review — unused imports removed, tests remain GREEN.

## Remediation Summary

| Status | Count |
|--------|-------|
| Open | 5 |
| In Progress | 0 |
| Fixed | 1 |

> All remaining open findings are LOW severity and pre-existing infrastructure concerns
> unrelated to story-0045-0002's scope (rule file + audit script). These do not
> block merge per the DoD checklist (no CRITICAL or HIGH findings).
