# Prompt: Geração de Épico e Histórias — ia-dev-environment Context Window Resilience

> **Instrução de uso**: Execute `/x-story-epic-full specs/SPEC-context-window-resilience-v1.md`.

---

## Sistema

**Projeto**: `ia-dev-environment` — CLI generator de ambientes de desenvolvimento assistidos por IA.

**Versão base analisada**: `v2.0.0-SNAPSHOT` (branch `develop`, EPIC-0029 completo).

**Objetivo desta especificação**: Reduzir o consumo de janela de contexto durante a execução de
tasks, stories e epics pelos skills de orquestração (`x-dev-epic-implement`, `x-dev-lifecycle`,
`x-tdd`), prevenindo overflow e melhorando a taxa de sucesso de execuções longas.

**Princípio central de todas as histórias**: A janela de contexto é um recurso finito. Cada token
consumido por instruções de skill, knowledge packs ou outputs intermediários reduz o espaço
disponível para o agente raciocinar e produzir código. A otimização deve preservar a qualidade
das instruções enquanto reduz drasticamente o volume carregado.

---

## Escopo do Épico

### Contexto de negócio

A cadeia de execução `x-dev-epic-implement -> x-dev-lifecycle -> x-tdd -> x-commit` consome
~94K tokens apenas em instruções de skills, sem contar rules (always-loaded, ~14K tokens),
knowledge packs (~24K tokens por subagent), e outputs intermediários (checkpoint files, plans,
reports). Isso causa:

- Estouro frequente da janela de contexto durante execuções de stories complexas
- Degradação na qualidade do raciocínio do agente nas fases finais
- Falhas silenciosas quando o contexto é comprimido automaticamente pelo sistema
- Impossibilidade de executar epics completos em uma única conversa

### Dimensões de melhoria

1. **Context Budget Tracking** — rastreamento do peso de cada skill para decisões de despacho
2. **Skill Instruction Compression** — separação de skills em core + references sob demanda
3. **Lazy Knowledge Pack Loading** — carregamento seletivo de seções de KPs em vez de inteiros
4. **Output Compaction** — redução de outputs intermediários no contexto do orquestrador
5. **Subagent Context Isolation** — auditoria e enforcement de RULE-001
6. **Progressive Skill Loading** — versões "slim" de skills quando invocados de dentro de outros

### Métricas de sucesso

| Métrica | Antes | Target |
|---------|-------|--------|
| Tamanho core x-dev-epic-implement | 1,733 linhas | ≤ 500 linhas |
| Tamanho core x-dev-lifecycle | 733 linhas | ≤ 350 linhas |
| Total cadeia de skills (core) | ~6,363 linhas | ≤ 3,200 linhas (~50%) |
| KP loading por subagent | KP inteiro | Seções relevantes apenas |

---

## Regras de Negócio Transversais (Cross-Cutting Rules)

**RULE-001**: A separação em `SKILL.md` core + `references/*.md` DEVE preservar a funcionalidade
completa do skill. O core contém workflow, flags e fases. As references contêm detalhes de
implementação de cada fase, schemas, e lógica condicional.

**RULE-002**: O core de cada skill DEVE ser auto-suficiente para execução básica. Se uma reference
file não existir, o skill DEVE degradar gracefully com warning (consistente com RULE-012 existente).

**RULE-003**: Skills que invocam outros skills via `Skill` tool DEVEM documentar o contexto
mínimo que o skill invocado precisa. O skill invocado DEVE ter um "slim mode" que carrega apenas
esse mínimo.

**RULE-004**: Outputs intermediários (checkpoint reads, phase reports, review dashboards) DEVEM
ser salvos em arquivo e referenciados por path, NUNCA acumulados inline no contexto do orquestrador.

**RULE-005**: Subagents DEVEM receber apenas metadata (story ID, branch, phase, flags). Source
code, diffs, e conteúdo de KPs NUNCA devem ser passados inline no prompt do subagent — o
subagent deve ler esses arquivos por conta própria.

**RULE-006**: O campo `context-budget` no frontmatter de skills é informativo para o orquestrador
e NÃO afeta o carregamento do skill pelo Claude Code. É consumido apenas por skills orquestradores
que decidem entre execução inline vs. delegação a subagent.

---

## Histórias

---

### STORY-0001: Context Budget Tracking

**Título**: Rastreamento de budget de contexto por skill com campo no frontmatter

**Tipo**: Feature — Skill Template + Orchestrator Logic

**Prioridade**: Alta (fundação para decisões de delegação inteligente)

