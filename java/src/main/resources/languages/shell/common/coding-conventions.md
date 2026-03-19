# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Shell/Bash Coding Conventions

## Style Enforcement

- **shellcheck** mandatory (all scripts must pass with zero warnings)
- **shfmt** mandatory (indent=4, binary operators at start of line)
- Shebang: `#!/usr/bin/env bash` (never `#!/bin/sh` or `#!/bin/bash`)
- Safety header: `set -euo pipefail` on every script (immediately after shebang)

```bash
#!/usr/bin/env bash
set -euo pipefail
```

## Naming Conventions

| Element              | Convention          | Example                        |
| -------------------- | ------------------- | ------------------------------ |
| Function             | verb_noun snake_case| `install_dependency()`         |
| Global variable      | UPPER_SNAKE_CASE    | `PROJECT_ROOT`                 |
| Local variable       | snake_case + `local`| `local file_path`              |
| Constant             | readonly UPPER_SNAKE| `readonly MAX_RETRIES=3`       |
| Script filename      | kebab-case.sh       | `run-tests.sh`                 |
| Array variable       | PLURAL_UPPER        | `REQUIRED_TOOLS`               |
| Boolean variable     | IS_/HAS_ prefix     | `IS_VERBOSE`, `HAS_DOCKER`    |
| Environment variable | UPPER_SNAKE_CASE    | `DATABASE_URL`                 |

## Variable Handling

Always quote variables. No exceptions.

```bash
# CORRECT - double-quoted variables
echo "${file_path}"
cp "${source}" "${destination}"

# CORRECT - array expansion
for item in "${REQUIRED_TOOLS[@]}"; do
    check_tool "${item}"
done

# CORRECT - default values
local timeout="${TIMEOUT:-30}"
local config_dir="${CONFIG_DIR:-/etc/myapp}"

# CORRECT - required variables (fail fast)
local database_url="${DATABASE_URL:?DATABASE_URL must be set}"

# FORBIDDEN - unquoted variables
echo $file_path
cp $source $destination
```

## Function Design

- Maximum 25 lines per function
- Maximum 4 parameters (use associative arrays or globals for more)
- `local` keyword for ALL variables inside functions
- Return data via stdout; return status via exit code
- Document parameters with a comment block for non-trivial functions

```bash
# CORRECT - well-structured function
install_tool() {
    local tool_name="$1"
    local version="${2:-latest}"
    local install_dir="${3:-/usr/local/bin}"

    if command -v "${tool_name}" &>/dev/null; then
        log_info "${tool_name} already installed"
        return 0
    fi

    log_info "Installing ${tool_name} ${version}..."
    curl -fsSL "https://example.com/${tool_name}/${version}" \
        -o "${install_dir}/${tool_name}" \
        || { log_error "Failed to install ${tool_name}"; return 1; }

    chmod +x "${install_dir}/${tool_name}"
    log_success "${tool_name} ${version} installed"
}

# FORBIDDEN - function without local, too long, no error handling
do_everything() {
    result=$(some_command)  # missing local
    # ... 50+ lines ...
}
```

## Error Handling

- Use `|| { log_error "context"; exit 1; }` pattern for critical operations
- Use `trap` for cleanup on exit (normal and error)
- Error messages MUST include context (what failed, with which values)
- Exit codes: `0` = success, `1` = general error, `2` = usage error

```bash
# CORRECT - trap for cleanup
cleanup() {
    local exit_code=$?
    rm -rf "${TEMP_DIR:-}"
    exit "${exit_code}"
}
trap cleanup EXIT

# CORRECT - error handling with context
create_directory() {
    local dir_path="$1"
    mkdir -p "${dir_path}" \
        || { log_error "Failed to create directory: ${dir_path}"; return 1; }
}

# CORRECT - validation with usage error
parse_arguments() {
    if [[ $# -lt 1 ]]; then
        log_error "Usage: $(basename "$0") <config-file> [--verbose]"
        exit 2
    fi
}
```

## Logging

Color-coded logging functions with consistent format. Reset color with `NC`.

