# Amazon Neptune — Migration Patterns

## Overview

Neptune is **schema-free** with no native migration framework. Data evolution requires bulk loading, streaming, or application-level strategies.

| Approach | Best For | Dependency |
|----------|----------|------------|
| **Neptune Bulk Loader** | Initial load, large data sets | S3 + IAM role |
| **Neptune Streams** | CDC, incremental sync | Neptune Streams API |
| **Gremlin scripts** | Incremental schema changes | `gremlin-driver` |
| **Re-ingestion** | Major model changes | S3 bulk loader + new cluster |

## Neptune Bulk Loader

### CSV Format (Gremlin)

**Vertices file** (`vertices.csv`):

```csv
~id,~label,name:String,email:String,createdAt:Date
usr-001,user,Alice,alice@example.com,2024-01-15
usr-002,user,Bob,bob@example.com,2024-02-20
prod-001,product,Widget,null,2024-01-01
```

**Edges file** (`edges.csv`):

```csv
~id,~from,~to,~label,date:Date,amount:Double
e-001,usr-001,prod-001,purchased,2024-03-20,99.99
e-002,usr-002,prod-001,purchased,2024-03-21,99.99
```

### N-Triples Format (SPARQL)

```ntriples
<http://example.com/user-001> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://schema.org/Person> .
<http://example.com/user-001> <http://schema.org/name> "Alice" .
<http://example.com/user-001> <http://schema.org/email> "alice@example.com" .
```

### Loader API Call

```bash
curl -X POST https://your-neptune-endpoint:8182/loader \
  -H 'Content-Type: application/json' \
  -d '{
    "source": "s3://my-bucket/graph-data/",
    "format": "csv",
    "iamRoleArn": "arn:aws:iam::123456789012:role/NeptuneLoadRole",
    "region": "us-east-1",
    "failOnError": "FALSE",
    "parallelism": "MEDIUM",
    "updateSingleCardinalityProperties": "TRUE"
  }'
```

### Loader Parameters

| Parameter | Values | Default | Purpose |
|-----------|--------|---------|---------|
| `format` | csv, ntriples, nquads, turtle, rdfxml | Required | Input format |
| `failOnError` | TRUE, FALSE | TRUE | Stop or continue on errors |
| `parallelism` | LOW, MEDIUM, HIGH, OVERSUBSCRIBE | MEDIUM | Load speed vs resource use |
| `updateSingleCardinalityProperties` | TRUE, FALSE | FALSE | Overwrite existing properties |
| `queueRequest` | TRUE, FALSE | FALSE | Queue if another load running |

## Neptune Streams for CDC

Neptune Streams records every mutation in a change log.

### Enable Streams

```bash
aws neptune modify-db-cluster \
  --db-cluster-identifier my-cluster \
  --neptune-parameters "Name=neptune_streams,Value=1"
```

### Poll Stream API

```bash
curl https://your-neptune-endpoint:8182/propertygraph/stream \
  -H 'Accept: application/json' \
  -d '{"limit": 100, "commitNum": 1}'
```

### Stream Event Format

```json
{
  "lastEventId": {"commitNum": 5, "opNum": 1},
  "lastTrxTimestamp": 1710000000000,
  "format": "PG_JSON",
  "records": [
    {
      "commitTimestamp": 1710000000000,
      "eventId": {"commitNum": 5, "opNum": 1},
      "data": {
        "id": "usr-003",
        "type": "vl",
        "key": "label",
        "value": {"value": "user", "dataType": "String"}
      },
      "op": "ADD"
    }
  ]
}
```

### CDC Use Cases

| Use Case | Implementation |
|----------|---------------|
| Sync to search index | Lambda polls stream, writes to OpenSearch |
| Audit log | Lambda polls stream, writes to DynamoDB/S3 |
| Cache invalidation | Lambda polls stream, invalidates cache keys |
| Cross-region replication | Lambda polls stream, writes to target Neptune |

## Schema Evolution (Schema-Free)

### Adding New Properties

```groovy
// Add new property to existing vertices
g.V().hasLabel('user')
  .has('email')
  .not(has('emailVerified'))
  .property('emailVerified', false)
```

### Renaming Properties (Copy + Delete)

```groovy
// Copy property value to new name
g.V().hasLabel('user')
  .has('userName')
  .property('name', values('userName'))
  .sideEffect(properties('userName').drop())
```

### Changing Edge Types

```groovy
// Add new edge type, then remove old
g.V().hasLabel('user').as('u')
  .outE('bought').as('e')
  .inV().as('p')
  .addE('purchased').from('u').to('p')
  .property('date', select('e').values('date'))
  .select('e').drop()
```

## Re-Ingestion Strategy

For major model changes, rebuild from scratch:

| Step | Action | Detail |
|------|--------|--------|
| 1 | Export current data | Query or use Neptune export utility |
| 2 | Transform data | ETL pipeline (Glue, Spark, Lambda) |
| 3 | Validate transformed data | Schema validation, referential integrity |
| 4 | Create new cluster | Fresh Neptune instance |
| 5 | Bulk load new data | S3 bulk loader |
| 6 | Validate new cluster | Run integration tests |
| 7 | Switch traffic | Update endpoint in application |
| 8 | Decommission old cluster | After validation period |

## Rules

| Rule | Detail |
|------|--------|
| Idempotent | Scripts must be safe to run multiple times |
| S3 staging | Always stage bulk load data in S3 with versioning |
| IAM roles | Neptune bulk loader requires IAM role with S3 read access |
| VPC access | Neptune is VPC-only; no public internet access |
| Backup before migration | Create cluster snapshot before any mutation |
| Monitor loader status | Poll loader status API until completion |
