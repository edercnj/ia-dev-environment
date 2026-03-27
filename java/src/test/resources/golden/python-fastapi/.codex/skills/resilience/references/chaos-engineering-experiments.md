# Chaos Engineering Experiments Catalog

Catalog of chaos experiments organized by failure type for {{LANGUAGE}} {{FRAMEWORK}} services.

## Network Failure Experiments

### Packet Loss

- **Setup**: Inject packet loss at network level using Toxiproxy or tc (traffic control)
- **Levels**: 5% (mild degradation), 25% (noticeable impact), 50% (severe), 100% (total failure)
- **Validation**: Circuit breaker trips, retry logic activates, fallback responses serve
- **Kill Switch**: Remove toxic/restore network rules

### Latency Injection

- **Setup**: Add delay to upstream connections via proxy or sidecar
- **Levels**: 50ms (within SLA), 200ms (degraded), 1s (timeout risk), 5s (beyond timeout)
- **Validation**: Timeout patterns activate, deadline propagation works, bulkhead prevents cascade
- **Kill Switch**: Remove proxy rules, restore direct connections

### DNS Failure

- **Setup**: Override DNS resolution to return NXDOMAIN or inject resolution delay
- **Validation**: Service handles DNS failure gracefully, cached DNS entries used, fallback endpoints activated
- **Kill Switch**: Restore DNS configuration

### Network Partition

- **Setup**: Block traffic between specific service pairs using iptables or Chaos Mesh NetworkChaos
- **Validation**: Split-brain detection, leader election recovery, data consistency after partition heals
- **Kill Switch**: Remove network rules, verify reconciliation

## Latency Injection Experiments

### Endpoint-Specific Delay

- **Setup**: Add latency to specific API endpoints via service mesh or proxy rules
- **Validation**: Timeout per endpoint, circuit breaker per dependency, user-facing degradation message
- **Kill Switch**: Remove endpoint-specific rules

### Slow Connection

- **Setup**: Inject TCP handshake delay using Toxiproxy slow_open
- **Validation**: Connection timeout triggers, connection pool handles slow connections
- **Kill Switch**: Remove toxic configuration

### Timeout Simulation

- **Setup**: Delay response until after client timeout threshold
- **Validation**: Client timeout fires, resources released, no thread leak, proper error propagation
- **Kill Switch**: Remove delay injection

## Resource Exhaustion Experiments

### CPU Stress

- **Setup**: Use stress-ng or Chaos Mesh StressChaos to saturate CPU cores
- **Levels**: 50% (moderate), 80% (high), 95% (near saturation)
- **Validation**: Request processing degrades gracefully, health checks still respond, autoscaling triggers
- **Kill Switch**: Terminate stress process

### Memory Pressure

- **Setup**: Allocate memory progressively using stress-ng or container memory limits
- **Levels**: 70% (warning), 85% (critical), 95% (near OOM)
- **Validation**: GC behavior acceptable, no OOM kill without graceful shutdown, memory-based alerts fire
- **Kill Switch**: Release allocated memory, restart container if needed

### Disk Full

- **Setup**: Fill disk using fallocate or dd to target mount point
- **Validation**: Write operations fail gracefully, logging continues to alternate sink, alerts fire
- **Kill Switch**: Remove generated files

### Thread Pool Exhaustion

- **Setup**: Send concurrent requests exceeding thread pool capacity
- **Validation**: Bulkhead rejects excess requests, rejection metrics recorded, no deadlock
- **Kill Switch**: Stop load generation

## Dependency Failure Experiments

### Service Unavailability

- **Setup**: Stop downstream service container or block its port
- **Validation**: Circuit breaker opens, fallback response served, recovery after service restart
- **Kill Switch**: Restart downstream service

### Degraded Responses

- **Setup**: Configure downstream mock to respond slowly or with partial data
- **Validation**: Timeout handling, partial response handling, graceful degradation
- **Kill Switch**: Restore normal downstream behavior

### Malformed Responses

- **Setup**: Configure downstream to return invalid JSON, wrong content-type, or unexpected status codes
- **Validation**: Deserialization errors handled, circuit breaker counts these as failures, error logging captures details
- **Kill Switch**: Restore normal downstream responses
