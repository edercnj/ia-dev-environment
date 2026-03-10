# Test Plan — STORY-013: HooksAssembler + SettingsAssembler

## Scope

Unit tests for `src/assembler/hooks-assembler.ts` and
`src/assembler/settings-assembler.ts`, covering hook file copying with
executable permissions, multi-source permission collection, merge/deduplication,
settings.json/settings.local.json generation, and hooks section inclusion for
compiled languages. All tests use `vitest` and a `mkdtempSync`-backed temporary
directory that is cleaned up in `afterEach`.

**Target files:**
- `tests/node/assembler/hooks-assembler.test.ts`
- `tests/node/assembler/settings-assembler.test.ts`

**Coverage targets:** >= 95 % line, >= 90 % branch.

---

## Conventions

- Test names follow `[methodOrBehavior]_[scenario]_[expectedBehavior]`.
- `buildConfig(overrides?)` helper constructs a `ProjectConfig` accepting optional
  fields: `language`, `buildTool`, `container`, `orchestrator`, `database`,
  `cache`, `smokeTests`. Defaults: `language = "java"`, `buildTool = "maven"`,
  `container = "none"`, `orchestrator = "none"`, `database = "none"`,
  `cache = "none"`, `smokeTests = false`.
- Both assemblers receive `(config, outputDir, resourcesDir, engine)` per the
  established assembler signature.
- `TemplateEngine` is instantiated with `new TemplateEngine(resourcesDir, config)`.
  Neither assembler uses the engine for template rendering, but it is accepted for
  API uniformity.
- Helper functions create fixture files (JSON permission arrays, shell scripts)
  in the temporary `resourcesDir`.

---

## Part 1 — HooksAssembler

### Helpers

```typescript
function createHookScript(
  resourcesDir: string,
  templateKey: string,
  content: string = "#!/bin/bash\necho compile",
): void {
  const dir = path.join(resourcesDir, "hooks-templates", templateKey);
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, "post-compile-check.sh"),
    content,
    "utf-8",
  );
}
```

### Test Group: `assemble` — compiled languages (hook generated)

---

**Test HA-01**
- **Name:** `assemble_javaWithMaven_copiesHookScript`
- **Scenario:** Config has `language = "java"`, `buildTool = "maven"`.
  `getHookTemplateKey("java", "maven")` returns `"java-maven"`.
- **Setup:** `createHookScript(resourcesDir, "java-maven")`
- **Expected:**
  - Return array has length 1.
  - Returned path equals `{outputDir}/hooks/post-compile-check.sh`.
  - File exists on disk.
  - File content matches the source script.

---

**Test HA-02**
- **Name:** `assemble_javaWithGradle_copiesHookScript`
- **Scenario:** Config has `language = "java"`, `buildTool = "gradle"`.
  `getHookTemplateKey("java", "gradle")` returns `"java-gradle"`.
- **Setup:** `createHookScript(resourcesDir, "java-gradle")`
- **Expected:**
  - Return array has length 1.
  - File exists at `{outputDir}/hooks/post-compile-check.sh`.

---

**Test HA-03**
- **Name:** `assemble_kotlinWithGradle_copiesHookScript`
- **Scenario:** Config has `language = "kotlin"`, `buildTool = "gradle"`.
  `getHookTemplateKey("kotlin", "gradle")` returns `"kotlin"`.
- **Setup:** `createHookScript(resourcesDir, "kotlin")`
- **Expected:**
  - Return array has length 1.
  - File exists at `{outputDir}/hooks/post-compile-check.sh`.

---

**Test HA-04**
- **Name:** `assemble_rustWithCargo_copiesHookScript`
- **Scenario:** Config has `language = "rust"`, `buildTool = "cargo"`.
  `getHookTemplateKey("rust", "cargo")` returns `"rust"`.
- **Setup:** `createHookScript(resourcesDir, "rust")`
- **Expected:**
  - Return array has length 1.
  - File exists at `{outputDir}/hooks/post-compile-check.sh`.

---

**Test HA-05**
- **Name:** `assemble_csharpWithDotnet_copiesHookScript`
- **Scenario:** Config has `language = "csharp"`, `buildTool = "dotnet"`.
  `getHookTemplateKey("csharp", "dotnet")` returns `"csharp"`.
- **Setup:** `createHookScript(resourcesDir, "csharp")`
- **Expected:**
  - Return array has length 1.
  - File exists at `{outputDir}/hooks/post-compile-check.sh`.

---

**Test HA-06**
- **Name:** `assemble_typescriptWithNpm_copiesHookScript`
- **Scenario:** Config has `language = "typescript"`, `buildTool = "npm"`.
  `getHookTemplateKey("typescript", "npm")` returns `"typescript"`.
  TypeScript is semi-compiled (transpiled) and has a post-compile hook.
- **Setup:** `createHookScript(resourcesDir, "typescript")`
- **Expected:**
  - Return array has length 1.
  - File exists at `{outputDir}/hooks/post-compile-check.sh`.

---

**Test HA-07**
- **Name:** `assemble_goWithGo_copiesHookScript`
- **Scenario:** Config has `language = "go"`, `buildTool = "go"`.
  `getHookTemplateKey("go", "go")` returns `"go"`.
