from __future__ import annotations

import copy
import json
import logging
from pathlib import Path
import pytest

from ia_dev_env.assembler.github_hooks_assembler import (
    HOOK_TEMPLATES,
    GithubHooksAssembler,
)
from ia_dev_env.models import ProjectConfig
from ia_dev_env.template_engine import TemplateEngine

from tests.conftest import FULL_PROJECT_DICT, MINIMAL_PROJECT_DICT

HOOK_NAMES = HOOK_TEMPLATES

EXPECTED_EVENTS = {
    "post-compile-check.json": "postToolUse",
    "pre-commit-lint.json": "preToolUse",
    "session-context-loader.json": "sessionStart",
}

EXPECTED_TIMEOUTS = {
    "post-compile-check.json": 60000,
    "pre-commit-lint.json": 30000,
    "session-context-loader.json": 10000,
}

MAX_TIMEOUT_MS = 60000


def _make_config(**overrides) -> ProjectConfig:
    base = copy.deepcopy(FULL_PROJECT_DICT)
    for key, value in overrides.items():
        base[key] = value
    return ProjectConfig.from_dict(base)


def _make_minimal_config(**overrides) -> ProjectConfig:
    base = copy.deepcopy(MINIMAL_PROJECT_DICT)
    for key, value in overrides.items():
        base[key] = value
    return ProjectConfig.from_dict(base)


def _create_hook_templates(resources: Path) -> None:
    """Create hook template files for testing."""
    hooks_dir = resources / "github-hooks-templates"
    hooks_dir.mkdir(parents=True, exist_ok=True)
    templates = {
        "post-compile-check.json": {
            "hooks": [{
                "event": "postToolUse",
                "matcher": {"tool": "edit_file"},
                "command": "scripts/post-compile-check.sh",
                "timeout": 60000,
                "description": "Verify compilation after file edits",
            }],
        },
        "pre-commit-lint.json": {
            "hooks": [{
                "event": "preToolUse",
                "matcher": {"tool": "git_commit"},
                "command": "scripts/pre-commit-lint.sh",
                "timeout": 30000,
                "description": "Run lint checks before committing",
            }],
        },
        "session-context-loader.json": {
            "hooks": [{
                "event": "sessionStart",
                "command": "scripts/load-context.sh",
                "timeout": 10000,
                "description": (
                    "Load project context at session start"
                ),
            }],
        },
    }
    for name, data in templates.items():
        path = hooks_dir / name
        path.write_text(
            json.dumps(data, indent=2) + "\n",
            encoding="utf-8",
        )



@pytest.fixture
def assembled_full(tmp_path: Path):
    """Assemble hooks with FULL config."""
    resources = tmp_path / "res"
    output = tmp_path / "out"
    _create_hook_templates(resources)
    config = _make_config()
    engine = TemplateEngine(resources, config)
    assembler = GithubHooksAssembler(resources)
    result = assembler.assemble(config, output, engine)
    return result, output


@pytest.fixture
def assembled_minimal(tmp_path: Path):
    """Assemble hooks with MINIMAL config."""
    resources = tmp_path / "res"
    output = tmp_path / "out"
    _create_hook_templates(resources)
    config = _make_minimal_config()
    engine = TemplateEngine(resources, config)
    assembler = GithubHooksAssembler(resources)
    result = assembler.assemble(config, output, engine)
    return result, output


class TestAssembleHooks:

    def test_generates_all_hooks(
        self, assembled_full,
    ) -> None:
        result, output = assembled_full
        for name in HOOK_NAMES:
            expected = (
                output / "github" / "hooks" / name
            )
            assert expected.exists(), (
                f"{name} not generated"
            )

    def test_returns_three_paths(
        self, assembled_full,
    ) -> None:
        result, _ = assembled_full
        assert len(result) == 3

    def test_returns_list_of_paths(
        self, assembled_full,
    ) -> None:
        result, _ = assembled_full
        assert isinstance(result, list)
        for path in result:
            assert isinstance(path, Path)
            assert path.exists()

    def test_minimal_config_generates_all_hooks(
        self, assembled_minimal,
    ) -> None:
        result, _ = assembled_minimal
        assert len(result) == 3


class TestHookJsonValidity:

    @pytest.mark.parametrize("hook_name", HOOK_NAMES)
    def test_valid_json(
        self, hook_name: str, assembled_full,
    ) -> None:
        _, output = assembled_full
        hook_file = (
            output / "github" / "hooks" / hook_name
        )
        content = hook_file.read_text(encoding="utf-8")
        data = json.loads(content)
        assert isinstance(data, dict)

    @pytest.mark.parametrize("hook_name", HOOK_NAMES)
    def test_hooks_array_present(
        self, hook_name: str, assembled_full,
    ) -> None:
        _, output = assembled_full
        hook_file = (
            output / "github" / "hooks" / hook_name
        )
        data = json.loads(
            hook_file.read_text(encoding="utf-8"),
        )
        assert "hooks" in data
        assert isinstance(data["hooks"], list)
        assert len(data["hooks"]) >= 1

    @pytest.mark.parametrize("hook_name", HOOK_NAMES)
    def test_hook_has_required_fields(
        self, hook_name: str, assembled_full,
    ) -> None:
        _, output = assembled_full
        hook_file = (
            output / "github" / "hooks" / hook_name
        )
        data = json.loads(
            hook_file.read_text(encoding="utf-8"),
        )
        hook = data["hooks"][0]
        assert "event" in hook
        assert "command" in hook
        assert "timeout" in hook
        assert "description" in hook


