# Security Specialist Review — PR #636 (epic/0055 → develop)

```
ENGINEER: Security
STORY: epic-0055-pr-636
SCORE: 30/30
STATUS: Approved
```

## Summary

PR #636 retrofits `x-review` for Rule 25 compliance and hardens two
bash audit scripts. The change surface is strictly documentation
(SKILL.md, Rule 25 text) plus two audit scripts that operate on
SKILL.md content as data. No runtime security surface is touched —
`HooksAssembler`, `settings.json` generation, and the
`enforce-phase-sequence` hook are unchanged from the PR #633
foundation. No secrets introduced. Injection surface analysed and
found safe.

---

## Focus-Area Findings

### 1. Sed/grep injection risk (audit-task-hierarchy.sh:173-184)

**PASSED.**

The normalization pipeline is:

```bash
subject=$(printf '%s' "$body" | sed -E 's/.*subject:[[:space:]]*"([^"]+)".*/\1/')
normalized=$(printf '%s' "$subject" \
    | sed -E 's/\{[A-Za-z_][A-Za-z0-9_]*\}/task-0000-0000/g')
if ! printf '%s' "$normalized" | grep -qE "$SUBJECT_REGEX"; then
```

- `$subject` and `$normalized` are ALWAYS quoted and piped via
  `printf '%s' "$var"` as stdin data to sed/grep. They are never
  used as a regex pattern, never passed to `eval`, never expanded
  as code.
- The sed replacement pattern `'\{[A-Za-z_][A-Za-z0-9_]*\}'` is a
  hardcoded single-quoted literal — no shell interpolation, no
  user data influences the regex itself.
- The grep pattern is the hardcoded `$SUBJECT_REGEX` constant.
- Empirically verified with hostile inputs:
  `'evil$(touch /tmp/pwned) › Review'`, backtick injections, and
  regex metachar salvos — zero side effects, pattern fails the
  regex cleanly.

No injection risk.

### 2. Regex ReDoS (SUBJECT_REGEX)

**PASSED.**

`SUBJECT_REGEX` is unchanged from the PR #633 foundation. The
anchoring (`^...$`) + bounded repetition (`{0,3}`) + non-overlapping
alternations eliminate catastrophic backtracking. Input length is
also bounded by the SKILL.md `subject: "..."` literal (< 200
chars). `grep -E` uses POSIX ERE (no backreferences), which is
immune to exponential blowup by construction.

### 3. Audit soundness — Copilot #3139317182 regression fix

**PASSED.**

Verified empirically that the normalized path catches the ASCII
`>` regression:

| Input subject | Normalized | Regex |
| :--- | :--- | :--- |
| `story-0055-0006 > Review > QA` | same | **FAIL** (correct) |
| `story-0055-0006 › Review › QA` | same | PASS |
| `{STORY_ID} › Review › QA` | `task-0000-0000 › Review › QA` | PASS |
| `Phase 2 › Implement` | same | PASS |

The previous "skip if placeholder" path would have falsely accepted
`{STORY_ID} > Review > QA` (ASCII `>`) because the check was
bypassed. The new normalization-then-validate keeps the separator
discipline intact for templates, closing the regression window.

### 4. Rule 25 / phase-gate trust model (audit-phase-gates.sh:156)

**PASSED.**

POST regex widened from `post` to `(post|wave|final)`:

```bash
if echo "$body" | grep -qE 'x-internal-phase-gate.*--mode (post|wave|final)'; then
```

This matches the `x-internal-phase-gate` contract (modes
pre/post/wave/final per the skill's SKILL.md). `wave` is
semantically a reinforced POST for parallel dispatch waves;
`final` composes with `x-internal-epic-integrity-gate`. Neither
weakens the security invariant — both perform the same
artifact-existence + task-completion checks as `post` plus extras.
No trust-model regression.

### 5. Secrets / credentials

**PASSED.**

Grep scan of the diff for `password|secret|api_key|token|credential|aws_|private_key|bearer` patterns returned zero hits. Only documentation edits and bash regex changes.

### 6. Runtime security surface

**PASSED.**

`HooksAssembler`, `settings.json`, and `enforce-phase-sequence.sh`
are NOT in the PR diff. The runtime hook-based Rule 24/25
enforcement remains exactly as landed in PR #633. PR #636 is
strictly (a) audit-script polish and (b) SKILL.md content
migration — both operate at CI-time, not runtime.

---

## Scored Breakdown (6 items × 5 pts = 30)

| # | Item | Score |
| :--- | :--- | :---: |
| 1 | Sed/grep injection safety (data-only pipeline) | 5/5 |
| 2 | ReDoS resistance (anchored, bounded, no backrefs) | 5/5 |
| 3 | Audit soundness — ASCII `>` regression caught | 5/5 |
| 4 | Phase-gate trust model (wave/final = reinforced POST) | 5/5 |
| 5 | No secrets / credentials introduced | 5/5 |
| 6 | Runtime surface unchanged from PR #633 | 5/5 |

---

## PASSED

- Injection-safe normalization — `$subject` flows as data, never as code or pattern.
- Regex is unchanged and remains ReDoS-resistant.
- Placeholder normalization restores enforcement for SKILL.md templates (closes Copilot #3139317182 ASCII-`>` regression window).
- POST gate extension to `wave|final` matches the documented `x-internal-phase-gate` 4-mode contract.
- Zero secrets, zero hardcoded credentials introduced by the diff.
- Runtime Rule 24/25 enforcement surface unchanged from the PR #633 foundation.

## FAILED

- None.

## PARTIAL

- None.

---

## Recommendation

**Approve.** PR #636 is a safe, focused retrofit. The audit-script
changes strictly tighten enforcement; the SKILL.md migration is
content-only and follows the canonical Rule 25 patterns already
validated by PR #633. No security blockers.

---

Generated: 2026-04-24 — Security specialist, epic-0055 PR #636