- **Setup:** `createHookScript(resourcesDir, "go")`
- **Expected:**
  - Return array has length 1.
  - File exists at `{outputDir}/hooks/post-compile-check.sh`.

---

**Test HA-08 (parametrized)**
- **Name:** `assemble_compiledLanguage_copiesHookScript` (via `it.each`)
- **Scenario:** Parametrized test exercising all 7 language/buildTool combos that
  produce a non-empty hook template key.
- **Parameters:**

  | Language | Build Tool | Template Key |
  |----------|-----------|--------------|
  | `java` | `maven` | `java-maven` |
  | `java` | `gradle` | `java-gradle` |
  | `kotlin` | `gradle` | `kotlin` |
  | `typescript` | `npm` | `typescript` |
  | `go` | `go` | `go` |
  | `rust` | `cargo` | `rust` |
  | `csharp` | `dotnet` | `csharp` |

- **Setup per row:** `createHookScript(resourcesDir, templateKey)`
- **Expected per row:**
  - Return array has length 1.
  - File exists at `{outputDir}/hooks/post-compile-check.sh`.
  - File content matches the source.

---

### Test Group: `assemble` — interpreted languages (no hook)

---

**Test HA-09**
- **Name:** `assemble_pythonWithPip_returnsEmptyArray`
- **Scenario:** Config has `language = "python"`, `buildTool = "pip"`.
  `getHookTemplateKey("python", "pip")` returns `""`.
- **Setup:** No hook template files on disk.
- **Expected:**
  - Return value is `[]`.
  - `{outputDir}/hooks/` directory does NOT exist.

---

**Test HA-10**
- **Name:** `assemble_unknownLanguage_returnsEmptyArray`
- **Scenario:** Config has `language = "ruby"`, `buildTool = "bundler"`.
  `getHookTemplateKey("ruby", "bundler")` returns `""` (not in map).
- **Expected:**
  - Return value is `[]`.
  - No directories or files created under `outputDir`.

---

### Test Group: `assemble` — file permissions

---

**Test HA-11**
- **Name:** `assemble_hookFileIsExecutable_chmod755Applied`
- **Scenario:** After copying, the destination file must have executable
  permission (`0o755`).
- **Setup:** `createHookScript(resourcesDir, "java-maven")`
- **Expected:**
  - `fs.statSync(dest).mode & 0o777` equals `0o755` (or at minimum, the
    execute bits `0o111` are set: `mode & 0o111 !== 0`).

---

### Test Group: `assemble` — directory creation

---

**Test HA-12**
- **Name:** `assemble_createsHooksDirectory_whenNotPreExisting`
- **Scenario:** The `{outputDir}/hooks/` directory does not exist before
  `assemble` is called.
- **Setup:** Clean output dir, `createHookScript(resourcesDir, "java-maven")`.
- **Expected:**
  - `{outputDir}/hooks/` is created automatically.
  - `post-compile-check.sh` is written inside it.

---

### Test Group: `assemble` — missing template file

---

**Test HA-13**
- **Name:** `assemble_templateKeyExistsButFileAbsent_returnsEmptyArray`
- **Scenario:** `getHookTemplateKey` returns a non-empty key (e.g., `"java-maven"`),
  but the `post-compile-check.sh` file does not exist in the corresponding
  template directory.
- **Setup:** Create the directory `{resourcesDir}/hooks-templates/java-maven/`
  but do NOT create the `post-compile-check.sh` file inside it.
- **Expected:**
  - Return value is `[]`.
  - No files created under `{outputDir}/hooks/`.

---

### Test Group: `assemble` — content fidelity

---

**Test HA-14**
- **Name:** `assemble_copiedFileContent_matchesSourceExactly`
- **Scenario:** Verify the hook script is copied byte-for-byte.
- **Setup:** `createHookScript(resourcesDir, "java-maven", "#!/bin/bash\nset -e\nmvn compile")`
- **Expected:**
  - `fs.readFileSync(dest, "utf-8")` equals `"#!/bin/bash\nset -e\nmvn compile"`.

---

## Part 2 — SettingsAssembler

### Helpers

```typescript
function createSettingsTemplate(
  resourcesDir: string,
  filename: string,
  permissions: string[],
): void {
  const dir = path.join(resourcesDir, "settings-templates");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, filename),
    JSON.stringify(permissions),
    "utf-8",
  );
}
```

### Test Group: Pure functions — `mergeJsonArrays`

---

**Test SA-01**
- **Name:** `mergeJsonArrays_twoNonEmptyArrays_returnsConcatenation`
- **Scenario:** `mergeJsonArrays(["a", "b"], ["c", "d"])`.
- **Expected:** `["a", "b", "c", "d"]`.

---

**Test SA-02**
- **Name:** `mergeJsonArrays_emptyOverlay_returnsBaseUnchanged`
- **Scenario:** `mergeJsonArrays(["a", "b"], [])`.
- **Expected:** `["a", "b"]`.

---

**Test SA-03**
- **Name:** `mergeJsonArrays_emptyBase_returnsOverlay`
- **Scenario:** `mergeJsonArrays([], ["c", "d"])`.
- **Expected:** `["c", "d"]`.

---

**Test SA-04**
- **Name:** `mergeJsonArrays_bothEmpty_returnsEmptyArray`
- **Scenario:** `mergeJsonArrays([], [])`.
- **Expected:** `[]`.

