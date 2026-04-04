# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Project Identity — my-spring-event-driven

## Identity
- **Name:** my-spring-event-driven
- **Purpose:** Describe your event-driven service here
- **Architecture Style:** event-driven
- **Domain-Driven Design:** true
- **Event-Driven:** true
- **Interfaces:** rest, event-consumer, event-producer
- **Language:** java 21
- **Framework:** spring-boot 3.x

## Technology Stack
| Layer | Technology |
|-------|-----------|
| Architecture | event-driven |
| Language | java 21 |
| Framework | spring-boot 3.x |
| Build Tool | gradle |
| Database | none |
| Migration | none |
| Cache | none |
| Message Broker | none |
| Container | docker |
| Orchestrator | none |
| Observability | none (none) |
| Resilience | Mandatory (always enabled) |
| Native Build | false |
| Smoke Tests | true |
| Contract Tests | false |

## Source of Truth (Hierarchy)
1. Epics / PRDs (vision and global rules)
2. ADRs (architectural decisions)
3. Stories / tickets (detailed requirements)
4. Rules (.claude/rules/)
5. Source code

## Language
- Code: English (classes, methods, variables)
- Commits: English (Conventional Commits)
- Documentation: English (customize as needed)
- Application logs: English

## Constraints
<!-- Customize constraints for your project -->
- Cloud-Agnostic: ZERO dependencies on cloud-specific services
- Horizontal scalability: Application must be stateless
- Externalized configuration: All configuration via environment variables or ConfigMaps
