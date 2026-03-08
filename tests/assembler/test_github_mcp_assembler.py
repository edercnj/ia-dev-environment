from __future__ import annotations

import json
import logging
from pathlib import Path
from unittest.mock import MagicMock

import pytest

from claude_setup.assembler.github_mcp_assembler import (
    GithubMcpAssembler,
    _build_copilot_mcp_dict,
    _warn_literal_env_values,
)
from claude_setup.models import (
    McpConfig,
    McpServerConfig,
    ProjectConfig,
)


def _make_config(
    servers: list | None = None,
) -> ProjectConfig:
    """Build a minimal ProjectConfig with optional MCP servers."""
    data = {
        "project": {"name": "test-proj", "purpose": "Testing"},
        "architecture": {"style": "library"},
        "interfaces": [{"type": "cli"}],
        "language": {"name": "python", "version": "3.9"},
        "framework": {"name": "click", "version": "8.1"},
    }
    if servers is not None:
        data["mcp"] = {"servers": servers}
    return ProjectConfig.from_dict(data)


class TestGithubMcpAssemblerAssemble:

    def test_no_servers_returns_empty_list(
        self, tmp_path: Path,
    ) -> None:
        assembler = GithubMcpAssembler()
        config = _make_config(servers=[])
        result = assembler.assemble(
            config, tmp_path, MagicMock(),
        )
        assert result == []

    def test_no_file_generated_when_servers_empty(
        self, tmp_path: Path,
    ) -> None:
        assembler = GithubMcpAssembler()
        config = _make_config(servers=[])
        assembler.assemble(config, tmp_path, MagicMock())
        github_dir = tmp_path / "github"
        assert not github_dir.exists()

    def test_generates_json_file_with_servers(
        self, tmp_path: Path,
    ) -> None:
        assembler = GithubMcpAssembler()
        config = _make_config(servers=[
            {"id": "srv1", "url": "https://example.com"},
        ])
        result = assembler.assemble(
            config, tmp_path, MagicMock(),
        )
        assert len(result) == 1
        assert result[0].name == "copilot-mcp.json"

    def test_output_path_is_github_copilot_mcp(
        self, tmp_path: Path,
    ) -> None:
        assembler = GithubMcpAssembler()
        config = _make_config(servers=[
            {"id": "srv1", "url": "https://example.com"},
        ])
        result = assembler.assemble(
            config, tmp_path, MagicMock(),
        )
        expected = tmp_path / "github" / "copilot-mcp.json"
        assert result[0] == expected

    def test_json_is_valid_and_parseable(
        self, tmp_path: Path,
    ) -> None:
        assembler = GithubMcpAssembler()
        config = _make_config(servers=[
            {"id": "srv1", "url": "https://example.com"},
        ])
        assembler.assemble(config, tmp_path, MagicMock())
        dest = tmp_path / "github" / "copilot-mcp.json"
        content = dest.read_text(encoding="utf-8")
        parsed = json.loads(content)
        assert "mcpServers" in parsed

    def test_multiple_servers_all_present(
        self, tmp_path: Path,
    ) -> None:
        assembler = GithubMcpAssembler()
        config = _make_config(servers=[
            {"id": "srv1", "url": "https://a.com"},
            {"id": "srv2", "url": "https://b.com"},
            {"id": "srv3", "url": "https://c.com"},
        ])
        assembler.assemble(config, tmp_path, MagicMock())
        dest = tmp_path / "github" / "copilot-mcp.json"
        parsed = json.loads(dest.read_text(encoding="utf-8"))
        assert len(parsed["mcpServers"]) == 3
        assert "srv1" in parsed["mcpServers"]
        assert "srv2" in parsed["mcpServers"]
        assert "srv3" in parsed["mcpServers"]

    def test_env_references_preserved(
        self, tmp_path: Path,
    ) -> None:
        assembler = GithubMcpAssembler()
        config = _make_config(servers=[
            {
                "id": "srv1",
                "url": "https://example.com",
                "env": {"API_KEY": "$MCP_API_KEY"},
            },
        ])
        assembler.assemble(config, tmp_path, MagicMock())
        dest = tmp_path / "github" / "copilot-mcp.json"
        parsed = json.loads(dest.read_text(encoding="utf-8"))
        srv = parsed["mcpServers"]["srv1"]
        assert srv["env"]["API_KEY"] == "$MCP_API_KEY"

    def test_capabilities_included_in_output(
        self, tmp_path: Path,
    ) -> None:
        assembler = GithubMcpAssembler()
        config = _make_config(servers=[
            {
                "id": "srv1",
                "url": "https://example.com",
                "capabilities": ["scrape", "crawl"],
            },
        ])
        assembler.assemble(config, tmp_path, MagicMock())
        dest = tmp_path / "github" / "copilot-mcp.json"
        parsed = json.loads(dest.read_text(encoding="utf-8"))
        srv = parsed["mcpServers"]["srv1"]
        assert srv["capabilities"] == ["scrape", "crawl"]

    def test_no_capabilities_omits_key(
        self, tmp_path: Path,
    ) -> None:
        assembler = GithubMcpAssembler()
        config = _make_config(servers=[
            {"id": "srv1", "url": "https://example.com"},
        ])
        assembler.assemble(config, tmp_path, MagicMock())
        dest = tmp_path / "github" / "copilot-mcp.json"
        parsed = json.loads(dest.read_text(encoding="utf-8"))
        assert "capabilities" not in parsed["mcpServers"]["srv1"]


