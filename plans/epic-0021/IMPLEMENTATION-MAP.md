# Mapa de Implementação — PR por Story no Orquestrador de Épicos

**Gerado a partir das dependências BlockedBy/Blocks de cada história do epic-0021.**

---

## 1. Matriz de Dependências

| Story | Título | Chave Jira | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| story-0021-0001 | Eliminar branch épica e adotar branching por story | — | — | story-0021-0003, story-0021-0005, story-0021-0006, story-0021-0009 | Concluída |
| story-0021-0002 | Delegar criação de PR e review ao x-dev-lifecycle | — | — | story-0021-0003, story-0021-0004 | Concluída |
| story-0021-0003 | Enforcement de dependências via PR merge status | — | story-0021-0001, story-0021-0002 | story-0021-0006, story-0021-0007, story-0021-0009 | Concluída |
| story-0021-0004 | Substituir Phase 2 consolidada por tracking incremental | — | story-0021-0002 | story-0021-0008 | Concluída |
| story-0021-0005 | Pre-flight analysis com strict mode | — | story-0021-0001 | story-0021-0008 | Concluída |
| story-0021-0006 | Integrity e consistency gates na main | — | story-0021-0001, story-0021-0003 | story-0021-0008 | Concluída |
| story-0021-0007 | Resume workflow para modelo per-story PR | — | story-0021-0003 | story-0021-0008 | Concluída |
| story-0021-0009 | Auto-rebase e resolução automática de conflitos em PRs paralelos | — | story-0021-0001, story-0021-0003 | story-0021-0008 | Concluída |
| story-0021-0008 | Verificação final e documentação de integração | — | story-0021-0004, story-0021-0005, story-0021-0006, story-0021-0007, story-0021-0009 | — | Concluída |

> **Nota:** story-0021-0004 depende apenas de story-0021-0002 (não de story-0021-0001), pois a substituição da Phase 2 requer apenas o SubagentResult com campos de PR, não a eliminação da branch épica. A Phase 2 atualizada coexiste com o modelo per-story sem conflito. story-0021-0009 depende de story-0021-0001 (branching model) e story-0021-0003 (merge mechanism) pois reimplementa o subagent de resolução e auto-rebase sobre a infraestrutura definida por essas stories.

---

## 2. Fases de Implementação

> As histórias são agrupadas em fases. Dentro de cada fase, as histórias podem ser implementadas **em paralelo**. Uma fase só pode iniciar quando todas as dependências das fases anteriores estiverem concluídas.

```
╔══════════════════════════════════════════════════════════════════════════╗
║                   FASE 0 — Fundação (paralelo)                         ║
║                                                                        ║
║   ┌─────────────────────────┐   ┌───────────────���─────────┐           ║
║   │  story-0021-0001        │   │  story-0021-0002        │           ║
║   │  Eliminar branch épica  │   │  Delegar PR ao lifecycle │           ║
║   └────────────┬────────────┘   └────────────┬────────────┘           ║
╚════════════════╪════════════════════════════╪══════════════════════════╝
                 │                            │
                 ▼                            ▼
╔══════════════════════════════════════════════════════════════════════════╗
║                   FASE 1 — Core (paralelo)                             ║
║                                                                        ║
║   ┌─────────────────────────┐   ┌─────────────────────────┐           ║
║   │  story-0021-0003        │   │  story-0021-0004        │           ║
║   │  Dependency enforcement │   │  Phase 2 → tracking     │           ║
║   │  (← 0001, 0002)        │   │  (← 0002)               │           ║
║   └────────────┬────────────┘   └────────────┬────────────┘           ║
╚════════════════╪════════════════════════════╪══════════════════════════╝
                 │                            │
                 ▼                            ▼
╔══════════════════════════════════════════════════════════════════════════════════════╗
║                         FASE 2 — Extensões (paralelo)                              ║
║                                                                                    ║
║  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐                   ║
║  │ story-0021-0005  │ │ story-0021-0006  │ │ story-0021-0007  │                   ║
║  │ Pre-flight strict│ │ Gates na main    │ │ Resume workflow  │                   ║
║  │ (← 0001)        │ │ (← 0001, 0003)   │ │ (← 0003)        │                   ║
║  └───────┬──────────┘ └───────┬──────────┘ └───────┬──────────┘                   ║
║          │                    │                     │                              ║
║  ┌──────────────────┐        │                     │                              ║
║  │ story-0021-0009  │        │                     │                              ║
║  │ Auto-rebase +    │        │                     │                              ║
║  │ conflict resolv. │        │                     │                              ║
║  │ (← 0001, 0003)  │        │                     │                              ║
║  └───────┬──────────┘        │                     │                              ║
╚══════════╪═══════════════════╪═════════════════════╪══════════════════════════════╝
           │                   │                     │
           ▼                   ▼                     ▼
╔══════════════════════════════════════════════════════════════════════════════════════╗
║                         FASE 3 — Finalização                                       ║
║                                                                                    ║
║   ┌──────────────────────────────────────────────────────────────────┐             ║
║   │  story-0021-0008                                                 │             ║
║   │  Verificação final e documentação de integração                  │             ║
║   │  (← 0004, 0005, 0006, 0007, 0009)                               │             ║
║   └──────────────────────────────────────────────────────────────────┘             ║
╚══════════════════════════════════════════════════════════════════════════════════════╝
```

