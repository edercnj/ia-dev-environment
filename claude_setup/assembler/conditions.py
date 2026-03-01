from __future__ import annotations

from typing import List

from claude_setup.models import ProjectConfig


def extract_interface_types(config: ProjectConfig) -> List[str]:
    """Return list of interface type strings from config."""
    return [iface.type for iface in config.interfaces]


def has_interface(config: ProjectConfig, iface_type: str) -> bool:
    """Check if a specific interface type exists in config."""
    return iface_type in extract_interface_types(config)


def has_any_interface(config: ProjectConfig, *types: str) -> bool:
    """Check if any of the specified interface types exist."""
    iface_types = extract_interface_types(config)
    return any(t in iface_types for t in types)