class TestHookEventTypes:

    @pytest.mark.parametrize("hook_name", HOOK_NAMES)
    def test_event_type_matches(
        self, hook_name: str, assembled_full,
    ) -> None:
        _, output = assembled_full
        hook_file = (
            output / "github" / "hooks" / hook_name
        )
        data = json.loads(
            hook_file.read_text(encoding="utf-8"),
        )
        hook = data["hooks"][0]
        assert hook["event"] == EXPECTED_EVENTS[hook_name]

    def test_post_compile_has_matcher(
        self, assembled_full,
    ) -> None:
        _, output = assembled_full
        hook_file = (
            output / "github" / "hooks"
            / "post-compile-check.json"
        )
        data = json.loads(
            hook_file.read_text(encoding="utf-8"),
        )
        hook = data["hooks"][0]
        assert hook["matcher"] == {"tool": "edit_file"}

    def test_pre_commit_lint_has_matcher(
        self, assembled_full,
    ) -> None:
        _, output = assembled_full
        hook_file = (
            output / "github" / "hooks"
            / "pre-commit-lint.json"
        )
        data = json.loads(
            hook_file.read_text(encoding="utf-8"),
        )
        hook = data["hooks"][0]
        assert hook["matcher"] == {"tool": "git_commit"}

    def test_session_loader_has_no_matcher(
        self, assembled_full,
    ) -> None:
        _, output = assembled_full
        hook_file = (
            output / "github" / "hooks"
            / "session-context-loader.json"
        )
        data = json.loads(
            hook_file.read_text(encoding="utf-8"),
        )
        hook = data["hooks"][0]
        assert "matcher" not in hook


class TestHookTimeouts:

    @pytest.mark.parametrize("hook_name", HOOK_NAMES)
    def test_timeout_within_limit(
        self, hook_name: str, assembled_full,
    ) -> None:
        _, output = assembled_full
        hook_file = (
            output / "github" / "hooks" / hook_name
        )
        data = json.loads(
            hook_file.read_text(encoding="utf-8"),
        )
        hook = data["hooks"][0]
        assert hook["timeout"] <= MAX_TIMEOUT_MS

    @pytest.mark.parametrize("hook_name", HOOK_NAMES)
    def test_timeout_value_matches(
        self, hook_name: str, assembled_full,
    ) -> None:
        _, output = assembled_full
        hook_file = (
            output / "github" / "hooks" / hook_name
        )
        data = json.loads(
            hook_file.read_text(encoding="utf-8"),
        )
        hook = data["hooks"][0]
        assert hook["timeout"] == EXPECTED_TIMEOUTS[hook_name]


class TestEdgeCases:

    def test_missing_templates_dir_returns_empty(
        self, tmp_path: Path,
    ) -> None:
        resources = tmp_path / "empty-res"
        resources.mkdir()
        output = tmp_path / "out"
        config = _make_minimal_config()
        engine = TemplateEngine(resources, config)
        assembler = GithubHooksAssembler(resources)
        result = assembler.assemble(config, output, engine)
        assert result == []

    def test_missing_template_file_logs_warning(
        self, tmp_path: Path, caplog,
    ) -> None:
        resources = tmp_path / "res"
        hooks_dir = (
            resources / "github-hooks-templates"
        )
        hooks_dir.mkdir(parents=True, exist_ok=True)
        # Create only one of three templates
        (hooks_dir / "post-compile-check.json").write_text(
            '{"hooks": []}', encoding="utf-8",
        )
        output = tmp_path / "out"
        config = _make_minimal_config()
        engine = TemplateEngine(resources, config)
        assembler = GithubHooksAssembler(resources)
        with caplog.at_level(logging.WARNING):
            result = assembler.assemble(
                config, output, engine,
            )
        assert len(result) == 1
        assert any(
            "Hook template not found" in msg
            for msg in caplog.messages
        )

    def test_missing_templates_dir_logs_warning(
        self, tmp_path: Path, caplog,
    ) -> None:
        resources = tmp_path / "res"
        resources.mkdir()
        output = tmp_path / "out"
        config = _make_minimal_config()
        engine = TemplateEngine(resources, config)
        assembler = GithubHooksAssembler(resources)
        with caplog.at_level(logging.WARNING):
            assembler.assemble(config, output, engine)
        assert any(
            "Templates dir not found" in msg
            for msg in caplog.messages
        )
