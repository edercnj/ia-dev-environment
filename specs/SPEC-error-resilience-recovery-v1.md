# Prompt: Geração de Épico e Histórias — ia-dev-environment Error Resilience & Recovery

> **Instrução de uso**: Execute `/x-story-epic-full specs/SPEC-error-resilience-recovery-v1.md`.

---

## Sistema

**Projeto**: `ia-dev-environment` — CLI generator de ambientes de desenvolvimento assistidos por IA.

**Versão base analisada**: `v2.0.0-SNAPSHOT` (branch `develop`, EPIC-0029 completo).

**Objetivo desta especificação**: Implementar tratamento robusto de erros internos, transientes e
sistêmicos durante a execução de tasks, stories e epics. Atualmente, os skills de orquestração
não possuem NENHUM tratamento para erros transientes (Claude overloaded, GitHub API rate limit,
tool timeout), erros de contexto (context window exceeded), ou falhas sistêmicas (múltiplas
stories falhando consecutivamente).

**Princípio central de todas as histórias**: A resiliência em execuções de IA é diferente de
resiliência em sistemas distribuídos tradicionais. Erros transientes são comuns (overloaded API,
token limits), e o sistema deve ser capaz de se recuperar automaticamente na maioria dos casos,
escalando para o humano apenas quando a recuperação automática falha.

---

## Escopo do Épico

### Contexto de negócio

Auditoria dos skills de execução revelou:
- **ZERO** tratamento de erros transientes em qualquer skill
- Placeholder não implementado: `[Placeholder: retry with error context — story-0005-0007]`
  na linha 1051 do x-dev-epic-implement
- Se Claude retorna "overloaded", o skill simplesmente falha sem retry
- Se GitHub API retorna 429 (rate limit), o tool call falha sem backoff
- Se um subagent excede timeout, não há recovery
- Se 5 stories consecutivas falham, o epic continua tentando sem avaliar falha sistêmica
- O default `--no-merge` defere o integrity gate entre fases, permitindo que Phase 2
  inicie sem Phase 1 estar integrada

### Dimensões de melhoria

1. **Transient Error Retry** — retry com exponential backoff para erros recuperáveis
2. **Subagent Failure Recovery** — re-despacho com contexto reduzido
3. **Circuit Breaker** — pausa automática em falhas sistêmicas
4. **Graceful Degradation** — degradação progressiva sob pressão de contexto
5. **Error Catalog** — catálogo padronizado de erros com categorias e ações
6. **Merge Gate Default** — gate obrigatório entre fases mesmo sem merge
7. **Checkpoint Error History** — histórico de erros no execution-state.json

### Métricas de sucesso

| Métrica | Antes | Target |
|---------|-------|--------|
| Erros transientes recuperados | 0% | ≥ 80% |
| Falhas sistêmicas detectadas | Nunca | Em ≤ 3 falhas consecutivas |
| Taxa de sucesso de epics completos | ~60% | ≥ 85% |
| Integrity gate entre fases | DEFERRED (default) | Sempre executado |

---

## Regras de Negócio Transversais (Cross-Cutting Rules)

**RULE-001**: Erros transientes (Claude overloaded, GitHub 5xx/429, tool timeout) DEVEM ser
retried com exponential backoff (1s, 2s, 4s) com máximo de 3 tentativas antes de marcar como
falha permanente.

**RULE-002**: Erros permanentes (file not found, invalid ID, compilation error) DEVEM falhar
imediatamente com mensagem contextual. NUNCA retry erros permanentes.

**RULE-003**: O circuit breaker DEVE abrir após 3 falhas consecutivas na mesma fase, pausando
a execução e escalando para o usuário via AskUserQuestion. DEVE resetar ao retomar com `--resume`.

**RULE-004**: O integrity gate entre fases DEVE executar SEMPRE, mesmo quando `mergeMode == "no-merge"`.
No modo no-merge, o gate executa com merge local temporário (branch temporária, testa, descarta).

**RULE-005**: Todo erro DEVE ser registrado no `execution-state.json` com: timestamp, error code,
story/task ID, phase, retry count, e resolução final (SUCCESS_AFTER_RETRY, FAILED, ESCALATED).

