from __future__ import annotations

import logging
import shutil
from pathlib import Path
from typing import List

from claude_setup.assembler.auditor import audit_rules_context
from claude_setup.assembler.consolidator import consolidate_framework_rules
from claude_setup.domain.core_kp_routing import get_active_routes
from claude_setup.domain.stack_pack_mapping import get_stack_pack_name
from claude_setup.domain.version_resolver import find_version_dir
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

logger = logging.getLogger(__name__)

SQL_DB_TYPES = ("postgresql", "oracle", "mysql")
NOSQL_DB_TYPES = ("mongodb", "cassandra")


class RulesAssembler:
    """Assembles .claude/rules/ and skills/ from source knowledge packs."""

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Orchestrate all assembly layers."""
        src_dir = Path(__file__).resolve().parent.parent.parent / "src"
        rules_dir = output_dir / "rules"
        skills_dir = output_dir / "skills"
        rules_dir.mkdir(parents=True, exist_ok=True)
        skills_dir.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        generated.extend(self._copy_core_rules(config, src_dir, rules_dir, engine))
        generated.extend(self._route_core_to_kps(config, src_dir, skills_dir))
        generated.extend(self._copy_language_kps(config, src_dir, skills_dir))
        generated.extend(self._copy_framework_kps(config, src_dir, skills_dir))
        generated.append(self._generate_project_identity(config, rules_dir))
        generated.append(self._copy_domain_template(config, src_dir, rules_dir, engine))
        generated.extend(self._copy_database_refs(config, src_dir, skills_dir, engine))
        generated.extend(self._copy_cache_refs(config, src_dir, skills_dir))
        generated.extend(self._assemble_security_rules(config, src_dir, skills_dir))
        generated.extend(self._assemble_cloud_knowledge(config, src_dir, skills_dir))
        generated.extend(self._assemble_infra_knowledge(config, src_dir, skills_dir))
        result = audit_rules_context(rules_dir)
        for warning in result.warnings:
            logger.warning(warning)
        return generated

    def _copy_core_rules(
        self,
        config: ProjectConfig,
        src_dir: Path,
        rules_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Layer 1: Copy core-rules/*.md with placeholder replacement."""
        core_rules = src_dir / "core-rules"
        if not core_rules.is_dir():
            return []
        generated: List[Path] = []
        for rule_file in sorted(core_rules.glob("*.md")):
            content = rule_file.read_text(encoding="utf-8")
            content = engine.replace_placeholders(content)
            dest = rules_dir / rule_file.name
            dest.write_text(content, encoding="utf-8")
            generated.append(dest)
        return generated

    def _route_core_to_kps(
        self,
        config: ProjectConfig,
        src_dir: Path,
        skills_dir: Path,
    ) -> List[Path]:
        """Layer 1b: Route core detailed rules to knowledge packs."""
        core_dir = src_dir / "core"
        if not core_dir.is_dir():
            return []
        routes = get_active_routes(config)
        generated: List[Path] = []
        for route in routes:
            src = core_dir / route.source_file
            if not src.is_file():
                continue
            dest_dir = skills_dir / route.kp_name / "references"
            dest_dir.mkdir(parents=True, exist_ok=True)
            dest = dest_dir / route.dest_file
            shutil.copy2(str(src), str(dest))
            generated.append(dest)
        return generated

    def _copy_language_kps(
        self,
        config: ProjectConfig,
        src_dir: Path,
        skills_dir: Path,
    ) -> List[Path]:
        """Layer 2: Route language files to coding-standards and testing KPs."""
        lang = config.language.name
        lang_dir = src_dir / "languages" / lang
        if not lang_dir.is_dir():
            return []
        coding_refs = skills_dir / "coding-standards" / "references"
        testing_refs = skills_dir / "testing" / "references"
        coding_refs.mkdir(parents=True, exist_ok=True)
        testing_refs.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        generated.extend(self._copy_lang_common(lang_dir, coding_refs, testing_refs))
        generated.extend(self._copy_lang_version(config, lang_dir, coding_refs))
        return generated

    def _copy_lang_common(
        self,
        lang_dir: Path,
        coding_refs: Path,
        testing_refs: Path,
    ) -> List[Path]:
        common = lang_dir / "common"
        if not common.is_dir():
            return []
        generated: List[Path] = []
        for md_file in sorted(common.glob("*.md")):
            dest = testing_refs if "testing" in md_file.name else coding_refs
            target = dest / md_file.name
            shutil.copy2(str(md_file), str(target))
            generated.append(target)
        return generated

    def _copy_lang_version(
        self,
        config: ProjectConfig,
        lang_dir: Path,
        coding_refs: Path,
    ) -> List[Path]:
        version_dir = find_version_dir(
            lang_dir, config.language.name, config.language.version,
        )
        if not version_dir:
            return []
        generated: List[Path] = []
        for md_file in sorted(version_dir.glob("*.md")):
            target = coding_refs / md_file.name
            shutil.copy2(str(md_file), str(target))
            generated.append(target)
        return generated

    def _copy_framework_kps(
        self,
        config: ProjectConfig,
        src_dir: Path,
        skills_dir: Path,
    ) -> List[Path]:
        """Layer 3: Route framework files to stack-patterns KP."""
        fw = config.framework.name
        pack_name = get_stack_pack_name(fw)
        if not pack_name:
            return []
        fw_dir = src_dir / "frameworks" / fw
        if not fw_dir.is_dir():
            return []
        refs_dir = skills_dir / pack_name / "references"
        refs_dir.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        generated.extend(self._copy_fw_common(fw_dir, refs_dir))
        generated.extend(self._copy_fw_version(config, fw_dir, refs_dir))
        return generated

    def _copy_fw_common(self, fw_dir: Path, refs_dir: Path) -> List[Path]:
        common = fw_dir / "common"
        if not common.is_dir():
            return []
        generated: List[Path] = []
        for md_file in sorted(common.glob("*.md")):
            target = refs_dir / md_file.name
            shutil.copy2(str(md_file), str(target))
            generated.append(target)
        return generated

    def _copy_fw_version(
        self,
        config: ProjectConfig,
        fw_dir: Path,
        refs_dir: Path,
    ) -> List[Path]:
        version_dir = find_version_dir(
            fw_dir, config.framework.name, config.framework.version,
        )
        if not version_dir:
            return []
        generated: List[Path] = []
        for md_file in sorted(version_dir.glob("*.md")):
            target = refs_dir / md_file.name
            shutil.copy2(str(md_file), str(target))
            generated.append(target)
        return generated

    def _generate_project_identity(
        self,
        config: ProjectConfig,
        rules_dir: Path,
    ) -> Path:
        """Layer 4: Generate 01-project-identity.md."""
        dest = rules_dir / "01-project-identity.md"
        content = _build_identity_content(config)
        dest.write_text(content, encoding="utf-8")
        return dest

    def _copy_domain_template(
        self,
        config: ProjectConfig,
        src_dir: Path,
        rules_dir: Path,
        engine: TemplateEngine,
    ) -> Path:
        """Layer 4: Copy/generate 02-domain.md."""
        dest = rules_dir / "02-domain.md"
        template = src_dir / "templates" / "domain-template.md"
        if template.is_file():
            content = template.read_text(encoding="utf-8")
            content = engine.replace_placeholders(content)
            dest.write_text(content, encoding="utf-8")
        else:
            dest.write_text(_fallback_domain_content(config), encoding="utf-8")
        return dest

    def _copy_database_refs(
        self,
        config: ProjectConfig,
        src_dir: Path,
        skills_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Conditional: copy database references."""
        db_name = config.data.database.name
        if db_name == "none":
            return []
        db_dir = src_dir / "databases"
        target = skills_dir / "database-patterns" / "references"
        target.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        generated.extend(self._copy_db_version_matrix(db_dir, target))
        generated.extend(self._copy_db_type_files(db_name, db_dir, target))
        _replace_placeholders_in_dir(target, engine)
        return generated

    def _copy_db_version_matrix(
        self,
        db_dir: Path,
        target: Path,
    ) -> List[Path]:
        matrix = db_dir / "version-matrix.md"
        if matrix.is_file():
            dest = target / "version-matrix.md"
            shutil.copy2(str(matrix), str(dest))
            return [dest]
        return []

    def _copy_db_type_files(
        self,
        db_name: str,
        db_dir: Path,
        target: Path,
    ) -> List[Path]:
        generated: List[Path] = []
        if db_name in SQL_DB_TYPES:
            generated.extend(_copy_md_dir(db_dir / "sql" / "common", target))
            generated.extend(_copy_md_dir(db_dir / "sql" / db_name, target))
        elif db_name in NOSQL_DB_TYPES:
            generated.extend(_copy_md_dir(db_dir / "nosql" / "common", target))
            generated.extend(_copy_md_dir(db_dir / "nosql" / db_name, target))
        return generated

    def _copy_cache_refs(
        self,
        config: ProjectConfig,
        src_dir: Path,
        skills_dir: Path,
    ) -> List[Path]:
        """Conditional: copy cache references."""
        cache_name = config.data.cache.name
        if cache_name == "none":
            return []
        db_dir = src_dir / "databases"
        target = skills_dir / "database-patterns" / "references"
        target.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        generated.extend(_copy_md_dir(db_dir / "cache" / "common", target))
        generated.extend(_copy_md_dir(db_dir / "cache" / cache_name, target))
        return generated

    def _assemble_security_rules(
        self,
        config: ProjectConfig,
        src_dir: Path,
        skills_dir: Path,
    ) -> List[Path]:
        """Conditional: copy security files to security/compliance KPs."""
        if not config.security.frameworks:
            return []
        sec_dir = src_dir / "security"
        generated: List[Path] = []
        generated.extend(self._copy_security_base(sec_dir, skills_dir))
        generated.extend(self._copy_compliance(config, sec_dir, skills_dir))
        return generated

    def _copy_security_base(
        self,
        sec_dir: Path,
        skills_dir: Path,
    ) -> List[Path]:
        sec_kp = skills_dir / "security" / "references"
        sec_kp.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        for name in ("application-security.md", "cryptography.md"):
            src = sec_dir / name
            if src.is_file():
                dest = sec_kp / name
                shutil.copy2(str(src), str(dest))
                generated.append(dest)
        return generated

    def _copy_compliance(
        self,
        config: ProjectConfig,
        sec_dir: Path,
        skills_dir: Path,
    ) -> List[Path]:
        comp_kp = skills_dir / "compliance" / "references"
        comp_kp.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        for framework in config.security.frameworks:
            src = sec_dir / "compliance" / f"{framework}.md"
            if src.is_file():
                dest = comp_kp / f"{framework}.md"
                shutil.copy2(str(src), str(dest))
                generated.append(dest)
        return generated

    def _assemble_cloud_knowledge(
        self,
        config: ProjectConfig,
        src_dir: Path,
        skills_dir: Path,
    ) -> List[Path]:
        """Conditional: copy cloud provider files."""
        provider = getattr(config.infrastructure, "cloud_provider", "none")
        if provider == "none" or not provider:
            return []
        cloud_dir = src_dir / "cloud-providers"
        kp_dir = skills_dir / "knowledge-packs"
        kp_dir.mkdir(parents=True, exist_ok=True)
        src = cloud_dir / f"{provider}.md"
        if src.is_file():
            dest = kp_dir / f"cloud-{provider}.md"
            shutil.copy2(str(src), str(dest))
            return [dest]
        return []

    def _assemble_infra_knowledge(
        self,
        config: ProjectConfig,
        src_dir: Path,
        skills_dir: Path,
    ) -> List[Path]:
        """Conditional: copy infrastructure knowledge packs."""
        infra_dir = src_dir / "infrastructure"
        kp_dir = skills_dir / "knowledge-packs"
        kp_dir.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        generated.extend(self._copy_k8s_files(config, infra_dir, kp_dir))
        generated.extend(self._copy_container_files(config, infra_dir, kp_dir))
        generated.extend(self._copy_iac_files(config, infra_dir, kp_dir))
        return generated

    def _copy_k8s_files(
        self,
        config: ProjectConfig,
        infra_dir: Path,
        kp_dir: Path,
    ) -> List[Path]:
        if config.infrastructure.orchestrator != "kubernetes":
            return []
        src = infra_dir / "kubernetes" / "deployment-patterns.md"
        if src.is_file():
            dest = kp_dir / "k8s-deployment.md"
            shutil.copy2(str(src), str(dest))
            return [dest]
        return []

    def _copy_container_files(
        self,
        config: ProjectConfig,
        infra_dir: Path,
        kp_dir: Path,
    ) -> List[Path]:
        if config.infrastructure.container == "none":
            return []
        generated: List[Path] = []
        for name, dest_name in [
            ("dockerfile-patterns.md", "dockerfile.md"),
            ("registry-patterns.md", "registry.md"),
        ]:
            src = infra_dir / "containers" / name
            if src.is_file():
                dest = kp_dir / dest_name
                shutil.copy2(str(src), str(dest))
                generated.append(dest)
        return generated

    def _copy_iac_files(
        self,
        config: ProjectConfig,
        infra_dir: Path,
        kp_dir: Path,
    ) -> List[Path]:
        iac = getattr(config.infrastructure, "iac", "none")
        if iac == "none" or not iac:
            return []
        src = infra_dir / "iac" / f"{iac}-patterns.md"
        if src.is_file():
            dest = kp_dir / f"iac-{iac}.md"
            shutil.copy2(str(src), str(dest))
            return [dest]
        return []


def _build_identity_content(config: ProjectConfig) -> str:
    """Build the 01-project-identity.md content."""
    ifaces = ", ".join(i.type for i in config.interfaces) or "none"
    fw_ver = f" {config.framework.version}" if config.framework.version else ""
    lines: List[str] = []
    lines.extend(_identity_header(config, ifaces, fw_ver))
    lines.extend(_identity_tech_stack(config, fw_ver))
    lines.extend(_identity_footer())
    return "\n".join(lines) + "\n"


def _identity_header(config: ProjectConfig, ifaces: str, fw_ver: str) -> List[str]:
    return [
        "# Global Behavior & Language Policy",
        "- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).",
        '- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. '
        "Start responses directly with technical information.",
        "- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.",
        "",
        f"# Project Identity — {config.project.name}",
        "",
        "## Identity",
        f"- **Name:** {config.project.name}",
        f"- **Purpose:** {config.project.purpose}",
        f"- **Architecture Style:** {config.architecture.style}",
        f"- **Domain-Driven Design:** {str(config.architecture.domain_driven).lower()}",
        f"- **Event-Driven:** {str(config.architecture.event_driven).lower()}",
        f"- **Interfaces:** {ifaces}",
        f"- **Language:** {config.language.name} {config.language.version}",
        f"- **Framework:** {config.framework.name}{fw_ver}",
    ]


def _identity_tech_stack(config: ProjectConfig, fw_ver: str) -> List[str]:
    obs = config.infrastructure.observability
    return [
        "",
        "## Technology Stack",
        "| Layer | Technology |",
        "|-------|-----------|",
        f"| Architecture | {config.architecture.style} |",
        f"| Language | {config.language.name} {config.language.version} |",
        f"| Framework | {config.framework.name}{fw_ver} |",
        f"| Build Tool | {config.framework.build_tool} |",
        f"| Database | {config.data.database.name} |",
        f"| Migration | {config.data.migration.name} |",
        f"| Cache | {config.data.cache.name} |",
        f"| Message Broker | none |",
        f"| Container | {config.infrastructure.container} |",
        f"| Orchestrator | {config.infrastructure.orchestrator} |",
        f"| Observability | {obs.tool} ({obs.tracing}) |",
        "| Resilience | Mandatory (always enabled) |",
        f"| Native Build | {str(config.framework.native_build).lower()} |",
        f"| Smoke Tests | {str(config.testing.smoke_tests).lower()} |",
        f"| Contract Tests | {str(config.testing.contract_tests).lower()} |",
    ]


def _identity_footer() -> List[str]:
    return [
        "",
        "## Source of Truth (Hierarchy)",
        "1. Epics / PRDs (vision and global rules)",
        "2. ADRs (architectural decisions)",
        "3. Stories / tickets (detailed requirements)",
        "4. Rules (.claude/rules/)",
        "5. Source code",
        "",
        "## Language",
        "- Code: English (classes, methods, variables)",
        "- Commits: English (Conventional Commits)",
        "- Documentation: English (customize as needed)",
        "- Application logs: English",
        "",
        "## Constraints",
        "<!-- Customize constraints for your project -->",
        "- Cloud-Agnostic: ZERO dependencies on cloud-specific services",
        "- Horizontal scalability: Application must be stateless",
        "- Externalized configuration: All configuration via environment variables or ConfigMaps",
    ]


def _fallback_domain_content(config: ProjectConfig) -> str:
    return f"# Rule — {{DOMAIN_NAME}} Domain\n\n{config.project.name}\n"


def _copy_md_dir(source_dir: Path, target: Path) -> List[Path]:
    if not source_dir.is_dir():
        return []
    generated: List[Path] = []
    for md_file in sorted(source_dir.glob("*.md")):
        dest = target / md_file.name
        shutil.copy2(str(md_file), str(dest))
        generated.append(dest)
    return generated


def _replace_placeholders_in_dir(
    target_dir: Path,
    engine: TemplateEngine,
) -> None:
    for md_file in sorted(target_dir.glob("*.md")):
        content = md_file.read_text(encoding="utf-8")
        replaced = engine.replace_placeholders(content)
        if replaced != content:
            md_file.write_text(replaced, encoding="utf-8")