---

## 3. Caminho Crítico

> O caminho crítico (a sequência mais longa de dependências) determina o tempo mínimo de implementação do projeto.

```
story-0021-0001 ─┐
                  ├──→ story-0021-0003 ──→ story-0021-0006 ──┐
story-0021-0002 ─┘                    └──→ story-0021-0009 ──┤
                                          story-0021-0007 ───┼──→ story-0021-0008
                                          story-0021-0005 ───┤
                                          story-0021-0004 ───┘
   Fase 0              Fase 1              Fase 2               Fase 3
```

**4 fases no caminho crítico, 4 histórias na cadeia mais longa (story-0021-0001 → story-0021-0003 → story-0021-0006 → story-0021-0008 ou story-0021-0001 → story-0021-0003 → story-0021-0009 → story-0021-0008).**

Qualquer atraso em story-0021-0001 (eliminação da branch épica) ou story-0021-0003 (dependency enforcement) impacta diretamente a entrega final. Essas são as stories de maior risco e devem receber atenção prioritária no design e revisão. A story-0021-0009 (auto-rebase) está no caminho crítico por ser essencial para a segurança do modelo per-story PR.

---

## 4. Grafo de Dependências (Mermaid)

```mermaid
graph TD
    S0021_0001["story-0021-0001<br/>Eliminar branch épica"]
    S0021_0002["story-0021-0002<br/>Delegar PR ao lifecycle"]
    S0021_0003["story-0021-0003<br/>Dependency enforcement"]
    S0021_0004["story-0021-0004<br/>Phase 2 → tracking"]
    S0021_0005["story-0021-0005<br/>Pre-flight strict mode"]
    S0021_0006["story-0021-0006<br/>Gates na main"]
    S0021_0007["story-0021-0007<br/>Resume workflow"]
    S0021_0009["story-0021-0009<br/>Auto-rebase + conflict resolution"]
    S0021_0008["story-0021-0008<br/>Verificação final"]

    %% Fase 0 → 1
    S0021_0001 --> S0021_0003
    S0021_0002 --> S0021_0003
    S0021_0002 --> S0021_0004

    %% Fase 0 → 2
    S0021_0001 --> S0021_0005
    S0021_0001 --> S0021_0009

    %% Fase 1 → 2
    S0021_0001 --> S0021_0006
    S0021_0003 --> S0021_0006
    S0021_0003 --> S0021_0007
    S0021_0003 --> S0021_0009

    %% Fase 2 → 3
    S0021_0004 --> S0021_0008
    S0021_0005 --> S0021_0008
    S0021_0006 --> S0021_0008
    S0021_0007 --> S0021_0008
    S0021_0009 --> S0021_0008

    %% Estilos por fase
    classDef fase0 fill:#1a1a2e,stroke:#e94560,color:#fff
    classDef fase1 fill:#16213e,stroke:#0f3460,color:#fff
    classDef fase2 fill:#533483,stroke:#e94560,color:#fff
    classDef fase3 fill:#e94560,stroke:#fff,color:#fff

    class S0021_0001,S0021_0002 fase0
    class S0021_0003,S0021_0004 fase1
    class S0021_0005,S0021_0006,S0021_0007,S0021_0009 fase2
    class S0021_0008 fase3
```

---

## 5. Resumo por Fase

