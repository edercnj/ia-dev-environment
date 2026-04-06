# Prompt: Geração de Épico e Histórias — ia-dev-environment Platform Target Filter

> **Instrução de uso**: Execute `/x-story-epic-full` com este arquivo como especificação de entrada.
> Exemplo: `/x-story-epic-full specs/SPEC-platform-target-filter-v1.md`

---

## Sistema

**Projeto**: `ia-dev-environment` — CLI generator de ambientes de desenvolvimento assistidos por IA.

**Versão base analisada**: branch `feat/epic-0023-full-implementation`, ~1050 commits.

**Objetivo desta especificação**: Adicionar uma flag `--platform` ao comando `generate` que permite
ao usuário escolher para qual ferramenta de IA gerar os artefatos de saída. As plataformas suportadas
são: `claude-code` (default), `copilot`, `codex` e `all`. Quando uma plataforma é selecionada,
somente os assemblers relevantes para aquela plataforma devem ser executados, produzindo apenas os
diretórios e arquivos necessários.

**Princípio central de todas as histórias**: Atualmente o `ia-dev-env generate` executa todos os 33
assemblers independentemente da plataforma desejada, produzindo artefatos para Claude Code (.claude/),
GitHub Copilot (.github/), OpenAI Codex (.codex/, .agents/) e documentação (ROOT) simultaneamente.
Isso gera diretórios desnecessários, aumenta o tempo de geração, e confunde o usuário que só utiliza
uma ferramenta. A mudança deve ser retrocompatível: `--platform all` reproduz o comportamento atual.

---

## Escopo do Épico

### Contexto de negócio

O gerador `ia-dev-environment` produz artefatos para múltiplas plataformas de IA em uma única execução.
Entretanto, a maioria dos usuários utiliza apenas uma ferramenta (Claude Code, GitHub Copilot, ou
OpenAI Codex). Gerar artefatos para todas as plataformas:

1. **Cria diretórios desnecessários** — Um usuário de Claude Code recebe `.github/`, `.codex/` e
   `.agents/` que não vai utilizar, poluindo o repositório.

2. **Aumenta o tempo de geração** — 33 assemblers rodam sequencialmente; se o usuário precisa apenas
   de 10, os outros 23 são desperdício.

3. **Confunde a documentação** — O README, CLAUDE.md e contagens de artefatos refletem todas as
   plataformas, quando o usuário só se importa com a sua.

4. **Complica o `.gitignore`** — Usuários precisam manualmente ignorar diretórios de plataformas
   que não usam.

### Dimensões de melhoria

1. **Nova flag CLI `--platform`** — Aceita valores: `claude-code` (default), `copilot`, `codex`,
   `all`. Múltiplos valores separados por vírgula: `--platform claude-code,copilot`.

2. **Mapeamento assembler → plataforma** — Cada assembler é categorizado em uma ou mais plataformas.
   A factory filtra assemblers com base na seleção.

3. **Suporte no YAML config** — Seção `platform:` no arquivo de configuração para definir o default
   sem precisar da flag CLI.

4. **Contagem dinâmica de artefatos** — README e CLAUDE.md gerados devem refletir apenas os
   artefatos da plataforma selecionada, não o total global.

5. **Atualização de documentação** — README.md, CLAUDE.md, e `--help` do CLI devem documentar a
   nova flag e seus valores.

6. **Atualização de smoke tests e cobertura** — Testes existentes devem cobrir cada plataforma
   isoladamente e combinações.

### Mapeamento de Assemblers por Plataforma

Os 33 assemblers existentes se categorizam assim:

| Plataforma | Assemblers | Target Dirs |
|:---|:---|:---|
| `shared` (sempre executa) | ConstitutionAssembler, DocsAssembler, GrpcDocsAssembler, RunbookAssembler, IncidentTemplatesAssembler, ReleaseChecklistAssembler, OperationalRunbookAssembler, SloSliTemplateAssembler, DocsContributingAssembler, DataMigrationPlanAssembler, CicdAssembler, EpicReportAssembler, DocsAdrAssembler | ROOT |
| `claude-code` | RulesAssembler, SkillsAssembler, AgentsAssembler, PatternsAssembler, ProtocolsAssembler, HooksAssembler, SettingsAssembler, ReadmeAssembler | CLAUDE |
| `copilot` | GithubInstructionsAssembler, GithubMcpAssembler, GithubSkillsAssembler, GithubAgentsAssembler, GithubHooksAssembler, GithubPromptsAssembler, PrIssueTemplateAssembler | GITHUB |
| `codex` | CodexAgentsMdAssembler, CodexConfigAssembler, CodexSkillsAssembler, CodexRequirementsAssembler, CodexOverrideAssembler | CODEX, CODEX_AGENTS, ROOT (AGENTS.md) |

Nota: Assemblers `shared` (ROOT/docs) SEMPRE executam independentemente da plataforma selecionada.

---

## Regras de Negócio Transversais (Cross-Cutting Rules)

**RULE-001**: **Retrocompatibilidade Total** — O comportamento default (`--platform all` ou sem flag
quando `platform` não está no YAML) DEVE reproduzir exatamente o mesmo conjunto de artefatos que a
versão atual. Nenhum teste existente pode quebrar.

