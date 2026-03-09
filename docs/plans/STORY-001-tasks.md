# Decomposição de Tarefas — STORY-001: Setup do Projeto Node.js + TypeScript

**Status:** PLANEJADO
**Escopo:** Fundação Node/TS (sem migração funcional de módulos)
**Regra mandatória:** RULE-011 — `resources/` inalterado

## G1 — Bootstrap de configuração
- Criar `package.json` com metadata, `bin`, scripts e dependências da história.
- Criar `tsconfig.json`, `tsup.config.ts` e `vitest.config.ts`.
- Critério: `npm install` e `npm run build` executam sem erro de configuração.

## G2 — Estrutura de diretórios e stubs
- Criar estrutura: `src/`, `src/assembler/`, `src/domain/`, `tests/fixtures/`.
- Criar stubs: `src/index.ts`, `src/cli.ts`, `src/config.ts`, `src/models.ts`, `src/template-engine.ts`, `src/utils.ts`, `src/exceptions.ts`, `src/interactive.ts`, `src/assembler/index.ts`, `src/domain/index.ts`.
- Critério: TypeScript compila e `dist/index.js` é gerado.

## G3 — CLI mínimo funcional
- Implementar entrypoint com shebang `#!/usr/bin/env node`.
- Implementar comando base com `--help` via Commander.
- Critério: `npx ia-dev-env --help` exibe ajuda sem erro.

## G4 — Testes de fundação
- Criar teste de smoke para comando de ajuda.
- Configurar coverage v8 (`text` e `lcov`) com thresholds 95/90.
- Critério: `npm run test` e `npm run test:coverage` executam.

## G5 — Integração e qualidade
- Garantir `.gitignore` com `node_modules`, `dist`, `coverage`, `*.tsbuildinfo`.
- Validar que `resources/` não sofreu alterações.
- Critério: `git diff --name-only -- resources/` retorna vazio.

## G6 — Documentação mínima
- Atualizar README com instruções de setup Node/TS da fundação.
- Critério: README descreve build/test/CLI sem remover fluxo Python existente.

## G7 — Verificação final da story
- Executar checklist DoD local da story.
- Registrar evidências de build/test/help/coverage.

## Dependências (DAG)
G1 -> G2 -> G3 -> G4 -> G5 -> G6 -> G7

## Caminho crítico
G1 -> G2 -> G3 -> G4 -> G5 -> G6 -> G7
