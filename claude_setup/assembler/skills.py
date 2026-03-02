from __future__ import annotations

import shutil
from pathlib import Path
from typing import Dict, List, Optional

from claude_setup.assembler.conditions import (
    has_any_interface,
    has_interface,
)
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

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

STACK_PACK_MAP: Dict[str, str] = {
    "quarkus": "quarkus-patterns",
    "spring-boot": "spring-patterns",
    "nestjs": "nestjs-patterns",
    "express": "express-patterns",
    "fastapi": "fastapi-patterns",
    "django": "django-patterns",
    "gin": "gin-patterns",
    "ktor": "ktor-patterns",
    "axum": "axum-patterns",
    "dotnet": "dotnet-patterns",
    "click": "click-cli-patterns",
}

SKILLS_TEMPLATES_DIR = "skills-templates"
CORE_DIR = "core"
CONDITIONAL_DIR = "conditional"
KNOWLEDGE_PACKS_DIR = "knowledge-packs"
INFRA_PATTERNS_DIR = "infra-patterns"
STACK_PATTERNS_DIR = "stack-patterns"
LIB_DIR = "lib"
SKILL_MD = "SKILL.md"
SKILLS_OUTPUT = "skills"


class SkillsAssembler:
    """Assembles skills from templates based on project config."""

    def select_core_skills(
        self, src_dir: Path,
    ) -> List[str]:
        """Scan core skills directories, returning skill names."""
        core_path = src_dir / SKILLS_TEMPLATES_DIR / CORE_DIR
        if not core_path.exists():
            return []
        skills: List[str] = []
        for item in sorted(core_path.iterdir()):
            if not item.is_dir():
                continue
            if item.name == LIB_DIR:
                skills.extend(self._scan_lib_entries(item))
            else:
                skills.append(item.name)
        return skills

    def _scan_lib_entries(self, lib_path: Path) -> List[str]:
        """Scan lib/ subdirectories and return prefixed names."""
        entries: List[str] = []
        for item in sorted(lib_path.iterdir()):
            if item.is_dir():
                entries.append(f"{LIB_DIR}/{item.name}")
        return entries

    def select_conditional_skills(
        self, config: ProjectConfig,
    ) -> List[str]:
        """Evaluate feature gates and return conditional skills."""
        skills: List[str] = []
        skills.extend(self._select_interface_skills(config))
        skills.extend(self._select_infra_skills(config))
        skills.extend(self._select_testing_skills(config))
        skills.extend(self._select_security_skills(config))
        return skills

    def _select_interface_skills(
        self, config: ProjectConfig,
    ) -> List[str]:
        """Select skills based on interface types."""
        skills: List[str] = []
        if has_interface(config, "rest"):
            skills.append("x-review-api")
        if has_interface(config, "grpc"):
            skills.append("x-review-grpc")
        if has_interface(config, "graphql"):
            skills.append("x-review-graphql")
        if has_any_interface(config, "event-consumer", "event-producer"):
            skills.append("x-review-events")
        return skills

    def _select_infra_skills(
        self, config: ProjectConfig,
    ) -> List[str]:
        """Select skills based on infrastructure config."""
        skills: List[str] = []
        if config.infrastructure.observability.tool != "none":
            skills.append("instrument-otel")
        if config.infrastructure.orchestrator != "none":
            skills.append("setup-environment")
        if config.infrastructure.api_gateway != "none":
            skills.append("x-review-gateway")
        return skills

    def _select_testing_skills(
        self, config: ProjectConfig,
    ) -> List[str]:
        """Select skills based on testing config."""
        skills: List[str] = []
        if config.testing.smoke_tests and has_interface(config, "rest"):
            skills.append("run-smoke-api")
        if config.testing.smoke_tests and has_interface(config, "tcp-custom"):
            skills.append("run-smoke-socket")
        skills.append("run-e2e")
        if config.testing.performance_tests:
            skills.append("run-perf-test")
        if config.testing.contract_tests:
            skills.append("run-contract-tests")
        return skills

    def _select_security_skills(
        self, config: ProjectConfig,
    ) -> List[str]:
        """Select skills based on security config."""
        if len(config.security.frameworks) > 0:
            return ["x-review-security"]
        return []

    def select_knowledge_packs(
        self, config: ProjectConfig,
    ) -> List[str]:
        """Determine which knowledge packs to include."""
        packs: List[str] = list(CORE_KNOWLEDGE_PACKS)
        packs.append("layer-templates")
        packs.extend(self._select_data_packs(config))
        return packs

    def _select_data_packs(
        self, config: ProjectConfig,
    ) -> List[str]:
        """Select data-related knowledge packs."""
        has_db = config.data.database.name != "none"
        has_cache = config.data.cache.name != "none"
        if has_db or has_cache:
            return ["database-patterns"]
        return []

    def _copy_core_skill(
        self,
        skill_name: str,
        src_dir: Path,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> Path:
        """Copy a single core skill directory with placeholders."""
        src = src_dir / SKILLS_TEMPLATES_DIR / CORE_DIR / skill_name
        dest = output_dir / SKILLS_OUTPUT / skill_name
        shutil.copytree(src, dest, dirs_exist_ok=True)
        self._replace_placeholders_in_dir(dest, engine)
        return dest

    def _copy_conditional_skill(
        self,
        skill_name: str,
        src_dir: Path,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> Optional[Path]:
        """Copy a conditional skill if source exists."""
        src = src_dir / SKILLS_TEMPLATES_DIR / CONDITIONAL_DIR / skill_name
        if not src.exists():
            return None
        dest = output_dir / SKILLS_OUTPUT / skill_name
        shutil.copytree(src, dest, dirs_exist_ok=True)
        self._replace_placeholders_in_dir(dest, engine)
        return dest

    def _copy_knowledge_pack(
        self,
        pack_name: str,
        src_dir: Path,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> Optional[Path]:
        """Copy knowledge pack: overwrite SKILL.md, skip existing."""
        src = src_dir / SKILLS_TEMPLATES_DIR / KNOWLEDGE_PACKS_DIR / pack_name
        if not src.exists():
            return None
        dest = output_dir / SKILLS_OUTPUT / pack_name
        dest.mkdir(parents=True, exist_ok=True)
        self._copy_skill_md(src, dest, engine)
        self._copy_non_skill_items(src, dest)
        return dest

    def _copy_skill_md(
        self, src: Path, dest: Path, engine: TemplateEngine,
    ) -> None:
        """Always overwrite SKILL.md with placeholder replacement."""
        skill_md_src = src / SKILL_MD
        if not skill_md_src.exists():
            return
        content = skill_md_src.read_text(encoding="utf-8")
        replaced = engine.replace_placeholders(content)
        (dest / SKILL_MD).write_text(replaced, encoding="utf-8")

    def _copy_non_skill_items(
        self, src: Path, dest: Path,
    ) -> None:
        """Copy items other than SKILL.md, skipping existing."""
        for item in src.iterdir():
            if item.name == SKILL_MD:
                continue
            target = dest / item.name
            if target.exists():
                continue
            if item.is_dir():
                shutil.copytree(item, target)
            else:
                shutil.copy2(item, target)

    def _copy_stack_patterns(
        self,
        config: ProjectConfig,
        src_dir: Path,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> Optional[Path]:
        """Copy stack-specific patterns based on framework."""
        pack_name = STACK_PACK_MAP.get(config.framework.name)
        if pack_name is None:
            return None
        src = (
            src_dir / SKILLS_TEMPLATES_DIR
            / KNOWLEDGE_PACKS_DIR / STACK_PATTERNS_DIR / pack_name
        )
        if not src.exists():
            return None
        dest = output_dir / SKILLS_OUTPUT / pack_name
        shutil.copytree(src, dest, dirs_exist_ok=True)
        self._replace_placeholders_in_dir(dest, engine)
        return dest

    def _copy_infra_patterns(
        self,
        config: ProjectConfig,
        src_dir: Path,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Copy infrastructure knowledge packs based on config."""
        rules = self._build_infra_rules(config)
        results: List[Path] = []
        for pack_name, condition in rules:
            if not condition:
                continue
            path = self._copy_single_infra_pack(
                pack_name, src_dir, output_dir, engine,
            )
            if path is not None:
                results.append(path)
        return results

    def _build_infra_rules(
        self, config: ProjectConfig,
    ) -> List[tuple]:
        """Build list of (pack_name, condition) for infra packs."""
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

    def _copy_single_infra_pack(
        self,
        pack_name: str,
        src_dir: Path,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> Optional[Path]:
        """Copy a single infra pattern pack if source exists."""
        src = (
            src_dir / SKILLS_TEMPLATES_DIR
            / KNOWLEDGE_PACKS_DIR / INFRA_PATTERNS_DIR / pack_name
        )
        if not src.exists():
            return None
        dest = output_dir / SKILLS_OUTPUT / pack_name
        shutil.copytree(src, dest, dirs_exist_ok=True)
        self._replace_placeholders_in_dir(dest, engine)
        return dest

    def _replace_placeholders_in_dir(
        self, directory: Path, engine: TemplateEngine,
    ) -> None:
        """Replace placeholders in all .md files in directory."""
        for md_file in directory.rglob("*.md"):
            content = md_file.read_text(encoding="utf-8")
            replaced = engine.replace_placeholders(content)
            md_file.write_text(replaced, encoding="utf-8")

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        src_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Main entry point: assemble all skills."""
        results: List[Path] = []
        results.extend(self._assemble_core(src_dir, output_dir, engine))
        results.extend(
            self._assemble_conditional(config, src_dir, output_dir, engine),
        )
        results.extend(
            self._assemble_knowledge(config, src_dir, output_dir, engine),
        )
        return results

    def _assemble_core(
        self, src_dir: Path, output_dir: Path, engine: TemplateEngine,
    ) -> List[Path]:
        """Copy all core skills."""
        results: List[Path] = []
        for skill in self.select_core_skills(src_dir):
            results.append(
                self._copy_core_skill(skill, src_dir, output_dir, engine),
            )
        return results

    def _assemble_conditional(
        self,
        config: ProjectConfig,
        src_dir: Path,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Copy all conditional skills."""
        results: List[Path] = []
        for skill in self.select_conditional_skills(config):
            path = self._copy_conditional_skill(
                skill, src_dir, output_dir, engine,
            )
            if path is not None:
                results.append(path)
        return results

    def _assemble_knowledge(
        self,
        config: ProjectConfig,
        src_dir: Path,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Copy knowledge packs, stack patterns, and infra patterns."""
        results: List[Path] = []
        for pack in self.select_knowledge_packs(config):
            path = self._copy_knowledge_pack(
                pack, src_dir, output_dir, engine,
            )
            if path is not None:
                results.append(path)
        stack = self._copy_stack_patterns(
            config, src_dir, output_dir, engine,
        )
        if stack is not None:
            results.append(stack)
        results.extend(
            self._copy_infra_patterns(config, src_dir, output_dir, engine),
        )
        return results
