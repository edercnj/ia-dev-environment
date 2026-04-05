# ArchUnit Baseline Report

**Generated:** 2026-04-04
**Story:** story-0015-0001
**ArchUnit Version:** 1.3.0
**Scanned Package:** `dev.iadev` (production classes only)

## Summary


| Rule                                         | ID        | Violations | Status    |
| -------------------------------------------- | --------- | ---------- | --------- |
| domainShouldNotDependOnInfrastructure        | RULE-001a | 98         | @Disabled |
| domainShouldNotDependOnApplication           | RULE-001b | 0          | @Disabled |
| domainModelShouldNotHaveFrameworkAnnotations | RULE-004  | 0          | @Disabled |
| outputPortsShouldBeInterfaces                | RULE-002  | 0          | @Disabled |
| inputPortsShouldBeInterfaces                 | RULE-003a | 0          | @Disabled |
| cliShouldOnlyAccessInputPorts                | RULE-003b | 0          | @Disabled |
| compositionRootShouldBeUnique                | RULE-005  | 0          | @Disabled |
| **Total**                                    |           | **98**     |           |


## RULE-001a: domainShouldNotDependOnInfrastructure (98 violations)

**Rule:** Classes in `..domain..` must not depend on classes in `dev.iadev.assembler`, `dev.iadev.cli`, `dev.iadev.config`, `dev.iadev.template`, `dev.iadev.checkpoint`, `dev.iadev.progress`, `dev.iadev.smoke`, `dev.iadev.util`, `dev.iadev.model`, or `dev.iadev.exception`.

**Root cause:** The `dev.iadev.model` package contains configuration records (`ProjectConfig`, `FrameworkConfig`, `LanguageConfig`, `InfraConfig`, `ArchitectureConfig`, `InterfaceConfig`) that are shared across all layers. The `domain.stack` sub-package depends heavily on these model records.

### Violating Classes (7 domain classes)


| Domain Class            | Depends On                                                                                    | Violation Count |
| ----------------------- | --------------------------------------------------------------------------------------------- | --------------- |
| `CoreKpRouting`         | `ProjectConfig`, `ArchitectureConfig`                                                         | 4               |
| `PatternMapping`        | `ProjectConfig`, `ArchitectureConfig`                                                         | 5               |
| `ProtocolMapping`       | `ProjectConfig`, `InterfaceConfig`                                                            | 9               |
| `SkillRegistry`         | `InfraConfig`                                                                                 | 7               |
| `StackResolver`         | `ProjectConfig`, `FrameworkConfig`, `LanguageConfig`, `InterfaceConfig`                       | 25              |
| `StackValidator`        | `ProjectConfig`, `FrameworkConfig`, `LanguageConfig`, `ArchitectureConfig`, `InterfaceConfig` | 25              |
| `StackVersionValidator` | `ProjectConfig`, `FrameworkConfig`, `LanguageConfig`                                          | 23              |


### Imported Model Types


| Model Type                           | Domain Classes Using It                                                                                          |
| ------------------------------------ | ---------------------------------------------------------------------------------------------------------------- |
| `dev.iadev.model.ProjectConfig`      | `CoreKpRouting`, `PatternMapping`, `ProtocolMapping`, `StackResolver`, `StackValidator`, `StackVersionValidator` |
| `dev.iadev.model.FrameworkConfig`    | `StackResolver`, `StackValidator`, `StackVersionValidator`                                                       |
| `dev.iadev.model.LanguageConfig`     | `StackResolver`, `StackValidator`, `StackVersionValidator`                                                       |
| `dev.iadev.model.InfraConfig`        | `SkillRegistry`                                                                                                  |
| `dev.iadev.model.ArchitectureConfig` | `CoreKpRouting`, `PatternMapping`, `StackValidator`                                                              |
| `dev.iadev.model.InterfaceConfig`    | `ProtocolMapping`, `StackResolver`, `StackValidator`                                                             |


### Remediation Plan

Move the configuration records that domain depends on into `dev.iadev.domain.model` (story-0015-0003). Classes in `dev.iadev.model` that are only used by adapters will remain as adapter DTOs.

## RULE-001b: domainShouldNotDependOnApplication (0 violations)

No `application` package exists yet. Rule will become relevant after story-0015-0005 creates the application layer.

## RULE-004: domainModelShouldNotHaveFrameworkAnnotations (0 violations)

No framework annotations (Jackson, Picocli, SnakeYAML, Jakarta) found in `domain` classes. The domain layer is currently clean of framework coupling.

## RULE-002: outputPortsShouldBeInterfaces (0 violations)

Package `domain.port.outbound` does not exist yet. Rule will become relevant after story-0015-0004 extracts output ports.

## RULE-003a: inputPortsShouldBeInterfaces (0 violations)

Package `domain.port.inbound` does not exist yet. Rule will become relevant after story-0015-0005 extracts input ports.

## RULE-003b: cliShouldOnlyAccessInputPorts (0 violations)

Package `domain.engine` does not exist yet. Rule will become relevant after story-0015-0005.

## RULE-005: compositionRootShouldBeUnique (0 violations)

Domain does not currently depend on `dev.iadev.config`. Rule passes in current state.

## Observations

1. **Primary violation pattern:** Domain classes in `domain.stack` depend on shared configuration records in `dev.iadev.model` (98 violations, all RULE-001a).
2. **Domain purity:** The `domain.implementationmap` sub-package has zero external dependencies and is already hexagonal-compliant.
3. **No framework leakage:** Zero framework annotations in domain -- Jackson, Picocli, and SnakeYAML are confined to adapter layers.
4. **Missing hexagonal infrastructure:** Ports (`port.inbound`, `port.outbound`) and application layer do not exist yet -- 4 of 7 rules target packages that will be created during migration.

## Migration Progress Tracking

This report serves as the baseline (T0). As migration stories complete, rules are activated (remove `@Disabled`), and this section will be updated:


| Story           | Rule(s) Activated              | Expected Violation Delta            |
| --------------- | ------------------------------ | ----------------------------------- |
| story-0015-0003 | RULE-001a, RULE-001b, RULE-004 | -98 (model classes moved to domain) |
| story-0015-0004 | RULE-002                       | 0 (new ports created as interfaces) |
| story-0015-0005 | RULE-003a, RULE-003b           | 0 (new ports created as interfaces) |
| story-0015-0014 | RULE-005                       | 0 (composition root properly wired) |


