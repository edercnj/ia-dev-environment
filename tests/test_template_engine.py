from __future__ import annotations

from pathlib import Path

import pytest
from jinja2 import StrictUndefined, TemplateNotFound, UndefinedError

from claude_setup.models import ProjectConfig
from claude_setup.template_engine import (
    TemplateEngine,
    _build_default_context,
)

FIXTURES_DIR = Path(__file__).parent / "fixtures"
TEMPLATES_DIR = FIXTURES_DIR / "templates"

EXPECTED_CONTEXT_KEYS = {
    "project_name",
    "project_purpose",
    "language_name",
    "language_version",
    "framework_name",
    "framework_version",
    "build_tool",
    "architecture_style",
    "domain_driven",
    "event_driven",
    "container",
    "orchestrator",
    "database_name",
    "cache_name",
    "coverage_line",
    "coverage_branch",
}

EXPECTED_CONTEXT_KEY_COUNT = 16


@pytest.fixture
def full_config(full_project_dict: dict) -> ProjectConfig:
    return ProjectConfig.from_dict(full_project_dict)


@pytest.fixture
def minimal_config(minimal_project_dict: dict) -> ProjectConfig:
    return ProjectConfig.from_dict(minimal_project_dict)


@pytest.fixture
def engine(full_config: ProjectConfig) -> TemplateEngine:
    return TemplateEngine(TEMPLATES_DIR, full_config)


# --- __init__ ---


class TestInit:

    def test_init_stores_config(
        self, full_config: ProjectConfig,
    ) -> None:
        eng = TemplateEngine(TEMPLATES_DIR, full_config)
        assert eng._config is full_config

    def test_init_sets_jinja2_options(
        self, full_config: ProjectConfig,
    ) -> None:
        eng = TemplateEngine(TEMPLATES_DIR, full_config)
        env = eng._env
        assert env.keep_trailing_newline is True
        assert env.trim_blocks is False
        assert env.lstrip_blocks is False
        assert env.undefined is StrictUndefined

    def test_init_nonexistent_src_dir(
        self, full_config: ProjectConfig,
    ) -> None:
        nonexistent = Path("/tmp/nonexistent_dir_xyz")
        eng = TemplateEngine(nonexistent, full_config)
        assert eng._env is not None


# --- _build_default_context ---


class TestBuildDefaultContext:

    def test_build_default_context_full_config(
        self, full_config: ProjectConfig,
    ) -> None:
        ctx = _build_default_context(full_config)
        assert set(ctx.keys()) == EXPECTED_CONTEXT_KEYS
        assert len(ctx) == EXPECTED_CONTEXT_KEY_COUNT

    def test_build_default_context_minimal_config(
        self, minimal_config: ProjectConfig,
    ) -> None:
        ctx = _build_default_context(minimal_config)
        assert len(ctx) == EXPECTED_CONTEXT_KEY_COUNT
        assert ctx["database_name"] == "none"
        assert ctx["cache_name"] == "none"
        assert ctx["orchestrator"] == "none"

    def test_build_default_context_values_match_config(
        self, full_config: ProjectConfig,
    ) -> None:
        ctx = _build_default_context(full_config)
        assert ctx["project_name"] == "my-service"
        assert ctx["project_purpose"] == "A sample service"
        assert ctx["language_name"] == "python"
        assert ctx["language_version"] == "3.9"
        assert ctx["framework_name"] == "click"
        assert ctx["framework_version"] == "8.1"
        assert ctx["build_tool"] == "pip"
        assert ctx["architecture_style"] == "hexagonal"
        assert ctx["domain_driven"] is True
        assert ctx["event_driven"] is False
        assert ctx["container"] == "docker"
        assert ctx["orchestrator"] == "kubernetes"
        assert ctx["database_name"] == "postgresql"
        assert ctx["cache_name"] == "redis"
        assert ctx["coverage_line"] == 95
        assert ctx["coverage_branch"] == 90


# --- render_template ---


