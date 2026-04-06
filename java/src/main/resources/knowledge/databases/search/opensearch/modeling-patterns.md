# OpenSearch — Modeling Patterns

## Version Matrix

| Version | Key Features | Notes |
|---------|-------------|-------|
| **2.11** | Improved k-NN, search pipelines GA | Stable |
| **2.13** | Neural search improvements, conversational search | Current |
| **2.15+** | Enhanced ML, improved observability | Latest |

## Index Design

OpenSearch index design follows Elasticsearch patterns with additional features:

### Index Creation

```json
PUT /products
{
  "settings": {
    "number_of_shards": 2,
    "number_of_replicas": 1,
    "index.knn": true,
    "analysis": {
      "analyzer": {
        "product_search": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "asciifolding", "stemmer"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "product_id": {"type": "keyword"},
      "name": {
        "type": "text",
        "analyzer": "product_search",
        "fields": {
          "keyword": {"type": "keyword", "ignore_above": 256}
        }
      },
      "description": {"type": "text", "analyzer": "product_search"},
      "category": {"type": "keyword"},
      "price": {"type": "long"},
      "tags": {"type": "keyword"},
      "created_at": {"type": "date", "format": "strict_date_optional_time"},
      "embedding": {
        "type": "knn_vector",
        "dimension": 768,
        "method": {
          "name": "hnsw",
          "space_type": "l2",
          "engine": "nmslib"
        }
      }
    }
  }
}
```

## k-NN (Vector Search)

### k-NN Field Mapping

```json
{
  "properties": {
    "embedding": {
      "type": "knn_vector",
      "dimension": 768,
      "method": {
        "name": "hnsw",
        "space_type": "cosinesimil",
        "engine": "faiss",
        "parameters": {
          "ef_construction": 256,
          "m": 16
        }
      }
    }
  }
}
```

### k-NN Engines

| Engine | Best For | Notes |
|--------|----------|-------|
| **nmslib** | Pure k-NN (no filters) | Fastest for unfiltered search |
| **faiss** | k-NN with filters, large datasets | GPU support, IVF option |
| **Lucene** | Small-medium datasets, hybrid search | Built-in, no external dependency |

### k-NN Query

```json
GET /products/_search
{
  "query": {
    "knn": {
      "embedding": {
        "vector": [0.1, 0.2, 0.3],
        "k": 10
      }
    }
  }
}
```

### Hybrid Search (k-NN + Text)

```json
GET /products/_search
{
  "query": {
    "hybrid": {
      "queries": [
        {
          "match": {"description": "wireless headphones"}
        },
        {
          "knn": {
            "embedding": {"vector": [0.1, 0.2], "k": 10}
          }
        }
      ]
    }
  },
  "search_pipeline": "hybrid_pipeline"
}
```

## Index State Management (ISM)

ISM is OpenSearch's equivalent of Elasticsearch ILM:

```json
PUT /_plugins/_ism/policies/logs_policy
{
  "policy": {
    "description": "Lifecycle for log indexes",
    "default_state": "hot",
    "states": [
      {
        "name": "hot",
        "actions": [
          {"rollover": {"min_size": "50gb", "min_index_age": "1d"}}
        ],
        "transitions": [
          {"state_name": "warm", "conditions": {"min_index_age": "7d"}}
        ]
      },
      {
        "name": "warm",
        "actions": [
          {"replica_count": {"number_of_replicas": 1}},
          {"force_merge": {"max_num_segments": 1}}
        ],
        "transitions": [
          {"state_name": "cold", "conditions": {"min_index_age": "30d"}}
        ]
      },
      {
        "name": "cold",
        "actions": [
          {"read_only": {}}
        ],
        "transitions": [
          {"state_name": "delete", "conditions": {"min_index_age": "90d"}}
        ]
      },
      {
        "name": "delete",
        "actions": [{"delete": {}}]
      }
    ],
    "ism_template": [
      {"index_patterns": ["logs-*"], "priority": 100}
    ]
  }
}
```

## Data Streams

```json
PUT /_index_template/metrics_template
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

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Index | snake_case, lowercase | `products`, `order_events` |
| Time-based index | `{name}-{date}` | `logs-2024.01.15` |
| Alias | snake_case | `products`, `products_search` |
| Field | snake_case | `product_id`, `created_at` |
| ISM policy | `{pattern}_policy` | `logs_policy` |
| k-NN field | `{concept}_embedding` | `product_embedding` |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| k-NN without `index.knn: true` | k-NN queries fail | Enable in index settings |
| High-dimension vectors (> 2048) | Slow indexing and search | Reduce dimensions or use PQ compression |
| Nested k-NN queries | Poor performance | Flatten vectors to top-level |
| ISM without rollover | Unbounded index growth | Configure rollover in hot state |
| Dynamic mapping for vectors | Wrong dimension or engine | Always define vector mappings explicitly |
| nmslib with post-filtering | Inaccurate results | Use faiss for filtered k-NN |
