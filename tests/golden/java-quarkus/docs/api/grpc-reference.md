# gRPC API Reference -- my-quarkus-service

## Overview

| Field | Value |
|-------|-------|
| Service | my-quarkus-service |
| Protocol | gRPC (Proto3) |
| Language | java |
| Framework | quarkus |

> Document all gRPC services exposed by this application.
> This reference is auto-scaffolded. Fill in service details from your .proto files.

## Service: {ServiceName}

> One section per gRPC service defined in your .proto files.

### RPCs

| Method | Request | Response | Type | Description |
|--------|---------|----------|------|-------------|
| _GetItem_ | _GetItemRequest_ | _GetItemResponse_ | _Unary_ | _Description_ |
| _ListItems_ | _ListItemsRequest_ | _ListItemsResponse_ | _Server Streaming_ | _Description_ |
| _CreateItems_ | _CreateItemsRequest_ | _CreateItemsResponse_ | _Client Streaming_ | _Description_ |
| _SyncItems_ | _SyncItemsRequest_ | _SyncItemsResponse_ | _Bidirectional_ | _Description_ |

### Message: {MessageName}

| Field | Type | Number | Description |
|-------|------|--------|-------------|
| _id_ | _int64_ | _1_ | _Unique identifier_ |
| _name_ | _string_ | _2_ | _Display name_ |
| _[DEPRECATED] old_field_ | _string_ | _3_ | _Use new_field instead_ |

## Backward Compatibility

> Document reserved field numbers, deprecated fields, and backward compatibility notes.
> Fields marked [DEPRECATED] in message tables above should have migration guidance here.

## Change History

| Date | Author | Description |
|------|--------|-------------|
| _YYYY-MM-DD_ | _Author_ | _Initial gRPC reference_ |
