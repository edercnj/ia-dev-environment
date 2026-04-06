# OpenSearch — Migration Patterns

## Overview

OpenSearch schema management uses ISM policies, reindex operations, and alias swapping:

| Approach | Use Case | Downtime |
|----------|----------|----------|
| Alias swap + reindex | Mapping changes, analyzer updates | Zero downtime |
| ISM rollover | Time-based index rotation | Zero downtime |
| Update by query | Add/modify field values | Zero downtime |
| Snapshot restore | Disaster recovery, migration | Depends on size |
| Plugin management | Feature upgrades | Rolling restart |

## ISM Policy Management

### Create ISM Policy

```json
PUT /_plugins/_ism/policies/logs_policy
{
  "policy": {
    "description": "Log index lifecycle",
    "default_state": "hot",
    "states": [
      {
        "name": "hot",
        "actions": [{"rollover": {"min_size": "50gb", "min_index_age": "1d"}}],
        "transitions": [{"state_name": "warm", "conditions": {"min_index_age": "7d"}}]
      },
      {
        "name": "warm",
        "actions": [{"force_merge": {"max_num_segments": 1}}],
        "transitions": [{"state_name": "delete", "conditions": {"min_index_age": "90d"}}]
      },
      {
        "name": "delete",
        "actions": [{"delete": {}}]
      }
    ]
  }
}
```

### Attach ISM Policy to Existing Index

```json
POST /_plugins/_ism/add/logs-2024.01
{
  "policy_id": "logs_policy"
}
```

### Update ISM Policy

```json
PUT /_plugins/_ism/policies/logs_policy?if_seq_no=2&if_primary_term=1
{
  "policy": {
    "description": "Updated log lifecycle",
    "default_state": "hot",
    "states": []
  }
}
```

### Change Policy on Managed Index

```json
POST /_plugins/_ism/change_policy/logs-*
{
  "policy_id": "logs_policy_v2",
  "state": "hot"
}
```

## Reindex Operations

### Basic Reindex

```json
POST /_reindex
{
  "source": {"index": "products_v1"},
  "dest": {"index": "products_v2"}
}
```

### Reindex with Pipeline

```json
PUT /_ingest/pipeline/enrich_pipeline
{
  "processors": [
    {
      "set": {
        "field": "migrated_at",
        "value": "{{_ingest.timestamp}}"
      }
    },
    {
      "rename": {
        "field": "old_field",
        "target_field": "new_field",
        "ignore_missing": true
      }
    }
  ]
}

POST /_reindex
{
  "source": {"index": "products_v1"},
  "dest": {
    "index": "products_v2",
    "pipeline": "enrich_pipeline"
  }
}
```

### Remote Reindex (Cross-Cluster)

```json
POST /_reindex
{
  "source": {
    "remote": {
      "host": "https://old-cluster:9200",
      "username": "admin",
      "password": "admin"
    },
    "index": "products"
  },
  "dest": {"index": "products"}
}
```

## Snapshot and Restore

### Register Repository

```json
PUT /_snapshot/s3_backup
{
  "type": "s3",
  "settings": {
    "bucket": "opensearch-snapshots",
    "region": "us-east-1",
    "base_path": "snapshots"
  }
}
```

### Create and Restore

```json
// Create snapshot
PUT /_snapshot/s3_backup/snapshot_2024_01
{
  "indices": "products,orders",
  "ignore_unavailable": true,
  "include_global_state": false
}

// Check snapshot status
GET /_snapshot/s3_backup/snapshot_2024_01/_status

// Restore to different index name
POST /_snapshot/s3_backup/snapshot_2024_01/_restore
{
  "indices": "products",
  "rename_pattern": "(.+)",
  "rename_replacement": "restored_$1"
}
```

## Plugin Management

### Install/Remove Plugins (Requires Restart)

```bash
# Install plugin
bin/opensearch-plugin install analysis-icu

# Remove plugin
bin/opensearch-plugin remove analysis-icu

# List installed plugins
bin/opensearch-plugin list
```

### Rolling Plugin Upgrade

| Step | Action |
|------|--------|
| 1 | Disable shard allocation |
| 2 | Stop node, upgrade plugin, restart |
| 3 | Re-enable allocation, wait for green |
| 4 | Repeat for each node |

## Migration Versioning

```
opensearch/
|-- mappings/
|   |-- V001__products_initial.json
|   |-- V002__products_add_embedding.json
|   +-- V003__products_update_analyzer.json
|-- ism_policies/
|   |-- logs_policy.json
|   +-- metrics_policy.json
|-- templates/
|   |-- logs_template.json
|   +-- metrics_template.json
|-- pipelines/
|   +-- enrich_pipeline.json
+-- scripts/
    |-- migrate_v1_to_v2.sh
    +-- reindex_products.json
```

## Migration from Elasticsearch

### Compatibility Notes

| Feature | Elasticsearch | OpenSearch | Migration Impact |
|---------|-------------|-----------|-----------------|
| ILM | `_ilm` API | `_plugins/_ism` API | Policy format differs |
| Security | X-Pack Security | Security plugin (built-in) | Config migration needed |
| ML | ML plugin | ML Commons | API differences |
| k-NN | Approximate k-NN (8.x) | k-NN plugin (native) | Different mapping format |
| Alerting | Watcher | Alerting plugin | Rule migration needed |

### Snapshot-Based Migration

```json
// 1. Take snapshot from Elasticsearch (7.x compatible)
// 2. Register same repository in OpenSearch
PUT /_snapshot/migration_repo
{
  "type": "s3",
  "settings": {"bucket": "migration-snapshots"}
}

// 3. Restore snapshot
POST /_snapshot/migration_repo/es_snapshot/_restore
{
  "indices": "*",
  "ignore_unavailable": true
}
```

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| ISM policy without rollover | Unbounded index growth | Always include rollover in hot state |
| Reindex without async | Blocks client until complete | Use `wait_for_completion=false` |
| Direct index queries | Cannot migrate without app changes | Always use aliases |
| Plugin upgrade without rolling restart | Downtime | Upgrade one node at a time |
| Snapshot without `include_global_state: false` | Restores cluster settings | Exclude global state |
