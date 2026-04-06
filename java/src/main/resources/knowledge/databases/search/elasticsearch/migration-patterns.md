# Elasticsearch — Migration Patterns

## Overview

Elasticsearch has no DDL migration framework. Schema changes use the Reindex API, alias swapping, and Index Lifecycle Management (ILM).

| Approach | Use Case | Downtime |
|----------|----------|----------|
| Alias swap + reindex | Mapping changes, analyzer updates | Zero downtime |
| ILM rollover | Time-based index rotation | Zero downtime |
| Update by query | Add/modify field values | Zero downtime |
| Rolling upgrade | Cluster version upgrade | Zero downtime |

## Reindex API

### Basic Reindex

```json
POST /_reindex
{
  "source": {"index": "orders_v1"},
  "dest": {"index": "orders_v2"}
}
```

### Reindex with Transformation

```json
POST /_reindex
{
  "source": {"index": "orders_v1"},
  "dest": {"index": "orders_v2"},
  "script": {
    "source": """
      ctx._source.total_cents = (long)(ctx._source.remove('total_amount') * 100);
      ctx._source.migrated_at = new Date().toString();
    """
  }
}
```

### Reindex with Query Filter

```json
POST /_reindex
{
  "source": {
    "index": "orders_v1",
    "query": {"range": {"created_at": {"gte": "2024-01-01"}}}
  },
  "dest": {"index": "orders_v2"}
}
```

## Alias-Based Zero-Downtime Migration

### Step-by-Step

```json
// Step 1: Create new index with updated mapping
PUT /orders_v2
{
  "settings": {"number_of_shards": 3},
  "mappings": {
    "properties": {
      "total_cents": {"type": "long"},
      "description": {"type": "text", "analyzer": "custom_analyzer"}
    }
  }
}

// Step 2: Reindex data
POST /_reindex?wait_for_completion=false
{
  "source": {"index": "orders_v1"},
  "dest": {"index": "orders_v2"}
}

// Step 3: Check reindex progress
GET /_tasks?actions=*reindex&detailed=true

// Step 4: Atomic alias swap
POST /_aliases
{
  "actions": [
    {"remove": {"index": "orders_v1", "alias": "orders"}},
    {"add": {"index": "orders_v2", "alias": "orders"}}
  ]
}

// Step 5: Delete old index after verification
DELETE /orders_v1
```

## Index Lifecycle Management (ILM)

### ILM Policy

```json
PUT /_ilm/policy/logs_policy
{
  "policy": {
    "phases": {
      "hot": {
        "min_age": "0ms",
        "actions": {
          "rollover": {
            "max_primary_shard_size": "50gb",
            "max_age": "1d"
          },
          "set_priority": {"priority": 100}
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "shrink": {"number_of_shards": 1},
          "forcemerge": {"max_num_segments": 1},
          "set_priority": {"priority": 50}
        }
      },
      "cold": {
        "min_age": "30d",
        "actions": {
          "searchable_snapshot": {
            "snapshot_repository": "my_repository"
          },
          "set_priority": {"priority": 0}
        }
      },
      "delete": {
        "min_age": "90d",
        "actions": {"delete": {}}
      }
    }
  }
}
```

## Rolling Upgrade

| Step | Action |
|------|--------|
| 1 | Disable shard allocation: `PUT /_cluster/settings {"transient": {"cluster.routing.allocation.enable": "primaries"}}` |
| 2 | Stop and upgrade one node |
| 3 | Start upgraded node, wait for join |
| 4 | Re-enable allocation: `PUT /_cluster/settings {"transient": {"cluster.routing.allocation.enable": null}}` |
| 5 | Wait for green status: `GET /_cluster/health?wait_for_status=green` |
| 6 | Repeat for each node |

## Mapping Updates (Non-Breaking)

```json
// Add new field (non-breaking, no reindex needed)
PUT /orders/_mapping
{
  "properties": {
    "priority": {"type": "keyword"},
    "notes": {"type": "text"}
  }
}
```

| Change | Reindex Required? |
|--------|------------------|
| Add new field | No |
| Change analyzer | Yes |
| Change field type | Yes |
| Add sub-field to multi-field | No |
| Remove field | No (just stop indexing) |
| Change shard count | Yes (new index) |

## Snapshot and Restore

```json
// Register snapshot repository
PUT /_snapshot/my_backup
{
  "type": "s3",
  "settings": {
    "bucket": "es-snapshots",
    "region": "us-east-1"
  }
}

// Create snapshot
PUT /_snapshot/my_backup/snapshot_2024_01
{
  "indices": "orders,products",
  "ignore_unavailable": true
}

// Restore from snapshot
POST /_snapshot/my_backup/snapshot_2024_01/_restore
{
  "indices": "orders",
  "rename_pattern": "(.+)",
  "rename_replacement": "restored_$1"
}
```

## Migration Versioning

```
elasticsearch/
|-- mappings/
|   |-- V001__orders_initial.json
|   |-- V002__orders_add_priority.json
|   +-- V003__orders_change_analyzer.json
|-- ilm_policies/
|   +-- logs_policy.json
|-- templates/
|   +-- logs_template.json
+-- scripts/
    |-- migrate_v1_to_v2.sh
    +-- reindex_orders.json
```

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Direct index queries (no alias) | Cannot reindex without app changes | Always use aliases |
| In-place mapping change | Cannot change field type | Create new index, reindex, swap alias |
| Reindex without progress check | Unknown completion status | Use async reindex with task API |
| No ILM for time-based data | Unbounded index growth | Implement ILM with rollover |
| Snapshot without verification | Corrupted backups go unnoticed | Verify snapshots regularly |
