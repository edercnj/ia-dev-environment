# Épico: Migração do ia-dev-environment de Python para Node.js com TypeScript

**Autor:** Arquiteto de Software / Claude
**Data:** 2026-03-09
**Versão:** 1.0.0
**Status:** Em Refinamento

## 1. Visão Geral

**Chave Jira:** EPIC-001

Migrar o **ia-dev-environment** — um gerador de scaffolding que produz diretórios `.claude/` e `.github/` completos para projetos Claude Code — de Python para Node.js com TypeScript. O projeto atual possui ~5.100 linhas de código-fonte em 38 módulos Python, com CLI baseada em Click, templates Jinja2 e 14 assemblers que geram artefatos condicionalmente a partir de configuração YAML.

O escopo inclui a reescrita completa de todos os módulos Python em TypeScript, preservando 100% das regras de negócio, interface CLI idêntica, output byte-for-byte compatível, e cobertura de testes ≥ 95% line / ≥ 90% branch. O diretório `resources/` (templates, config-templates, knowledge packs) permanece **inalterado**.

**Excluído do escopo:** Novas funcionalidades, alterações em templates Markdown/YAML, mudanças na estrutura de output.

## 2. Anexos e Referências

- Código-fonte Python: `src/ia_dev_env/` (38 módulos, ~5.100 LOC)
- Testes Python: `tests/` (22 arquivos, ~3.900 LOC)
- Resources: `resources/` (templates, config-templates, knowledge packs, agents, skills)
- Templates de decomposição: `resources/templates/_TEMPLATE-EPIC.md`, `_TEMPLATE-STORY.md`, `_TEMPLATE-IMPLEMENTATION-MAP.md`

## 3. Definições de Qualidade Globais

### Global Definition of Ready (DoR)

- Módulo Python de origem lido e compreendido integralmente
- Interfaces TypeScript de entrada/saída definidas e documentadas
- Dependências npm necessárias identificadas e versionadas
- Testes de paridade com Python definidos (fixtures YAML + outputs esperados)
- Nenhuma dependência bloqueante em status "Em Andamento"

### Global Definition of Done (DoD)

- **Cobertura:** ≥ 95% Line Coverage, ≥ 90% Branch Coverage por módulo
- **Testes Automatizados:** Unitários + integração. Cenários Gherkin implementados como testes. Testes de paridade com output Python para assemblers.
- **Relatório de Cobertura:** `vitest` coverage report em formato lcov e text, granularidade por arquivo
- **Documentação:** JSDoc em funções/classes públicas. README atualizado na STORY-020.
- **Persistência:** N/A (filesystem-only, sem banco de dados)
- **Performance:** Tempo de geração ≤ 2× tempo do Python para o mesmo input (aceitável por ser I/O-bound)

## 4. Regras de Negócio Transversais (Source of Truth)

> Regras que se aplicam a múltiplas histórias. Cada história referencia as regras pelo ID. Alterações de regra propagam automaticamente para todas as histórias dependentes.

| ID | Título | Descrição |
| :--- | :--- | :--- |
| **[RULE-001]** | Compatibilidade de output | Output gerado pelo Node/TS deve ser byte-for-byte idêntico ao Python para o mesmo input YAML.<br>Isso inclui: conteúdo de arquivos, nomes de arquivos, estrutura de diretórios, permissões de arquivos (ex: hooks executáveis).<br>Verificação via `diff -r` entre outputs Python e TypeScript com mesma config. |
| **[RULE-002]** | Migração v2→v3 | Lógica de detecção (`detect_v2_format`) e migração (`migrate_v2_to_v3`) de formato legado deve ser preservada exatamente.<br>Mappings `TYPE_MAPPING` e `STACK_MAPPING` devem ser idênticos.<br>Warnings emitidos durante migração devem ter mesma semântica. |
| **[RULE-003]** | Validação de paths | Proteção contra symlinks (`_validate_dest_path`), path traversal e protected paths (`_reject_dangerous_path`) deve ser idêntica.<br>PROTECTED_PATHS: `/`, `/tmp`, `/var`, `/etc`, `/usr`.<br>Rejeitar CWD e home directory como destino. |
| **[RULE-004]** | Atomic output | Escrita atômica via temp dir + copy deve ser preservada.<br>Usar `node:os.tmpdir()` + `node:fs/promises` para criar temp, executar pipeline, e copiar atomicamente para destino.<br>Em caso de erro, temp dir deve ser limpo. |
| **[RULE-005]** | Placeholder replacement | Regex `\{(\w+)\}` e contexto default (project_name, language_name, framework_name, etc.) devem produzir resultado idêntico.<br>O contexto flat dict com 25 campos deve ser preservado exatamente. |
| **[RULE-006]** | Feature gating | Toda lógica condicional de seleção de skills, agents, knowledge packs, rules, hooks e settings deve ser preservada.<br>Condições baseadas em: interface types, infrastructure config, architecture style, security frameworks, testing config, database/cache, language/build_tool. |
| **[RULE-007]** | Template engine config | Nunjucks deve ser configurado para produzir output idêntico ao Jinja2 SandboxedEnvironment:<br>`autoescape=false`, `keep_trailing_newline=true`, `trim_blocks=false`, `lstrip_blocks=false`, `StrictUndefined` equivalent.<br>FileSystemLoader apontando para `resources_dir`. |
| **[RULE-008]** | Assembler ordering | Os 14 assemblers devem executar na mesma ordem: Rules → Skills → Agents → Patterns → Protocols → Hooks → Settings → GithubInstructions → GithubMcp → GithubSkills → GithubAgents → GithubHooks → GithubPrompts → Readme. |
| **[RULE-009]** | Knowledge pack detection | Lógica de leitura de `SKILL.md` para classificar knowledge packs (busca `user-invocable: false` ou `# Knowledge Pack`) deve ser idêntica à implementação Python. |
| **[RULE-010]** | Interactive mode choices | Mesmos choices hardcoded, mesma ordem, mesmo comportamento.<br>Architecture: library, microservice, monolith.<br>Language: python, java, go, kotlin, typescript, rust.<br>Interface: rest, grpc, cli, event-consumer, event-producer.<br>Build tool: pip, maven, gradle, go, cargo, npm.<br>Framework: mapeamento por linguagem idêntico ao Python. |
| **[RULE-011]** | Resources inalterados | Nenhum arquivo do diretório `resources/` pode ser modificado, adicionado ou removido.<br>Templates Markdown, YAML de config, knowledge packs, agents e skills permanecem exatamente como estão. |
| **[RULE-012]** | Auditor thresholds | Limites de auditoria de rules devem ser preservados: ≤ 10 arquivos de regra, ≤ 50KB total.<br>Warnings gerados quando limites são excedidos. |
| **[RULE-013]** | Consolidator logic | Consolidação de framework rules em 3 grupos (core, data, ops) deve ser preservada.<br>Lógica de `consolidate_files()` e `consolidate_framework_rules()` deve produzir output idêntico. |
| **[RULE-014]** | Version resolver fallback | Resolução de diretórios versionados com fallback (exact version → major.x) deve ser preservada.<br>Usado para language e framework specific files. |
| **[RULE-015]** | CLI interface idêntica | Mesmos comandos (`generate`, `validate`), mesmas opções (`--config`, `--interactive`, `--output-dir`, `--resources-dir`, `--verbose`, `--dry-run`), mesma exclusão mútua entre `--config` e `--interactive`, mesma tabela de resultado. |

