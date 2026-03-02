from __future__ import annotations

from pathlib import Path

import pytest

from claude_setup.assembler.agents import AgentsAssembler
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine


@pytest.fixture
def assembler():
    return AgentsAssembler()


def _make_engine(resources_dir: Path, config: ProjectConfig) -> TemplateEngine:
    return TemplateEngine(resources_dir, config)


def _create_core_agent(resources_dir: Path, name: str, content: str) -> Path:
    """Helper to create a core agent template."""
    path = resources_dir / "agents-templates" / "core"
    path.mkdir(parents=True, exist_ok=True)
    agent = path / name
    agent.write_text(content, encoding="utf-8")
    return agent


def _create_conditional_agent(
    resources_dir: Path, name: str, content: str,
) -> Path:
    """Helper to create a conditional agent template."""
    path = resources_dir / "agents-templates" / "conditional"
    path.mkdir(parents=True, exist_ok=True)
    agent = path / name
    agent.write_text(content, encoding="utf-8")
    return agent


def _create_developer_agent(
    resources_dir: Path, name: str, content: str,
) -> Path:
    """Helper to create a developer agent template."""
    path = resources_dir / "agents-templates" / "developers"
    path.mkdir(parents=True, exist_ok=True)
    agent = path / name
    agent.write_text(content, encoding="utf-8")
    return agent


def _create_checklist(
    resources_dir: Path, name: str, content: str,
) -> Path:
    """Helper to create a checklist template."""
    path = resources_dir / "agents-templates" / "checklists"
    path.mkdir(parents=True, exist_ok=True)
    checklist = path / name
    checklist.write_text(content, encoding="utf-8")
    return checklist


