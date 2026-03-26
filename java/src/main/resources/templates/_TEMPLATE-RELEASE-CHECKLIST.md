# Release Checklist — {{ project_name }}

## 1. Pre-Release Validation

- [ ] All unit tests passing
- [ ] All integration tests passing
- [ ] Coverage meets threshold (>= {{ coverage_line }}% line, >= {{ coverage_branch }}% branch)
- [ ] No open blocker/critical issues
- [ ] Security scan clean (SAST, dependency audit)
- [ ] Dependency audit clean (no known critical CVEs)

## 2. Version & Changelog

- [ ] Version number validated per Semantic Versioning
- [ ] Version bumped in project files ({{ build_tool }} config)
- [ ] CHANGELOG.md updated with all changes since last release
- [ ] Release notes drafted for stakeholders
- [ ] Breaking changes documented with migration guide

## 3. Artifact Build

- [ ] Application artifact built successfully ({{ build_tool }})
- [ ] Artifact size within expected range
{% if container != "none" %}
- [ ] Container image built and tagged with version
- [ ] Container image tagged with `latest`
{% endif %}
{% if native_build == true %}
- [ ] Native image built and smoke-tested
{% endif %}

## 4. Quality Gate

- [ ] Smoke tests pass against built artifact
- [ ] Performance baseline met (no regression > 10%)
{% if contract_tests == "True" %}
- [ ] Contract tests pass
{% endif %}
- [ ] API compatibility verified (no unintended breaking changes)

## 5. Publish

- [ ] Artifact published to registry
{% if container != "none" %}
- [ ] Container image pushed to registry
{% endif %}
- [ ] GitHub Release created with release notes
- [ ] Git tag created (v{version})
- [ ] Release signed (GPG or Sigstore)

## 6. Post-Release

- [ ] Monitoring dashboards reviewed (error rate, latency)
- [ ] Rollback plan verified and documented
- [ ] Release communication sent (team, stakeholders)
- [ ] Documentation updated (API docs, user guides)
- [ ] Next version placeholder added to CHANGELOG.md
