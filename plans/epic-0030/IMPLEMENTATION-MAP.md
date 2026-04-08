# Mapa de Implementação — Context Window Resilience

**Gerado a partir das dependências BlockedBy/Blocks de cada história do epic-0030.**

---

## 1. Matriz de Dependências

| Story | Título | Chave Jira | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| story-0030-0001 | Context Budget Tracking | — | — | story-0030-0006 | Pendente |
| story-0030-0002 | Skill Instruction Compression via References | — | — | story-0030-0006 | Pendente |
| story-0030-0003 | Lazy Knowledge Pack Loading | — | — | — | Pendente |
| story-0030-0004 | Output Compaction | — | — | — | Pendente |
| story-0030-0005 | Subagent Context Isolation Enforcement | — | — | — | Pendente |
| story-0030-0006 | Progressive Skill Loading (Slim Mode) | — | story-0030-0002 | — | Pendente |

> **Nota:** Stories 0001-0005 são independentes entre si e podem ser executadas em paralelo. Story 0006 depende de 0002 porque o pattern de references deve estar estabelecido antes de implementar slim mode.

---

## 2. Fases de Implementação

```
╔══════════════════════════════════════════════════════════════════════════════════════════╗
║                        FASE 0 — Fundações (paralelo máximo)                            ║
║                                                                                        ║
║   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ ║
║   │ story-0030-  │  │ story-0030-  │  │ story-0030-  │  │ story-0030-  │  │ story-0030-  │ ║
║   │ 0001         │  │ 0002         │  │ 0003         │  │ 0004         │  │ 0005         │ ║
║   │ Budget Track │  │ Compression  │  │ Lazy KP      │  │ Output Comp  │  │ Isolation    │ ║
║   └──────┬───────┘  └──────┬───────┘  └──────────────┘  └──────────────┘  └──────────────┘ ║
║          │                 │                                                               ║
╚══════════╪═════════════════╪═══════════════════════════════════════════════════════════════╝
           │                 │
           │                 ▼
           │   ╔═══════════════════════════════╗
           │   ║   FASE 1 — Refinamento        ║
           │   ║                               ║
           └──►║   ┌──────────────────────┐    ║
               ║   │ story-0030-0006      │    ║
               ║   │ Slim Mode            │    ║
               ║   │ (← 0002)             │    ║
               ║   └──────────────────────┘    ║
               ╚═══════════════════════════════╝
```

---

## 3. Caminho Crítico

```
story-0030-0002 → story-0030-0006
   Fase 0            Fase 1
```

**2 fases no caminho crítico, 2 histórias na cadeia mais longa (story-0030-0002 → story-0030-0006).**

O caminho crítico é curto porque a maioria das stories são independentes. O gargalo principal é story-0030-0002 (Skill Compression), que estabelece o pattern de references necessário para story-0030-0006 (Slim Mode).

---

## 4. Grafo de Dependências (Mermaid)

```mermaid
graph TD
    S0001["story-0030-0001<br/>Context Budget Tracking"]
    S0002["story-0030-0002<br/>Skill Compression"]
    S0003["story-0030-0003<br/>Lazy KP Loading"]
    S0004["story-0030-0004<br/>Output Compaction"]
    S0005["story-0030-0005<br/>Isolation Enforcement"]
    S0006["story-0030-0006<br/>Slim Mode"]

    %% Fase 0 → 1
    S0002 --> S0006

    %% Estilos por fase
    classDef fase0 fill:#1a1a2e,stroke:#e94560,color:#fff
    classDef fase1 fill:#16213e,stroke:#0f3460,color:#fff

    class S0001 fase0
    class S0002 fase0
    class S0003 fase0
    class S0004 fase0
    class S0005 fase0
    class S0006 fase1
```

---

## 5. Resumo por Fase

| Fase | Histórias | Camada | Paralelismo | Pré-requisito |
| :--- | :--- | :--- | :--- | :--- |
| 0 | story-0030-0001, 0002, 0003, 0004, 0005 | Skill Templates + Assembler | 5 paralelas | — |
| 1 | story-0030-0006 | Skill Templates | 1 | Fase 0 concluída (especificamente 0002) |

**Total: 6 histórias em 2 fases.**

> **Nota:** A Fase 0 tem paralelismo máximo (5 stories independentes). A Fase 1 é um refinamento que depende apenas de uma story da Fase 0.

---

## 6. Detalhamento por Fase

### Fase 0 — Fundações de Resiliência de Contexto

| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| story-0030-0001 | Campo `context-budget` no frontmatter + lógica de delegação | SkillAssembler.java, x-dev-lifecycle/SKILL.md, x-dev-epic-implement/SKILL.md |
| story-0030-0002 | Separação de skills pesados em core + references | x-dev-epic-implement/ (core + 6 refs), x-dev-lifecycle/ (core + 3 refs) |
| story-0030-0003 | Prompts de subagent referenciam KP files específicos | x-dev-lifecycle/SKILL.md (Phase 1B-1F prompts) |
| story-0030-0004 | Checkpoint reads seletivos + TDD logs compactos | x-dev-epic-implement/SKILL.md, x-tdd/SKILL.md |
| story-0030-0005 | Auditoria e enforcement de isolamento de contexto | x-dev-epic-implement, x-dev-lifecycle, x-review, x-epic-plan |

**Entregas da Fase 0:**

- Campo `context-budget` em todos os skills core
- x-dev-epic-implement reduzido de 1,733 para ~400 linhas core
- x-dev-lifecycle reduzido de 733 para ~300 linhas core
- Subagents carregam ~75% menos KP tokens
- Outputs intermediários referenciados por path, não inline
- 100% dos prompts de subagent auditados e com instrução de isolation

### Fase 1 — Refinamento

| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| story-0030-0006 | Seção "Slim Mode" em x-commit, x-format, x-lint, x-tdd | 4 SKILL.md com seções slim ≤ 50 linhas |

**Entregas da Fase 1:**

- Cadeia x-tdd→x-commit→x-format→x-lint consome ~760 linhas menos por invocação

---

## 7. Observações Estratégicas

### Gargalo Principal

**story-0030-0002 (Skill Instruction Compression)** é o gargalo porque:
1. É a story com maior volume de trabalho (separar 2 skills grandes em core + references)
2. Bloqueia story-0030-0006 (Slim Mode depende do pattern de references)
3. Tem maior impacto individual (~70% redução nos 2 maiores skills)

Investir mais tempo e revisão nesta story compensa porque ela define o pattern que todas as outras stories e futuros skills seguirão.

### Histórias Folha (sem dependentes)

- **story-0030-0003** (Lazy KP Loading) — sem dependentes, pode absorver atrasos
- **story-0030-0004** (Output Compaction) — sem dependentes
- **story-0030-0005** (Subagent Isolation) — sem dependentes
- **story-0030-0006** (Slim Mode) — leaf da Fase 1

Todas as 4 podem ser implementadas sem pressão de timeline, já que não bloqueiam nada.

### Otimização de Tempo

- **Paralelismo máximo na Fase 0**: 5 stories podem ser executadas simultaneamente por 5 desenvolvedores/worktrees
- **Stories imediatas**: Todas as 5 da Fase 0 podem começar imediatamente
- **Ponto de aceleração**: Completar story-0030-0002 primeiro desbloqueia a Fase 1

### Dependências Cruzadas

Não há dependências cruzadas complexas neste epic. O grafo é um DAG simples com uma única aresta (0002 → 0006). Não há pontos de convergência.

### Marco de Validação Arquitetural

**story-0030-0002** serve como checkpoint de validação. Ela valida:
- O pattern de separação core + references funciona com o assembler
- Golden files são gerados corretamente com reference files
- O skill mantém funcionalidade completa após separação

Se esta story falhar, as outras stories não são diretamente afetadas (exceto 0006), mas o approach de compression precisa ser revisitado.

---

## 8. Dependências entre Tasks (Cross-Story)

> Tasks neste epic são independentes entre stories. Não há dependências cross-story entre tasks.

### 8.1 Dependências Cross-Story entre Tasks

| Task | Depends On | Story Source | Story Target | Tipo |
| :--- | :--- | :--- | :--- | :--- |
| — | — | — | — | — |

> **Validação RULE-012:** Sem dependências cross-story. Cada story pode ser implementada e merged independentemente (exceto 0006 que depende de 0002 no nível de story).

### 8.2 Ordem de Merge (Topological Sort)

| Ordem | Task ID | Story | Parallelizável Com | Fase |
| :--- | :--- | :--- | :--- | :--- |
| 1 | TASK-0030-0001-* | story-0030-0001 | TASK-0030-0002-*, 0003-*, 0004-*, 0005-* | 0 |
| 1 | TASK-0030-0002-* | story-0030-0002 | TASK-0030-0001-*, 0003-*, 0004-*, 0005-* | 0 |
| 1 | TASK-0030-0003-* | story-0030-0003 | TASK-0030-0001-*, 0002-*, 0004-*, 0005-* | 0 |
| 1 | TASK-0030-0004-* | story-0030-0004 | TASK-0030-0001-*, 0002-*, 0003-*, 0005-* | 0 |
| 1 | TASK-0030-0005-* | story-0030-0005 | TASK-0030-0001-*, 0002-*, 0003-*, 0004-* | 0 |
| 2 | TASK-0030-0006-* | story-0030-0006 | — | 1 |

**Total: ~19 tasks em 2 fases de execução.**
