# Prompt: Geração de Épico e Histórias — ia-dev-environment Model Selection Enforcement & Token Optimization

> **Instrução de uso**: Execute `/x-epic-decompose specs/SPEC-model-selection-enforcement-v1.md --no-jira`.

---

## Sistema

**Projeto**: `ia-dev-environment` — CLI generator de ambientes de desenvolvimento assistidos por IA.

**Versão base analisada**: `develop @ 3da5f9a18` (pós-EPIC-0040 Telemetry Instrumentation).

**Objetivo desta especificação**: Reduzir o consumo de tokens em modelos premium (Opus) durante a
execução de skills e agents de orquestração, aplicando seleção determinística de modelo
(Opus / Sonnet / Haiku) por camada de responsabilidade — planejamento profundo em Opus,
orquestração e revisão em Sonnet, operações utilitárias em Haiku. Preservar qualidade dos
artefatos enquanto reduz em ~22% o custo de execução de epic completo.

**Princípio central de todas as histórias**: O modelo é um recurso econômico. Cada token em
Opus custa ~2× Sonnet e ~5× Haiku. A seleção de modelo DEVE ser explícita no ponto de
invocação — frontmatter de skills, parâmetro `model:` em `Agent(...)` e `Skill(...)`, e metadata
dos agents. "Default implícito" (herdar do pai) é a raiz do problema e deve ser eliminada nos
pontos de alta alavancagem.

---

## Escopo do Épico

### Contexto de negócio

O dashboard de consumo de 17/abr/2026 revela: **84,4% dos tokens gastos em `claude-opus-4-6`**
(1,8M in / 37,8M out) contra apenas 0,2% em Sonnet 4.6 e 5,8% em Haiku 4.5. Auditoria dos 72
skills e 10 agents expõe três gaps técnicos:

1. **Nenhum skill declara `model:` no frontmatter** — todas 72 skills herdam Opus do contexto pai.
2. **Invocações `Agent(subagent_type: "general-purpose", ...)` e `Skill(skill: "...")` não passam
   `model:` como parâmetro** — a "RULE-009" referenciada em 8 locais (`x-arch-plan:153,255`,
   `x-story-implement:429`, `x-test-plan:36,57,310`, `x-story-plan` subagent prompts) vive apenas
   em prosa, sem enforcement técnico.
3. **8 dos 10 agents declaram `Recommended Model: Adaptive`** — placeholder não-determinístico que
   na prática resolve para Opus.

Cada execução de epic amplifica esses defaults por cascateamento: `x-epic-implement` →
`x-story-implement` → `x-test-tdd` → `x-git-commit` — 4 níveis de Opus onde 3 poderiam ser Sonnet
ou Haiku. Estimativa de desperdício: **~7.850 tokens/execução** em alvos corrigíveis sem perda
de qualidade.

### Dimensões de melhoria

1. **Frontmatter `model:` em skills** — declaração explícita no YAML header (os 72 skills herdam
   Opus hoje; queremos ≥25 com seleção explícita após o épico).
2. **`Agent(...)` com `model:` parâmetro** — invocações de `general-purpose` subagents passam o
   modelo explicitamente (5 subagents em `x-story-plan` + 4 em `x-arch-plan`/`x-test-plan`).
3. **`Skill(...)` com `model:` parâmetro** — chamadas entre skills em orquestradores passam
   o modelo apropriado (alvo: ~30 call-sites em `x-epic-implement`, `x-story-implement`,
   `x-review`, `x-release`).
4. **Agent metadata determinístico** — substituir `Recommended Model: Adaptive` pelo modelo
   específico (8 agents afetados; `product-owner` de Opus → Sonnet).
5. **Haiku eligibility** — marcar skills utilitárias (git ops, format, lint, knowledge packs
   read-only) como `model: haiku` (10 skills candidatas).
6. **Governança via Rule nova + CI audit** — Rule nova de model selection + script de audit
   bash que falha no CI quando invocações sem `model:` aparecem em skills de orquestração.

### Métricas de sucesso

