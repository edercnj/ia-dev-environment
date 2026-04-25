# Capacity Planning Template

## Service Information

| Field | Value |
|-------|-------|
| Service Name | `<service-name>` |
| Owner Team | `<team-name>` |
| Planning Period | `<quarter/year>` |
| Last Updated | `<date>` |
| Review Cadence | Monthly |

## Current Load Profile

### Traffic Patterns

| Metric | Current Value | Unit |
|--------|-------------|------|
| Average RPS | | requests/second |
| Peak RPS | | requests/second |
| Peak-to-Average Ratio | | multiplier |
| Daily Unique Users | | count |
| Monthly Active Users | | count |

### Resource Utilization (Current)

| Resource | Average | P95 | P99 | Peak | Capacity |
|---------|---------|-----|-----|------|----------|
| CPU | | | | | |
| Memory | | | | | |
| Disk I/O | | | | | |
| Network | | | | | |
| DB Connections | | | | | |
| Thread Pool | | | | | |

### Performance Baselines

| Metric | P50 | P95 | P99 | SLO Target |
|--------|-----|-----|-----|-----------|
| Response Time | | | | |
| Error Rate | | | | |
| Throughput | | | | |

## Growth Projections

### User Growth Model

| Period | Projected Users | Growth Rate | Model |
|--------|---------------|-------------|-------|
| Current | | baseline | measured |
| +3 months | | % | linear/exponential |
| +6 months | | % | linear/exponential |
| +12 months | | % | linear/exponential |

### Traffic Growth Model

| Period | Projected RPS | Growth Factor | Confidence |
|--------|-------------|--------------|-----------|
| Current | | 1.0x | measured |
| +3 months | | x | high/medium/low |
| +6 months | | x | high/medium/low |
| +12 months | | x | high/medium/low |

### Seasonal Adjustments

| Event/Season | Traffic Multiplier | Duration | Preparation Lead Time |
|-------------|-------------------|----------|---------------------|
| Holiday peak | x | weeks | |
| Marketing campaign | x | days | |
| End of quarter | x | days | |

## Resource Requirements

### Compute

| Period | Instances | CPU per Instance | Memory per Instance | Total CPU | Total Memory |
|--------|----------|-----------------|--------------------|-----------|-----------  |
| Current | | | | | |
| +3 months | | | | | |
| +6 months | | | | | |
| +12 months | | | | | |

### Storage

| Storage Type | Current Size | Growth Rate | Projected (12mo) | Retention Policy |
|-------------|-------------|-------------|-----------------|-----------------|
| Database | | GB/month | | |
| Object storage | | GB/month | | |
| Logs | | GB/day | | |
| Backups | | GB/week | | |

### Database

| Metric | Current | +3mo | +6mo | +12mo | Max Capacity |
|--------|---------|------|------|-------|-------------|
| Connection pool | | | | | |
| Query throughput | | | | | |
| Storage size | | | | | |
| IOPS | | | | | |

## Scaling Triggers

### Auto-Scaling Configuration

| Trigger | Scale-Up Threshold | Scale-Down Threshold | Cooldown |
|---------|-------------------|---------------------|----------|
| CPU utilization | 70% | 30% | 5 minutes |
| Memory utilization | 80% | 40% | 5 minutes |
| Request queue depth | > 100 | < 10 | 3 minutes |
| Response time P99 | > SLO target | < 50% SLO | 10 minutes |

### Manual Scaling Triggers

| Condition | Action | Owner | Lead Time |
|-----------|--------|-------|----------|
| Projected peak > auto-scale max | Pre-provision instances | SRE | 1 week |
| Database nearing capacity | Vertical scaling or sharding | DBA + SRE | 2 weeks |
| Storage > 60% utilized | Provision additional storage | SRE | 3 days |
| New region deployment | Full stack provisioning | Platform | 4 weeks |

## Cost Estimates

### Monthly Infrastructure Cost

| Resource | Current Cost | +3mo Projected | +6mo Projected | +12mo Projected |
|---------|-------------|---------------|---------------|----------------|
| Compute | | | | |
| Storage | | | | |
| Database | | | | |
| Network/CDN | | | | |
| Monitoring | | | | |
| **Total** | | | | |

### Cost Optimization Opportunities

| Opportunity | Current Cost | Optimized Cost | Savings | Effort |
|------------|-------------|---------------|---------|--------|
| Reserved instances | | | | |
| Right-sizing | | | | |
| Storage tiering | | | | |
| Spot instances (non-critical) | | | | |

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|-----------|
| Traffic exceeds projections | | | |
| Database bottleneck | | | |
| Storage exhaustion | | | |
| Third-party dependency limit | | | |
| Region capacity constraint | | | |

## Action Items

| Action | Priority | Owner | Due Date | Status |
|--------|---------|-------|---------|--------|
| | | | | |
| | | | | |
| | | | | |

## Approval

| Role | Name | Date | Approved |
|------|------|------|---------|
| SRE Lead | | | |
| Engineering Manager | | | |
| Finance (if budget increase) | | | |
