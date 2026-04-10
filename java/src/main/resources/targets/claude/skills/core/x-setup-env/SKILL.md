---
name: x-setup-env
description: "Validate and configure local development environment: detect stack, check prerequisites, verify versions, validate IDE config, test database connectivity, run initial build, and report status with fix suggestions."
user-invocable: true
allowed-tools: Read, Bash, Glob, Grep, Write
argument-hint: "[--check-only] [--fix]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Setup Dev Environment

## Purpose

Validates and configures the local development environment for {{PROJECT_NAME}}, detecting the project stack, checking prerequisites, verifying versions, validating IDE configuration, testing database connectivity, running the initial build, and reporting status with fix suggestions.

## Triggers

- `/x-setup-dev-environment` — check-only mode (default)
- `/x-setup-dev-environment --check-only` — explicitly report status without modifications
- `/x-setup-dev-environment --fix` — attempt to fix detected issues

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `--check-only` | Flag | true | Report status only, do not modify anything (default) |
| `--fix` | Flag | false | Attempt to correct problems found (non-destructive, never overwrites existing files) |

## Workflow

```
1. DETECT     -> Detect project stack from config files
2. CHECK      -> Check prerequisites (runtime, build tool, Docker, DB client)
3. VERIFY     -> Verify installed versions against required versions
4. IDE        -> Check IDE configuration (.editorconfig, formatters, linters)
5. DATABASE   -> Verify database connectivity (skip if database=none)
6. BUILD      -> Run initial build to validate dependency resolution
7. REPORT     -> Generate status report with PASS/FAIL/WARN per check
```

### Step 1 — Detect Project Stack

Analyze project root for config files to identify language, framework, and build tool:

| Config File | Language | Build Tool |
|-------------|----------|------------|
| `pom.xml` | Java | Maven |
| `build.gradle.kts` / `build.gradle` | Java/Kotlin | Gradle |
| `package.json` | TypeScript/JavaScript | npm/yarn/pnpm |
| `go.mod` | Go | go |
| `Cargo.toml` | Rust | cargo |
| `pyproject.toml` / `setup.py` | Python | pip/poetry |

```bash
# Detect project config files
ls -la pom.xml package.json go.mod Cargo.toml pyproject.toml build.gradle.kts build.gradle setup.py 2>/dev/null
```

Cross-reference with project identity in `.claude/rules/01-project-identity.md` (RULE-001 — Project Identity) if available.

### Step 2 — Check Prerequisites

Verify presence of required tools based on detected stack:

{% if language_name == "java" %}
**Java Stack:**
```bash
# Check JDK
java --version 2>&1 || echo "FAIL: JDK not found"

# Check build tool
{% if build_tool == "maven" %}
mvn --version 2>&1 || echo "FAIL: Maven not found"
{% elif build_tool == "gradle" %}
gradle --version 2>&1 || ./gradlew --version 2>&1 || echo "FAIL: Gradle not found"
{% endif %}
```
{% endif %}

{% if language_name == "typescript" or language_name == "javascript" %}
**Node.js Stack:**
```bash
node --version 2>&1 || echo "FAIL: Node.js not found"
npm --version 2>&1 || echo "FAIL: npm not found"
```
{% endif %}

{% if language_name == "go" %}
**Go Stack:**
```bash
go version 2>&1 || echo "FAIL: Go not found"
```
{% endif %}

{% if language_name == "rust" %}
**Rust Stack:**
```bash
rustc --version 2>&1 || echo "FAIL: Rust not found"
cargo --version 2>&1 || echo "FAIL: Cargo not found"
```
{% endif %}

{% if language_name == "python" %}
**Python Stack:**
```bash
python3 --version 2>&1 || echo "FAIL: Python not found"
pip3 --version 2>&1 || echo "FAIL: pip not found"
```
{% endif %}

{% if container != "none" %}
**Docker:**
```bash
docker --version 2>&1 || echo "FAIL: Docker not found"
docker info >/dev/null 2>&1 || echo "WARN: Docker daemon not running"
```
{% endif %}

{% if database_name != "none" %}
**Database Client:**
```bash
{% if database_name == "postgresql" %}
psql --version 2>&1 || echo "FAIL: PostgreSQL client not found"
{% elif database_name == "mysql" %}
mysql --version 2>&1 || echo "FAIL: MySQL client not found"
{% elif database_name == "mongodb" %}
mongosh --version 2>&1 || echo "FAIL: MongoDB shell not found"
{% endif %}
```
{% endif %}

### Step 3 — Verify Versions

Compare installed versions against project requirements:

{% if language_name == "java" %}
```bash
# Extract major version from java --version output
JAVA_VERSION=$(java --version 2>&1 | head -1 | grep -oP '\d+' | head -1)
REQUIRED_VERSION="{{ language_version }}"
if [ "$JAVA_VERSION" != "$REQUIRED_VERSION" ]; then
    echo "WARN: Java $JAVA_VERSION installed, project requires $REQUIRED_VERSION"
fi
```
{% endif %}

{% if language_name == "typescript" or language_name == "javascript" %}
```bash
NODE_VERSION=$(node --version 2>&1 | grep -oP '\d+' | head -1)
REQUIRED_VERSION="{{ language_version }}"
if [ "$NODE_VERSION" != "$REQUIRED_VERSION" ]; then
    echo "WARN: Node $NODE_VERSION installed, project requires $REQUIRED_VERSION"
fi
```
{% endif %}

### Step 4 — Check IDE Configuration

```bash
# Check .editorconfig
if [ -f .editorconfig ]; then
    echo "PASS: .editorconfig found"
else
    echo "WARN: .editorconfig not found"
fi

# Check VS Code settings
if [ -d .vscode ]; then
    echo "PASS: .vscode/ directory found"
    ls .vscode/settings.json .vscode/extensions.json 2>/dev/null
fi

# Check IntelliJ settings
if [ -d .idea ]; then
    echo "PASS: .idea/ directory found"
fi
```

### Step 5 — Verify Database Connectivity

{% if database_name != "none" %}
```bash
{% if database_name == "postgresql" %}
psql -h localhost -p 5432 -U postgres -c "SELECT 1" 2>&1 || echo "FAIL: Cannot connect to PostgreSQL"
{% elif database_name == "mysql" %}
mysql -h localhost -P 3306 -u root -e "SELECT 1" 2>&1 || echo "FAIL: Cannot connect to MySQL"
{% elif database_name == "mongodb" %}
mongosh --eval "db.runCommand({ping:1})" 2>&1 || echo "FAIL: Cannot connect to MongoDB"
{% endif %}
```
{% else %}
> Database check SKIPPED: no database configured for this project.
{% endif %}

### Step 6 — Run Initial Build

{% if build_tool == "maven" %}
```bash
mvn clean compile -q 2>&1
if [ $? -eq 0 ]; then
    echo "PASS: Maven build successful"
else
    echo "FAIL: Maven build failed"
fi
```
{% elif build_tool == "gradle" %}
```bash
./gradlew build -q 2>&1
```
{% elif build_tool == "npm" %}
```bash
npm install && npm run build 2>&1
```
{% elif build_tool == "cargo" %}
```bash
cargo build 2>&1
```
{% elif build_tool == "pip" or build_tool == "poetry" %}
```bash
pip install -e ".[dev]" 2>&1 || poetry install 2>&1
```
{% else %}
```bash
echo "WARN: Unknown build tool, skipping build step"
```
{% endif %}

### Step 7 — Generate Report

```
============================================
  Dev Environment Setup Report
  Project: {{PROJECT_NAME}}
  Stack:   {{LANGUAGE_NAME}} / {{FRAMEWORK_NAME}}
============================================

| Check                  | Status    | Details                    |
|------------------------|-----------|----------------------------|
| Language Runtime       | PASS/FAIL | {version info}             |
| Build Tool             | PASS/FAIL | {version info}             |
| Docker                 | PASS/SKIP | {skip if container=none}   |
| Database Client        | PASS/SKIP | {skip if database=none}    |
| IDE Configuration      | PASS/WARN | {.editorconfig status}     |
| Database Connectivity  | PASS/SKIP | {skip if database=none}    |
| Initial Build          | PASS/FAIL | {build output summary}     |

Overall: X/Y checks passed
```

## Fix Mode Behavior

When `--fix` is specified, attempt corrections for FAIL/WARN items:

| Issue | Fix Action |
|-------|------------|
| Missing `.editorconfig` | Create with project defaults (indent_style, charset, end_of_line) |
| Missing dependencies | Run package manager install command |
| Docker not running | Suggest `docker desktop` or `systemctl start docker` |
| Wrong runtime version | Suggest version manager (sdkman, nvm, rustup, pyenv) |

**Non-destructive guarantee:** Fix mode NEVER overwrites existing files. It only creates missing files or installs missing dependencies.

## Error Handling

| Scenario | Action |
|----------|--------|
| Config file not found | Report as WARN, suggest creating it |
| Tool not installed | Report as FAIL with installation URL |
| Version mismatch | Report as WARN with upgrade instructions |
| Build failure | Report as FAIL with last 20 lines of error output |
| Database unreachable | Report as FAIL with connection troubleshooting |