| Métrica | Antes | Target |
|---|---|---|
| % tokens em Opus | 84,4% | ≤ 50% |
| % tokens em Sonnet | 0,2% | ≥ 35% |
| % tokens em Haiku | 5,8% | ≥ 12% |
| Skills com `model:` frontmatter declarado | 0 / 72 | ≥ 25 |
| `Agent(...)` sem `model:` em skills de orquestração | ~20 | 0 |
| `Skill(...)` sem `model:` em orquestradores | ~30 | 0 |
| Agents com `Recommended Model: Adaptive` | 8 / 10 | 0 |
| Audit script de CI cobrindo as 3 regras acima | Inexistente | Executa em cada PR |

---

## Regras de Negócio Transversais (Cross-Cutting Rules)

**RULE-001**: Toda skill identificada como "orquestrador" (disparar múltiplos `Agent()` ou
`Skill()`) DEVE declarar `model:` no frontmatter. O valor default para orquestradores é `sonnet`
(não-planejamento), exceto quando a skill é dedicada a design arquitetural profundo (`x-arch-plan`,
subagent Architect em `x-story-plan`), onde `opus` é apropriado.

**RULE-002**: Toda invocação `Agent(subagent_type: "general-purpose", ...)` dentro de um skill
DEVE passar o parâmetro `model:` explicitamente. Heranças implícitas do contexto pai são
proibidas em delegações (a skill pai pode rodar em Opus, mas o subagent decide seu próprio
tier).

**RULE-003**: Toda invocação `Skill(skill: "...", ...)` dentro de um orquestrador DEVE passar
o parâmetro `model:` explicitamente quando o skill invocado tem perfil de tier diferente do
pai. Exceção: skills invocadas pelo usuário diretamente (entry points) não precisam —
herança vale para caso de invocação direta pelo humano.

**RULE-004**: Agents em `.claude/agents/*.md` DEVEM declarar `Recommended Model` com valor
determinístico (`Opus`, `Sonnet` ou `Haiku`). O valor `Adaptive` é proibido. Agents de revisão
(qa, security, sre, performance, tech-lead, devops, devsecops) usam `Sonnet` por padrão. Agents
de planejamento profundo (`architect`) usam `Opus`. Agents de validação leve (`product-owner`)
usam `Sonnet`.

**RULE-005**: Skills elegíveis para `model: haiku` são aquelas que (a) executam operações
utilitárias sem raciocínio de design (git ops, format, lint), OU (b) são knowledge packs
read-only consumidos como referência (não executam lógica). A lista alvo inicial é:
`x-git-worktree`, `x-git-commit`, `x-code-format`, `x-code-lint`, `architecture`,
`coding-standards`, `testing`, `layer-templates`, `patterns`, `dockerfile`.

**RULE-006**: O CI do projeto DEVE executar um audit script (`scripts/audit-model-selection.sh`)
em cada PR. O script falha quando detecta: (a) `Agent(subagent_type: "general-purpose", ...)`
sem `model:` em skill de orquestração declarada; (b) `Skill(skill: "x-...", ...)` sem `model:`
em orquestrador; (c) agent com `Recommended Model: Adaptive`. Skills/agents fora da matriz
de enforcement são ignorados (backward compatibility).

**RULE-007**: Backward compatibility — o épico NÃO altera o comportamento de skills existentes
que não estão na matriz de enforcement (skills menores, user-invoked, ou não-orquestrador). A
seleção de modelo é **aditiva**: adicionar `model:` onde não existe; não remove nem modifica
skills fora do escopo declarado. O audit script do CI começa com os alvos desta matriz e pode
ser expandido em épicos futuros.

---

## Histórias

---

### STORY-0001: Rule nova — Model Selection Strategy (foundation)

**Título**: Criação da Rule de estratégia de seleção de modelos com matriz de tiers e audit contract

**Tipo**: Feature — Rule / Governance

**Prioridade**: Alta (foundation — bloqueia todas as demais stories)

**Dependências**: Nenhuma.

**Contexto técnico**:
Não existe hoje um documento normativo que defina quando usar Opus, Sonnet ou Haiku, nem como
aplicar essa decisão tecnicamente (frontmatter, Agent param, Skill param). A RULE-009 citada em
8 locais vive apenas em prosa dentro de SKILL.md específicos, sem status de rule normativa.

**Escopo de implementação**:

1. Criar rule normativa **Rule 23** (próximo slot livre após Rule 22 do EPIC-0049).
   Nome: `model-selection-strategy.md`. Numeração consistente com todos os demais artefatos
   deste épico (stories, IMPLEMENTATION-MAP).

