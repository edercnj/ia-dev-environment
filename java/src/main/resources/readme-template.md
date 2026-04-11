# .claude/ -- Usage Guide

This directory contains all Claude Code configuration for the **{{PROJECT_NAME}}** project.
It includes coding rules, skills (slash commands), knowledge packs, agents, and hooks.

> **Note:** The `.claude/` directory is a **generated output** produced by `ia-dev-env`.
> Do not edit it manually -- regenerate instead.

> The `CLAUDE.md` file at the project root provides an executive summary loaded automatically in EVERY conversation.

## Structure

```
CLAUDE.md                   <-- Executive summary (project root, loaded automatically)
.claude/
|-- README.md               <-- You are here
|-- settings.json           <-- Shared settings (committed to git)
|-- settings.local.json     <-- Local overrides (gitignored)
|-- hooks/                  <-- Automations (post-compile, etc.)
|-- rules/                  <-- Project rules (loaded into system prompt)
|-- skills/                 <-- Skills invocable via /command
|   +-- {knowledge-packs}/  <-- Knowledge packs (not invocable, referenced internally)
+-- agents/                 <-- AI personas (used by skills and lifecycle)
```

## Platform Selection

The generator currently produces Claude Code artifacts only. Support for `copilot`, `codex`, and the generic `agents` target was removed (see EPIC-0034). Legacy `--platform` values are rejected by the CLI.

| Value | Description | Directories Generated |
|-------|-------------|-----------------------|
| `claude-code` | Anthropic Claude Code (only accepted value) | `.claude/` + docs |

### CLI Examples

```bash
# Generate Claude Code artifacts (default behavior)
ia-dev-env generate --platform claude-code --config my-config.yaml

# Platform flag can be omitted — claude-code is the only supported target
ia-dev-env generate --config my-config.yaml
```

### YAML Configuration

You can also specify the platform in your YAML config file:

```yaml
platform: claude-code
```

### Default Behavior

When no `--platform` flag is provided and no `platform:` key exists in the YAML config, the generator produces artifacts for `claude-code` (the only supported target). Any legacy value (`copilot`, `codex`, `agents`, `all`) is rejected with a clear error message.

### settings.json vs settings.local.json

- **`settings.json`**: Team settings (permissions, hooks). Committed to git.
- **`settings.local.json`**: Local overrides. In `.gitignore`. Overrides `settings.json`.

---

## Rules

Rules are loaded automatically into the system prompt of EVERY conversation.
They define mandatory standards that Claude MUST follow when generating code.

{{RULES_TABLE}}

**Total: {{RULES_COUNT}} rules**

### Numbering

- Gaps in numbering allow future insertion without renumbering existing rules.

---

## Skills (Slash Commands)

Skills are invoked by the user via `/name` in chat. They are lazy-loaded (only load when invoked).

{{SKILLS_TABLE}}

**Total: {{SKILLS_COUNT}} skills**

### Usage Examples

```bash
# Run a specific skill
/skill-name argument

# Get help on available skills
# Type / in the chat to see the full list
```

---

## Knowledge Packs (Internal Context)

Knowledge Packs do NOT appear in the `/` menu. They are referenced internally by agents and skills
to inject domain knowledge. Configured with `user-invocable: false`.

{{KNOWLEDGE_PACKS_TABLE}}

---

## Agents (AI Personas)

Agents are system prompts that define specialized personas. They are not invoked directly --
they are used by skills (via Task tool) to delegate work to agents with specific expertise.

{{AGENTS_TABLE}}

**Total: {{AGENTS_COUNT}} agents**

---

## Hooks (Automations)

Hooks are scripts executed automatically in response to Claude Code events.
Configured in `settings.json` under the `hooks` key.

{{HOOKS_SECTION}}

---

## Settings

{{SETTINGS_SECTION}}

---

## Artifact Conventions

| Artifact | Extension | Naming | Frontmatter |
|----------|-----------|--------|-------------|
| Rules | `.md` | `NN-name.md` (numbered) | None |
| Skills | `SKILL.md` | `skills/{name}/SKILL.md` | YAML (name, description) |
| Agents | `.md` | `{name}.md` | None |
| Hooks | `.sh` / `.json` | Event-based naming | N/A |

---

## Tips

- **Rules are always active** -- no need to invoke them, Claude already knows them.
- **Skills are lazy** -- they only load when you type `/name`.
- **Knowledge Packs do not appear in the `/` menu** -- they are internal context for agents.
- **Agents are not invoked directly** -- they are used by skills internally.
- **Hooks run automatically** -- compilation after editing source files detects errors early.
- **To create a new skill**: create `.claude/skills/{name}/SKILL.md` and it appears automatically.
- **To create a new rule**: add a `.md` file in `.claude/rules/` with the appropriate numbering.
- **Both directories are generated** -- run `ia-dev-env generate` to regenerate.

---

## Generation Summary

{{GENERATION_SUMMARY}}
