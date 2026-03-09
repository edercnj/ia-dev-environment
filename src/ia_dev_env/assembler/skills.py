from __future__ import annotations

import logging
import shutil
from pathlib import Path
from typing import List, Optional

from ia_dev_env.assembler.conditions import (
    has_any_interface,
    has_interface,
)
from ia_dev_env.assembler.copy_helpers import (
    copy_template_file,
    copy_template_tree,
    copy_template_tree_if_exists,
)
from ia_dev_env.domain.skill_registry import (
    CORE_KNOWLEDGE_PACKS,
    build_infra_pack_rules,
)
from ia_dev_env.domain.stack_pack_mapping import get_stack_pack_name
from ia_dev_env.models import ProjectConfig
from ia_dev_env.template_engine import TemplateEngine

logger = logging.getLogger(__name__)

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

    def select_core_skills(self, resources_dir: Path) -> List[str]:
        """Scan core skills directories, returning skill names."""
        core_path = resources_dir / SKILLS_TEMPLATES_DIR / CORE_DIR
        if not core_path.exists():
            return []
        skills: List[str] = []
        for item in sorted(core_path.iterdir()):
            if not item.is_dir():
                continue
            if item.name == LIB_DIR:
                skills.extend(
                    f"{LIB_DIR}/{sub.name}"
                    for sub in sorted(item.iterdir()) if sub.is_dir()
                )
            else:
                skills.append(item.name)
        return skills

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
        if config.data.database.name != "none" or config.data.cache.name != "none":
            return ["database-patterns"]
        return []

    def _copy_core_skill(
        self, skill_name: str, resources_dir: Path,
        output_dir: Path, engine: TemplateEngine,
    ) -> Path:
        """Copy a single core skill directory with placeholders."""
        src = resources_dir / SKILLS_TEMPLATES_DIR / CORE_DIR / skill_name
        dest = output_dir / SKILLS_OUTPUT / skill_name
        return copy_template_tree(src, dest, engine)

    def _copy_conditional_skill(
        self, skill_name: str, resources_dir: Path,
        output_dir: Path, engine: TemplateEngine,
    ) -> Optional[Path]:
        """Copy a conditional skill if source exists."""
        src = resources_dir / SKILLS_TEMPLATES_DIR / CONDITIONAL_DIR / skill_name
        dest = output_dir / SKILLS_OUTPUT / skill_name
        return copy_template_tree_if_exists(src, dest, engine)

    def _copy_knowledge_pack(
        self, pack_name: str, resources_dir: Path,
        output_dir: Path, engine: TemplateEngine,
    ) -> Optional[Path]:
        """Copy knowledge pack: overwrite SKILL.md, skip existing."""
        src = resources_dir / SKILLS_TEMPLATES_DIR / KNOWLEDGE_PACKS_DIR / pack_name
        if not src.exists():
            return None
        dest = output_dir / SKILLS_OUTPUT / pack_name
        dest.mkdir(parents=True, exist_ok=True)
        skill_md_src = src / SKILL_MD
        if skill_md_src.exists():
            copy_template_file(skill_md_src, dest / SKILL_MD, engine)
        self._copy_non_skill_items(src, dest)
        return dest

    def _copy_non_skill_items(self, src: Path, dest: Path) -> None:
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
        self, config: ProjectConfig, resources_dir: Path,
        output_dir: Path, engine: TemplateEngine,
    ) -> Optional[Path]:
        """Copy stack-specific patterns based on framework."""
        pack_name = get_stack_pack_name(config.framework.name)
        if not pack_name:
            return None
        src = (
            resources_dir / SKILLS_TEMPLATES_DIR
            / KNOWLEDGE_PACKS_DIR / STACK_PATTERNS_DIR / pack_name
        )
        dest = output_dir / SKILLS_OUTPUT / pack_name
        return copy_template_tree_if_exists(src, dest, engine)

    def _copy_infra_patterns(
        self, config: ProjectConfig, resources_dir: Path,
        output_dir: Path, engine: TemplateEngine,
    ) -> List[Path]:
        """Copy infrastructure knowledge packs based on config."""
        results: List[Path] = []
        for pack_name, condition in build_infra_pack_rules(config):
            if not condition:
                continue
            src = (
                resources_dir / SKILLS_TEMPLATES_DIR
                / KNOWLEDGE_PACKS_DIR / INFRA_PATTERNS_DIR / pack_name
            )
            dest = output_dir / SKILLS_OUTPUT / pack_name
            path = copy_template_tree_if_exists(src, dest, engine)
            if path is not None:
                results.append(path)
        return results

    def assemble(
        self, config: ProjectConfig, output_dir: Path,
        resources_dir: Path, engine: TemplateEngine,
    ) -> List[Path]:
        """Main entry point: assemble all skills."""
        logger.info("Assembling skills for project '%s'", config.project.name)
        results: List[Path] = []
        core = self._assemble_core(resources_dir, output_dir, engine)
        results.extend(core)
        logger.debug("Assembled %d core skills", len(core))
        conditional = self._assemble_conditional(
            config, resources_dir, output_dir, engine,
        )
        results.extend(conditional)
        logger.debug("Assembled %d conditional skills", len(conditional))
        knowledge = self._assemble_knowledge(
            config, resources_dir, output_dir, engine,
        )
        results.extend(knowledge)
        logger.info("Skills assembly complete: %d total artifacts", len(results))
        return results

    def _assemble_core(
        self, resources_dir: Path, output_dir: Path, engine: TemplateEngine,
    ) -> List[Path]:
        """Copy all core skills."""
        return [
            self._copy_core_skill(skill, resources_dir, output_dir, engine)
            for skill in self.select_core_skills(resources_dir)
        ]

    def _assemble_conditional(
        self, config: ProjectConfig, resources_dir: Path,
        output_dir: Path, engine: TemplateEngine,
    ) -> List[Path]:
        """Copy all conditional skills."""
        results: List[Path] = []
        for skill in self.select_conditional_skills(config):
            path = self._copy_conditional_skill(
                skill, resources_dir, output_dir, engine,
            )
            if path is not None:
                results.append(path)
        return results

    def _assemble_knowledge(
        self, config: ProjectConfig, resources_dir: Path,
        output_dir: Path, engine: TemplateEngine,
    ) -> List[Path]:
        """Copy knowledge packs, stack patterns, and infra patterns."""
        results: List[Path] = []
        for pack in self.select_knowledge_packs(config):
            path = self._copy_knowledge_pack(
                pack, resources_dir, output_dir, engine,
            )
            if path is not None:
                results.append(path)
        stack = self._copy_stack_patterns(
            config, resources_dir, output_dir, engine,
        )
        if stack is not None:
            results.append(stack)
        results.extend(
            self._copy_infra_patterns(config, resources_dir, output_dir, engine),
        )
        return results