2. Seções obrigatórias do arquivo:
   - **Purpose** — justifica o trade-off Opus/Sonnet/Haiku.
   - **Matrix** — tabela que lista por camada (orquestrador, planner, reviewer, executor,
     utility, KP) o tier padrão e exemplos.
   - **Enforcement points** — três contratos técnicos (frontmatter `model:`, `Agent(model:)`,
     `Skill(model:)`) com sintaxe exata.
   - **Agent metadata** — contrato para `Recommended Model` em `.claude/agents/*.md`.
   - **Haiku eligibility criteria** — critérios (a) e (b) conforme RULE-005.
   - **Audit contract** — comando bash canônico (padrão EPIC-0033 Rule 13), com grep e
     expectativa de 0 matches.
   - **Backward compatibility** — escopo aditivo, skills fora da matriz ignoradas.
   - **Exceptions** — lista explícita (ex: user-invoked skills não precisam de `model:` no
     frontmatter).

3. Arquivo source-of-truth: `java/src/main/resources/targets/claude/rules/23-model-selection.md`.
   Output gerado em `.claude/rules/23-model-selection.md`.

4. Atualizar `CLAUDE.md` e `.claude/rules/README.md` para referenciar a nova rule no índice.

**Critérios de Aceitação (DoD)**:

- [ ] Arquivo de rule existe em source-of-truth com todas as 8 seções listadas.
- [ ] Golden files regenerados cobrem o novo rule em todos os 17 stacks suportados.
- [ ] `CLAUDE.md` tabela de rules inclui a nova entrada.
- [ ] Testes de integração passam (`mvn verify`).
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90% no módulo de Rule assembly.
- [ ] Audit script mencionado na seção "Audit contract" tem shebang e é executável.

**Gherkin**:

```gherkin
Feature: Rule de model selection strategy

  Scenario: Rule é gerada no output para stack java-picocli
    Given o template da rule em targets/claude/rules/23-model-selection.md
    When o assembler gera o profile "java-picocli"
    Then o output contém ".claude/rules/23-model-selection.md"
    And a rule contém a seção "## Matrix"
    And a rule contém a seção "## Enforcement points"

  Scenario: Rule referencia os contratos técnicos corretos
    Given a rule model-selection-strategy
    When um desenvolvedor a lê
    Then ela cita "frontmatter model:"
    And ela cita "Agent(subagent_type: \"general-purpose\", model: \"<tier>\", ...)"
    And ela cita "Skill(skill: \"...\", model: \"<tier>\", ...)"

  Scenario: Audit contract é executável
    Given o comando bash citado na seção "Audit contract"
    When executado em um repo sem violações
    Then retorna 0 matches
    And exit code é 0
```

---

### STORY-0002: Frontmatter `model:` em orquestradores pesados

**Título**: Declaração de `model: sonnet` nos 4 skills de orquestração mais pesados

**Tipo**: Feature — Skill Template Metadata

**Prioridade**: Alta (captura ~40% da redução total de tokens)

**Dependências**: STORY-0001 (matriz da rule define `sonnet` como default para orquestradores).

**Contexto técnico**:
Os 4 orquestradores com maior peso de SKILL.md são: `x-epic-implement` (1.941 linhas),
`x-story-implement` (1.263 linhas), `x-release` (1.646 linhas), `x-review` (374 linhas). Cada
invocação via `Skill()` reinjeta o body inteiro — multiplicar por Opus é caro. Nenhum deles
realiza raciocínio de design; orquestração é tarefa apropriada para Sonnet.

**Escopo de implementação**:

1. Adicionar `model: sonnet` no frontmatter YAML dos 4 arquivos:
   - `java/src/main/resources/targets/claude/skills/core/x-epic-implement/SKILL.md`
   - `java/src/main/resources/targets/claude/skills/core/x-story-implement/SKILL.md`
   - `java/src/main/resources/targets/claude/skills/core/x-release/SKILL.md`
   - `java/src/main/resources/targets/claude/skills/core/x-review/SKILL.md`

2. Cada arquivo recebe uma linha `model: sonnet` entre `name:` e `description:` no frontmatter
   (consistente com a ordem observada em outros SKILL.md).

3. Regenerar golden files para todos os 17 stacks suportados.

4. Não alterar body de SKILL.md (escopo restrito a metadata).

