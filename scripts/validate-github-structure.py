"""Validate all generated .github/ artifacts end-to-end."""
from __future__ import annotations

import json
import re
import sys
from dataclasses import dataclass, field
from enum import Enum
from pathlib import Path

class Severity(Enum):
    MINOR = "minor"
    MAJOR = "major"
    CRITICAL = "critical"


class Decision(Enum):
    GO = "GO"
    NO_GO = "NO-GO"


@dataclass
class Finding:
    component: str
    output_target: str
    file: str
    message: str
    severity: Severity


@dataclass
class ComponentResult:
    component: str
    output_target: str
    total_artifacts: int = 0
    passed: int = 0
    failed: int = 0
    severity: Severity = Severity.MINOR
    findings: list[Finding] = field(default_factory=list)


@dataclass
class ValidationReport:
    results: list[ComponentResult] = field(default_factory=list)

    @property
    def decision(self) -> Decision:
        for r in self.results:
            if any(f.severity == Severity.CRITICAL for f in r.findings):
                return Decision.NO_GO
        return Decision.GO

    def print_report(self) -> None:
        print("=" * 70)
        print("VALIDATION REPORT — Generated Structure")
        print("=" * 70)
        for r in self.results:
            status = "PASS" if r.failed == 0 else "FAIL"
            print(
                f"  [{status}] {r.output_target}/{r.component}: "
                f"{r.passed}/{r.total_artifacts} passed"
            )
            for f in r.findings:
                print(f"    [{f.severity.value.upper()}] {f.file}: {f.message}")
        print("-" * 70)
        print(f"Decision: {self.decision.value}")
        print("=" * 70)


def _has_yaml_frontmatter(text: str) -> bool:
    return text.startswith("---\n")


def _extract_frontmatter(text: str) -> str:
    if not text.startswith("---\n"):
        return ""
    end = text.find("\n---", 4)
    if end == -1:
        return ""
    return text[4:end]


def _validate_instructions(output_dir: Path) -> ComponentResult:
    """Validate .github/instructions/*.instructions.md files."""
    result = ComponentResult("instructions", ".github")
    instr_dir = output_dir / "github" / "instructions"
    if not instr_dir.is_dir():
        return result
    files = list(instr_dir.glob("*"))
    result.total_artifacts = len(files)
    for f in files:
        if not f.name.endswith(".instructions.md"):
            result.findings.append(Finding(
                "instructions", ".github", f.name,
                f"Wrong extension: expected .instructions.md, got {f.name}",
                Severity.CRITICAL,
            ))
            result.failed += 1
            continue
        content = f.read_text(encoding="utf-8")
        if len(content.strip()) == 0:
            result.findings.append(Finding(
                "instructions", ".github", f.name,
                "Empty file",
                Severity.MAJOR,
            ))
            result.failed += 1
            continue
        result.passed += 1
    result.severity = _max_severity(result.findings)
    return result


def _validate_global_instructions(output_dir: Path) -> ComponentResult:
    """Validate .github/copilot-instructions.md."""
    result = ComponentResult("copilot-instructions", ".github")
    global_file = output_dir / "github" / "copilot-instructions.md"
    result.total_artifacts = 1
    if not global_file.is_file():
        result.findings.append(Finding(
            "copilot-instructions", ".github",
            "copilot-instructions.md", "File missing",
            Severity.CRITICAL,
        ))
        result.failed = 1
        result.severity = Severity.CRITICAL
        return result
    content = global_file.read_text(encoding="utf-8")
    if len(content.strip()) == 0:
        result.findings.append(Finding(
            "copilot-instructions", ".github",
            "copilot-instructions.md", "Empty file",
            Severity.CRITICAL,
        ))
        result.failed = 1
    else:
        result.passed = 1
    result.severity = _max_severity(result.findings)
    return result