---

### Test Group: Pure functions — `deduplicate`

---

**Test SA-05**
- **Name:** `deduplicate_duplicateEntries_removedPreservingInsertionOrder`
- **Scenario:** `deduplicate(["a", "b", "a", "c", "b"])`.
- **Expected:** `["a", "b", "c"]`.

---

**Test SA-06**
- **Name:** `deduplicate_noDuplicates_returnsOriginalOrder`
- **Scenario:** `deduplicate(["x", "y", "z"])`.
- **Expected:** `["x", "y", "z"]`.

---

**Test SA-07**
- **Name:** `deduplicate_emptyArray_returnsEmptyArray`
- **Scenario:** `deduplicate([])`.
- **Expected:** `[]`.

---

**Test SA-08**
- **Name:** `deduplicate_singleElement_returnsSameElement`
- **Scenario:** `deduplicate(["only"])`.
- **Expected:** `["only"]`.

---

### Test Group: Pure functions — `readJsonArray`

---

**Test SA-09**
- **Name:** `readJsonArray_validJsonArray_returnsArray`
- **Scenario:** File contains `["Bash(git *)", "Bash(npm *)"]`.
- **Setup:** Write a JSON file with a valid array.
- **Expected:** Returns `["Bash(git *)", "Bash(npm *)"]`.

---

**Test SA-10**
- **Name:** `readJsonArray_malformedJson_returnsEmptyArray`
- **Scenario:** File contains `{invalid json`.
- **Setup:** Write a file with malformed content.
- **Expected:** Returns `[]`.

---

**Test SA-11**
- **Name:** `readJsonArray_jsonObjectNotArray_returnsEmptyArray`
- **Scenario:** File contains `{"key": "value"}` (valid JSON but not an array).
- **Setup:** Write a JSON object file.
- **Expected:** Returns `[]`.

---

**Test SA-12**
- **Name:** `readJsonArray_emptyJsonArray_returnsEmptyArray`
- **Scenario:** File contains `[]`.
- **Expected:** Returns `[]`.

---

### Test Group: Pure functions — `buildSettingsDict`

---

**Test SA-13**
- **Name:** `buildSettingsDict_withHooksTrue_includesHooksSection`
- **Scenario:** `buildSettingsDict(["perm1", "perm2"], true)`.
- **Expected:**
  - Result has `permissions.allow` containing `["perm1", "perm2"]`.
  - Result has `hooks` key with `PostToolUse` array.

---

**Test SA-14**
- **Name:** `buildSettingsDict_withHooksFalse_omitsHooksSection`
- **Scenario:** `buildSettingsDict(["perm1"], false)`.
- **Expected:**
  - Result has `permissions.allow` containing `["perm1"]`.
  - Result does NOT have `hooks` key.

---

**Test SA-15**
- **Name:** `buildSettingsDict_emptyPermissions_outputsEmptyAllowArray`
- **Scenario:** `buildSettingsDict([], false)`.
- **Expected:**
  - Result is `{ permissions: { allow: [] } }`.

---

### Test Group: Pure functions — `buildHooksSection`

---

**Test SA-16**
- **Name:** `buildHooksSection_returnsCorrectStructure`
- **Scenario:** Call `buildHooksSection()` and verify exact JSON structure.
- **Expected:**
  ```json
  {
    "PostToolUse": [
      {
        "matcher": "Write|Edit",
        "hooks": [
          {
            "type": "command",
            "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/post-compile-check.sh",
            "timeout": 60,
            "statusMessage": "Checking compilation..."
          }
        ]
      }
    ]
  }
  ```

---

**Test SA-17**
- **Name:** `buildHooksSection_commandStringContainsClaudeProjectDirVariable`
- **Scenario:** Verify the `command` field contains the exact embedded-quote
  pattern `"$CLAUDE_PROJECT_DIR"` (with literal double quotes).
- **Expected:**
  - The `command` value starts with `"$CLAUDE_PROJECT_DIR"` (including the
    surrounding double-quote characters).

---

### Test Group: `assemble` — base permissions

---

**Test SA-18**
- **Name:** `assemble_basePermissions_alwaysLoaded`
- **Scenario:** Config with an unknown language (no lang key, no infra, no data).
  Only `base.json` exists in settings-templates.
- **Setup:** `createSettingsTemplate(resourcesDir, "base.json", ["Bash(git *)"])`
- **Expected:**
  - `settings.json` contains `"Bash(git *)"` in `permissions.allow`.

---

### Test Group: `assemble` — language-specific permissions

---

**Test SA-19**
- **Name:** `assemble_javaMaven_mergesLangPermissions`
- **Scenario:** Config has `language = "java"`, `buildTool = "maven"`.
  `getSettingsLangKey("java", "maven")` returns `"java-maven"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["Bash(git *)"])`
  - `createSettingsTemplate(resourcesDir, "java-maven.json", ["Bash(mvn *)"])`
- **Expected:**
  - `settings.json` permissions include both `"Bash(git *)"` and `"Bash(mvn *)"`.

---

**Test SA-20**
- **Name:** `assemble_typescriptNpm_mergesLangPermissions`
- **Scenario:** Config has `language = "typescript"`, `buildTool = "npm"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["Bash(git *)"])`
  - `createSettingsTemplate(resourcesDir, "typescript-npm.json", ["Bash(npm *)"])`
