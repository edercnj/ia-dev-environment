# Spec: Security Program — Expansao de Skills Executaveis e Agentes de Seguranca

> **Instrucao de uso**: Execute `/x-story-epic-full specs/SPEC-security-program-v1.md` dentro do
> repositorio `ia-dev-environment`. A skill produz automaticamente o epic, stories individuais e
> implementation map no diretorio `plans/epic-XXXX/`.

---

## Sistema

**Projeto**: `ia-dev-environment` — CLI generator de ambientes de desenvolvimento assistidos por IA.

**Versao base analisada**: `v2.0.0-SNAPSHOT` (branch `main`).

**Objetivo desta especificacao**: Expandir as capacidades de seguranca do gerador de
**review-oriented** (checklists e knowledge packs de referencia) para **execution-oriented**
(skills que executam scans automatizados, integracoes com quality gates, framework de pentest
multi-ambiente, avaliacao de hardening, e personas de seguranca especializadas).

**Principio central de todas as historias**: Seguranca efetiva requer automacao executavel, nao
apenas documentacao. Toda skill de seguranca DEVE produzir output acionavel (findings com
severidade, remediacoes concretas, scores normalizados) e integrar-se ao pipeline CI/CD existente.

---

## Escopo do Epico

### Contexto de negocio

O gerador atual produz um ecossistema de seguranca funcional mas predominantemente **passivo**:
- `security-engineer.md` (agent) faz review com checklist de 20 pontos
- `x-dependency-audit` (skill) analisa vulnerabilidades e licencas de dependencias
- `x-threat-model` (skill) gera modelos de ameaca STRIDE/PASTA/LINDDUN
- `x-codebase-audit` (skill) tem dimensao de seguranca no audit
- `x-review` (skill) inclui security engineer com 10 items no review paralelo
- `security/SKILL.md` (KP) documenta OWASP Top 10, supply chain, SBOM
- `compliance/SKILL.md` (KP) documenta GDPR, HIPAA, LGPD, PCI-DSS, SOX

**Gaps identificados** que motivam este epico:

1. **Ausencia de scanning executavel** — Nao ha skills que executem SAST, DAST, secret scanning,
   container scanning ou infrastructure scanning automaticamente. O agente revisa codigo mas nao
   roda ferramentas.

2. **Sem quality gate de seguranca** — Nao ha integracao com SonarQube/SonarLint para tracking
   de security hotspots e quality gate enforcement.

3. **Sem framework de pentest** — Nao existe orquestrador de penetration testing com suporte a
   multiplos ambientes (local, dev, homologacao, producao) e restricoes de seguranca por ambiente.

4. **Personas insuficientes** — Apenas o `security-engineer` existe. Faltam personas para:
   pentest (ofensivo), application security (SDLC), DevSecOps (pipeline), compliance (auditoria).

5. **Sem avaliacao de hardening** — Nao ha skill para avaliar configuracoes de seguranca da
   aplicacao (headers HTTP, TLS, CORS, rate limiting, session management).

6. **Supply chain superficial** — `x-dependency-audit` cobre vulnerabilidades e licencas mas nao
   analisa risco de mantenedor, typosquatting, phantom dependencies ou age analysis.

7. **KP references incompletos** — O security KP referencia `application-security.md`,
   `cryptography.md` e `pentest-readiness.md` mas estes arquivos nao existem.

8. **Sem pipeline de seguranca integrado** — Nao existe gerador de pipeline CI/CD com stages
   de seguranca (secret scan pre-commit, SAST no build, DAST em staging, etc.).

### Dimensoes de melhoria

1. **Scanning executavel** — SAST, DAST, secret scan, container scan, infra scan, OWASP Top 10
2. **Quality gate** — SonarQube integration com security hotspot tracking
3. **Pentest framework** — Orquestrador multi-fase com parametro de ambiente
4. **Personas especializadas** — Pentest engineer, AppSec engineer, DevSecOps engineer, Compliance auditor
5. **Hardening assessment** — HTTP headers, TLS, CORS, rate limiting, CIS benchmarks
6. **Supply chain profunda** — Maintainer risk, typosquatting, phantom deps
7. **KP completion** — References ausentes no security knowledge pack
8. **CI pipeline** — Gerador de pipeline de seguranca multi-CI (GH Actions, GitLab CI, Azure DevOps)
9. **Security dashboard** — Visao consolidada de postura de seguranca
10. **Anti-patterns de seguranca** — Rule condicional por language/framework

---

## Regras de Negocio Transversais (Cross-Cutting Rules)

**RULE-001**: Toda skill de seguranca executavel (x-sast-scan, x-secret-scan, x-dast-scan, etc.)
DEVE ser idempotente — executar duas vezes produz o mesmo output sem efeitos colaterais. Resultados
sao escritos em `results/security/` com filenames datados (e.g., `sast-scan-2026-04-05.md`).

**RULE-002**: Skills de scanning NAO DEVEM hardcodar uma unica ferramenta. Cada skill define uma
**tabela de selecao de ferramentas** com colunas `build-tool | primary tool | alternative`. A skill
tenta a ferramenta primaria e faz fallback para a alternativa. O usuario nunca precisa configurar
qual ferramenta usar. Quando nenhuma ferramenta esta disponivel, a skill reporta um finding de
severidade INFO indicando que o scan nao pode ser executado e lista os comandos de instalacao.

**RULE-003**: Todas as skills de scanning DEVEM produzir findings em **SARIF** (Static Analysis
Results Interchange Format) como formato intermediario para integracao CI. O report Markdown
legivel por humanos e gerado a partir do SARIF. O SARIF permite upload direto para GitHub
Advanced Security, GitLab SAST, e SonarQube.

**RULE-004**: Skills que interagem com sistemas em execucao (DAST, pentest) DEVEM aceitar parametro
`--env local|dev|homolog|prod`. Default e `local`. Producao requer flag explicita `--confirm-prod`.
Em producao: APENAS scans passivos (sem testes de injecao, sem fuzzing, sem exploitation). Em
homologacao: scans ativos permitidos mas sem testes destrutivos. Em dev/local: todos os testes
permitidos.

**RULE-005**: Todas as avaliacoes de seguranca produzem um **score normalizado 0-100**. Formula:
iniciar em 100 e subtrair deducoes ponderadas por severidade — CRITICAL: -15, HIGH: -8, MEDIUM: -3,
LOW: -1. Score cap em 0 (nunca negativo). Isso permite tracking de tendencia entre auditorias.

**RULE-006**: Agentes de seguranca (personas) TEM escopos nao-sobrepostos. Cada agent declara
`## Scope` com inclusoes e exclusoes explicitas para prevenir findings duplicados:
- `security-engineer` (existente): code review, checklist de seguranca no codigo
- `pentest-engineer` (novo): exploitation, attack chains, PoC
- `appsec-engineer` (novo): secure SDLC, security architecture, testing strategy
- `devsecops-engineer` (novo): pipeline security, artifact signing, SLSA
- `compliance-auditor` (novo): regulatory evidence, gap analysis, audit prep

**RULE-007**: Toda nova skill de seguranca DEVE referenciar secoes apropriadas do security KP
(`skills/security/SKILL.md`) e compliance KP (`skills/compliance/SKILL.md`) para orientacao de
remediacao. Skills produzem findings; KPs fornecem as correcoes.

**RULE-008**: Classificacao de severidade usa CVSS 4.0 base score mapping: CRITICAL >= 9.0,
HIGH >= 7.0, MEDIUM >= 4.0, LOW < 4.0. Quando CVSS nao esta disponivel, usar matriz
impacto x probabilidade do x-threat-model.

**RULE-009**: Toda skill de scanning DEVE incluir secao `## CI Integration` com snippets YAML
para GitHub Actions, GitLab CI, e Azure DevOps mostrando como executar a skill como stage do
pipeline. O snippet deve ser copy-paste ready.

**RULE-010**: Novas skills de seguranca seguem o padrao existente de `SkillsSelection`. Skills
condicionais sao incluidas quando flags especificas do `SecurityConfig` estao ativas
(e.g., `security.scanning.sast: true`). A classe `SkillsSelection.selectSecurityScanningSkills()`
avalia os novos flags.

**RULE-011**: Skills orquestradoras (x-pentest, x-security-dashboard, x-security-pipeline) invocam
skills atomicas via delegacao de subagent. NUNCA duplicam logica de scanning. Se o x-pentest
precisa de SAST, ele invoca x-sast-scan.

**RULE-012**: Novos agentes de seguranca seguem exatamente o formato de `security-engineer.md`:
persona, role, recommended model, responsibilities, checklist numerado, output format, regras de
conduta.

**RULE-013**: Cada verificacao OWASP Top 10 mapeia para um nivel OWASP ASVS (Application Security
Verification Standard) — L1 (minimo), L2 (padrao), L3 (avancado). Skills referenciam qual nivel
ASVS cobrem.

**RULE-014**: Componentes de seguranca existentes (`security-engineer.md`, `06-security-baseline.md`,
`security/SKILL.md`, `x-dependency-audit`, `x-threat-model`) permanecem inalterados. Novas skills
e agents ESTENDEM, nao substituem.

**RULE-015**: Todos os novos skill templates usam sintaxe `{{PLACEHOLDER}}` compativel com o
`TemplateEngine` existente (Mustache-style). Placeholders obrigatorios: `{{LANGUAGE}}`,
`{{FRAMEWORK}}`, `{{BUILD_TOOL}}`, `{{PROJECT_NAME}}`.

---

## Historias

---

### STORY-0001: Security Config Model Extension

**Titulo**: Extensao do SecurityConfig com flags de scanning, quality gate e pentest

**Tipo**: Feature — Domain Model + Assembler

**Prioridade**: Alta (foundation para todas as demais stories)

**Dependencias**: Nenhuma. Ponto de entrada do epico.

**Contexto tecnico**:
O `SecurityConfig.java` atual e um record com apenas um campo `frameworks` (List<String>) para
compliance frameworks. Para gerar condicionalmente as novas skills de scanning, quality gate e
pentest, o model precisa de flags adicionais que o `SkillsSelection` possa avaliar.

A classe `SkillsSelection.selectSecuritySkills()` atualmente retorna apenas `x-review-security`
quando `frameworks` e nao-vazio. Precisa ser estendida para avaliar flags de scanning individuais.

**Escopo de implementacao**:

1. Criar sub-record `ScanningConfig` com campos:
   - `sast` (boolean, default false)
   - `dast` (boolean, default false)
   - `secretScan` (boolean, default false)
   - `containerScan` (boolean, default false)
   - `infraScan` (boolean, default false)

2. Criar sub-record `QualityGateConfig` com campos:
   - `provider` (String, default "none", aceita "sonarqube", "sonarcloud")
   - `serverUrl` (String, optional)
   - `qualityGate` (String, default "default", aceita "default", "strict")

3. Adicionar campos ao `SecurityConfig`:
   - `scanning` (ScanningConfig, default all false)
   - `qualityGate` (QualityGateConfig, default provider="none")
   - `pentest` (boolean, default false)
   - `pentestDefaultEnv` (String, default "local", aceita "local", "dev", "homolog")

4. Estender `SecurityConfig.fromMap()` usando `MapHelper.optionalMap()` para backward compatibility.

5. Adicionar `SkillsSelection.selectSecurityScanningSkills()` que avalia os novos flags.

6. Integrar no `selectConditionalSkills()` existente.

**Data Contract — config YAML input**:

| Campo | Tipo | M/O | Validacoes | Default |
|-------|------|-----|-----------|---------|
| `security.scanning.sast` | boolean | O | — | false |
| `security.scanning.dast` | boolean | O | — | false |
| `security.scanning.secretScan` | boolean | O | — | false |
| `security.scanning.containerScan` | boolean | O | — | false |
| `security.scanning.infraScan` | boolean | O | — | false |
| `security.qualityGate.provider` | String | O | none, sonarqube, sonarcloud | none |
| `security.qualityGate.serverUrl` | String | O | URL valida se provider != none | — |
| `security.qualityGate.qualityGate` | String | O | default, strict | default |
| `security.pentest` | boolean | O | — | false |
| `security.pentestDefaultEnv` | String | O | local, dev, homolog | local |

**Criterios de Aceitacao (DoD)**:

- [ ] `SecurityConfig.java` estendido com `ScanningConfig`, `QualityGateConfig`, `pentest`, `pentestDefaultEnv`
- [ ] `ScanningConfig.java` e `QualityGateConfig.java` criados como records imutaveis
- [ ] `SecurityConfig.fromMap()` parseia novos campos com backward compatibility (YAML antigo sem novos campos funciona)
- [ ] `SkillsSelection.selectSecurityScanningSkills()` retorna skills baseado nos flags
- [ ] `selectConditionalSkills()` integra `selectSecurityScanningSkills()`
- [ ] Testes unitarios cobrem: all-false (nada retornado), cada flag individual, combinacoes
- [ ] Line coverage >= 95%, branch coverage >= 90%
- [ ] Golden file parity tests passam (backward compatibility)

**Gherkin**:

```gherkin
Feature: Security Config Model Extension

  Cenario: Config YAML sem campos de scanning preserva backward compatibility
    DADO um config YAML com apenas "security.compliance: [pci-dss]"
    QUANDO o SecurityConfig.fromMap() e invocado
    ENTAO scanning.sast e false
    E scanning.dast e false
    E pentest e false
    E qualityGate.provider e "none"

  Cenario: Config com SAST habilitado gera skill x-sast-scan
    DADO um config YAML com "security.scanning.sast: true"
    QUANDO selectSecurityScanningSkills() e invocado
    ENTAO a lista retornada contem "x-sast-scan"
    E a lista NAO contem "x-dast-scan"

  Cenario: Config com todos os scanning flags habilitados gera todas as skills
    DADO um config YAML com todos os campos de scanning como true
    QUANDO selectSecurityScanningSkills() e invocado
    ENTAO a lista contem "x-sast-scan", "x-dast-scan", "x-secret-scan", "x-container-scan", "x-infra-scan"

  Cenario: Config com pentest habilitado gera skill x-pentest
    DADO um config YAML com "security.pentest: true"
    QUANDO selectSecurityScanningSkills() e invocado
    ENTAO a lista contem "x-pentest"

  Cenario: Config com qualityGate sonarqube gera skill x-sonar-gate
    DADO um config YAML com "security.qualityGate.provider: sonarqube"
    QUANDO selectSecurityScanningSkills() e invocado
    ENTAO a lista contem "x-sonar-gate"
```

---

### STORY-0002: Security Report Infrastructure

**Titulo**: Infraestrutura de relatorios de seguranca com SARIF template e scoring normalizado

**Tipo**: Feature — Knowledge Pack Reference

**Prioridade**: Alta (foundation para todas as skills de scanning)

**Dependencias**: Nenhuma. Pode ser implementada em paralelo com STORY-0001.

**Contexto tecnico**:
Todas as novas skills de scanning produzem output. Para consistencia, precisam de um formato padrao
(SARIF para CI) e um modelo de scoring normalizado (0-100) que permita comparacao entre auditorias.
Estes templates sao references do security KP que as skills referenciam.

**Escopo de implementacao**:

1. Criar `security/references/sarif-template.md` com:
   - Estrutura SARIF 2.1.0 minima (tool, runs, results, rules)
   - Mapeamento de severidade para SARIF level (error, warning, note)
   - Exemplo completo por linguagem
   - Instrucoes de upload para GH Advanced Security, GitLab SAST, SonarQube

2. Criar `security/references/security-scoring.md` com:
   - Formula de scoring: start 100, deductions per severity
   - Tabela de deducoes: CRITICAL -15, HIGH -8, MEDIUM -3, LOW -1
   - Exemplo de calculo com findings reais
   - Definicao de thresholds: A (90-100), B (75-89), C (60-74), D (40-59), F (0-39)
   - Template de trend tracking (comparacao entre auditorias)

3. Definir convencao de diretorio `results/security/` com naming:
   - `{skill-name}-{YYYY-MM-DD}.md` para reports Markdown
   - `{skill-name}-{YYYY-MM-DD}.sarif.json` para SARIF

**Criterios de Aceitacao (DoD)**:

- [ ] `security/references/sarif-template.md` criado com template SARIF 2.1.0 completo
- [ ] `security/references/security-scoring.md` criado com formula e thresholds
- [ ] Convencao `results/security/` documentada com naming pattern
- [ ] SARIF template valida contra schema SARIF 2.1.0 oficial
- [ ] Exemplo de scoring com pelo menos 3 cenarios (clean, moderate, critical)
- [ ] References registrados no `security/SKILL.md` principal

**Gherkin**:

```gherkin
Feature: Security Report Infrastructure

  Cenario: SARIF template contem estrutura minima valida
    DADO o arquivo security/references/sarif-template.md gerado
    QUANDO o template JSON e extraido
    ENTAO o JSON contem campo "$schema" com URL do SARIF 2.1.0 schema
    E o JSON contem campo "runs" com array nao-vazio
    E cada run contem "tool" e "results"

  Cenario: Scoring formula calcula score correto para findings mistos
    DADO 2 findings CRITICAL, 3 HIGH e 5 MEDIUM
    QUANDO a formula de scoring e aplicada
    ENTAO o score e 100 - (2*15) - (3*8) - (5*3) = 100 - 30 - 24 - 15 = 31
    E o grade e "F"

  Cenario: Score nunca fica negativo
    DADO 10 findings CRITICAL
    QUANDO a formula de scoring e aplicada
    ENTAO o score e 0
    E o grade e "F"

  Cenario: Zero findings resulta em score perfeito
    DADO 0 findings de qualquer severidade
    QUANDO a formula de scoring e aplicada
    ENTAO o score e 100
    E o grade e "A"
```

---

### STORY-0003: Security Skill Template e CI Integration Pattern

**Titulo**: Template base para skills executaveis de seguranca com CI integration snippets

**Tipo**: Feature — Knowledge Pack Reference

**Prioridade**: Alta (padrao que todas as skills de scanning seguem)

**Dependencias**: STORY-0001 (precisa dos flags de config para documentar a gating pattern)

**Contexto tecnico**:
Para manter consistencia entre as 6+ skills de scanning, precisamos de um template de referencia
que defina: estrutura do SKILL.md, secao de tool-selection-table, secao de CI integration, formato
de output, e error handling para tool-not-found.

**Escopo de implementacao**:

1. Criar `security/references/security-skill-template.md` com:
   - Estrutura padrao do SKILL.md para skills executaveis
   - Template da tool-selection-table (build-tool | primary | alternative | install cmd)
   - Template da secao `## CI Integration` com GH Actions, GitLab CI, Azure DevOps YAML
   - Template da secao `## Output Format` (Markdown report + SARIF)
   - Template da secao `## Error Handling` (tool not found, permission denied, timeout)
   - Template da secao `## Parameters` com formato de documentacao de parametros

2. Cada skill de scanning DEVE seguir este template como base e adicionar suas secoes especificas.

**Criterios de Aceitacao (DoD)**:

- [ ] `security/references/security-skill-template.md` criado com todos os templates
- [ ] Template da tool-selection-table testado com pelo menos 3 build tools
- [ ] CI snippets para GH Actions, GitLab CI e Azure DevOps sao validos YAML
- [ ] Template de error handling cobre: tool not found, permission, timeout, network
- [ ] Reference registrado no `security/SKILL.md` principal

**Gherkin**:

```gherkin
Feature: Security Skill Template

  Cenario: Template contem tool-selection-table com primary e alternative
    DADO o arquivo security/references/security-skill-template.md gerado
    QUANDO a secao "Tool Selection Table" e lida
    ENTAO a tabela contem colunas "Build Tool", "Primary", "Alternative", "Install"
    E ao menos 3 combinacoes de build tool estao presentes

  Cenario: CI snippet GitHub Actions e YAML valido
    DADO o snippet GitHub Actions do template
    QUANDO parseado como YAML
    ENTAO o YAML e valido
    E contem campo "jobs" com ao menos um step

  Cenario: Error handling cobre tool-not-found
    DADO a secao "Error Handling" do template
    QUANDO a secao e lida
    ENTAO contem instrucoes para cenario "tool not found"
    E inclui comandos de instalacao da ferramenta
```

---

### STORY-0004: OWASP ASVS Reference Knowledge Pack

**Titulo**: Knowledge pack de referencia OWASP ASVS com cross-references a OWASP Top 10 e CIS

**Tipo**: Feature — Knowledge Pack

**Prioridade**: Alta (base para x-owasp-scan e hardening skills)

**Dependencias**: Nenhuma. Pode ser implementada em paralelo com STORY-0001.

**Contexto tecnico**:
O OWASP Application Security Verification Standard (ASVS) e o framework mais completo para
verificacao de seguranca de aplicacoes. Define 3 niveis de verificacao (L1 baseline, L2 standard,
L3 advanced) com requisitos especificos por categoria. As skills de scanning referenciam ASVS
levels para indicar profundidade de verificacao.

**Escopo de implementacao**:

1. Criar `knowledge-packs/owasp-asvs/SKILL.md` com:
   - Visao geral dos 3 niveis ASVS (L1, L2, L3)
   - Cross-reference table: OWASP Top 10 category -> ASVS chapters -> verification items
   - Cross-reference table: CIS Benchmarks -> ASVS items
   - Cross-reference table: NIST CSF -> ASVS items
   - Cross-reference table: SANS Top 25 -> ASVS items

2. Criar `owasp-asvs/references/asvs-verification-items.md` com:
   - V1: Architecture, Design, Threat Modeling (25 items)
   - V2: Authentication (40 items)
   - V3: Session Management (20 items)
   - V4: Access Control (20 items)
   - V5: Validation, Sanitization, Encoding (30 items)
   - V6: Stored Cryptography (15 items)
   - V7: Error Handling and Logging (15 items)
   - V8: Data Protection (15 items)
   - V9: Communication (10 items)
   - V10: Malicious Code (10 items)
   - V11: Business Logic (15 items)
   - V12: Files and Resources (15 items)
   - V13: API and Web Service (20 items)
   - V14: Configuration (15 items)

**Criterios de Aceitacao (DoD)**:

- [ ] `knowledge-packs/owasp-asvs/SKILL.md` criado com overview dos 3 niveis
- [ ] Cross-reference table OWASP Top 10 -> ASVS completa para todas as 10 categorias
- [ ] Cross-reference table CIS -> ASVS com ao menos 20 mapeamentos
- [ ] Reference file com verification items cobre todos os 14 capitulos ASVS
- [ ] Cada item indica nivel minimo (L1, L2 ou L3)
- [ ] KP registrado no `KnowledgePackSelection` para inclusao condicional

**Gherkin**:

```gherkin
Feature: OWASP ASVS Reference Knowledge Pack

  Cenario: Cross-reference OWASP Top 10 A01 mapeia para ASVS V4
    DADO o knowledge pack owasp-asvs gerado
    QUANDO a tabela de cross-reference e consultada para "A01 Broken Access Control"
    ENTAO o mapeamento aponta para "V4 Access Control"
    E lista ao menos 5 verification items especificos

  Cenario: Todos os 14 capitulos ASVS estao representados
    DADO o arquivo asvs-verification-items.md gerado
    QUANDO as secoes sao contadas
    ENTAO existem exatamente 14 secoes (V1-V14)
    E cada secao contem ao menos 10 verification items

  Cenario: Cada item indica nivel ASVS minimo
    DADO qualquer verification item do ASVS
    QUANDO o item e lido
    ENTAO contem indicacao de nivel (L1, L2 ou L3)
    E L1 items sao subconjunto de L2
    E L2 items sao subconjunto de L3

  Cenario: Knowledge pack inclui referencia NIST CSF
    DADO o knowledge pack owasp-asvs
    QUANDO a tabela NIST CSF -> ASVS e consultada
    ENTAO ao menos as 5 funcoes NIST (Identify, Protect, Detect, Respond, Recover) estao mapeadas
```

---

### STORY-0005: SAST Scanner Skill (x-sast-scan)

**Titulo**: Static Application Security Testing skill com tool-selection e SARIF output

**Tipo**: Feature — Skill Executavel

**Prioridade**: Alta (core scanning capability)

**Dependencias**: STORY-0001, STORY-0002, STORY-0003

**Contexto tecnico**:
SAST analisa codigo-fonte sem executar a aplicacao. Detecta vulnerabilidades como SQL injection,
XSS, desserializacao insegura, hardcoded secrets, e uso incorreto de criptografia. E o tipo de
scan mais fundamental e deve ser a primeira skill de scanning implementada.

**Escopo de implementacao**:

1. Criar `skills/conditional/x-sast-scan/SKILL.md` seguindo o template de STORY-0003

2. Tool-selection table:

