# Data Migration Plan — {{ project_name }}

## Migration Summary

| Field | Value |
|-------|-------|
| Description | [Brief description of the migration] |
| Affected tables/collections | [List of affected data structures] |
| Estimated duration | [Time estimate for complete migration] |
| Migration type | [Schema-only / Data + Schema / Data-only] |
| Database | {{ database_name }} |
| Migration tool | {{ migration_name }} |
| Ticket/Issue reference | [Link to story or issue] |

## Risk Assessment

| Risk Factor | Assessment |
|-------------|------------|
| Data volume | [Number of rows/documents affected] |
| Downtime window | [Required downtime, if any] |
| Rollback complexity | LOW / MEDIUM / HIGH |
| Data loss risk | NONE / LOW / MEDIUM / HIGH |
| Dependencies | [Other services or migrations that must complete first] |

## Pre-Migration Steps

- [ ] Backup verified and tested
- [ ] Migration tested in staging environment with production-like data
- [ ] Communication sent to affected teams
- [ ] Monitoring dashboards prepared
- [ ] Rollback script tested and verified
- [ ] Maintenance window scheduled (if downtime required)
{% if migration_name == "flyway" %}

### Flyway Pre-Migration

```bash
# Verify current schema version
flyway info

# Validate pending migrations
flyway validate

# Create backup before migration
pg_dump -h <host> -U <user> -d <database> > backup_pre_migration.sql
```
{% endif %}
{% if migration_name == "alembic" %}

### Alembic Pre-Migration

```bash
# Check current revision
alembic current

# View pending migrations
alembic history --verbose

# Create backup before migration
pg_dump -h <host> -U <user> -d <database> > backup_pre_migration.sql
```
{% endif %}
{% if migration_name == "mongock" %}

### Mongock Pre-Migration

```bash
# Verify current changelog state
# Check mongockChangeLog collection for applied changes
mongosh --eval "db.mongockChangeLog.find().sort({timestamp: -1}).limit(5)"

# Create backup before migration
mongodump --host <host> --db <database> --out backup_pre_migration/
```
{% endif %}
{% if migration_name == "prisma" %}

### Prisma Pre-Migration

```bash
# Check migration status
npx prisma migrate status

# Validate schema
npx prisma validate

# Create backup before migration
pg_dump -h <host> -U <user> -d <database> > backup_pre_migration.sql
```
{% endif %}
{% if migration_name == "golang-migrate" %}

### golang-migrate Pre-Migration

```bash
# Check current migration version
migrate -path ./migrations -database "$DATABASE_URL" version

# Create backup before migration
pg_dump -h <host> -U <user> -d <database> > backup_pre_migration.sql
```
{% endif %}
{% if migration_name == "diesel" %}

### Diesel Pre-Migration

```bash
# Check pending migrations
diesel migration pending

# List applied migrations
diesel migration list

# Create backup before migration
pg_dump -h <host> -U <user> -d <database> > backup_pre_migration.sql
```
{% endif %}

## Expand Phase

- [ ] New columns/tables created ({{ migration_name }} migration script)
- [ ] Application code deployed reading from old, writing to both old and new
- [ ] Data consistency check between old and new structures
- [ ] Performance metrics within acceptable range
{% if migration_name == "flyway" %}

### Flyway Expand Migration

```bash
# Apply expand migration
flyway migrate

# Verify migration was applied
flyway info
```
{% endif %}
{% if migration_name == "alembic" %}

### Alembic Expand Migration

```bash
# Apply expand migration
alembic upgrade head

# Verify current revision
alembic current
```
{% endif %}
{% if migration_name == "mongock" %}

### Mongock Expand Migration

```bash
# Mongock runs automatically on application startup
# Verify changelog was applied
mongosh --eval "db.mongockChangeLog.find({changeId: '<expand-change-id>'}).pretty()"
```
{% endif %}
{% if migration_name == "prisma" %}

### Prisma Expand Migration

```bash
# Apply expand migration
npx prisma migrate deploy

# Verify migration status
npx prisma migrate status
```
{% endif %}
{% if migration_name == "golang-migrate" %}

### golang-migrate Expand Migration

```bash
# Apply expand migration
migrate -path ./migrations -database "$DATABASE_URL" up

# Verify current version
migrate -path ./migrations -database "$DATABASE_URL" version
```
{% endif %}
{% if migration_name == "diesel" %}

### Diesel Expand Migration

```bash
# Apply expand migration
diesel migration run

# Verify migration was applied
diesel migration list
```
{% endif %}

## Migration Phase

| Field | Value |
|-------|-------|
| Data transformation script | [Script path or inline] |
| Batch size | [Number of rows per batch] |
| Progress monitoring | [How to track progress -- logs, metrics, queries] |
| Estimated batches | [Total rows / batch size] |
| Pause/resume strategy | [How to pause and resume safely] |

## Contract Phase

- [ ] Application code deployed reading and writing only to new structures
- [ ] Old columns/tables marked for removal
- [ ] Cleanup migration script prepared
- [ ] Old structures dropped after observation period
{% if migration_name == "flyway" %}