- **Expected:**
  - `settings.json` permissions include `"Bash(npm *)"`.

---

**Test SA-21 (parametrized)**
- **Name:** `assemble_langKey_mergesCorrectLangFile` (via `it.each`)
- **Parameters:**

  | Language | Build Tool | Settings Key |
  |----------|-----------|--------------|
  | `java` | `maven` | `java-maven` |
  | `java` | `gradle` | `java-gradle` |
  | `kotlin` | `gradle` | `java-gradle` |
  | `typescript` | `npm` | `typescript-npm` |
  | `python` | `pip` | `python-pip` |
  | `go` | `go` | `go` |
  | `rust` | `cargo` | `rust-cargo` |
  | `csharp` | `dotnet` | `csharp-dotnet` |

- **Setup per row:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base-perm"])`
  - `createSettingsTemplate(resourcesDir, `${settingsKey}.json`, ["lang-perm"])`
- **Expected per row:**
  - `settings.json` permissions include both `"base-perm"` and `"lang-perm"`.

---

### Test Group: `assemble` — infrastructure permissions

---

**Test SA-22**
- **Name:** `assemble_dockerContainer_mergesDockerPermissions`
- **Scenario:** Config has `container = "docker"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "docker.json", ["Bash(docker *)"])`
- **Expected:**
  - `settings.json` permissions include `"Bash(docker *)"`.

---

**Test SA-23**
- **Name:** `assemble_podmanContainer_mergesDockerPermissions`
- **Scenario:** Config has `container = "podman"`. Podman loads `docker.json`
  (same as Docker — identical permissions).
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "docker.json", ["Bash(docker *)"])`
- **Expected:**
  - `settings.json` permissions include `"Bash(docker *)"`.

---

**Test SA-24**
- **Name:** `assemble_noneContainer_doesNotLoadDockerPermissions`
- **Scenario:** Config has `container = "none"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "docker.json", ["Bash(docker *)"])`
- **Expected:**
  - `settings.json` permissions do NOT include `"Bash(docker *)"`.

---

**Test SA-25**
- **Name:** `assemble_kubernetesOrchestrator_mergesK8sPermissions`
- **Scenario:** Config has `orchestrator = "kubernetes"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "kubernetes.json", ["Bash(kubectl *)"])`
- **Expected:**
  - `settings.json` permissions include `"Bash(kubectl *)"`.

---

**Test SA-26**
- **Name:** `assemble_dockerComposeOrchestrator_mergesComposePermissions`
- **Scenario:** Config has `orchestrator = "docker-compose"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "docker-compose.json", ["Bash(docker compose *)"])`
- **Expected:**
  - `settings.json` permissions include `"Bash(docker compose *)"`.

---

**Test SA-27**
- **Name:** `assemble_noneOrchestrator_doesNotLoadOrchestratorPermissions`
- **Scenario:** Config has `orchestrator = "none"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "kubernetes.json", ["Bash(kubectl *)"])`
  - `createSettingsTemplate(resourcesDir, "docker-compose.json", ["Bash(docker compose *)"])`
- **Expected:**
  - `settings.json` permissions do NOT include `"Bash(kubectl *)"` or
    `"Bash(docker compose *)"`.

---

### Test Group: `assemble` — data permissions (database)

---

**Test SA-28**
- **Name:** `assemble_postgresqlDatabase_mergesDbPermissions`
- **Scenario:** Config has `database = "postgresql"`.
  `getDatabaseSettingsKey("postgresql")` returns `"database-psql"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "database-psql.json", ["Bash(psql *)"])`
- **Expected:**
  - `settings.json` permissions include `"Bash(psql *)"`.

---

**Test SA-29**
- **Name:** `assemble_mysqlDatabase_mergesDbPermissions`
- **Scenario:** Config has `database = "mysql"`.
  `getDatabaseSettingsKey("mysql")` returns `"database-mysql"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "database-mysql.json", ["Bash(mysql *)"])`
- **Expected:**
  - `settings.json` permissions include `"Bash(mysql *)"`.

---

**Test SA-30**
- **Name:** `assemble_mongodbDatabase_mergesDbPermissions`
- **Scenario:** Config has `database = "mongodb"`.
  `getDatabaseSettingsKey("mongodb")` returns `"database-mongodb"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "database-mongodb.json", ["Bash(mongosh *)"])`
- **Expected:**
  - `settings.json` permissions include `"Bash(mongosh *)"`.

---

**Test SA-31 (parametrized)**
- **Name:** `assemble_databaseKey_mergesCorrectDbFile` (via `it.each`)
- **Parameters:**

  | Database | Settings Key |
  |----------|-------------|
  | `postgresql` | `database-psql` |
  | `mysql` | `database-mysql` |
  | `oracle` | `database-oracle` |
  | `mongodb` | `database-mongodb` |
  | `cassandra` | `database-cassandra` |

- **Setup per row:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, `${settingsKey}.json`, ["db-perm"])`
- **Expected per row:**
  - `settings.json` permissions include `"db-perm"`.

---

