# Plano de Testes — STORY-001: Setup do Projeto Node.js + TypeScript

**Status:** PLANEJADO  
**História:** STORY-001  
**Escopo:** Fundação Node/TypeScript (build, testes, CLI stub e estrutura inicial)

---

## 1) Contexto, Escopo e Premissas

Este plano cobre apenas a fundação técnica descrita na STORY-001: `package.json`, `tsconfig.json`, `tsup.config.ts`, `vitest.config.ts`, estrutura de diretórios e entrypoint CLI básico.

Premissas de planejamento:
- Não alterar `resources/` (RULE-011).
- Não escrever testes neste documento; apenas cenários.
- Seguir convenção de nome de teste: `[methodUnderTest]_[scenario]_[expectedBehavior]`.
- Meta global de cobertura: **line >= 95%** e **branch >= 90%**.

---

## 2) Matriz de Aplicabilidade por Categoria

| Categoria | Aplicável? | Justificativa |
|---|---|---|
| Unit | Sim | Necessário para validar bootstrap do CLI e contratos de configuração exportados por módulos TypeScript. |
| Integration | Sim | STORY-001 exige validação de scripts npm, build, coverage e estrutura gerada no filesystem. |
| API | Não | O escopo é CLI com Commander; não há endpoints REST/gRPC nesta história. |
| E2E | Sim | Há fluxo completo explícito nos critérios Gherkin (install -> build -> help -> test/coverage). |
| Contract | Sim | A história define contratos de dados para `package.json` e `tsconfig.json`, além de regras fixas de `vitest`/`tsup`. |
| Performance | Sim | DoD local/global define limites de tempo (build < 10s, testes < 30s). |

---

## 3) Cenários Planejados por Categoria

## 3.1 Unit

| ID | Nome do cenário (convenção obrigatória) | Módulo alvo | Objetivo |
|---|---|---|---|
| U-01 | `buildCliProgram_defaultCommand_registersHelpAndVersion` | `src/cli.ts` | Garantir comando raiz com `--help` e `--version` configurados. |
| U-02 | `runCli_withoutArgs_displaysHelp` | `src/index.ts` | Validar comportamento padrão do stub ao executar sem argumentos. |
| U-03 | `loadVersion_fromPackageJson_returnsSemver` | `src/config.ts` | Garantir leitura/propagação correta de versão do projeto. |
| U-04 | `createVitestConfig_withDefaults_enforcesCoverageThresholds` | `vitest.config.ts` | Confirmar thresholds `lines:95` e `branches:90`. |
| U-05 | `createTsupConfig_forCliEntry_generatesEsmBundleWithShebang` | `tsup.config.ts` | Validar formato ESM, target Node18+ e shebang no entrypoint. |
| U-06 | `resolveProjectPaths_whenInitialized_returnsExpectedFoundationDirs` | `src/config.ts` | Garantir resolução consistente de `src/`, `dist/`, `tests/fixtures/`. |

## 3.2 Integration

| ID | Nome do cenário (convenção obrigatória) | Escopo integrado | Objetivo |
|---|---|---|---|
| I-01 | `npmInstall_withFoundationDependencies_completesWithoutErrors` | npm + lock/deps | Validar instalação inicial sem falhas. |
| I-02 | `npmRunBuild_withBaseSetup_generatesDistIndex` | TS + tsup | Garantir geração de `dist/index.js`. |
| I-03 | `npmRunBuild_withCliEntry_preservesNodeShebang` | tsup + artefato final | Confirmar `#!/usr/bin/env node` no artefato. |
| I-04 | `npmRunTest_withVitestConfigured_executesWithoutConfigErrors` | vitest runner | Validar execução mesmo sem suites implementadas. |
| I-05 | `npmRunCoverage_withV8Provider_generatesLcovAndTextReports` | vitest coverage | Confirmar geração dos formatos exigidos. |
| I-06 | `projectScaffold_afterInitialization_containsRequiredDirectories` | filesystem | Verificar `src/assembler/`, `src/domain/` e `tests/fixtures/`. |
| I-07 | `resourceTree_beforeAndAfterFoundation_remainsUnchanged` | regra transversal | Garantir que `resources/` permaneça inalterado. |

## 3.3 API (não aplicável)

Não aplicável na STORY-001, pois não há superfície HTTP/gRPC/WebSocket nem contrato de endpoint.

## 3.4 E2E

| ID | Nome do cenário (convenção obrigatória) | Fluxo ponta a ponta | Objetivo |
|---|---|---|---|
| E2E-01 | `bootstrapFlow_installBuildAndHelp_completesSuccessfully` | `npm install -> npm run build -> npx ia-dev-env --help` | Validar fluxo mínimo utilizável do produto. |
| E2E-02 | `bootstrapFlow_buildAndTestCoverage_producesExpectedArtifacts` | `npm run build -> npm run test -> npm run test:coverage` | Confirmar artefatos finais (`dist/`, relatório coverage). |
| E2E-03 | `bootstrapFlow_cleanWorkspace_generatesOnlyExpectedFiles` | execução em diretório limpo | Garantir ausência de artefatos inesperados. |

