# Prompt: Migração ia-dev-environment de Python para Node.js com TypeScript

## Contexto do Projeto

O **ia-dev-environment** é um gerador de scaffolding reutilizável e project-agnostic que produz diretórios `.claude/` completos para projetos Claude Code. É uma CLI Python que recebe configuração YAML e gera regras, skills, agents, configurações GitHub, patterns, protocols e hooks.

**Versão atual:** 0.1.0
**Licença:** MIT
**Python:** >= 3.9
**LOC fonte:** ~5.100 linhas (22 módulos)
**LOC testes:** ~3.900 linhas (22 arquivos de teste)

---

## Objetivo

Migrar o **ia-dev-environment** de Python para **Node.js com TypeScript**, mantendo:

1. **100% das regras de negócio** — toda lógica condicional, mappings, validações e feature gating
2. **Interface CLI idêntica** — mesmos comandos, opções, flags e comportamento
3. **Compatibilidade de saída** — output byte-for-byte idêntico ao Python para o mesmo input YAML
4. **Cobertura de testes** — ≥ 95% line coverage, ≥ 90% branch coverage
5. **Estrutura de resources inalterada** — nenhum template Markdown, YAML de config ou knowledge pack é modificado

---

## Escopo da Migração

### O que MUDA

| Componente Python | Equivalente Node/TypeScript |
| :--- | :--- |
| `click` (CLI framework) | `commander` ou `yargs` |
| `pyyaml` (YAML parsing) | `js-yaml` |
| `jinja2` (template engine) | `nunjucks` (compatível com Jinja2) |
| `dataclasses` (models) | Interfaces TypeScript + classes com validação |
| `pytest` + `pytest-cov` | `vitest` (ou `jest`) com coverage nativo |
| `setuptools` / `pyproject.toml` | `package.json` + `tsconfig.json` + `tsup` (ou `tsx`) |
| `pathlib.Path` | `node:path` + `node:fs/promises` |
| `shutil` / `tempfile` | `node:fs/promises` + `tmp` (ou `node:os.tmpdir`) |
| Entry point: `ia-dev-env` via setuptools scripts | Entry point: `ia-dev-env` via `package.json#bin` |

### O que NÃO MUDA

- Diretório `resources/` inteiro (templates, config-templates, knowledge packs, agents, skills)
- Formato do YAML de configuração (v2 e v3)
- Nomes de arquivos gerados e estrutura de diretórios de output
- Lógica de placeholder replacement (`{project_name}`, etc.)
- Lógica de template Jinja2 (Nunjucks é compatível)
- Comportamento de dry-run, verbose, interactive mode
- Validação de segurança de paths (symlink, protected paths, path traversal)

---

## Arquitetura Atual (Python) — Mapeamento Completo

### 1. Entry Point e CLI (`__main__.py`)

**Comandos:**
- `ia-dev-env generate` — Gera scaffolding a partir de config YAML ou modo interativo
  - `--config / -c` (Path, exists=True) — Caminho para YAML
  - `--interactive / -i` (flag) — Modo interativo (mutuamente exclusivo com --config)
  - `--output-dir / -o` (Path, default=".") — Diretório de saída
  - `--resources-dir / -s` (Path, exists=True) — Diretório de resources (auto-detectado se omitido)
  - `--verbose / -v` (flag) — Logging verbose
  - `--dry-run` (flag) — Preview sem escrita
- `ia-dev-env validate` — Valida config YAML sem gerar output
  - `--config / -c` (Path, exists=True, required) — Caminho para YAML
  - `--verbose / -v` (flag)

**Lógica:**
- `--config` e `--interactive` são mutuamente exclusivos
- Sem nenhum dos dois: erro
- Display de resultado: tabela de contagem por componente (Rules, Skills, Knowledge Packs, Agents, Hooks, Settings, README, GitHub)
- Classificação de arquivos: `_classify_files()` analisa partes do path e nome para categorizar
- Detecção de knowledge pack: lê `SKILL.md` procurando `user-invocable: false` ou `# Knowledge Pack`

### 2. Models (`models.py`)

**Dataclasses (todas com `from_dict` classmethod):**

