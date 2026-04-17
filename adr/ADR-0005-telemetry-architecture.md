---
status: Accepted
date: 2026-04-17
deciders:
  - Platform Team
  - Security / Platform Team (Rule 20)
story-ref: "story-0040-0012"
---

# ADR-0005: Telemetry Architecture for Skill Execution Visibility (EPIC-0040)

## Status

Accepted | 2026-04-17

> Numbering note: the originating story (`story-0040-0012`) references
> `ADR-0004-telemetry-architecture.md`. ADR-0004 was already allocated to
> ADR-0004 (Worktree-First Branch Creation Policy, EPIC-0037, 2026-04-13)
> before EPIC-0040 merged its docs story. This ADR therefore lands at
> slot 0005 to avoid a numbering collision; the intent and content are
> unchanged from the story specification.

## Context

Before EPIC-0040, the ia-dev-env skill catalog had grown past 70 skills and
several orchestrators (e.g., `x-epic-implement`, `x-story-implement`,
`x-task-implement`, `x-story-plan`) now dispatch parallel subagents and
multi-phase flows. Operators had no structured way to answer three
operational questions:

1. **Which phase or skill is the bottleneck?** — long-running releases and
   epics produced only line-oriented logs; there was no per-phase duration,
   no per-skill aggregate, and no cross-session timeline.
2. **Is skill X getting slower over the last N epics?** — reasoning about
   regression required manual scraping of `plans/epic-*/` directories.
3. **Did the subagent I just spawned actually finish, and how long did it
   take?** — subagent boundaries were implicit; lifecycle events (start /
   end, success / failure) were not captured anywhere machine-readable.

Ad-hoc instrumentation attempts across individual skills produced
incompatible formats (some logging to stdout, some writing sidecar JSON
under the epic directory, some not instrumented at all). Dashboards built
on top were necessarily one-off scripts.

A unified telemetry architecture was needed that:

- captures data with **zero operator action** (enabled by default, opt-out
  only via `CLAUDE_TELEMETRY_DISABLED=1` or `ProjectConfig.telemetryEnabled`);
- **never aborts a running skill** if the telemetry pipeline fails (fail-open);
- produces a format that is **safe to commit** to a public repository (no
  PII, no secrets);
- supports both **point-in-time analysis** (`/x-telemetry-analyze`) and
  **trend detection across epics** (`/x-telemetry-trend`);
- adds **minimal token/time overhead** to each skill invocation.

## Decision

Adopt a hybrid telemetry capture architecture that combines four cooperating
layers:

### D1. Hook-based automatic capture (outside-in)

Six Bash hook scripts shipped under
`java/src/main/resources/targets/claude/hooks/` (source of truth) are copied
to `.claude/hooks/` by `HooksAssembler` and registered in `settings.json`
by `SettingsAssembler`. They cover the Claude Code lifecycle without any
per-skill code change:

| Hook event    | Script                    | Event emitted     |
| :-----------  | :------------------------ | :---------------- |
| `SessionStart`| `telemetry-session.sh`    | `session.start`   |
| `PreToolUse`  | `telemetry-pretool.sh`    | (start file only) |
| `PostToolUse` | `telemetry-posttool.sh`   | `tool.call`       |
| `SubagentStop`| `telemetry-subagent.sh`   | `subagent.end`    |
| `Stop`        | `telemetry-stop.sh`       | `session.end`     |
| helper        | `telemetry-emit.sh`       | — (append/scrub)  |

All scripts use `set +e` (fail-open per RULE-004), enforce a 5 s stdin
timeout when `timeout(1)` is available, and append to NDJSON via `flock(1)`
or an `mkdir`-based advisory lock. Full design recorded in
`java/src/main/resources/targets/claude/hooks/TELEMETRY-README.md`.

### D2. In-skill phase markers (inside-out)

Canonical phase wrappers (`telemetry-phase.sh start|end`,
`telemetry-subagent.sh start|end`, `telemetry-mcp.sh start|end`) are
invoked at the top and bottom of each numbered phase / subagent / MCP call
inside instrumented skills. Markers produce `phase.start` / `phase.end`
events with the same context-resolution rules as the hooks (so events merge
cleanly in the NDJSON stream). The authoring template
`_TEMPLATE-SKILL.md` gained a dedicated "Telemetry (Optional)" section
(story-0040-0009) documenting the contract. Implementation, planning, and
creation skills were instrumented in stories 0040-0006, 0040-0007, and
0040-0008.