class TestRenderTemplate:

    def test_render_template_resolves_variables(
        self, engine: TemplateEngine,
    ) -> None:
        result = engine.render_template(Path("simple.md.j2"))
        assert "my-service" in result
        assert "click" in result

    def test_render_template_no_residual_placeholders(
        self, engine: TemplateEngine,
    ) -> None:
        result = engine.render_template(Path("multivar.md.j2"))
        assert "{{" not in result
        assert "}}" not in result

    def test_render_template_with_context_override(
        self, engine: TemplateEngine, tmp_path: Path,
    ) -> None:
        tpl = tmp_path / "custom.j2"
        tpl.write_text("{{ custom_key }}")
        eng = TemplateEngine(
            tmp_path,
            engine._config,
        )
        result = eng.render_template(
            Path("custom.j2"),
            {"custom_key": "custom_value"},
        )
        assert result == "custom_value"

    def test_render_template_context_override_takes_precedence(
        self, engine: TemplateEngine,
    ) -> None:
        result = engine.render_template(
            Path("simple.md.j2"),
            {"project_name": "overridden"},
        )
        assert "overridden" in result
        assert "my-service" not in result

    def test_render_template_preserves_whitespace(
        self, engine: TemplateEngine,
    ) -> None:
        result = engine.render_template(
            Path("whitespace.txt.j2"),
        )
        assert result.startswith("  Leading spaces")
        assert result.endswith("Trailing newline below\n")
        assert "\n\n" in result

    def test_render_template_missing_variable_raises(
        self, engine: TemplateEngine, tmp_path: Path,
    ) -> None:
        tpl = tmp_path / "missing.j2"
        tpl.write_text("{{ undefined_var }}")
        eng = TemplateEngine(tmp_path, engine._config)
        with pytest.raises(UndefinedError):
            eng.render_template(Path("missing.j2"))

    def test_render_template_nonexistent_file_raises(
        self, engine: TemplateEngine,
    ) -> None:
        with pytest.raises(TemplateNotFound):
            engine.render_template(Path("nonexistent.md.j2"))

    def test_render_template_empty_template(
        self, tmp_path: Path,
        full_config: ProjectConfig,
    ) -> None:
        tpl = tmp_path / "empty.j2"
        tpl.write_text("")
        eng = TemplateEngine(tmp_path, full_config)
        result = eng.render_template(Path("empty.j2"))
        assert result == ""

    def test_render_template_multiline(
        self, engine: TemplateEngine,
    ) -> None:
        result = engine.render_template(Path("multivar.md.j2"))
        lines = result.split("\n")
        assert len(lines) > 1
        assert "python 3.9" in result

    def test_render_template_none_context_uses_defaults(
        self, engine: TemplateEngine,
    ) -> None:
        result = engine.render_template(
            Path("simple.md.j2"), None,
        )
        assert "my-service" in result


# --- render_string ---


class TestRenderString:

    def test_render_string_simple_variable(
        self, engine: TemplateEngine,
    ) -> None:
        result = engine.render_string(
            "Hello {{ project_name }}",
        )
        assert result == "Hello my-service"

    def test_render_string_multiple_variables(
        self, engine: TemplateEngine,
    ) -> None:
        result = engine.render_string(
            "{{ language_name }} {{ language_version }}",
        )
        assert result == "python 3.9"

    def test_render_string_with_context_override(
        self, engine: TemplateEngine,
    ) -> None:
        result = engine.render_string(
            "{{ project_name }}",
            {"project_name": "overridden"},
        )
        assert result == "overridden"

    def test_render_string_missing_variable_raises(
        self, engine: TemplateEngine,
    ) -> None:
        with pytest.raises(UndefinedError):
            engine.render_string("{{ unknown }}")

    def test_render_string_empty_string(
        self, engine: TemplateEngine,
    ) -> None:
        result = engine.render_string("")
        assert result == ""

    def test_render_string_no_variables(
        self, engine: TemplateEngine,
    ) -> None:
        result = engine.render_string(
            "plain text without variables",
        )
        assert result == "plain text without variables"

    def test_render_string_preserves_newlines(
        self, engine: TemplateEngine,
    ) -> None:
        result = engine.render_string("line1\nline2\n")
        assert result == "line1\nline2\n"


# --- replace_placeholders ---


