# Investigation Report — EPIC-0048

> **Story:** story-0048-0001
> **Base commit:** d8f7ff0c2 (develop após bootstrap PR #545)
> **Data:** 2026-04-22
> **Estado:** Em consolidação — seções serão preenchidas em commits subsequentes dentro da mesma story

## 1. Inventário canônico de remoção

Consolidado em artefato irmão: [`removal-inventory.md`](./removal-inventory.md).

Resumo: 6 linguagens (python, go, kotlin, typescript, rust, csharp) tocam ~50+ linhas de código Java, 16 paths de template, 21 arquivos de knowledge-pack/rule, 8 dirs de golden (3256 arquivos), 8 YAMLs de setup-config. Detalhes + cross-referência por story-target em `removal-inventory.md`.

## 2. Ambiguity resolution — `.codex` / `.cursor`

### 2.1 Contexto

Durante o planejamento de EPIC-0048, dois agents de exploração divergiram sobre o status de `.codex/` e `.cursor/` no output do gerador:

- **Agent A** afirmou que `.codex/` e `.cursor/` são **dirs de saída** criados pelo `AssemblerPipeline` e permanecem como diretórios vazios no projeto gerado (consistente com Bug A — pastas vazias).
- **Agent B** afirmou que `.codex/` e `.cursor/` são **artefatos históricos** removidos por EPIC-0034 (Artifact Persistence) e não são mais gerados.

Esta ambiguidade foi resolvida empiricamente nesta investigação.

### 2.2 Evidência — enum `AssemblerTarget`

`java/src/main/java/dev/iadev/application/assembler/AssemblerTarget.java:22-28`:

```java
public enum AssemblerTarget {
    /** Output root directory. */
    ROOT(""),

    /** {@code .claude/} subdirectory. */
    CLAUDE(".claude");
}
```

O enum tem **exatamente 2 valores:** `ROOT` e `CLAUDE`. **Nenhum target `CODEX` ou `CURSOR` existe no código atual.**

### 2.3 Evidência — uso em `AssemblerFactory`

`grep -rn "AssemblerTarget\." java/src/main/java` retorna 22 hits, todos apontando apenas para `AssemblerTarget.ROOT` ou `AssemblerTarget.CLAUDE`:

```text
AssemblerFactory.java:84  → AssemblerTarget.ROOT
AssemblerFactory.java:103 → AssemblerTarget.CLAUDE
AssemblerFactory.java:201 → AssemblerTarget.CLAUDE
...
```

Nenhum assembler escreve em `.codex/` ou `.cursor/`. A CLI NÃO produz esses diretórios.

### 2.4 Evidência — occurrências em `java/src`

`grep -rn "\.codex\b" java/src` retorna **5 arquivos**, todos em `java/src/test`:

| Arquivo | Linha | Uso |
| :--- | :--- | :--- |
| `OverwriteDetectorTest.java:198` | `List.of(".claude/", ".codex/")` | **Input literal** para `OverwriteDetector` — testa formatação de mensagem quando o usuário passa esses paths como existentes. Não prova geração. |
| `OverwriteDetectorTest.java:201` | `assertThat(message).contains(".codex/")` | Asserção sobre a mensagem de erro formatada. |
| `OverwriteDetectorTest.java:209` | `List.of(".claude/", ".codex/", "steering/")` | Outro input literal. |
| `OverwriteDetectorTest.java:212` | `assertThat(message).contains("  - .codex/ (exists)")` | Asserção sobre formatação textual. |
| `ReadmeAssemblerPlatformTest.java:52` | `.doesNotContain("(.codex)")` | **Asserção negativa** — testa que o README gerado NÃO contém `.codex`. |
| `ReadmeAssemblerPlatformTest.java:97` | `.doesNotContain("\| .claude/ \| .codex/")` | Outra asserção negativa. |
| `SummaryTableBuilderPlatformTest.java:54` | `.doesNotContain("(.codex)")` | Asserção negativa. |
| `ReadmeTablesTest.java:417` | `.doesNotContain("Codex (.codex)")` | Asserção negativa. |
| `SummaryTableBuilderTest.java:88` | `.doesNotContain("Codex (.codex)")` | Asserção negativa. |

Resumo: `.codex/` aparece em testes ou como **input literal** em formatadores de mensagem, ou em **asserções negativas** (testes ativos garantindo que `.codex/` NÃO aparece no output). Nunca como alvo de geração ou path de output no código de produção.

`grep -rn "\.cursor\b" java/src` retorna **0 hits em código de produção** (também apenas testes similares).

### 2.5 Evidência — enum `Platform` e histórico EPIC-0034

Um grep adicional por `CODEX` e `CURSOR` em `java/src/main/java` retorna **0 hits** — nenhum enum `Platform.CODEX` ou `Platform.CURSOR` existe. Consistente com EPIC-0034 ("Artifact Persistence") que removeu suporte a Copilot/Codex/Cursor (referenciado em `plans/epic-0034/` e na seção "In progress" do CLAUDE.md).

### 2.6 Veredito categórico

**`.codex/` e `.cursor/` NÃO são criados pela CLI atual.** São artefatos históricos totalmente removidos pelo EPIC-0034. As ocorrências remanescentes em `java/src/test` são:

- Inputs literais em testes de formatadores de mensagem (não reproduzem o bug).
- Asserções negativas confirmando ausência (são gates de regressão).

**Classificação:** `.codex/` e `.cursor/` são **historical removed** — nem "fonte" nem "output" na CLI atual.

### 2.7 Impacto no Bug A

A spec do épico afirma no §10 (e a DoD global §DoD-Golden-Parity §f) que Bug A produz empty dirs `.github/`, `.codex/`, `.cursor/`. **Corrigindo:** Bug A pode ainda produzir `.github/` empty (confirmar via repro-bug-a.sh em §3), mas **não** `.codex/` e `.cursor/`. O PR body da story 0009 (Bug A fix) deve ajustar a descrição para refletir este inventário.

### 2.8 Ação

- Adicionar à narrativa da story 0009 que o fix estrutural em `CopyHelpers` cobre APENAS os dirs efetivamente vazios no output atual (a serem enumerados em `repro-bug-a.sh` §3.3 deste relatório).
- Remover qualquer referência a `.codex`/`.cursor` como "outputs vazios" na spec epicada — são distratores baseados em estado pré-EPIC-0034.

## 3. Bug A — empty directories (repro-bug-a.sh)

*Seção a ser preenchida em TASK-0048-0001-003.*

## 4. Bug B — CLAUDE.md ausente na raiz (repro-bug-b.sh)

*Seção a ser preenchida em TASK-0048-0001-004.*

## 5. Baseline metrics + Divergências

*Seção a ser preenchida em TASK-0048-0001-005.*