**RULE-006**: Graceful degradation é progressiva: Level 1 (reduzir verbosidade) → Level 2
(forçar delegação) → Level 3 (salvar estado e sugerir --resume). NUNCA pular diretamente
para Level 3.

**RULE-007**: O catálogo de erros DEVE ser um reference file do orquestrador, NÃO um knowledge
pack separado. Erros são específicos do runtime de execução, não do domínio.

---

## Histórias

---

### STORY-0001: Transient Error Retry with Backoff

**Título**: Retry com exponential backoff para erros transientes

**Tipo**: Feature — Orchestrator Error Handling

**Prioridade**: Crítica (resolve a causa raiz de ~40% das falhas)

**Dependências**: Nenhuma.

**Contexto técnico**:
Quando um tool call falha com erro transiente (Claude API overloaded, GitHub rate limit),
o skill atual simplesmente propaga o erro como falha da story/task. O resultado é marcado
como FAILED no checkpoint sem tentativa de recuperação.

**Escopo de implementação**:

1. Definir categorias de erro nos templates dos orquestradores:

   | Categoria | Padrões de detecção | Ação |
   |-----------|---------------------|------|
   | TRANSIENT | "overloaded", "rate limit", "429", "503", "504", "timeout", "ETIMEDOUT" | Retry com backoff |
   | CONTEXT | "context", "token limit", "too long", "exceeded" | Graceful degradation |
   | PERMANENT | Todos os demais | Fail imediato |

2. Adicionar instrução de retry nos templates de x-dev-epic-implement e x-dev-lifecycle:
   ```
   ERROR HANDLING — TRANSIENT RETRY:
   When a tool call fails with a transient error pattern (overloaded, rate limit, 429, 503, timeout):
   1. Log: "Transient error detected: {error}. Retry 1/3 in 2s..."
   2. Wait 2 seconds (first retry)
   3. Retry the exact same tool call
   4. If fails again: wait 4 seconds, retry (2/3)
   5. If fails again: wait 8 seconds, retry (3/3)
   6. If all retries fail: mark as FAILED with errorCode ERR-TRANSIENT-{NNN}
   ```

3. Adicionar instrução de detecção no dispatch de subagents:
   ```
   When a subagent returns an error or empty result:
   1. Check if the error matches transient patterns
   2. If transient: retry subagent dispatch (max 2 retries)
   3. If permanent: mark story/task as FAILED immediately
   ```

4. NÃO retry erros de compilação, teste, ou lógica de negócio (esses são permanentes)

**Critérios de Aceitação (DoD)**:

- [ ] Templates de x-dev-epic-implement contêm instrução de transient retry
- [ ] Templates de x-dev-lifecycle contêm instrução de transient retry
- [ ] Categorias de erro estão definidas com padrões de detecção
- [ ] Retry usa backoff exponencial (2s, 4s, 8s)
- [ ] Máximo de 3 retries para tool calls, 2 para subagent dispatch
- [ ] Erros permanentes NÃO são retried
- [ ] Golden files atualizados
- [ ] Testes de integração passam

**Gherkin**:

```gherkin
Feature: Transient error retry com backoff

  Scenario: Tool call com erro transiente é retried
    Given um tool call que retorna "overloaded_error"
    When o orquestrador detecta o erro
    Then o erro é classificado como TRANSIENT
    And o tool call é retried após 2 segundos
    And log contém "Transient error detected. Retry 1/3 in 2s"

  Scenario: Retry com backoff exponencial
    Given um tool call que falha 3 vezes com "rate limit exceeded"
    When o orquestrador executa retries
    Then o primeiro retry ocorre após 2 segundos
    And o segundo retry ocorre após 4 segundos
    And o terceiro retry ocorre após 8 segundos
    And após 3 falhas, o erro é marcado como FAILED

  Scenario: Erro permanente não é retried
    Given um tool call que retorna "file not found"
    When o orquestrador detecta o erro
    Then o erro é classificado como PERMANENT
    And o task é marcado como FAILED imediatamente
    And NENHUM retry é tentado
```

---

### STORY-0002: Subagent Failure Recovery

**Título**: Recuperação de falhas de subagent com contexto reduzido

**Tipo**: Feature — Subagent Dispatch Resilience

**Prioridade**: Alta (subagents são o principal ponto de falha)

