# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Rule 14 — Refactoring Guidelines

## Purpose
Refactoring is the disciplined technique of restructuring existing code without changing its external behavior.
These guidelines ensure the REFACTOR phase of the TDD cycle is safe, systematic, and predictable.

## Refactoring Triggers

### When to Extract Method
- Function exceeds **25 lines** (CC-02 Hard Limit)
- Code block has a comment explaining what it does — the comment becomes the method name
- Same code appears **3+ times** (DRY threshold — CC-05)

### When to Extract Class
- Class exceeds **250 lines** (CC-03 Hard Limit)
- A subset of methods uses only a subset of fields (low cohesion — CC-10)
- Class has more than one reason to change (SRP violation)

### When to Inline
- Method is used exactly once AND does not improve readability
- Delegating method adds no value — just forwarding a call
- Temporary variable is assigned once from a simple expression

### When to Rename
- Name does not reveal intent (CC-01)
- Name is misleading or misinformative
- Name requires a comment to explain what it means

## Prioritized Techniques

Ordered by frequency of use in TDD refactoring phase:

| # | Technique | When to Apply |
|---|-----------|---------------|
| 1 | **Extract Method** | Function too long, duplicated code block, or comment-before-block pattern |
| 2 | **Rename Variable/Method/Class** | Name does not reveal intent (CC-01) |
| 3 | **Replace Magic Number with Named Constant** | Literal value with non-obvious meaning (CC-04) |
| 4 | **Extract Interface** | Multiple implementations needed, or to satisfy Dependency Inversion (DIP) |
| 5 | **Move Method** | Method uses more features of another class than its own (SRP) |
| 6 | **Replace Conditional with Polymorphism** | Repeated switch/if-else on type discriminator — only when pattern clearly emerges |

## Safety Rules

These rules are **non-negotiable** during the refactoring phase:

1. **All tests GREEN before starting.** Never refactor on a RED test suite — fix the failing test first.
2. **All tests GREEN after each step.** Run the full test suite after every single refactoring operation.
3. **NEVER add behavior during refactoring.** Refactoring changes structure, not behavior. If you need new behavior, return to the RED phase.
4. **Small, safe steps.** Each refactoring operation must be independently reversible. One extract, one rename, one move — then run tests.
5. **UNDO if any test breaks.** If a test fails after a refactoring step, immediately revert the last change. Do not debug forward — revert and rethink.

## Refactoring Anti-Patterns (FORBIDDEN)

- Refactoring and adding features in the same commit
- Refactoring without a green test suite as starting point
- Large-step refactoring (multiple changes before running tests)
- Speculative refactoring for hypothetical future requirements
- Refactoring code that has no test coverage
