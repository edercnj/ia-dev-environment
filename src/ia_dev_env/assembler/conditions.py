from __future__ import annotations

from typing import List

from ia_dev_env.models import ProjectConfig


def extract_interface_types(config: ProjectConfig) -> List[str]:
    """Return list of interface type strings from config."""
    return [iface.type for iface in config.interfaces]


def has_interface(config: ProjectConfig, iface_type: str) -> bool:
    """Check if a specific interface type exists in config."""
    return any(iface.type == iface_type for iface in config.interfaces)


def has_any_interface(config: ProjectConfig, *types: str) -> bool:
    """Check if any of the specified interface types exist."""
    type_set = set(types)
    return any(iface.type in type_set for iface in config.interfaces)
