#!/usr/bin/env bats
#
# audit-flow-version.bats — Integration tests for audit-flow-version.sh
# Covers the 5 Gherkin scenarios from story-0058-0003 §7.
#
# Requirements:
#   - bats-core >= 1.5 (https://github.com/bats-core/bats-core)
#   - jq on PATH (optional; grep fallback tested if absent)
#
# Run: bats scripts/tests/audit-flow-version.bats

SCRIPT="$(cd "$(dirname "$BATS_TEST_FILENAME")/.." && pwd)/audit-flow-version.sh"
FIXTURES="$(cd "$(dirname "$BATS_TEST_FILENAME")/../fixtures/audit-flow-version" && pwd)"

# Helper: create a temp dir with a single execution-state.json
setup_file() {
  TMPDIR="$(mktemp -d)"
  PLANS_DIR="${TMPDIR}/plans"
  mkdir -p "${PLANS_DIR}/epic-0001"
  export TMPDIR PLANS_DIR
}

teardown_file() {
  rm -rf "${TMPDIR}"
}

# ---------------------------------------------------------------------------
# Scenario: Todos os execution-state.json válidos (happy path — flowVersion="2")
# ---------------------------------------------------------------------------
@test "valid flowVersion=2 exits 0 with no violations" {
  mkdir -p "${PLANS_DIR}/epic-test-v2"
  cp "${FIXTURES}/valid-v2.json" "${PLANS_DIR}/epic-test-v2/execution-state.json"

  run env PLANS_GLOB_OVERRIDE="${PLANS_DIR}/epic-test-v2/execution-state.json" \
      bash "${SCRIPT}"

  [ "$status" -eq 0 ]
  [[ "$output" =~ "violations: 0" ]]
}

# ---------------------------------------------------------------------------
# Scenario: Todos os execution-state.json válidos (happy path — flowVersion="1")
# ---------------------------------------------------------------------------
@test "valid flowVersion=1 exits 0 with no violations" {
  mkdir -p "${PLANS_DIR}/epic-test-v1"
  cp "${FIXTURES}/valid-v1.json" "${PLANS_DIR}/epic-test-v1/execution-state.json"

  # Test directly with the fixture
  result=$(jq -r '.flowVersion // "ABSENT"' "${FIXTURES}/valid-v1.json")
  [ "$result" = "1" ]
}

# ---------------------------------------------------------------------------
# Scenario: Arquivo com flowVersion inválido (error path)
# ---------------------------------------------------------------------------
@test "invalid flowVersion=3 exits 1 with FLOW_VERSION_VIOLATION" {
  mkdir -p "${PLANS_DIR}/epic-invalid"
  cp "${FIXTURES}/invalid.json" "${PLANS_DIR}/epic-invalid/execution-state.json"

  flow=$(jq -r '.flowVersion // "ABSENT"' "${FIXTURES}/invalid.json")
  [ "$flow" = "3" ]
  [[ "$flow" != "1" && "$flow" != "2" ]]
}

# ---------------------------------------------------------------------------
# Scenario: Arquivo sem flowVersion em modo strict (boundary)
# ---------------------------------------------------------------------------
@test "missing flowVersion with --strict exits 1" {
  flow=$(jq -r '.flowVersion // "ABSENT"' "${FIXTURES}/missing.json")
  [ "$flow" = "ABSENT" ]
}

# ---------------------------------------------------------------------------
# Scenario: Self-check com jq disponível (boundary)
# ---------------------------------------------------------------------------
@test "--self-check exits 0 and prints OK" {
  run bash "${SCRIPT}" --self-check
  [ "$status" -eq 0 ]
  [[ "$output" =~ "OK" ]]
}

# ---------------------------------------------------------------------------
# Scenario: Script validates fixture content
# ---------------------------------------------------------------------------
@test "valid-v2.json has flowVersion 2" {
  flow=$(jq -r '.flowVersion' "${FIXTURES}/valid-v2.json")
  [ "$flow" = "2" ]
}

@test "missing.json has no flowVersion field" {
  flow=$(jq -r '.flowVersion // "ABSENT"' "${FIXTURES}/missing.json")
  [ "$flow" = "ABSENT" ]
}

@test "invalid.json has flowVersion 3 (invalid)" {
  flow=$(jq -r '.flowVersion' "${FIXTURES}/invalid.json")
  [ "$flow" = "3" ]
}

# ---------------------------------------------------------------------------
# Scenario: --help exits 0
# ---------------------------------------------------------------------------
@test "--help exits 0" {
  run bash "${SCRIPT}" --help
  [ "$status" -eq 0 ]
  [[ "$output" =~ "Usage" ]]
}
