# Épico: Suporte ao OpenAI Codex no ia-dev-environment

**Autor:** Arquiteto de Software / Claude
**Data:** 2026-03-10
**Versão:** 1.0.0
**Status:** Em Refinamento

## 1. Visão Geral

**Chave Jira:** EPIC-002

Estender o **ia-dev-environment** para gerar, além dos diretórios `.claude/` e `.github/`, também o diretório `.codex/` para projetos que utilizam o **OpenAI Codex CLI**. O gerador deve produzir um arquivo `AGENTS.md` consolidado (equivalente às rules + agents do Claude Code) e um `config.toml` estruturado (equivalente ao settings.json), mantendo consistência semântica com os artefatos já gerados para os outros ecossistemas.

O escopo inclui: criação de templates Nunjucks modulares para Codex, implementação de 2 novos assemblers (`CodexAgentsMdAssembler` e `CodexConfigAssembler`), integração no pipeline existente (posição 14-15, antes do ReadmeAssembler), e atualização do ReadmeAssembler para contabilizar artefatos Codex. O diretório `resources/` recebe novos templates em `codex-templates/`. Nenhum byte do output `.claude/` ou `.github/` existente é alterado.

**Excluído do escopo:** Modificação do formato YAML de configuração (v3), alteração da geração existente de `.claude/` ou `.github/`, implementação de MCP servers para Codex, geração de scripts executáveis para skills, implementação de multi-agent orchestration real.

## 2. Anexos e Referências

- EPIC-001 (Migração Python → Node/TS): `stories/migration-python-to-node-ts/EPIC-001.md`
- IMPLEMENTATION-MAP do EPIC-001: `stories/migration-python-to-node-ts/IMPLEMENTATION-MAP.md`
- Código-fonte TypeScript (em migração): `src/`
- Resources (templates): `resources/`
- OpenAI Codex CLI docs: https://developers.openai.com/codex/config-basic/
- OpenAI Codex AGENTS.md guide: https://developers.openai.com/codex/guides/agents-md/
- OpenAI Codex config reference: https://developers.openai.com/codex/config-reference/
- OpenAI Codex config sample: https://developers.openai.com/codex/config-sample/

## 3. Definições de Qualidade Globais

### Global Definition of Ready (DoR)

- Stories do EPIC-001 das quais depende estão concluídas (Phase 1-3 mínimo)
- Interfaces TypeScript de assemblers e template engine disponíveis e compiláveis
- Documentação oficial do Codex CLI consultada para validação de formato
- Fixtures YAML de teste preparadas com múltiplas configurações (minimal, full, domain-driven)
- Nenhuma dependência bloqueante em status "Em Andamento"

### Global Definition of Done (DoD)

- **Cobertura:** ≥ 95% Line Coverage, ≥ 90% Branch Coverage por módulo
- **Testes Automatizados:** Unitários + integração. Cenários Gherkin implementados como testes. Testes de regressão confirmando que output `.claude/` e `.github/` não foi alterado.
- **Relatório de Cobertura:** `vitest` coverage report em formato lcov e text, granularidade por arquivo
- **Documentação:** JSDoc em funções/classes públicas
- **Persistência:** N/A (filesystem-only, sem banco de dados)
- **Performance:** Tempo de geração ≤ 2× tempo anterior do pipeline (aceitável por ser I/O-bound)

## 4. Regras de Negócio Transversais (Source of Truth)

> Regras que se aplicam a múltiplas histórias. Cada história referencia as regras pelo ID. Alterações de regra propagam automaticamente para todas as histórias dependentes.

