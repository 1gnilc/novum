# Agent Instruction Files

This repository uses two agent-facing file types: `AGENTS.md` for working and implementation instructions, and `CONTEXT.md` for domain language and boundaries. Tool-specific instruction files are intentionally omitted so rules have one canonical owner.

Keep one pair at the repository root and one pair for each independently managed large module. Current large modules are `apps/admin`, `apps/mobile`, and `apps/server`; their internal feature and Maven subdirectories do not receive additional instruction files.

System, developer, organization, and user-prompt instructions remain outside this repository chain and take precedence when the agent host defines them as higher priority.

## Codex

Codex builds its instruction chain once per run:

1. It reads `AGENTS.override.md` or `AGENTS.md` from the Codex home directory, using the first non-empty file.
2. It walks from the project root to the current working directory.
3. In each directory it selects at most one file: `AGENTS.override.md`, then `AGENTS.md`, then configured fallback names.
4. It concatenates selected files from broadest to nearest; later, nearer instructions override conflicting earlier instructions.

A nested file below the current working directory is not part of that initial chain. When Codex starts at the repository root and later works in a module, this repository's root instruction explicitly requires it to inspect the nearer `AGENTS.md` before editing that subtree.

Official reference: [Custom instructions with AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md).

## Claude Code

Claude Code officially discovers `CLAUDE.md`, not `AGENTS.md`. This repository intentionally does not maintain `CLAUDE.md`, so Claude Code has no repository-native automatic instruction chain here; start it with an explicit instruction to read the applicable root and nested `AGENTS.md` files, or configure that behavior outside the repository.

If tool-specific `CLAUDE.md` files are introduced later, Claude Code loads managed policy and user instructions first, then the project hierarchy from the filesystem root to the working directory; descendant files load lazily. They must not copy rules already owned by `AGENTS.md`.

Official reference: [How Claude remembers your project](https://code.claude.com/docs/en/memory#agentsmd).

## CONTEXT.md And ADRs

Neither Codex nor Claude Code gives `CONTEXT.md` or ADRs built-in precedence. They are read because the applicable `AGENTS.md` or an explicit task instruction tells the agent to do so.

The repository-defined order is:

1. Applicable root and nested `AGENTS.md` instructions.
2. Root [`CONTEXT.md`](../../CONTEXT.md) index.
3. Relevant module `CONTEXT.md` files.
4. Relevant ADRs from the root [`docs/adr/`](../adr/).
5. Task-specific guides such as the testing guide.

Context files describe different bounded vocabularies; a nearer context does not override an unrelated parent context. Read all contexts involved in a cross-context change.

## Nested Example

```text
repo/
├── AGENTS.md                 # shared repository rules
├── CONTEXT.md                # context index and relationships
└── apps/admin/
    ├── AGENTS.md             # Admin-only additions or overrides
    └── CONTEXT.md            # Admin domain vocabulary, read by instruction
```

For work launched in `apps/admin`, Codex orders global -> root -> Admin instructions. After those instructions apply, the repository-defined context order is root index -> Admin context -> relevant ADRs. Other agents must reproduce that sequence explicitly if they do not support `AGENTS.md` discovery.