| Build Tool | Primary | Alternative | Install |
|-----------|---------|-------------|---------|
| maven | SpotBugs + FindSecBugs | Semgrep | `mvn com.github.spotbugs:spotbugs-maven-plugin:check` |
| gradle | SpotBugs + FindSecBugs | Semgrep | `./gradlew spotbugsMain` |
| npm/yarn/pnpm | ESLint security plugin | Semgrep | `npx eslint --ext .ts,.js` |
| pip/poetry | Bandit | Semgrep | `bandit -r src/` |
| go mod | gosec | Semgrep | `gosec ./...` |
| cargo | cargo-audit + clippy | Semgrep | `cargo audit && cargo clippy` |

3. Parametros:
   - `--scope all|owasp|custom-rules` — escopo do scan
   - `--fix auto|suggest|report-only` — modo de correcao
   - `--severity-threshold critical|high|medium|low` — filtro de severidade minima
   - `--exclude path1,path2` — paths a excluir

4. Workflow:
   a. Detectar linguagem e build tool do projeto
   b. Selecionar ferramenta da tool-selection table
   c. Executar scan com configuracao apropriada
   d. Parsear output para SARIF
   e. Calcular security score
   f. Gerar report Markdown
   g. Salvar em `results/security/sast-scan-YYYY-MM-DD.md` + `.sarif.json`

5. Categorias de findings mapeadas para OWASP Top 10:
   - Injection (A03) — SQL, Command, LDAP, XPath injection
   - Cryptographic Failures (A02) — Weak algorithms, hardcoded keys
   - Insecure Design (A04) — Missing input validation, unsafe deserialization
   - Security Misconfiguration (A05) — Debug enabled, default credentials
   - Auth Failures (A07) — Weak password handling, session issues

**Criterios de Aceitacao (DoD)**:

- [ ] `x-sast-scan/SKILL.md` criado seguindo security skill template
- [ ] Tool-selection table cobre 6 build tools com primary e alternative
- [ ] Parametros `--scope`, `--fix`, `--severity-threshold`, `--exclude` documentados
- [ ] Output em SARIF + Markdown com scoring normalizado
- [ ] Secao CI Integration com GH Actions, GitLab CI, Azure DevOps
- [ ] Mapeamento de findings para OWASP Top 10 categories
- [ ] Error handling para tool-not-found com instrucoes de instalacao
- [ ] Skill registrada no SkillsSelection quando `security.scanning.sast: true`

**Gherkin**:

```gherkin
Feature: SAST Scanner Skill

  Cenario: Projeto Java Maven executa SpotBugs com FindSecBugs
    DADO um projeto com build tool "maven" e language "java"
    QUANDO x-sast-scan e executado com --scope all
    ENTAO SpotBugs com FindSecBugs e invocado como ferramenta primaria
    E o report contem findings categorizados por OWASP Top 10
    E o SARIF e gerado em results/security/sast-scan-YYYY-MM-DD.sarif.json

  Cenario: Ferramenta primaria nao disponivel faz fallback para Semgrep
    DADO um projeto Python sem Bandit instalado mas com Semgrep disponivel
    QUANDO x-sast-scan e executado
    ENTAO Semgrep e usado como ferramenta alternativa
    E o report indica que fallback foi usado

  Cenario: Nenhuma ferramenta disponivel reporta INFO finding
    DADO um projeto sem nenhuma ferramenta SAST instalada
    QUANDO x-sast-scan e executado
    ENTAO o report contem finding de severidade INFO
    E o finding lista comandos de instalacao para primary e alternative

  Cenario: Filtro de severidade exclui findings abaixo do threshold
    DADO findings com severidades CRITICAL, HIGH, MEDIUM e LOW
    QUANDO x-sast-scan e executado com --severity-threshold high
    ENTAO apenas findings CRITICAL e HIGH aparecem no report
    E o score e calculado apenas com os findings filtrados
```

---

### STORY-0006: Secret Scanner Skill (x-secret-scan)

**Titulo**: Deteccao de secrets no codebase e historico git com gitleaks/truffleHog

**Tipo**: Feature — Skill Executavel

**Prioridade**: Alta (prevencao de vazamento de credenciais)

**Dependencias**: STORY-0001, STORY-0002, STORY-0003

**Contexto tecnico**:
Vazamento de secrets (API keys, passwords, tokens) em repositorios e uma das causas mais comuns
de breaches. Uma skill dedicada detecta secrets no codigo atual e no historico git, diferenciando
entre secrets ativos e revogados.

**Escopo de implementacao**:

1. Criar `skills/conditional/x-secret-scan/SKILL.md`

2. Tool-selection table:

| Context | Primary | Alternative | Install |
|---------|---------|-------------|---------|
| Codebase scan | gitleaks | truffleHog | `brew install gitleaks` / `pip install truffleHog` |
| Git history | gitleaks --log-opts | truffleHog --since-commit | Same as above |
| Pre-commit | gitleaks protect | detect-secrets | `pre-commit install` |

3. Parametros:
   - `--scope current|history|both` — escopo do scan (default: both)
   - `--baseline path/to/.gitleaks-baseline.json` — baseline de falsos positivos
   - `--since-commit SHA` — scan historico a partir de commit
   - `--format markdown|sarif|json` — formato do output

4. Categorias de secrets detectados:
   - AWS keys (AKIA...), GCP service account keys, Azure connection strings
   - API tokens (GitHub, GitLab, Slack, Stripe, Twilio)
   - Private keys (RSA, ECDSA, Ed25519)
   - Passwords em config files (jdbc, redis, mongo URIs)
   - JWT secrets, encryption keys
   - Database connection strings com credentials

5. Workflow: detect git repo -> scan current + history -> classify secrets -> check baseline -> report

**Criterios de Aceitacao (DoD)**:

- [ ] `x-secret-scan/SKILL.md` criado seguindo security skill template
- [ ] Tool-selection cobre codebase, git history e pre-commit contexts
- [ ] Parametros `--scope`, `--baseline`, `--since-commit`, `--format` documentados
- [ ] Categorias de secrets cobrem AWS, GCP, Azure, API tokens, private keys, passwords, JWT
- [ ] Baseline support para gerenciamento de falsos positivos
- [ ] Output em SARIF + Markdown com scoring
- [ ] CI Integration snippets incluidos
- [ ] Skill registrada quando `security.scanning.secretScan: true`

**Gherkin**:

```gherkin
Feature: Secret Scanner Skill

  Cenario: Scan do codebase detecta API key hardcoded
    DADO um arquivo com "AKIA1234567890EXAMPLE" no codigo
    QUANDO x-secret-scan e executado com --scope current
    ENTAO o report contem finding de tipo "AWS Access Key"
    E a severidade e CRITICAL
    E a localizacao (arquivo + linha) e indicada

  Cenario: Scan do historico git detecta secret em commit antigo
    DADO um commit anterior que adicionou um password em plaintext
    QUANDO x-secret-scan e executado com --scope history
    ENTAO o report contem finding com SHA do commit e autor
    E recomendacao de rotacao do secret e incluida

  Cenario: Baseline exclui falsos positivos conhecidos
    DADO um baseline com hash de um finding conhecido como falso positivo
    QUANDO x-secret-scan e executado com --baseline path/to/baseline
    ENTAO o finding conhecido NAO aparece no report
    E novos findings nao presentes no baseline sao reportados

  Cenario: Zero secrets encontrados resulta em score perfeito
    DADO um repositorio sem nenhum secret detectado
    QUANDO x-secret-scan e executado
    ENTAO o score e 100 e o grade e "A"
    E o report indica "No secrets detected"
```

---

### STORY-0007: Container Security Scanner Skill (x-container-scan)

**Titulo**: Scanning de imagens Docker e Dockerfile best practices com Trivy/Grype

**Tipo**: Feature — Skill Executavel

**Prioridade**: Media (relevante apenas para projetos com containers)

**Dependencias**: STORY-0001, STORY-0002, STORY-0003

**Contexto tecnico**:
Container security envolve duas dimensoes: vulnerabilidades em imagens (CVEs nos pacotes do OS e
dependencies) e best practices no Dockerfile (rodar como root, secrets em layers, base image
desatualizada). A skill cobre ambas.

**Escopo de implementacao**:

1. Criar `skills/conditional/x-container-scan/SKILL.md`

2. Tool-selection table:

| Scan Type | Primary | Alternative | Install |
|-----------|---------|-------------|---------|
| Image vulnerabilities | Trivy | Grype | `brew install trivy` / `brew install grype` |
| Dockerfile lint | Trivy config | hadolint | `trivy config` / `brew install hadolint` |
| Layer analysis | Trivy | dive | `trivy image` / `brew install dive` |

3. Parametros:
   - `--image name:tag` — imagem para scan
   - `--dockerfile path` — Dockerfile para lint
   - `--severity-threshold critical|high|medium|low`
   - `--ignore-unfixed` — ignorar CVEs sem fix disponivel

4. Checks de Dockerfile best practices:
   - Running as root (sem USER instruction)
   - Secrets em build args ou COPY de .env
   - Base image sem tag fixa (using :latest)
   - Ausencia de multi-stage build
   - Pacotes desnecessarios (curl, wget em runtime stage)
   - Permissoes excessivas em COPY/ADD
   - Health check ausente

**Criterios de Aceitacao (DoD)**:

- [ ] `x-container-scan/SKILL.md` criado seguindo security skill template
- [ ] Tool-selection cobre image scan, Dockerfile lint e layer analysis
- [ ] Parametros documentados com defaults
- [ ] Checks de Dockerfile cobrem ao menos 7 best practices
- [ ] Output em SARIF + Markdown com scoring
- [ ] Skill registrada quando `security.scanning.containerScan: true`

**Gherkin**:

```gherkin
Feature: Container Security Scanner

  Cenario: Scan de imagem detecta CVE critica
    DADO uma imagem Docker com pacote vulneravel (CVE conhecida, CVSS >= 9.0)
    QUANDO x-container-scan e executado com --image myapp:latest
    ENTAO o report contem finding CRITICAL com CVE ID
    E a versao fixa do pacote e indicada

  Cenario: Dockerfile lint detecta execucao como root
    DADO um Dockerfile sem instrucao USER
    QUANDO x-container-scan e executado com --dockerfile ./Dockerfile
    ENTAO o report contem finding HIGH "Container runs as root"
    E a remediacao sugere adicionar "USER nonroot"

  Cenario: Dockerfile com multi-stage e non-root e aprovado
    DADO um Dockerfile com multi-stage build, USER nonroot, e sem secrets
    QUANDO x-container-scan e executado
    ENTAO o score de Dockerfile best practices e >= 90
    E nenhum finding CRITICAL ou HIGH e reportado

  Cenario: Imagem com --ignore-unfixed exclui CVEs sem fix
    DADO uma imagem com 5 CVEs, 2 sem fix disponivel
    QUANDO x-container-scan e executado com --ignore-unfixed
    ENTAO apenas 3 CVEs aparecem no report
```

---

### STORY-0008: Infrastructure Security Scanner Skill (x-infra-scan)

**Titulo**: Scanning de IaC e manifests Kubernetes com checkov/kube-bench

**Tipo**: Feature — Skill Executavel

**Prioridade**: Media (relevante para projetos com infra as code)

**Dependencias**: STORY-0001, STORY-0002, STORY-0003

**Contexto tecnico**:
Infrastructure as Code (IaC) security detecta misconfiguration em Kubernetes manifests, Terraform
plans, Helm charts e Docker Compose files antes do deploy. Foca em CIS benchmarks, security
contexts, network policies e RBAC.

**Escopo de implementacao**:

1. Criar `skills/conditional/x-infra-scan/SKILL.md`

2. Tool-selection table:

| IaC Type | Primary | Alternative | Install |
|----------|---------|-------------|---------|
| Kubernetes | kube-bench + checkov | kubescape | `pip install checkov` / `brew install kubescape` |
| Terraform | checkov | tfsec | `pip install checkov` / `brew install tfsec` |
| Helm | checkov | kubescape | Same |
| Docker Compose | checkov | — | `pip install checkov` |

3. Parametros:
   - `--scope k8s|terraform|helm|compose|all` (default: auto-detect)
   - `--benchmark cis-1.8|cis-1.7|custom`
   - `--framework checkov|kube-bench|kubescape`

4. Categorias de checks:
   - Security contexts (runAsNonRoot, readOnlyRootFilesystem, capabilities)
   - Network policies (ingress/egress rules, default deny)
   - RBAC (least privilege, service account permissions)
   - Secrets management (no plaintext secrets in manifests)
   - Resource limits (CPU/memory limits defined)
   - Pod security standards (restricted, baseline, privileged)

**Criterios de Aceitacao (DoD)**:

- [ ] `x-infra-scan/SKILL.md` criado seguindo security skill template
- [ ] Tool-selection cobre K8s, Terraform, Helm, Docker Compose
- [ ] CIS benchmark reference incluida
- [ ] Categorias cobrem security contexts, network policies, RBAC, secrets, resources, PSS
- [ ] Output em SARIF + Markdown com scoring
- [ ] Skill registrada quando `security.scanning.infraScan: true`

**Gherkin**:

```gherkin
Feature: Infrastructure Security Scanner

  Cenario: K8s manifest sem security context gera finding HIGH
    DADO um Kubernetes Deployment sem securityContext definido
    QUANDO x-infra-scan e executado com --scope k8s
    ENTAO o report contem finding HIGH "Missing security context"
    E a remediacao inclui exemplo de securityContext correto

  Cenario: Terraform plan com security group aberto gera finding CRITICAL
    DADO um Terraform plan com security group permitindo ingress 0.0.0.0/0 na porta 22
    QUANDO x-infra-scan e executado com --scope terraform
    ENTAO o report contem finding CRITICAL "Unrestricted SSH access"

  Cenario: Auto-detect identifica tipo de IaC correto
    DADO um diretorio com arquivos .tf e .yaml
    QUANDO x-infra-scan e executado com --scope all
    ENTAO tanto Terraform quanto Kubernetes sao escaneados
    E o report consolida findings de ambos

  Cenario: Manifest compliant com CIS resulta em score alto
    DADO manifests K8s com security contexts, network policies, RBAC e resource limits
    QUANDO x-infra-scan e executado
    ENTAO o score e >= 90
```

---

### STORY-0009: DAST Scanner Skill (x-dast-scan)

**Titulo**: Dynamic Application Security Testing com OWASP ZAP/Nuclei e suporte multi-ambiente

**Tipo**: Feature — Skill Executavel

**Prioridade**: Alta (unica skill que testa a aplicacao em execucao)

**Dependencias**: STORY-0001, STORY-0002, STORY-0003

**Contexto tecnico**:
DAST testa a aplicacao em execucao, enviando requests maliciosos para detectar vulnerabilidades
como XSS refletido, SQL injection, CSRF, headers ausentes, e misconfiguration. E a unica
tecnica que valida o comportamento real da aplicacao (nao apenas o codigo-fonte).

A skill DEVE respeitar RULE-004: parametro de ambiente com restricoes de seguranca por ambiente.

**Escopo de implementacao**:

1. Criar `skills/conditional/x-dast-scan/SKILL.md`

2. Tool-selection table:

| Mode | Primary | Alternative | Install |
|------|---------|-------------|---------|
| Passive scan | OWASP ZAP (daemon mode) | Nuclei (passive templates) | `docker pull zaproxy/zap-stable` |
| Active scan | OWASP ZAP (attack mode) | Nuclei (active templates) | Same |
| API scan | OWASP ZAP (API scan) | Nuclei + custom templates | Same |

3. Parametros:
   - `--target url` — URL alvo (obrigatorio)
   - `--env local|dev|homolog|prod` — ambiente (default: local)
   - `--mode passive|active|full` — modo de scan (default: passive)
   - `--confirm-prod` — confirmacao explicita para producao
   - `--openapi path/to/spec.yaml` — spec OpenAPI para API scan
   - `--auth-token token` — token de autenticacao

4. Restricoes por ambiente (RULE-004):
   - **local**: todos os modos (passive, active, full)
   - **dev**: todos os modos
   - **homolog**: passive + active (sem fuzzing destrutivo)
   - **prod**: APENAS passive (scan de headers, TLS, info disclosure). Requer `--confirm-prod`

5. Categorias de findings:
   - Injection (XSS, SQLi, Command injection, LDAP injection)
   - Broken Authentication (session issues, cookie flags)
   - Security Misconfiguration (headers, CORS, TLS)
   - Information Disclosure (stack traces, version headers, directory listing)

**Criterios de Aceitacao (DoD)**:

- [ ] `x-dast-scan/SKILL.md` criado com tool-selection e environment support
- [ ] Parametros `--target`, `--env`, `--mode`, `--confirm-prod`, `--openapi` documentados
- [ ] Restricoes por ambiente claramente documentadas
- [ ] Producao bloqueada sem `--confirm-prod` (fail-safe)
- [ ] API scan mode suporta OpenAPI spec como input
- [ ] Output em SARIF + Markdown com scoring
- [ ] Skill registrada quando `security.scanning.dast: true`

**Gherkin**:

```gherkin
Feature: DAST Scanner Skill

  Cenario: Scan passivo em local detecta headers ausentes
    DADO uma aplicacao rodando em localhost:8080 sem security headers
    QUANDO x-dast-scan e executado com --target http://localhost:8080 --mode passive
    ENTAO o report contem findings para X-Frame-Options, X-Content-Type-Options, HSTS ausentes
    E a severidade e MEDIUM

  Cenario: Producao sem --confirm-prod e bloqueada
    DADO um target em producao
    QUANDO x-dast-scan e executado com --env prod SEM --confirm-prod
    ENTAO a skill FALHA com mensagem "Production scan requires --confirm-prod flag"
    E nenhum scan e executado

  Cenario: Producao com --confirm-prod executa apenas passive
    DADO um target em producao com --confirm-prod
    QUANDO x-dast-scan e executado com --env prod --mode active --confirm-prod
    ENTAO o modo e automaticamente reduzido para passive
    E o report indica "Mode downgraded to passive for production environment"

  Cenario: Active scan detecta XSS refletido
    DADO uma aplicacao vulneravel a XSS refletido
    QUANDO x-dast-scan e executado com --mode active --env local
    ENTAO o report contem finding HIGH "Reflected XSS"
    E o finding inclui a URL e payload que causou a deteccao
```

---

### STORY-0010: OWASP Top 10 Verification Skill (x-owasp-scan)

**Titulo**: Verificacao automatizada OWASP Top 10 com mapeamento ASVS

**Tipo**: Feature — Skill Executavel (Composicao parcial)

**Prioridade**: Alta (verificacao padrao de seguranca)

**Dependencias**: STORY-0002, STORY-0003, STORY-0004

**Contexto tecnico**:
A verificacao OWASP Top 10 combina analise estatica de codigo com checks de configuracao para
validar cada uma das 10 categorias OWASP. Para A06 (Vulnerable Components), delega ao
x-dependency-audit existente. Mapeia cada verificacao para ASVS levels.

**Escopo de implementacao**:

1. Criar `skills/core/x-owasp-scan/SKILL.md`

2. Verificacoes por categoria:

| OWASP | Verificacao | ASVS |
|-------|-----------|------|
| A01 Broken Access Control | Check authorization patterns, RBAC, path traversal | V4 |
| A02 Cryptographic Failures | Check crypto usage, key storage, TLS config | V6, V9 |
| A03 Injection | Check input validation, parameterized queries, ORM usage | V5 |
| A04 Insecure Design | Check security design patterns, threat model presence | V1 |
| A05 Security Misconfiguration | Check configs, defaults, debug mode, error pages | V14 |
| A06 Vulnerable Components | Delegate to x-dependency-audit | V10 |
| A07 Auth Failures | Check auth implementation, password policy, MFA | V2, V3 |
| A08 Data Integrity Failures | Check deserialization, CI/CD security, signatures | V10 |
| A09 Logging Failures | Check logging patterns, audit trail, monitoring | V7 |
| A10 SSRF | Check URL validation, outbound request patterns | V5, V13 |

3. Parametros:
   - `--level L1|L2|L3` — ASVS level de verificacao (default: L1)
   - `--category A01..A10|all` — categorias especificas ou todas (default: all)
   - `--report-format markdown|sarif|both` (default: both)

4. Output: report com pass/fail por categoria, score geral, ASVS coverage percentage

**Criterios de Aceitacao (DoD)**:

- [ ] `x-owasp-scan/SKILL.md` criado com verificacoes para todas as 10 categorias
- [ ] Cada categoria mapeia para ASVS chapters
- [ ] Parametros `--level`, `--category`, `--report-format` documentados
- [ ] A06 delega ao x-dependency-audit existente (RULE-011)
- [ ] Output com pass/fail por categoria e ASVS coverage percentage
- [ ] Score normalizado 0-100 por RULE-005

**Gherkin**:

```gherkin
Feature: OWASP Top 10 Verification

  Cenario: Verificacao completa L1 cobre todas as 10 categorias
    DADO um projeto Java com security headers e input validation
    QUANDO x-owasp-scan e executado com --level L1 --category all
    ENTAO o report contem resultado para cada A01 a A10
    E o ASVS coverage para L1 e calculado como percentage

  Cenario: Categoria A06 delega para x-dependency-audit
    DADO um projeto com dependencias
    QUANDO x-owasp-scan e executado com --category A06
    ENTAO o x-dependency-audit existente e invocado
    E os resultados sao integrados no report OWASP

  Cenario: Verificacao L3 inclui checks avancados
    DADO um projeto com requisito de alta seguranca
    QUANDO x-owasp-scan e executado com --level L3
    ENTAO verificacoes adicionais de L3 sao executadas (crypto avancado, anti-automation, etc.)
    E o report indica quais checks sao L3-only

  Cenario: Projeto sem input validation falha A03
    DADO um projeto sem validacao de input em endpoints
    QUANDO x-owasp-scan e executado
    ENTAO A03 (Injection) e marcado como FAIL
    E o score reflete deducoes para findings de A03
```

---

### STORY-0011: SonarQube Quality Gate Skill (x-sonar-gate)

**Titulo**: Integracao SonarQube/SonarLint com security hotspot tracking e quality gate

**Tipo**: Feature — Skill Executavel + Config Generator

**Prioridade**: Media

**Dependencias**: STORY-0002, STORY-0005

**Contexto tecnico**:
SonarQube e a plataforma mais usada para continuous code quality. A skill gera a configuracao
do projeto (sonar-project.properties), executa o SonarScanner, e verifica o quality gate de
seguranca. Foca em security hotspots (codigo que requer review de seguranca) e vulnerabilities.

**Escopo de implementacao**:

1. Criar `skills/conditional/x-sonar-gate/SKILL.md`

2. Funcionalidades:
   - Geracao de `sonar-project.properties` com security rules habilitadas
   - Execucao do SonarScanner (CLI ou Maven plugin)
   - Polling do quality gate status
   - Report de security hotspots e vulnerabilities

3. Parametros:
   - `--server url` — SonarQube/SonarCloud URL
   - `--token token` — authentication token
   - `--quality-gate default|strict` — nivel do quality gate
   - `--project-key key` — chave do projeto (default: auto-detect)

4. Quality gate thresholds (strict mode):
   - Security vulnerabilities: 0 (zero tolerance)
   - Security hotspots reviewed: 100%
   - Security rating: A
   - New security hotspots: 0

**Criterios de Aceitacao (DoD)**:

- [ ] `x-sonar-gate/SKILL.md` criado com config generation e quality gate check
- [ ] Parametros `--server`, `--token`, `--quality-gate`, `--project-key` documentados
- [ ] Geracao de sonar-project.properties com security rules
- [ ] Quality gate thresholds para default e strict modes
- [ ] CI Integration snippets incluidos
- [ ] Skill registrada quando `security.qualityGate.provider` != "none"

**Gherkin**:

```gherkin
Feature: SonarQube Quality Gate

  Cenario: Geracao de sonar-project.properties com security rules
    DADO um projeto Java Maven
    QUANDO x-sonar-gate e executado
    ENTAO o arquivo sonar-project.properties e gerado
    E contem sonar.security.hotspots.reviewed.threshold=100

  Cenario: Quality gate strict com vulnerabilidades falha
    DADO um projeto com 2 security vulnerabilities no SonarQube
    QUANDO x-sonar-gate e executado com --quality-gate strict
    ENTAO o report indica quality gate FAILED
    E lista as 2 vulnerabilidades com severity

  Cenario: Quality gate default com hotspots nao-revisados
    DADO um projeto com security hotspots nao-revisados
    QUANDO x-sonar-gate e executado com --quality-gate default
    ENTAO o report lista hotspots pendentes de review
    E inclui links diretos para cada hotspot no SonarQube

  Cenario: Projeto sem servidor SonarQube configurado
    DADO um config sem security.qualityGate.serverUrl
    QUANDO x-sonar-gate e executado sem --server
    ENTAO a skill falha com mensagem "SonarQube server URL required"
```

---

### STORY-0012: Application Hardening Evaluation Skill (x-hardening-eval)