**Critérios de Aceitação (DoD)**:

- [ ] Os 4 arquivos têm linha `model: sonnet` no frontmatter.
- [ ] `grep -n "^model: " .claude/skills/x-{epic-implement,story-implement,release,review}/SKILL.md`
      retorna 4 linhas.
- [ ] Golden files regenerados sem diffs além da linha `model:`.
- [ ] Testes passam em `mvn verify`.
- [ ] Smoke test: `/x-review` invocado em conversa de teste carrega sem erro de frontmatter.

**Gherkin**:

```gherkin
Feature: Orquestradores pesados declaram model: sonnet

  Scenario: x-epic-implement tem model sonnet
    Given o arquivo x-epic-implement/SKILL.md gerado
    When o frontmatter é parseado
    Then o campo "model" vale "sonnet"

  Scenario: Orquestrador carrega sem erro com model declarado
    Given o claude-code lê x-review/SKILL.md
    When o skill é invocado
    Then nenhum warning de frontmatter é emitido
    And o subagent criado usa modelo sonnet
```

---

### STORY-0003: Frontmatter `model:` em orquestradores secundários

**Título**: Declaração de `model: sonnet` nos orquestradores de suporte

**Tipo**: Feature — Skill Template Metadata

**Prioridade**: Média (amplifica cobertura da matriz de enforcement)

**Dependências**: STORY-0001.

**Contexto técnico**:
Além dos 4 pesados da STORY-0002, existem orquestradores menores que também invocam
`Skill()` em cascata: `x-epic-orchestrate` (621 linhas), `x-pr-fix-epic` (1.298 linhas),
`x-task-implement` (627 linhas), `x-epic-decompose` (variável).

**Escopo de implementação**:

1. Adicionar `model: sonnet` em:
   - `x-epic-orchestrate/SKILL.md`
   - `x-pr-fix-epic/SKILL.md`
   - `x-task-implement/SKILL.md`
   - `x-epic-decompose/SKILL.md`

2. Regenerar golden files.

**Critérios de Aceitação (DoD)**:

- [ ] Os 4 arquivos têm `model: sonnet` no frontmatter.
- [ ] Golden files regenerados.
- [ ] Testes passam.

**Gherkin**:

```gherkin
Feature: Orquestradores secundários declaram model

  Scenario: x-epic-orchestrate tem model sonnet
    Given x-epic-orchestrate/SKILL.md gerado
    When o frontmatter é lido
    Then o campo "model" vale "sonnet"
```

---

### STORY-0004: Frontmatter `model: haiku` em skills utilitárias e knowledge packs

**Título**: Declaração de `model: haiku` em skills de operações git, format/lint e KPs read-only

**Tipo**: Feature — Skill Template Metadata

**Prioridade**: Média (captura ~15% da redução via Haiku-eligibility)

**Dependências**: STORY-0001 (rule define critérios de haiku-eligibility).

**Contexto técnico**:
Skills utilitárias que executam comandos git ou format/lint, e knowledge packs usados como
referência read-only, não requerem raciocínio premium. Haiku é 2,5× mais barato que Sonnet
sem perda de qualidade nessas tarefas.

**Escopo de implementação**:

1. Adicionar `model: haiku` em skills utilitárias:
   - `x-git-worktree`, `x-git-commit`, `x-code-format`, `x-code-lint`

2. Adicionar `model: haiku` em knowledge packs read-only:
   - `architecture`, `coding-standards`, `testing`, `layer-templates`, `patterns`, `dockerfile`

3. Regenerar golden files.

**Critérios de Aceitação (DoD)**:

- [ ] Os 10 arquivos têm `model: haiku` no frontmatter.
- [ ] Golden files regenerados.
- [ ] Testes passam.
- [ ] Smoke test: `x-git-commit` executa um commit de teste sem regressão.

**Gherkin**:

```gherkin
Feature: Skills utilitárias e KPs declaram model: haiku

  Scenario: x-git-commit usa Haiku
    Given x-git-commit/SKILL.md
    When o frontmatter é lido
    Then o campo "model" vale "haiku"

  Scenario: KP architecture usa Haiku
    Given architecture/SKILL.md
    When o frontmatter é lido
    Then o campo "model" vale "haiku"
```

---

### STORY-0005: `Agent(...)` com `model:` explícito em x-story-plan