| Dataclass | Campos Obrigatórios | Campos Opcionais (defaults) |
| :--- | :--- | :--- |
| `ProjectIdentity` | name, purpose | — |
| `ArchitectureConfig` | style | domain_driven=False, event_driven=False |
| `InterfaceConfig` | type | spec="", broker="" |
| `LanguageConfig` | name, version | — |
| `FrameworkConfig` | name, version | build_tool="pip", native_build=False |
| `TechComponent` | — | name="none", version="" |
| `DataConfig` | — | database=TechComponent(), migration=TechComponent(), cache=TechComponent() |
| `SecurityConfig` | — | frameworks=[] |
| `ObservabilityConfig` | — | tool="none", metrics="none", tracing="none" |
| `InfraConfig` | — | container="docker", orchestrator="none", templating="kustomize", iac="none", registry="none", api_gateway="none", service_mesh="none", observability=ObservabilityConfig() |
| `TestingConfig` | — | smoke_tests=True, contract_tests=False, performance_tests=True, coverage_line=95, coverage_branch=90 |
| `McpServerConfig` | id, url | capabilities=[], env={} |
| `McpConfig` | — | servers=[] |
| `ProjectConfig` | project, architecture, interfaces, language, framework | data, infrastructure, security, testing, mcp |
| `PipelineResult` | success, output_dir, files_generated, warnings, duration_ms | — |
| `FileDiff` | path, diff, python_size, reference_size | — |
| `VerificationResult` | success, total_files, mismatches, missing_files, extra_files | — |

**Função auxiliar:** `_require(data, key, model)` — extrai campo obrigatório ou lança `KeyError` com mensagem descritiva.

### 3. Config Loader (`config.py`)

**Constantes:**
- `REQUIRED_SECTIONS` = ("project", "architecture", "interfaces", "language", "framework")
- `TYPE_MAPPING`: v2 type → (architecture_style, interfaces)
  - api → ("microservice", [{"type": "rest"}])
  - cli → ("library", [{"type": "cli"}])
  - library → ("library", [])
  - worker → ("microservice", [{"type": "event-consumer"}])
  - fullstack → ("monolith", [{"type": "rest"}])
- `STACK_MAPPING`: v2 stack → (language, version, framework, version)
  - java-quarkus → ("java", "21", "quarkus", "3.17")
  - java-spring → ("java", "21", "spring-boot", "3.4")
  - python-fastapi → ("python", "3.12", "fastapi", "0.115")
  - python-click-cli → ("python", "3.9", "click", "8.1")
  - go-gin → ("go", "1.23", "gin", "1.10")
  - kotlin-ktor → ("kotlin", "2.1", "ktor", "3.0")
  - typescript-nestjs → ("typescript", "5.7", "nestjs", "10.4")
  - rust-axum → ("rust", "1.83", "axum", "0.8")

**Fluxo:**
1. `load_config(path)` → lê YAML → detecta v2 → migra se necessário → valida → `ProjectConfig.from_dict()`
2. `detect_v2_format()` — verifica se `type` ou `stack` estão presentes no root
3. `migrate_v2_to_v3()` — transforma formato legado com `warnings.warn()`
4. `validate_config()` — verifica seções obrigatórias, lança `ConfigValidationError`

### 4. Template Engine (`template_engine.py`)

**Classe `TemplateEngine`:**
- Construtor: recebe `resources_dir` e `ProjectConfig`
- Usa `SandboxedEnvironment` do Jinja2 com `StrictUndefined`
- `FileSystemLoader` apontando para `resources_dir`
- Configuração: `autoescape=False`, `keep_trailing_newline=True`, `trim_blocks=False`, `lstrip_blocks=False`

**Métodos:**
- `render_template(template_path, context?)` — renderiza template Jinja2 de arquivo
- `render_string(template_str, context?)` — renderiza string inline como template
- `replace_placeholders(content, config?)` — substitui `{placeholder}` por valores do config (regex: `\{(\w+)\}`)
- `inject_section(base_content, section, marker)` — substitui marker por conteúdo (static)
- `concat_files(paths, separator="\n")` — lê e concatena arquivos (static)

**Context default (flat dict):** project_name, project_purpose, language_name, language_version, framework_name, framework_version, build_tool, architecture_style, domain_driven, event_driven, container, orchestrator, templating, iac, registry, api_gateway, service_mesh, database_name, cache_name, smoke_tests, contract_tests, performance_tests, coverage_line, coverage_branch

### 5. Utils (`utils.py`)

- `atomic_output(dest_dir)` — context manager: cria temp dir, executa, copia para dest atomicamente, limpa temp
- `_validate_dest_path()` — rejeita symlinks
- `_reject_dangerous_path()` — rejeita CWD, home, PROTECTED_PATHS ("/", "/tmp", "/var", "/etc", "/usr")
- `setup_logging(verbose)` — configura root logger
- `find_resources_dir()` — localiza `resources/` relativo ao pacote (`__file__/../../resources`)

### 6. Exceptions (`exceptions.py`)

