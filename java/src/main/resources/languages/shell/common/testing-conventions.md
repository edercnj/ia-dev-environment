# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Shell/Bash Testing Conventions

## Frameworks

- **bats-core** for structured test suites (preferred for libraries and complex scripts)
- **Counter-based runners** for project-internal test scripts (project convention)
- Test files: `test-*.sh` or `*.bats` in a `tests/` directory

## Coverage Thresholds

| Metric              | Minimum |
| ------------------- | ------- |
| Path Coverage       | >= 90%  |
| Error Path Tests    | >= 95%  |

## Naming Convention

```
test_[function]_[scenario]_[expected]
```

```bash
test_install_tool_missing_binary_installs_successfully
test_parse_config_empty_file_returns_error
test_validate_input_valid_args_returns_zero
```

## Counter-Based Test Pattern (Project Convention)

```bash
#!/usr/bin/env bash
set -euo pipefail

PASS=0
FAIL=0
TOTAL=0

assert_equals() {
    local description="$1"
    local expected="$2"
    local actual="$3"
    ((TOTAL++))

    if [[ "${expected}" == "${actual}" ]]; then
        echo -e "\033[0;32m[PASS]\033[0m ${description}"
        ((PASS++))
    else
        echo -e "\033[0;31m[FAIL]\033[0m ${description}"
        echo "  Expected: ${expected}"
        echo "  Actual:   ${actual}"
        ((FAIL++))
    fi
}

assert_contains() {
    local description="$1"
    local haystack="$2"
    local needle="$3"
    ((TOTAL++))

    if [[ "${haystack}" == *"${needle}"* ]]; then
        echo -e "\033[0;32m[PASS]\033[0m ${description}"
        ((PASS++))
    else
        echo -e "\033[0;31m[FAIL]\033[0m ${description}"
        echo "  Expected to contain: ${needle}"
        echo "  Actual: ${haystack}"
        ((FAIL++))
    fi
}

assert_file_exists() {
    local description="$1"
    local file_path="$2"
    ((TOTAL++))

    if [[ -f "${file_path}" ]]; then
        echo -e "\033[0;32m[PASS]\033[0m ${description}"
        ((PASS++))
    else
        echo -e "\033[0;31m[FAIL]\033[0m ${description}"
        echo "  File not found: ${file_path}"
        ((FAIL++))
    fi
}

# --- Test cases ---

test_install_tool_missing_binary_installs_successfully() {
    # setup, execute, assert
    assert_equals "installs missing tool" "0" "$?"
}

# --- Run tests ---

test_install_tool_missing_binary_installs_successfully

# --- Summary ---

echo ""
echo "Results: ${PASS} passed, ${FAIL} failed, ${TOTAL} total"

if ((FAIL > 0)); then
    exit 1
fi
```

## Bats Test Pattern

```bash
#!/usr/bin/env bats

setup() {
    TEST_DIR="$(mktemp -d)"
    export TEST_DIR
    # Source the script under test
    source "${BATS_TEST_DIRNAME}/../src/my-script.sh"
}

teardown() {
    rm -rf "${TEST_DIR}"
}

@test "install_tool: missing binary installs successfully" {
    run install_tool "jq" "1.7"
    [ "$status" -eq 0 ]
    [[ "$output" == *"installed"* ]]
}

@test "install_tool: existing binary skips installation" {
    run install_tool "bash"
    [ "$status" -eq 0 ]
    [[ "$output" == *"already installed"* ]]
}

@test "parse_config: empty file returns error" {
    touch "${TEST_DIR}/empty.conf"
    run parse_config "${TEST_DIR}/empty.conf"
    [ "$status" -eq 1 ]
    [[ "$output" == *"empty"* ]]
}

@test "validate_input: valid arguments returns zero" {
    run validate_input "config.yaml" "--verbose"
    [ "$status" -eq 0 ]
}
```

## Test Isolation

- Create a fresh `mktemp -d` per test; clean up in teardown/trap
- No dependency on test execution order
- No mutation of external state (files, env vars, network)
- Restore original `PATH` and environment after mocking
- Each test must be independently runnable

```bash
# CORRECT - isolated temp directory
setup() {
    ORIG_PATH="${PATH}"
    TEST_DIR="$(mktemp -d)"
}

teardown() {
    rm -rf "${TEST_DIR}"
    PATH="${ORIG_PATH}"
}
```

## Mocking External Commands

Use `PATH` override to intercept external commands.

```bash
# Mock 'docker' command
setup() {
    TEST_DIR="$(mktemp -d)"
    MOCK_DIR="$(mktemp -d)"

    cat > "${MOCK_DIR}/docker" <<'MOCK'
#!/usr/bin/env bash
echo "mock-docker: $*"
exit 0
MOCK
    chmod +x "${MOCK_DIR}/docker"

    ORIG_PATH="${PATH}"
    PATH="${MOCK_DIR}:${PATH}"
}

teardown() {
    PATH="${ORIG_PATH}"
    rm -rf "${TEST_DIR}" "${MOCK_DIR}"
}

@test "deploy: calls docker build" {
    run deploy "my-image"
    [ "$status" -eq 0 ]
    [[ "$output" == *"mock-docker: build"* ]]
}
```

## Test Helpers

```bash
# CORRECT - reusable helper with descriptive name
create_test_config() {
    local dir="$1"
    local name="${2:-test.conf}"
    cat > "${dir}/${name}" <<EOF
key=value
timeout=30
EOF
    echo "${dir}/${name}"
}

# CORRECT - wait-for pattern (no sleep)
wait_for_file() {
    local file_path="$1"
    local timeout="${2:-10}"
    local elapsed=0

    while [[ ! -f "${file_path}" ]] && ((elapsed < timeout)); do
        sleep 0.1
        ((elapsed++)) || true
    done

    [[ -f "${file_path}" ]]
}
```

## Anti-Patterns

- Leaked temp files (missing cleanup in teardown/trap)
- `sleep`-based waits without timeout or condition check
- Tests requiring network access to external services
- Hardcoded absolute paths (`/home/user/...` instead of `${TEST_DIR}/...`)
- Tests depending on execution order
- Testing implementation details instead of behavior
- Missing error path tests (only testing happy path)
- Using `set +e` globally instead of `run` for expected failures