**Título**: Refator dos 5 subagents de x-story-plan para passar `model:` parâmetro explícito

**Tipo**: Feature — Subagent Invocation Refactor

**Prioridade**: Alta (captura ~13% da redução; elimina o maior ponto único de cascata Opus)

**Dependências**: STORY-0001.

**Contexto técnico**:
`x-story-plan` (1.007 linhas) dispara 5 subagents em paralelo via `Agent(subagent_type:
"general-purpose", ...)`. O texto atual documenta "model hint: opus" ou "model hint: sonnet" em
prosa, mas nenhum `model:` parameter é passado na chamada — todos os 5 herdam Opus.

**Escopo de implementação**:

1. No arquivo `java/src/main/resources/targets/claude/skills/core/x-story-plan/SKILL.md`,
   localizar as 5 invocações de subagents (aprox. linhas 231-436).

2. Refatorar cada chamada para incluir `model:` conforme matriz:
   - Subagent 1 (Architect) — `model: "opus"` (design profundo justifica Opus)
   - Subagent 2 (QA Engineer) — `model: "sonnet"`
   - Subagent 3 (Security Engineer) — `model: "sonnet"`
   - Subagent 4 (Tech Lead) — `model: "sonnet"`
   - Subagent 5 (Product Owner) — `model: "sonnet"`

3. Alinhar com o Pattern 2 (SUBAGENT-GENERAL) da Rule 13: formato exato
   `Agent(subagent_type: "general-purpose", model: "<tier>", description: "...", prompt: "...")`.

4. Remover comentários em prosa de "model hint" redundantes (a declaração formal os substitui).

5. Regenerar golden files.

**Critérios de Aceitação (DoD)**:

- [ ] As 5 invocações `Agent(...)` em x-story-plan têm `model:` explícito.
- [ ] Valores batem com matriz da STORY-0001.
- [ ] `grep -A1 "Agent(subagent_type" x-story-plan/SKILL.md | grep "model:"` retorna 5 linhas.
- [ ] Nenhum "model hint" em prosa remanescente.
- [ ] Golden files regenerados.
- [ ] Smoke test: `/x-story-plan` invocado em story de teste completa sem erro.

**Gherkin**:

```gherkin
Feature: x-story-plan subagents têm model explícito

  Scenario: Subagent Architect usa Opus
    Given x-story-plan/SKILL.md
    When a invocação do subagent Architect é lida
    Then contém 'model: "opus"'

  Scenario: Subagent QA usa Sonnet
    Given x-story-plan/SKILL.md
    When a invocação do subagent QA é lida
    Then contém 'model: "sonnet"'

  Scenario: Não há mais "model hint" em prosa
    Given x-story-plan/SKILL.md
    When o arquivo é grepado por "model hint"
    Then 0 matches são retornados
```

---

### STORY-0006: `Agent(...)` com `model:` explícito em x-arch-plan e x-test-plan

**Título**: Aplicar o padrão da STORY-0005 em x-arch-plan e x-test-plan

**Tipo**: Feature — Subagent Invocation Refactor

**Prioridade**: Média

**Dependências**: STORY-0005 (padrão estabelecido).

**Contexto técnico**:
Assim como `x-story-plan`, os skills `x-arch-plan` (linhas 153, 255) e `x-test-plan` (linhas
36, 57, 310) documentam em prosa "use `model: opus`" mas não passam o param na chamada
`Agent(...)`.

**Escopo de implementação**:

1. `x-arch-plan/SKILL.md` — refatorar ~2 subagent calls para `model: "opus"` (design
   arquitetural é raciocínio profundo).

2. `x-test-plan/SKILL.md` — refatorar ~3 subagent calls para `model: "opus"` (planning de
   testes envolve decisão de cobertura, TPP, Double-Loop TDD — justificam Opus).

3. Alinhar ao Pattern 2 da Rule 13.

4. Regenerar golden files.

**Critérios de Aceitação (DoD)**:

- [ ] Todas invocações `Agent(...)` em x-arch-plan e x-test-plan têm `model:`.
- [ ] Golden files regenerados.
- [ ] Testes passam.

**Gherkin**:

```gherkin
Feature: x-arch-plan e x-test-plan têm model explícito

  Scenario: x-arch-plan subagent usa Opus
    Given x-arch-plan/SKILL.md
    When uma invocação Agent(...) é lida
    Then contém 'model: "opus"'

  Scenario: x-test-plan subagent usa Opus
    Given x-test-plan/SKILL.md
    When uma invocação Agent(...) é lida
    Then contém 'model: "opus"'
```

---

### STORY-0007: `Skill(...)` com `model:` param em orquestradores

**Título**: Propagação de `model:` nas chamadas Skill() de x-epic-implement, x-story-implement e x-review

**Tipo**: Feature — Skill-to-Skill Invocation Refactor

**Prioridade**: Alta (captura ~10% da redução; cobre ~30 call-sites)

**Dependências**: STORY-0001.

**Contexto técnico**:
Os orquestradores invocam sub-skills via `Skill(skill: "...", args: "...")`. Sem `model:`
param, o sub-skill herda o modelo do orquestrador — e se a cadeia inteira começa em Opus,
cascateia. Exemplo: `x-epic-implement` (que após STORY-0002 será Sonnet) → `Skill("x-story-
implement")` herda Sonnet, o que é OK; mas `x-story-implement` → `Skill("x-git-commit")` deveria
forçar Haiku explicitamente.

**Escopo de implementação**:

1. Em `x-epic-implement/SKILL.md`, adicionar `model: "sonnet"` nas invocações:
   - `Skill(skill: "x-story-implement", ...)` — explícito mesmo herdando

2. Em `x-story-implement/SKILL.md`, adicionar:
   - `Skill(skill: "x-test-tdd", model: "sonnet", ...)`
   - `Skill(skill: "x-git-commit", model: "haiku", ...)`
   - `Skill(skill: "x-task-implement", model: "sonnet", ...)`

3. Em `x-review/SKILL.md`, adicionar `model: "sonnet"` nas ~10 invocações de sub-skills de review.

4. Em `x-task-implement/SKILL.md`, propagar `model:` para `x-git-commit` (haiku) e
   `x-test-tdd` (sonnet).

5. Regenerar golden files.

**Critérios de Aceitação (DoD)**:

- [ ] Todos os call-sites de `Skill()` nos 4 orquestradores têm `model:` explícito.
- [ ] `grep -c 'Skill(skill: "x-' x-{epic,story}-implement/SKILL.md | grep model` ≥ N-0.
- [ ] Golden files regenerados.
- [ ] Testes passam.
- [ ] Smoke test: `x-review` completa um review de teste sem regressão.

**Gherkin**:

```gherkin
Feature: Orquestradores propagam model em Skill() calls

  Scenario: x-story-implement invoca x-git-commit com Haiku
    Given x-story-implement/SKILL.md
    When uma chamada Skill(skill: "x-git-commit", ...) é lida
    Then contém 'model: "haiku"'

  Scenario: x-review invoca x-review-qa com Sonnet
    Given x-review/SKILL.md
    When uma chamada Skill(skill: "x-review-qa", ...) é lida
    Then contém 'model: "sonnet"'
```

---

### STORY-0008: Agent metadata determinístico (substituir Adaptive)

**Título**: Substituição de "Recommended Model: Adaptive" pelos modelos determinísticos nos 10 agents

**Tipo**: Feature — Agent Metadata

**Prioridade**: Média (complementa as demais; elimina ambiguidade dos agents)

**Dependências**: STORY-0001.

**Contexto técnico**:
Em `.claude/agents/*.md`, 8 dos 10 agents declaram `Recommended Model: Adaptive` — placeholder
que não resolve deterministicamente e na prática cai em Opus. O `product-owner` declara
`Recommended Model: Opus` mas a tarefa dele (validação de DoR, decomposição de spec) não requer
raciocínio premium — deve ser Sonnet.

**Escopo de implementação**:

1. Atualizar `java/src/main/resources/targets/claude/agents/*.md` conforme matriz:
   - `architect.md` → **Opus** (mantém — design profundo)
   - `product-owner.md` → **Sonnet** (mudança de Opus)
   - `qa-engineer.md` → **Sonnet**
   - `security-engineer.md` → **Sonnet**
   - `tech-lead.md` → **Sonnet**
   - `sre-engineer.md` → **Sonnet**
   - `performance-engineer.md` → **Sonnet**
   - `devops-engineer.md` → **Sonnet**
   - `devsecops-engineer.md` → **Sonnet**
   - `java-developer.md` → **Sonnet** (override para Haiku pode ser feito pelo caller em
     tarefas de boilerplate)