| Fase | Histórias | Camada | Paralelismo | Pré-requisito |
| :--- | :--- | :--- | :--- | :--- |
| 0 | story-0021-0001, story-0021-0002 | Fundação | 2 paralelas | — |
| 1 | story-0021-0003, story-0021-0004 | Core | 2 paralelas | Fase 0 concluída |
| 2 | story-0021-0005, story-0021-0006, story-0021-0007, story-0021-0009 | Extensões | 4 paralelas | Fase 1 concluída (parcial: cada story tem deps específicas) |
| 3 | story-0021-0008 | Finalização | 1 | Fase 2 concluída |

**Total: 9 histórias em 4 fases.**

> **Nota:** Na Fase 2, story-0021-0005 depende apenas de story-0021-0001 (Fase 0) e pode iniciar assim que a Fase 0 concluir. Stories 0021-0006, 0021-0007 e 0021-0009 dependem de story-0021-0003 (Fase 1), portanto a Fase 2 completa só inicia após a Fase 1. Story-0021-0005 é a mais flexível para antecipação.

---

## 6. Detalhamento por Fase

### Fase 0 — Fundação

| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| story-0021-0001 | Eliminar branch épica, remover rebase-before-merge, substituir conflict resolution por placeholder, adicionar --single-pr | Sections 1.2, 1.4a, 1.4b modificadas/removidas; Section 1.4c com placeholder para story-0009; flag --single-pr |
| story-0021-0002 | Delegar PR ao lifecycle, estender SubagentResult com prUrl/prNumber | Sections 1.4, 1.4a, 1.5 modificadas; SubagentResult schema atualizado |

**Entregas da Fase 0:**

- Branch épica eliminada do fluxo padrão (preservada sob --single-pr)
- Section 1.4b removida; Section 1.4c substituída por placeholder (reimplementada na Fase 2 por story-0009)
- Status REBASING, REBASE_SUCCESS, REBASE_FAILED removidos do status principal de story
- SubagentResult estendido com prUrl e prNumber
- Prompt template instrui lifecycle a criar PR targeting main com referência ao épico

### Fase 1 — Core

| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| story-0021-0003 | Dependency enforcement via PR merge status, flag --auto-merge, polling/wait | Section 1.3 (getExecutableStories), schema execution-state.json, flag --auto-merge |
| story-0021-0004 | Substituir Phase 2 por tracking incremental, atualizar template de report | Phase 2 reescrita, template com {{PR_LINKS_TABLE}} |

**Entregas da Fase 1:**

- `getExecutableStories()` verifica `prMergeStatus == "MERGED"` para todas as dependências
- Mecanismo de polling/wait para PRs pendentes de merge
- Flag --auto-merge com merge via `gh pr merge`
- Phase 2 gera relatório de progresso com tabela de PRs (não mega-PR)
- Template de execution report atualizado com `{{PR_LINKS_TABLE}}`

### Fase 2 — Extensões

| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| story-0021-0005 | Pre-flight analysis com strict mode (advisory default, --strict-overlap opt-in) | Sections 0.5.4, 0.5.5 modificadas; flag --strict-overlap |
| story-0021-0006 | Integrity/consistency gates rodando na main após PRs merged | Sections 1.7, 1.8 modificadas; mainShaBeforePhase no checkpoint |
| story-0021-0007 | Resume workflow com novos status de PR (PR_CREATED, PR_PENDING_REVIEW, PR_MERGED) | Resume Steps 1-4 atualizados; failure handling fecha PR |
| story-0021-0009 | Auto-rebase e resolução automática de conflitos em PRs paralelos | Section 1.4c reimplementada; Section 1.4e nova; campos rebaseStatus, lastRebaseSha, rebaseAttempts |

**Entregas da Fase 2:**

- Pre-flight analysis com dual-mode: advisory (default) e strict (--strict-overlap)
- Integrity gates rodam na `main` com diff pré/pós-fase
- Resume workflow lida corretamente com estados de PR
- Failure handling fecha PR de stories que falharam
- Auto-rebase automático de PRs remanescentes após cada merge na fase
- Subagent de resolução de conflitos adaptado para per-story PR com contexto completo
- Schema completo com todos os novos status e campos (incluindo rebaseStatus, lastRebaseSha, rebaseAttempts)

### Fase 3 — Finalização

| Story | Escopo Principal | Artefatos Chave |
| :--- | :--- | :--- |
| story-0021-0008 | Verificação final, consistência interna, --dry-run, argument-hint | Phase 3, --dry-run, frontmatter atualizados |

