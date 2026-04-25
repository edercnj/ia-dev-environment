# Backup Strategy Matrix

Recommended backup strategies by database type, environment, and RTO/RPO targets.

## Strategy by Database Type

| Database | Full Backup | Incremental | Point-in-Time | Tool |
|----------|------------|-------------|---------------|------|
| PostgreSQL | pg_dump / pg_basebackup | WAL archiving | WAL replay | pgBackRest, Barman |
| MySQL | mysqldump / xtrabackup | binlog | binlog replay | Percona XtraBackup |
| MongoDB | mongodump | oplog | oplog replay | MongoDB Ops Manager |
| Oracle | RMAN full | RMAN incremental | RMAN PITR | Oracle RMAN |
| SQL Server | Full backup | Differential / Log | Log restore | SQL Server Agent |

## Strategy by Environment

| Environment | Full Frequency | Incremental | Retention | Verification |
|-------------|---------------|-------------|-----------|--------------|
| Production | Daily | Every 15 min | 30 days | Weekly restore test |
| Staging | Weekly | Daily | 7 days | Monthly restore test |
| Development | Weekly | None | 3 days | On demand |
| DR Site | Continuous replication | N/A | Same as production | Quarterly failover test |

## RTO/RPO Decision Matrix

| RTO Target | RPO Target | Recommended Strategy | Cost |
|------------|------------|---------------------|------|
| < 15 min | < 1 min | Synchronous replication + automated failover | High |
| < 1 hour | < 15 min | Async replication + WAL shipping + automated failover | Medium-High |
| < 4 hours | < 1 hour | Daily full + incremental + PITR | Medium |
| < 24 hours | < 4 hours | Daily full + periodic incremental | Low-Medium |
| < 72 hours | < 24 hours | Daily full backup only | Low |

## Backup Verification Checklist

| Check | Frequency | Method |
|-------|-----------|--------|
| Backup completion | Every backup | Automated monitoring |
| Backup integrity | Daily | Checksum verification |
| Restore test | Weekly (prod), Monthly (staging) | Full restore to test environment |
| PITR test | Monthly | Restore to specific timestamp |
| DR failover | Quarterly | Full failover drill |

## Storage Considerations

| Factor | Recommendation |
|--------|---------------|
| Encryption | AES-256 at rest, TLS in transit |
| Compression | Enable for all backups (50-80% reduction typical) |
| Offsite copies | Minimum 1 copy in different region |
| Retention automation | Automated cleanup of expired backups |
| Cost optimization | Tier older backups to cold storage |