**Dependências**: Nenhuma.

**Contexto técnico**:
Atualmente nenhum skill declara seu peso de contexto. Os orquestradores (x-dev-lifecycle,
x-dev-epic-implement) despacham subagents ou executam inline sem considerar o impacto no
contexto. Isso leva a estouros imprevisíveis.

**Escopo de implementação**:

1. Adicionar campo `context-budget` ao frontmatter de SKILL.md com valores:
   - `light` (< 200 linhas core, ~3K tokens) — x-format, x-lint, x-commit
   - `medium` (200-500 linhas core, 3-7K tokens) — x-tdd, x-plan-task, x-pr-create
   - `heavy` (> 500 linhas core, > 7K tokens) — x-dev-lifecycle, x-dev-epic-implement, x-review

2. Adicionar ao template Jinja dos skills core a geração do campo `context-budget`

3. Nos orquestradores (x-dev-lifecycle, x-dev-epic-implement), adicionar lógica de decisão:
   - Antes de invocar um skill inline via `Skill` tool, somar o budget acumulado
   - Se acumulado > threshold (configurable, default: `heavy`), forçar delegação via `Agent`
   - Log: `"Context budget exceeded ({accumulated}). Delegating {skill} to subagent."`

4. Arquivo afetado no assembler Java: o `SkillAssembler` que gera o frontmatter YAML

**Critérios de Aceitação (DoD)**:

- [ ] Todos os skills core têm campo `context-budget` no frontmatter
- [ ] O campo é gerado pelo assembler Java (não hardcoded nos templates)
- [ ] x-dev-lifecycle consulta budget antes de invocar skills inline
- [ ] Golden files atualizados com o novo campo
- [ ] Testes de integração passam (`mvn verify -Pintegration-tests`)
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90%

**Gherkin**:

```gherkin
Feature: Context budget tracking por skill

  Scenario: Skill declara budget no frontmatter
    Given um skill template com context-budget: "light"
    When o assembler gera o SKILL.md
    Then o frontmatter contém "context-budget: light"

  Scenario: Orquestrador delega quando budget excede threshold
    Given o budget acumulado na conversa é "heavy"
    And o próximo skill a invocar tem budget "medium"
    When o orquestrador avalia o despacho
    Then o skill é delegado via Agent em vez de Skill inline
    And log contém "Context budget exceeded"

  Scenario: Budget não afeta execução direta do skill
    Given um usuário invoca /x-tdd diretamente
    When o skill carrega
    Then o campo context-budget é ignorado
    And o skill executa normalmente
```

---

### STORY-0002: Skill Instruction Compression via References

**Título**: Separação de skills pesados em core + references sob demanda

**Tipo**: Feature — Skill Template Restructure

**Prioridade**: Alta (maior impacto na redução de contexto)

**Dependências**: Nenhuma.

**Contexto técnico**:
O skill `x-dev-epic-implement` tem 1,733 linhas carregadas integralmente quando invocado.
Muito desse conteúdo é relevante apenas em fases específicas (resume workflow só importa com
`--resume`, preflight analysis só importa com execução paralela, etc.).

**Escopo de implementação**:

1. **x-dev-epic-implement** (1,733 → ~400 core + ~1,300 em references):
   - `SKILL.md` core: workflow overview, phases 0-4, flags, prerequisites, subagent prompt
   - `references/resume-workflow.md`: Phase 0.5 resume detection, reclassification, PR verification
   - `references/merge-modes.md`: auto-merge, no-merge, interactive-merge decision mechanism
   - `references/preflight-analysis.md`: Phase 0.5 conflict analysis, overlap matrix, classification
   - `references/integrity-gate.md`: gate preconditions, subagent prompt, regression diagnosis, version bump
   - `references/checkpoint-schema.md`: execution-state.json schema, per-task fields, story entry schema
   - `references/phase-reports.md`: phase completion report generation, report content

2. **x-dev-lifecycle** (733 → ~300 core + ~430 em references):
   - `SKILL.md` core: phases 0-3 overview, task execution loop, flags
   - `references/planning-phases.md`: Phase 1A-1F details, subagent prompts
   - `references/verification-phase.md`: Phase 3 coverage, consistency, review, PR creation
   - `references/scope-assessment.md`: SIMPLE/STANDARD/COMPLEX classification

3. O core DEVE conter instruções de "Read reference" com condição:
   ```
   If --resume: Read references/resume-workflow.md for resume logic
   If Phase 0.5: Read references/preflight-analysis.md for conflict analysis
   ```