**Entregas da Fase 3:**

- Phase 3 (Verification) completamente alinhada com modelo per-story PR
- --dry-run mostra plano per-story PR
- Zero referências órfãs fora do guard --single-pr
- Argument-hint atualizado com todas as novas flags
- SKILL.md auto-consistente e pronto para uso

---

## 7. Observações Estratégicas

### Gargalo Principal

**story-0021-0001 (Eliminar branch épica)** é o maior gargalo — bloqueia 4 stories diretamente (0003, 0005, 0006, 0009) e indiretamente toda a cadeia até story-0021-0008. Investir tempo extra no design da flag `--single-pr` e na substituição correta da Section 1.4c por placeholder previne retrabalho nas 7 stories downstream que dependem da eliminação da branch épica.

**story-0021-0003 (Dependency enforcement)** é o segundo gargalo — bloqueia 3 stories (0006, 0007, 0009). O design do mecanismo de polling/wait e da flag --auto-merge é crítico para a usabilidade do orquestrador.

### Histórias Folha (sem dependentes)

**story-0021-0008** é a única história folha. Como story de finalização e verificação, ela pode absorver atrasos nas Fases 0-2 sem impacto externo, mas é bloqueada por 5 stories — qualquer atraso em qualquer uma das stories de Fase 1 ou 2 impacta diretamente o fechamento do épico.

### Otimização de Tempo

- **Fase 0**: Máximo paralelismo com 2 stories independentes. Podem começar imediatamente.
- **Fase 1**: 2 stories paralelas. story-0021-0004 depende apenas de story-0021-0002 — pode iniciar assim que story-0021-0002 concluir, mesmo que story-0021-0001 ainda esteja em andamento.
- **Fase 2**: 4 stories paralelas. story-0021-0005 pode ser antecipada (depende apenas de Fase 0). Stories 0021-0006, 0021-0007 e 0021-0009 precisam de Fase 1.
- **Alocação ideal**: 2-3 engenheiros em paralelo nas Fases 0-2, convergindo para 1 na Fase 3.

### Dependências Cruzadas

story-0021-0008 é o ponto de convergência principal — depende de 5 stories de 2 fases diferentes (Fase 1: 0004; Fase 2: 0005, 0006, 0007, 0009). Isso significa que a Fase 3 só inicia quando TODA a Fase 2 (e toda a Fase 1 por transitividade) está completa.

story-0021-0006 (gates na main) e story-0021-0009 (auto-rebase) dependem de 2 ramos do DAG: story-0021-0001 (Fase 0, ramo de branch elimination) e story-0021-0003 (Fase 1, ramo de dependency enforcement). Ambos os ramos devem convergir antes que essas stories possam ser implementadas.

story-0021-0009 (auto-rebase + conflict resolution) complementa story-0021-0006 (integrity gates): enquanto 0009 previne e resolve conflitos proativamente (antes do merge), 0006 valida a integridade pós-merge. Juntas, formam a camada de segurança do modelo per-story PR.

### Marco de Validação Arquitetural

**story-0021-0003 (Dependency enforcement via PR merge status)** é o marco de validação. Ela estabelece o padrão central de todo o novo modelo: verificar PRs merged como condição de execução. Se esse padrão funcionar corretamente — stories esperando PRs merged, auto-merge funcional, polling robusto — todas as stories de Fase 2 e 3 são extensões seguras desse padrão. Se falhar, o modelo per-story PR não é viável.

### Camada de Segurança do Modelo Per-Story PR

O epic-0021 mantém todas as camadas de segurança do modelo antigo, adaptadas para per-story PR:

| Camada | Modelo Antigo | Modelo Novo (Per-Story PR) | Story |
| :--- | :--- | :--- | :--- |
| Prevenção | Pre-flight blocking | Pre-flight advisory + `--strict-overlap` opt-in | story-0021-0005 |
| Detecção | Rebase-before-merge | Auto-rebase após PR merge | story-0021-0009 |
| Resolução | Conflict Resolution Subagent | Subagent adaptado (Section 1.4c) | story-0021-0009 |
| Validação | Integrity gates (epic branch) | Integrity gates (main, pós-merge) | story-0021-0006 |
| Recuperação | Checkpoint (REBASING states) | Checkpoint (rebaseStatus sub-field) | story-0021-0007, story-0021-0009 |
