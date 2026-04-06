# Search Engine Principles

## Inverted Index

The inverted index is the core data structure of search engines:

| Concept | Detail |
|---------|--------|
| Forward index | Document -> terms (standard DB index) |
| Inverted index | Term -> documents containing that term |
| Posting list | List of document IDs for a given term |
| Term frequency (TF) | How often a term appears in a document |
| Document frequency (DF) | How many documents contain the term |

### How Inverted Index Works

```
Document 1: "quick brown fox"
Document 2: "quick red car"
Document 3: "brown car drives"

Inverted Index:
  "brown"  -> [1, 3]
  "car"    -> [2, 3]
  "drives" -> [3]
  "fox"    -> [1]
  "quick"  -> [1, 2]
  "red"    -> [2]
```

## Analysis Pipeline

Text must be analyzed (tokenized, normalized) before indexing:

```
Input: "The Quick Brown FOX jumped!"
  |-- Character filter: strip HTML, normalize unicode
  |-- Tokenizer: split into tokens ["The", "Quick", "Brown", "FOX", "jumped"]
  |-- Token filters:
      |-- lowercase: ["the", "quick", "brown", "fox", "jumped"]
      |-- stop words: ["quick", "brown", "fox", "jumped"]
      +-- stemming: ["quick", "brown", "fox", "jump"]
```

### Analyzer Components

| Component | Purpose | Examples |
|-----------|---------|---------|
| Character filter | Pre-process raw text | HTML strip, pattern replace |
| Tokenizer | Split text into tokens | Standard, whitespace, keyword, pattern |
| Token filter | Transform tokens | Lowercase, stemmer, synonym, stop words |

## Relevance Scoring

### BM25 (Default Scoring Algorithm)

| Factor | Effect on Score |
|--------|----------------|
| Term frequency (TF) | Higher TF = higher score (with saturation) |
| Inverse document frequency (IDF) | Rare terms score higher |
| Field length | Shorter fields score higher for same term |
| k1 parameter (default 1.2) | Controls TF saturation |
| b parameter (default 0.75) | Controls field length normalization |

### Score Components

```
score(q, d) = sum(IDF(t) * (TF(t,d) * (k1 + 1)) / (TF(t,d) + k1 * (1 - b + b * |d| / avgdl)))
```

## Mapping Design

### Field Types

| Type | Use Case | Indexed? | Searchable? |
|------|----------|----------|-------------|
| `text` | Full-text search | Analyzed | Full-text queries |
| `keyword` | Exact match, aggregations | Not analyzed | Term queries, sorting |
| `integer/long` | Numeric range queries | Yes | Range, term |
| `date` | Date/time queries | Yes | Range, date math |
| `boolean` | Boolean filters | Yes | Term |
| `object` | Nested JSON | Flattened | Depends on inner types |
| `nested` | Array of objects (independent) | Each object separate | Nested queries |
| `geo_point` | Latitude/longitude | Yes | Geo queries |
| `dense_vector` | Vector embeddings (k-NN) | Yes (HNSW/flat) | k-NN search |

### Mapping Rules

| Rule | Detail |
|------|--------|
| Explicit mapping | Always define mappings explicitly; never rely on dynamic mapping in production |
| Multi-field | Use `text` + `keyword` sub-field for fields needing both search and aggregation |
| Disable `_source` carefully | Only disable if storage is critical; needed for updates and reindex |
| Date formats | Specify exact format: `strict_date_optional_time` |

## Sharding Strategy

| Factor | Recommendation |
|--------|---------------|
| Shard size | 10-50 GB per shard (optimal) |
| Shard count | Plan for growth; changing shard count requires reindex |
| Replicas | Minimum 1 for production; more for read-heavy workloads |
| Index per time period | Daily/weekly/monthly indexes for time-based data |
| Index aliases | Always query via alias, not index name directly |

### Shard Sizing Formula

```
total_shards = ceil(expected_data_size / target_shard_size)
primary_shards = total_shards / (1 + replica_count)
```

## When to Use Search Engines

### Choose Search Engine When

| Criterion | Threshold |
|-----------|-----------|
| Query type | Full-text search, fuzzy matching, relevance scoring |
| Data volume | > 1 GB of searchable text |
| Query complexity | Multi-field search, nested filters, aggregations |
| Latency | Sub-100ms search on millions of documents |
| Analytics | Faceted search, histograms, cardinality counts |

### Anti-Patterns (When NOT to Use Search)

| Scenario | Why Search is Wrong | Better Alternative |
|----------|-------------------|-------------------|
| Primary data store | Not ACID, eventual consistency | PostgreSQL, MySQL |
| Transactional updates | No transactions | SQL database |
| Graph traversals | No relationship support | Neo4j, Neptune |
| Time-series metrics | Not optimized for append-heavy | InfluxDB, TimescaleDB |
| Simple key-value | Overhead not justified | Redis, DynamoDB |

## CAP Positioning

| System | CAP Choice | Consistency Model | Notes |
|--------|-----------|-------------------|-------|
| Elasticsearch | AP | Eventual (configurable) | Tunable consistency per request |
| OpenSearch | AP | Eventual (configurable) | Fork of Elasticsearch |

## Framework Integration

| Engine | Java Client | Protocol |
|--------|------------|----------|
| Elasticsearch | `co.elastic.clients:elasticsearch-java` | HTTP REST |
| OpenSearch | `org.opensearch.client:opensearch-java` | HTTP REST |

## Sensitive Data

| Rule | Detail |
|------|--------|
| PII in indexes | Encrypt or tokenize PII before indexing |
| Field-level security | Use document/field-level security where available |
| Audit logging | Enable audit logs for all search operations |
| Index-level access | Role-based access per index/alias |
| Snapshot encryption | Encrypt repository snapshots at rest |