```bash
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${NC} $*"; }
log_success() { echo -e "${GREEN}[OK]${NC} $*"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $*" >&2; }
log_pass()    { echo -e "${GREEN}[PASS]${NC} $*"; }
log_fail()    { echo -e "${RED}[FAIL]${NC} $*" >&2; }
```

## Conditionals

- Use `[[ ]]` for string/file tests (never `[ ]` or `test`)
- Use `(( ))` for arithmetic comparisons
- Always quote variables inside conditionals

```bash
# CORRECT - double brackets
if [[ -f "${config_file}" ]]; then
    source "${config_file}"
fi

if [[ "${status}" == "active" ]]; then
    process_active
fi

# CORRECT - arithmetic
if (( retry_count >= MAX_RETRIES )); then
    log_error "Max retries exceeded"
    exit 1
fi

# FORBIDDEN - single brackets
if [ -f $config_file ]; then  # wrong: [ ] and unquoted
    source $config_file
fi
```

## Arrays

```bash
# Declaration
declare -a REQUIRED_TOOLS=("curl" "jq" "docker" "kubectl")

# Iteration
for tool in "${REQUIRED_TOOLS[@]}"; do
    command -v "${tool}" &>/dev/null \
        || { log_error "Missing: ${tool}"; exit 1; }
done

# Append
REQUIRED_TOOLS+=("helm")

# Length
log_info "Checking ${#REQUIRED_TOOLS[@]} tools..."

# Associative arrays (bash 4+)
declare -A STATUS_MAP=(
    ["success"]="0"
    ["failure"]="1"
    ["skipped"]="2"
)

# Membership check
if [[ -v STATUS_MAP["${key}"] ]]; then
    echo "Found: ${STATUS_MAP[${key}]}"
fi
```

## Text Processing

Prefer parameter expansion over external commands for simple operations.

```bash
# CORRECT - parameter expansion
local filename="${filepath##*/}"        # basename
local directory="${filepath%/*}"        # dirname
local extension="${filename##*.}"       # extension
local name_only="${filename%.*}"        # name without extension
local upper="${value^^}"                # uppercase
local lower="${value,,}"                # lowercase
local replaced="${text//old/new}"       # global replace
local trimmed="${value#"${value%%[![:space:]]*}"}"  # trim leading whitespace

# CORRECT - pipe chains for complex processing
find "${src_dir}" -name "*.sh" -print0 \
    | xargs -0 shellcheck --severity=warning

# CORRECT - sed for multi-line or complex substitution
sed -i "s|__PLACEHOLDER__|${actual_value}|g" "${config_file}"

# FORBIDDEN - unnecessary external commands
cat file | grep pattern       # UUOC: use grep pattern file
echo "$var" | sed 's/a/b/'   # use ${var//a/b}
```

## File Operations

```bash
# CORRECT - temp files with cleanup
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TEMP_DIR}"' EXIT

local temp_file
temp_file="$(mktemp "${TEMP_DIR}/output.XXXXXX")"

# CORRECT - process substitution
while IFS= read -r line; do
    process_line "${line}"
done < <(grep -v "^#" "${config_file}")

# CORRECT - safe file reading
if [[ -r "${input_file}" ]]; then
    while IFS= read -r line || [[ -n "${line}" ]]; do
        echo "${line}"
    done < "${input_file}"
fi
```

## Anti-Patterns (FORBIDDEN)

- `eval` for any purpose (code injection risk)
- Unquoted variable expansions (`$var` instead of `"${var}"`)
- `#!/bin/sh` shebang (use `#!/usr/bin/env bash`)
- Parsing `ls` output (use globbing or `find`)
- Useless use of `cat` (`cat file | grep` instead of `grep file`)
- Backtick command substitution (`` `cmd` `` instead of `$(cmd)`)
- `test` or `[ ]` instead of `[[ ]]`
- Global variables without `readonly`
- Functions without `local` for internal variables
- `cd` without error handling (`cd dir` instead of `cd dir || exit 1`)
- `echo -e` for portability-sensitive output (use `printf`)
- Hardcoded paths instead of variables or `$(dirname "$0")`