- `ConfigValidationError(missing_fields: List[str])` — campos faltantes na config
- `PipelineError(assembler_name: str, reason: str)` — falha fatal no pipeline

### 7. Interactive Mode (`interactive.py`)

- `run_interactive()` — coleta inputs via `click.prompt` e `click.confirm`
- Choices hardcoded:
  - Architecture: library, microservice, monolith
  - Language: python, java, go, kotlin, typescript, rust
  - Interface: rest, grpc, cli, event-consumer, event-producer
  - Build tool: pip, maven, gradle, go, cargo, npm
  - Framework: dicionário por linguagem (python→[fastapi, click, django, flask], etc.)

### 8. Pipeline Orchestrator (`assembler/__init__.py`)

**Fluxo:**
1. `run_pipeline(config, resources_dir, output_dir, dry_run)` — orquestra tudo
2. Se `dry_run`: executa em temp dir, descarta, retorna resultado com warning
3. Se real: usa `atomic_output()` para escrita atômica
4. `_build_assemblers(resources_dir)` — cria lista ordenada de 14 assemblers
5. `_execute_assemblers()` — executa sequencialmente, coleta files e warnings
6. Assemblers com assinatura especial (recebem `resources_dir`): SkillsAssembler, AgentsAssembler
7. Mede `duration_ms` via `time.monotonic()`

**Ordem dos 14 Assemblers:**
1. RulesAssembler
2. SkillsAssembler
3. AgentsAssembler
4. PatternsAssembler
5. ProtocolsAssembler
6. HooksAssembler
7. SettingsAssembler
8. GithubInstructionsAssembler
9. GithubMcpAssembler
10. GithubSkillsAssembler
11. GithubAgentsAssembler
12. GithubHooksAssembler
13. GithubPromptsAssembler
14. ReadmeAssembler

### 9. Assemblers — Resumo de Cada

| Assembler | Output Dir | Lógica Principal |
| :--- | :--- | :--- |
| **RulesAssembler** | `rules/` | Copia core-rules com placeholders, roteia regras detalhadas para knowledge packs via `core_kp_routing`, copia language/framework knowledge packs, gera project-identity rule, consolida framework rules em 3 grupos |
| **SkillsAssembler** | `skills/` | Core skills (sempre), conditional skills (interface/infra/testing/security-based), knowledge packs (core + data), stack-specific patterns, infra patterns |
| **AgentsAssembler** | `agents/` | Core agents (6), conditional agents (database/observability/devops/api/event), developer agent (language-specific), injeta checklists condicionais via markers |
| **PatternsAssembler** | `skills/patterns/` | Seleciona patterns por architecture style, cria references/, consolida SKILL.md |
| **ProtocolsAssembler** | `skills/protocols/` | Deriva protocols do config, concatena arquivos, output em references/ |
| **HooksAssembler** | `hooks/` | Copia hook script baseado em language+build_tool, marca como executável |
| **SettingsAssembler** | `.` (root) | Coleta permissions de múltiplos JSONs (base + lang + infra + data + testing), deduplica, gera settings.json e settings.local.json |
| **GithubInstructionsAssembler** | `.github/` | Gera copilot-instructions.md com tabela de stack, renderiza templates contextuais |
| **GithubMcpAssembler** | `.github/` | Gera copilot-mcp.json para MCP servers, valida env vars usam $VARIABLE |
| **GithubSkillsAssembler** | `.github/skills/` | Copia skills por grupo (story/dev/review/testing/infra/kp/git), filtra infra skills por config |
| **GithubAgentsAssembler** | `.github/agents/` | Core agents, conditional (DevOps/API/Event), developer agent específico |
| **GithubHooksAssembler** | `.github/hooks/` | Copia templates de hooks (post-compile-check, pre-commit-lint, session-context-loader) |
| **GithubPromptsAssembler** | `.github/prompts/` | Renderiza templates Jinja2 (new-feature, decompose-spec, code-review, troubleshoot) |
| **ReadmeAssembler** | `.` (root) | Conta regras/skills/agents/kp, extrai numeração, gera tabelas de mapping, README completo |

### 10. Domain Layer

