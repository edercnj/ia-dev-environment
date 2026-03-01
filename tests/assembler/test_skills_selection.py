from __future__ import annotations

import pytest

from claude_setup.assembler.skills import (
    CORE_KNOWLEDGE_PACKS,
    STACK_PACK_MAP,
    SkillsAssembler,
)


@pytest.fixture
def assembler():
    return SkillsAssembler()


# --- select_conditional_skills: 13 rules ---


class TestSelectConditionalSkills:

    def test_rest_includes_x_review_api(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(interfaces=[{"type": "rest"}])
        assert "x-review-api" in assembler.select_conditional_skills(config)

    def test_no_rest_excludes_x_review_api(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_conditional_skills(minimal_cli_config)
        assert "x-review-api" not in result

    def test_grpc_includes_x_review_grpc(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(interfaces=[{"type": "grpc"}])
        assert "x-review-grpc" in assembler.select_conditional_skills(config)

    def test_no_grpc_excludes_x_review_grpc(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_conditional_skills(minimal_cli_config)
        assert "x-review-grpc" not in result

    def test_graphql_includes_x_review_graphql(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(interfaces=[{"type": "graphql"}])
        assert "x-review-graphql" in assembler.select_conditional_skills(config)

    def test_event_consumer_includes_x_review_events(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(interfaces=[{"type": "event-consumer"}])
        assert "x-review-events" in assembler.select_conditional_skills(config)

    def test_event_producer_includes_x_review_events(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(interfaces=[{"type": "event-producer"}])
        assert "x-review-events" in assembler.select_conditional_skills(config)

    def test_no_events_excludes_x_review_events(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(interfaces=[{"type": "rest"}])
        assert "x-review-events" not in assembler.select_conditional_skills(config)

    def test_observability_includes_instrument_otel(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            infrastructure={
                "observability": {"tool": "opentelemetry"},
            },
        )
        assert "instrument-otel" in assembler.select_conditional_skills(config)

    def test_observability_none_excludes_instrument_otel(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_conditional_skills(minimal_cli_config)
        assert "instrument-otel" not in result

    def test_orchestrator_includes_setup_environment(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            infrastructure={"orchestrator": "kubernetes"},
        )
        assert "setup-environment" in assembler.select_conditional_skills(config)

    def test_orchestrator_none_excludes_setup_environment(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_conditional_skills(minimal_cli_config)
        assert "setup-environment" not in result

    def test_smoke_rest_includes_run_smoke_api(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            interfaces=[{"type": "rest"}],
            testing={"smoke_tests": True},
        )
        assert "run-smoke-api" in assembler.select_conditional_skills(config)

    def test_smoke_false_excludes_run_smoke_api(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            interfaces=[{"type": "rest"}],
            testing={"smoke_tests": False},
        )
        assert "run-smoke-api" not in assembler.select_conditional_skills(config)

    def test_smoke_no_rest_excludes_run_smoke_api(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            interfaces=[{"type": "cli"}],
            testing={"smoke_tests": True},
        )
        assert "run-smoke-api" not in assembler.select_conditional_skills(config)

    def test_smoke_tcp_includes_run_smoke_socket(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            interfaces=[{"type": "tcp-custom"}],
            testing={"smoke_tests": True},
        )
        assert "run-smoke-socket" in assembler.select_conditional_skills(config)

    def test_smoke_no_tcp_excludes_run_smoke_socket(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            interfaces=[{"type": "rest"}],
            testing={"smoke_tests": True},
        )
        assert "run-smoke-socket" not in assembler.select_conditional_skills(config)

    def test_always_includes_run_e2e(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_conditional_skills(minimal_cli_config)
        assert "run-e2e" in result

    def test_performance_true_includes_run_perf_test(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            testing={"performance_tests": True},
        )
        assert "run-perf-test" in assembler.select_conditional_skills(config)

    def test_performance_false_excludes_run_perf_test(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            testing={"performance_tests": False},
        )
        assert "run-perf-test" not in assembler.select_conditional_skills(config)

    def test_contract_true_includes_run_contract_tests(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            testing={"contract_tests": True},
        )
        assert "run-contract-tests" in assembler.select_conditional_skills(config)

    def test_contract_false_excludes_run_contract_tests(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_conditional_skills(minimal_cli_config)
        assert "run-contract-tests" not in result

    def test_security_frameworks_includes_x_review_security(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            security={"frameworks": ["owasp"]},
        )
        assert "x-review-security" in assembler.select_conditional_skills(config)

    def test_security_empty_excludes_x_review_security(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_conditional_skills(minimal_cli_config)
        assert "x-review-security" not in result

    def test_api_gateway_includes_x_review_gateway(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            infrastructure={"api_gateway": "kong"},
        )
        assert "x-review-gateway" in assembler.select_conditional_skills(config)

    def test_api_gateway_none_excludes_x_review_gateway(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_conditional_skills(minimal_cli_config)
        assert "x-review-gateway" not in result

    @pytest.mark.parametrize(
        "skill_name",
        [
            "x-review-api",
            "x-review-grpc",
            "x-review-events",
            "instrument-otel",
            "setup-environment",
            "run-smoke-api",
            "run-e2e",
            "run-perf-test",
            "run-contract-tests",
            "x-review-security",
            "x-review-gateway",
        ],
        ids=lambda s: s,
    )
    def test_full_featured_includes_all_applicable(
        self, assembler, full_featured_config, skill_name,
    ) -> None:
        result = assembler.select_conditional_skills(full_featured_config)
        assert skill_name in result

    def test_minimal_cli_only_always_on(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_conditional_skills(minimal_cli_config)
        assert "run-e2e" in result
        assert "run-perf-test" in result


# --- select_knowledge_packs ---


class TestSelectKnowledgePacks:

    def test_always_includes_core_packs(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_knowledge_packs(minimal_cli_config)
        for pack in CORE_KNOWLEDGE_PACKS:
            assert pack in result

    def test_always_includes_layer_templates(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_knowledge_packs(minimal_cli_config)
        assert "layer-templates" in result

    def test_database_includes_database_patterns(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            data={"database": {"name": "postgresql", "version": "15"}},
        )
        result = assembler.select_knowledge_packs(config)
        assert "database-patterns" in result

    def test_cache_includes_database_patterns(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            data={"cache": {"name": "redis", "version": "7"}},
        )
        result = assembler.select_knowledge_packs(config)
        assert "database-patterns" in result

    def test_no_db_no_cache_excludes_database_patterns(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_knowledge_packs(minimal_cli_config)
        assert "database-patterns" not in result


# --- STACK_PACK_MAP ---


class TestStackPackMap:

    @pytest.mark.parametrize(
        "framework_name, expected_pack",
        [
            ("quarkus", "quarkus-patterns"),
            ("spring-boot", "spring-patterns"),
            ("nestjs", "nestjs-patterns"),
            ("express", "express-patterns"),
            ("fastapi", "fastapi-patterns"),
            ("django", "django-patterns"),
            ("gin", "gin-patterns"),
            ("ktor", "ktor-patterns"),
            ("axum", "axum-patterns"),
            ("dotnet", "dotnet-patterns"),
            ("click", "click-cli-patterns"),
        ],
    )
    def test_stack_pack_mapping(
        self, framework_name, expected_pack,
    ) -> None:
        assert STACK_PACK_MAP[framework_name] == expected_pack

    def test_map_has_eleven_entries(self) -> None:
        assert len(STACK_PACK_MAP) == 11


# --- select_core_skills ---


class TestSelectCoreSkills:

    def test_scans_core_directories(
        self, assembler, tmp_path,
    ) -> None:
        core = tmp_path / "skills-templates" / "core"
        (core / "coding-standards").mkdir(parents=True)
        (core / "architecture").mkdir()
        result = assembler.select_core_skills(tmp_path)
        assert "coding-standards" in result
        assert "architecture" in result

    def test_excludes_lib_from_top_level(
        self, assembler, tmp_path,
    ) -> None:
        core = tmp_path / "skills-templates" / "core"
        (core / "lib" / "some-util").mkdir(parents=True)
        (core / "coding-standards").mkdir(parents=True)
        result = assembler.select_core_skills(tmp_path)
        assert "lib" not in result

    def test_includes_lib_entries(
        self, assembler, tmp_path,
    ) -> None:
        core = tmp_path / "skills-templates" / "core"
        (core / "lib" / "some-util").mkdir(parents=True)
        result = assembler.select_core_skills(tmp_path)
        assert "lib/some-util" in result

    def test_empty_core_returns_empty(
        self, assembler, tmp_path,
    ) -> None:
        core = tmp_path / "skills-templates" / "core"
        core.mkdir(parents=True)
        assert assembler.select_core_skills(tmp_path) == []

    def test_no_core_dir_returns_empty(
        self, assembler, tmp_path,
    ) -> None:
        assert assembler.select_core_skills(tmp_path) == []
