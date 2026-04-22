# Story Planning Report — story-0046-0001

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0046-0001 |
| Epic ID | 0046 |
| Date | 2026-04-16 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Foundation story: publica Rule 22 (`lifecycle-integrity`), atualiza 3 templates principais com matriz de transição de status e entrega pacote Java `dev.iadev.application.lifecycle` (`StatusFieldParser`, `LifecycleTransitionMatrix`, `LifecycleAuditRunner` skeleton). Não altera skills — apenas disponibiliza infraestrutura. Desbloqueia as 6 stories restantes do EPIC-0046.

## Architecture Assessment

**Layers afetadas:** Domain (`dev.iadev.domain.lifecycle.LifecycleStatus` enum) + Application (`dev.iadev.application.lifecycle.*`). Adapter e Config intocados nesta story.

**Novos componentes:**
- `dev.iadev.domain.lifecycle.LifecycleStatus` (enum, 6 valores + `fromLabel/toLabel`)
- `dev.iadev.application.lifecycle.LifecycleTransitionMatrix` (imutável, Map<From, Set<To>>)
- `dev.iadev.application.lifecycle.StatusFieldParser` (regex MULTILINE + atomic write via `.tmp` + `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`)
- `dev.iadev.application.lifecycle.LifecycleAuditRunner` (interface + skeleton; impl real em story 0007)
- `dev.iadev.application.lifecycle.StatusSyncException` (checked, code `STATUS_SYNC_FAILED`)
- `dev.iadev.application.lifecycle.StatusTransitionInvalidException` (checked, code `STATUS_TRANSITION_INVALID`)

**Dependency direction:** domain não importa application; application importa domain.lifecycle (correto). Adapter/Config não tocam nesta story — Rule 04 respeitada.

**Ordem de implementação:** domain/LifecycleStatus (TASK-003) → application/LifecycleTransitionMatrix (TASK-003 continuação) → application/StatusFieldParser (TASK-004) → smoke (TASK-006).

## Test Strategy Summary

**Outer loop (acceptance):** `LifecycleFoundationSmokeTest` — sandbox tmp dir, read→validateTransition(PENDENTE, PLANEJADA)→write→read; assert final=PLANEJADA.

**Inner loop (TPP order, unit tests):**
- TPP Level 1 (nil/degenerate): `readStatus_whenNoStatusField_returnsEmpty` — arquivo sem `**Status:**`
- TPP Level 2 (constant): `readStatus_whenValidPendente_returnsPendente`
- TPP Level 3 (scalar): `isAllowed_fromPendenteToPlanejada_returnsTrue` + variantes proibidas
- TPP Level 4 (collection): `validateOrThrow_fullMatrix_parametrized` (tabela de transições)
- TPP Level 5 (conditional): `writeStatus_whenTargetFileExists_atomicallyReplaces`
- TPP Level 6 (iteration/boundary): `writeStatus_withMultipleWhitespace_parsesCorrectly` + `writeStatus_interruptedBeforeRename_leavesOriginalIntact`

**Coverage target:** ≥ 95% Line / ≥ 90% Branch nos helpers.

**Parametrized tests:** matriz de transição é naturalmente parametrizável — JUnit 5 `@ParameterizedTest` com `@CsvSource` cobrindo as 36 combinações (6×6), cada uma com resultado esperado.

## Security Assessment Summary

**OWASP Top 10 aplicáveis:**
- **A04 — Insecure Design:** path traversal em `StatusFieldParser.writeStatus` → canonicalizar path antes de I/O; recusar caminhos fora do epic dir.
- **A08 — Software and Data Integrity Failures:** escrita atômica via `.tmp` + `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)` para evitar corruption parcial em crash.

**Augmentation:** TASK-0046-0001-004 recebe DoD adicional SEC:
- [ ] Path canonicalizado (`Path.toAbsolutePath().normalize()`) antes de qualquer write
- [ ] Nenhum follow de symlink (`LinkOption.NOFOLLOW_LINKS` onde aplicável)
- [ ] `Files.move(ATOMIC_MOVE)` falha claramente se filesystem não suporta (handling explícito)

**Dependency security:** nenhuma nova library externa. Uso de `java.nio.file` (JDK-only).

**Secrets:** N/A — nenhuma credencial manipulada.

## Implementation Approach

**Chosen approach:** `StatusFieldParser` usa regex compilada como constant (reuse entre reads) — evita recompilação. `LifecycleTransitionMatrix` é `final` class com `Map<LifecycleStatus, Set<LifecycleStatus>>` imutável carregado em static block. `StatusSyncException` e `StatusTransitionInvalidException` são checked exceptions estendendo `Exception` (não RuntimeException) para forçar handling explícito nos callers.

**Coding standards (Rule 03):**
- Métodos ≤ 25 linhas ✓ (todos os métodos são curtos)
- Classes ≤ 250 linhas ✓ (parser ≈ 80 LOC; matrix ≈ 60 LOC)
- No null returns ✓ (`Optional<LifecycleStatus>`)
- Intent-revealing names ✓ (`isAllowed`, `validateOrThrow`, `readStatus`, `writeStatus`)
- Constructor injection ✓ (nenhuma dependência injetável nesta story — todos stateless com static factories)

**Rejected alternatives:**
- JDK `Properties` ao invés de regex: rejeitado — formato markdown, não `.properties`.
- Yaml frontmatter parser: rejeitado — overhead de dep externa; arquivos são markdown simples.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 6 |
| Architecture tasks | 5 (ARCH-001..005) |
| Test tasks | 6 RED/GREEN pairs (QA-001..006, merged into TASK-003/004/006) |
| Security tasks | 1 (SEC-001 augmented into TASK-004) |
| Quality gate tasks | 1 (TL-001 implicit em DoD) |
| Validation tasks | 1 (PO-001 validated 6 Gherkin scenarios) |
| Merged tasks | 3 (TASK-001 ARCH+TL; TASK-003 ARCH+QA; TASK-004 ARCH+QA+SEC) |
| Augmented tasks | 1 (TASK-004 recebeu SEC criteria) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Regex frágil a encoding (BOM, CRLF) | QA | MEDIUM | LOW | Ler via `Files.readString(path, UTF_8)`; testes com CRLF + BOM |
| Atomic move não suportado em filesystem exótico (não-ext4, nfs) | SEC | LOW | LOW | Detectar `AtomicMoveNotSupportedException` e fallback documentado (log warn + non-atomic) |
| Rule slot 22 conflito com outro épico paralelo | Tech Lead | LOW | LOW | Revalidado 2026-04-20: slot 21 ocupado por EPIC-0045 ci-watch; 22 livre no source |
| Templates atualizados quebram outros épicos em planejamento | Tech Lead | LOW | MEDIUM | Golden diff captura; `_TEMPLATE-TASK/STORY/EPIC` mudança é aditiva (novo bloco header) |

## DoR Status

**READY** — ver `dor-story-0046-0001.md` para o checklist completo.
