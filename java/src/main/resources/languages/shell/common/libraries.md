# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Shell/Bash Libraries and Tools

## Mandatory

| Tool        | Purpose    | Justification                                      |
| ----------- | ---------- | -------------------------------------------------- |
| shellcheck  | Linting    | Static analysis for shell scripts, catches bugs    |
| shfmt       | Formatting | Consistent formatting, enforces style              |

### ShellCheck

```bash
# Run on all scripts
find . -name "*.sh" -exec shellcheck --severity=warning {} +

# Inline directives (use sparingly, with justification)
# shellcheck disable=SC2059  # printf format string is intentionally variable
printf "${format_string}" "${value}"

# .shellcheckrc (project root)
severity=warning
shell=bash
enable=all
```

### shfmt

```bash
# Format all scripts (indent=4, binary ops at start of line)
shfmt -i 4 -bn -w .

# Check without modifying
shfmt -i 4 -bn -d .

# CI integration
shfmt -i 4 -bn -l . | head -1 && echo "Formatting issues found" && exit 1
```

## Recommended

| Tool         | Purpose           | When to Use                                     |
| ------------ | ----------------- | ----------------------------------------------- |
| bats-core    | Testing framework | Structured test suites for shell libraries      |
| bats-assert  | Test assertions   | Richer assertions in bats tests                 |
| bats-file    | File assertions   | File existence/content checks in bats tests     |
| jq           | JSON processing   | Parsing/transforming JSON from APIs or configs   |
| yq           | YAML processing   | Parsing/transforming YAML configuration files    |
| envsubst     | Template rendering| Substituting env vars in config templates        |
| mktemp       | Temp files        | Creating temporary files/directories safely      |
| curl         | HTTP client       | API calls, file downloads (always with `-f`)     |

### jq

```bash
# Extract field
local version
version=$(jq -r '.version' package.json)

# Filter array
jq -r '.items[] | select(.status == "active") | .name' data.json

# Build JSON
jq -n --arg name "${NAME}" --arg ver "${VERSION}" \
    '{"name": $name, "version": $ver}'
```

### yq

```bash
# Read YAML value
local replicas
replicas=$(yq '.spec.replicas' deployment.yaml)

# Update YAML in-place
yq -i '.spec.replicas = 3' deployment.yaml
```

### envsubst

```bash
# Template rendering
export APP_NAME="my-service"
export APP_PORT="8080"
envsubst < template.yaml > output.yaml

# Specific variables only
envsubst '${APP_NAME} ${APP_PORT}' < template.yaml > output.yaml
```

### curl

```bash
# CORRECT - always use -f (fail on HTTP errors), -sS (silent + show errors)
curl -fsSL "https://example.com/install.sh" -o install.sh

# CORRECT - API call with error handling
local response
response=$(curl -fsSL -H "Authorization: Bearer ${TOKEN}" \
    "https://api.example.com/resource") \
    || { log_error "API call failed"; return 1; }

# FORBIDDEN - curl without -f (silent failure on 404/500)
curl -sL "https://example.com/data"
```

## Prohibited

| Pattern/Tool          | Reason                                   | Alternative                     |
| --------------------- | ---------------------------------------- | ------------------------------- |
| `eval`                | Code injection risk                      | Direct variable assignment      |
| `source` untrusted    | Arbitrary code execution                 | Parse config with `read`/`grep` |
| `curl` without `-f`   | Silent failure on HTTP errors            | `curl -fsSL`                    |
| `wget`                | Inconsistent across platforms            | `curl`                          |
| `awk` for simple ops  | Overkill for single-field extraction     | Parameter expansion or `cut`    |
| `bc` for integer math | Unnecessary dependency                   | `$(( ))` arithmetic             |
| `seq`                 | Non-portable                             | `{1..N}` brace expansion        |
| `echo -e`             | Non-portable escape handling             | `printf`                        |
| Backticks `` `cmd` `` | Cannot nest, harder to read              | `$(cmd)`                        |
| `which`               | Inconsistent behavior across systems     | `command -v`                    |
| `realpath`            | Not available on all systems             | `readlink -f` or manual resolve |

## Portability Notes

- **Minimum bash version**: 4.3+ (associative arrays, `nameref`, `mapfile`)
- **Command existence check**: `command -v tool &>/dev/null` (not `which`)
- **Shebang**: `#!/usr/bin/env bash` (finds bash in PATH, not hardcoded)
- **POSIX mode**: Do NOT write for POSIX sh; use bash features intentionally
- **macOS**: Default bash is 3.2; scripts requiring 4.3+ must document this

## Security

- Never store secrets in script variables visible to `ps`
- Use `mktemp` for temporary files (never predictable names in `/tmp`)
- Validate all external input before use in commands
- Use `--` to terminate option parsing: `rm -- "${filename}"`
- Quote all variables to prevent word splitting and globbing
