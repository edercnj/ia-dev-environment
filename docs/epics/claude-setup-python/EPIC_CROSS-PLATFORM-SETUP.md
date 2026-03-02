# Reescrita Cross-Platform do setup.sh em Python

**Autor:** Arquiteto de Software
**Data:** 2026-03-01
**Versão:** 1.0
**Status:** Em Refinamento

---

## 1. Visão Geral

**Chave Jira:** CLAUDE-SETUP-PY

O setup.sh (3.214 linhas Bash) será reescrito como uma ferramenta CLI Python cross-platform. O objetivo é eliminar a dependência de GNU coreutils, tornar a geração de boilerplate Claude Code portável para macOS, Linux e Windows (WSL/nativo), e manter compatibilidade byte-a-byte com o output atual. O escopo inclui: parsing de config YAML, resolução de stack, engine de templates Jinja2, assemblers modulares para cada artefato, pipeline CLI com Click, e suíte de verificação automatizada. Excluído: refatoração dos templates ou regras existentes — apenas a ferramenta de montagem é reescrita.

---

## 2. Anexos e Referências

- `src/setup.sh` — Script original (source of truth para comportamento)
- `src/config-templates/setup-config.*.yaml` — Schemas de configuração por stack
- `src/tests/test-*.sh` — Testes de contrato existentes (referência para verificação)
- `src/templates/_TEMPLATE-*.md` — Templates de documentação do projeto

---

## 3. Definições de Qualidade Globais

### Global Definition of Ready (DoR)

- Épico aprovado e regras transversais revisadas
- Config YAML de referência disponível (`setup-config.java-quarkus.yaml`)
- Ambiente Python 3.9+ com dependências instaláveis (`pyyaml`, `jinja2`, `click`)
- Output de referência gerado pelo bash para comparação byte-a-byte

### Global Definition of Done (DoD)

- **Cobertura:** ≥ 95% Line, ≥ 90% Branch
- **Testes Automatizados:** Unit (pytest), integration (comparação de output), contract (parametrizados por config profile)
- **Relatório de Cobertura:** `pytest-cov` com relatório HTML e XML
- **Documentação:** README.md da ferramenta com usage, `--help` funcional em todos os comandos
- **Persistência:** N/A (ferramenta de geração, sem estado persistente)
- **Performance:** Execução completa < 5s para qualquer perfil de configuração

---

## 4. Regras de Negócio Transversais (Source of Truth)

| ID | Título | Descrição |
| :--- | :--- | :--- |
| **[RULE-001]** | Sintaxe Jinja2 | Templates usam `{{ var }}` e `{% block %}` — compatível com Jinja2 nativamente. O engine deve processar todos os templates existentes sem modificação. Delimiters customizados não são permitidos. |
| **[RULE-002]** | Migração v2→v3 | Config suporta formato v2 (legacy com `type: java-quarkus`) com auto-migração para v3 (seções separadas `language`/`framework`). A migração deve ser transparente e logar warning ao usuário. Formato v2 será deprecated em release futura. |
| **[RULE-003]** | Output atômico | Toda geração ocorre em diretório temporário (`tempfile.mkdtemp`). Somente após sucesso completo o conteúdo é movido para o destino final. Em caso de falha, cleanup automático do temp dir. Nenhum arquivo parcial deve existir no destino. |
| **[RULE-004]** | Python 3.9+ | Dependências limitadas a: `pyyaml`, `jinja2`, `click`. Nenhuma dependência nativa (C extensions) permitida. Compatível com Python 3.9 até 3.13+. Type hints usando `from __future__ import annotations`. |
| **[RULE-005]** | Compatibilidade byte-a-byte | Output gerado pelo Python deve ser idêntico ao gerado pelo Bash para os 7 perfis de config existentes. Diferenças de whitespace, newlines, ou ordenação de seções são bugs. Verificação automatizada obrigatória. |
| **[RULE-006]** | Modo interativo | Quando executado sem arquivo de config (`--interactive`), a ferramenta deve coletar todas as informações via prompts Click (select, input, confirm). O resultado deve ser idêntico ao modo config-file. |
| **[RULE-007]** | Assemblers independentes | Cada assembler recebe `ProjectConfig` + `output_dir` como parâmetros, sem estado compartilhado entre assemblers. Assemblers podem ser executados individualmente para debug. A ordem de execução não deve afetar o resultado. |

---

## 5. Índice de Histórias

| ID | Título | Dependências (Blocked By) |
| :--- | :--- | :--- |
| [STORY-001](./STORY_STORY-001_Scaffolding-Models.md) | Scaffolding do Projeto e Modelos de Domínio | — |
| [STORY-002](./STORY_STORY-002_Config-Loading.md) | Carregamento e Validação de Configuração | STORY-001 |
| [STORY-003](./STORY_STORY-003_Stack-Resolution.md) | Resolução e Validação de Stack | STORY-001 |
| [STORY-004](./STORY_STORY-004_Template-Engine.md) | Engine de Templates Jinja2 | STORY-001 |
| [STORY-005](./STORY_STORY-005_Rules-Assembly.md) | Assembler de Regras (.claude/rules/) | STORY-001, STORY-004 |
| [STORY-006](./STORY_STORY-006_Patterns-Protocols.md) | Assemblers de Patterns e Protocols | STORY-001, STORY-004 |
| [STORY-007](./STORY_STORY-007_Skills-Agents.md) | Assemblers de Skills e Agents | STORY-001, STORY-004 |
| [STORY-008](./STORY_STORY-008_Hooks-Settings-Readme.md) | Assemblers de Hooks, Settings e README | STORY-001, STORY-004 |
| [STORY-009](./STORY_STORY-009_CLI-Pipeline.md) | CLI Pipeline e Orquestração | STORY-002, STORY-003, STORY-004, STORY-005, STORY-006, STORY-007, STORY-008 |
| [STORY-010](./STORY_STORY-010_Tests-Verification.md) | Testes e Verificação End-to-End | STORY-009 |
| [STORY-011](./STORY_STORY-011_Src-Layout-Migration.md) | Migração para src Layout (PyPA) | STORY-010 |