| ID | Título | Descrição |
| :--- | :--- | :--- |
| **[RULE-101]** | Consolidação AGENTS.md | O `AGENTS.md` deve consolidar informações de rules (01-project-identity, 03-coding-standards, 04-architecture-summary, 05-quality-gates), domain rule, agents e skills em um único Markdown coerente.<br>Cada conceito aparece **uma única vez** — sem duplicação entre seções.<br>Seções são modulares e condicionalmente incluídas/excluídas baseado no config. |
| **[RULE-102]** | Seções condicionais | Seções do AGENTS.md são condicionais:<br>• Domain → somente se `architecture.domain_driven = true`<br>• Security → somente se `security.frameworks` não vazio<br>• Database/Cache (em Tech Stack) → somente se != "none"<br>• MCP (em config.toml) → somente se `mcp.servers` não vazio<br>• Agent Personas → somente se agents foram gerados (sempre true no momento) |
| **[RULE-103]** | Derivação determinística do config.toml | O `config.toml` deve ser derivado deterministicamente da config YAML do projeto:<br>• `model` → default `"o4-mini"` (hardcoded)<br>• `approval_policy` → `"on-request"` se hooks presentes, `"untrusted"` caso contrário<br>• `sandbox_mode` → `"workspace-write"` (default seguro para projetos gerados)<br>• `[mcp_servers]` → mapeamento 1:1 de `config.mcp.servers`<br>Sem inputs adicionais do usuário. |
| **[RULE-104]** | Modularidade de templates | O AGENTS.md é composto por seções modulares (templates `.njk` individuais em `sections/`).<br>O template principal (`agents-md.md.njk`) inclui cada seção condicionalmente via `{% include %}` com guards `{% if %}`.<br>Cada seção é testável independentemente. |
| **[RULE-105]** | Impacto zero no output existente | A adição de output Codex NÃO deve alterar nenhum byte do output `.claude/` ou `.github/` existente.<br>Os novos assemblers são **adicionais** ao pipeline, não substitutos.<br>Testes de regressão obrigatórios comparando output antes/depois da integração. |
| **[RULE-106]** | Padrão de extensão do pipeline | Novos assemblers devem:<br>• Implementar a mesma interface `Assembler` existente<br>• Ser inseridos após `GithubPromptsAssembler` e antes de `ReadmeAssembler`<br>• Receber os mesmos parâmetros (config, outputDir, resourcesDir, engine)<br>• Retornar `{ files: string[]; warnings: string[] }` |
| **[RULE-107]** | Paridade de placeholders | Os mesmos 25 campos de placeholder context (definidos em `template-engine.ts`) devem ser usados nos templates Codex.<br>Nenhum campo novo é necessário para a geração base. |
| **[RULE-108]** | Contexto estendido para AGENTS.md | Além do context flat de 25 campos, o `CodexAgentsMdAssembler` precisa de dados adicionais que **já existem** no pipeline:<br>• `ResolvedStack` (build_cmd, test_cmd, compile_cmd, coverage_cmd)<br>• Lista de agents gerados (lida do diretório de output após AgentsAssembler)<br>• Lista de skills geradas (lida do diretório de output após SkillsAssembler)<br>Esses dados são passados pelo pipeline, não recomputados. |
| **[RULE-109]** | Feature gating Codex | A geração de artefatos Codex deve respeitar o mesmo feature gating existente.<br>Se database é "none", a linha de database não aparece na tabela Tech Stack do AGENTS.md.<br>Se não há interfaces de messaging, seção de event-driven não aparece.<br>Mesma lógica de `RULE-006` do EPIC-001 aplicada ao contexto Codex. |
| **[RULE-110]** | TOML via template | O `config.toml` deve ser gerado via template Nunjucks (não serialização programática de TOML), para manter controle total do formato de saída e comentários inline explicativos. |
| **[RULE-111]** | Atualização do ReadmeAssembler | O ReadmeAssembler deve ser atualizado para:<br>• Contar artefatos Codex (AGENTS.md + config.toml = 2 artefatos mínimos)<br>• Adicionar coluna/seção Codex na tabela de mapping<br>• Incluir seção Codex na tabela de Generation Summary |

## 5. Índice de Histórias

| ID | Título | Dependências (Blocked By) |
| :--- | :--- | :--- |
| [STORY-021](./STORY_STORY-021_codex-nunjucks-templates.md) | Templates Nunjucks para Codex | EPIC-001/STORY-005 |
| [STORY-022](./STORY_STORY-022_codex-agents-md-assembler.md) | CodexAgentsMdAssembler | STORY-021, EPIC-001/STORY-007, EPIC-001/STORY-008 |
| [STORY-023](./STORY_STORY-023_codex-config-assembler.md) | CodexConfigAssembler | STORY-021, EPIC-001/STORY-007, EPIC-001/STORY-008 |
| [STORY-024](./STORY_STORY-024_pipeline-readme-update.md) | Pipeline + ReadmeAssembler Update | STORY-022, STORY-023, EPIC-001/STORY-016 |
| [STORY-025](./STORY_STORY-025_codex-integration-tests.md) | Testes de Integração Codex | STORY-024 |
