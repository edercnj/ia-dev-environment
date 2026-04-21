# Mapa de Implementação — EPIC-0047: Skill Body Compression Framework

**Gerado a partir das dependências BlockedBy/Blocks de cada história do epic-0047.**

---

## 1. Matriz de Dependências

| Story | Título | Chave Jira | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| story-0047-0001 | Diretório `_shared/` + ADR-0006 (estratégia de inclusão) | — | Bucket A mergeado + Sprint 2 medição | story-0047-0002, story-0047-0004 | Concluída |
| story-0047-0002 | Retirar pattern Slim Mode + ADR-0007 (flipped orientation) | — | story-0047-0001 | — | Pendente |
| story-0047-0003 | CI lint `SkillSizeLinter` (limite 500 LoC + `references/` sibling) | — | Bucket A mergeado | — | Pendente |
| story-0047-0004 | Sweep de compressão dos 5 maiores knowledge packs | — | story-0047-0001 | — | Pendente |

> **Valores de Status:** `Pendente` (padrão) · `Em Andamento` · `Concluída` · `Falha` · `Bloqueada` · `Parcial`

> **Nota:** As precondições externas — Bucket A mergeado (8 PRs do plano `mellow-mixing-rainbow.md`) e Sprint 2 de medição (`/cost` delta capturado em workflows reais) — não são stories deste épico. São trackeadas no DoR Local de cada story e no DoR Global do epic. STORY-0047-0003 é independente das outras (não bloqueada por 0001 nem 0002 nem 0004) e pode ser priorizada sozinha após Bucket A mergeado.

---

## 2. Fases de Implementação

> As histórias são agrupadas em fases. Dentro de cada fase, podem ser implementadas **em paralelo**. Uma fase só pode iniciar quando todas as dependências das fases anteriores estiverem concluídas.

```
╔══════════════════════════════════════════════════════════════════════════╗
║                FASE 0 — Foundation + Guard-rail (paralelo)              ║
║                                                                          ║
║   ┌───────────────────┐                  ┌───────────────────┐          ║
║   │ story-0047-0001     │  shared dir +   │ story-0047-0003     │          ║
║   │ ADR-0006            │  pilot pre-     │ SkillSizeLinter     │          ║
║   │ (cluster pre-commit)│  commit         │ (CI lint preventivo)│          ║
║   └────────┬──────────┘                  └───────────────────┘          ║
╚════════════╪═════════════════════════════════════════════════════════════╝
             │
             ▼
╔══════════════════════════════════════════════════════════════════════════╗
║                FASE 1 — Conteúdo (paralelo, após 0001)                  ║
║                                                                          ║
║   ┌───────────────────┐                  ┌───────────────────┐          ║
║   │ story-0047-0002     │                  │ story-0047-0004     │          ║
║   │ ADR-0007 + flip 5   │                  │ KP sweep (5 KPs    │          ║
║   │ skills ex-Slim Mode │                  │ maiores: click-cli,│          ║
║   │ → references/       │                  │ k8s-helm, axum,    │          ║
║   │ full-protocol.md    │                  │ terraform, dotnet) │          ║
║   └───────────────────┘                  └───────────────────┘          ║
║                                                                          ║
║   ─── Medição final em RULE-047-07 ao concluir fase 1 ──                 ║
╚══════════════════════════════════════════════════════════════════════════╝
```

---

## 3. Critical Path

A path crítica do épico é:

```
[Bucket A mergeado] → story-0047-0001 → story-0047-0002 → [medição final]
                  └→ story-0047-0003 (paralela)
                  story-0047-0001 → story-0047-0004 (paralela com 0002)
```

Story 0047-0001 é o gargalo (ADR-0006 + pilot). Após o merge de 0001, stories 0002 e 0004 paralelizam. Story 0047-0003 é independente e pode rodar em paralelo desde o início (após Bucket A).

**Estimativa de duração** (escala interna, não exata):
- Pré-condição: Bucket A do plano (~5 dias) + Sprint 2 medição (~2 dias)
- FASE 0: 0001 (~3 dias com ADR + pilot) || 0003 (~2 dias)
- FASE 1: 0002 (~5 dias × 5 skills) || 0004 (~7 dias × 5 KPs)
- Total epic: ~15-18 dias úteis após Bucket A pronto

---

## 4. Critérios de Conclusão do Épico

EPIC-0047 está **Concluído** quando:

- [ ] Todas as 4 stories estão `Concluída`
- [ ] ADR-0006 + ADR-0007 mergeadas como `Accepted`
- [ ] `_shared/` ativo com snippets reusáveis
- [ ] `SkillSizeLinter` rodando em `mvn test` default scope
- [ ] 5 skills ex-Slim Mode com SKILL.md slim + `references/full-protocol.md`
- [ ] 5 KPs maiores com SKILL.md ≤ 250 LoC + `references/examples-*.md`
- [ ] `mvn test -Dtest=SkillCorpusSizeAudit` afirma `total_lines < 30_000` (RULE-047-07)
- [ ] §6 do `epic-0047.md` (Histórico de Medições) atualizado com baseline + pós-Bucket-A + pós-EPIC-0047
- [ ] CHANGELOG `[Unreleased]` contém entry agregada do épico
- [ ] Goldens dos 17 perfis regenerados; smoke `Epic0047CompressionSmokeTest` verde
- [ ] Se delta final < −20% vs baseline, issue de investigação aberta

---

## 5. Dependências Externas

| Externa | Bloqueia | Mitigação |
| :--- | :--- | :--- |
| Bucket A do plano `mellow-mixing-rainbow.md` (8 PRs) | toda fase 0 | Sequenciar: aguardar v3.7.0 cortada antes de iniciar 0047 |
| Sprint 2 de medição (`/cost` delta) | story-0047-0002 (precisa baseline para validar flip) | Documentar delta na §6 do epic; STORY-0047-0001/0003 podem rodar em paralelo com a medição |
| EPIC-0036 (skill taxonomy refactor — ADR-0003 ainda Proposed) | indireto: renames podem afetar paths | Coordenar: se EPIC-0036 mergear primeiro, rebase 0047 stories nos novos paths |
| EPIC-0042 merge-train (em flight) | nenhum direto | KPs e orchestrators tocados são disjuntos do escopo merge-train |
| EPIC-0043 interactive gates | nenhum direto | — |
| EPIC-0044 deprecated removal (Java) | nenhum direto | Java cleanup não afeta SKILL.md fonte |
| EPIC-0045 CI Watch (mergeado) | nenhum direto | — |
| EPIC-0046 (em curso, escopo ainda não mapeado para mim) | TBD | Confirmar com maintainer se há overlap antes de iniciar 0047 |
