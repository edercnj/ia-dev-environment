## 1. Matriz de Dependencias

| Story | Titulo | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- |
| story-0042-0001 | Root A | - | story-0042-0003 | Pendente |
| story-0042-0002 | Root B | - | story-0042-0004 | Pendente |
| story-0042-0003 | Mid A | story-0042-0001 | story-0042-0005 | Pendente |
| story-0042-0004 | Mid B | story-0042-0002 | - | Pendente |
| story-0042-0005 | Leaf | story-0042-0003 | - | Pendente |

## 5. Resumo por Fase

| Fase | Historias | Camada | Paralelismo | Pre-requisito |
| :--- | :--- | :--- | :--- | :--- |
| 0 | 0001, 0002 | Foundation | 2 paralelas | - |
| 1 | 0003, 0004 | Core | 2 paralelas | Fase 0 |
| 2 | 0005 | Extension | 1 | Fase 1 |
