# Test Plan -- STORY-005: Rules Assembly

## Summary
- Total test classes: 8 (7 unit/integration + 1 contract)
- Total test methods: ~55 (estimated)
- Categories covered: Unit, Contract, Integration
- Estimated line coverage: ~97%

---

## Test Class 1: TestStackPackMapping (`tests/domain/test_stack_pack_mapping.py`)

### Happy Path
| # | Method | Test Name | Description |
|---|--------|-----------|-------------|
| 1 | `get_stack_pack_name` | `test_get_stack_pack_name_known_framework_returns_pack_name` | Parameterized: all 11 mappings |

### Error Path
| # | Exception | Test Name | Trigger |
|---|-----------|-----------|---------|
| 1 | N/A (returns empty) | `test_get_stack_pack_name_unknown_framework_returns_empty` | Unknown framework string |

### Boundary
| # | Boundary | Test Name | Values Tested |
|---|----------|-----------|---------------|
| 1 | Empty input | `test_get_stack_pack_name_empty_string_returns_empty` | `""` |
| 2 | Case | `test_get_stack_pack_name_uppercase_returns_empty` | `"QUARKUS"` |

### Parametrized
| # | Matrix | Test Name | Source | Rows |
|---|--------|-----------|--------|------|
| 1 | All frameworks | `test_get_stack_pack_name_known_framework_returns_pack_name` | inline | 11 |

---

## Test Class 2: TestVersionResolver (`tests/domain/test_version_resolver.py`)

### Happy Path
| # | Method | Test Name | Description |
|---|--------|-----------|-------------|
| 1 | `find_version_dir` | `test_find_version_dir_exact_match_returns_path` | Exact `python-3.9/` dir exists |

### Error Path
| # | Exception | Test Name | Trigger |
|---|-----------|-----------|---------|
| 1 | Returns None | `test_find_version_dir_no_match_returns_none` | Neither dir exists |

### Boundary
| # | Boundary | Test Name | Values Tested |
|---|----------|-----------|---------------|
| 1 | Fallback | `test_find_version_dir_major_fallback_returns_path` | `3.9` -> `python-3.x/` |
| 2 | Multi-dot | `test_find_version_dir_multi_dot_version_extracts_major` | `3.9.1` -> major `3` |
| 3 | Single-segment | `test_find_version_dir_single_segment_version` | `21` -> `name-21.x` |

---

## Test Class 3: TestCoreKpRouting (`tests/domain/test_core_kp_routing.py`)

### Happy Path
| # | Method | Test Name | Description |
|---|--------|-----------|-------------|
| 1 | constants | `test_core_to_kp_mapping_has_expected_count` | 11 entries |
| 2 | constants | `test_conditional_core_kp_has_one_entry` | 1 entry |
| 3 | `get_active_routes` | `test_get_active_routes_non_library_includes_all` | All 12 routes |

### Error Path
| # | Exception | Test Name | Trigger |
|---|-----------|-----------|---------|
| 1 | Exclusion | `test_get_active_routes_library_excludes_cloud_native` | `architecture.style == "library"` |

### Parametrized
| # | Matrix | Test Name | Source | Rows |
|---|--------|-----------|--------|------|
| 1 | Route fields | `test_core_route_fields_non_empty` | CORE_TO_KP_MAPPING | 11 |

---

## Test Class 4: TestAuditor (`tests/assembler/test_auditor.py`)

### Happy Path
| # | Method | Test Name | Description |
|---|--------|-----------|-------------|
| 1 | `audit_rules_context` | `test_audit_under_thresholds_no_warnings` | 5 files, 10KB |
| 2 | `audit_rules_context` | `test_audit_file_sizes_sorted_descending` | Verify sort order |

### Error Path
| # | Exception | Test Name | Trigger |
|---|-----------|-----------|---------|
| 1 | N/A | `test_audit_nonexistent_dir_handles_gracefully` | Missing directory |

### Boundary
| # | Boundary | Test Name | Values Tested |
|---|----------|-----------|---------------|
| 1 | Empty dir | `test_audit_empty_dir_returns_zeros` | 0 files, 0 bytes |
| 2 | File threshold | `test_audit_exceeds_file_count_warns` | 11 files |
| 3 | Size threshold | `test_audit_exceeds_total_size_warns` | >50KB total |
| 4 | Both exceeded | `test_audit_both_thresholds_two_warnings` | 11 files + >50KB |

---

## Test Class 5: TestConsolidator (`tests/assembler/test_consolidator.py`)

