# Story Planning Report — story-0046-0007

| Field | Value |
|-------|-------|
| Story ID | story-0046-0007 |
| Epic ID | 0046 |
| Date | 2026-04-16 |
| Agents | Architect, QA, Security, Tech Lead, PO |

## Planning Summary

Fecha o épico com audit CI-blocking `LifecycleIntegrityAuditTest` que detecta regressões das rules 046-02/03/04/05 em 3 dimensões: (1) phases órfãs, (2) writes em `reports/` sem commit subsequente, (3) flags `--skip-*` no happy path. Impede que stories 0002-0005 regridam em refatorações futuras.

## Architecture Assessment

**Componentes novos em `dev.iadev.application.lifecycle`:**
- `OrphanPhaseDetector` — usa `commonmark-java` para parse de headings; identifica seções não referenciadas no Core Loop.
- `WriteWithoutCommitDetector` — regex `write\s+plans/epic-\*/reports/\S+` + window de 20 lines para match de `Skill(skill:\s*"x-git-commit"`.
- `SkipFlagDetector` — grep `--skip-\w+` fora de seções "Recovery" / "Error Handling".
- `LifecycleAuditRunner` — agrega os 3 detectors; substitui skeleton da story 0001.
- `LifecycleAuditCli` — CLI standalone.
- `dev.iadev.audit.LifecycleIntegrityAuditTest` — teste JUnit em `src/test/java/` que invoca o runner sobre o skills tree real e assertEquals(List.of(), violations).

**Escape hatch:** comentário markdown `<!-- audit-exempt: <motivo> -->` permite exempção revisada por humano. Runner conta exemptions e WARN se > 3 (sinaliza abuso).

## Test Strategy Summary

**Outer loop E2E:** `LifecycleAuditRegressionSmokeTest` — cria sandbox com cópia do skills tree, injeta 3 regressões sintéticas (uma por dimension), assert exatamente 3 violations detectadas, performance < 2s.

**Inner loop TPP por detector:**

OrphanPhaseDetector:
- L1: `scan_whenNoSectionsHeadings_empty`
- L2: `scan_whenAllSectionsReferenced_empty`
- L3: `scan_whenOneOrphanSection_findsIt`
- L4: `scan_multipleOrphans_findsAll`
- L5: `scan_respectsExemptHatch`
- L6: `scan_performanceBoundary` (40 files < 2s)

Análogo para os outros 2 detectors.

**Coverage:** ≥ 95% Line, ≥ 90% Branch em cada detector.

## Security Assessment Summary

Nenhuma surface nova. Escape hatch `<!-- audit-exempt -->` é controlado:
- Threshold alerta se > 3 exemptions → sinaliza abuso
- Audit trail: cada exempção requer comentário explicativo

Paths processados via `Path.normalize()` (defense in depth).

## Implementation Approach

**Sequência sugerida:**
1. Detectors (001, 002, 003) em paralelo — independentes.
2. Runner (004) agrega.
3. CI test (005) — requer runner.
4. Regression smoke (006) — valida end-to-end.

**Heurística OrphanPhaseDetector:**
1. Parse markdown via commonmark.
2. Coletar todos headings `##`, `###` com padrão `\d+(\.\d+)?`.
3. Identificar bloco "Core Loop" (heading próximo de "workflow"/"core loop"/"phases").
4. No bloco, listar referências a `Phase \d+(\.\d+)?` ou `Section \d+(\.\d+)?`.
5. Diferença: headings não referenciados = órfãos.

**Falsos positivos esperados:** exemption via `<!-- audit-exempt: intentionally documented but not called, see ... -->`. Limite: 3 por skill, log WARN.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total | 6 |
| ARCH | 4 |
| QA | 6 embedded + 1 E2E |
| SEC | 1 (escape hatch governance) |
| Merged | 5 |

## Consolidated Risk Matrix

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| False positives bloqueiam CI legítimo | HIGH | MEDIUM | Escape hatch `<!-- audit-exempt -->` + threshold alerta |
| Detector regex frágil (CRLF, encoding) | MEDIUM | LOW | Unit tests com variações encoding |
| Performance > 2s em 40 SKILL.md | MEDIUM | LOW | Smoke de perf + profiling se necessário |
| Story 0007 depende de 0002..0005 mergeadas primeiro | HIGH | MEDIUM | Sequencialização documentada no implementation-map |

## DoR Status

**READY** — ver `dor-story-0046-0007.md`.