### Flyway Contract Migration

```bash
# Apply contract migration (drop old structures)
flyway migrate

# Verify final schema state
flyway info
```
{% endif %}
{% if migration_name == "alembic" %}

### Alembic Contract Migration

```bash
# Apply contract migration
alembic upgrade head

# Verify final state
alembic current
```
{% endif %}
{% if migration_name == "mongock" %}

### Mongock Contract Migration

```bash
# Mongock runs automatically on application startup
# Verify contract changelog was applied
mongosh --eval "db.mongockChangeLog.find({changeId: '<contract-change-id>'}).pretty()"
```
{% endif %}
{% if migration_name == "prisma" %}

### Prisma Contract Migration

```bash
# Apply contract migration
npx prisma migrate deploy

# Verify final state
npx prisma migrate status
```
{% endif %}
{% if migration_name == "golang-migrate" %}

### golang-migrate Contract Migration

```bash
# Apply contract migration
migrate -path ./migrations -database "$DATABASE_URL" up

# Verify final version
migrate -path ./migrations -database "$DATABASE_URL" version
```
{% endif %}
{% if migration_name == "diesel" %}

### Diesel Contract Migration

```bash
# Apply contract migration
diesel migration run

# Verify final state
diesel migration list
```
{% endif %}

## Validation Queries
{% if database_name == "postgresql" or database_name == "mysql" %}

### SQL Validation Queries

```sql
-- Row count verification
SELECT COUNT(*) AS row_count FROM <table_name>;

-- Data integrity check
SELECT COUNT(*) FROM <new_table>
WHERE <primary_key> NOT IN (SELECT <primary_key> FROM <old_table>);

-- Constraint validation
SELECT conname, contype
FROM pg_constraint
WHERE conrelid = '<table_name>'::regclass;

-- Null check on required columns
SELECT COUNT(*) FROM <table_name>
WHERE <required_column> IS NULL;

-- Business rule validation
SELECT COUNT(*) FROM <table_name>
WHERE <business_invariant_condition>;
```
{% endif %}
{% if database_name == "mongodb" %}

### MongoDB Validation Queries

```javascript
// Row count verification
db.<collection>.countDocuments();

// Data integrity check
db.<new_collection>.aggregate([
  { $lookup: {
    from: "<old_collection>",
    localField: "_id",
    foreignField: "_id",
    as: "matched"
  }},
  { $match: { matched: { $size: 0 } } },
  { $count: "orphaned_documents" }
]);

// Schema validation check
db.<collection>.find({
  $or: [
    { <required_field>: { $exists: false } },
    { <required_field>: null }
  ]
}).count();

// Business rule validation
db.<collection>.find({
  <business_invariant_condition>
}).count();
```
{% endif %}

## Rollback Plan

| Field | Value |
|-------|-------|
| Rollback migration script | [Path or inline -- reverse of expand phase] |
| Rollback deployment | [Application version to redeploy] |
| Data restore procedure | [Steps if rollback migration insufficient] |
| Rollback time estimate | [How long to fully rollback] |
| Rollback decision criteria | [When to trigger rollback vs fix-forward] |
{% if migration_name == "flyway" %}

### Flyway Rollback

```bash
# Undo last migration (Flyway Teams required)
flyway undo

# Or restore from backup
psql -h <host> -U <user> -d <database> < backup_pre_migration.sql

# Verify rollback state
flyway info
```
{% endif %}
{% if migration_name == "alembic" %}

### Alembic Rollback

```bash
# Downgrade to previous revision
alembic downgrade -1

# Or downgrade to specific revision
alembic downgrade <revision_id>

# Verify rollback state
alembic current
```
{% endif %}
{% if migration_name == "mongock" %}

### Mongock Rollback

```bash
# Restore from backup
mongorestore --host <host> --db <database> backup_pre_migration/

# Verify rollback state
mongosh --eval "db.mongockChangeLog.find().sort({timestamp: -1}).limit(5)"
```
{% endif %}
{% if migration_name == "prisma" %}

### Prisma Rollback

```bash
# Rollback last migration
npx prisma migrate resolve --rolled-back <migration_name>

# Or restore from backup
psql -h <host> -U <user> -d <database> < backup_pre_migration.sql

# Verify rollback state
npx prisma migrate status
```
{% endif %}
{% if migration_name == "golang-migrate" %}

### golang-migrate Rollback

```bash
# Rollback last migration
migrate -path ./migrations -database "$DATABASE_URL" down 1

# Verify rollback state
migrate -path ./migrations -database "$DATABASE_URL" version
```
{% endif %}
{% if migration_name == "diesel" %}

### Diesel Rollback

```bash
# Revert last migration
diesel migration revert

# Verify rollback state
diesel migration list
```
{% endif %}

## Post-Migration

- [ ] Monitoring dashboard reviewed (error rate, query latency)
- [ ] Alerting rules verified for new structures
- [ ] Cleanup tasks scheduled (drop old columns after observation period)
- [ ] Documentation updated (schema diagrams, data dictionary)
- [ ] Lessons learned documented