**Test SA-32**
- **Name:** `assemble_noneDatabase_doesNotLoadAnyDbPermissions`
- **Scenario:** Config has `database = "none"`.
  `getDatabaseSettingsKey("none")` returns `""`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "database-psql.json", ["Bash(psql *)"])`
- **Expected:**
  - `settings.json` permissions do NOT include `"Bash(psql *)"`.

---

### Test Group: `assemble` — data permissions (cache)

---

**Test SA-33**
- **Name:** `assemble_redisCache_mergesCachePermissions`
- **Scenario:** Config has `cache = "redis"`.
  `getCacheSettingsKey("redis")` returns `"cache-redis"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "cache-redis.json", ["Bash(redis-cli *)"])`
- **Expected:**
  - `settings.json` permissions include `"Bash(redis-cli *)"`.

---

**Test SA-34**
- **Name:** `assemble_memcachedCache_mergesCachePermissions`
- **Scenario:** Config has `cache = "memcached"`.
  `getCacheSettingsKey("memcached")` returns `"cache-memcached"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "cache-memcached.json", ["Bash(memcached *)"])`
- **Expected:**
  - `settings.json` permissions include `"Bash(memcached *)"`.

---

**Test SA-35 (parametrized)**
- **Name:** `assemble_cacheKey_mergesCorrectCacheFile` (via `it.each`)
- **Parameters:**

  | Cache | Settings Key |
  |-------|-------------|
  | `redis` | `cache-redis` |
  | `dragonfly` | `cache-dragonfly` |
  | `memcached` | `cache-memcached` |

- **Setup per row:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, `${settingsKey}.json`, ["cache-perm"])`
- **Expected per row:**
  - `settings.json` permissions include `"cache-perm"`.

---

**Test SA-36**
- **Name:** `assemble_noneCache_doesNotLoadAnyCachePermissions`
- **Scenario:** Config has `cache = "none"`.
  `getCacheSettingsKey("none")` returns `""`.
- **Expected:**
  - Only base permissions are present.

---

### Test Group: `assemble` — testing permissions

---

**Test SA-37**
- **Name:** `assemble_smokeTestsEnabled_mergesNewmanPermissions`
- **Scenario:** Config has `smokeTests = true`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "testing-newman.json", ["Bash(newman *)"])`
- **Expected:**
  - `settings.json` permissions include `"Bash(newman *)"`.

---

**Test SA-38**
- **Name:** `assemble_smokeTestsDisabled_doesNotLoadNewmanPermissions`
- **Scenario:** Config has `smokeTests = false`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "testing-newman.json", ["Bash(newman *)"])`
- **Expected:**
  - `settings.json` permissions do NOT include `"Bash(newman *)"`.

---

### Test Group: `assemble` — multi-source merge and deduplication

---

**Test SA-39**
- **Name:** `assemble_multipleSourcesMerged_allPermissionsPresent`
- **Scenario:** Config has `language = "java"`, `buildTool = "maven"`,
  `container = "docker"`, `database = "postgresql"`, `cache = "redis"`,
  `smokeTests = true`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["perm-base"])`
  - `createSettingsTemplate(resourcesDir, "java-maven.json", ["perm-java"])`
  - `createSettingsTemplate(resourcesDir, "docker.json", ["perm-docker"])`
  - `createSettingsTemplate(resourcesDir, "database-psql.json", ["perm-db"])`
  - `createSettingsTemplate(resourcesDir, "cache-redis.json", ["perm-cache"])`
  - `createSettingsTemplate(resourcesDir, "testing-newman.json", ["perm-newman"])`
- **Expected:**
  - `settings.json` `permissions.allow` contains all 6 items:
    `["perm-base", "perm-java", "perm-docker", "perm-db", "perm-cache", "perm-newman"]`.

---

**Test SA-40**
- **Name:** `assemble_overlappingPermissions_deduplicatedPreservingOrder`
- **Scenario:** Multiple sources share the same permission string.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["Bash(git *)", "common"])`
  - `createSettingsTemplate(resourcesDir, "java-maven.json", ["common", "Bash(mvn *)"])`
  - Config: `language = "java"`, `buildTool = "maven"`.
- **Expected:**
  - `settings.json` `permissions.allow` is
    `["Bash(git *)", "common", "Bash(mvn *)"]` (no duplicate `"common"`).

---

**Test SA-41**
- **Name:** `assemble_unknownLanguage_basePermissionsOnly`
- **Scenario:** Config has `language = "ruby"`, `buildTool = "bundler"`.
  `getSettingsLangKey` returns `""`. No infra, no data, no smoke tests.
- **Setup:** `createSettingsTemplate(resourcesDir, "base.json", ["base-only"])`
- **Expected:**
  - `settings.json` `permissions.allow` is `["base-only"]`.

---

### Test Group: `assemble` — hooks section in settings.json

---

**Test SA-42**
- **Name:** `assemble_compiledLanguage_settingsJsonIncludesHooksSection`
- **Scenario:** Config has `language = "java"`, `buildTool = "maven"`.
  `getHookTemplateKey("java", "maven")` returns `"java-maven"` (non-empty),
  so `hasHooks = true`.
- **Setup:** `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
- **Expected:**
  - `settings.json` has `"hooks"` key at root level.
  - `hooks.PostToolUse` is an array of length 1.
  - `hooks.PostToolUse[0].matcher` is `"Write|Edit"`.

---

**Test SA-43**
- **Name:** `assemble_interpretedLanguage_settingsJsonOmitsHooksSection`
- **Scenario:** Config has `language = "python"`, `buildTool = "pip"`.
  `getHookTemplateKey("python", "pip")` returns `""`, so `hasHooks = false`.
- **Setup:** `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
- **Expected:**
  - Parsed `settings.json` does NOT have a `"hooks"` key.

