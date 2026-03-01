from __future__ import annotations

import shutil
from pathlib import Path
from typing import List, Optional

from claude_setup.assembler.auditor import audit_rules_context
from claude_setup.assembler.consolidator import consolidate_framework_rules
from claude_setup.domain.core_kp_routing import get_active_routes
from claude_setup.domain.stack_pack_mapping import get_stack_pack_name
from claude_setup.domain.version_resolver import find_version_dir
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

SQL_DB_TYPES = ("postgresql", "oracle", "mysql")
NOSQL_DB_TYPES = ("mongodb", "cassandra")
TESTING_PATTERN = "testing"


class RulesAssembler:
    """Orchestrates 4-layer assembly of .claude/rules/ files."""

    def __init__(self, src_dir: Path) -> None:
        self._src_dir = src_dir

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Orchestrate all layers; return generated file paths."""
        rules_dir = output_dir / "rules"
        skills_dir = output_dir / "skills"
        rules_dir.mkdir(parents=True, exist_ok=True)
        skills_dir.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        generated.extend(self._copy_core_rules(config, rules_dir, engine))
        generated.extend(self._route_core_to_kps(config, skills_dir))
        generated.extend(self._copy_language_kps(config, skills_dir))
        generated.extend(self._copy_framework_kps(config, skills_dir))
        generated.append(self._generate_project_identity(config, rules_dir))
        generated.append(self._copy_domain_template(config, rules_dir, engine))
        generated.extend(self._copy_database_refs(config, skills_dir, engine))
        generated.extend(self._copy_cache_refs(config, skills_dir, engine))
        generated.extend(self._assemble_security(config, skills_dir))
        generated.extend(self._assemble_cloud(config, skills_dir))
        generated.extend(self._assemble_infra(config, skills_dir))
        audit_rules_context(rules_dir)
        return generated

    def _copy_core_rules(
        self,
        config: ProjectConfig,
        rules_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Layer 1: Copy src/core-rules/*.md with placeholder replacement."""
        core_rules_dir = self._src_dir / "core-rules"
        generated: List[Path] = []
        if not core_rules_dir.is_dir():
            return generated
        for rule_file in sorted(core_rules_dir.glob("*.md")):
            dest = rules_dir / rule_file.name
            content = rule_file.read_text()
            dest.write_text(engine.replace_placeholders(content))
            generated.append(dest)
        return generated

    def _route_core_to_kps(
        self,
        config: ProjectConfig,
        skills_dir: Path,
    ) -> List[Path]:
        """Layer 1b: Route src/core/*.md to KP destinations."""
        core_dir = self._src_dir / "core"
        generated: List[Path] = []
        if not core_dir.is_dir():
            return generated
        for route in get_active_routes(config):
            src = core_dir / route.source_file
            if not src.is_file():
                continue
            dest_dir = skills_dir / route.kp_name / "references"
            dest_dir.mkdir(parents=True, exist_ok=True)
            dest = dest_dir / route.dest_file
            shutil.copy2(src, dest)
            generated.append(dest)
        return generated

    def _copy_language_kps(
        self,
        config: ProjectConfig,
        skills_dir: Path,
    ) -> List[Path]:
        """Layer 2: Route language files to coding-standards/testing KPs."""
        lang = config.language.name
        lang_ver = config.language.version
        generated: List[Path] = []
        coding_refs = skills_dir / "coding-standards" / "references"
        testing_refs = skills_dir / "testing" / "references"
        coding_refs.mkdir(parents=True, exist_ok=True)
        testing_refs.mkdir(parents=True, exist_ok=True)
        generated.extend(self._copy_lang_common(lang, coding_refs, testing_refs))
        generated.extend(self._copy_lang_version(lang, lang_ver, coding_refs))
        return generated

    def _copy_lang_common(
        self,
        lang: str,
        coding_refs: Path,
        testing_refs: Path,
    ) -> List[Path]:
        """Copy language common files, routing testing to testing KP."""
        common_dir = self._src_dir / "languages" / lang / "common"
        generated: List[Path] = []
        if not common_dir.is_dir():
            return generated
        for f in sorted(common_dir.glob("*.md")):
            if TESTING_PATTERN in f.name:
                dest = testing_refs / f.name
            else:
                dest = coding_refs / f.name
            shutil.copy2(f, dest)
            generated.append(dest)
        return generated

    def _copy_lang_version(
        self,
        lang: str,
        lang_ver: str,
        coding_refs: Path,
    ) -> List[Path]:
        """Copy language version-specific files to coding-standards KP."""
        lang_base = self._src_dir / "languages" / lang
        version_dir = find_version_dir(lang_base, lang, lang_ver)
        generated: List[Path] = []
        if version_dir is None:
            return generated
        for f in sorted(version_dir.glob("*.md")):
            dest = coding_refs / f.name
            shutil.copy2(f, dest)
            generated.append(dest)
        return generated

    def _copy_framework_kps(
        self,
        config: ProjectConfig,
        skills_dir: Path,
    ) -> List[Path]:
        """Layer 3: Route framework files to stack-patterns KP."""
        fw = config.framework.name
        fw_ver = config.framework.version
        pack_name = get_stack_pack_name(fw)
        if not pack_name:
            return []
        fw_refs = skills_dir / pack_name / "references"
        fw_refs.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        generated.extend(self._copy_fw_common(fw, fw_refs))
        generated.extend(self._copy_fw_version(fw, fw_ver, fw_refs))
        return generated

    def _copy_fw_common(self, fw: str, fw_refs: Path) -> List[Path]:
        """Copy framework common files."""
        common_dir = self._src_dir / "frameworks" / fw / "common"
        generated: List[Path] = []
        if not common_dir.is_dir():
            return generated
        for f in sorted(common_dir.glob("*.md")):
            dest = fw_refs / f.name
            shutil.copy2(f, dest)
            generated.append(dest)
        return generated

    def _copy_fw_version(
        self,
        fw: str,
        fw_ver: str,
        fw_refs: Path,
    ) -> List[Path]:
        """Copy framework version-specific files."""
        if not fw_ver:
            return []
        fw_base = self._src_dir / "frameworks" / fw
        version_dir = find_version_dir(fw_base, fw, fw_ver)
        generated: List[Path] = []
        if version_dir is None:
            return generated
        for f in sorted(version_dir.glob("*.md")):
            dest = fw_refs / f.name
            shutil.copy2(f, dest)
            generated.append(dest)
        return generated

    def _generate_project_identity(
        self,
        config: ProjectConfig,
        rules_dir: Path,
    ) -> Path:
        """Layer 4: Generate 01-project-identity.md."""
        dest = rules_dir / "01-project-identity.md"
        content = _build_identity_content(config)
        dest.write_text(content)
        return dest

    def _copy_domain_template(
        self,
        config: ProjectConfig,
        rules_dir: Path,
        engine: TemplateEngine,
    ) -> Path:
        """Layer 4: Copy/generate 02-domain.md."""
        dest = rules_dir / "02-domain.md"
        template = self._src_dir / "templates" / "domain-template.md"
        if template.is_file():
            content = template.read_text()
            dest.write_text(engine.replace_placeholders(content))
        else:
            dest.write_text("# Domain\n\nCustomize for your domain.\n")
        return dest

    def _copy_database_refs(
        self,
        config: ProjectConfig,
        skills_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Conditional: copy DB refs to database-patterns KP."""
        db_name = config.data.database.name
        if db_name == "none":
            return []
        target = skills_dir / "database-patterns" / "references"
        target.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        generated.extend(self._copy_db_version_matrix(target))
        generated.extend(self._copy_db_type_files(db_name, target))
        _replace_placeholders_in_dir(target, engine)
        return generated

    def _copy_db_version_matrix(self, target: Path) -> List[Path]:
        """Copy version-matrix.md if it exists."""
        matrix = self._src_dir / "databases" / "version-matrix.md"
        if matrix.is_file():
            dest = target / "version-matrix.md"
            shutil.copy2(matrix, dest)
            return [dest]
        return []

    def _copy_db_type_files(
        self,
        db_name: str,
        target: Path,
    ) -> List[Path]:
        """Copy database type-specific files (sql/nosql)."""
        db_base = self._src_dir / "databases"
        generated: List[Path] = []
        if db_name in SQL_DB_TYPES:
            generated.extend(_copy_md_dir(db_base / "sql" / "common", target))
            generated.extend(_copy_md_dir(db_base / "sql" / db_name, target))
        elif db_name in NOSQL_DB_TYPES:
            generated.extend(_copy_md_dir(db_base / "nosql" / "common", target))
            generated.extend(_copy_md_dir(db_base / "nosql" / db_name, target))
        return generated

    def _copy_cache_refs(
        self,
        config: ProjectConfig,
        skills_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Conditional: copy cache refs to database-patterns KP."""
        cache_name = config.data.cache.name
        if cache_name == "none":
            return []
        target = skills_dir / "database-patterns" / "references"
        target.mkdir(parents=True, exist_ok=True)
        db_base = self._src_dir / "databases"
        generated: List[Path] = []
        generated.extend(_copy_md_dir(db_base / "cache" / "common", target))
        generated.extend(_copy_md_dir(db_base / "cache" / cache_name, target))
        _replace_placeholders_in_dir(target, engine)
        return generated

    def _assemble_security(
        self,
        config: ProjectConfig,
        skills_dir: Path,
    ) -> List[Path]:
        """Conditional: copy security files to security/compliance KPs."""
        if not config.security.frameworks:
            return []
        sec_dir = self._src_dir / "security"
        if not sec_dir.is_dir():
            return []
        generated: List[Path] = []
        generated.extend(self._copy_security_base(sec_dir, skills_dir))
        generated.extend(self._copy_compliance(config, sec_dir, skills_dir))
        return generated

    def _copy_security_base(
        self,
        sec_dir: Path,
        skills_dir: Path,
    ) -> List[Path]:
        """Copy base security files to security KP."""
        security_kp = skills_dir / "security" / "references"
        security_kp.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        for name in ("application-security.md", "cryptography.md"):
            src = sec_dir / name
            if src.is_file():
                dest = security_kp / name
                shutil.copy2(src, dest)
                generated.append(dest)
        return generated

    def _copy_compliance(
        self,
        config: ProjectConfig,
        sec_dir: Path,
        skills_dir: Path,
    ) -> List[Path]:
        """Copy compliance framework files to compliance KP."""
        compliance_kp = skills_dir / "compliance" / "references"
        compliance_kp.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        for framework in config.security.frameworks:
            src = sec_dir / "compliance" / f"{framework}.md"
            if src.is_file():
                dest = compliance_kp / f"{framework}.md"
                shutil.copy2(src, dest)
                generated.append(dest)
        return generated

    def _assemble_cloud(
        self,
        config: ProjectConfig,
        skills_dir: Path,
    ) -> List[Path]:
        """Conditional: copy cloud provider file to KPs."""
        cloud_dir = self._src_dir / "cloud-providers"
        kp_dir = skills_dir / "knowledge-packs"
        return _copy_cloud_provider(config, cloud_dir, kp_dir)

    def _assemble_infra(
        self,
        config: ProjectConfig,
        skills_dir: Path,
    ) -> List[Path]:
        """Conditional: copy infrastructure files to KPs."""
        infra_dir = self._src_dir / "infrastructure"
        kp_dir = skills_dir / "knowledge-packs"
        kp_dir.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        generated.extend(_copy_k8s_files(config, infra_dir, kp_dir))
        generated.extend(_copy_container_files(config, infra_dir, kp_dir))
        generated.extend(_copy_iac_files(config, infra_dir, kp_dir))
        return generated


def _build_identity_content(config: ProjectConfig) -> str:
    """Build the project identity markdown content."""
    iface_list = " ".join(i.type for i in config.interfaces)
    fw_label = config.framework.name
    if config.framework.version:
        fw_label = f"{fw_label} {config.framework.version}"
    obs = config.infrastructure.observability
    obs_label = f"{obs.tool} ({obs.metrics})"
    lines = _identity_lines(config, iface_list, fw_label, obs_label)
    return "\n".join(lines) + "\n"


def _identity_lines(
    config: ProjectConfig,
    iface_list: str,
    fw_label: str,
    obs_label: str,
) -> List[str]:
    """Return lines for the project identity file."""
    c = config
    return [
        "# Global Behavior & Language Policy",
        "- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).",
        "- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff."
        " Start responses directly with technical information.",
        "- **Priority**: Maintain 100% fidelity to the technical constraints"
        " defined in the original rules below.",
        "",
        f"# Project Identity — {c.project.name}",
        "",
        "## Identity",
        f"- **Name:** {c.project.name}",
        f"- **Purpose:** {c.project.purpose}",
        f"- **Architecture Style:** {c.architecture.style}",
        f"- **Domain-Driven Design:** {c.architecture.domain_driven}",
        f"- **Event-Driven:** {c.architecture.event_driven}",
        f"- **Interfaces:** {iface_list}",
        f"- **Language:** {c.language.name} {c.language.version}",
        f"- **Framework:** {fw_label}",
        "",
        "## Technology Stack",
        "| Layer | Technology |",
        "|-------|-----------|",
        f"| Architecture | {c.architecture.style} |",
        f"| Language | {c.language.name} {c.language.version} |",
        f"| Framework | {fw_label} |",
        f"| Build Tool | {c.framework.build_tool} |",
        f"| Database | {c.data.database.name} |",
        f"| Migration | {c.data.migration.name} |",
        f"| Cache | {c.data.cache.name} |",
        f"| Message Broker | none |",
        f"| Container | {c.infrastructure.container} |",
        f"| Orchestrator | {c.infrastructure.orchestrator} |",
        f"| Observability | {obs_label} |",
        "| Resilience | Mandatory (always enabled) |",
        f"| Native Build | {c.framework.native_build} |",
        f"| Smoke Tests | {c.testing.smoke_tests} |",
        f"| Contract Tests | {c.testing.contract_tests} |",
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


def _copy_md_dir(src_dir: Path, target: Path) -> List[Path]:
    """Copy all .md files from src_dir to target."""
    generated: List[Path] = []
    if not src_dir.is_dir():
        return generated
    for f in sorted(src_dir.glob("*.md")):
        dest = target / f.name
        shutil.copy2(f, dest)
        generated.append(dest)
    return generated


def _replace_placeholders_in_dir(
    target: Path,
    engine: TemplateEngine,
) -> None:
    """Replace placeholders in all .md files in a directory."""
    for md_file in sorted(target.glob("*.md")):
        content = md_file.read_text()
        md_file.write_text(engine.replace_placeholders(content))


def _copy_cloud_provider(
    config: ProjectConfig,
    cloud_dir: Path,
    kp_dir: Path,
) -> List[Path]:
    """Copy cloud provider file if configured."""
    provider = _get_cloud_provider(config)
    if not provider:
        return []
    src = cloud_dir / f"{provider}.md"
    if not src.is_file():
        return []
    kp_dir.mkdir(parents=True, exist_ok=True)
    dest = kp_dir / f"cloud-{provider}.md"
    shutil.copy2(src, dest)
    return [dest]


def _get_cloud_provider(config: ProjectConfig) -> Optional[str]:
    """Infer cloud provider from infrastructure config."""
    registry = config.infrastructure.registry
    provider_map = {"ecr": "aws", "gcr": "gcp", "acr": "azure"}
    return provider_map.get(registry)


def _copy_k8s_files(
    config: ProjectConfig,
    infra_dir: Path,
    kp_dir: Path,
) -> List[Path]:
    """Copy Kubernetes files if orchestrator is kubernetes."""
    if config.infrastructure.orchestrator != "kubernetes":
        return []
    generated: List[Path] = []
    k8s_dir = infra_dir / "kubernetes"
    deploy = k8s_dir / "deployment-patterns.md"
    if deploy.is_file():
        dest = kp_dir / "k8s-deployment.md"
        shutil.copy2(deploy, dest)
        generated.append(dest)
    templating = config.infrastructure.templating
    tmpl_file = k8s_dir / f"{templating}-patterns.md"
    if tmpl_file.is_file():
        dest = kp_dir / f"k8s-{templating}.md"
        shutil.copy2(tmpl_file, dest)
        generated.append(dest)
    return generated


def _copy_container_files(
    config: ProjectConfig,
    infra_dir: Path,
    kp_dir: Path,
) -> List[Path]:
    """Copy container files if container is configured."""
    if config.infrastructure.container == "none":
        return []
    containers_dir = infra_dir / "containers"
    generated: List[Path] = []
    for name, dest_name in [
        ("dockerfile-patterns.md", "dockerfile.md"),
        ("registry-patterns.md", "registry.md"),
    ]:
        src = containers_dir / name
        if src.is_file():
            dest = kp_dir / dest_name
            shutil.copy2(src, dest)
            generated.append(dest)
    return generated


def _copy_iac_files(
    config: ProjectConfig,
    infra_dir: Path,
    kp_dir: Path,
) -> List[Path]:
    """Copy IaC files if iac is configured."""
    iac = config.infrastructure.iac
    if iac == "none":
        return []
    src = infra_dir / "iac" / f"{iac}-patterns.md"
    if not src.is_file():
        return []
    dest = kp_dir / f"iac-{iac}.md"
    shutil.copy2(src, dest)
    return [dest]