### D3. Java domain types (library)

`dev.iadev.telemetry` (story-0040-0002) provides:

- `TelemetryEvent` — immutable record shaped after the NDJSON schema
  published by story-0040-0001 (`_TEMPLATE-TELEMETRY-EVENT.json`).
- `TelemetryWriter` — append-only NDJSON writer with the same scrub /
  lock / fail-open semantics as the shell layer.
- `TelemetryReader` — streaming NDJSON parser tolerant of partial lines
  and malformed events (`UncheckedIOException` never propagates).
- `TelemetryScrubber` — PII / secret scrubber used by both `/x-telemetry-*`
  skills and the Java writer (story-0040-0005). Pattern catalog mirrors
  Rule 20 (§3) and extends the shell-layer regex subset.

These types are language-native and domain-pure (zero framework imports),
consistent with Rule 04 (Architecture Summary) and the cloud-agnostic
constraint in Rule 01.

### D4. Storage layout

```
plans/epic-XXXX/telemetry/events.ndjson    # per-epic, committed
plans/unknown/telemetry/events.ndjson      # fallback when context cannot be resolved
.claude/telemetry/index.json               # cross-epic index (gitignored, regenerated)
```

The per-epic NDJSON lives next to the story / plan artifacts it describes,
so `git bisect` and pull-request review surface telemetry changes with the
code they measure. The `.claude/telemetry/index.json` file is explicitly
excluded from version control (`.gitignore`) because it is a rebuildable
cache populated by `/x-telemetry-trend` on demand.

### D5. Analysis surface

Two single-responsibility skills consume the NDJSON:

- `/x-telemetry-analyze` — point-in-time report for one or more epics.
  Aggregates by skill / phase / tool, renders a Mermaid Gantt timeline,
  and supports JSON / CSV export for downstream dashboards.
- `/x-telemetry-trend` — cross-epic P95 regression detector. Ranks the
  top-10 slowest skills and flags deltas above a configurable threshold
  (default 20%).

### D6. Privacy

Rule 20 (`rules/20-telemetry-privacy.md`) is the normative contract: every
value written to `events.ndjson` MUST have passed through
`TelemetryScrubber` (or the equivalent shell regex chain in
`telemetry-emit.sh`). Committed NDJSON is, by policy, safe to republish.

### D7. Opt-out

- Global, per-session: `CLAUDE_TELEMETRY_DISABLED=1` in the shell env.
- Global, per-project (persisted): `ProjectConfig.telemetryEnabled = false`
  in the generator YAML; `SettingsAssembler` then omits the hook
  registration (regeneration required).

## Consequences

### Positive

- **Zero-touch adoption.** Existing users running `ia-dev-env generate` on
  3.8.0 receive telemetry capture automatically; no migration step is
  required. Hooks install themselves through the standard `settings.json`
  flow (story-0040-0004).
- **Uniform data format.** NDJSON with a single documented schema replaces
  heterogeneous per-skill logging. `/x-telemetry-analyze` and `-trend`
  operate on every new epic without per-skill adapters.
- **Privacy-first by construction.** Rule 20 + `TelemetryScrubber` are on
  the write path, not an afterthought. Regressions are caught by
  scrubber unit tests before the event touches disk.
- **Trend-capable.** `/x-telemetry-trend` answers "is this getting slower?"
  questions against committed historical data, enabling evidence-based
  performance conversations.
- **Fail-open.** Every layer (shell hooks, markers, Java writer) degrades
  to a no-op on failure. Skill execution is never blocked by telemetry.

### Negative

- **Storage growth.** Per-epic NDJSON files can reach multi-MB for long
  epics. Mitigation: files live in the epic directory and age out naturally
  with the epic; operators can `git rm` old epics without affecting tooling.
- **Shell dependency.** The hook layer requires `bash >= 4` and `jq` on
  `PATH`. Windows developers without a POSIX shell lose automatic capture;
  the Java domain still records events from instrumented skills.
