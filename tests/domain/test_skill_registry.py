from __future__ import annotations

import pytest

from ia_dev_env.domain.skill_registry import (
    CORE_KNOWLEDGE_PACKS,
    build_infra_pack_rules,
)


class TestCoreKnowledgePacks:

    def test_has_eleven_entries(self) -> None:
        assert len(CORE_KNOWLEDGE_PACKS) == 11

    def test_contains_coding_standards(self) -> None:
        assert "coding-standards" in CORE_KNOWLEDGE_PACKS

    def test_contains_architecture(self) -> None:
        assert "architecture" in CORE_KNOWLEDGE_PACKS


class TestBuildInfraPackRules:

    def test_kubernetes_rule(self, create_project_config) -> None:
        config = create_project_config(
            infrastructure={"orchestrator": "kubernetes"},
        )
        rules = build_infra_pack_rules(config)
        active = [name for name, cond in rules if cond]
        assert "k8s-deployment" in active

    def test_kustomize_rule(self, create_project_config) -> None:
        config = create_project_config(
            infrastructure={"templating": "kustomize"},
        )
        rules = build_infra_pack_rules(config)
        active = [name for name, cond in rules if cond]
        assert "k8s-kustomize" in active

    def test_helm_rule(self, create_project_config) -> None:
        config = create_project_config(
            infrastructure={"templating": "helm"},
        )
        rules = build_infra_pack_rules(config)
        active = [name for name, cond in rules if cond]
        assert "k8s-helm" in active

    def test_dockerfile_rule(self, create_project_config) -> None:
        config = create_project_config(
            infrastructure={"container": "docker"},
        )
        rules = build_infra_pack_rules(config)
        active = [name for name, cond in rules if cond]
        assert "dockerfile" in active

    def test_terraform_rule(self, create_project_config) -> None:
        config = create_project_config(
            infrastructure={"iac": "terraform"},
        )
        rules = build_infra_pack_rules(config)
        active = [name for name, cond in rules if cond]
        assert "iac-terraform" in active

    def test_minimal_config_no_k8s(self, create_project_config) -> None:
        config = create_project_config()
        rules = build_infra_pack_rules(config)
        active = [name for name, cond in rules if cond]
        assert "k8s-deployment" not in active

    def test_returns_seven_rules(self, create_project_config) -> None:
        config = create_project_config()
        rules = build_infra_pack_rules(config)
        assert len(rules) == 7
