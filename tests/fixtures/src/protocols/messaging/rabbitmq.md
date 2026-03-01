# RabbitMQ Conventions

## Overview
RabbitMQ messaging conventions and best practices.

## Exchanges
- Use topic exchanges for flexible routing
- Use direct exchanges for point-to-point
- Use fanout for broadcast patterns

## Queues
- Name queues after the consuming service
- Set dead-letter exchanges for failed messages
- Use prefetch limits to control throughput