2. Remover todas ocorrências de "Adaptive" nos agents.

3. Atualizar seção de rationale curta (1 parágrafo) em cada agent explicando por que aquele tier.

4. Regenerar golden files.

**Critérios de Aceitação (DoD)**:

- [ ] `grep -l "Adaptive" .claude/agents/` retorna 0 arquivos.
- [ ] Cada agent tem `Recommended Model: <tier>` determinístico.
- [ ] Rationale atualizado em cada agent.
- [ ] Golden files regenerados.
- [ ] Testes passam.

**Gherkin**:

```gherkin
Feature: Agents têm Recommended Model determinístico

  Scenario: product-owner agent usa Sonnet
    Given agents/product-owner.md
    When o cabeçalho "Recommended Model" é lido
    Then o valor é "Sonnet"
    And não contém "Adaptive"

  Scenario: architect agent mantém Opus
    Given agents/architect.md
    When o cabeçalho "Recommended Model" é lido
    Then o valor é "Opus"

  Scenario: Nenhum agent tem Adaptive
    Given todos arquivos em agents/
    When grepados por "Adaptive"
    Then 0 matches
```

---

### STORY-0009: CI audit script para model selection

**Título**: Script bash de audit integrado ao CI que valida os 3 contratos técnicos da Rule

**Tipo**: Feature — CI Validation

**Prioridade**: Alta (previne regressões; seals the enforcement loop)

**Dependências**: STORY-0002, STORY-0003, STORY-0004, STORY-0005, STORY-0006, STORY-0007,
STORY-0008 (todos os fixes aplicados antes do CI ligar).

**Contexto técnico**:
Sem audit automatizado, qualquer PR futuro pode reintroduzir invocações sem `model:`. O
padrão canônico é o audit bash do EPIC-0033 (Rule 13) em `.claude/rules/13-skill-invocation-
protocol.md:230-255` — replicar a forma (grep → 0 matches == pass).

**Escopo de implementação**:

1. Criar `scripts/audit-model-selection.sh` com 3 checks:
   - **Check A**: skill de orquestração sem `model:` no frontmatter
     (lista de alvos extraída da matriz da Rule; grep para `^model:` em cada arquivo alvo).
   - **Check B**: `Agent(subagent_type: "general-purpose", ...)` sem `model:` em skills de
     orquestração (grep multiline).
   - **Check C**: `Skill(skill: "x-...", ...)` sem `model:` em skills listadas como
     orquestradores.
   - **Check D**: agent com `Recommended Model: Adaptive` (grep em `.claude/agents/`).

2. Cada check imprime `PASS` ou `FAIL: <count> violations` + lista de file:line.

3. Exit code: 0 se todos passam, 1 se qualquer um falha.

4. Integrar em `.github/workflows/ci.yml` como step pré-build.

5. Documentar uso do script em `scripts/README.md` (se existir) ou em `CLAUDE.md`.

**Critérios de Aceitação (DoD)**:

- [ ] Script existe e é executável (`chmod +x`).
- [ ] Executa em < 5s no repo atual.
- [ ] Todos 4 checks passam no estado pós-STORY-0007.
- [ ] Hook no CI workflow falha o PR se o script retorna exit 1.
- [ ] Documentação de uso existe.
- [ ] Teste do script: introduzir violação proposital em branch → CI falha; remover → CI passa.

**Gherkin**:

```gherkin
Feature: CI audit de model selection

  Scenario: Audit passa em estado limpo
    Given o repositório em estado pós-STORY-0007
    When scripts/audit-model-selection.sh é executado
    Then exit code é 0
    And stdout contém "Check A: PASS"
    And stdout contém "Check B: PASS"
    And stdout contém "Check C: PASS"
    And stdout contém "Check D: PASS"

  Scenario: Audit falha quando Agent() sem model é introduzido
    Given um skill de orquestração recebe Agent(subagent_type: "general-purpose", prompt: "...") sem model
    When o script é executado
    Then exit code é 1
    And stdout contém "Check B: FAIL"
    And lista o file:line da violação

  Scenario: Audit falha quando agent tem Adaptive
    Given agents/custom.md com "Recommended Model: Adaptive"
    When o script é executado
    Then exit code é 1
    And stdout contém "Check D: FAIL"

  Scenario: CI bloqueia PR com violação
    Given um PR introduzindo violação
    When o workflow CI é executado
    Then o step audit-model-selection falha
    And o PR não pode ser mergeado sem correção
```

