from __future__ import annotations

import json
import logging
from pathlib import Path
from typing import Any, Dict, List

from claude_setup.models import McpServerConfig, ProjectConfig
from claude_setup.template_engine import TemplateEngine

logger = logging.getLogger(__name__)


class GithubMcpAssembler:
    """Generates github/copilot-mcp.json under the output directory."""

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Generate copilot-mcp.json if MCP servers are configured."""
        if not config.mcp.servers:
            return []
        _warn_literal_env_values(config.mcp.servers)
        github_dir = output_dir / "github"
        github_dir.mkdir(parents=True, exist_ok=True)
        dest = github_dir / "copilot-mcp.json"
        mcp_dict = _build_copilot_mcp_dict(config)
        content = json.dumps(mcp_dict, indent=2) + "\n"
        dest.write_text(content, encoding="utf-8")
        return [dest]


def _warn_literal_env_values(
    servers: List[McpServerConfig],
) -> None:
    """Warn if env values don't use $ variable references."""
    for server in servers:
        for key, value in server.env.items():
            if value and not value.startswith("$"):
                logger.warning(
                    "MCP server '%s' env '%s' appears to contain "
                    "a literal value instead of a $VARIABLE reference. "
                    "Secrets should use environment variable references.",
                    server.id,
                    key,
                )


def _build_copilot_mcp_dict(
    config: ProjectConfig,
) -> Dict[str, Any]:
    """Build the copilot-mcp.json structure."""
    servers: Dict[str, Any] = {}
    for server in config.mcp.servers:
        entry: Dict[str, Any] = {"url": server.url}
        if server.capabilities:
            entry["capabilities"] = list(server.capabilities)
        if server.env:
            entry["env"] = dict(server.env)
        servers[server.id] = entry
    return {"mcpServers": servers}