---

**Test SA-44 (parametrized)**
- **Name:** `assemble_hooksPresence_matchesHookTemplateKey` (via `it.each`)
- **Parameters:**

  | Language | Build Tool | Has Hooks |
  |----------|-----------|-----------|
  | `java` | `maven` | `true` |
  | `java` | `gradle` | `true` |
  | `kotlin` | `gradle` | `true` |
  | `typescript` | `npm` | `true` |
  | `go` | `go` | `true` |
  | `rust` | `cargo` | `true` |
  | `csharp` | `dotnet` | `true` |
  | `python` | `pip` | `false` |

- **Setup per row:** `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
- **Expected per row:**
  - If `hasHooks === true`: parsed `settings.json` has `"hooks"` key.
  - If `hasHooks === false`: parsed `settings.json` does NOT have `"hooks"` key.

---

### Test Group: `assemble` — settings.json format

---

**Test SA-45**
- **Name:** `assemble_settingsJson_twoSpaceIndentation`
- **Scenario:** Verify the output is formatted with 2-space indentation.
- **Setup:** `createSettingsTemplate(resourcesDir, "base.json", ["perm"])`
- **Expected:**
  - Raw file content starts with `{\n  "permissions"`.
  - Content uses 2-space indentation (no tabs, no 4-space indent).

---

**Test SA-46**
- **Name:** `assemble_settingsJson_endsWithTrailingNewline`
- **Scenario:** `JSON.stringify(..., null, 2) + "\n"`.
- **Expected:**
  - Raw file content ends with `\n`.

---

**Test SA-47**
- **Name:** `assemble_settingsJson_validJsonParseable`
- **Scenario:** The output file is valid JSON.
- **Expected:**
  - `JSON.parse(fs.readFileSync(dest, "utf-8"))` does not throw.

---

### Test Group: `assemble` — settings.local.json

---

**Test SA-48**
- **Name:** `assemble_settingsLocalJson_emptyPermissionsTemplate`
- **Scenario:** `settings.local.json` is always generated with empty permissions.
- **Setup:** `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
- **Expected:**
  - File exists at `{outputDir}/settings.local.json`.
  - Parsed content equals `{ permissions: { allow: [] } }`.

---

**Test SA-49**
- **Name:** `assemble_settingsLocalJson_twoSpaceIndentWithTrailingNewline`
- **Scenario:** Same formatting as `settings.json`.
- **Expected:**
  - Content uses 2-space indentation and ends with `\n`.

---

### Test Group: `assemble` — return value

---

**Test SA-50**
- **Name:** `assemble_returnsBothFilePaths`
- **Scenario:** Verify return array has exactly 2 paths.
- **Setup:** `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
- **Expected:**
  - Return array has length 2.
  - Return array contains `{outputDir}/settings.json`.
  - Return array contains `{outputDir}/settings.local.json`.

---

### Test Group: `assemble` — missing optional template files

---

**Test SA-51**
- **Name:** `assemble_missingLangFile_skipsGracefully`
- **Scenario:** `getSettingsLangKey` returns a key, but the corresponding JSON
  file does not exist in `settings-templates/`.
- **Setup:** `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  (no `java-maven.json` on disk).
- **Expected:**
  - No error thrown.
  - `settings.json` permissions contain only base permissions.

---

**Test SA-52**
- **Name:** `assemble_missingBaseFile_emptyPermissions`
- **Scenario:** Even `base.json` is absent. All other sources are also absent.
- **Setup:** Create the `settings-templates/` directory but no files.
- **Expected:**
  - No error thrown.
  - `settings.json` is generated with `permissions.allow` as an empty array.

---

**Test SA-53**
- **Name:** `assemble_missingInfraFiles_skipsGracefully`
- **Scenario:** Config has `container = "docker"`, `orchestrator = "kubernetes"`,
  but `docker.json` and `kubernetes.json` do not exist on disk.
- **Setup:** `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
- **Expected:**
  - No error thrown.
  - `settings.json` permissions contain only base permissions.

---

### Test Group: `assemble` — combined infrastructure scenario

---

**Test SA-54**
- **Name:** `assemble_dockerPlusKubernetes_mergesBothInfraPermissions`
- **Scenario:** Config has `container = "docker"`, `orchestrator = "kubernetes"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "docker.json", ["docker-perm"])`
  - `createSettingsTemplate(resourcesDir, "kubernetes.json", ["k8s-perm"])`
- **Expected:**
  - `settings.json` permissions include `"docker-perm"` and `"k8s-perm"`.

---

**Test SA-55**
- **Name:** `assemble_podmanPlusDockerCompose_mergesBothInfraPermissions`
- **Scenario:** Config has `container = "podman"`, `orchestrator = "docker-compose"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "docker.json", ["docker-perm"])`
  - `createSettingsTemplate(resourcesDir, "docker-compose.json", ["compose-perm"])`
- **Expected:**
  - `settings.json` permissions include both `"docker-perm"` and `"compose-perm"`.

---

### Test Group: `assemble` — combined database + cache scenario

---

**Test SA-56**
- **Name:** `assemble_postgresAndRedis_mergesBothDataPermissions`
- **Scenario:** Config has `database = "postgresql"`, `cache = "redis"`.
- **Setup:**
  - `createSettingsTemplate(resourcesDir, "base.json", ["base"])`
  - `createSettingsTemplate(resourcesDir, "database-psql.json", ["db-perm"])`
  - `createSettingsTemplate(resourcesDir, "cache-redis.json", ["cache-perm"])`
- **Expected:**
  - `settings.json` permissions include both `"db-perm"` and `"cache-perm"`.

---

## Part 3 — Parity Tests

### Test Group: Parity with Python output

Parity tests compare the TypeScript assembler output against the Python assembler
output for representative configurations. These tests run against the real
`resources/` directory (not fixtures) to catch any template file discrepancies.

---

**Test PAR-01**
- **Name:** `parity_hooksAssembler_javaMaven_matchesPythonOutput`
- **Scenario:** Run both Python and TypeScript HooksAssembler with
  `language = "java"`, `buildTool = "maven"`, using the real `resources/`
  templates. Compare the generated `hooks/post-compile-check.sh` file.
- **Expected:**
  - File content is byte-identical.
  - File permissions are equivalent (executable).

---

**Test PAR-02**
- **Name:** `parity_settingsAssembler_javaMavenDockerPostgres_matchesPythonOutput`
- **Scenario:** Run both assemblers with a representative config:
  `language = "java"`, `buildTool = "maven"`, `container = "docker"`,
  `database = "postgresql"`, `smokeTests = true`.
- **Expected:**
  - `settings.json` is byte-identical between Python and TypeScript output.
  - `settings.local.json` is byte-identical.

---

**Test PAR-03**
- **Name:** `parity_settingsAssembler_pythonPip_noHooksInSettings`
- **Scenario:** Run both assemblers with `language = "python"`,
  `buildTool = "pip"`. Verify no hooks section in settings.json.
- **Expected:**
  - `settings.json` is byte-identical (no `hooks` key in either).

---

**Test PAR-04**
- **Name:** `parity_settingsAssembler_fullConfig_allSourcesMerged`
- **Scenario:** Comprehensive config covering all permission sources:
  `language = "java"`, `buildTool = "maven"`, `container = "docker"`,
  `orchestrator = "kubernetes"`, `database = "postgresql"`,
  `cache = "redis"`, `smokeTests = true`.
- **Expected:**
  - `settings.json` permission arrays match exactly (same order, same values).
  - Hooks section matches exactly.

---

## Summary Table

| ID | Assembler | Behavior Under Test | Key Assertion |
|----|-----------|---------------------|---------------|
| HA-01 | Hooks | Java/Maven hook copy | File exists |
| HA-02 | Hooks | Java/Gradle hook copy | File exists |
| HA-03 | Hooks | Kotlin/Gradle hook copy | File exists |
| HA-04 | Hooks | Rust/Cargo hook copy | File exists |
| HA-05 | Hooks | C#/dotnet hook copy | File exists |
| HA-06 | Hooks | TypeScript/npm hook copy | File exists |
| HA-07 | Hooks | Go/go hook copy | File exists |
| HA-08 | Hooks | Parametrized: all 7 compiled combos | it.each file exists |
| HA-09 | Hooks | Python: no hook generated | `result === []` |
| HA-10 | Hooks | Unknown language: no hook generated | `result === []` |
| HA-11 | Hooks | Executable permission (chmod 755) | Mode bits |
| HA-12 | Hooks | Hooks directory created automatically | Dir exists |
| HA-13 | Hooks | Template key exists but file absent | `result === []` |
| HA-14 | Hooks | Content fidelity (byte-for-byte copy) | Content match |
| SA-01 | Settings | mergeJsonArrays: two arrays | Concatenation |
| SA-02 | Settings | mergeJsonArrays: empty overlay | Base unchanged |
| SA-03 | Settings | mergeJsonArrays: empty base | Overlay returned |
| SA-04 | Settings | mergeJsonArrays: both empty | `[]` |
| SA-05 | Settings | deduplicate: removes duplicates preserving order | `["a","b","c"]` |
| SA-06 | Settings | deduplicate: no duplicates | Original order |
| SA-07 | Settings | deduplicate: empty array | `[]` |
| SA-08 | Settings | deduplicate: single element | `["only"]` |
| SA-09 | Settings | readJsonArray: valid JSON array | Array returned |
| SA-10 | Settings | readJsonArray: malformed JSON | `[]` |
| SA-11 | Settings | readJsonArray: JSON object (not array) | `[]` |
| SA-12 | Settings | readJsonArray: empty array | `[]` |
| SA-13 | Settings | buildSettingsDict: hooks=true includes hooks | Hooks key present |
| SA-14 | Settings | buildSettingsDict: hooks=false omits hooks | No hooks key |
| SA-15 | Settings | buildSettingsDict: empty permissions | `{ permissions: { allow: [] } }` |
| SA-16 | Settings | buildHooksSection: exact JSON structure | PostToolUse structure |
| SA-17 | Settings | buildHooksSection: CLAUDE_PROJECT_DIR in command | Embedded quotes |
| SA-18 | Settings | Base permissions always loaded | base.json content |
| SA-19 | Settings | Java/Maven lang permissions merged | Both sources |
| SA-20 | Settings | TypeScript/npm lang permissions merged | Both sources |
| SA-21 | Settings | Parametrized: all 8 lang keys | it.each merge |
| SA-22 | Settings | Docker container permissions | docker.json loaded |
| SA-23 | Settings | Podman loads docker.json | Same as docker |
| SA-24 | Settings | None container skips docker.json | Not loaded |
| SA-25 | Settings | Kubernetes orchestrator permissions | kubernetes.json loaded |
| SA-26 | Settings | Docker-compose orchestrator permissions | docker-compose.json loaded |
| SA-27 | Settings | None orchestrator skips all | Neither loaded |
| SA-28 | Settings | PostgreSQL database permissions | database-psql.json loaded |
| SA-29 | Settings | MySQL database permissions | database-mysql.json loaded |
| SA-30 | Settings | MongoDB database permissions | database-mongodb.json loaded |
| SA-31 | Settings | Parametrized: all 5 database keys | it.each merge |
| SA-32 | Settings | None database skips all | Not loaded |
| SA-33 | Settings | Redis cache permissions | cache-redis.json loaded |
| SA-34 | Settings | Memcached cache permissions | cache-memcached.json loaded |
| SA-35 | Settings | Parametrized: all 3 cache keys | it.each merge |
| SA-36 | Settings | None cache skips all | Not loaded |
| SA-37 | Settings | Smoke tests enabled: Newman permissions | testing-newman.json loaded |
| SA-38 | Settings | Smoke tests disabled: no Newman | Not loaded |
| SA-39 | Settings | Full multi-source merge (6 sources) | All 6 present |
| SA-40 | Settings | Deduplication across sources | No duplicates |
| SA-41 | Settings | Unknown language: base only | Base-only array |
| SA-42 | Settings | Compiled lang: hooks section in settings.json | hooks key present |
| SA-43 | Settings | Interpreted lang: no hooks in settings.json | hooks key absent |
| SA-44 | Settings | Parametrized: hooks presence per lang | it.each 8 combos |
| SA-45 | Settings | 2-space indentation | Format check |
| SA-46 | Settings | Trailing newline | Ends with `\n` |
| SA-47 | Settings | Valid parseable JSON | JSON.parse succeeds |
| SA-48 | Settings | settings.local.json: empty template | `{ permissions: { allow: [] } }` |
| SA-49 | Settings | settings.local.json: formatting | 2-space + trailing newline |
| SA-50 | Settings | Returns both file paths | Array length 2 |
| SA-51 | Settings | Missing lang file: graceful skip | Base only |
| SA-52 | Settings | Missing base file: empty permissions | `allow: []` |
| SA-53 | Settings | Missing infra files: graceful skip | Base only |
| SA-54 | Settings | Docker + Kubernetes: both infra merged | Both present |
| SA-55 | Settings | Podman + Docker-compose: both merged | Both present |
| SA-56 | Settings | PostgreSQL + Redis: both data merged | Both present |
| PAR-01 | Parity | Hooks: Java/Maven matches Python | Byte-identical |
| PAR-02 | Parity | Settings: Java/Maven/Docker/Postgres | Byte-identical |
| PAR-03 | Parity | Settings: Python (no hooks) | No hooks key |
| PAR-04 | Parity | Settings: full config all sources | Exact match |

---

## Notes

1. **Hook template key mapping:** Per `HOOK_TEMPLATE_MAP` in `stack-mapping.ts`,
   Python (`python-pip`) is the only language in the map with an empty string
   value. All other 7 entries produce non-empty keys. Languages not in the map
   at all (e.g., `ruby`) also return `""` via the `?? ""` fallback.

2. **TypeScript has hooks:** Unlike the story description that mentions
   "compiled languages only", `HOOK_TEMPLATE_MAP` assigns `"typescript"` as the
   template key for `typescript-npm`. The TypeScript language does get a
   post-compile hook (`npx tsc --noEmit`). Tests must reflect this reality.

3. **Go has hooks:** Similarly, Go (`go-go`) maps to template key `"go"` and has
   a corresponding `resources/hooks-templates/go/` directory. Tests should treat
   Go as having hooks.

4. **Permission order:** The Python implementation concatenates permissions in
   this order: base -> language -> infrastructure (docker, then orchestrator) ->
   data (database, then cache) -> testing. TypeScript must maintain this same
   order. Test SA-39 verifies the exact order.

5. **Podman treated as Docker:** The Python code uses
   `if container in ("docker", "podman"): merge docker.json`. The TypeScript
   implementation must replicate this behavior. Test SA-23 specifically validates
   this edge case.

6. **`it.each` usage:** Tests HA-08, SA-21, SA-31, SA-35, and SA-44 are
   parametrized with `it.each` to reduce boilerplate while covering all
   mapping entries exhaustively.

7. **File ordering for parity:** Parity tests (PAR-01 through PAR-04) should
   use the project's real `resources/` directory, not synthetic fixtures, to
   catch any differences in template content between what Python and TypeScript
   read from disk.

8. **Total test count:** 60 tests (14 HooksAssembler + 42 SettingsAssembler +
   4 Parity). This covers all branches identified in the Python source and all
   edge cases from the implementation plan.
