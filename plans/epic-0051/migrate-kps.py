#!/usr/bin/env python3
"""Migration script for EPIC-0051 STORY-0051-0002.

Moves KPs from targets/claude/skills/knowledge-packs/{name}/
to targets/claude/knowledge/{name}.md (simple) or {name}/ (complex),
stripping skill-only frontmatter fields (RULE-051-07).
"""
import re
import shutil
from pathlib import Path

ROOT = Path("/Users/edercnj/workspaces/ia-dev-environment")
SOURCE = ROOT / "java/src/main/resources/targets/claude/skills/knowledge-packs"
TARGET = ROOT / "java/src/main/resources/targets/claude/knowledge"

FORBIDDEN = {"user-invocable", "allowed-tools", "argument-hint", "context-budget"}


def clean_frontmatter(content: str) -> str:
    """Strip forbidden fields from YAML frontmatter."""
    if not content.startswith("---"):
        return content
    end = content.find("---", 3)
    if end < 0:
        return content
    fm = content[3:end]
    rest = content[end:]
    cleaned_lines = []
    skip_continuation = False
    for line in fm.split("\n"):
        stripped = line.strip()
        if not stripped:
            cleaned_lines.append(line)
            skip_continuation = False
            continue
        m = re.match(r"^([a-zA-Z0-9_-]+)\s*:", stripped)
        if m and m.group(1) in FORBIDDEN:
            skip_continuation = True
            continue
        if skip_continuation and (line.startswith(" ") or line.startswith("\t") or stripped.startswith("-")):
            continue
        skip_continuation = False
        cleaned_lines.append(line)
    cleaned_fm = "\n".join(cleaned_lines)
    cleaned_fm = cleaned_fm.rstrip() + "\n"
    return "---" + cleaned_fm + rest


def is_simple_kp(kp_dir: Path) -> bool:
    files = list(kp_dir.iterdir())
    md_files = {f.name for f in files if f.is_file()}
    dirs = [f for f in files if f.is_dir()]
    if dirs:
        return False
    allowed = {"SKILL.md", "README.md"}
    return md_files.issubset(allowed)


def migrate_simple(kp_name: str, kp_dir: Path):
    skill_md = kp_dir / "SKILL.md"
    content = skill_md.read_text(encoding="utf-8")
    content = clean_frontmatter(content)
    dest = TARGET / f"{kp_name}.md"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(content, encoding="utf-8")
    print(f"  simple: {kp_name} → {dest.name}")


def migrate_complex(kp_name: str, kp_dir: Path):
    dest_dir = TARGET / kp_name
    dest_dir.mkdir(parents=True, exist_ok=True)
    for src in kp_dir.rglob("*"):
        if src.is_dir():
            continue
        rel = src.relative_to(kp_dir)
        if rel.name == "README.md" and len(rel.parts) == 1:
            continue
        if rel.name == "SKILL.md" and len(rel.parts) == 1:
            target_rel = Path("index.md")
        else:
            if rel.name == "SKILL.md":
                target_rel = rel.parent / "index.md"
            elif rel.name == "README.md":
                continue
            elif rel.parts[0] == "references" and len(rel.parts) == 2:
                target_rel = Path(rel.name)
            else:
                if "references" in rel.parts:
                    idx = rel.parts.index("references")
                    parts = rel.parts[:idx] + rel.parts[idx+1:]
                    target_rel = Path(*parts)
                else:
                    target_rel = rel
        dest = dest_dir / target_rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        content = src.read_text(encoding="utf-8")
        if dest.name.endswith(".md"):
            content = clean_frontmatter(content)
        dest.write_text(content, encoding="utf-8")
    print(f"  complex: {kp_name}/ → knowledge/{kp_name}/")


def main():
    gitkeep = TARGET / ".gitkeep"
    if gitkeep.exists():
        gitkeep.unlink()
    TARGET.mkdir(parents=True, exist_ok=True)

    kps = sorted([d for d in SOURCE.iterdir() if d.is_dir()])
    print(f"Migrating {len(kps)} KPs:")
    for kp_dir in kps:
        if is_simple_kp(kp_dir):
            migrate_simple(kp_dir.name, kp_dir)
        else:
            migrate_complex(kp_dir.name, kp_dir)

    shutil.rmtree(SOURCE)
    print(f"Removed source: {SOURCE}")


if __name__ == "__main__":
    main()