**RULE-002**: **Ordem de Assemblers Preservada** — A ordem dos 33 assemblers (RULE-005 do projeto)
deve ser mantida. A filtragem apenas pula assemblers, nunca reordena.

**RULE-003**: **Shared é Sempre Incluído** — Os assemblers de documentação e CI/CD (target ROOT)
que não são específicos de nenhuma plataforma devem executar SEMPRE, independente da seleção.

**RULE-004**: **CLI Tem Precedência sobre YAML** — Se `--platform` é fornecido na CLI E `platform:`
existe no YAML config, a flag CLI prevalece.

**RULE-005**: **Validação de Valores** — Valores inválidos para `--platform` devem falhar com
mensagem clara listando os valores aceitos. A validação ocorre antes do pipeline.

**RULE-006**: **Contagens Dinâmicas** — O README.md e CLAUDE.md gerados devem refletir APENAS os
artefatos da plataforma selecionada. A seção "Generation Summary" deve ser condicional.

**RULE-007**: **Dry-Run Respeita Filtro** — O modo `--dry-run` deve listar apenas os assemblers que
seriam executados para a plataforma selecionada.

**RULE-008**: **Verbose Respeita Filtro** — O modo `--verbose` deve exibir quais assemblers foram
incluídos e quais foram filtrados, com a razão (plataforma).

**RULE-009**: **Composição de Plataformas** — `--platform claude-code,copilot` deve executar
assemblers de AMBAS as plataformas (união), mais os `shared`.

**RULE-010**: **Enum Extensível** — O design deve permitir adicionar novas plataformas no futuro
(ex: `cursor`, `windsurf`) sem modificar lógica existente.

---

## Histórias

---

### STORY-0001: Platform Enum e Mapeamento de Assemblers

**Escopo**: Criar o enum `Platform` no domain model e o mapeamento de cada assembler para sua(s)
plataforma(s). Atualizar `AssemblerDescriptor` para incluir o campo `platform`.

**Detalhes**:
- Enum `Platform` com valores: `CLAUDE_CODE`, `COPILOT`, `CODEX`, `SHARED`
- `AssemblerDescriptor` ganha campo `Set<Platform> platforms`
- `AssemblerFactory` atribui plataformas a cada assembler no momento da construção
- Nenhuma filtragem ainda — apenas metadata

**Critérios de Aceite**:
- Enum existe no pacote `domain.model`
- Cada um dos 33 assemblers tem pelo menos 1 plataforma atribuída
- Assemblers ROOT/docs têm platform = SHARED
- Testes unitários validam o mapeamento completo

---

### STORY-0002: Filtro de Assemblers no Pipeline

**Escopo**: Implementar a lógica de filtragem no `AssemblerFactory` ou `AssemblerPipeline` que
recebe um `Set<Platform>` e retorna apenas os assemblers aplicáveis.

**Detalhes**:
- Novo campo em `PipelineOptions`: `Set<Platform> platforms`
- `AssemblerFactory.buildAssemblers()` filtra descriptors onde a interseção entre
  `descriptor.platforms` e `options.platforms` é não-vazia
- `SHARED` é sempre incluído (RULE-003)
- Quando `platforms` está vazio ou contém todos, nenhum filtro é aplicado (RULE-001)
- Preservar a ordem original (RULE-002)

**Critérios de Aceite**:
- Com `platforms = {CLAUDE_CODE}`, apenas 8 assemblers Claude + shared executam
- Com `platforms = {COPILOT}`, apenas 7 assemblers GitHub + shared executam
- Com `platforms = {CODEX}`, apenas 5 assemblers Codex + shared executam
- Com `platforms = {CLAUDE_CODE, COPILOT}`, é a união de ambos + shared
- Com `platforms = all`, todos os 33 assemblers executam
- Testes unitários para cada combinação

---

### STORY-0003: Flag CLI `--platform`

**Escopo**: Adicionar a opção `--platform` ao `GenerateCommand` com Picocli, incluindo validação
e conversão de valores.

**Detalhes**:
- Flag: `--platform` / `-p` (short form)
- Tipo: `List<Platform>` via custom `ITypeConverter<Platform>`
- Default: sem valor (comportamento = all, para retrocompatibilidade — RULE-001)
- Aceita múltiplos valores separados por vírgula: `--platform claude-code,copilot`
- Valores aceitos: `claude-code`, `copilot`, `codex`, `all`
- Validação com mensagem amigável (RULE-005)
- Passa `Set<Platform>` para `PipelineOptions`

**Critérios de Aceite**:
- `ia-dev-env generate --platform claude-code` gera apenas .claude/ + docs
- `ia-dev-env generate --platform copilot` gera apenas .github/ + docs
- `ia-dev-env generate --platform codex` gera apenas .codex/ + .agents/ + docs
- `ia-dev-env generate --platform all` gera tudo (comportamento atual)
- `ia-dev-env generate` (sem flag) gera tudo (retrocompatibilidade)
- `ia-dev-env generate --platform invalid` falha com mensagem clara
- `ia-dev-env generate -p claude-code,copilot` funciona
- `--help` documenta a flag com descrição e valores aceitos

