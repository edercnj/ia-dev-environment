# Task: Add greet(name) to Greeter class

**ID:** TASK-0038-0003-EXAMPLE
**Story:** story-0038-0003
**Status:** Pendente

## 1. Objetivo

Add a `greet(String name)` method to `Greeter` returning `"Hello, {name}!"` for
non-empty input and throwing `IllegalArgumentException` on null/blank. Unit of work
is a single atomic commit (method + test).

## 2. Contratos I/O

### 2.1 Inputs
- `Greeter.java` exists in `example/greet/` package (empty class)
- JUnit 5 + AssertJ available in the test classpath

### 2.2 Outputs
- `Greeter.greet(String)` method exists (verifiable via grep)
- `GreeterTest.greet_validName_returnsGreeting` passes
- `mvn compile` green

### 2.3 Testabilidade

- [x] Independentemente testável
- [ ] Requer mock de TASK-XXXX-YYYY-NNN
- [ ] Coalescível com TASK-XXXX-YYYY-NNN

## 3. Definition of Done

- [ ] Method implemented
- [ ] Test covers both valid and invalid input
- [ ] `mvn compile` green
- [ ] Red → Green → Refactor walked consciously
- [ ] Contracts I/O respected (grep verifiable)
- [ ] Atomic commit in Conventional Commits form

## 4. Dependências

| Depends on | Relação | Pode mockar? |
| :--- | :--- | :--- |
| — | — | — |

## 5. Plano de Implementação

See `plan-task-TASK-0038-0003-EXAMPLE.md` (produced by `x-task-plan`).