- **Schema drift risk.** Adding a field to `TelemetryEvent` requires
  coordinating the Java record, the shell `build_event`, the
  `_TEMPLATE-TELEMETRY-EVENT.json` reference, the scrubber allow-list, and
  both analysis skills. Mitigated by the golden-file harness and the
  `/x-telemetry-analyze` smoke tests.
- **Learning curve for skill authors.** The "Telemetry (Optional)" section
  in `_TEMPLATE-SKILL.md` introduces new markers that new contributors must
  learn. Mitigated by copy-pastable snippets and the canonical example in
  `x-story-implement`.

### Neutral

- **Cloud-agnostic.** The architecture writes NDJSON to the repository;
  no cloud service, no SaaS dependency. Teams that want a hosted
  dashboard can forward the NDJSON on their own terms.
- **Reversible.** `ProjectConfig.telemetryEnabled = false` plus
  `git rm -r plans/*/telemetry/` fully removes the feature footprint
  from a project.

## Alternatives Considered

### A1. OpenTelemetry SDK (rejected)

Embedding the OTel Java SDK and emitting OTLP would have given us a mature
standard format and plug-in exporters. Rejected because:

- Adds a heavyweight runtime dependency to every CLI invocation.
- Requires a collector (local or remote) to produce human-readable
  output; violates the cloud-agnostic constraint (Rule 01).
- Shell-layer hooks would still need a separate emitter (OTLP-in-bash is
  impractical), producing exactly the heterogeneity we wanted to avoid.

### A2. Hooks-only capture (rejected)

Relying solely on `SessionStart` / `PreToolUse` / `PostToolUse` / `Stop`
would have required no in-skill changes. Rejected because the hook layer
cannot see phase boundaries inside a single-skill run — the most
operationally useful signal ("which numbered phase took 4 minutes?") would
be invisible.

### A3. In-skill markers only (rejected)

The mirror of A2: instrument every skill, no hooks. Rejected because it
leaves uninstrumented skills invisible (several knowledge-pack skills and
third-party additions would never report anything), and because subagent
lifecycle events cannot be captured from inside a skill that has already
returned.

### A4. Per-skill sidecar JSON (rejected)

Each instrumented skill writes its own `<skill>.metrics.json` next to the
artifacts it produces. Rejected because cross-skill aggregation becomes a
schema-merge problem and trend detection would require reading N files per
epic; NDJSON is strictly simpler.

## Related ADRs

- ADR-0001 — Intentional Architectural Deviations for CLI Tool
  (cloud-agnostic constraint, Rule 01).
- ADR-0003 — Skill Taxonomy and Naming (enables `/x-telemetry-analyze`
  and `/x-telemetry-trend` to live in the `ops/` category).
- ADR-0004 — Worktree-First Branch Creation Policy (interacts with the
  context resolver in `telemetry-lib.sh`, which reads the current branch
  name to populate `epicId` / `storyId` / `taskId`).

## Rollback

Telemetry can be fully disabled at three layers in order of decreasing
invasiveness:

1. Per-session: export `CLAUDE_TELEMETRY_DISABLED=1` before running any
   skill. Hooks short-circuit before any I/O.
2. Per-project: set `telemetryEnabled: false` in the generator config and
   regenerate. `SettingsAssembler` emits a `settings.json` without the
   telemetry hook registrations.
3. Permanent: revert the EPIC-0040 merge commits. The shell scripts, Java
   package, Rule 20, and the two analysis skills are removed atomically;
   no schema migrations are required because all artifacts live under
   `plans/epic-*/telemetry/` and `.claude/`.

## Story Reference

- EPIC-0040 — Telemetria de Execução de Skills
- Stories 0040-0001 through 0040-0011 (phased delivery: schema, Java
  domain, shell hooks, settings wiring, privacy rule, phase markers in
  implementation / planning / creation skills, template section,
  `/x-telemetry-analyze`, `/x-telemetry-trend`).
- Skills shipped: `x-telemetry-analyze`, `x-telemetry-trend`.
- Rule introduced: Rule 20 — Telemetry Privacy.
