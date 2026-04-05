# Contributing to {{ project_name }}

Thank you for your interest in contributing to **{{ project_name }}**. This guide covers everything you need to get started.

## Prerequisites

Before contributing, ensure you have the following installed:

{% if language_name == "java" %}
- **Java JDK** {{ language_version }}+ ([download](https://adoptium.net/))
{% if build_tool == "maven" %}
- **Apache Maven** 3.9+ ([download](https://maven.apache.org/download.cgi))
{% elseif build_tool == "gradle" %}
- **Gradle** 8+ ([download](https://gradle.org/install/)) or use the included wrapper
{% endif %}
{% elseif language_name == "kotlin" %}
- **Java JDK** {{ language_version }}+ ([download](https://adoptium.net/))
- **Kotlin** compiler (bundled with build tool)
{% if build_tool == "gradle" %}
- **Gradle** 8+ ([download](https://gradle.org/install/)) or use the included wrapper
{% elseif build_tool == "maven" %}
- **Apache Maven** 3.9+ ([download](https://maven.apache.org/download.cgi))
{% endif %}
{% elseif language_name == "typescript" or language_name == "javascript" %}
- **Node.js** {{ language_version }}+ ([download](https://nodejs.org/))
- **npm** 9+ (bundled with Node.js)
{% elseif language_name == "go" %}
- **Go** {{ language_version }}+ ([download](https://go.dev/dl/))
{% elseif language_name == "rust" %}
- **Rust** {{ language_version }}+ via rustup ([install](https://rustup.rs/))
- **Cargo** (bundled with Rust)
{% elseif language_name == "python" %}
- **Python** {{ language_version }}+ ([download](https://www.python.org/downloads/))
{% if build_tool == "poetry" %}
- **Poetry** ([install](https://python-poetry.org/docs/))
{% else %}
- **pip** (bundled with Python)
{% endif %}
{% endif %}
{% if container != "none" %}
- **Docker** ([download](https://www.docker.com/products/docker-desktop/))
{% endif %}
{% if database_name != "none" %}
- **{{ database_name }}** client tools
{% endif %}
- **Git** 2.30+ ([download](https://git-scm.com/))

## Getting Started

1. **Clone the repository:**
   ```bash
   git clone <repository-url>
   cd {{ project_name }}
   ```

2. **Install dependencies:**
{% if build_tool == "maven" %}
   ```bash
   mvn clean install -DskipTests
   ```
{% elseif build_tool == "gradle" %}
   ```bash
   ./gradlew build -x test
   ```
{% elseif build_tool == "npm" %}
   ```bash
   npm install
   ```
{% elseif build_tool == "cargo" %}
   ```bash
   cargo build
   ```
{% elseif build_tool == "pip" %}
   ```bash
   pip install -e ".[dev]"
   ```
{% elseif build_tool == "poetry" %}
   ```bash
   poetry install
   ```
{% elseif build_tool == "go" %}
   ```bash
   go mod download
   ```
{% endif %}

3. **Configure your environment:**
   - Copy `.env.example` to `.env` (if applicable)
   - Ensure your IDE respects `.editorconfig` settings
{% if database_name != "none" %}
   - Start the local database (see Docker Compose if available)
{% endif %}

4. **Verify your setup:**
{% if build_tool == "maven" %}
   ```bash
   mvn test
   ```
{% elseif build_tool == "gradle" %}
   ```bash
   ./gradlew test
   ```
{% elseif build_tool == "npm" %}
   ```bash
   npm test
   ```
{% elseif build_tool == "cargo" %}
   ```bash
   cargo test
   ```
{% elseif build_tool == "pip" or build_tool == "poetry" %}
   ```bash
   pytest
   ```
{% elseif build_tool == "go" %}
   ```bash
   go test ./...
   ```
{% endif %}

## Development Workflow

### Branching Strategy

We use **feature branches** off `main`:

| Branch Pattern | Purpose |
|----------------|---------|
| `feat/<description>` | New features |
| `fix/<description>` | Bug fixes |
| `chore/<description>` | Maintenance, refactoring, tooling |
| `docs/<description>` | Documentation changes |

```bash
git checkout -b feat/my-new-feature
```

### Commit Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

**Types:** `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `ci`, `build`

**Examples:**
```
feat(auth): add JWT token refresh endpoint
fix(db): handle connection timeout gracefully
test(user): add unit tests for password validation
refactor(api): extract validation into shared module
```

### TDD Cycle

All production code follows **Red-Green-Refactor**:

1. **Red** -- Write a failing test that defines expected behavior
2. **Green** -- Write the minimum code to make the test pass
3. **Refactor** -- Improve design without changing behavior

Test must appear in git history **before or in the same commit** as implementation.

## Code Standards

This project follows strict coding standards defined in `.claude/rules/03-coding-standards.md`.

Key constraints:

| Constraint | Limit |
|------------|-------|
| Method/function length | 25 lines max |
| Class/module length | 250 lines max |
| Parameters per function | 4 max |
| Line width | 120 characters max |

Refer to the full coding standards for SOLID principles, naming conventions, and forbidden patterns.

## Testing

### Running Tests

{% if build_tool == "maven" %}
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=MyClassTest

# Run with coverage report
mvn verify
```
{% elseif build_tool == "gradle" %}
```bash
# Run all tests
./gradlew test

# Run with coverage report
./gradlew jacocoTestReport
```
{% elseif build_tool == "npm" %}
```bash
# Run all tests
npm test

# Run with coverage
npm run test:coverage
```
{% elseif build_tool == "cargo" %}
```bash
# Run all tests
cargo test

# Run with output
cargo test -- --nocapture
```
{% elseif build_tool == "pip" or build_tool == "poetry" %}
```bash
# Run all tests
pytest

# Run with coverage
pytest --cov
```
{% elseif build_tool == "go" %}
```bash
# Run all tests
go test ./...

# Run with coverage
go test -coverprofile=coverage.out ./...
```
{% endif %}

### Coverage Requirements

| Metric | Minimum |
|--------|---------|
| Line Coverage | >= {{ coverage_line }}% |
| Branch Coverage | >= {{ coverage_branch }}% |

### Test Naming Convention

```
[methodUnderTest]_[scenario]_[expectedBehavior]
```

Example: `processPayment_whenAmountExceedsLimit_throwsValidationException`

## Pull Request Process

### Branch Naming

Use the patterns from the branching strategy above (`feat/`, `fix/`, `chore/`, `docs/`).

### Creating a PR

1. Push your branch:
   ```bash
   git push -u origin feat/my-new-feature
   ```

2. Create a Pull Request targeting `main`

3. Fill in the PR template with:
   - Summary of changes
   - Test plan
   - Related issues/stories

### Review Requirements

- At least **1 approving review** required
- All CI checks must pass
- Coverage thresholds must be met
- No unresolved review comments

### Merge Strategy

We use **squash merge** to keep `main` history clean.

## Architecture Overview

This project follows the architecture defined in `.claude/rules/04-architecture-summary.md`.

Key principles:

- **Dependencies point inward** toward the domain layer
- **Domain layer has zero external dependencies** (standard library only)
- **Ports and adapters** pattern for all I/O operations
- **Implementation order:** domain -> ports -> adapters -> application -> inbound -> tests

Refer to the architecture documentation for package structure and layer rules.

## Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](https://www.contributor-covenant.org/version/2/1/code_of_conduct/).

By participating, you agree to uphold this code. Report unacceptable behavior to the project maintainers.
