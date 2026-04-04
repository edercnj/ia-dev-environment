# Product — ia-dev-environment

> Derived from: README.md, .claude/rules/01-project-identity.md

## Purpose

A CLI tool that generates complete `.claude/`, `.github/`, `.codex/`, and `.agents/` boilerplate for AI-assisted development environments. Produces rules, skills, agents, hooks, settings, and documentation -- everything a Claude Code, GitHub Copilot, or OpenAI Codex project needs to enforce engineering standards from day one.

## How It Works

`ia-dev-env` reads a YAML configuration file describing a project's tech stack (language, framework, database, infrastructure, etc.) and generates a complete set of AI assistant configurations:

- **Claude Code** (`.claude/`) -- rules, skills, agents, hooks, settings
- **GitHub Copilot** (`.github/`) -- instructions, skills, agents, prompts, hooks
- **OpenAI Codex** (`.codex/`, `.agents/`) -- config, agent instructions, shared skills
- **Documentation** -- architecture templates, ADR index, runbook
- **CI/CD** -- Dockerfile, docker-compose, GitHub Actions workflows, Kubernetes manifests

## Target Audience

- Development teams adopting AI-assisted coding tools
- Teams wanting to enforce consistent engineering standards across AI agents
- Projects needing a standardized SDD (Specification-Driven Development) workflow

## Interfaces

- CLI (command-line interface via Picocli)
- Commands: `generate`, `validate`
- Input: YAML configuration files or bundled stack profiles
- Output: Generated file trees for AI assistant platforms

## Source of Truth Hierarchy

1. Epics / PRDs (vision and global rules)
2. ADRs (architectural decisions)
3. Stories / tickets (detailed requirements)
4. Rules (.claude/rules/)
5. Source code
