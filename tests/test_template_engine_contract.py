from __future__ import annotations

from pathlib import Path

import pytest

from ia_dev_env.models import ProjectConfig
from ia_dev_env.template_engine import TemplateEngine

FIXTURES_DIR = Path(__file__).parent / "fixtures"
TEMPLATES_DIR = FIXTURES_DIR / "templates"
REFERENCE_DIR = FIXTURES_DIR / "reference"


@pytest.fixture
def full_config(full_project_dict: dict) -> ProjectConfig:
    return ProjectConfig.from_dict(full_project_dict)


@pytest.fixture
def engine(full_config: ProjectConfig) -> TemplateEngine:
    return TemplateEngine(TEMPLATES_DIR, full_config)


@pytest.mark.parametrize(
    "template_name, reference_name",
    [
        ("simple.md.j2", "simple_rendered.md"),
        ("multivar.md.j2", "multivar_rendered.md"),
        ("whitespace.txt.j2", "whitespace_rendered.txt"),
    ],
)
def test_contract_render_template_byte_compatible(
    template_name: str,
    reference_name: str,
    engine: TemplateEngine,
) -> None:
    result = engine.render_template(Path(template_name))
    reference = (REFERENCE_DIR / reference_name).read_text()
    assert result == reference


def test_contract_replace_placeholders_legacy(
    engine: TemplateEngine,
    full_config: ProjectConfig,
) -> None:
    content = (FIXTURES_DIR / "legacy_placeholders.txt").read_text()
    result = engine.replace_placeholders(content, full_config)
    reference = (REFERENCE_DIR / "legacy_replaced.txt").read_text()
    assert result == reference


def test_contract_inject_section() -> None:
    base = (FIXTURES_DIR / "section_base.md").read_text()
    section = (FIXTURES_DIR / "section_inject.md").read_text()
    marker = "<!-- INSERT:rules -->"
    result = TemplateEngine.inject_section(base, section, marker)
    reference = (REFERENCE_DIR / "section_injected.md").read_text()
    assert result == reference


def test_contract_concat_files() -> None:
    paths = [
        FIXTURES_DIR / "concat_a.txt",
        FIXTURES_DIR / "concat_b.txt",
    ]
    result = TemplateEngine.concat_files(paths)
    reference = (REFERENCE_DIR / "concat_result.txt").read_text()
    assert result == reference
