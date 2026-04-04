# Migration Safety Checklist

Pre-flight checklist for production database migrations. Complete ALL items before executing.

## Pre-Migration

- [ ] Migration tested against production-like data volume
- [ ] Rollback script written and tested
- [ ] Migration execution time measured (must complete within maintenance window)
- [ ] Backward compatibility verified (application works with both old and new schema)
- [ ] No destructive changes without expand/contract pattern
- [ ] Database backup taken and verified restorable

## Schema Review

- [ ] Column additions are nullable or have defaults
- [ ] No column renames (use expand/contract)
- [ ] No type narrowing (use expand/contract)
- [ ] Index additions use CONCURRENTLY where supported
- [ ] Foreign keys have matching indexes on referencing columns
- [ ] Constraints are deferrable where needed for data migration

## Data Migration

- [ ] Batch processing for large data moves (avoid single transaction)
- [ ] Progress monitoring in place (row counts, elapsed time)
- [ ] Idempotent: safe to re-run if interrupted
- [ ] No full table locks during data migration
- [ ] Estimated row count and execution time documented

## Rollback Plan

- [ ] Rollback script tested on staging
- [ ] Rollback does not lose data written after migration
- [ ] Rollback decision criteria defined (error rates, latency thresholds)
- [ ] Rollback communication plan prepared
- [ ] Maximum rollback time documented and within SLA

## Monitoring

- [ ] Database metrics dashboard open during migration
- [ ] Query performance baselines recorded
- [ ] Error rate alerting configured
- [ ] Lock wait timeout monitoring active
- [ ] Replication lag monitoring (if applicable)

## Communication

- [ ] Team notified of migration window
- [ ] Stakeholders informed of potential impact
- [ ] On-call engineer available during migration
- [ ] Incident channel prepared for real-time updates
- [ ] Post-migration verification plan documented

## Post-Migration

- [ ] Application health checks passing
- [ ] Query performance within baseline thresholds
- [ ] No increase in error rates
- [ ] Replication caught up (if applicable)
- [ ] Migration recorded in change log