---

### STORY-0010: Medição pós-deploy via telemetria (EPIC-0040)

**Título**: Dashboard e query de telemetria para validar redução de tokens em Opus pós-deploy

**Tipo**: Feature — Observability

**Prioridade**: Média (valida os targets do épico)

**Dependências**: STORY-0009 (base estável após CI enforcement); EPIC-0040 (telemetria já
mergeada).

**Contexto técnico**:
O EPIC-0040 instalou hooks de telemetria (`.claude/hooks/telemetry-*.sh`) que registram
invocações de skill com metadata. A STORY-0011 do EPIC-0040 criou `/x-telemetry-trend` para
comparação entre epics. Este épico fecha o loop medindo o impacto real em produção.

**Escopo de implementação**:

1. Adicionar na telemetria o campo `model` registrado (se ausente; a query atual pode já
   capturar pelo log de subagent dispatch).

2. Criar uma consulta/report `scripts/telemetry-model-mix.sh` (ou subcommando de
   `/x-telemetry-analyze`) que computa:
   - % tokens por modelo (opus / sonnet / haiku) nas últimas N execuções
   - Top 10 skills consumidoras de Opus
   - Trend comparativo: pré-EPIC-0050 vs pós-EPIC-0050

3. Validar em 2 epics de referência pós-merge que:
   - % Opus ≤ 50% (target da métrica)
   - % Sonnet ≥ 35%
   - % Haiku ≥ 12%

4. Se targets não forem atingidos, gerar lista de skills que continuam em Opus excessivamente
   e propor ajustes (feedback para Rule).

5. Documentar os resultados em `plans/epic-0050/reports/post-deploy-measurement.md`.

**Critérios de Aceitação (DoD)**:

- [ ] Script/subcommando executa e imprime mix de modelos.
- [ ] Report de 2 epics pós-merge salvo em `plans/epic-0050/reports/`.
- [ ] Targets atingidos OU análise clara do gap com próximos passos.
- [ ] Telemetria captura `model` em log se não capturava antes.
- [ ] Testes do script passam.

**Gherkin**:

```gherkin
Feature: Medição pós-deploy do mix de modelos

  Scenario: Script reporta mix de modelos
    Given telemetria disponível para 2 epics pós-merge
    When scripts/telemetry-model-mix.sh é executado
    Then stdout contém "% Opus: <valor>"
    And stdout contém "% Sonnet: <valor>"
    And stdout contém "% Haiku: <valor>"

  Scenario: Report pós-deploy é gerado
    Given execução do script em estado pós-EPIC-0050
    When o report é salvo
    Then existe plans/epic-0050/reports/post-deploy-measurement.md
    And contém tabela de targets vs real

  Scenario: Target atingido
    Given mix de modelos medido
    When comparado com os targets do épico
    Then % Opus ≤ 50%
    And % Sonnet ≥ 35%
    And % Haiku ≥ 12%
```

---

## Sumário de Fases

| Fase | Stories | Dependências | Objetivo |
|---|---|---|---|
| 0 — Foundation | STORY-0001 | — | Rule normativa publicada |
| 1 — Frontmatter | STORY-0002, -0003, -0004 | 0001 | `model:` em skills |
| 2 — Agent() param | STORY-0005, -0006 | 0001 | `model:` em subagents |
| 3 — Skill() param | STORY-0007 | 0001 | `model:` em Skill() calls |
| 4 — Agent metadata | STORY-0008 | 0001 | `Recommended Model` determinístico |
| 5 — CI enforcement | STORY-0009 | 0002-0008 | Audit script no CI |
| 6 — Medição | STORY-0010 | 0009 | Validação pós-deploy |

**Total**: 10 histórias, 7 fases (DAG permitindo execução paralela dentro de fases 1-4).

**Ordem de execução recomendada**:
1. Fase 0 (serializada — blocking)
2. Fases 1, 2, 3, 4 (paralelas após Fase 0)
3. Fase 5 (após fases 1-4 concluídas)
4. Fase 6 (após Fase 5, pós-merge)