**Titulo**: Avaliacao sistematica de hardening da aplicacao contra CIS benchmarks

**Tipo**: Feature — Skill Executavel

**Prioridade**: Media

**Dependencias**: STORY-0002, STORY-0004

**Contexto tecnico**:
Hardening avalia configuracoes de seguranca da aplicacao em execucao: HTTP security headers,
TLS configuration, CORS policy, cookie attributes, session management. Diferente de DAST
(que busca vulnerabilidades), hardening avalia a postura defensiva da aplicacao.

**Escopo de implementacao**:

1. Criar `skills/core/x-hardening-eval/SKILL.md`

2. Categorias de avaliacao:

| Categoria | Checks | Weight |
|-----------|--------|--------|
| HTTP Security Headers | HSTS, X-Frame-Options, X-Content-Type-Options, CSP, CORP, Permissions-Policy, Referrer-Policy | 25% |
| TLS Configuration | Min TLS 1.2, cipher suites, OCSP stapling, certificate validity | 20% |
| CORS Policy | Origin validation, credentials handling, max-age | 15% |
| Cookie Security | Secure, HttpOnly, SameSite, path restriction | 15% |
| Error Handling | No stack traces, generic error pages, custom error codes | 10% |
| Input Limits | Max body size, max header size, max URL length, request rate | 10% |
| Information Disclosure | Server header, X-Powered-By, version leaking | 5% |

3. Parametros:
   - `--target url` — URL alvo para avaliacao
   - `--scope headers|tls|cors|cookies|errors|limits|disclosure|all` (default: all)
   - `--benchmark cis|owasp|custom`

**Criterios de Aceitacao (DoD)**:

- [ ] `x-hardening-eval/SKILL.md` criado com 7 categorias de avaliacao
- [ ] Cada categoria tem peso ponderado no score final
- [ ] Parametros `--target`, `--scope`, `--benchmark` documentados
- [ ] Report com score por categoria e score geral
- [ ] Cada finding inclui remediacao especifica por framework ({{FRAMEWORK}})

**Gherkin**:

```gherkin
Feature: Application Hardening Evaluation

  Cenario: Aplicacao sem HSTS gera finding HIGH
    DADO uma aplicacao respondendo sem header Strict-Transport-Security
    QUANDO x-hardening-eval e executado com --scope headers
    ENTAO o report contem finding HIGH "Missing HSTS header"
    E a remediacao inclui config especifica para o framework

  Cenario: TLS 1.1 gera finding CRITICAL
    DADO uma aplicacao aceitando TLS 1.1
    QUANDO x-hardening-eval e executado com --scope tls
    ENTAO o report contem finding CRITICAL "TLS 1.1 is deprecated"
    E a remediacao indica como configurar TLS 1.2 minimo

  Cenario: Aplicacao totalmente hardened recebe score alto
    DADO uma aplicacao com todos os headers, TLS 1.3, CORS restrito, cookies secure
    QUANDO x-hardening-eval e executado com --scope all
    ENTAO o score geral e >= 95
    E nenhum finding CRITICAL ou HIGH

  Cenario: Score ponderado reflete peso das categorias
    DADO findings MEDIUM em headers (25%) e input limits (10%)
    QUANDO o score e calculado
    ENTAO a deducao de headers tem peso 2.5x maior que input limits
```

---

### STORY-0013: Runtime Protection Evaluation Skill (x-runtime-protection)

**Titulo**: Avaliacao de controles de protecao runtime (rate limiting, WAF, bot protection)

**Tipo**: Feature — Skill Executavel

**Prioridade**: Baixa

**Dependencias**: STORY-0002, STORY-0004

**Contexto tecnico**:
Alem do hardening (configuracao passiva), aplicacoes precisam de controles ativos de protecao:
rate limiting para prevenir brute force, WAF rules para filtrar payloads maliciosos, bot
protection para distinguir usuarios reais de bots.

**Escopo de implementacao**:

1. Criar `skills/core/x-runtime-protection/SKILL.md`

2. Categorias:
   - Rate limiting (global, per-endpoint, per-user)
   - WAF rules (OWASP CRS, custom rules)
   - Bot protection (CAPTCHA, fingerprinting, behavioral analysis)
   - Account lockout (max attempts, lockout duration, progressive delay)
   - Brute force protection (login, password reset, API keys)
   - Content Security Policy (CSP directives, report-uri)
   - Feature Policy / Permissions Policy

3. Parametros:
   - `--scope rate-limit|waf|bot-protection|account-lockout|all` (default: all)
   - `--target url` — URL alvo (para checks remotos)

**Criterios de Aceitacao (DoD)**:

- [ ] `x-runtime-protection/SKILL.md` criado com todas as categorias
- [ ] Checks cobrem rate limiting, WAF, bot protection, account lockout
- [ ] Parametros documentados
- [ ] Report com recomendacoes especificas por framework
- [ ] Score normalizado 0-100

**Gherkin**:

```gherkin
Feature: Runtime Protection Evaluation

  Cenario: Aplicacao sem rate limiting gera finding HIGH
    DADO uma aplicacao sem middleware de rate limiting
    QUANDO x-runtime-protection e executado com --scope rate-limit
    ENTAO o report contem finding HIGH "No rate limiting detected"
    E a remediacao inclui exemplos por framework

  Cenario: Rate limiting presente mas sem lockout gera finding MEDIUM
    DADO uma aplicacao com rate limiting mas sem account lockout
    QUANDO x-runtime-protection e executado com --scope all
    ENTAO rate limiting e marcado como PASS
    E account lockout e marcado como FAIL com severidade MEDIUM

  Cenario: CSP report-only mode gera finding LOW
    DADO uma aplicacao com CSP em modo report-only
    QUANDO x-runtime-protection e executado
    ENTAO o report contem finding LOW "CSP in report-only mode"
    E recomenda migrar para enforce mode

  Cenario: Protecao completa resulta em score alto
    DADO uma aplicacao com rate limiting, WAF, bot protection e account lockout
    QUANDO x-runtime-protection e executado
    ENTAO o score e >= 90
```

---

### STORY-0014: Enhanced Supply Chain Analysis Skill (x-supply-chain-audit)

**Titulo**: Analise profunda de supply chain alem do x-dependency-audit existente

**Tipo**: Feature — Skill Executavel

**Prioridade**: Media

**Dependencias**: STORY-0001, STORY-0002

**Contexto tecnico**:
O x-dependency-audit existente cobre vulnerabilidades (CVE), outdated packages e license
compliance. Esta skill ESTENDE (nao substitui) com analises mais profundas: risco de mantenedor,
typosquatting detection, phantom dependencies, dependency age analysis e CVE exploit prediction.

**Escopo de implementacao**:

1. Criar `skills/core/x-supply-chain-audit/SKILL.md`

2. Analises adicionais (alem do x-dependency-audit):
   - **Maintainer Risk**: single-maintainer packages, low bus factor, inactive maintainers
   - **Typosquatting Detection**: name similarity analysis contra packages populares
   - **Phantom Dependencies**: deps usadas mas nao declaradas (e.g., transitive dep usada diretamente)
   - **Dependency Age**: age since last release, packages sem release ha >1 ano
   - **CVE Exploit Prediction**: EPSS (Exploit Prediction Scoring System) para CVEs existentes
   - **SLSA Level Assessment**: avaliacao do nivel SLSA de cada dependency

3. Parametros:
   - `--depth shallow|deep` (default: shallow — apenas direct deps)
   - `--include-dev-deps` — incluir dev dependencies na analise
   - `--risk-threshold low|medium|high|critical`

4. Risk scoring formula: CVE weight 40%, depth 20%, maintainer activity 15%, license 15%, popularity 10%

**Criterios de Aceitacao (DoD)**:

- [ ] `x-supply-chain-audit/SKILL.md` criado com 6 analises adicionais
- [ ] Integra com x-dependency-audit existente (RULE-011, RULE-014)
- [ ] Parametros `--depth`, `--include-dev-deps`, `--risk-threshold` documentados
- [ ] Risk scoring formula documentada com pesos
- [ ] Report com risk score por dependency e score geral

**Gherkin**:

```gherkin
Feature: Enhanced Supply Chain Analysis

  Cenario: Dependency com single maintainer gera finding MEDIUM
    DADO uma dependency com apenas 1 maintainer no registry
    QUANDO x-supply-chain-audit e executado com --depth deep
    ENTAO o report contem finding MEDIUM "Single maintainer package"
    E recomenda avaliar alternativas

  Cenario: Nome similar a pacote popular detecta typosquatting risk
    DADO uma dependency com nome similar a um pacote top-1000 (edit distance <= 2)
    QUANDO x-supply-chain-audit e executado
    ENTAO o report contem finding HIGH "Potential typosquatting"
    E indica o pacote popular similar

  Cenario: Dependency sem release ha mais de 1 ano gera finding LOW
    DADO uma dependency cuja ultima release foi ha 18 meses
    QUANDO x-supply-chain-audit e executado
    ENTAO o report contem finding LOW "Stale dependency"
    E indica a data da ultima release

  Cenario: Deep analysis inclui transitive dependencies
    DADO um projeto com 10 direct e 50 transitive dependencies
    QUANDO x-supply-chain-audit e executado com --depth deep
    ENTAO todas as 60 dependencies sao analisadas
    E o report distingue entre direct e transitive
```

---

### STORY-0015: Pentest Engineer Agent

**Titulo**: Persona ofensiva de seguranca: exploitation, attack chains, PoC

**Tipo**: Feature — Agent

**Prioridade**: Media

**Dependencias**: STORY-0002

**Contexto tecnico**:
O pentest engineer e a persona de seguranca ofensiva. Diferente do security-engineer (que revisa
codigo com checklist), o pentest engineer valida se vulnerabilidades sao exploraveis, construi
attack chains, e desenvolve provas de conceito. Ativado quando `security.pentest: true`.

**Escopo de implementacao**:

1. Criar `agents/conditional/pentest-engineer.md` seguindo formato de `security-engineer.md`

2. Checklist (15 items):
   1. Reconnaissance — Mapear superficie de ataque (endpoints, servicos, tecnologias)
   2. Vulnerability Validation — Confirmar se findings SAST/DAST sao exploraveis
   3. Authentication Testing — Testar bypass de autenticacao, brute force, credential stuffing
   4. Authorization Testing — Testar escalacao de privilegios, IDOR, path traversal
   5. Injection Testing — Validar SQL injection, command injection, LDAP injection
   6. XSS Validation — Testar XSS refletido, stored, DOM-based
   7. Deserialization Testing — Testar desserializacao insegura
   8. SSRF Testing — Testar Server-Side Request Forgery
   9. Attack Chain Analysis — Encadear vulnerabilidades para impacto maximo
   10. Proof of Concept — Desenvolver PoC reprodutivel para cada finding exploitavel
   11. Lateral Movement Analysis — Avaliar possibilidade de movimento lateral
   12. Data Exfiltration Risk — Avaliar risco de exfiltracao de dados
   13. CVSS Scoring — Calcular CVSS 4.0 para cada finding validado
   14. Remediation Priority — Priorizar remediacoes por risco real (nao apenas severidade)
   15. Executive Summary — Resumo executivo com business impact

3. Scope declaration (RULE-006):
   - **Inclui**: exploitation, attack chains, PoC, impact analysis
   - **Exclui**: code review (security-engineer), SDLC (appsec-engineer), pipeline (devsecops)

**Criterios de Aceitacao (DoD)**:

- [ ] `agents/conditional/pentest-engineer.md` criado com 15-point checklist
- [ ] Scope declaration explicita com inclusoes e exclusoes
- [ ] Formato identico ao security-engineer.md (RULE-012)
- [ ] Ativacao condicional quando `security.pentest: true`
- [ ] Agent registrado no AgentsSelection

**Gherkin**:

```gherkin
Feature: Pentest Engineer Agent

  Cenario: Agent ativado quando security.pentest e true
    DADO um config com "security.pentest: true"
    QUANDO o gerador executa
    ENTAO o arquivo agents/pentest-engineer.md e incluido no output

  Cenario: Agent NAO ativado quando security.pentest e false
    DADO um config sem security.pentest ou com security.pentest: false
    QUANDO o gerador executa
    ENTAO o arquivo agents/pentest-engineer.md NAO e incluido

  Cenario: Checklist contem 15 items numerados
    DADO o agent pentest-engineer.md gerado
    QUANDO o conteudo e lido
    ENTAO existem exatamente 15 items de checklist numerados
    E cada item tem titulo e descricao

  Cenario: Scope declaration nao sobrepoe com security-engineer
    DADO os agents pentest-engineer e security-engineer
    QUANDO os scopes sao comparados
    ENTAO nao ha sobreposicao nas responsabilidades declaradas
```

