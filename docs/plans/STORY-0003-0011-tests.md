# Test Plan — STORY-0003-0011

## Test Strategy

Golden file byte-for-byte parity (existing test infrastructure in `tests/node/integration/byte-for-byte.test.ts`).

## Test Scenarios

### 1. Quality checklist includes TDD validation (Gherkin: Cenario 1)

**Type:** Golden file content validation
**Verify:** All 8 profile golden files contain "mandatory categories" in the quality checklist
**Profiles:** go-gin, java-quarkus, java-spring, kotlin-ktor, python-click-cli, python-fastapi, rust-axum, typescript-nestjs
**Locations:** `.claude/skills/x-story-epic-full/SKILL.md`, `.github/skills/x-story-epic-full/SKILL.md`, `.agents/skills/x-story-epic-full/SKILL.md`

### 2. Phase B mentions TDD in DoD (Gherkin: Cenario 2)

**Type:** Golden file content validation
**Verify:** Phase B description contains reference to TDD Compliance in DoD

### 3. Phase C mentions enriched Gherkin (Gherkin: Cenario 3)

**Type:** Golden file content validation
**Verify:** Phase C description mentions mandatory categories and TPP ordering

### 4. Workflow preserved (Gherkin: Cenario 4)

**Type:** Golden file structural validation
**Verify:** All 4 phases (A, B, C, D) remain present, none removed or renamed

### 5. Dual copy consistency (Gherkin: Cenario 5)

**Type:** Content comparison
**Verify:** Both `.claude` and `.github` golden copies contain the TDD quality checklist items

### 6. Byte-for-byte parity (existing test)

**Type:** Integration
**Verify:** `npm test -- --run tests/node/integration/byte-for-byte.test.ts` passes for all 8 profiles

## Execution

1. Modify source templates
2. Regenerate golden files via pipeline
3. Run byte-for-byte tests to confirm parity
4. Manually verify content assertions (scenarios 1-5) via grep