class TestCopyCoreAgent:

    def test_creates_file(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_core_agent(src, "review.md", "# Review")
        engine = _make_engine(src, minimal_cli_config)
        result = assembler._copy_core_agent(
            "review.md", src, output, engine,
        )
        assert result.exists()
        assert (output / "agents" / "review.md").exists()

    def test_replaces_placeholders(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_core_agent(
            src, "review.md", "Project: {project_name}",
        )
        engine = _make_engine(src, minimal_cli_config)
        assembler._copy_core_agent("review.md", src, output, engine)
        content = (output / "agents" / "review.md").read_text(
            encoding="utf-8",
        )
        assert "minimal-cli" in content
        assert "{project_name}" not in content


class TestCopyConditionalAgent:

    def test_exists_returns_path(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_conditional_agent(
            src, "database-engineer.md", "# DB Engineer",
        )
        engine = _make_engine(src, minimal_cli_config)
        result = assembler._copy_conditional_agent(
            "database-engineer.md", src, output, engine,
        )
        assert result is not None
        assert result.exists()

    def test_missing_returns_none(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        engine = _make_engine(src, minimal_cli_config)
        result = assembler._copy_conditional_agent(
            "nonexistent.md", src, output, engine,
        )
        assert result is None


class TestCopyDeveloperAgent:

    def test_exists_returns_path(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_developer_agent(
            src, "python-developer.md",
            "# Python Dev {language_version}",
        )
        engine = _make_engine(src, minimal_cli_config)
        result = assembler._copy_developer_agent(
            minimal_cli_config, src, output, engine,
        )
        assert result is not None
        content = result.read_text(encoding="utf-8")
        assert "3.9" in content

    def test_missing_returns_none(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            language={"name": "cobol", "version": "85"},
        )
        engine = _make_engine(src, config)
        result = assembler._copy_developer_agent(
            config, src, output, engine,
        )
        assert result is None


class TestInjectChecklists:

    def test_pci_dss_injected(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            security={"frameworks": ["pci-dss"]},
        )
        agents_dir = output / "agents"
        agents_dir.mkdir(parents=True)
        (agents_dir / "security-engineer.md").write_text(
            "# Security\n<!-- PCI_DSS_SECURITY -->\n",
            encoding="utf-8",
        )
        _create_checklist(src, "pci-dss-security.md", "PCI-DSS content")
        engine = _make_engine(src, config)
        assembler._inject_checklists(config, output, src, engine)
        content = (agents_dir / "security-engineer.md").read_text(
            encoding="utf-8",
        )
        assert "PCI-DSS content" in content

    def test_privacy_lgpd_injected(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            security={"frameworks": ["lgpd"]},
        )
        agents_dir = output / "agents"
        agents_dir.mkdir(parents=True)
        (agents_dir / "security-engineer.md").write_text(
            "# Security\n<!-- PRIVACY_SECURITY -->\n",
            encoding="utf-8",
        )
        _create_checklist(src, "privacy-security.md", "Privacy content")
        engine = _make_engine(src, config)
        assembler._inject_checklists(config, output, src, engine)
        content = (agents_dir / "security-engineer.md").read_text(
            encoding="utf-8",
        )
        assert "Privacy content" in content

    def test_privacy_gdpr_injected(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            security={"frameworks": ["gdpr"]},
        )
        agents_dir = output / "agents"
        agents_dir.mkdir(parents=True)
        (agents_dir / "security-engineer.md").write_text(
            "# Security\n<!-- PRIVACY_SECURITY -->\n",
            encoding="utf-8",
        )
        _create_checklist(src, "privacy-security.md", "Privacy content")
        engine = _make_engine(src, config)
        assembler._inject_checklists(config, output, src, engine)
        content = (agents_dir / "security-engineer.md").read_text(
            encoding="utf-8",
        )
        assert "Privacy content" in content

    def test_hipaa_injected(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            security={"frameworks": ["hipaa"]},
        )
        agents_dir = output / "agents"
        agents_dir.mkdir(parents=True)
        (agents_dir / "security-engineer.md").write_text(
            "# Security\n<!-- HIPAA_SECURITY -->\n",
            encoding="utf-8",
        )
        _create_checklist(src, "hipaa-security.md", "HIPAA content")
        engine = _make_engine(src, config)
        assembler._inject_checklists(config, output, src, engine)
        content = (agents_dir / "security-engineer.md").read_text(
            encoding="utf-8",
        )
        assert "HIPAA content" in content

    def test_sox_injected(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            security={"frameworks": ["sox"]},
        )
        agents_dir = output / "agents"
        agents_dir.mkdir(parents=True)
        (agents_dir / "security-engineer.md").write_text(
            "# Security\n<!-- SOX_SECURITY -->\n",
            encoding="utf-8",
        )
        _create_checklist(src, "sox-security.md", "SOX content")
        engine = _make_engine(src, config)
        assembler._inject_checklists(config, output, src, engine)
        content = (agents_dir / "security-engineer.md").read_text(
            encoding="utf-8",
        )
        assert "SOX content" in content

    def test_grpc_checklist_injected(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            interfaces=[{"type": "grpc"}],
        )
        agents_dir = output / "agents"
        agents_dir.mkdir(parents=True)
        (agents_dir / "api-engineer.md").write_text(
            "# API\n<!-- GRPC_API -->\n",
            encoding="utf-8",
        )
        _create_checklist(src, "grpc-api.md", "gRPC content")
        engine = _make_engine(src, config)
        assembler._inject_checklists(config, output, src, engine)
        content = (agents_dir / "api-engineer.md").read_text(
            encoding="utf-8",
        )
        assert "gRPC content" in content

    def test_graphql_checklist_injected(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            interfaces=[{"type": "graphql"}],
        )
        agents_dir = output / "agents"
        agents_dir.mkdir(parents=True)
        (agents_dir / "api-engineer.md").write_text(
            "# API\n<!-- GRAPHQL_API -->\n",
            encoding="utf-8",
        )
        _create_checklist(src, "graphql-api.md", "GraphQL content")
        engine = _make_engine(src, config)
        assembler._inject_checklists(config, output, src, engine)
        content = (agents_dir / "api-engineer.md").read_text(
            encoding="utf-8",
        )
        assert "GraphQL content" in content

    def test_websocket_checklist_injected(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            interfaces=[{"type": "websocket"}],
        )
        agents_dir = output / "agents"
        agents_dir.mkdir(parents=True)
        (agents_dir / "api-engineer.md").write_text(
            "# API\n<!-- WEBSOCKET_API -->\n",
            encoding="utf-8",
        )
        _create_checklist(src, "websocket-api.md", "WS content")
        engine = _make_engine(src, config)
        assembler._inject_checklists(config, output, src, engine)
        content = (agents_dir / "api-engineer.md").read_text(
            encoding="utf-8",
        )
        assert "WS content" in content

    def test_helm_checklist_injected(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            infrastructure={"templating": "helm"},
        )
        agents_dir = output / "agents"
        agents_dir.mkdir(parents=True)
        (agents_dir / "devops-engineer.md").write_text(
            "# DevOps\n<!-- HELM_DEVOPS -->\n",
            encoding="utf-8",
        )
        _create_checklist(src, "helm-devops.md", "Helm content")
        engine = _make_engine(src, config)
        assembler._inject_checklists(config, output, src, engine)
        content = (agents_dir / "devops-engineer.md").read_text(
            encoding="utf-8",
        )
        assert "Helm content" in content

    def test_iac_checklist_injected(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            infrastructure={"iac": "terraform"},
        )
        agents_dir = output / "agents"
        agents_dir.mkdir(parents=True)
        (agents_dir / "devops-engineer.md").write_text(
            "# DevOps\n<!-- IAC_DEVOPS -->\n",
            encoding="utf-8",
        )
        _create_checklist(src, "iac-devops.md", "IaC content")
        engine = _make_engine(src, config)
        assembler._inject_checklists(config, output, src, engine)
        content = (agents_dir / "devops-engineer.md").read_text(
            encoding="utf-8",
        )
        assert "IaC content" in content

    def test_mesh_checklist_injected(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            infrastructure={"service_mesh": "istio"},
        )
        agents_dir = output / "agents"
        agents_dir.mkdir(parents=True)
        (agents_dir / "devops-engineer.md").write_text(
            "# DevOps\n<!-- MESH_DEVOPS -->\n",
            encoding="utf-8",
        )
        _create_checklist(src, "mesh-devops.md", "Mesh content")
        engine = _make_engine(src, config)
        assembler._inject_checklists(config, output, src, engine)
        content = (agents_dir / "devops-engineer.md").read_text(
            encoding="utf-8",
        )
        assert "Mesh content" in content

    def test_registry_checklist_injected(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            infrastructure={"registry": "ecr"},
        )
        agents_dir = output / "agents"
        agents_dir.mkdir(parents=True)
        (agents_dir / "devops-engineer.md").write_text(
            "# DevOps\n<!-- REGISTRY_DEVOPS -->\n",
            encoding="utf-8",
        )
        _create_checklist(src, "registry-devops.md", "Registry content")
        engine = _make_engine(src, config)
        assembler._inject_checklists(config, output, src, engine)
        content = (agents_dir / "devops-engineer.md").read_text(
            encoding="utf-8",
        )
        assert "Registry content" in content

    def test_no_target_agent_no_error(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            interfaces=[{"type": "grpc"}],
        )
        (output / "agents").mkdir(parents=True)
        _create_checklist(src, "grpc-api.md", "gRPC content")
        engine = _make_engine(src, config)
        assembler._inject_checklists(config, output, src, engine)

    def test_no_frameworks_no_injection(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        agents_dir = output / "agents"
        agents_dir.mkdir(parents=True)
        marker_content = "# Security\n<!-- PCI_DSS_SECURITY -->\n"
        (agents_dir / "security-engineer.md").write_text(
            marker_content, encoding="utf-8",
        )
        engine = _make_engine(src, minimal_cli_config)
        assembler._inject_checklists(
            minimal_cli_config, output, src, engine,
        )
        content = (agents_dir / "security-engineer.md").read_text(
            encoding="utf-8",
        )
        assert content == marker_content


class TestAssemble:

    def test_creates_agents_directory(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_core_agent(src, "review.md", "# Review")
        engine = _make_engine(src, minimal_cli_config)
        assembler.assemble(minimal_cli_config, output, src, engine)
        assert (output / "agents").exists()

    def test_returns_list(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_core_agent(src, "review.md", "# Review")
        engine = _make_engine(src, minimal_cli_config)
        result = assembler.assemble(
            minimal_cli_config, output, src, engine,
        )
        assert isinstance(result, list)
        assert len(result) >= 1

    def test_copies_conditional_when_met(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            data={"database": {"name": "postgresql", "version": "15"}},
        )
        _create_conditional_agent(
            src, "database-engineer.md", "# DB {project_name}",
        )
        engine = _make_engine(src, config)
        assembler.assemble(config, output, src, engine)
        agent = output / "agents" / "database-engineer.md"
        assert agent.exists()
        content = agent.read_text(encoding="utf-8")
        assert "minimal-cli" in content

    def test_skips_conditional_when_not_met(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_conditional_agent(
            src, "database-engineer.md", "# DB",
        )
        engine = _make_engine(src, minimal_cli_config)
        assembler.assemble(minimal_cli_config, output, src, engine)
        assert not (output / "agents" / "database-engineer.md").exists()

    def test_copies_developer_agent(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_developer_agent(
            src, "python-developer.md", "# Python {language_version}",
        )
        engine = _make_engine(src, minimal_cli_config)
        assembler.assemble(minimal_cli_config, output, src, engine)
        agent = output / "agents" / "python-developer.md"
        assert agent.exists()
        content = agent.read_text(encoding="utf-8")
        assert "3.9" in content