class TestReplacePlaceholders:

    def test_replace_placeholders_single_placeholder(
        self, engine: TemplateEngine,
        full_config: ProjectConfig,
    ) -> None:
        result = engine.replace_placeholders(
            "{project_name}", full_config,
        )
        assert result == "my-service"

    def test_replace_placeholders_multiple_placeholders(
        self, engine: TemplateEngine,
        full_config: ProjectConfig,
    ) -> None:
        result = engine.replace_placeholders(
            "{project_name} uses {framework_name}",
            full_config,
        )
        assert result == "my-service uses click"

    def test_replace_placeholders_all_config_fields(
        self, engine: TemplateEngine,
        full_config: ProjectConfig,
    ) -> None:
        content = (
            "{project_name} {project_purpose} "
            "{language_name} {framework_name}"
        )
        result = engine.replace_placeholders(
            content, full_config,
        )
        assert "my-service" in result
        assert "A sample service" in result
        assert "python" in result
        assert "click" in result

    def test_replace_placeholders_unknown_unchanged(
        self, engine: TemplateEngine,
        full_config: ProjectConfig,
    ) -> None:
        result = engine.replace_placeholders(
            "{unknown_field}", full_config,
        )
        assert result == "{unknown_field}"

    def test_replace_placeholders_jinja2_syntax_untouched(
        self, engine: TemplateEngine,
        full_config: ProjectConfig,
    ) -> None:
        result = engine.replace_placeholders(
            "{{ project_name }}", full_config,
        )
        assert result == "{{ project_name }}"

    def test_replace_placeholders_empty_content(
        self, engine: TemplateEngine,
        full_config: ProjectConfig,
    ) -> None:
        result = engine.replace_placeholders(
            "", full_config,
        )
        assert result == ""

    def test_replace_placeholders_no_placeholders(
        self, engine: TemplateEngine,
        full_config: ProjectConfig,
    ) -> None:
        result = engine.replace_placeholders(
            "plain text", full_config,
        )
        assert result == "plain text"

    def test_replace_placeholders_idempotent(
        self, engine: TemplateEngine,
        full_config: ProjectConfig,
    ) -> None:
        content = "{project_name} and {framework_name}"
        first = engine.replace_placeholders(
            content, full_config,
        )
        second = engine.replace_placeholders(
            first, full_config,
        )
        assert first == second

    def test_replace_placeholders_json_in_content(
        self, engine: TemplateEngine,
        full_config: ProjectConfig,
    ) -> None:
        content = '{"key": "value"}'
        result = engine.replace_placeholders(
            content, full_config,
        )
        assert result == '{"key": "value"}'

    def test_replace_placeholders_adjacent(
        self, engine: TemplateEngine,
        full_config: ProjectConfig,
    ) -> None:
        result = engine.replace_placeholders(
            "{project_name}{framework_name}",
            full_config,
        )
        assert result == "my-serviceclick"

    def test_replace_placeholders_in_line(
        self, engine: TemplateEngine,
        full_config: ProjectConfig,
    ) -> None:
        content = "Name: {project_name}, Lang: {language_name}"
        result = engine.replace_placeholders(
            content, full_config,
        )
        assert result == "Name: my-service, Lang: python"

    def test_replace_placeholders_multiline(
        self, engine: TemplateEngine,
        full_config: ProjectConfig,
    ) -> None:
        content = "Line1: {project_name}\nLine2: {framework_name}"
        result = engine.replace_placeholders(
            content, full_config,
        )
        assert "my-service" in result
        assert "click" in result


# --- inject_section ---


