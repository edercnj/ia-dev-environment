# Event-Driven Conventions

## Overview
Conventions for event-driven architectures and async messaging.

## Event Naming
- Use past tense for domain events: OrderPlaced, PaymentReceived
- Use PascalCase for event type names
- Include aggregate ID in every event

## Payload
- Include event metadata: timestamp, correlation ID, source
- Keep payloads self-contained
- Version event schemas explicitly
