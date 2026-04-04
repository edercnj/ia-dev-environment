# Test Plan — story-0004-0012: Performance Baseline Tracking

## Summary

- Total test files: 2 (1 new + 1 extended)
- Total test methods: ~40 (estimated)
- Categories covered: Unit (content validation), Integration (golden file parity)
- Estimated line coverage: maintained at ≥ 95%
- Estimated branch coverage: maintained at ≥ 90%

## TPP Progression

> Degenerate (file exists, non-empty) → unconditional (mandatory sections, columns, metrics) →
> conditions (delta interpretation, recommended language, lifecycle prompt) → edge cases (dual copy, backward compat, placeholders)

---

## Test File 1: `tests/node/content/performance-baseline-content.test.ts`

**Pattern:** Follows `deploy-runbook-content.test.ts` (template content validation)

### AT-1: Template file exists and is well-formed

| # | Test Name | Description | TPP Level | Depends On |
|---|-----------|-------------|-----------|------------|
| UT-1 | `templateFile_exists_inResourcesTemplates` | `resources/templates/_TEMPLATE-PERFORMANCE-BASELINE.md` exists | L1 Degenerate | — |
| UT-2 | `templateFile_isNotEmpty_hasContent` | File has non-zero trimmed content | L1 Degenerate | UT-1 |
| Parallel: yes (UT-1 and UT-2 are independent reads) |

### AT-2: Mandatory sections present

| # | Test Name | Description | TPP Level | Depends On |
|---|-----------|-------------|-----------|------------|
| UT-3 | `templateFile_containsTitle_performanceBaselines` | Contains `# Performance Baselines` H1 heading | L2 Unconditional | UT-1 |
| UT-4 | `templateFile_containsMeasurementGuideSection` | Contains `## Measurement Guide` H2 heading | L2 Unconditional | UT-1 |
| UT-5 | `templateFile_containsBaselinesSection` | Contains `## Baselines` H2 heading | L2 Unconditional | UT-1 |
| Parallel: yes (UT-3 through UT-5 are independent content checks) |

### AT-3: Baselines table with standardized columns

| # | Test Name | Description | TPP Level | Depends On |
|---|-----------|-------------|-----------|------------|
| UT-6 | `templateFile_baselinesTable_containsAllColumns` | Parametrized `it.each` over 7 columns: Feature/Story ID, Date, Metric, Before, After, Delta, Notes | L2 Unconditional | UT-5 |
| Parallel: yes (each column check independent) |

### AT-4: Measurement Guide documents all metrics

| # | Test Name | Description | TPP Level | Depends On |
|---|-----------|-------------|-----------|------------|
| UT-7 | `templateFile_measurementGuide_containsAllMetrics` | Parametrized `it.each` over 6 metrics: `latency_p50`, `latency_p95`, `latency_p99`, `throughput_rps`, `memory_mb`, `startup_ms` | L2 Unconditional | UT-4 |
| Parallel: yes (each metric check independent) |

### AT-5: Measurement Guide includes tools by stack

| # | Test Name | Description | TPP Level | Depends On |
|---|-----------|-------------|-----------|------------|
| UT-8 | `templateFile_measurementGuide_containsToolsByStack` | Contains "Tools by Stack" or equivalent table/section | L2 Unconditional | UT-4 |
| UT-9 | `templateFile_measurementGuide_containsLanguagePlaceholder` | Contains `{{LANGUAGE}}` runtime placeholder | L3 Condition | UT-8 |
| UT-10 | `templateFile_measurementGuide_containsFrameworkPlaceholder` | Contains `{{FRAMEWORK}}` runtime placeholder | L3 Condition | UT-8 |
| Parallel: yes |

### AT-6: Delta interpretation with severity thresholds

| # | Test Name | Description | TPP Level | Depends On |
|---|-----------|-------------|-----------|------------|
| UT-11 | `templateFile_containsDeltaInterpretation` | Contains Delta Interpretation section or equivalent | L3 Condition | UT-5 |
| UT-12 | `templateFile_deltaInterpretation_containsAcceptableThreshold` | Contains `<= +10%` or equivalent acceptable range | L3 Condition | UT-11 |
| UT-13 | `templateFile_deltaInterpretation_containsWarningThreshold` | Contains `+10% to +25%` or warning range | L3 Condition | UT-11 |
| UT-14 | `templateFile_deltaInterpretation_containsInvestigationThreshold` | Contains `> +25%` or investigation range | L3 Condition | UT-11 |
| Parallel: yes (UT-12 through UT-14 independent) |

