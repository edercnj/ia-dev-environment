# Circuit Breaker

## Overview
Circuit breaker prevents cascading failures in distributed systems.

## States
- Closed: requests pass through normally
- Open: requests fail fast without calling downstream
- Half-Open: limited requests test recovery

## Application
Implement circuit breakers for all external service calls.
