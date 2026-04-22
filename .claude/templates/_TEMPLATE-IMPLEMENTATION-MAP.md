# Mapa de Implementação — <Nome do Projeto/Épico>

**Gerado a partir das dependências BlockedBy/Blocks de cada história do <epic-XXXX>.**

---

## 1. Matriz de Dependências

| Story | Título | Chave Jira | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| <story-XXXX-YYYY> | <Título> | <CHAVE-JIRA> | — | <story-XXXX-YYYY> | <Status> |

> **Valores de Status:** `Pendente` (padrão) · `Em Andamento` · `Concluída` · `Falha` · `Bloqueada` · `Parcial`

> **Nota:** <Observações sobre dependências implícitas ou funcionais que não estão declaradas explicitamente nas histórias mas existem na prática.>

---

## 2. Fases de Implementação

> As histórias são agrupadas em fases. Dentro de cada fase, as histórias podem ser implementadas **em paralelo**. Uma fase só pode iniciar quando todas as dependências das fases anteriores estiverem concluídas.

```
╔══════════════════════════════════════════════════════════════════════════╗
║                   FASE 0 — <Nome da Fase> (paralelo)                   ║
║                                                                        ║
║   ┌─────────────┐                   ┌─────────────┐                    ║
║   │  story-XXXX-YYYY  │  <Escopo curto>   │  story-XXXX-YYYY  │  <Escopo curto>    ║
║   └──────┬──────┘                   └──────┬──────┘                    ║
╚══════════╪═════════════════════════════════╪════════════════════════════╝
           │                                 │
           ▼                                 ▼
╔══════════════════════════════════════════════════════════════════════════╗
║                   FASE 1 — <Nome da Fase>                              ║
║                                                                        ║
║   ┌──────────────────────────────────────────────────────────┐         ║
║   │  story-XXXX-YYYY  <Escopo>                                     │         ║
║   │  (← dependências)                                        │         ║
║   └──────────────────────────┬───────────────────────────────┘         ║
╚══════════════════════════════╪═════════════════════════════════════════╝
                               │
                               ▼
╔══════════════════════════════════════════════════════════════════════════╗
║                   FASE N — <Nome da Fase> (paralelo)                   ║
║                                                                        ║
║   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                   ║
║   │  story-XXXX-YYYY  │  │  story-XXXX-YYYY  │  │  story-XXXX-YYYY  │                   ║
║   │  <Escopo>   │  │  <Escopo>   │  │  <Escopo>   │                   ║
║   └─────────────┘  └─────────────┘  └─────────────┘                   ║
╚══════════════════════════════════════════════════════════════════════════╝
```

---

## 3. Caminho Crítico

> O caminho crítico (a sequência mais longa de dependências) determina o tempo mínimo de implementação do projeto.

```
story-XXXX-YYYY ─┐
            ├──→ story-XXXX-YYYY → story-XXXX-YYYY ──┐
story-XXXX-YYYY ─┘                              ├──→ story-XXXX-YYYY
                 story-XXXX-YYYY → story-XXXX-YYYY ──┘
   Fase 0           Fase 1       Fase 2            Fase 3
```

**<N> fases no caminho crítico, <N> histórias na cadeia mais longa (<sequência>).**

<Impacto de atrasos no caminho crítico.>

---

## 4. Grafo de Dependências (Mermaid)

```mermaid
graph TD
    S0001["story-XXXX-YYYY<br/><Título curto>"]
    S0002["story-XXXX-YYYY<br/><Título curto>"]

    %% Fase 0 → 1
    S0001 --> S0002

    %% Estilos por fase
    classDef fase0 fill:#1a1a2e,stroke:#e94560,color:#fff
    classDef fase1 fill:#16213e,stroke:#0f3460,color:#fff
    classDef fase2 fill:#533483,stroke:#e94560,color:#fff
    classDef fase3 fill:#e94560,stroke:#fff,color:#fff
    classDef faseQE fill:#0d7377,stroke:#14ffec,color:#fff
    classDef faseTD fill:#2d3436,stroke:#fdcb6e,color:#fff
    classDef faseCR fill:#6c5ce7,stroke:#a29bfe,color:#fff

    class S0001 fase0
    class S0002 fase1
```

---

## 5. Resumo por Fase

| Fase | Histórias | Camada | Paralelismo | Pré-requisito |
| :--- | :--- | :--- | :--- | :--- |
| 0 | <story-XXXX-YYYY, story-XXXX-YYYY> | <Camada arquitetural> | <N paralelas> | — |
| 1 | <story-XXXX-YYYY> | <Camada> | <N> | Fase 0 concluída |
| N | <story-XXXX-YYYY, NNN, NNN> | <Camada> | <N paralelas> | <Pré-requisito> |

**Total: <N> histórias em <N> fases.**

> **Nota:** <Observações sobre fases transversais, ordem de execução flexível, etc.>

