# RPO/RTO Calculator

Guide for calculating Recovery Point Objective and Recovery Time Objective based on SLA requirements and business impact.

## RPO Calculation

### Step 1: Identify Data Criticality

| Data Type | Criticality | Typical RPO |
|-----------|------------|-------------|
| Financial transactions | Critical | 0 (zero loss) |
| User-generated content | High | < 5 minutes |
| Session/cache data | Medium | < 1 hour |
| Analytics/logs | Low | < 24 hours |

### Step 2: Calculate Acceptable Data Loss

```
RPO = min(data_criticality_rpo, replication_capability)

Example:
- Transaction data RPO requirement: 0
- Replication capability: synchronous (RPO = 0)
- Result: RPO = 0 (Active-Active or synchronous replication required)
```

### Step 3: Map RPO to Replication Strategy

| RPO Target | Replication Strategy | Technology |
|------------|---------------------|------------|
| 0 | Synchronous multi-region | Database multi-master, distributed log |
| < 5 min | Asynchronous continuous | Streaming replication, CDC |
| < 1 hour | Near-continuous | Incremental backup every 15 min |
| < 24 hours | Periodic | Daily automated backup |

## RTO Calculation

### Step 1: Determine Business Impact

```
Hourly_Cost_of_Downtime = (Annual_Revenue / 8760) * Impact_Factor

Impact_Factor:
- 1.0 = 100% revenue affected
- 0.5 = partial impact (some services available)
- 0.1 = minimal direct revenue impact
```

### Step 2: Define Maximum Acceptable Downtime

```
RTO = min(SLA_commitment, business_tolerance)

SLA to Downtime Mapping:
- 99.9%  = 8.76 hours/year  (~43 min/month)
- 99.95% = 4.38 hours/year  (~21 min/month)
- 99.99% = 52.6 min/year    (~4.3 min/month)
- 99.999% = 5.26 min/year   (~26 sec/month)
```

### Step 3: Map RTO to DR Strategy

| RTO Target | DR Strategy | Automation Level |
|------------|------------|------------------|
| < 1 min | Active-Active | Fully automated |
| < 15 min | Active-Passive | Automated with health checks |
| < 1 hour | Warm Standby | Semi-automated |
| < 4 hours | Pilot Light | Manual with runbooks |

## Cost-Benefit Analysis

### DR Investment Formula

```
Justified_DR_Budget = Annual_Downtime_Cost * Risk_Probability

Annual_Downtime_Cost = Hourly_Cost * Expected_Annual_Downtime_Hours
Risk_Probability = Historical_Incident_Rate * Severity_Weight
```

### Example Calculation

```
Service: Payment Processing API
Annual Revenue: $10M
Impact Factor: 1.0 (full revenue loss during outage)
Hourly Cost: $10M / 8760 = $1,142/hour

SLA: 99.95% (4.38 hours/year max downtime)
Historical incidents: 2 per year, avg 3 hours each

Current annual cost: 2 * 3 * $1,142 = $6,852
Target: Active-Passive (RTO < 15 min)
Reduced annual cost: 2 * 0.25 * $1,142 = $571

Savings: $6,281/year
DR Infrastructure cost: ~$5,000/month = $60,000/year

Decision: Evaluate if SLA penalties and reputation
damage justify the $60K investment beyond direct
revenue loss.
```

## Validation Checklist

- [ ] RPO aligned with data criticality classification
- [ ] RTO aligned with SLA commitments
- [ ] DR strategy matches RPO/RTO requirements
- [ ] Cost-benefit analysis documented and approved
- [ ] Replication technology supports RPO target
- [ ] Failover automation supports RTO target
- [ ] DR testing schedule established
- [ ] Communication plan defined per severity level