class TestInjectSection:

    def test_inject_section_replaces_marker(self) -> None:
        result = TemplateEngine.inject_section(
            "before <!-- MARKER --> after",
            "INJECTED",
            "<!-- MARKER -->",
        )
        assert result == "before INJECTED after"

    def test_inject_section_marker_removed(self) -> None:
        result = TemplateEngine.inject_section(
            "a <!-- M --> b",
            "X",
            "<!-- M -->",
        )
        assert "<!-- M -->" not in result

    def test_inject_section_missing_marker(self) -> None:
        base = "content without marker"
        result = TemplateEngine.inject_section(
            base, "section", "<!-- MISSING -->",
        )
        assert result == base

    def test_inject_section_preserves_surrounding(self) -> None:
        result = TemplateEngine.inject_section(
            "HEAD\n<!-- M -->\nTAIL",
            "BODY",
            "<!-- M -->",
        )
        assert result == "HEAD\nBODY\nTAIL"

    def test_inject_section_empty_section(self) -> None:
        result = TemplateEngine.inject_section(
            "a<!-- M -->b", "", "<!-- M -->",
        )
        assert result == "ab"

    def test_inject_section_empty_base(self) -> None:
        result = TemplateEngine.inject_section(
            "", "section", "<!-- M -->",
        )
        assert result == ""

    def test_inject_section_multiple_markers(self) -> None:
        result = TemplateEngine.inject_section(
            "A <!-- M --> B <!-- M --> C",
            "X",
            "<!-- M -->",
        )
        assert result == "A X B X C"

    def test_inject_section_multiline_section(self) -> None:
        section = "line1\nline2\nline3"
        result = TemplateEngine.inject_section(
            "before\n<!-- M -->\nafter",
            section,
            "<!-- M -->",
        )
        assert "line1\nline2\nline3" in result

    def test_inject_section_preserves_whitespace(self) -> None:
        result = TemplateEngine.inject_section(
            "  <!-- M -->  ", "X", "<!-- M -->",
        )
        assert result == "  X  "

    def test_inject_section_marker_at_start(self) -> None:
        result = TemplateEngine.inject_section(
            "<!-- M -->rest", "START", "<!-- M -->",
        )
        assert result == "STARTrest"

    def test_inject_section_marker_at_end(self) -> None:
        result = TemplateEngine.inject_section(
            "start<!-- M -->", "END", "<!-- M -->",
        )
        assert result == "startEND"


# --- concat_files ---


class TestConcatFiles:

    def test_concat_files_two_files(self) -> None:
        file_a = FIXTURES_DIR / "concat_a.txt"
        file_b = FIXTURES_DIR / "concat_b.txt"
        result = TemplateEngine.concat_files(
            [file_a, file_b],
        )
        assert result.startswith("Line A1")
        assert "Line B1" in result

    def test_concat_files_custom_separator(self) -> None:
        file_a = FIXTURES_DIR / "concat_a.txt"
        file_b = FIXTURES_DIR / "concat_b.txt"
        result = TemplateEngine.concat_files(
            [file_a, file_b], separator="\n---\n",
        )
        assert "\n---\n" in result

    def test_concat_files_single_file(self) -> None:
        file_a = FIXTURES_DIR / "concat_a.txt"
        content = file_a.read_text()
        result = TemplateEngine.concat_files([file_a])
        assert result == content

    def test_concat_files_empty_list(self) -> None:
        result = TemplateEngine.concat_files([])
        assert result == ""

    def test_concat_files_preserves_order(self) -> None:
        file_a = FIXTURES_DIR / "concat_a.txt"
        file_b = FIXTURES_DIR / "concat_b.txt"
        result_ab = TemplateEngine.concat_files(
            [file_a, file_b],
        )
        result_ba = TemplateEngine.concat_files(
            [file_b, file_a],
        )
        assert result_ab != result_ba
        assert result_ab.index("Line A1") < result_ab.index("Line B1")

    def test_concat_files_nonexistent_raises(self) -> None:
        fake = Path("/tmp/nonexistent_file_xyz.txt")
        with pytest.raises(FileNotFoundError):
            TemplateEngine.concat_files([fake])

    def test_concat_files_empty_file(
        self, tmp_path: Path,
    ) -> None:
        empty = tmp_path / "empty.txt"
        empty.write_text("")
        full = FIXTURES_DIR / "concat_a.txt"
        result = TemplateEngine.concat_files(
            [empty, full],
        )
        assert "Line A1" in result

    def test_concat_files_preserves_trailing_newlines(
        self,
    ) -> None:
        file_a = FIXTURES_DIR / "concat_a.txt"
        result = TemplateEngine.concat_files([file_a])
        assert result.endswith("\n")

    def test_concat_files_three_files(
        self, tmp_path: Path,
    ) -> None:
        file_c = tmp_path / "c.txt"
        file_c.write_text("Line C1\n")
        file_a = FIXTURES_DIR / "concat_a.txt"
        file_b = FIXTURES_DIR / "concat_b.txt"
        result = TemplateEngine.concat_files(
            [file_a, file_b, file_c],
        )
        assert "Line A1" in result
        assert "Line B1" in result
        assert "Line C1" in result