def _validate_skills(output_dir: Path) -> ComponentResult:
    """Validate .github/skills/*/SKILL.md files."""
    result = ComponentResult("skills", ".github")
    skills_dir = output_dir / "github" / "skills"
    if not skills_dir.is_dir():
        return result
    skill_files = list(skills_dir.glob("*/SKILL.md"))
    result.total_artifacts = len(skill_files)
    for sf in skill_files:
        skill_name = sf.parent.name
        content = sf.read_text(encoding="utf-8")
        if not _has_yaml_frontmatter(content):
            result.findings.append(Finding(
                "skills", ".github", skill_name,
                "Missing YAML frontmatter",
                Severity.CRITICAL,
            ))
            result.failed += 1
            continue
        fm = _extract_frontmatter(content)
        if not re.match(r"^[a-z0-9-]+$", skill_name):
            result.findings.append(Finding(
                "skills", ".github", skill_name,
                f"Name must be lowercase-hyphens: {skill_name}",
                Severity.CRITICAL,
            ))
            result.failed += 1
            continue
        if "description:" not in fm:
            result.findings.append(Finding(
                "skills", ".github", skill_name,
                "Missing 'description' field in frontmatter",
                Severity.MAJOR,
            ))
            result.failed += 1
            continue
        result.passed += 1
    result.severity = _max_severity(result.findings)
    return result


def _validate_agents(output_dir: Path) -> ComponentResult:
    """Validate .github/agents/*.agent.md files."""
    result = ComponentResult("agents", ".github")
    agents_dir = output_dir / "github" / "agents"
    if not agents_dir.is_dir():
        return result
    files = list(agents_dir.glob("*"))
    result.total_artifacts = len(files)
    for f in files:
        if not f.name.endswith(".agent.md"):
            result.findings.append(Finding(
                "agents", ".github", f.name,
                f"Wrong extension: expected .agent.md, got {f.name}",
                Severity.CRITICAL,
            ))
            result.failed += 1
            continue
        content = f.read_text(encoding="utf-8")
        if not _has_yaml_frontmatter(content):
            result.findings.append(Finding(
                "agents", ".github", f.name,
                "Missing YAML frontmatter",
                Severity.CRITICAL,
            ))
            result.failed += 1
            continue
        fm = _extract_frontmatter(content)
        has_tools = "tools:" in fm
        has_disallowed = "disallowed-tools:" in fm or "disallowedTools:" in fm
        if not has_tools and not has_disallowed:
            result.findings.append(Finding(
                "agents", ".github", f.name,
                "Missing both 'tools' and 'disallowed-tools' in frontmatter",
                Severity.MAJOR,
            ))
            result.failed += 1
            continue
        result.passed += 1
    result.severity = _max_severity(result.findings)
    return result


def _validate_prompts(output_dir: Path) -> ComponentResult:
    """Validate .github/prompts/*.prompt.md files."""
    result = ComponentResult("prompts", ".github")
    prompts_dir = output_dir / "github" / "prompts"
    if not prompts_dir.is_dir():
        return result
    files = list(prompts_dir.glob("*"))
    result.total_artifacts = len(files)
    for f in files:
        if not f.name.endswith(".prompt.md"):
            result.findings.append(Finding(
                "prompts", ".github", f.name,
                f"Wrong extension: expected .prompt.md",
                Severity.CRITICAL,
            ))
            result.failed += 1
            continue
        content = f.read_text(encoding="utf-8")
        if len(content.strip()) == 0:
            result.findings.append(Finding(
                "prompts", ".github", f.name,
                "Empty file",
                Severity.MAJOR,
            ))
            result.failed += 1
            continue
        result.passed += 1
    result.severity = _max_severity(result.findings)
    return result


