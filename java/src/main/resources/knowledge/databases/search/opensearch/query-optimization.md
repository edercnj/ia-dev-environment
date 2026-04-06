# OpenSearch — Query Optimization

## Query Optimization Basics

OpenSearch query optimization follows Elasticsearch patterns with additional features for neural search and observability.

## Query vs Filter Context

```json
GET /products/_search
{
  "query": {
    "bool": {
      "must": [
        {"match": {"description": "wireless headphones"}}
      ],
      "filter": [
        {"term": {"status": "active"}},
        {"range": {"price": {"gte": 50, "lte": 200}}}
      ]
    }
  }
}
```

**Rule:** Place exact-match and range conditions in `filter` context. Filters are cached as bitsets and skip relevance scoring.

## Search Pipelines

Search pipelines modify queries and results at execution time:

### Request Processor (Query Rewriting)

```json
PUT /_search/pipeline/my_pipeline
{
  "request_processors": [
    {
      "filter_query": {
        "query": {
          "term": {"status": "active"}
        }
      }
    }
  ],
  "response_processors": [
    {
      "rename_field": {
        "field": "internal_id",
        "target_field": "id"
      }
    }
  ]
}

// Use pipeline in search
GET /products/_search?search_pipeline=my_pipeline
{
  "query": {"match": {"name": "laptop"}}
}
```

### Normalization Processor (Hybrid Search)

```json
PUT /_search/pipeline/hybrid_pipeline
{
  "phase_results_processors": [
    {
      "normalization-processor": {
        "normalization": {"technique": "min_max"},
        "combination": {
          "technique": "arithmetic_mean",
          "parameters": {"weights": [0.3, 0.7]}
        }
      }
    }
  ]
}
```

## Neural Search

### Semantic Search with ML Model

```json
GET /products/_search
{
  "query": {
    "neural": {
      "embedding": {
        "query_text": "comfortable wireless headphones for running",
        "model_id": "model_id_here",
        "k": 10
      }
    }
  }
}
```

### Hybrid (Neural + BM25)

```json
GET /products/_search?search_pipeline=hybrid_pipeline
{
  "query": {
    "hybrid": {
      "queries": [
        {
          "match": {"description": "wireless headphones"}
        },
        {
          "neural": {
            "embedding": {
              "query_text": "wireless headphones for sports",
              "model_id": "model_id_here",
              "k": 20
            }
          }
        }
      ]
    }
  }
}
```

## Observability Integration

### Query Insights

```json
// Enable query insights (2.12+)
PUT /_cluster/settings
{
  "persistent": {
    "search.insights.top_queries.latency.enabled": true,
    "search.insights.top_queries.latency.window_size": "5m",
    "search.insights.top_queries.latency.top_n_size": 10
  }
}

// Get top slow queries
GET /_insights/top_queries?type=latency
```

### Search Monitoring

```json
// Node-level search statistics
GET /_nodes/stats/indices/search

// Per-index search statistics
GET /products/_stats/search
```

| Metric | Alert Threshold | Action |
|--------|----------------|--------|
| Search latency (p99) | > 500ms | Profile queries, add filters |
| Search rate | > 80% capacity | Scale replicas or nodes |
| Scroll contexts | > 500 open | Close unused scrolls |
| Circuit breaker trips | > 0 per hour | Reduce query complexity or add memory |

## Aggregation Optimization

```json
GET /orders/_search
{
  "size": 0,
  "query": {
    "bool": {
      "filter": [
        {"range": {"created_at": {"gte": "now-30d"}}}
      ]
    }
  },
  "aggs": {
    "daily_stats": {
      "date_histogram": {
        "field": "created_at",
        "calendar_interval": "day"
      },
      "aggs": {
        "total_revenue": {"sum": {"field": "total_amount"}},
        "order_count": {"value_count": {"field": "order_id"}},
        "top_categories": {
          "terms": {"field": "category", "size": 5}
        }
      }
    }
  }
}
```

## Pagination

### Search After (Recommended for Deep Pagination)

```json
GET /products/_search
{
  "size": 20,
  "query": {"match_all": {}},
  "sort": [
    {"created_at": "desc"},
    {"_id": "asc"}
  ],
  "search_after": ["2024-01-15T10:30:00Z", "abc123"]
}
```

### Point in Time (PIT) for Consistent Pagination

```json
// Create PIT
POST /products/_search/point_in_time?keep_alive=5m

// Search with PIT
GET /_search
{
  "size": 20,
  "query": {"match_all": {}},
  "pit": {"id": "pit_id_here", "keep_alive": "5m"},
  "sort": [{"created_at": "desc"}, {"_id": "asc"}],
  "search_after": ["2024-01-15T10:30:00Z", "abc123"]
}
```

## Performance Tuning

| Setting | Default | Tuning | Purpose |
|---------|---------|--------|---------|
| `refresh_interval` | 1s | 30s for write-heavy | Search visibility delay |
| `translog.durability` | request | async for bulk | Fsync frequency |
| `max_result_window` | 10000 | Use `search_after` instead of increasing | Deep pagination limit |
| `search.max_buckets` | 65535 | Increase only if needed | Aggregation bucket limit |

## Caching Strategies

| Cache | Scope | Use Case |
|-------|-------|----------|
| Node query cache | Filter bitsets | Repeated filter clauses |
| Shard request cache | Full shard results | Aggregations on static data |
| Fielddata cache | Sorting, aggregations | Low-cardinality keyword fields |

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Scoring in filter context | Wasted CPU | Use `filter` for exact/range |
| k-NN without search pipeline | No score normalization for hybrid | Use normalization processor |
| `from: 9999` deep pagination | Slow, memory-intensive | Use `search_after` + PIT |
| Aggregation on `text` field | Fielddata explosion | Use `keyword` sub-field |
| No time filter on log indexes | Scans all indexes | Always filter on `@timestamp` |
| Neural search without hybrid | Misses keyword matches | Combine neural + BM25 |
