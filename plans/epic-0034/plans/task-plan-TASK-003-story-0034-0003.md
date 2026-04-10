# Task Plan -- TASK-0034-0003-003

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0003-003 |
| Story ID | story-0034-0003 |
| Epic ID | 0034 |
| Source Agent | QA + Security + Tech Lead (consolidated) |
| Type | migration (delete) |
| TDD Phase | GREEN (boundary) |
| Layer | adapter.test |
| Estimated Effort | M |
| Date | 2026-04-10 |

## Objective

Delete `.agents/` subdirectories recursively from all 17 golden file profiles (~2910 files). Verify that `java/src/main/resources/targets/agents/` does not exist (story §3.3 invariant). Leave `mvn clean verify` green.

## Implementation Guide

1. **Pre-check invariant:** `test -d java/src/main/resources/targets/agents` — must return non-zero exit (directory does not exist). If it exists, delete it recursively and document discovery in commit body.
2. **Pre-delete security scans:**
   - **Symlink check (CWE-22):** `find java/src/test/resources/golden -type l -path '*/.agents/*'` — expect empty.
   - **Secrets scan:** `grep -rE '(password|secret|token|api_?key)' java/src/test/resources/golden/*/.agents/ 2>/dev/null` — expect 0 matches. If hits found, inspect before delete — they are golden templates, but an unexpected hit could indicate a fixture with real secrets. Document any allow-listed hits in commit body.
3. **Delete `.agents/` directories in 17 profiles:**
   ```
   go-gin
   java-quarkus
   java-spring
   java-spring-clickhouse
   java-spring-cqrs-es
   java-spring-elasticsearch
   java-spring-event-driven
   java-spring-fintech-pci
   java-spring-hexagonal
   java-spring-neo4j
   kotlin-ktor
   python-click-cli
   python-fastapi
   python-fastapi-timescale
   rust-axum
   typescript-commander-cli
   typescript-nestjs
   ```
   Use `rm -rf java/src/test/resources/golden/{profile}/.agents/` per profile, OR one-shot `find java/src/test/resources/golden -type d -name '.agents' -exec rm -rf {} +`.
4. **Post-delete verification:**
   - `find java/src/test/resources/golden -type d -name '.agents' | wc -l` → 0
   - `find java/src/test/resources/golden -type d -name '.claude' | wc -l` → 17 (unchanged)
   - `git status --short | grep -c '^ D'` should reflect ~2910 deleted files
5. **Compile and test:** `mvn compile` → green; `mvn test-compile` → green; `mvn test` → green. `AgentsGoldenMatchTest` (deleted in TASK-001) should no longer exist to complain about missing fixtures.
6. **Commit:** `test(golden)!: delete .agents/ golden subdirs from 17 profiles`.

## Definition of Done

- [ ] [SEC-003/CWE-22] Symlink pre-check performed: `find ... -type l` returns empty
- [ ] [SEC-002] Secrets pre-scan performed: `grep -rE 'password|secret|token|api_?key' .agents/` returns 0 (or allow-listed hits documented)
- [ ] `.agents/` directory deleted in all 17 profiles
- [ ] ~2910 files removed (actual count recorded in commit body)
- [ ] [QA-004] `find golden -type d -name '.agents'` returns 0
- [ ] [QA-005] `.claude/` dir count unchanged (17 profiles still have it)
- [ ] `java/src/main/resources/targets/agents/` confirmed non-existent (story §3.3 invariant)
- [ ] `mvn compile` green
- [ ] `mvn test-compile` green
- [ ] `mvn test` green
- [ ] [TL-006] Conventional commit: `test(golden)!: delete .agents/ golden subdirs from 17 profiles`

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0003-002 | Enum must be cleaned first; otherwise `AssemblerTargetTest` may still reference `.agents/` and fail when golden files disappear. More importantly, `AgentsGoldenMatchTest` (deleted in TASK-001) would have complained about missing files had it not been deleted first. |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Bulk delete removes protected workflows subdirectory by accident | LOW | Protected file deleted | `.agents/` is not a subdir of `.github/` — no overlap with RULE-003 protected paths; delete is scoped by name |
| A profile is missing its `.agents/` directory (already cleaned) | LOW | Delete step appears no-op for that profile | Loop over profiles; `rm -rf` is idempotent on missing paths |
| `java/src/main/resources/targets/agents/` unexpectedly exists | LOW | Story §3.3 assumption violated | Pre-check finds it; delete as safety net and document in commit body |
| Golden match tests fail because another test class consumes `.agents/` fixtures | LOW | Build regression | Grep `java/src/test/java` for references to `.agents` path literals before delete; ensure only deleted Agents*Test classes consumed them |