def _validate_hooks(output_dir: Path) -> ComponentResult:
    """Validate .github/hooks/*.json files."""
    result = ComponentResult("hooks", ".github")
    hooks_dir = output_dir / "github" / "hooks"
    if not hooks_dir.is_dir():
        return result
    files = list(hooks_dir.glob("*"))
    result.total_artifacts = len(files)
    valid_events = {
        "PreToolUse", "PostToolUse", "PreCommit",
        "PostCommit", "Notification",
    }
    for f in files:
        if f.suffix != ".json":
            result.findings.append(Finding(
                "hooks", ".github", f.name,
                "Expected .json extension",
                Severity.CRITICAL,
            ))
            result.failed += 1
            continue
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
        except json.JSONDecodeError as e:
            result.findings.append(Finding(
                "hooks", ".github", f.name,
                f"Invalid JSON: {e}",
                Severity.CRITICAL,
            ))
            result.failed += 1
            continue
        hooks_list = data if isinstance(data, list) else [data]
        hook_ok = True
        for hook in hooks_list:
            event = hook.get("event", "")
            if event and event not in valid_events:
                result.findings.append(Finding(
                    "hooks", ".github", f.name,
                    f"Unknown event type: {event}",
                    Severity.MAJOR,
                ))
                hook_ok = False
            timeout = hook.get("timeout_ms", 0)
            if timeout > 60000:
                result.findings.append(Finding(
                    "hooks", ".github", f.name,
                    f"Timeout {timeout}ms exceeds 60s limit",
                    Severity.MAJOR,
                ))
                hook_ok = False
        if hook_ok:
            result.passed += 1
        else:
            result.failed += 1
    result.severity = _max_severity(result.findings)
    return result


def _validate_mcp(output_dir: Path) -> ComponentResult:
    """Validate .github/copilot-mcp.json."""
    result = ComponentResult("mcp", ".github")
    mcp_file = output_dir / "github" / "copilot-mcp.json"
    result.total_artifacts = 1
    if not mcp_file.is_file():
        result.findings.append(Finding(
            "mcp", ".github", "copilot-mcp.json",
            "File missing",
            Severity.MAJOR,
        ))
        result.failed = 1
        result.severity = Severity.MAJOR
        return result
    try:
        data = json.loads(mcp_file.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        result.findings.append(Finding(
            "mcp", ".github", "copilot-mcp.json",
            f"Invalid JSON: {e}",
            Severity.CRITICAL,
        ))
        result.failed = 1
        result.severity = Severity.CRITICAL
        return result
    raw = mcp_file.read_text(encoding="utf-8")
    secret_patterns = [
        r"sk-[a-zA-Z0-9]{20,}",
        r"ghp_[a-zA-Z0-9]{36}",
        r"password\s*[:=]\s*['\"][^'\"]+['\"]",
    ]
    for pattern in secret_patterns:
        if re.search(pattern, raw):
            result.findings.append(Finding(
                "mcp", ".github", "copilot-mcp.json",
                f"Possible hardcoded secret detected (pattern: {pattern})",
                Severity.CRITICAL,
            ))
            result.failed = 1
            result.severity = Severity.CRITICAL
            return result
    result.passed = 1
    result.severity = _max_severity(result.findings)
    return result


def _max_severity(findings: list[Finding]) -> Severity:
    if any(f.severity == Severity.CRITICAL for f in findings):
        return Severity.CRITICAL
    if any(f.severity == Severity.MAJOR for f in findings):
        return Severity.MAJOR
    return Severity.MINOR


def validate(output_dir: Path) -> ValidationReport:
    """Run all validations and produce a report."""
    report = ValidationReport()
    report.results.append(_validate_global_instructions(output_dir))
    report.results.append(_validate_instructions(output_dir))
    report.results.append(_validate_skills(output_dir))
    report.results.append(_validate_agents(output_dir))
    report.results.append(_validate_prompts(output_dir))
    report.results.append(_validate_hooks(output_dir))
    report.results.append(_validate_mcp(output_dir))
    return report


def main() -> int:
    """Entry point for validation script."""
    import argparse

    parser = argparse.ArgumentParser(
        description="Validate generated .github/ structure",
    )
    parser.add_argument(
        "output_dir",
        type=Path,
        help="Path to the generated output directory",
    )
    args = parser.parse_args()
    if not args.output_dir.is_dir():
        print(f"Directory not found: {args.output_dir}")
        return 1
    report = validate(args.output_dir)
    report.print_report()
    return 0 if report.decision == Decision.GO else 1


if __name__ == "__main__":
    sys.exit(main())
