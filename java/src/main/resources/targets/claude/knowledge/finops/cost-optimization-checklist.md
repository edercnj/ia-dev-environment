# Cost Optimization Checklist

Layer-by-layer checklist for optimizing cloud costs.

## Compute

- [ ] Right-size instances based on CPU/memory utilization (target 60-80%)
- [ ] Use spot/preemptible instances for fault-tolerant workloads
- [ ] Implement auto-scaling with appropriate min/max boundaries
- [ ] Reserve capacity for predictable baseline load (70-80%)
- [ ] Remove idle instances and unused compute resources
- [ ] Use ARM-based instances where supported (typically 20% cheaper)
- [ ] Schedule non-production environments to shut down outside business hours

## Storage

- [ ] Implement lifecycle policies to transition data to cheaper tiers
- [ ] Delete orphaned snapshots and unattached volumes
- [ ] Use appropriate storage classes (standard, infrequent access, archive)
- [ ] Enable compression for stored data where applicable
- [ ] Review and clean up unused container images in registries
- [ ] Set expiration policies for temporary and log data

## Network

- [ ] Minimize cross-region data transfer (co-locate dependent services)
- [ ] Use CDN for static content delivery
- [ ] Review and optimize NAT Gateway usage
- [ ] Consolidate VPC endpoints to reduce per-endpoint costs
- [ ] Monitor and optimize data transfer between availability zones
- [ ] Use private endpoints for service-to-service communication

## Database

- [ ] Right-size database instances based on actual query load
- [ ] Use reserved capacity for production databases
- [ ] Implement read replicas instead of scaling up primary
- [ ] Archive historical data to cheaper storage tiers
- [ ] Review and optimize provisioned IOPS and throughput
- [ ] Use serverless database options for variable workloads
- [ ] Clean up unused database snapshots and backups beyond retention policy