## 5. Índice de Histórias

| ID | Título | Dependências (Blocked By) |
| :--- | :--- | :--- |
| [STORY-001](./STORY_STORY-001_project-foundation.md) | Setup do Projeto Node.js + TypeScript | — |
| [STORY-002](./STORY_STORY-002_exceptions-utils.md) | Exceptions e Utilitários | STORY-001 |
| [STORY-003](./STORY_STORY-003_models.md) | Models — Interfaces e Classes TypeScript | STORY-001 |
| [STORY-004](./STORY_STORY-004_config-loader.md) | Config Loader + Migração v2→v3 | STORY-002, STORY-003 |
| [STORY-005](./STORY_STORY-005_template-engine.md) | Template Engine (Nunjucks) | STORY-001 |
| [STORY-006](./STORY_STORY-006_domain-mappings.md) | Domain Layer — Mappings e Constantes | STORY-003 |
| [STORY-007](./STORY_STORY-007_domain-validator-resolver.md) | Domain Layer — Validator, Resolver e Skill Registry | STORY-003, STORY-006 |
| [STORY-008](./STORY_STORY-008_assembler-helpers.md) | Assembler Helpers | STORY-003, STORY-005 |
| [STORY-009](./STORY_STORY-009_rules-assembler.md) | RulesAssembler | STORY-006, STORY-007, STORY-008 |
| [STORY-010](./STORY_STORY-010_skills-assembler.md) | SkillsAssembler | STORY-006, STORY-007, STORY-008 |
| [STORY-011](./STORY_STORY-011_agents-assembler.md) | AgentsAssembler | STORY-006, STORY-007, STORY-008 |
| [STORY-012](./STORY_STORY-012_patterns-protocols-assembler.md) | PatternsAssembler + ProtocolsAssembler | STORY-006, STORY-008 |
| [STORY-013](./STORY_STORY-013_hooks-settings-assembler.md) | HooksAssembler + SettingsAssembler | STORY-006, STORY-008 |
| [STORY-014](./STORY_STORY-014_github-assemblers.md) | GitHub Assemblers (Instructions, MCP, Skills, Agents, Hooks, Prompts) | STORY-005, STORY-006, STORY-008 |
| [STORY-015](./STORY_STORY-015_readme-assembler.md) | ReadmeAssembler | STORY-005, STORY-008 |
| [STORY-016](./STORY_STORY-016_pipeline-orchestrator.md) | Pipeline Orchestrator | STORY-002, STORY-009, STORY-010, STORY-011, STORY-012, STORY-013, STORY-014, STORY-015 |
| [STORY-017](./STORY_STORY-017_interactive-mode.md) | Interactive Mode | STORY-003, STORY-004 |
| [STORY-018](./STORY_STORY-018_cli-entry-point.md) | CLI Entry Point | STORY-004, STORY-016, STORY-017 |
| [STORY-019](./STORY_STORY-019_integration-tests-parity.md) | Testes de Integração + Verificação de Paridade | STORY-018 |
| [STORY-020](./STORY_STORY-020_cicd-packaging.md) | CI/CD, Packaging e Documentação | STORY-019 |