4. O assembler Java DEVE copiar os reference files para o output junto com o SKILL.md

**Critérios de Aceitação (DoD)**:

- [ ] x-dev-epic-implement core ≤ 500 linhas
- [ ] x-dev-lifecycle core ≤ 350 linhas
- [ ] Todas as reference files existem e são legíveis
- [ ] Nenhuma funcionalidade foi removida (apenas redistribuída)
- [ ] Skill executa corretamente com `--dry-run` lendo references
- [ ] Skill executa corretamente com `--resume` lendo resume reference
- [ ] Golden files incluem reference files nos diretórios corretos
- [ ] Testes de integração passam

**Gherkin**:

```gherkin
Feature: Skill instruction compression via references

  Scenario: Core do skill é carregado sem references
    Given o skill x-dev-epic-implement com core de 400 linhas
    When o Claude carrega o skill
    Then apenas o core é carregado (~6K tokens)
    And references não são carregadas automaticamente

  Scenario: Reference é carregada sob demanda
    Given o usuário invoca /x-dev-epic-implement 0042 --resume
    When a Phase 0.5 é alcançada
    Then o skill instrui "Read references/resume-workflow.md"
    And a lógica de resume é executada corretamente

  Scenario: Reference ausente degrada gracefully
    Given references/preflight-analysis.md não existe
    When Phase 0.5 tenta ler o arquivo
    Then um WARNING é emitido: "Reference preflight-analysis.md not found"
    And a execução continua sem preflight analysis

  Scenario: Assembler gera reference files
    Given o template contém references/ com 6 arquivos
    When o assembler executa para profile java-quarkus
    Then o output contém skills/x-dev-epic-implement/SKILL.md
    And o output contém skills/x-dev-epic-implement/references/resume-workflow.md
    And o output contém skills/x-dev-epic-implement/references/merge-modes.md
```

---

### STORY-0003: Lazy Knowledge Pack Loading

**Título**: Carregamento seletivo de seções de KPs em subagents

**Tipo**: Feature — Subagent Prompt Optimization

**Prioridade**: Média (reduz ~24K tokens por subagent da Phase 1)

**Dependências**: Nenhuma.

**Contexto técnico**:
Subagents da Phase 1B-1F recebem instruções como "Read skills/architecture/SKILL.md -> then
read its references". Isso carrega o KP inteiro no contexto do subagent, mesmo que apenas
uma seção seja relevante.

**Escopo de implementação**:

1. Mapear quais seções de cada KP são usadas por cada subagent:
   - Phase 1B (Architect): `architecture/references/architecture-principles.md` + `layer-templates/SKILL.md`
   - Phase 1B (Test Plan): `testing/references/tdd-methodology.md` + `testing/references/test-patterns.md`
   - Phase 1E (Security): `security/references/owasp-top10.md` + `security/references/input-validation.md`
   - Phase 1F (Compliance): `compliance/references/` (seção relevante ao tipo de compliance)

2. Atualizar os prompts de subagent nos templates de x-dev-lifecycle para referenciar
   arquivos específicos em vez do KP inteiro:
   ```
   Antes: "Read skills/security/SKILL.md -> then read its references"
   Depois: "Read skills/security/references/owasp-top10.md and
            skills/security/references/input-validation.md"
   ```

3. Manter o KP SKILL.md como índice navegável, mas não instruir subagents a carregá-lo inteiro

**Critérios de Aceitação (DoD)**:

- [ ] Prompts de subagent em x-dev-lifecycle referenciam arquivos específicos (não KP inteiro)
- [ ] Nenhum subagent recebe instrução "read SKILL.md -> then read its references"
- [ ] Cada subagent lê no máximo 3 reference files do KP (em vez do KP inteiro)
- [ ] A qualidade dos artefatos produzidos (plans, assessments) é preservada
- [ ] Golden files atualizados
- [ ] Testes de integração passam

**Gherkin**:

```gherkin
Feature: Lazy knowledge pack loading em subagents

  Scenario: Security subagent lê apenas references relevantes
    Given um subagent de Security Assessment na Phase 1E
    When o prompt do subagent é gerado
    Then o prompt referencia "security/references/owasp-top10.md"
    And o prompt referencia "security/references/input-validation.md"
    And o prompt NÃO referencia "security/SKILL.md"

  Scenario: Architect subagent lê apenas architecture principles
    Given um subagent de Architecture Planning na Phase 1B
    When o prompt do subagent é gerado
    Then o prompt referencia "architecture/references/architecture-principles.md"
    And o prompt referencia "layer-templates/SKILL.md"
    And o prompt NÃO referencia "architecture/SKILL.md" inteiro
```