**Dependências**: STORY-0001 (Transient Error Retry), STORY-0005 (Error Catalog)

**Contexto técnico**:
Subagents podem falhar por: timeout, crash, resultado inválido, context overflow,
ou erro transiente. O orquestrador atual valida o SubagentResult (Section 1.5) mas
não tenta recuperação.

**Escopo de implementação**:

1. Expandir SubagentResult com campos de erro:
   ```json
   {
     "status": "FAILED",
     "errorType": "TRANSIENT|CONTEXT|PERMANENT|TIMEOUT|INVALID_RESULT",
     "errorMessage": "Context window exceeded during Phase 2",
     "errorCode": "ERR-CONTEXT-002"
   }
   ```

2. Adicionar lógica de recovery no dispatch (Section 1.4/1.4a) dos templates:
   ```
   SUBAGENT FAILURE RECOVERY:
   After validating SubagentResult:
   1. If status == FAILED and errorType == TRANSIENT:
      → Retry dispatch (max 2 times) with same prompt
   2. If status == FAILED and errorType == CONTEXT:
      → Re-dispatch with reduced prompt:
        - Remove optional instructions
        - Use slim mode for invoked skills
        - Add "CONTEXT PRESSURE: minimize output, skip verbose logs"
   3. If status == FAILED and errorType == TIMEOUT:
      → Re-dispatch with --skip-verification flag
   4. If 3 consecutive subagent failures of same errorType:
      → Escalate to user via AskUserQuestion:
        "3 subagent failures ({errorType}). Options: Retry / Skip story / Abort epic"
   ```

3. Registrar cada falha e recovery no checkpoint (ver STORY-0007)

**Critérios de Aceitação (DoD)**:

- [ ] SubagentResult template inclui campos errorType, errorMessage, errorCode
- [ ] Recovery logic está documentada no template de dispatch
- [ ] Transient failures são retried (max 2)
- [ ] Context failures re-despacham com prompt reduzido
- [ ] Timeout failures re-despacham com --skip-verification
- [ ] 3 falhas consecutivas escalam para o usuário
- [ ] Golden files atualizados
- [ ] Testes de integração passam

**Gherkin**:

```gherkin
Feature: Subagent failure recovery

  Scenario: Subagent com erro transiente é re-despachado
    Given um subagent que retorna { status: "FAILED", errorType: "TRANSIENT" }
    When o orquestrador processa o resultado
    Then o subagent é re-despachado com o mesmo prompt
    And log contém "Retrying subagent dispatch (1/2)"

  Scenario: Subagent com context overflow recebe prompt reduzido
    Given um subagent que retorna { status: "FAILED", errorType: "CONTEXT" }
    When o orquestrador re-despacha
    Then o novo prompt inclui "CONTEXT PRESSURE: minimize output"
    And instruções opcionais são removidas do prompt

  Scenario: 3 falhas consecutivas escalam para usuário
    Given 3 subagents consecutivos falharam com errorType "TRANSIENT"
    When o orquestrador detecta o padrão
    Then AskUserQuestion é apresentada com opções Retry/Skip/Abort
```

---

### STORY-0003: Circuit Breaker for Epic Execution

**Título**: Circuit breaker para pausar execução em falhas sistêmicas

**Tipo**: Feature — Epic Orchestrator Resilience

**Prioridade**: Alta (previne desperdício de recursos em falhas sistêmicas)

**Dependências**: STORY-0005 (Error Catalog)

**Contexto técnico**:
Se o Claude API está instável ou há um bug sistêmico nos skills, múltiplas stories falham
consecutivamente. O orquestrador atual continua tentando, desperdiçando contexto e tempo.

**Escopo de implementação**:

1. Adicionar lógica de circuit breaker no template de x-dev-epic-implement:
   ```
   CIRCUIT BREAKER:
   Track consecutive failures in the current phase.
   
   | Consecutive Failures | Action |
   |---------------------|--------|
   | 1 | Log WARNING, continue |
   | 2 | Log WARNING with pattern analysis, continue |
   | 3 | PAUSE: AskUserQuestion "3 consecutive failures. Continue?" |
   | 5 total in phase | ABORT phase with diagnostic report |
   
   Circuit breaker resets:
   - When a story succeeds (consecutiveFailures = 0)
   - When --resume is used (full reset)
   - When user explicitly chooses "Continue" at the PAUSE prompt
   ```