### Happy Path
| # | Method | Test Name | Description |
|---|--------|-----------|-------------|
| 1 | `consolidate_files` | `test_consolidate_two_files_with_separator` | Content merged with `---` |
| 2 | `consolidate_files` | `test_consolidate_creates_parent_dirs` | Non-existent parent |
| 3 | `consolidate_framework_rules` | `test_consolidate_framework_produces_three_files` | All groups populated |

### Error Path
| # | Exception | Test Name | Trigger |
|---|-----------|-----------|---------|
| 1 | N/A | `test_consolidate_skips_missing_sources` | Missing source paths |

### Boundary
| # | Boundary | Test Name | Values Tested |
|---|----------|-----------|---------------|
| 1 | Single file | `test_consolidate_single_file_no_separator` | 1 source |
| 2 | Empty list | `test_consolidate_empty_sources` | 0 sources |

### Parametrized
| # | Matrix | Test Name | Source | Rows |
|---|--------|-----------|--------|------|
| 1 | File patterns | `test_framework_file_routed_to_correct_group` | inline | ~12 patterns |

---

## Test Class 6: TestRulesAssembler (`tests/assembler/test_rules_assembler.py`)

### Happy Path
| # | Method | Test Name | Description |
|---|--------|-----------|-------------|
| 1 | `assemble` | `test_assemble_full_config_generates_all_core_rules` | Full pipeline |
| 2 | `assemble` | `test_assemble_returns_existing_paths` | All returned paths exist |
| 3 | `_copy_core_rules` | `test_copy_core_rules_replaces_placeholders` | Substitution |
| 4 | `_route_core_to_kps` | `test_route_core_to_knowledge_packs` | KP directories |
| 5 | `_copy_language_kps` | `test_copy_language_kps_routes_testing_files` | Testing KP routing |
| 6 | `_copy_language_kps` | `test_copy_language_kps_routes_standards_files` | Standards KP |
| 7 | `_copy_framework_kps` | `test_copy_framework_kps_uses_stack_pack_name` | Pack name |
| 8 | `_generate_project_identity` | `test_generate_project_identity_contains_config_values` | Config values |

### Error Path / Conditional
| # | Exception | Test Name | Trigger |
|---|-----------|-----------|---------|
| 1 | Skip | `test_copy_database_references_skipped_for_none` | `db.name == "none"` |
| 2 | Skip | `test_assemble_no_security_skips_security_files` | Empty security |
| 3 | Skip | `test_assemble_cloud_knowledge_skipped_for_none` | No cloud |
| 4 | Include | `test_copy_database_references_for_postgresql` | DB configured |
| 5 | Include | `test_copy_cache_references_for_redis` | Cache configured |
| 6 | Include | `test_assemble_security_rules_when_configured` | Security set |
| 7 | Include | `test_assemble_infrastructure_for_kubernetes` | K8s configured |

---

## Test Class 7: TestRulesAssemblyContract (`tests/test_rules_assembly_contract.py`)

### Contract Tests
| # | Method | Test Name | Description |
|---|--------|-----------|-------------|
| 1 | `assemble` | `test_rules_output_matches_bash_reference` | Byte-by-byte comparison |
| 2 | `assemble` | `test_rules_file_count_matches_bash` | Same file count |
| 3 | `assemble` | `test_rules_directory_structure_matches_bash` | Same tree structure |

---

## Coverage Estimation

| Class | Public Methods | Branches | Est. Tests | Line % | Branch % |
|-------|---------------|----------|-----------|--------|----------|
| `stack_pack_mapping` | 1 | 2 | 4 | 100% | 100% |
| `version_resolver` | 1 | 4 | 5 | 100% | 100% |
| `core_kp_routing` | 1 + 2 constants | 3 | 6 | 100% | 100% |
| `auditor` | 1 + dataclass | 6 | 7 | 100% | 100% |
| `consolidator` | 2 | 8 | 10 | 98% | 95% |
| `rules_assembler` | 1 public + 11 private | 20+ | 15 | 96% | 92% |
| **Total** | | | **~55** | **~97%** | **~94%** |

All modules estimated above 95% line / 90% branch thresholds.

## Risks and Gaps

- **Contract tests depend on pre-captured bash output** — must generate reference output before tests can validate. If bash output not available, contract tests will be deferred or use content-equivalence checks.
- **Framework file grouping** — Edge case files not matching any pattern need explicit handling (test with unmatched file).
- **TemplateEngine integration** — RulesAssembler tests use real TemplateEngine instance with minimal fixtures. If template rendering fails, root cause may be in STORY-004 code.
- **File I/O heavy tests** — `tmp_path` mitigates filesystem side effects but tests may be slower than pure unit tests.