---

### STORY-0016: AppSec Engineer Agent

**Titulo**: Persona de Application Security: secure SDLC, security architecture, testing strategy

**Tipo**: Feature — Agent

**Prioridade**: Media

**Dependencias**: Nenhuma. Pode ser implementada em paralelo com Layer 0.

**Contexto tecnico**:
O AppSec engineer foca no SDLC seguro: como integrar seguranca em cada fase do desenvolvimento.
Diferente do security-engineer (review de codigo) e pentest-engineer (exploitation), o appsec
engineer trabalha em nivel de arquitetura e processo.

**Escopo de implementacao**:

1. Criar `agents/conditional/appsec-engineer.md`

2. Checklist (12 items):
   1. Security Requirements — Extrair requisitos de seguranca da spec
   2. Threat Model Validation — Validar modelo de ameacas existente
   3. Secure Design Patterns — Recomendar patterns de design seguro
   4. Security Test Plan — Definir estrategia de testes de seguranca
   5. SAST Integration — Garantir que SAST esta integrado no pipeline
   6. DAST Integration — Garantir que DAST esta integrado em staging
   7. Security Regression Tests — Testes que previnem reintroducao de vulnerabilidades
   8. Security Acceptance Criteria — Criterios de aceitacao de seguranca em stories
   9. Security Documentation — Documentar decisoes de seguranca (ADRs)
   10. Security Training Needs — Identificar necessidades de treinamento
   11. Security Metrics — Definir metricas de seguranca (MTTR, vulnerability density)
   12. Shift-Left Recommendations — Recomendacoes para antecipar seguranca no ciclo

3. Ativacao: quando `security.frameworks` e nao-vazio

**Criterios de Aceitacao (DoD)**:

- [ ] `agents/conditional/appsec-engineer.md` criado com 12-point checklist
- [ ] Scope nao sobrepoe com outros agents de seguranca
- [ ] Ativacao condicional quando security.frameworks e nao-vazio
- [ ] Formato identico ao security-engineer.md

**Gherkin**:

```gherkin
Feature: AppSec Engineer Agent

  Cenario: Agent ativado quando security.frameworks e nao-vazio
    DADO um config com "security.compliance: [pci-dss]"
    QUANDO o gerador executa
    ENTAO o arquivo agents/appsec-engineer.md e incluido no output

  Cenario: Checklist contem 12 items
    DADO o agent appsec-engineer.md gerado
    QUANDO o conteudo e lido
    ENTAO existem exatamente 12 items de checklist numerados

  Cenario: Agent foca em SDLC, nao em code review
    DADO o agent appsec-engineer.md
    QUANDO as responsabilidades sao lidas
    ENTAO o scope inclui "security architecture" e "testing strategy"
    E o scope exclui "code review" e "exploitation"
```

---

### STORY-0017: DevSecOps Engineer Agent

**Titulo**: Persona de DevSecOps: pipeline security, artifact signing, SLSA compliance

**Tipo**: Feature — Agent

**Prioridade**: Baixa

**Dependencias**: Nenhuma. Pode ser implementada em paralelo com Layer 0.

**Contexto tecnico**:
O DevSecOps engineer foca na seguranca do pipeline CI/CD e supply chain: configuracao de stages
de seguranca, assinatura de artefatos, compliance SLSA, secrets management no pipeline.

**Escopo de implementacao**:

1. Criar `agents/conditional/devsecops-engineer.md`

2. Checklist (12 items):
   1. Pipeline Secrets Management — Secrets injetados via vault, nao env vars
   2. Build Isolation — Builds em ambientes isolados (containers)
   3. Artifact Signing — Assinatura com cosign/sigstore
   4. SLSA Compliance Level — Avaliacao do nivel SLSA (1-4) do pipeline
   5. Dependency Pinning — Dependencies com hash pinning, nao version ranges
   6. Security Scan Stages — Stages de seguranca no pipeline (SAST, secret scan, etc.)
   7. Quality Gate Enforcement — Quality gates bloqueiam merge em caso de falha
   8. Deployment Approval Gates — Aprovacoes para deploy em homolog/prod
   9. Runtime Security Monitoring — Monitoramento de seguranca em runtime
   10. Incident Response Automation — Automacao de resposta a incidentes
   11. Security as Code — Politicas de seguranca versionadas como codigo
   12. Compliance Evidence Collection — Coleta automatica de evidencias de compliance

3. Ativacao: quando `infrastructure.container != "none"` OR `infrastructure.orchestrator != "none"`

**Criterios de Aceitacao (DoD)**:

- [ ] `agents/conditional/devsecops-engineer.md` criado com 12-point checklist
- [ ] Ativacao condicional baseada em infrastructure flags
- [ ] Scope nao sobrepoe com outros agents
- [ ] Formato identico ao security-engineer.md

**Gherkin**:

```gherkin
Feature: DevSecOps Engineer Agent

  Cenario: Agent ativado quando container e configurado
    DADO um config com "infrastructure.container: docker"
    QUANDO o gerador executa
    ENTAO o arquivo agents/devsecops-engineer.md e incluido

  Cenario: Agent ativado quando orchestrator e configurado
    DADO um config com "infrastructure.orchestrator: kubernetes"
    QUANDO o gerador executa
    ENTAO o arquivo agents/devsecops-engineer.md e incluido

  Cenario: Agent NAO ativado quando ambos sao none
    DADO um config com container: none e orchestrator: none
    QUANDO o gerador executa
    ENTAO o arquivo agents/devsecops-engineer.md NAO e incluido

  Cenario: Checklist contem 12 items de pipeline security
    DADO o agent devsecops-engineer.md gerado
    QUANDO o conteudo e lido
    ENTAO existem 12 items focados em pipeline e supply chain
```

---

### STORY-0018: Pentest Orchestrator Skill (x-pentest)

**Titulo**: Orquestrador de penetration testing multi-fase com suporte multi-ambiente

**Tipo**: Feature — Skill Orquestradora (Composicao)

**Prioridade**: Alta

**Dependencias**: STORY-0005, STORY-0006, STORY-0007, STORY-0008, STORY-0009, STORY-0012, STORY-0013, STORY-0015, STORY-0016

**Contexto tecnico**:
O pentest orchestrator coordena as fases de um penetration test completo, invocando skills
atomicas via subagent delegation (RULE-011). Suporta multiplos ambientes com restricoes
de seguranca por ambiente (RULE-004).

**Escopo de implementacao**:

1. Criar `skills/core/x-pentest/SKILL.md`

2. Fases do pentest:
   - **Phase 1 — Reconnaissance**: x-codebase-audit (security dimension) + x-threat-model
   - **Phase 2 — Vulnerability Scanning**: x-sast-scan + x-dast-scan + x-container-scan + x-infra-scan + x-secret-scan
   - **Phase 3 — Exploitation Validation**: pentest-engineer agent analisa findings e valida exploitability
   - **Phase 4 — Report Generation**: Consolida findings com risk ratings, attack chains, remediacao

3. Parametros:
   - `--env local|dev|homolog|prod` (default: local)
   - `--phase 1|2|3|4|all` (default: all)
   - `--scope full|quick` — quick pula Phase 3 (exploitation)
   - `--confirm-prod` — obrigatorio para prod
   - `--target url` — URL alvo para DAST (obrigatorio se phase inclui 2)

4. Restricoes por ambiente:
   - **local/dev**: todas as fases, todos os modos
   - **homolog**: todas as fases, sem fuzzing destrutivo
   - **prod**: Phase 1 + Phase 2 (passive DAST only), SEM Phase 3 (exploitation)

5. Output: `results/security/pentest-report-YYYY-MM-DD.md` com:
   - Executive summary com business impact
   - Findings priorizados por risco real
   - Attack chains identificados
   - CVSS scores para cada finding
   - Remediation roadmap priorizado

**Criterios de Aceitacao (DoD)**:

- [ ] `x-pentest/SKILL.md` criado com 4 fases documentadas
- [ ] Invoca skills atomicas via subagent (RULE-011)
- [ ] Restricoes por ambiente documentadas e enforced
- [ ] Producao bloqueada sem `--confirm-prod`
- [ ] Producao limitada a Phase 1+2 (sem exploitation)
- [ ] Report com executive summary, findings, attack chains, CVSS, remediation

**Gherkin**:

```gherkin
Feature: Pentest Orchestrator

  Cenario: Pentest completo local executa todas as 4 fases
    DADO um projeto com aplicacao rodando localmente
    QUANDO x-pentest e executado com --env local --phase all
    ENTAO Phase 1 (reconnaissance) e executada
    E Phase 2 (vulnerability scanning) invoca SAST, DAST, secret scan
    E Phase 3 (exploitation) invoca pentest-engineer agent
    E Phase 4 (report) consolida tudo em pentest-report.md

  Cenario: Pentest em producao limita a passive scanning
    DADO um target em producao
    QUANDO x-pentest e executado com --env prod --confirm-prod
    ENTAO Phase 1 e Phase 2 sao executadas
    E DAST roda em modo passive-only
    E Phase 3 (exploitation) NAO e executada
    E o report indica "Production: exploitation phase skipped"

  Cenario: Quick scope pula exploitation
    DADO um projeto
    QUANDO x-pentest e executado com --scope quick
    ENTAO Phase 1 e Phase 2 sao executadas
    E Phase 3 (exploitation) e pulada
    E Phase 4 (report) e gerado sem findings de exploitation

  Cenario: Producao sem --confirm-prod e bloqueada
    DADO um target em producao
    QUANDO x-pentest e executado com --env prod SEM --confirm-prod
    ENTAO a skill FALHA com mensagem "Production pentest requires --confirm-prod"
```

---

### STORY-0019: Security Posture Dashboard Skill (x-security-dashboard)

**Titulo**: Dashboard consolidado de postura de seguranca com score e trend

**Tipo**: Feature — Skill Orquestradora (Composicao)

**Prioridade**: Media

**Dependencias**: STORY-0005 a STORY-0014, STORY-0016, STORY-0017

**Contexto tecnico**:
O security dashboard agrega resultados de todos os scans de seguranca em uma visao consolidada
com score geral, trend (melhorando/estavel/degradando), e risk heatmap por categoria.

**Escopo de implementacao**:

1. Criar `skills/core/x-security-dashboard/SKILL.md`

2. Fontes de dados (le results existentes):
   - SAST scan results
   - DAST scan results
   - Secret scan results
   - Container scan results
   - Infrastructure scan results
   - OWASP Top 10 results
   - SonarQube gate results
   - Hardening eval results
   - Supply chain audit results
   - Dependency audit results

3. Parametros:
   - `--period last-7d|last-30d|last-90d|all` (default: last-30d)
   - `--format markdown|json` (default: markdown)
   - `--compare-previous` — inclui comparacao com periodo anterior

4. Output:
   - Overall security score (0-100) com grade (A-F)
   - Per-dimension scores (SAST, DAST, secrets, containers, etc.)
   - Risk heatmap (categorias x severidade)
   - Trend analysis (improving, stable, degrading) por dimensao
   - Top 10 findings por risco
   - Remediation priority queue

**Criterios de Aceitacao (DoD)**:

- [ ] `x-security-dashboard/SKILL.md` criado com agregacao de 10 fontes
- [ ] Score geral e per-dimension calculados
- [ ] Trend analysis com comparacao temporal
- [ ] Risk heatmap em Markdown table
- [ ] Parametros `--period`, `--format`, `--compare-previous` documentados

**Gherkin**:

```gherkin
Feature: Security Posture Dashboard

  Cenario: Dashboard agrega resultados de todos os scans disponiveis
    DADO resultados de SAST, DAST e secret scan no diretorio results/security/
    QUANDO x-security-dashboard e executado
    ENTAO o dashboard inclui scores de SAST, DAST e secret scan
    E dimensoes sem resultados sao marcadas como "Not scanned"
    E o score geral e calculado apenas com dimensoes escaneadas

  Cenario: Trend analysis identifica melhoria
    DADO resultados de SAST com score 70 ha 30 dias e score 85 agora
    QUANDO x-security-dashboard e executado com --compare-previous
    ENTAO a dimensao SAST mostra trend "improving" (+15)

  Cenario: Zero resultados indica necessidade de scans
    DADO diretorio results/security/ vazio
    QUANDO x-security-dashboard e executado
    ENTAO o dashboard indica "No security scans found"
    E lista os scans recomendados com comandos de execucao

  Cenario: Output JSON para integracao
    DADO resultados de scans disponiveis
    QUANDO x-security-dashboard e executado com --format json
    ENTAO o output e JSON valido com campos: overallScore, dimensions, trend, topFindings
```