| Módulo | Responsabilidade |
| :--- | :--- |
| **resolver.py** | Resolve `ResolvedStack` a partir de `ProjectConfig` (commands, docker image, health path, port, protocols) |
| **validator.py** | Valida compatibilidade framework↔language, versões mínimas (Java 17+ para Quarkus/Spring, Python 3.10+ para Django 5.x), native build, interface types, architecture styles, cross-references |
| **skill_registry.py** | Define 11 core knowledge packs + regras condicionais para infra packs (k8s, dockerfile, container-registry, terraform, crossplane) |
| **stack_mapping.py** | Mapeamentos centrais: LANGUAGE_COMMANDS, FRAMEWORK_PORTS, FRAMEWORK_HEALTH_PATHS, FRAMEWORK_LANGUAGE_RULES, DOCKER_BASE_IMAGES, HOOK_TEMPLATE_MAP, SETTINGS_LANG_MAP, etc. |
| **stack_pack_mapping.py** | Framework → knowledge pack name (quarkus→quarkus-patterns, spring-boot→spring-patterns, etc.) |
| **pattern_mapping.py** | Architecture style → pattern categories (UNIVERSAL_PATTERNS + ARCHITECTURE_PATTERNS + EVENT_DRIVEN_PATTERNS) |
| **protocol_mapping.py** | Interface type → protocol directories, filtragem broker-specific para messaging |
| **version_resolver.py** | Resolve diretórios versionados com fallback (exact → major.x) |
| **core_kp_routing.py** | Roteia core-rules para knowledge packs com condições (CoreKpRoute, ConditionalCoreKpRoute) |
| **resolved_stack.py** | Frozen dataclass com valores computados do stack |

### 11. Helpers de Assembler

| Módulo | Função |
| :--- | :--- |
| **copy_helpers.py** | `copy_template_file()`, `copy_template_tree()`, `replace_placeholders_in_dir()` |
| **conditions.py** | `has_interface()`, `has_any_interface()`, `extract_interface_types()` |
| **consolidator.py** | `consolidate_files()`, `consolidate_framework_rules()` (3 grupos: core/data/ops) |
| **auditor.py** | `AuditResult` — verifica ≤10 rule files, ≤50KB total |

---

## Instruções para Decomposição

Use as skills **x-story-epic-full**, **x-story-epic** e **x-story-create** do projeto para gerar os deliverables. Essas skills estão em `resources/skills-templates/core/` e definem o processo completo de decomposição.

### Templates a Seguir

Os arquivos gerados devem seguir exatamente estes templates:
- **Épico:** `resources/templates/_TEMPLATE-EPIC.md`
- **Histórias:** `resources/templates/_TEMPLATE-STORY.md`
- **Mapa de Implementação:** `resources/templates/_TEMPLATE-IMPLEMENTATION-MAP.md`

### Filosofia de Decomposição

Seguir o `decomposition-guide.md` bundled com a skill `x-story-epic-full`:

- **Layer 0 (Foundation):** Setup do projeto Node/TS, configuração de build, estrutura de diretórios, infra de testes
- **Layer 1 (Core Domain):** Config loader + Models + Template Engine — o coração que todas as outras partes usam
- **Layer 2 (Extensions):** Cada assembler migrado individualmente, domain layer, interactive mode
- **Layer 3 (Compositions):** Pipeline orchestrator integrando todos os assemblers, CLI completa
- **Layer 4 (Cross-Cutting):** Testes de integração, verificação de compatibilidade de output, CI/CD, documentação

### Regras de Negócio Transversais (Candidatas a RULE-NNN)

Ao gerar o épico, extrair como regras transversais pelo menos:

1. **Compatibilidade de output** — Output gerado pelo Node/TS deve ser byte-for-byte idêntico ao Python para o mesmo input
2. **Migração v2→v3** — Lógica de detecção e migração de formato legado deve ser preservada exatamente
3. **Validação de paths** — Proteção contra symlinks, path traversal, protected paths deve ser idêntica
4. **Atomic output** — Escrita atômica via temp dir + copy deve ser preservada
5. **Placeholder replacement** — Regex `\{(\w+)\}` e contexto default devem produzir resultado idêntico
6. **Feature gating** — Toda lógica condicional de seleção de skills/agents/knowledge packs deve ser preservada
7. **Template engine config** — Nunjucks deve ser configurado para produzir output idêntico ao Jinja2 (autoescape=false, keep trailing newline, strict undefined)
8. **Assembler ordering** — Os 14 assemblers devem executar na mesma ordem
9. **Knowledge pack detection** — Lógica de leitura de SKILL.md para classificar knowledge packs deve ser idêntica
10. **Interactive mode choices** — Mesmos choices hardcoded, mesma ordem, mesmo comportamento

### Restrições Técnicas para as Histórias

Cada história deve especificar:

1. **Módulo Python de origem** — qual(is) arquivo(s) Python está migrando
2. **Módulo TypeScript de destino** — caminho proposto no novo projeto
3. **Dependências npm** — quais pacotes novos são necessários
4. **Testes de paridade** — como verificar que o output é idêntico ao Python
5. **Mapping de tipos** — como dataclasses Python mapeiam para interfaces/classes TS