---

### STORY-0004: Suporte `platform:` no YAML Config

**Escopo**: Adicionar a seção `platform:` ao schema YAML de configuração, permitindo definir a
plataforma default sem flag CLI.

**Detalhes**:
- Nova seção no YAML: `platform: claude-code` (ou lista: `platform: [claude-code, copilot]`)
- `ProjectConfig` ganha campo opcional `PlatformConfig`
- `ConfigSourceLoader` e `ProjectConfigFactory` parseiam a nova seção
- CLI flag `--platform` tem precedência sobre YAML (RULE-004)
- Validação no `StackValidator`
- Atualizar config templates dos 14 profiles com `platform: all` (default explícito)

**Critérios de Aceite**:
- YAML com `platform: claude-code` gera apenas .claude/ + docs
- YAML com `platform: [claude-code, copilot]` gera ambas + docs
- YAML sem seção `platform:` gera tudo (retrocompatibilidade)
- CLI `--platform codex` sobrescreve YAML `platform: claude-code`
- Todos os 14 profile templates atualizados
- Testes de parsing e precedência

---

### STORY-0005: Contagem Dinâmica de Artefatos no README e CLAUDE.md

**Escopo**: Atualizar `ReadmeAssembler` e template do CLAUDE.md para que as contagens reflitam
apenas os artefatos da plataforma selecionada (RULE-006).

**Detalhes**:
- A seção "Generation Summary" do CLAUDE.md deve listar apenas plataformas geradas
- O README deve indicar qual plataforma foi configurada
- Contagens de skills, agents, rules, hooks devem ser condicionais
- A tabela de mapeamento .claude/ ↔ .github/ ↔ .codex/ só aparece se mais de uma plataforma
  está selecionada ou se `all`
- Template Nunjucks com condicionais `{% if platform.includes('claude-code') %}`

**Critérios de Aceite**:
- CLAUDE.md gerado com `--platform claude-code` não menciona .github/ nem .codex/
- README gerado indica "Platform: claude-code" na identidade do projeto
- Contagens são precisas para cada combinação de plataforma
- Golden files atualizados para os 8 profiles core
- Testes de snapshot validam output por plataforma

---

### STORY-0006: Verbose e Dry-Run com Awareness de Plataforma

**Escopo**: Atualizar os modos verbose e dry-run para refletirem o filtro de plataforma.

**Detalhes**:
- `--verbose` exibe: "Platform filter: claude-code → 21 assemblers (8 platform + 13 shared)"
- `--verbose` lista assemblers pulados: "SKIPPED: GithubSkillsAssembler (platform: copilot)"
- `--dry-run` lista apenas os arquivos que seriam gerados para a plataforma selecionada
- `CliDisplay` e `VerbosePipelineRunner` atualizados

**Critérios de Aceite**:
- Verbose com `--platform claude-code` mostra assemblers filtrados
- Verbose lista assemblers skipped com razão
- Dry-run com `--platform codex` lista apenas artefatos codex + shared
- Testes para output verbose e dry-run

---

### STORY-0007: Atualização de Testes e Golden Files

**Escopo**: Atualizar o test suite existente para cobrir cenários de filtragem por plataforma.
Adicionar golden files por plataforma.

**Detalhes**:
- Testes unitários para `Platform` enum, converter, validação
- Testes de integração para pipeline filtrado (cada plataforma isolada)
- Testes de integração para combinações de plataforma
- Smoke tests atualizados: verificar que diretórios não-selecionados NÃO existem
- Golden files: manter os existentes (`--platform all`) e adicionar variantes por plataforma
  para pelo menos 2 profiles representativos
- Profile integrity tests atualizados

**Critérios de Aceite**:
- ≥ 95% line coverage, ≥ 90% branch coverage mantidos
- Nenhum teste existente quebrado (RULE-001)
- Smoke test valida que `--platform claude-code` NÃO gera .github/
- Smoke test valida que `--platform copilot` NÃO gera .claude/
- Testes de golden files passam para todas as combinações
- Testes cobrem: default (all), single platform, multi-platform, invalid value

---

### STORY-0008: Documentação e Help Text

**Escopo**: Atualizar toda documentação user-facing: README.md do projeto, `--help`, CHANGELOG,
e exemplos de uso.

**Detalhes**:
- README.md principal: nova seção "Platform Selection" com exemplos
- README.md: atualizar "Quick Start" com menção à flag `--platform`
- `--help`: descrição clara da flag com valores aceitos e exemplos
- CHANGELOG: entrada para a nova feature
- Atualizar o CLAUDE.md do projeto (root) se necessário
- Atualizar a tabela de mapeamento .claude/ ↔ .github/ ↔ .codex/ para explicar que é condicional

**Critérios de Aceite**:
- README tem seção "Platform Selection" com exemplos de uso
- `ia-dev-env generate --help` mostra a flag documentada
- CHANGELOG atualizado com `feat: add --platform flag for targeted generation`
- Exemplos cobrem: single platform, multi-platform, YAML config
- Documentação menciona default behavior e retrocompatibilidade
