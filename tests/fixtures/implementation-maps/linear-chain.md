## 1. Matriz de Dependencias

| Story | Titulo | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- |
| story-0042-0001 | First Story | - | story-0042-0002 | Pendente |
| story-0042-0002 | Second Story | story-0042-0001 | story-0042-0003 | Pendente |
| story-0042-0003 | Third Story | story-0042-0002 | - | Pendente |

## 5. Resumo por Fase

| Fase | Historias | Camada | Paralelismo | Pre-requisito |
| :--- | :--- | :--- | :--- | :--- |
| 0 | 0001 | Foundation | 1 | - |
| 1 | 0002 | Core | 1 | Fase 0 |
| 2 | 0003 | Extension | 1 | Fase 1 |