## 3.5 Contract

| ID | Nome do cenário (convenção obrigatória) | Contrato validado | Objetivo |
|---|---|---|---|
| C-01 | `validatePackageJson_requiredFields_matchStoryContract` | `package.json` | Validar `name`, `version`, `bin.ia-dev-env`, `type`, scripts mínimos. |
| C-02 | `validatePackageJson_dependencies_matchApprovedStack` | `package.json` | Garantir dependências runtime/dev definidas na história. |
| C-03 | `validateTsconfig_compilerOptions_matchStoryContract` | `tsconfig.json` | Validar `target`, `module`, `strict`, `outDir`, `rootDir`, include/exclude. |
| C-04 | `validateVitestConfig_coverageRules_matchStoryThresholds` | `vitest.config.ts` | Confirmar provider `v8`, include e excludes esperados. |
| C-05 | `validateTsupConfig_buildRules_matchStoryContract` | `tsup.config.ts` | Confirmar entry, bundle, ESM, target Node18+, dts. |

## 3.6 Performance

| ID | Nome do cenário (convenção obrigatória) | Meta | Objetivo |
|---|---|---|---|
| P-01 | `npmRunBuild_onBaselineMachine_completesUnderTenSeconds` | `< 10s` | Validar requisito de performance local da história. |
| P-02 | `npmRunTest_onBaselineMachine_completesUnderThirtySeconds` | `< 30s` | Confirmar test runner funcional dentro da meta. |
| P-03 | `cliHelp_afterBuild_returnsWithinFiveHundredMilliseconds` | `< 500ms` | Garantir responsividade mínima do comando de ajuda. |
| P-04 | `testCoverage_onBaselineMachine_staysWithinAcceptableRuntime` | limite acordado em CI | Evitar regressão de tempo do pipeline de cobertura. |

---

## 4) Estimativa de Cobertura por Módulo/Classe Planejada

| Módulo/Classe planejada | Métodos públicos (estimado) | Ramos (estimado) | Nº de testes planejados | Cobertura de linha (estimada) | Cobertura de branch (estimada) | Status |
|---|---:|---:|---:|---:|---:|---|
| `src/index.ts` | 1 | 2 | 3 | 100% | 100% | ✅ |
| `src/cli.ts` | 2 | 6 | 6 | 96% | 92% | ✅ |
| `src/config.ts` | 2 | 5 | 5 | 95% | 90% | ✅ |
| `tsup.config.ts` | 1 | 2 | 3 | 100% | 100% | ✅ |
| `vitest.config.ts` | 1 | 2 | 3 | 100% | 100% | ✅ |
| `src/interactive.ts` (stub) | 1 | 4 | 1 | 82% | 65% | ⚠️ abaixo do alvo |
| `src/template-engine.ts` (stub) | 1 | 5 | 1 | 78% | 55% | ⚠️ abaixo do alvo |
| `src/models.ts` (stub) | 0–1 | 2 | 0–1 | 70% | 50% | ⚠️ abaixo do alvo |
| `src/utils.ts` (stub) | 1 | 3 | 1 | 84% | 68% | ⚠️ abaixo do alvo |
| `src/exceptions.ts` (stub) | 1 | 2 | 1 | 88% | 70% | ⚠️ abaixo do alvo |

### Módulos sinalizados abaixo do threshold (obrigatório)

- `src/interactive.ts`
- `src/template-engine.ts`
- `src/models.ts`
- `src/utils.ts`
- `src/exceptions.ts`

**Ação de mitigação planejada:** manter stubs realmente mínimos (sem ramificações desnecessárias) e adicionar cenários unitários de carga/import/export para elevar cobertura sem expandir escopo funcional da STORY-001.

---

## 5) Riscos Principais e Estratégia de Mitigação

1. **Risco de cobertura global insuficiente** por excesso de arquivos stub com baixa exercitação.  
   **Mitigação:** reduzir lógica nos stubs e cobrir contratos de exportação/importação.

2. **Risco de inconsistência de ambiente (Node/npm)** entre local e CI.  
   **Mitigação:** fixar versão mínima Node 18+ e validar em pipeline.

3. **Risco de regressão de performance** em build/test mesmo sem lógica de domínio.  
   **Mitigação:** cenários P-01/P-02 como gate obrigatório antes de avançar para stories dependentes.

---

## 6) Sequência Recomendada de Execução (quando implementar os testes)

1. Unit (U-01..U-06)  
2. Contract (C-01..C-05)  
3. Integration (I-01..I-07)  
4. E2E (E2E-01..E2E-03)  
5. Performance (P-01..P-04)

Essa ordem reduz retrabalho: primeiro valida contratos locais, depois valida pipeline completo.
