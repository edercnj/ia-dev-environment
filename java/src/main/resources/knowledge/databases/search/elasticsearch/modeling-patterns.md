# Elasticsearch — Modeling Patterns

## Version Matrix

| Version | Key Features | Notes |
|---------|-------------|-------|
| **7.17** | Last 7.x, security free tier | LTS |
| **8.x** | Native vector search, new Java client | Current |
| **8.12+** | ES|QL, improved k-NN, ELSER v2 | Latest |

## Index Design

### Index Creation

```json
PUT /orders
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "index.mapping.total_fields.limit": 200,
    "analysis": {
      "analyzer": {
        "product_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "asciifolding", "product_synonyms"]
        }
      },
      "filter": {
        "product_synonyms": {
          "type": "synonym",
          "synonyms": ["laptop,notebook", "phone,mobile,cell"]
        }
      }
    }
  }
}
```

### Explicit Mapping

```json
PUT /orders/_mapping
{
  "properties": {
    "order_id": {"type": "keyword"},
    "customer_name": {
      "type": "text",
      "analyzer": "standard",
      "fields": {
        "keyword": {"type": "keyword", "ignore_above": 256}
      }
    },
    "description": {
      "type": "text",
      "analyzer": "product_analyzer"
    },
    "status": {"type": "keyword"},
    "total_amount": {"type": "long"},
    "created_at": {
      "type": "date",
      "format": "strict_date_optional_time"
    },
    "tags": {"type": "keyword"},
    "shipping_address": {
      "type": "object",
      "properties": {
        "street": {"type": "text"},
        "city": {"type": "keyword"},
        "country": {"type": "keyword"}
      }
    }
  }
}
```

## Nested vs Parent-Child

### Nested (For Small Arrays of Objects)

```json
PUT /products/_mapping
{
  "properties": {
    "reviews": {
      "type": "nested",
      "properties": {
        "author": {"type": "keyword"},
        "rating": {"type": "integer"},
        "comment": {"type": "text"}
      }
    }
  }
}
```

### Parent-Child (For Large or Frequently Updated Relations)

```json
PUT /qa_index/_mapping
{
  "properties": {
    "join_field": {
      "type": "join",
      "relations": {
        "question": "answer"
      }
    }
  }
}
```

| Approach | When to Use | Performance |
|----------|-------------|-------------|
| Nested | < 100 nested objects, rarely updated | Fast queries, slow updates |
| Parent-child | Many children, independent updates | Slower queries, fast child updates |
| Flattened object | No independent querying needed | Fastest, but cross-object matches |

## Index Templates

```json
PUT /_index_template/logs_template
{
  "index_patterns": ["logs-*"],
  "priority": 100,
  "template": {
    "settings": {
      "number_of_shards": 2,
      "number_of_replicas": 1,
      "index.lifecycle.name": "logs_policy"
    },
    "mappings": {
      "properties": {
        "@timestamp": {"type": "date"},
        "message": {"type": "text"},
        "level": {"type": "keyword"},
        "service": {"type": "keyword"}
      }
    }
  }
}
```

## Data Streams (Time-Based Data)

```json
PUT /_index_template/metrics_stream
{
  "index_patterns": ["metrics-*"],
  "data_stream": {},
  "priority": 200,
  "template": {
    "settings": {"number_of_shards": 1},
    "mappings": {
      "properties": {
        "@timestamp": {"type": "date"},
        "host": {"type": "keyword"},
        "metric_name": {"type": "keyword"},
        "value": {"type": "double"}
      }
    }
  }
}
```

## Alias Strategy

```json
POST /_aliases
{
  "actions": [
    {"add": {"index": "orders_v2", "alias": "orders"}},
    {"remove": {"index": "orders_v1", "alias": "orders"}}
  ]
}
```

**Rule:** Applications MUST query via aliases, never direct index names. Aliases enable zero-downtime reindexing.

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Index | snake_case, lowercase | `orders`, `product_catalog` |
| Time-based index | `{name}-{date}` | `logs-2024.01.15` |
| Alias | snake_case | `orders`, `orders_read`, `orders_write` |
| Field | snake_case | `customer_name`, `created_at` |
| Template | `{pattern}_template` | `logs_template` |
| ILM policy | `{pattern}_policy` | `logs_policy` |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Dynamic mapping in prod | Unexpected field types, mapping explosion | Define explicit mappings |
| `keyword` for full-text fields | No text analysis, no fuzzy search | Use `text` with keyword sub-field |
| Too many shards | Cluster overhead, slow queries | Target 10-50 GB per shard |
| Direct index names in queries | Cannot reindex without downtime | Always use aliases |
| Deeply nested objects (> 3 levels) | Query complexity, poor performance | Flatten or use parent-child |
| Missing `ignore_above` on keyword | Large strings consume heap | Set `ignore_above: 256` |