---

### STORY-0020: Security CI Pipeline Generator (x-security-pipeline)

**Titulo**: Gerador de pipeline CI/CD com stages de seguranca condicionais

**Tipo**: Feature — Skill Geradora (Composicao)

**Prioridade**: Media

**Dependencias**: STORY-0005 a STORY-0011

**Contexto tecnico**:
Gera configuracao de pipeline CI/CD com stages de seguranca baseado nas skills habilitadas
no config. Suporta GitHub Actions, GitLab CI e Azure DevOps.

**Escopo de implementacao**:

1. Criar `skills/core/x-security-pipeline/SKILL.md`

2. Pipeline stages (order de execucao):
   1. **Pre-commit**: secret scan (x-secret-scan)
   2. **Build**: SAST (x-sast-scan) + dependency audit (x-dependency-audit)
   3. **Build**: SonarQube analysis (x-sonar-gate) — se habilitado
   4. **Build**: container scan (x-container-scan) — se Dockerfile presente
   5. **Deploy-staging**: DAST passive (x-dast-scan --mode passive)
   6. **Deploy-staging**: OWASP scan (x-owasp-scan)
   7. **Deploy-staging**: hardening eval (x-hardening-eval)
   8. **Quality Gate**: SonarQube quality gate check

3. Parametros:
   - `--ci github|gitlab|azure` (default: github)
   - `--stages all|minimal` — minimal inclui apenas secret scan + SAST + dependency audit
   - `--trigger push|pr|schedule` (default: pr)

4. Cada stage e condicional baseado no config do projeto (RULE-010)

**Criterios de Aceitacao (DoD)**:

- [ ] `x-security-pipeline/SKILL.md` criado com 8 stages condicionais
- [ ] Gera YAML para GH Actions, GitLab CI e Azure DevOps
- [ ] Stages sao condicionais baseados nos flags de SecurityConfig
- [ ] Parametros `--ci`, `--stages`, `--trigger` documentados
- [ ] YAML gerado e valido e copy-paste ready

**Gherkin**:

```gherkin
Feature: Security CI Pipeline Generator

  Cenario: Config com SAST e secret scan gera pipeline minimal
    DADO um config com scanning.sast: true e scanning.secretScan: true
    QUANDO x-security-pipeline e executado com --ci github --stages minimal
    ENTAO o workflow GH Actions e gerado com steps para secret-scan e sast-scan
    E NAO inclui steps de DAST ou container scan

  Cenario: Config completo gera pipeline com todas as stages
    DADO um config com todos os scanning flags habilitados e SonarQube
    QUANDO x-security-pipeline e executado com --ci github --stages all
    ENTAO o workflow contem 8 stages na ordem correta
    E o YAML e valido

  Cenario: GitLab CI gera stages equivalentes
    DADO o mesmo config
    QUANDO x-security-pipeline e executado com --ci gitlab
    ENTAO o .gitlab-ci.yml e gerado com stages equivalentes

  Cenario: Stage condicional omitida quando flag e false
    DADO um config com scanning.containerScan: false
    QUANDO x-security-pipeline e executado
    ENTAO a stage de container scan NAO esta presente no pipeline
```

---

### STORY-0021: Compliance Auditor Agent

**Titulo**: Persona de compliance: evidence collection, gap analysis, audit preparation

**Tipo**: Feature — Agent

**Prioridade**: Baixa

**Dependencias**: STORY-0004, STORY-0016

**Contexto tecnico**:
O compliance auditor foca em verificacao de conformidade regulatoria: coleta de evidencias,
analise de gaps, preparacao para auditorias. Diferente do security-engineer (code review)
e appsec-engineer (SDLC), foca exclusivamente em requisitos regulatorios.

**Escopo de implementacao**:

1. Criar `agents/conditional/compliance-auditor.md`

2. Checklist (15 items):
   1. Data Classification Completeness — Verificar se todos os dados estao classificados
   2. Consent Management — Verificar implementacao de gestao de consentimento
   3. Data Subject Rights — Verificar implementacao de direitos do titular
   4. Retention Policy — Verificar enforcement de politicas de retencao
   5. Audit Log Completeness — Verificar cobertura de audit logs
   6. Encryption Compliance — Verificar encryption at rest e in transit
   7. Access Control Compliance — Verificar RBAC e least privilege
   8. Incident Response Procedures — Verificar procedimentos de resposta a incidentes
   9. Vendor/Third-Party Compliance — Verificar compliance de terceiros
   10. Cross-Border Transfer Controls — Verificar controles de transferencia internacional
   11. Privacy Impact Assessment — Verificar PIA/DPIA
   12. Compliance Documentation — Verificar documentacao de compliance
   13. Evidence Package — Coletar pacote de evidencias para auditor
   14. Remediation Roadmap — Plano de remediacao para gaps identificados
   15. Compliance Score — Score de compliance por framework (GDPR, LGPD, PCI-DSS, etc.)

3. Ativacao: quando compliance frameworks estao configurados

**Criterios de Aceitacao (DoD)**:

- [ ] `agents/conditional/compliance-auditor.md` criado com 15-point checklist
- [ ] Scope nao sobrepoe com security-engineer ou appsec-engineer
- [ ] Ativacao quando compliance frameworks estao ativos
- [ ] Formato identico ao security-engineer.md

**Gherkin**:

```gherkin
Feature: Compliance Auditor Agent

  Cenario: Agent ativado quando compliance frameworks existem
    DADO um config com "compliance: [lgpd, pci-dss]"
    QUANDO o gerador executa
    ENTAO o arquivo agents/compliance-auditor.md e incluido

  Cenario: Checklist contem 15 items de compliance
    DADO o agent compliance-auditor.md gerado
    QUANDO o conteudo e lido
    ENTAO existem 15 items focados em conformidade regulatoria

  Cenario: Scope e exclusivamente regulatory
    DADO os agents compliance-auditor, security-engineer e appsec-engineer
    QUANDO os scopes sao comparados
    ENTAO compliance-auditor foca exclusivamente em regulatory evidence
    E NAO inclui code review ou SDLC
```

---

### STORY-0022: Security Review Integration Enhancement

**Titulo**: Enriquecimento da dimensao de seguranca no x-review com 15 items e scan references

**Tipo**: Enhancement — Skill Existente

**Prioridade**: Media

**Dependencias**: STORY-0018, STORY-0019, STORY-0020, STORY-0021

**Contexto tecnico**:
O `x-review` atual tem dimensao de seguranca com 10 items (/20). Com os novos scans disponiveis,
o security engineer no review pode referenciar resultados existentes e cobrir mais areas.

**Escopo de implementacao**:

1. Estender security dimension no `x-review` de 10 para 15 items:
   - Items 1-10: manter existentes
   - Item 11: Secret detection compliance (referenciar x-secret-scan results)
   - Item 12: Container security posture (referenciar x-container-scan results)
   - Item 13: Supply chain risk assessment (referenciar x-supply-chain-audit results)
   - Item 14: Hardening compliance (referenciar x-hardening-eval results)
   - Item 15: OWASP Top 10 coverage (referenciar x-owasp-scan results)

2. Secao "Scan Results Integration": se resultados existem em `results/security/`, referenciar

3. Score security sobe de /20 para /30 (15 items x 2 pontos)

**Criterios de Aceitacao (DoD)**:

- [ ] Security dimension estendida para 15 items no template do x-review
- [ ] 5 novos items referenciam resultados de scans quando disponiveis
- [ ] Score ajustado para /30
- [ ] Backward compatible: funciona sem resultados de scans (items marcados como "Not scanned")

**Gherkin**:

```gherkin
Feature: Security Review Integration Enhancement

  Cenario: Review com scan results disponiveis referencia-os
    DADO resultados de x-sast-scan e x-secret-scan em results/security/
    QUANDO x-review e executado (dimensao de seguranca)
    ENTAO os items 11-15 referenciam os resultados existentes
    E o score e calculado com base nos 15 items

  Cenario: Review sem scan results marca items como Not Scanned
    DADO nenhum resultado em results/security/
    QUANDO x-review e executado
    ENTAO items 11-15 sao marcados como "Not scanned — run x-{skill} first"
    E o score e calculado apenas com items 1-10

  Cenario: Score total e /30 com 15 items
    DADO todos os 15 items avaliados
    QUANDO o score e calculado
    ENTAO o maximo e 30 pontos (15 items x 2)
```

---

### STORY-0023: Security Baseline Rule Enhancement

**Titulo**: Adicao de secao "Automated Verification" ao 06-security-baseline.md

**Tipo**: Enhancement — Rule Template

**Prioridade**: Baixa

**Dependencias**: STORY-0005, STORY-0006

**Contexto tecnico**:
O `06-security-baseline.md` atual lista requisitos de seguranca mas nao indica como verifica-los
automaticamente. Adicionar secao mapeando cada requisito ao skill que o verifica.

**Escopo de implementacao**:

1. Adicionar secao `## Automated Verification` ao template `06-security-baseline.md`:

| Requisito | Skill de Verificacao | Comando |
|-----------|---------------------|---------|
| Input deserialization | x-sast-scan | `/x-sast-scan --scope owasp` |
| String escaping | x-sast-scan | `/x-sast-scan --scope owasp` |
| Path operations | x-sast-scan | `/x-sast-scan --scope owasp` |
| Hardcoded secrets | x-secret-scan | `/x-secret-scan --scope current` |
| Cryptographic RNG | x-sast-scan | `/x-sast-scan --scope owasp` |
| HTTP security headers | x-hardening-eval | `/x-hardening-eval --scope headers` |
| TLS configuration | x-hardening-eval | `/x-hardening-eval --scope tls` |

2. Secao e condicional: apenas incluida quando scanning skills estao habilitadas.

**Criterios de Aceitacao (DoD)**:

- [ ] Secao "Automated Verification" adicionada ao template
- [ ] Mapeamento cobre todos os requisitos existentes
- [ ] Secao e condicional (nao aparece se scanning nao habilitado)

**Gherkin**:

```gherkin
Feature: Security Baseline Rule Enhancement

  Cenario: Baseline com scanning habilitado inclui Automated Verification
    DADO um config com scanning.sast: true e scanning.secretScan: true
    QUANDO o gerador produz 06-security-baseline.md
    ENTAO a secao "Automated Verification" esta presente
    E lista comandos para verificar cada requisito

  Cenario: Baseline sem scanning NAO inclui Automated Verification
    DADO um config sem flags de scanning
    QUANDO o gerador produz 06-security-baseline.md
    ENTAO a secao "Automated Verification" NAO esta presente
    E o conteudo existente permanece inalterado
```

---

### STORY-0024: Security KP — Application Security Reference

**Titulo**: Criar security/references/application-security.md com OWASP Top 10 implementation guide

**Tipo**: Feature — Knowledge Pack Reference

**Prioridade**: Media

**Dependencias**: STORY-0004

**Contexto tecnico**:
O security KP referencia `application-security.md` mas o arquivo nao existe. Precisa ser criado
com guia de implementacao detalhado para cada OWASP Top 10 category, SANS Top 25 cross-references,
e exemplos de codigo por linguagem.

**Escopo de implementacao**:

1. Criar `security/references/application-security.md` com:
   - Para cada OWASP Top 10 (A01-A10): descricao, impacto, patterns de deteccao, remediacoes com codigo
   - Cross-reference SANS Top 25 -> OWASP Top 10
   - Exemplos de codigo seguro por {{LANGUAGE}} (input validation, parameterized queries, output encoding)
   - Tabela de ferramentas recomendadas por tipo de vulnerabilidade

**Criterios de Aceitacao (DoD)**:

- [ ] `application-security.md` criado com guia para todas as 10 categorias OWASP
- [ ] SANS Top 25 cross-references incluidos
- [ ] Exemplos de codigo usam placeholders {{LANGUAGE}}
- [ ] Referencia registrada no `security/SKILL.md` principal

**Gherkin**:

```gherkin
Feature: Application Security Reference

  Cenario: Cada categoria OWASP tem secao completa
    DADO o arquivo application-security.md gerado
    QUANDO as secoes sao contadas
    ENTAO existem 10 secoes (A01-A10)
    E cada secao contem: descricao, impacto, deteccao, remediacao com codigo

  Cenario: Cross-reference SANS Top 25 completo
    DADO o arquivo application-security.md
    QUANDO a tabela SANS e consultada
    ENTAO os 25 items do SANS estao mapeados para categorias OWASP

  Cenario: Exemplos de codigo usam template placeholders
    DADO o arquivo application-security.md
    QUANDO os blocos de codigo sao inspecionados
    ENTAO usam {{LANGUAGE}} como placeholder para linguagem
```

---

### STORY-0025: Security KP — Cryptography Reference

**Titulo**: Criar security/references/cryptography.md com TLS, key management e hashing

**Tipo**: Feature — Knowledge Pack Reference

**Prioridade**: Baixa

**Dependencias**: Nenhuma. Pode ser implementada em paralelo.

**Contexto tecnico**:
O security KP referencia `cryptography.md` mas o arquivo nao existe. Precisa ser criado com
guias de implementacao para TLS, key management, hashing e tokenization.

**Escopo de implementacao**:

1. Criar `security/references/cryptography.md` com:
   - TLS 1.3 configuration guide por framework ({{FRAMEWORK}})
   - Cipher suite selection (recommended, acceptable, deprecated)
   - Key management patterns (KMS integration, envelope encryption, rotation)
   - Hashing algorithm selection (argon2id para passwords, bcrypt fallback, SHA-256 para data integrity)
   - HMAC para message authentication
   - Field-level encryption patterns
   - Tokenization patterns (format-preserving, vault-based)

**Criterios de Aceitacao (DoD)**:

- [ ] `cryptography.md` criado com todas as secoes
- [ ] TLS config guide com cipher suites recommended/acceptable/deprecated
- [ ] Key management patterns com exemplos
- [ ] Hashing guide com decision tree (qual algoritmo usar quando)
- [ ] Referencia registrada no `security/SKILL.md`

**Gherkin**:

```gherkin
Feature: Cryptography Reference

  Cenario: TLS guide cobre versoes recomendadas
    DADO o arquivo cryptography.md gerado
    QUANDO a secao TLS e lida
    ENTAO TLS 1.3 e marcado como "recommended"
    E TLS 1.2 e marcado como "acceptable"
    E TLS 1.1 e 1.0 sao marcados como "deprecated"

  Cenario: Hashing guide recomenda argon2id para passwords
    DADO o arquivo cryptography.md
    QUANDO a secao de hashing e consultada para "password storage"
    ENTAO argon2id e recomendado como primeira opcao
    E bcrypt e listado como fallback aceitavel
    E MD5 e SHA-1 sao listados como "NEVER for passwords"

  Cenario: Cipher suites tem 3 categorias
    DADO o arquivo cryptography.md
    QUANDO a secao de cipher suites e lida
    ENTAO existem categorias: recommended, acceptable, deprecated
    E cada cipher suite tem justificativa
```

---

### STORY-0026: Security KP — Pentest Readiness Reference

**Titulo**: Criar security/references/pentest-readiness.md com checklists e remediation playbooks

**Tipo**: Feature — Knowledge Pack Reference

**Prioridade**: Baixa

**Dependencias**: STORY-0015

**Contexto tecnico**:
O security KP referencia `pentest-readiness.md` mas o arquivo nao existe. Precisa ser criado
com checklists de pre-pentest, vulnerability patterns por framework, e remediation playbooks.

**Escopo de implementacao**:

1. Criar `security/references/pentest-readiness.md` com:
   - Pre-pentest hardening checklist (30 items organizados por categoria)
   - Common vulnerability patterns por framework ({{FRAMEWORK}})
   - Remediation playbooks por tipo de vulnerabilidade (SQLi, XSS, CSRF, auth bypass, etc.)
   - Pentest scope definition template
   - Post-pentest remediation tracking template
   - Re-test verification checklist

**Criterios de Aceitacao (DoD)**:

- [ ] `pentest-readiness.md` criado com todas as secoes
- [ ] Pre-pentest checklist com 30 items categorizados
- [ ] Remediation playbooks para ao menos 10 tipos de vulnerabilidade
- [ ] Templates de scope e tracking incluidos
- [ ] Referencia registrada no `security/SKILL.md`

**Gherkin**:

```gherkin
Feature: Pentest Readiness Reference

  Cenario: Pre-pentest checklist tem 30 items
    DADO o arquivo pentest-readiness.md gerado
    QUANDO a secao pre-pentest checklist e contada
    ENTAO existem ao menos 30 items
    E estao organizados por categoria (auth, input, config, etc.)

  Cenario: Remediation playbook para SQL injection
    DADO o arquivo pentest-readiness.md
    QUANDO o playbook de SQL injection e consultado
    ENTAO contem: descricao do problema, impacto, remediacao com codigo, verificacao
    E o codigo usa {{LANGUAGE}} como placeholder

  Cenario: Scope template e preenchivel
    DADO o arquivo pentest-readiness.md
    QUANDO o scope template e lido
    ENTAO contem campos para: sistemas alvo, exclusoes, janela de teste, contatos, regras de engajamento
```

---

### STORY-0027: Security Anti-Patterns Rule (per language)

**Titulo**: Rule condicional 12-security-anti-patterns.md por language/framework

**Tipo**: Feature — Rule Template Condicional

**Prioridade**: Media

**Dependencias**: Nenhuma. Pode ser implementada em paralelo.

**Contexto tecnico**:
Similar ao `07-anti-patterns.md` (code quality anti-patterns), esta rule foca especificamente
em anti-patterns de SEGURANCA por language/framework. Cada anti-pattern tem codigo errado com
explicacao de vulnerabilidade e codigo correto com fix.

**Escopo de implementacao**:

1. Criar templates condicionais `rules/conditional/12-security-anti-patterns.md` por language:

**Java**:
- SQL concatenation em vez de parameterized query (SQL Injection)
- `Math.random()` para tokens de seguranca (Weak RNG)
- `ObjectInputStream.readObject()` sem whitelist (Deserialization)
- Hardcoded password em String literal (Credential Leak)
- `X509TrustManager` vazio (TLS Bypass)
- `new File(userInput)` sem path normalization (Path Traversal)
- `Response.ok(exception.getMessage())` (Information Disclosure)
- CORS com `allowedOrigins("*")` (CORS Misconfiguration)

**Python**:
- `pickle.loads(user_input)` (Deserialization RCE)
- `eval()` / `exec()` com input externo (Code Injection)
- `cursor.execute(f"SELECT * FROM users WHERE id={user_id}")` (SQL Injection)
- `jwt.decode(token, algorithms=['HS256'])` sem verify (JWT Bypass)
- `subprocess.call(f"ls {user_input}", shell=True)` (Command Injection)

**Go**:
- `http.ListenAndServe()` sem TLS (Cleartext Transport)
- Errors ignorados em crypto operations (Weak Crypto)
- `template.HTML(userInput)` (XSS)
- `sql.DB.Query("SELECT * FROM users WHERE id=" + id)` (SQL Injection)

**TypeScript/Node**:
- `eval(req.body.expression)` (Code Injection)
- Prototype pollution via `Object.assign({}, userInput)` (Prototype Pollution)
- ReDoS via regex com input nao-limitado (Denial of Service)
- `innerHTML = userInput` (DOM XSS)
- `jwt.verify(token, secret)` sem algorithm restriction (JWT Algorithm Confusion)

2. Gerado condicionalmente pelo `RulesAssembler` baseado em `language + framework`

**Criterios de Aceitacao (DoD)**:

- [ ] Templates criados para Java, Python, Go, TypeScript
- [ ] Cada anti-pattern tem: codigo errado, explicacao da vulnerabilidade, codigo correto, referencia CWE
- [ ] Java template contem ao menos 8 anti-patterns de seguranca
- [ ] Python template contem ao menos 5 anti-patterns
- [ ] Go template contem ao menos 4 anti-patterns
- [ ] TypeScript template contem ao menos 5 anti-patterns
- [ ] Geracao condicional por language/framework (isolamento entre linguagens)
- [ ] Anti-patterns Java NAO aparecem em output Python e vice-versa

**Gherkin**:

```gherkin
Feature: Security Anti-Patterns Rule

  Cenario: Java project gera anti-patterns de seguranca Java
    DADO um config com language "java"
    QUANDO o gerador executa
    ENTAO o arquivo 12-security-anti-patterns.md e gerado
    E contem "SQL concatenation" anti-pattern com codigo Java
    E NAO contem anti-patterns Python (pickle, eval)

  Cenario: Python project gera anti-patterns Python
    DADO um config com language "python"
    QUANDO o gerador executa
    ENTAO o arquivo contem "pickle.loads" anti-pattern
    E contem "eval()" anti-pattern
    E NAO contem anti-patterns Java

  Cenario: Cada anti-pattern tem codigo errado E correto
    DADO qualquer anti-pattern no arquivo gerado
    QUANDO o anti-pattern e lido
    ENTAO contem secao com codigo incorreto e explicacao da vulnerabilidade
    E contem secao com codigo correto e fix
    E contem referencia CWE

  Cenario: Anti-pattern sem fix causa falha de validacao
    DADO um template de anti-pattern sem bloco de codigo correto
    QUANDO o assembler processa
    ENTAO uma excecao e lancada indicando que fix e obrigatorio
```

---

### STORY-0028: Integration Verification e Smoke Test

**Titulo**: Verificacao de wiring completo e smoke test com todas as flags de seguranca

**Tipo**: Verification — Cross-Cutting

**Prioridade**: Alta (terminal — valida todo o epico)

**Dependencias**: STORY-0022, STORY-0023, STORY-0024, STORY-0025, STORY-0026, STORY-0027

**Contexto tecnico**:
Story terminal que verifica que todos os novos componentes estao corretamente integrados no
pipeline de geracao. Roda smoke test gerando ambiente para um projeto Java Spring com todas
as flags de seguranca habilitadas.

**Escopo de implementacao**:

1. Verificacoes de wiring:
   - Todas as novas skills registradas no `SkillsSelection`
   - Todos os novos agents registrados no `AgentsSelection`
   - Todos os novos KP references resolvem corretamente
   - `SecurityConfig.fromMap()` parseia todos os novos campos
   - Settings JSON merge funciona para todas as novas skills condicionais

2. Smoke test:
   - Gerar ambiente para um projeto Java Spring com config:
     ```yaml
     security:
       compliance: [pci-dss, lgpd]
       scanning:
         sast: true
         dast: true
         secretScan: true
         containerScan: true
         infraScan: true
       qualityGate:
         provider: sonarqube
       pentest: true
     infrastructure:
       container: docker
       orchestrator: kubernetes
     ```
   - Verificar que TODOS os novos arquivos estao presentes no output
   - Verificar que nenhum arquivo existente foi quebrado

3. Golden file update:
   - Adicionar golden files para o novo config profile
   - Parity test valida output completo

**Criterios de Aceitacao (DoD)**:

- [ ] Todas as skills condicionais sao geradas quando flags estao ativas
- [ ] Todos os agents condicionais sao gerados quando flags estao ativas
- [ ] KP references resolvem sem 404
- [ ] Backward compatibility: config YAML antigo gera output identico ao anterior
- [ ] Smoke test com all-flags-enabled gera output completo
- [ ] Line coverage >= 95%, branch coverage >= 90% para todo o codigo novo do epico
- [ ] Zero warnings no build

**Gherkin**:

```gherkin
Feature: Integration Verification

  Cenario: Config com todas as flags gera todos os componentes
    DADO um config YAML com todas as flags de seguranca habilitadas
    QUANDO o gerador executa
    ENTAO todos os 13 novos skills estao presentes no output
    E todos os 4 novos agents estao presentes
    E o knowledge pack owasp-asvs esta presente
    E os 3 security references estao presentes
    E o security anti-patterns rule esta presente

  Cenario: Config antigo sem novos campos gera output identico
    DADO um config YAML da versao anterior (sem campos de scanning)
    QUANDO o gerador executa
    ENTAO o output e identico ao output da versao anterior
    E nenhum novo arquivo e incluido

  Cenario: SkillsSelection retorna skills corretas
    DADO SecurityConfig com sast=true, dast=false, secretScan=true
    QUANDO selectSecurityScanningSkills() e invocado
    ENTAO retorna ["x-sast-scan", "x-secret-scan"]
    E NAO retorna "x-dast-scan"

  Cenario: Build completo sem warnings
    DADO o codigo do epico inteiro implementado
    QUANDO mvn verify e executado
    ENTAO zero warnings de compilacao
    E line coverage >= 95%
    E branch coverage >= 90%
```