---

## 6. Detalhamento por Fase

### Fase 0 — <Nome>

| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| <story-XXXX-YYYY> | <Descrição do escopo> | <Classes/componentes/migrations gerados> |

**Entregas da Fase 0:**

- <Entrega concreta 1>
- <Entrega concreta N>

### Fase N — <Nome>

| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| <story-XXXX-YYYY> | <Descrição> | <Artefatos> |

**Entregas da Fase N:**

- <Entrega>

---

## 7. Observações Estratégicas

### Gargalo Principal

<Identificar a história que é o maior gargalo — a que bloqueia mais histórias downstream. Explicar por que investir mais tempo nela compensa.>

### Histórias Folha (sem dependentes)

<Listar histórias que não bloqueiam nenhuma outra. São candidatas a paralelismo e podem absorver atrasos sem impacto no caminho crítico.>

### Otimização de Tempo

- <Onde o paralelismo é máximo>
- <Quais histórias podem começar imediatamente>
- <Como alocar equipes para acelerar>

### Dependências Cruzadas

<Histórias que dependem de ramos diferentes da árvore de dependências. Identificar pontos de convergência.>

### Marco de Validação Arquitetural

<Qual história deve servir como checkpoint de validação antes de expandir o escopo. O que ela valida (patterns, pipeline, integração).>

---

## 8. Dependências entre Tasks (Cross-Story)

> Esta seção é gerada automaticamente quando as histórias contêm tasks formais com IDs `TASK-XXXX-YYYY-NNN`. Se as histórias não possuem task IDs formais, esta seção é omitida.

### 8.1 Dependências Cross-Story entre Tasks

| Task | Depends On | Story Source | Story Target | Tipo |
| :--- | :--- | :--- | :--- | :--- |
| <TASK-XXXX-YYYY-NNN> | <TASK-XXXX-ZZZZ-MMM> | <story-XXXX-YYYY> | <story-XXXX-ZZZZ> | <data/interface/schema/config> |

> **Validação RULE-012:** <Resultado da validação de consistência entre dependências de tasks e dependências de histórias. Erros e warnings listados aqui.>

### 8.2 Ordem de Merge (Topological Sort)

| Ordem | Task ID | Story | Parallelizável Com | Fase |
| :--- | :--- | :--- | :--- | :--- |
| 1 | <TASK-XXXX-YYYY-NNN> | <story-XXXX-YYYY> | <TASK IDs paralelos> | 0 |

**Total: <N> tasks em <N> fases de execução.**

### 8.3 Grafo de Dependências entre Tasks (Mermaid)

```mermaid
graph LR
    subgraph story-XXXX-0001["Story 0001 (<Título>)"]
        style story-XXXX-0001 fill:#e8f4fd
        T001["TASK-XXXX-0001-001<br/><Descrição curta>"]
        T002["TASK-XXXX-0001-002<br/><Descrição curta>"]
        T001 --> T002
    end

    subgraph story-XXXX-0002["Story 0002 (<Título>)"]
        style story-XXXX-0002 fill:#fde8e8
        T003["TASK-XXXX-0002-001<br/><Descrição curta>"]
        T004["TASK-XXXX-0002-002<br/><Descrição curta>"]
        T003 --> T004
    end

    T002 -.->|cross-story| T003
```

---

## 8.5 Restrições de Paralelismo

<!--
  Seção gerada pelo Step 8.5 de x-epic-map (EPIC-0041 / story-0041-0005).
  A análise invoca /x-parallel-eval --scope=epic e popula a matriz de colisão.

  Fail-open (RULE-005 / RULE-006): se /x-parallel-eval não estiver disponível
  ou retornar exit code >= 1, o Step 8.5 substitui o corpo desta seção por:

      > análise pulada — /x-parallel-eval não disponível (RULE-006 fail-open)

  e o map continua sendo salvo normalmente — o Step 8.5 NUNCA bloqueia
  a geração do map.

  Determinismo (RULE-008): o conteúdo é byte-identical entre re-execuções
  EXCETO pelo comentário de timestamp na primeira linha. Ordenar pares por
  Fase ASC -> Story-A lex -> Story-B lex.
-->

> Análise gerada por /x-parallel-eval em <timestamp omitido para determinismo>.

**Conflitos detectados:** <H> hard, <R> regen, <S> soft

### 8.5.1 Pares Serializados Dentro da Fase

| Fase | A | B | Categoria | Motivo |
| :--- | :--- | :--- | :--- | :--- |
| <Phase N> | <story-XXXX-YYYY> | <story-XXXX-ZZZZ> | <hard|regen|soft> | <arquivo compartilhado ou colisão de regen> |

### 8.5.2 Recomendação de Reagrupamento

<Para cada fase com conflitos, descrever a nova ordem após a serialização forçada. Se "Conflitos detectados: 0", omitir subsections 8.5.1 e 8.5.2.>
