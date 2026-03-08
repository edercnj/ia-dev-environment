# História: Hooks (.github/hooks/*.json)

**ID:** STORY-011

## 1. Dependências

| Blocked By | Blocks |
| :--- | :--- |
| STORY-010 | STORY-013 |

## 2. Regras Transversais Aplicáveis

| ID | Título |
| :--- | :--- |
| RULE-001 | Paridade funcional |
| RULE-002 | Convenções do Copilot |
| RULE-007 | Consistência de hooks |

## 3. Descrição

Como **DevOps Engineer**, eu quero criar `.github/hooks/*.json` com hooks determinísticos em formato JSON, garantindo que os mesmos pontos de verificação cobertos por `.claude/hooks/post-compile-check.sh` existam no Copilot, além de hooks adicionais para lint e context loading.

Os hooks dependem dos agents (STORY-010) porque executam no workflow dos agents. O formato JSON substitui os shell scripts diretos usados no Claude Code, seguindo as convenções do Copilot.

### 3.1 Hooks a implementar

| Hook | Event | Matcher | Comando | Timeout |
| :--- | :--- | :--- | :--- | :--- |
| post-compile-check | postToolUse | `{ "tool": "edit_file" }` | `scripts/post-compile-check.sh` | 60000ms |
| pre-commit-lint | preToolUse | `{ "tool": "git_commit" }` | `scripts/pre-commit-lint.sh` | 30000ms |
| session-context-loader | sessionStart | — | `scripts/load-context.sh` | 10000ms |

### 3.2 Formato JSON

```json
{
  "hooks": [
    {
      "event": "postToolUse",
      "matcher": { "tool": "edit_file" },
      "command": "scripts/post-compile-check.sh",
      "timeout": 60000,
      "description": "Verify compilation after file edits"
    }
  ]
}
```

### 3.3 Paridade com .claude/hooks/

- `post-compile-check.sh` existe em `.claude/hooks/` e deve ter equivalente funcional
- Hooks adicionais expandem a cobertura para lint e session start

## 4. Definições de Qualidade Locais

### DoR Local (Definition of Ready)

- [ ] STORY-010 concluída (agents disponíveis)
- [ ] Hook `.claude/hooks/post-compile-check.sh` lido
- [ ] Formato JSON de hooks validado com Copilot docs

### DoD Local (Definition of Done)

- [ ] 3 hooks criados em formato JSON válido
- [ ] post-compile-check equivalente ao existente em .claude/hooks/
- [ ] Timeouts configurados e documentados
- [ ] JSON parseável sem erros

### Global Definition of Done (DoD)

- **Validação de formato:** JSON válido e parseável
- **Convenções Copilot:** Event types válidos, matcher correto
- **Consistência:** Paridade com hooks .claude/ existentes
- **Performance:** Timeouts ≤ 60s
- **Documentação:** README.md atualizado

## 5. Contratos de Dados (Data Contract)

**Hook Definition Contract:**

| Campo | Formato | Request | Response | Origem / Regra |
| :--- | :--- | :--- | :--- | :--- |
| `hooks[].event` | enum(sessionStart, postToolUse, preToolUse, etc.) | M | — | Tipo de evento |
| `hooks[].matcher` | object | O | — | Filtro de tool/evento |
| `hooks[].command` | string (path) | M | — | Script a executar |
| `hooks[].timeout` | integer (ms) | M | — | Timeout máximo (≤ 60000) |
| `hooks[].description` | string | M | — | Descrição do propósito |

## 6. Diagramas

### 6.1 Fluxo de Hook post-compile-check

```mermaid
sequenceDiagram
    participant U as Usuário
    participant C as Copilot Runtime
    participant H as Hook (postToolUse)
    participant S as post-compile-check.sh

    U->>C: Solicitar edição de arquivo
    C->>C: Executar edit_file
    C->>H: Disparar hook postToolUse
    H->>S: Executar post-compile-check.sh
    S-->>H: Resultado (pass/fail)
    H-->>C: Feedback ao Copilot
    C-->>U: Resultado da compilação
```

## 7. Critérios de Aceite (Gherkin)

```gherkin
Cenario: JSON válido para hooks
  DADO que .github/hooks/post-compile-check.json foi criado
  QUANDO um parser JSON processa o arquivo
  ENTÃO o parse é bem-sucedido
  E o array "hooks" contém pelo menos 1 hook

Cenario: Hook post-compile-check dispara após edit_file
  DADO que o hook está configurado com event "postToolUse" e matcher "edit_file"
  QUANDO o Copilot executa uma edição de arquivo
  ENTÃO o hook post-compile-check.sh é executado
  E o resultado (pass/fail) é reportado ao Copilot

Cenario: Paridade com hook .claude existente
  DADO que .claude/hooks/post-compile-check.sh existe
  QUANDO o hook equivalente é criado em .github/hooks/
  ENTÃO o comando referencia o mesmo script ou equivalente funcional
  E o timeout é ≤ 60000ms

Cenario: Hook com timeout excedido
  DADO que session-context-loader tem timeout de 10000ms
  QUANDO o script demora mais de 10 segundos
  ENTÃO o hook é cancelado por timeout
  E o Copilot continua sem o contexto adicional

Cenario: Hook pre-commit-lint bloqueia commit inválido
  DADO que pre-commit-lint está configurado com event "preToolUse" e matcher "git_commit"
  QUANDO o código tem violations de lint
  ENTÃO o hook reporta falha
  E o commit é bloqueado até correção
```

## 8. Sub-tarefas

- [ ] [Dev] Criar `.github/hooks/post-compile-check.json` com hook postToolUse
- [ ] [Dev] Criar `.github/hooks/pre-commit-lint.json` com hook preToolUse
- [ ] [Dev] Criar `.github/hooks/session-context-loader.json` com hook sessionStart
- [ ] [Dev] Criar scripts auxiliares se necessário (ou referenciar existentes)
- [ ] [Test] Validar JSON de todos os 3 hooks
- [ ] [Test] Verificar event types e matchers válidos
- [ ] [Test] Testar timeout de cada hook
- [ ] [Doc] Documentar hooks no README