2. Adicionar análise de padrão ao atingir 2 falhas:
   ```
   When consecutiveFailures >= 2:
   Analyze failure patterns:
   - Same errorType across failures? → Likely systemic issue
   - Same phase in lifecycle? → Likely skill bug
   - Different errorTypes? → Likely transient, continue
   Log: "Failure pattern: {analysis}"
   ```

3. Adicionar ao checkpoint:
   ```json
   "circuitBreaker": {
     "consecutiveFailures": 0,
     "totalFailuresInPhase": 0,
     "lastFailurePattern": "TRANSIENT|CONTEXT|MIXED",
     "status": "CLOSED|OPEN|HALF_OPEN"
   }
   ```

**Critérios de Aceitação (DoD)**:

- [ ] Circuit breaker logic está no template de x-dev-epic-implement
- [ ] 3 falhas consecutivas pausam com AskUserQuestion
- [ ] 5 falhas totais na fase abortam com relatório
- [ ] Padrão de falha é analisado a partir de 2 falhas
- [ ] Circuit breaker reseta com sucesso ou --resume
- [ ] Checkpoint inclui campo circuitBreaker
- [ ] Golden files atualizados
- [ ] Testes de integração passam

**Gherkin**:

```gherkin
Feature: Circuit breaker para epic execution

  Scenario: 3 falhas consecutivas pausam execução
    Given stories 0001, 0002 e 0003 falharam consecutivamente
    When a story 0003 falha
    Then AskUserQuestion é apresentada: "3 consecutive failures"
    And opções incluem "Continue", "Skip phase", "Abort"

  Scenario: Sucesso reseta circuit breaker
    Given 2 stories falharam consecutivamente
    When a próxima story completa com SUCCESS
    Then consecutiveFailures reseta para 0
    And status do circuit breaker permanece CLOSED

  Scenario: 5 falhas totais abortam a fase
    Given 5 stories falharam no total na fase atual
    When a 5ª falha é registrada
    Then a fase é abortada com relatório diagnóstico
    And log contém "Circuit breaker OPEN: phase aborted"
```

---

### STORY-0004: Graceful Degradation on Context Pressure

**Título**: Degradação progressiva sob pressão de contexto

**Tipo**: Feature — Context Pressure Management

**Prioridade**: Média (refinamento que depende de fundações anteriores)

**Dependências**: EPIC-0030/STORY-0001 (Context Budget), STORY-0002 (Subagent Recovery), STORY-0005 (Error Catalog)

**Contexto técnico**:
Quando a janela de contexto se aproxima do limite, o sistema Claude Code comprime
mensagens anteriores automaticamente. Isso pode causar perda de instruções críticas
do skill. Não existe mecanismo para o skill detectar ou reagir a essa pressão.

**Escopo de implementação**:

1. Definir sinais de pressão de contexto nos templates:
   ```
   CONTEXT PRESSURE DETECTION:
   Monitor for these signals during execution:
   
   Level 1 (Warning):
   - Subagent returns truncated output (missing expected fields)
   - Tool call returns "output too large" message
   - Multiple phases completed in single conversation (>= 3 phases)
   
   Level 2 (Critical):
   - System compression message detected in conversation
   - Subagent fails with context-related error
   - Tool calls start failing with token limit errors
   
   Level 3 (Emergency):
   - Multiple tool calls failing consecutively
   - Cannot read files that previously existed
   - Responses becoming incoherent or losing skill instructions
   ```

2. Ações de degradação progressiva:
   ```
   Level 1 Actions:
   - Reduce log verbosity: only emit status lines, no details
   - Skip optional phases (1D Event Schema if not event-driven)
   - Use slim mode for all skill invocations
   
   Level 2 Actions:
   - Force all remaining work into subagents (max context isolation)
   - Skip Phase 3 reviews (use --skip-verification implicitly)
   - Emit: "CONTEXT PRESSURE Level 2: delegating remaining work to subagents"
   
   Level 3 Actions:
   - Save execution state immediately
   - Emit: "CONTEXT PRESSURE Level 3: saving state. Resume with --resume in a new conversation"
   - Stop execution gracefully
   ```

