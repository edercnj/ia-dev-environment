# Elasticsearch — Query Optimization

## Query vs Filter Context

| Context | Scoring | Caching | Use For |
|---------|---------|---------|---------|
| **Query** | Yes (relevance) | No | Full-text search, fuzzy matching |
| **Filter** | No (binary match) | Yes (bitset cache) | Exact match, range, boolean flags |

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
        {"range": {"price": {"gte": 50, "lte": 200}}},
        {"term": {"category": "electronics"}}
      ]
    }
  }
}
```

**Rule:** Place exact-match and range conditions in `filter` context, not `must`. Filters are cached and skip scoring.

## Search Profiling

```json
GET /products/_search
{
  "profile": true,
  "query": {
    "match": {"description": "wireless headphones"}
  }
}
```

### Key Profile Metrics

| Metric | Good | Bad |
|--------|------|-----|
| `query` time | < 50ms | > 500ms |
| `fetch` time | < 10ms | > 100ms (too many fields) |
| `collector` type | TopScoreDocCollector | Total hit count collector |
| Shard count | Proportional to data | Too many small shards |

## Aggregation Optimization

### Terms Aggregation

```json
GET /orders/_search
{
  "size": 0,
  "aggs": {
    "status_counts": {
      "terms": {"field": "status", "size": 20}
    },
    "daily_revenue": {
      "date_histogram": {
        "field": "created_at",
        "calendar_interval": "day"
      },
      "aggs": {
        "total": {"sum": {"field": "total_amount"}}
      }
    }
  }
}
```

### Aggregation Best Practices

| Practice | Detail |
|----------|--------|
| `size: 0` | Omit hits when only aggregations needed |
| `keyword` fields | Aggregations on `keyword`, never `text` |
| `execution_hint: map` | For low-cardinality terms aggregations |
| Shard size | `shard_size = size * 1.5 + 10` for accuracy |
| Composite aggregation | For paginating through all buckets |

## Pagination Strategies

### Standard (For First Pages)

```json
GET /products/_search
{
  "from": 0,
  "size": 20,
  "query": {"match_all": {}}
}
```

**Limit:** `from + size` must be <= 10,000 (default `max_result_window`).

### Search After (Deep Pagination)

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

### Scroll API (Full Export)

```json
POST /products/_search?scroll=5m
{
  "size": 1000,
  "query": {"match_all": {}}
}

POST /_search/scroll
{
  "scroll": "5m",
  "scroll_id": "DXF1ZXJ5..."
}
```

| Method | Use Case | Max Results |
|--------|----------|-------------|
| `from/size` | UI pagination (first 10K results) | 10,000 |
| `search_after` | Deep pagination, infinite scroll | Unlimited |
| `scroll` | Batch export, reindexing | Unlimited |
| `PIT + search_after` | Consistent deep pagination (7.10+) | Unlimited |

## Caching

| Cache | Scope | Best For |
|-------|-------|----------|
| Node query cache | Filter clauses (bitsets) | Repeated filter queries |
| Shard request cache | Full shard-level results | Aggregations on static data |
| Field data cache | Aggregations, sorting | Low-cardinality keyword fields |

```json
// Disable cache for real-time queries
GET /orders/_search?request_cache=false
{
  "query": {"range": {"created_at": {"gte": "now-1m"}}}
}
```

## Index Tuning

| Setting | Default | Tuning | Purpose |
|---------|---------|--------|---------|
| `refresh_interval` | 1s | 30s for write-heavy | Control search visibility delay |
| `translog.durability` | request | async for performance | Fsync frequency |
| `merge.policy.max_merged_segment` | 5 GB | Adjust per shard size | Segment merge ceiling |
| `number_of_replicas` | 1 | 0 during bulk index | Reduce write overhead |

### Bulk Indexing Optimization

```json
// Disable refresh during bulk load
PUT /orders/_settings
{"index": {"refresh_interval": "-1", "number_of_replicas": 0}}

// Bulk index
POST /orders/_bulk
{"index": {}}
{"order_id": "001", "status": "pending", "total_amount": 9900}
{"index": {}}
{"order_id": "002", "status": "completed", "total_amount": 4500}

// Re-enable after bulk
PUT /orders/_settings
{"index": {"refresh_interval": "1s", "number_of_replicas": 1}}

// Force merge for optimal read performance
POST /orders/_forcemerge?max_num_segments=1
```

## Anti-Patterns

| Anti-Pattern | Problem | Solution |
|--------------|---------|----------|
| Scoring in filter context | Wasted CPU on irrelevant scoring | Use `filter` for exact/range |
| `from: 9999` deep pagination | Slow, memory-intensive | Use `search_after` |
| Aggregation on `text` field | Fielddata explosion, OOM | Use `keyword` sub-field |
| `refresh_interval: 1s` during bulk | Excessive segment creation | Disable during bulk, re-enable after |
| Missing `size: 0` for agg-only | Returns unnecessary hits | Set `size: 0` when only aggregations needed |
| `match_all` without filter | Returns everything, no caching | Add filters for selective queries |
