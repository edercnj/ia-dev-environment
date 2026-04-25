# Spec — EPIC-0057: Extensão da Rule 24 (Execution Integrity) — Pós-mortem EPIC-0053

## 1. Problem Statement

No EPIC-0053 foi diagnosticado que a skill `x-pr-watch-ci` foi silenciosamente pulada em 6 task-PRs e no PR final #619, sem que qualquer camada de enforcement da Rule 24 detectasse a omissão. O merge cascata só foi pego por `Epic0047CompressionSmokeTest` em `mvn verify` na CI remota — blind spot local. Análise exaustiva do catálogo identificou 14 gaps análogos em orquestradoras principais.

## 2. Causa raiz tripla

1. **Tabela "Mandatory Evidence Artifacts" incompleta** — Rule 24 §32–42 lista apenas 5 sub-skills. `x-pr-watch-ci`, `x-pr-create`, `x-test-tdd`/`x-test-run`, `x-git-commit` (ciclo TDD), `x-dependency-audit`, `x-threat-model` produzem evidência mas não são tabulados. Camadas 3 e 4 cegas.
2. **Invocações em prosa sem marker MANDATORY — NON-NEGOTIABLE** em ao menos 8 pontos: `x-story-implement/SKILL.md:202` (x-pr-watch-ci), `x-task-implement/SKILL.md:496`, `x-release/SKILL.md:1381`, `x-review/SKILL.md:99–155` (9 specialists sem marker), `x-story-implement/SKILL.md:250–254` (MANDATORY em comment, não no bloco Skill), `x-epic-implement/SKILL.md:333` (x-pr-fix-epic), `x-epic-orchestrate/SKILL.md:338` (x-story-plan), `x-owasp-scan/SKILL.md:273` (x-dependency-audit).
3. **Infraestrutura de enforcement incompleta**: `scripts/audit-execution-integrity.sh` (Camada 3) NÃO existe. Rule 45 (CI-Watch Integrity) NÃO existe. Stop hook (`verify-story-completion.sh`) não verifica `.claude/state/pr-watch-*.json`. Flags `--no-ci-watch`, `--no-auto-remediation`, `--skip-pr-comments`, `--no-github-release`, `--no-jira` em happy-path violando Rule 24 §30.

## 3. Escopo

### 3.1 Incluído

- Expansão da tabela "Mandatory Evidence Artifacts" (Rule 24) com 6 novas entradas.
- Implementação de `scripts/audit-execution-integrity.sh` (Camada 3) com `--self-check` e exit codes 0/1/2/3 conforme Rule 24 §66–72.
- Criação da Rule 45 (CI-Watch Integrity) consolidando `RULE-045-*` hoje referenciadas apenas em `x-pr-watch-ci/SKILL.md`.
- Retrofit de markers **MANDATORY — NON-NEGOTIABLE** em 6 orquestradoras (x-story-implement, x-task-implement, x-release, x-review, x-epic-implement, x-owasp-scan).
- Implementação de `scripts/audit-bypass-flags.sh` e remediação de flags em happy-path (mover para `## Recovery` ou remover).
- Extensão do Stop hook (`verify-story-completion.sh`) para verificar `.claude/state/pr-watch-*.json` e novos artefatos tabulados.
- Promoção de `Epic0047CompressionSmokeTest` (e smoke tests congêneres) para `mvn test` ou criação de gate local `mvn pre-push`.
- Decisão e aplicação retroativa para EPIC-0053: backfill de evidências ou adição ao `audits/execution-integrity-baseline.txt`.

### 3.2 Fora de escopo

- Refatoração profunda de skills (apenas retrofit de markers e tabelas).
- Alterações em x-epic-implement Phase 5 (manual-gate é design intencional, Rule 21).
- Adição de classes Java runtime (violaria Rule 14 — Project Scope Guard).
- Alterações em ADR-0010 ou ADR-0012.

## 4. Stories propostas

### Layer 0 — Foundation (Rules)

- Story 0057-0001 — Expandir tabela "Mandatory Evidence Artifacts" na Rule 24. Regenerar golden.
- Story 0057-0003 — Criar Rule 45 (CI-Watch Integrity) consolidando RULE-045-*.

### Layer 1 — Core Enforcement

- Story 0057-0002 — Implementar `scripts/audit-execution-integrity.sh` (Camada 3) com exit codes e `--self-check`.
- Story 0057-0006 — Estender Stop hook (Camada 2) para verificar `.claude/state/pr-watch-*.json` e novos artefatos.

### Layer 2 — Retrofit (paralelo)

- Story 0057-0004 — Adicionar marker MANDATORY em 6 SKILL.md orquestradoras. Regenerar goldens.
- Story 0057-0005 — Implementar `scripts/audit-bypass-flags.sh` e remediar flags em happy-path.

### Layer 3 — Cross-cutting

- Story 0057-0007 — Promover smoke tests críticos para `mvn test` ou criar hook local pre-push.
- Story 0057-0008 — Decidir aplicação retroativa EPIC-0053 (backfill de evidências ou baseline grandfather).

## 5. Dependências

- 0057-0001 Blocks 0057-0002, 0057-0006.
- 0057-0003 Blocks 0057-0002.
- 0057-0002 Blocks 0057-0004.
- 0057-0001, 0057-0003 Block 0057-0005.
- 0057-0007 e 0057-0008 paralelas a 0057-0005 e 0057-0004.

## 6. Rules aplicáveis

- RULE-001 — Rule 24 (Execution Integrity) é o alvo da expansão.
- RULE-002 — Rule 22 (Skill Visibility) para internal skills referenciadas.
- RULE-003 — Rule 14 (Project Scope Guard) — scripts bash (não Java), respeita scope.
- RULE-004 — Rule 13 (Skill Invocation Protocol) preserva INLINE-SKILL pattern.
- RULE-005 — Rule 08 (Conventional Commits) aplicada aos commits.
- RULE-006 — Rule 21 (Epic Branch Model) — stories PRs targetam `epic/0057`, final gate manual.

## 7. Entrega de valor

Fecha uma vulnerabilidade comprovadamente explorável no protocolo de execução provada pelo EPIC-0053: LLM pode silenciosamente pular sub-skills críticas (CI-watch, reviews, audits) sem que qualquer camada detecte. Pós-épico: Camada 3 CI audit passa a existir; 14 gaps de prosa sem MANDATORY eliminados; flags bypass em happy-path reduzidas a zero; blind spot local (mvn verify só em CI) eliminado. Impacto mensurável: redução para zero de PROTOCOL_VIOLATION silencioso em x-story-implement / x-task-implement / x-release.

## 8. Definition of Done (por story)

- Artefatos alvo atualizados na source of truth (`java/src/main/resources/targets/claude/`).
- Golden files regenerados (`mvn process-resources` + `GoldenFileRegenerator`).
- `mvn verify` passa (incluindo smoke tests).
- Story PR targeta `epic/0057` com auto-merge habilitado (Rule 21).
- Rule 24 retroativo: story produz evidência completa (review + techlead-review + verify-envelope + completion-report).

## 9. Anti-escopo

- NÃO reformula Rule 24 (expansão aditiva).
- NÃO introduz código Java runtime (Rule 14).
- NÃO força backfill destrutivo em `audits/execution-integrity-baseline.txt` fora da Story 0057-0008.
- NÃO altera contrato de `x-pr-watch-ci` ou seus exit codes.
