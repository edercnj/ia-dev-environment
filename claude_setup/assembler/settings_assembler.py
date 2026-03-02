from __future__ import annotations

import json
import logging
from pathlib import Path
from typing import Any, Dict, List

from claude_setup.domain.stack_mapping import (
    get_cache_settings_key,
    get_database_settings_key,
    get_hook_template_key,
    get_settings_lang_key,
)
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

logger = logging.getLogger(__name__)

NONE_VALUE = "none"


class SettingsAssembler:
    """Generates settings.json and settings.local.json."""

    def __init__(self, src_dir: Path) -> None:
        self._src_dir = src_dir
        self._templates_dir = src_dir / "settings-templates"

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Generate settings.json and settings.local.json."""
        permissions = self._collect_permissions(config)
        permissions = _deduplicate(permissions)
        has_hooks = bool(get_hook_template_key(
            config.language.name,
            config.framework.build_tool,
        ))
        settings = _build_settings_dict(permissions, has_hooks)
        generated: List[Path] = []
        generated.append(self._write_settings(output_dir, settings))
        generated.append(self._write_settings_local(output_dir))
        return generated

    def _collect_permissions(self, config: ProjectConfig) -> List[str]:
        """Collect permission arrays from all applicable sources."""
        result: List[str] = []
        result = self._merge_file(result, "base.json")
        lang_key = get_settings_lang_key(
            config.language.name,
            config.framework.build_tool,
        )
        if lang_key:
            result = self._merge_file(result, f"{lang_key}.json")
        result = self._collect_infra_permissions(config, result)
        result = self._collect_data_permissions(config, result)
        if config.testing.smoke_tests:
            result = self._merge_file(result, "testing-newman.json")
        return result

    def _collect_infra_permissions(
        self,
        config: ProjectConfig,
        result: List[str],
    ) -> List[str]:
        container = config.infrastructure.container
        if container in ("docker", "podman"):
            result = self._merge_file(result, "docker.json")
        orch = config.infrastructure.orchestrator
        if orch == "kubernetes":
            result = self._merge_file(result, "kubernetes.json")
        elif orch == "docker-compose":
            result = self._merge_file(result, "docker-compose.json")
        return result

    def _collect_data_permissions(
        self,
        config: ProjectConfig,
        result: List[str],
    ) -> List[str]:
        db_key = get_database_settings_key(config.data.database.name)
        if db_key:
            result = self._merge_file(result, f"{db_key}.json")
        cache_key = get_cache_settings_key(config.data.cache.name)
        if cache_key:
            result = self._merge_file(result, f"{cache_key}.json")
        return result

    def _merge_file(self, base: List[str], filename: str) -> List[str]:
        path = self._templates_dir / filename
        if not path.is_file():
            return base
        overlay = _read_json_array(path)
        return merge_json_arrays(base, overlay)

    @staticmethod
    def _write_settings(output_dir: Path, settings: Dict[str, Any]) -> Path:
        dest = output_dir / "settings.json"
        content = json.dumps(settings, indent=2) + "\n"
        dest.write_text(content, encoding="utf-8")
        logger.info("Generated %s", dest)
        return dest

    @staticmethod
    def _write_settings_local(output_dir: Path) -> Path:
        dest = output_dir / "settings.local.json"
        content = json.dumps({"permissions": {"allow": []}}, indent=2) + "\n"
        dest.write_text(content, encoding="utf-8")
        logger.info("Generated %s", dest)
        return dest


def merge_json_arrays(base: List[str], overlay: List[str]) -> List[str]:
    """Merge two JSON arrays, concatenating without deduplication."""
    return base + overlay


def _deduplicate(items: List[str]) -> List[str]:
    """Remove duplicates preserving insertion order."""
    seen: set = set()
    result: List[str] = []
    for item in items:
        if item not in seen:
            seen.add(item)
            result.append(item)
    return result


def _read_json_array(path: Path) -> List[str]:
    """Read a JSON file containing an array of strings."""
    text = path.read_text(encoding="utf-8")
    data = json.loads(text)
    if not isinstance(data, list):
        return []
    return data


def _build_settings_dict(
    permissions: List[str],
    has_hooks: bool,
) -> Dict[str, Any]:
    """Build the settings.json structure."""
    settings: Dict[str, Any] = {
        "permissions": {"allow": permissions},
    }
    if has_hooks:
        settings["hooks"] = _build_hooks_section()
    return settings


def _build_hooks_section() -> Dict[str, Any]:
    """Build the hooks section for compiled languages."""
    return {
        "PostToolUse": [
            {
                "matcher": "Write|Edit",
                "hooks": [
                    {
                        "type": "command",
                        "command": (
                            '"$CLAUDE_PROJECT_DIR"'
                            "/.claude/hooks/post-compile-check.sh"
                        ),
                        "timeout": 60,
                        "statusMessage": "Checking compilation...",
                    },
                ],
            },
        ],
    }
