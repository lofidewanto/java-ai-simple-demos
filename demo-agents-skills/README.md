# demo-agents-skills

A collection of sample **skills** and **agents** for use with [OpenCode](https://opencode.ai).

## What are Skills?

Skills are reusable instruction sets loaded on-demand by OpenCode agents via the `skill` tool.
They are defined as `SKILL.md` files and placed under a named folder, e.g.:

```
.opencode/skills/<name>/SKILL.md
```

Each `SKILL.md` requires YAML frontmatter with at least `name` and `description`.

See the [OpenCode Skills docs](https://opencode.ai/docs/skills/) for full details.

## What are Agents?

Agents are specialized AI assistants configured for specific tasks. They can be:

- **Primary agents** — interact with directly (e.g. `build`, `plan`)
- **Subagents** — invoked automatically or via `@mention` (e.g. `general`, `explore`)

Agents can be defined as markdown files (`.md`) placed under:

```
.opencode/agents/<name>.md
```

Each agent markdown file supports YAML frontmatter for `description`, `mode`, `model`, `tools`, `permissions`, and more.

See the [OpenCode Agents docs](https://opencode.ai/docs/agents/) for full details.

## Structure

Samples in this directory are organized as standalone examples. Each sample includes
its own folder with the necessary files and a short explanation of what it does.

## Usage

Copy any sample into your project's `.opencode/skills/` or `.opencode/agents/` directory
(or the global equivalents at `~/.config/opencode/skills/` and `~/.config/opencode/agents/`).

## Resources

- [OpenCode Docs](https://opencode.ai/docs)
- [Agent Skills](https://opencode.ai/docs/skills/)
- [Agents](https://opencode.ai/docs/agents/)
