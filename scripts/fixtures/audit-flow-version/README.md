# Fixtures — audit-flow-version.sh

Test fixtures for `scripts/audit-flow-version.sh` (story-0058-0003).

| File | flowVersion | Expected behavior |
| :--- | :--- | :--- |
| `valid-v1.json` | `"1"` | No violation, exit 0 |
| `valid-v2.json` | `"2"` | No violation, exit 0 |
| `missing.json` | _(absent)_ | Warning only (non-strict), exit 0; violation in --strict mode, exit 1 |
| `invalid.json` | `"3"` | `FLOW_VERSION_VIOLATION`, exit 1 |

These fixtures are used by `scripts/tests/audit-flow-version.bats`.
