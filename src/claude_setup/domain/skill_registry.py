from __future__ import annotations

from typing import List, Tuple

from claude_setup.models import ProjectConfig

CORE_KNOWLEDGE_PACKS: List[str] = [
    "coding-standards",
    "architecture",
    "testing",
    "security",
    "compliance",
    "api-design",
    "observability",
    "resilience",
    "infrastructure",
    "protocols",
    "story-planning",
]


def build_infra_pack_rules(
    config: ProjectConfig,
) -> List[Tuple[str, bool]]:
    """Return (pack_name, condition) tuples for infra packs."""
    infra = config.infrastructure
    return [
        ("k8s-deployment", infra.orchestrator == "kubernetes"),
        ("k8s-kustomize", infra.templating == "kustomize"),
        ("k8s-helm", infra.templating == "helm"),
        ("dockerfile", infra.container != "none"),
        ("container-registry", infra.registry != "none"),
        ("iac-terraform", infra.iac == "terraform"),
        ("iac-crossplane", infra.iac == "crossplane"),
    ]