### AT-7: Example baseline row

| # | Test Name | Description | TPP Level | Depends On |
|---|-----------|-------------|-----------|------------|
| UT-15 | `templateFile_baselines_containsExampleRow` | Contains an example/sample row in the baselines table | L4 Edge | UT-6 |
| Parallel: yes |

### AT-8: Measurement conditions

| # | Test Name | Description | TPP Level | Depends On |
|---|-----------|-------------|-----------|------------|
| UT-16 | `templateFile_measurementGuide_containsMeasurementConditions` | Contains guidance on measurement conditions (environment, warm-up, repetitions) | L2 Unconditional | UT-4 |
| Parallel: yes |

---

## Test File 2: `tests/node/content/x-dev-lifecycle-doc-phase.test.ts` (EXTEND)

**Pattern:** Follows existing structure with new `describe` blocks for performance baseline

### AT-9: Claude source — Performance baseline prompt in Phase 3

| # | Test Name | Description | TPP Level | Depends On |
|---|-----------|-------------|-----------|------------|
| UT-17 | `claudeSource_phase3_containsPerformanceBaselineHeading` | Contains "Performance Baseline" heading or subsection within Phase 3 | L2 Unconditional | — |
| UT-18 | `claudeSource_phase3_performanceBaselineRecommended` | Contains "recommended" or "Recommended" (not mandatory) | L3 Condition | UT-17 |
| UT-19 | `claudeSource_phase3_performanceBaselineSkipNotBlock` | Contains language indicating skip does not block phase | L3 Condition | UT-17 |
| UT-20 | `claudeSource_phase3_referencesTemplateFile` | Contains reference to `_TEMPLATE-PERFORMANCE-BASELINE.md` | L2 Unconditional | UT-17 |
| UT-21 | `claudeSource_phase3_referencesOutputFile` | Contains reference to `docs/performance/baselines.md` | L2 Unconditional | UT-17 |
| UT-22 | `claudeSource_phase3_containsDeltaWarningThreshold` | Contains `> 10%` or `10%` warning threshold | L3 Condition | UT-17 |
| UT-23 | `claudeSource_phase3_containsInvestigationThreshold` | Contains `> 25%` or `25%` investigation threshold | L3 Condition | UT-17 |
| Parallel: yes (all independent content checks on Claude source) |

### AT-10: Claude source — Performance baseline positioning

| # | Test Name | Description | TPP Level | Depends On |
|---|-----------|-------------|-----------|------------|
| UT-24 | `claudeSource_performanceBaseline_withinPhase3` | Performance Baseline text appears after Phase 3 heading and before Phase 4 heading | L4 Edge | UT-17 |
| Parallel: no (depends on UT-17 context) |

### AT-11: GitHub source — Performance baseline prompt in Phase 3

| # | Test Name | Description | TPP Level | Depends On |
|---|-----------|-------------|-----------|------------|
| UT-25 | `githubSource_phase3_containsPerformanceBaselineHeading` | Contains "Performance Baseline" heading or subsection | L2 Unconditional | — |
| UT-26 | `githubSource_phase3_performanceBaselineRecommended` | Contains "recommended" or "Recommended" | L3 Condition | UT-25 |
| UT-27 | `githubSource_phase3_performanceBaselineSkipNotBlock` | Contains language indicating skip does not block phase | L3 Condition | UT-25 |
| UT-28 | `githubSource_phase3_referencesTemplateFile` | Contains reference to `_TEMPLATE-PERFORMANCE-BASELINE.md` | L2 Unconditional | UT-25 |
| UT-29 | `githubSource_phase3_referencesOutputFile` | Contains reference to `docs/performance/baselines.md` | L2 Unconditional | UT-25 |
| UT-30 | `githubSource_phase3_containsDeltaWarningThreshold` | Contains `> 10%` or `10%` warning | L3 Condition | UT-25 |
| UT-31 | `githubSource_phase3_containsInvestigationThreshold` | Contains `> 25%` or `25%` investigation | L3 Condition | UT-25 |
| Parallel: yes |

