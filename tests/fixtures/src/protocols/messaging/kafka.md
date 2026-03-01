# Kafka Conventions

## Overview
Apache Kafka messaging conventions and best practices.

## Topics
- Use dot-separated naming: domain.entity.event
- Partition by aggregate ID for ordering
- Set appropriate retention periods

## Consumers
- Use consumer groups for scaling
- Handle rebalancing gracefully
- Implement idempotent processing
