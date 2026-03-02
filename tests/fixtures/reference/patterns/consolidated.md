# Hexagonal Architecture

## Overview
Hexagonal architecture separates core domain logic from external concerns.

## Key Principles
- Ports define boundaries between layers
- Adapters implement port interfaces
- Domain has zero external dependencies

## Application
Use this pattern for microservice projects built with click.


---

# Repository Pattern

## Overview
The repository pattern abstracts data access behind a clean interface.

## Key Principles
- Repositories encapsulate query logic
- Domain model remains persistence-agnostic
- Unit of work manages transactions

## Application
Implement repositories using python idioms.


---

# Anti-Corruption Layer

## Overview
ACL translates between bounded contexts to prevent model pollution.

## Key Principles
- Isolate external models from domain
- Transform at the boundary
- Maintain clean domain language

## Application
Use ACL when integrating with legacy systems in python.


---

# Saga Pattern

## Overview
Sagas coordinate distributed transactions across microservices.

## Key Principles
- Each step has a compensating action
- Orchestrator or choreography coordination
- Eventual consistency guaranteed

## Application
Apply saga pattern in microservice architectures.


---

# Circuit Breaker

## Overview
Circuit breaker prevents cascading failures in distributed systems.

## States
- Closed: requests pass through normally
- Open: requests fail fast without calling downstream
- Half-Open: limited requests test recovery

## Application
Implement circuit breakers for all external service calls.