---

### STORY-0004: Output Compaction

**Título**: Redução de outputs intermediários no contexto do orquestrador

**Tipo**: Feature — Orchestrator Output Management

**Prioridade**: Média (previne acúmulo progressivo de contexto)

**Dependências**: Nenhuma.

**Contexto técnico**:
Durante a execução de um epic, o orquestrador acumula no contexto: execution-state.json
completo (lido após cada story), phase reports (gerados inline), review dashboards, e logs
detalhados de TDD cycles. Esse acúmulo é cumulativo — cada iteração adiciona mais.

**Escopo de implementação**:

1. **Checkpoint reads seletivos**: Em vez de ler execution-state.json inteiro após cada story,
   ler apenas os campos necessários:
   - Para verificar dependências: `stories.{id}.status` e `stories.{id}.prMergeStatus`
   - Para verificar tasks: `stories.{id}.tasks.{taskId}.status`
   - Usar `jq` ou parsing parcial em vez de Read completo

2. **Phase reports em subagent**: Gerar phase completion reports em subagent dedicado
   que salva em arquivo. O orquestrador recebe apenas `{ "status": "GENERATED", "path": "..." }`

3. **Review output compaction**: x-review já salva dashboard em arquivo. Garantir que o
   orquestrador lê apenas o score final e lista de bloqueios, não o dashboard inteiro

4. **TDD cycle logs**: x-tdd emite log completo de cada ciclo. Instruir para emitir apenas:
   ```
   Cycle N/M: RED ✓ GREEN ✓ REFACTOR skipped → {sha}
   ```
   Em vez do bloco completo com detalhes de cada fase

5. Adicionar instrução nos templates dos orquestradores:
   ```
   CONTEXT MANAGEMENT: Do NOT read full files into context when partial data suffices.
   Use targeted reads (offset/limit) or grep for specific fields.
   ```

**Critérios de Aceitação (DoD)**:

- [ ] Orquestrador lê execution-state.json com targeted reads (não inteiro)
- [ ] Phase reports são gerados em subagent, não inline
- [ ] x-tdd emite logs compactos (1 linha por ciclo) em modo orquestrado
- [ ] Review scores são extraídos sem carregar dashboard inteiro
- [ ] Golden files atualizados
- [ ] Testes de integração passam

**Gherkin**:

```gherkin
Feature: Output compaction para preservar contexto

  Scenario: Checkpoint read seletivo
    Given um execution-state.json com 20 stories
    When o orquestrador verifica dependências de story-0042-0015
    Then apenas os campos status e prMergeStatus das dependências são lidos
    And o arquivo inteiro NÃO é carregado no contexto

  Scenario: TDD logs compactos em modo orquestrado
    Given x-tdd executando dentro de x-dev-lifecycle
    When o Cycle 3 de 5 completa com RED/GREEN/REFACTOR
    Then o log emitido é "Cycle 3/5: RED ✓ GREEN ✓ REFACTOR ✓ → abc1234"
    And detalhes de test name, assertion, e implementation NÃO são emitidos

  Scenario: Phase report gerado em subagent
    Given Phase 0 de um epic completa com 3 stories SUCCESS
    When o phase completion report é gerado
    Then a geração ocorre em um subagent dedicado
    And o orquestrador recebe apenas path do arquivo gerado
```

---

### STORY-0005: Subagent Context Isolation Enforcement

**Título**: Auditoria e enforcement de isolamento de contexto em subagents

**Tipo**: Improvement — Subagent Prompt Audit

**Prioridade**: Média (previne context leaks entre orquestrador e subagents)

**Dependências**: Nenhuma.

**Contexto técnico**:
RULE-001 do x-dev-epic-implement define: "The orchestrator passes ONLY metadata to the subagent.
Never pass source code, knowledge packs, or diffs." Porém não há auditoria sistemática para
verificar que todos os prompts de subagent seguem essa regra.

**Escopo de implementação**:

1. Auditar todos os prompts de subagent em:
   - `x-dev-epic-implement/SKILL.md`: Section 1.4, 1.4a, 1.4c (dispatch prompts)
   - `x-dev-lifecycle/SKILL.md`: Phase 1B, 1D, 1E, 1F (planning subagents)
   - `x-review/SKILL.md`: specialist subagent prompts
   - `x-epic-plan/SKILL.md`: planning subagents