3. Adicionar ao checkpoint quando degradation é acionada

**Critérios de Aceitação (DoD)**:

- [ ] Sinais de pressão de contexto estão definidos nos templates
- [ ] 3 níveis de degradação com ações progressivas
- [ ] Level 1 reduz verbosidade
- [ ] Level 2 força delegação a subagents
- [ ] Level 3 salva estado e sugere --resume
- [ ] Degradação nunca pula de Level 1 para Level 3
- [ ] Golden files atualizados
- [ ] Testes de integração passam

**Gherkin**:

```gherkin
Feature: Graceful degradation sob pressão de contexto

  Scenario: Level 1 - redução de verbosidade
    Given 3 fases já foram executadas na conversa atual
    When o orquestrador detecta signal de Level 1
    Then logs passam a emitir apenas status lines
    And fases opcionais são puladas

  Scenario: Level 2 - delegação forçada
    Given compressão de sistema foi detectada
    When o orquestrador detecta signal de Level 2
    Then todas as fases restantes são delegadas a subagents
    And log contém "CONTEXT PRESSURE Level 2"

  Scenario: Level 3 - salvamento e pausa
    Given múltiplos tool calls estão falhando consecutivamente
    When o orquestrador detecta signal de Level 3
    Then execution-state.json é salvo imediatamente
    And mensagem sugere "--resume in a new conversation"
    And execução para gracefully
```

---

### STORY-0005: Error Catalog & Standardized Error Responses

**Título**: Catálogo padronizado de erros para skills de execução

**Tipo**: Feature — Error Standardization

**Prioridade**: Alta (fundação para retry, circuit breaker e diagnostico)

**Dependências**: Nenhuma.

**Contexto técnico**:
Cada skill trata erros de forma ad-hoc com mensagens diferentes. Não existe
padronização que permita ao orquestrador classificar e reagir a erros de forma
sistemática.

**Escopo de implementação**:

1. Criar reference file `references/error-catalog.md` no skill x-dev-epic-implement:

   | Code | Category | Retryable | Pattern | Action |
   |------|----------|-----------|---------|--------|
   | ERR-TRANSIENT-001 | TRANSIENT | Yes | "overloaded", "capacity" | Retry 3x with backoff |
   | ERR-TRANSIENT-002 | TRANSIENT | Yes | "rate limit", "429" | Retry 3x with backoff |
   | ERR-TRANSIENT-003 | TRANSIENT | Yes | "timeout", "ETIMEDOUT" | Retry 2x with backoff |
   | ERR-TRANSIENT-004 | TRANSIENT | Yes | "503", "504", "502" | Retry 3x with backoff |
   | ERR-CONTEXT-001 | CONTEXT | No | "context", "token limit" | Graceful degradation |
   | ERR-CONTEXT-002 | CONTEXT | No | "output too large", "truncated" | Re-dispatch with reduced prompt |
   | ERR-PERM-001 | PERMANENT | No | "not found", "no such file" | Fail with path suggestion |
   | ERR-PERM-002 | PERMANENT | No | "invalid", "malformed" | Fail with format guidance |
   | ERR-PERM-003 | PERMANENT | No | "compilation", "compile error" | Fail with error details |
   | ERR-PERM-004 | PERMANENT | No | "test failure", "assertion" | Fail with test output |
   | ERR-PERM-005 | PERMANENT | No | "permission denied", "forbidden" | Fail with access guidance |
   | ERR-CIRCUIT-001 | CIRCUIT | No | 3+ consecutive failures | Pause with AskUserQuestion |
   | ERR-CIRCUIT-002 | CIRCUIT | No | 5+ total failures in phase | Abort phase |

2. Adicionar instrução de classificação nos templates dos orquestradores:
   ```
   ERROR CLASSIFICATION:
   When any tool call or subagent returns an error:
   1. Read references/error-catalog.md
   2. Match error message against patterns in the catalog
   3. Apply the action specified for the matched error code
   4. If no pattern matches: classify as PERMANENT (conservative)
   5. Log: "Error classified: {errorCode} ({category}). Action: {action}"
   ```

3. O catálogo é gerado pelo assembler como reference file do skill

**Critérios de Aceitação (DoD)**:

- [ ] Error catalog reference file criado com todos os códigos
- [ ] Cada código tem: category, retryable flag, detection pattern, action
- [ ] Instrução de classificação nos templates de x-dev-epic-implement e x-dev-lifecycle
- [ ] Erros sem match são classificados como PERMANENT (conservador)
- [ ] Golden files incluem o error-catalog.md
- [ ] Testes de integração passam

**Gherkin**:

```gherkin
Feature: Error catalog e classificação padronizada

  Scenario: Erro transiente é classificado corretamente
    Given um tool call retorna "Error: overloaded_error"
    When o orquestrador classifica o erro
    Then o erro é mapeado para ERR-TRANSIENT-001
    And a ação é "Retry 3x with backoff"
    And log contém "Error classified: ERR-TRANSIENT-001 (TRANSIENT)"

  Scenario: Erro sem match é classificado como permanente
    Given um tool call retorna "Error: unknown cosmic ray"
    When o orquestrador classifica o erro
    Then o erro é classificado como PERMANENT (conservador)
    And NENHUM retry é tentado

  Scenario: Error catalog é gerado como reference file
    Given o assembler executa para profile java-quarkus
    When os skills são gerados
    Then x-dev-epic-implement/references/error-catalog.md existe
    And contém pelo menos 12 códigos de erro
```

---

### STORY-0006: Merge Gate Between Phases (Default Behavior)

**Título**: Integrity gate obrigatório entre fases mesmo sem merge

**Tipo**: Feature — Phase Transition Safety

**Prioridade**: Alta (resolve gap de integridade entre fases)

**Dependências**: Nenhuma.

**Contexto técnico**:
O default `--no-merge` defere o integrity gate (`integrityGate.status = "DEFERRED"`),
permitindo que Phase 2 inicie sem validar que Phase 1 integra corretamente. Isso é
perigoso quando stories da Phase 2 dependem de código da Phase 1.

O `--interactive-merge` já existe mas exige que o usuário opte por ele. A maioria dos
usuários usa o default e não percebe que o gate está sendo pulado.

**Escopo de implementação**:

1. Adicionar novo modo `--safe-gate` (ou alterar comportamento de `--no-merge`):
   ```
   When mergeMode == "no-merge" AND --safe-gate is set (or default):
   Instead of DEFERRED, execute a LOCAL integrity gate:
   1. Create temporary branch: temp/gate-phase-{N}-{timestamp}
   2. Merge all SUCCESS story branches into temp branch:
      git merge origin/feat/story-{id} --no-edit (for each)
   3. Run compile + test + coverage on temp branch
   4. If PASS: log "Local integrity gate PASSED for phase {N}"
   5. If FAIL: log "Local integrity gate FAILED" with details
   6. Delete temp branch: git branch -D temp/gate-phase-{N}-{timestamp}
   7. Record gate result in checkpoint (PASS/FAIL, not DEFERRED)
   ```

2. Alterar o default em x-dev-epic-implement:
   - Antes: `--no-merge` defere integrity gate
   - Depois: `--no-merge` executa local integrity gate (temp merge)
   - Flag `--skip-gate` para explicitamente pular (mantém opt-out)

3. Adicionar prompt ao final de cada fase quando gate PASS:
   ```
   Phase {N} integrity gate PASSED (local merge validation).
   All {count} story branches integrate cleanly.
   
   PRs are still OPEN (--no-merge mode). You can:
   1. Merge PRs now and continue
   2. Continue to next phase (PRs remain open)
   3. Pause for manual review
   ```

**Critérios de Aceitação (DoD)**:

- [ ] Local integrity gate executa por default quando `--no-merge`
- [ ] Gate cria branch temporária, faz merge, testa, e deleta
- [ ] Gate result registrado no checkpoint (nunca DEFERRED por default)
- [ ] Flag `--skip-gate` permite pular explicitamente
- [ ] Prompt ao final de cada fase com opções
- [ ] Golden files atualizados
- [ ] Testes de integração passam

**Gherkin**:

```gherkin
Feature: Integrity gate obrigatório entre fases

  Scenario: Gate local executa no modo no-merge
    Given --no-merge está ativo (default)
    And Phase 0 completou com 3 stories SUCCESS
    When a transição para Phase 1 ocorre
    Then uma branch temporária é criada
    And as 3 story branches são merged na temp branch
    And compile + test + coverage executam
    And a branch temporária é deletada
    And o gate result é PASS ou FAIL (não DEFERRED)

  Scenario: Gate FAIL bloqueia próxima fase
    Given o local integrity gate FALHA (testes quebram no merge)
    When o resultado é registrado
    Then a próxima fase NÃO inicia
    And o usuário é notificado com detalhes da falha
    And opções incluem "Fix and retry" e "Skip gate (--skip-gate)"

  Scenario: --skip-gate permite pular explicitamente
    Given o usuário passa --skip-gate
    When a transição entre fases ocorre
    Then o integrity gate é pulado
    And log contém "Integrity gate skipped (--skip-gate)"
    And o gate é registrado como SKIPPED (não DEFERRED)
```

---

### STORY-0007: Checkpoint Error History

**Título**: Histórico de erros no execution-state.json para diagnóstico

**Tipo**: Feature — Checkpoint Schema Extension

**Prioridade**: Média (diagnóstico e melhoria contínua)

**Dependências**: STORY-0005 (Error Catalog)

**Contexto técnico**:
O execution-state.json registra status de stories/tasks mas não registra histórico de
erros. Quando um epic falha, não há como diagnosticar padrões de falha sem revisar logs
manuais.

**Escopo de implementação**:

1. Adicionar campos ao schema do execution-state.json:
   ```json
   {
     "version": "3.0",
     "errorHistory": [
       {
         "timestamp": "2026-04-08T14:30:00Z",
         "storyId": "story-0042-0003",
         "taskId": "TASK-0042-0003-002",
         "errorCode": "ERR-TRANSIENT-001",
         "errorMessage": "Claude API overloaded",
         "phase": "2.2.5",
         "retryCount": 2,
         "resolution": "SUCCESS_AFTER_RETRY"
       }
     ],
     "circuitBreaker": {
       "consecutiveFailures": 0,
       "totalFailuresInPhase": 0,
       "lastFailureAt": null,
       "lastFailurePattern": null,
       "status": "CLOSED"
     },
     "contextPressure": {
       "currentLevel": 0,
       "degradationActivatedAt": null,
       "phasesCompletedInConversation": 0
     }
   }
   ```

2. Instruir orquestradores a registrar cada erro:
   ```
   ERROR HISTORY RECORDING:
   After every error (transient, permanent, or context):
   1. Append to errorHistory array in execution-state.json
   2. Update circuitBreaker counters
   3. Update contextPressure if applicable
   ```

3. Bump schema version para "3.0" (backward compat: "2.0" files sem errorHistory continuam funcionando)

4. Na retomada (--resume), incluir error history summary no log:
   ```
   Resume: Previous execution had {N} errors.
   Pattern: {most common errorCode} ({count} occurrences)
   Circuit breaker: {status}
   ```

**Critérios de Aceitação (DoD)**:

- [ ] Schema version bumped para "3.0"
- [ ] Campo errorHistory adicionado com estrutura definida
- [ ] Campo circuitBreaker adicionado com estados CLOSED/OPEN/HALF_OPEN
- [ ] Campo contextPressure adicionado com níveis 0-3
- [ ] Backward compat: "2.0" files sem novos campos continuam funcionando
- [ ] Resume mostra summary de erros anteriores
- [ ] Golden files atualizados
- [ ] Testes de integração passam

**Gherkin**:

```gherkin
Feature: Checkpoint error history

  Scenario: Erro é registrado no histórico
    Given um tool call falha com ERR-TRANSIENT-001
    When o erro é processado
    Then uma entrada é adicionada ao errorHistory
    And contém timestamp, errorCode, storyId, phase, retryCount
    And execution-state.json é persistido

  Scenario: Resume mostra summary de erros anteriores
    Given execution-state.json contém 5 erros no errorHistory
    When o usuário executa --resume
    Then log contém "Previous execution had 5 errors"
    And log contém o padrão mais comum de erro

  Scenario: Backward compatibility com schema 2.0
    Given execution-state.json com version "2.0" (sem errorHistory)
    When o orquestrador lê o checkpoint
    Then a execução continua normalmente
    And errorHistory é inicializado como array vazio
    And version é mantida como "2.0" (não auto-upgrade)
```
