from __future__ import annotations

from pathlib import Path
from typing import Dict, List

from claude_setup.models import ProjectConfig

INTERFACE_PROTOCOL_MAP: Dict[str, List[str]] = {
    "rest": ["rest"],
    "grpc": ["grpc"],
    "graphql": ["graphql"],
    "websocket": ["websocket"],
    "event-consumer": ["event-driven", "messaging"],
    "event-producer": ["event-driven", "messaging"],
    "cli": [],
}

EVENT_PREFIX = "event-"
EVENT_DRIVEN_PROTOCOL = "event-driven"


def derive_protocols(config: ProjectConfig) -> List[str]:
    """Map interface types to protocol directory names.

    Returns deduplicated, sorted list of protocol names.
    """
    protocols: List[str] = []
    for iface in config.interfaces:
        mapped = INTERFACE_PROTOCOL_MAP.get(iface.type)
        if mapped is not None:
            protocols.extend(mapped)
        elif iface.type.startswith(EVENT_PREFIX):
            protocols.append(EVENT_DRIVEN_PROTOCOL)
    return sorted(set(protocols))


def derive_protocol_files(
    resources_dir: Path,
    protocol_names: List[str],
    config: ProjectConfig,
) -> Dict[str, List[Path]]:
    """List .md files per protocol directory.

    Applies broker-specific filtering for messaging protocol.
    Skips missing directories without error.
    """
    protocols_root = resources_dir / "protocols"
    result: Dict[str, List[Path]] = {}
    for protocol in protocol_names:
        protocol_dir = protocols_root / protocol
        if not protocol_dir.is_dir():
            continue
        if protocol == "messaging":
            files = _select_messaging_files(
                protocol_dir, config,
            )
        else:
            files = sorted(protocol_dir.glob("*.md"))
        if files:
            result[protocol] = files
    return result


def _select_messaging_files(
    messaging_dir: Path,
    config: ProjectConfig,
) -> List[Path]:
    """Select broker-specific or all messaging files."""
    broker = _extract_broker(config)
    if broker:
        specific = messaging_dir / f"{broker}.md"
        if specific.is_file():
            return [specific]
    return sorted(messaging_dir.glob("*.md"))


def _extract_broker(config: ProjectConfig) -> str:
    """Extract broker name from first interface that has one."""
    for iface in config.interfaces:
        if iface.broker:
            return iface.broker
    return ""