2. Para cada prompt, verificar:
   - Passa apenas metadata (IDs, paths, flags)? ✓
   - Instrui o subagent a ler arquivos por conta própria? ✓
   - NÃO embute conteúdo de arquivos no prompt? ✓
   - NÃO copia KP content inline? ✓

3. Corrigir qualquer violação encontrada

4. Adicionar instrução explícita em cada prompt de subagent:
   ```
   CONTEXT ISOLATION: You receive only metadata. Read all files yourself.
   Do NOT expect source code, diffs, or knowledge pack content in this prompt.
   ```

**Critérios de Aceitação (DoD)**:

- [ ] Todos os prompts de subagent passam apenas metadata
- [ ] Nenhum prompt embute conteúdo de arquivo inline
- [ ] Cada prompt tem instrução explícita de context isolation
- [ ] Golden files atualizados
- [ ] Testes de integração passam

**Gherkin**:

```gherkin
Feature: Subagent context isolation enforcement

  Scenario: Prompt de dispatch contém apenas metadata
    Given o template de prompt para dispatch de story
    When o prompt é gerado para story-0042-0003
    Then o prompt contém storyId, epicId, branchName, phase, flags
    And o prompt NÃO contém código-fonte
    And o prompt NÃO contém conteúdo de knowledge packs

  Scenario: Subagent lê arquivos por conta própria
    Given um subagent de Architecture Planning
    When o subagent inicia execução
    Then o subagent usa Read tool para ler o story file
    And o subagent usa Read tool para ler architecture references
    And nenhum conteúdo foi passado no prompt original
```

---

### STORY-0006: Progressive Skill Loading (Slim Mode)

**Título**: Versões reduzidas de skills para invocação dentro de outros skills

**Tipo**: Feature — Skill Template + Invocation Pattern

**Prioridade**: Baixa (refinamento após compressão principal)

**Dependências**: STORY-0002 (Skill Instruction Compression)

**Contexto técnico**:
Quando x-tdd invoca x-commit via `Skill` tool, o x-commit inteiro (289 linhas) é carregado.
Mas x-tdd só precisa da mecânica de commit, não da explicação de Conventional Commits ou da
tabela de prefixos. O mesmo vale para x-format (226L) e x-lint (245L).

**Escopo de implementação**:

1. Adicionar campo `slim-mode` ao frontmatter de skills que suportam invocação slim:
   ```yaml
   slim-mode: true
   slim-sections: ["Workflow", "Commands", "Error Handling"]
   ```

2. Para skills com slim-mode, criar seção `## Slim Mode` no SKILL.md que contém
   apenas o essencial para invocação de dentro de outro skill:
   - x-commit slim: formato do commit message, pre-commit chain command, error handling
   - x-format slim: comando de formatação, lista de formatters por linguagem
   - x-lint slim: comando de lint, lista de linters por linguagem
   - x-tdd slim: workflow de ciclo RED/GREEN/REFACTOR, sem TPP reference

3. Os orquestradores DEVEM referenciar a seção slim quando invocam via Skill tool:
   ```
   Invoke /x-commit with slim mode: only read the "Slim Mode" section.
   ```

4. A seção slim é um subset do skill, não um arquivo separado (evita duplicação)

**Critérios de Aceitação (DoD)**:

- [ ] x-commit, x-format, x-lint, x-tdd têm seção "Slim Mode" no SKILL.md
- [ ] Cada seção slim ≤ 50 linhas
- [ ] Orquestradores referenciam slim mode ao invocar esses skills
- [ ] Execução direta pelo usuário ignora slim mode (carrega skill completo)
- [ ] Golden files atualizados
- [ ] Testes de integração passam

**Gherkin**:

```gherkin
Feature: Progressive skill loading com slim mode

  Scenario: Skill invocado em slim mode dentro de outro skill
    Given x-tdd está executando um ciclo GREEN
    And x-tdd precisa invocar x-commit
    When x-tdd invoca /x-commit com referência a slim mode
    Then x-commit carrega apenas a seção "Slim Mode" (~50 linhas)
    And o commit é criado corretamente

  Scenario: Skill invocado diretamente pelo usuário carrega completo
    Given um usuário digita /x-commit --task TASK-0042-0001-001
    When o skill é carregado
    Then o SKILL.md completo é carregado (289 linhas)
    And a seção "Slim Mode" é ignorada

  Scenario: Slim mode section é gerada pelo assembler
    Given o template de x-commit contém seção "Slim Mode"
    When o assembler gera para profile java-quarkus
    Then o SKILL.md gerado contém seção "## Slim Mode"
    And a seção tem ≤ 50 linhas
```
