# GitHub Actions Patterns

## Workflow Structure

### Job Dependencies

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build
        run: pip build

  test:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Test
        run: pip test

  deploy:
    needs: [build, test]
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment: production
```

### Matrix Builds

```yaml
strategy:
  fail-fast: false
  matrix:
    os: [ubuntu-latest, macos-latest]
    version: ['17', '21']
    exclude:
      - os: macos-latest
        version: '17'
```

### Reusable Workflows

```yaml
# .github/workflows/reusable-build.yml
on:
  workflow_call:
    inputs:
      environment:
        required: true
        type: string
    secrets:
      deploy-token:
        required: true

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    environment: ${{ github.event.inputs.environment }}
```

### Composite Actions

```yaml
# .github/actions/setup-build/action.yml
name: Setup Build Environment
runs:
  using: composite
  steps:
    - name: Cache dependencies
      uses: actions/cache@v4
      with:
        path: ~/.m2/repository
        key: deps-${{ hashFiles('**/pom.xml') }}
    - name: Setup JDK
      uses: actions/setup-java@v4
      with:
        distribution: temurin
        java-version: '21'
```

## Environment Protection Rules

- Required reviewers for production deployments
- Wait timer between staging and production
- Branch restrictions (deploy only from main)
- Custom deployment protection rules via webhooks

## Concurrency Control

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

## Artifact Handling

```yaml
- uses: actions/upload-artifact@v4
  with:
    name: build-output
    path: target/*.jar
    retention-days: 5

- uses: actions/download-artifact@v4
  with:
    name: build-output
```

## Path-Based Triggers

```yaml
on:
  push:
    paths:
      - 'src/**'
      - 'pom.xml'
      - '.github/workflows/ci.yml'
    paths-ignore:
      - 'docs/**'
      - '*.md'
```

## Conditional Steps

```yaml
- name: Deploy to production
  if: >-
    github.event_name == 'push' &&
    github.ref == 'refs/heads/main' &&
    success()
  run: ./deploy.sh production
```