### AT-12: Dual copy consistency — Performance baseline (RULE-001)

| # | Test Name | Description | TPP Level | Depends On |
|---|-----------|-------------|-----------|------------|
| UT-32 | `dualCopy_bothContainPerformanceBaselineHeading` | Both Claude and GitHub contain "Performance Baseline" | L2 Unconditional | — |
| UT-33 | `dualCopy_bothContainPerformanceBaselineRecommended` | Both contain "recommended" language | L3 Condition | UT-32 |
| UT-34 | `dualCopy_bothReferenceTemplateFile` | Both reference `_TEMPLATE-PERFORMANCE-BASELINE.md` | L2 Unconditional | UT-32 |
| UT-35 | `dualCopy_bothReferenceOutputFile` | Both reference `docs/performance/baselines.md` | L2 Unconditional | UT-32 |
| UT-36 | `dualCopy_bothContainDeltaWarningThreshold` | Both contain 10% warning threshold | L3 Condition | UT-32 |
| UT-37 | `dualCopy_bothContainInvestigationThreshold` | Both contain 25% investigation threshold | L3 Condition | UT-32 |
| Parallel: yes |

### IT-1: Golden file parity (24 files)

| # | Test Name | Description | TPP Level | Depends On |
|---|-----------|-------------|-----------|------------|
| IT-1 | `byte-for-byte.test.ts` (existing, no changes) | Validates all 24 golden files match pipeline output after source template updates | Integration | All UTs |
| Parallel: handled by existing test infrastructure |

**Update strategy:** After editing source templates, copy to 24 golden files using mechanical script:
- 8 profiles × `.claude/` (copy Claude source)
- 8 profiles × `.agents/` (copy Claude source)
- 8 profiles × `.github/` (copy GitHub source)

---

## Coverage Estimation

| Component | Public API | Branches | Est. Tests | Line % | Branch % |
|-----------|-----------|----------|-----------|--------|----------|
| `_TEMPLATE-PERFORMANCE-BASELINE.md` | N/A (static) | 0 | 16 | 100% | N/A |
| `x-dev-lifecycle` Claude source (perf baseline section) | N/A (static) | 0 | 8 | 100% | N/A |
| `x-dev-lifecycle` GitHub source (perf baseline section) | N/A (static) | 0 | 7 | 100% | N/A |
| Dual copy consistency | N/A | 0 | 6 | 100% | N/A |
| Golden file parity | N/A | 0 | 0 (existing) | 100% | N/A |
| **Total** | — | — | **~37** | **maintained ≥ 95%** | **maintained ≥ 90%** |

## Risks and Gaps

- **No runtime code to test:** This is a content-only story. All tests are static content validation. No TypeScript production code changes means coverage metrics are maintained (not improved, not degraded).
- **Golden file mechanical copy:** If the copy script misses a profile or copies the wrong source file, byte-for-byte tests will catch it.
- **Dual copy divergence:** If the GitHub source gets different content than the Claude source, the dual copy consistency tests will catch it.
- **Dependency on story-0004-0005:** The documentation phase must already exist in the lifecycle SKILL.md. Tests assume Phase 3 Documentation heading is present.

## Acceptance Criteria → Test Mapping

| AC# | Story Scenario | Test IDs |
|-----|---------------|----------|
| AC-1 | Template generated with mandatory sections | UT-1, UT-2, UT-3, UT-4, UT-5 |
| AC-2 | Baselines table with standardized columns | UT-6 (parametrized × 7) |
| AC-3 | Measurement Guide documents metrics | UT-7 (parametrized × 6), UT-8, UT-16 |
| AC-4 | Delta calculation with regression warnings | UT-11, UT-12, UT-13, UT-14 |
| AC-5 | Recording is recommended, not mandatory | UT-18, UT-19, UT-26, UT-27, UT-33 |
| AC-6 | Backward compatibility | UT-19 (skip not block), UT-15 (example row as guidance) |
| AC-7 | Dual copy consistency (RULE-001) | UT-32 through UT-37 |
| AC-8 | Golden file tests validating output | IT-1 (byte-for-byte) |