### Estrutura Proposta do Projeto Node/TS

```
ia-dev-environment/
├── src/
│   ├── index.ts                    # Entry point
│   ├── cli.ts                      # Commander/Yargs CLI
│   ├── config.ts                   # Config loader + v2→v3 migration
│   ├── models.ts                   # Interfaces e classes TypeScript
│   ├── template-engine.ts          # Nunjucks wrapper
│   ├── utils.ts                    # Atomic output, path validation, logging
│   ├── exceptions.ts               # Custom error classes
│   ├── interactive.ts              # Interactive prompts (inquirer)
│   ├── assembler/
│   │   ├── index.ts                # Pipeline orchestrator
│   │   ├── rules-assembler.ts
│   │   ├── skills-assembler.ts
│   │   ├── agents-assembler.ts
│   │   ├── patterns-assembler.ts
│   │   ├── protocols-assembler.ts
│   │   ├── hooks-assembler.ts
│   │   ├── settings-assembler.ts
│   │   ├── github-instructions-assembler.ts
│   │   ├── github-mcp-assembler.ts
│   │   ├── github-skills-assembler.ts
│   │   ├── github-agents-assembler.ts
│   │   ├── github-hooks-assembler.ts
│   │   ├── github-prompts-assembler.ts
│   │   ├── readme-assembler.ts
│   │   ├── copy-helpers.ts
│   │   ├── conditions.ts
│   │   ├── consolidator.ts
│   │   └── auditor.ts
│   └── domain/
│       ├── resolver.ts
│       ├── validator.ts
│       ├── skill-registry.ts
│       ├── stack-mapping.ts
│       ├── stack-pack-mapping.ts
│       ├── pattern-mapping.ts
│       ├── protocol-mapping.ts
│       ├── version-resolver.ts
│       ├── core-kp-routing.ts
│       └── resolved-stack.ts
├── tests/
│   ├── (espelha estrutura de src/)
│   └── fixtures/
├── resources/                       # INALTERADO — copiado do Python
├── package.json
├── tsconfig.json
├── tsup.config.ts                   # Build config
├── vitest.config.ts                 # Test config
└── README.md
```

### Dependências npm Propostas

**Runtime:**
- `commander` (ou `yargs`) — CLI framework
- `js-yaml` — YAML parsing
- `nunjucks` — Template engine (compatível Jinja2)
- `inquirer` (ou `prompts`) — Interactive mode

**Dev:**
- `typescript` — Compilador
- `tsup` — Bundler
- `vitest` — Test runner + coverage
- `@types/node` — Type definitions
- `@types/js-yaml` — Type definitions
- `@types/nunjucks` — Type definitions
- `tsx` — Dev runner

---

## Output Esperado

Gerar dentro de `stories/migration-python-to-node-ts/`:

1. **`EPIC-001.md`** — Épico completo seguindo `_TEMPLATE-EPIC.md`
2. **`STORY-001.md` ... `STORY-NNN.md`** — Uma história por story seguindo `_TEMPLATE-STORY.md`
3. **`IMPLEMENTATION-MAP.md`** — Mapa de implementação seguindo `_TEMPLATE-IMPLEMENTATION-MAP.md`

### Critérios de Qualidade

- [ ] Toda regra no épico é referenciada por pelo menos uma história
- [ ] Toda história referencia pelo menos uma regra (exceto infraestrutura)
- [ ] Dependências são simétricas (A blocks B ↔ B blocked by A)
- [ ] Sem dependências circulares
- [ ] Computação de fases correta (stories só entram em fase quando TODAS deps estão em fases anteriores)
- [ ] Caminho crítico é a cadeia mais longa real
- [ ] Contratos de dados precisos (nomes de módulos, interfaces, tipos)
- [ ] Cada história tem pelo menos 4 cenários Gherkin
- [ ] Observações do mapa de implementação são específicas, não genéricas
- [ ] Todos os arquivos seguem seus templates exatamente

### Regras de Idioma

- Todo conteúdo gerado em **Português Brasileiro (pt-BR)**
- Termos técnicos em inglês permanecem em inglês (cache, timeout, handler, endpoint, assembler, pipeline, template engine, etc.)
- Identificadores de código, nomes de campo, valores enum permanecem em inglês
- Gherkin em português: `Cenario`, `DADO`, `QUANDO`, `ENTÃO`, `E`, `MAS`
- IDs em formato inglês: RULE-NNN, STORY-NNN, EPIC-NNN