class TestMcpServerConfigFromDict:

    def test_valid_data(self) -> None:
        data = {"id": "s1", "url": "https://x.com"}
        result = McpServerConfig.from_dict(data)
        assert result.id == "s1"
        assert result.url == "https://x.com"
        assert result.env == {}
        assert result.capabilities == []

    def test_with_env(self) -> None:
        data = {
            "id": "s1",
            "url": "https://x.com",
            "env": {"K": "V"},
        }
        result = McpServerConfig.from_dict(data)
        assert result.env == {"K": "V"}

    def test_with_capabilities(self) -> None:
        data = {
            "id": "s1",
            "url": "https://x.com",
            "capabilities": ["read", "write"],
        }
        result = McpServerConfig.from_dict(data)
        assert result.capabilities == ["read", "write"]

    def test_missing_id_raises(self) -> None:
        with pytest.raises(KeyError, match="id"):
            McpServerConfig.from_dict({"url": "https://x.com"})

    def test_missing_url_raises(self) -> None:
        with pytest.raises(KeyError, match="url"):
            McpServerConfig.from_dict({"id": "s1"})


class TestMcpConfigFromDict:

    def test_empty_dict(self) -> None:
        result = McpConfig.from_dict({})
        assert result.servers == []

    def test_with_servers(self) -> None:
        data = {
            "servers": [
                {"id": "a", "url": "https://a.com"},
                {"id": "b", "url": "https://b.com"},
            ],
        }
        result = McpConfig.from_dict(data)
        assert len(result.servers) == 2
        assert result.servers[0].id == "a"
        assert result.servers[1].id == "b"


class TestProjectConfigBackwardCompat:

    def test_no_mcp_section_defaults(self) -> None:
        config = _make_config()
        assert config.mcp.servers == []


class TestBuildCopilotMcpDict:

    def test_structure_single_server(self) -> None:
        config = _make_config(servers=[
            {"id": "srv1", "url": "https://example.com"},
        ])
        result = _build_copilot_mcp_dict(config)
        assert result == {
            "mcpServers": {
                "srv1": {"url": "https://example.com"},
            },
        }

    def test_structure_with_env(self) -> None:
        config = _make_config(servers=[
            {
                "id": "srv1",
                "url": "https://example.com",
                "env": {"KEY": "$VAL"},
            },
        ])
        result = _build_copilot_mcp_dict(config)
        assert result == {
            "mcpServers": {
                "srv1": {
                    "url": "https://example.com",
                    "env": {"KEY": "$VAL"},
                },
            },
        }

    def test_structure_with_capabilities(self) -> None:
        config = _make_config(servers=[
            {
                "id": "srv1",
                "url": "https://example.com",
                "capabilities": ["search"],
            },
        ])
        result = _build_copilot_mcp_dict(config)
        srv = result["mcpServers"]["srv1"]
        assert srv["capabilities"] == ["search"]

    def test_no_env_omits_env_key(self) -> None:
        config = _make_config(servers=[
            {"id": "srv1", "url": "https://example.com"},
        ])
        result = _build_copilot_mcp_dict(config)
        assert "env" not in result["mcpServers"]["srv1"]

    def test_no_capabilities_omits_key(self) -> None:
        config = _make_config(servers=[
            {"id": "srv1", "url": "https://example.com"},
        ])
        result = _build_copilot_mcp_dict(config)
        assert "capabilities" not in result["mcpServers"]["srv1"]


class TestWarnLiteralEnvValues:

    def test_warns_on_literal_value(self, caplog) -> None:
        server = McpServerConfig(
            id="srv1",
            url="https://example.com",
            env={"API_KEY": "sk-live-secret123"},
        )
        with caplog.at_level(logging.WARNING):
            _warn_literal_env_values([server])
        assert "literal value" in caplog.text
        assert "srv1" in caplog.text

    def test_no_warning_for_variable_reference(self, caplog) -> None:
        server = McpServerConfig(
            id="srv1",
            url="https://example.com",
            env={"API_KEY": "$MCP_API_KEY"},
        )
        with caplog.at_level(logging.WARNING):
            _warn_literal_env_values([server])
        assert caplog.text == ""

    def test_no_warning_for_empty_env(self, caplog) -> None:
        server = McpServerConfig(
            id="srv1",
            url="https://example.com",
        )
        with caplog.at_level(logging.WARNING):
            _warn_literal_env_values([server])
        assert caplog.text == ""

    def test_no_warning_for_empty_value(self, caplog) -> None:
        server = McpServerConfig(
            id="srv1",
            url="https://example.com",
            env={"API_KEY": ""},
        )
        with caplog.at_level(logging.WARNING):
            _warn_literal_env_values([server])
        assert caplog.text == ""
