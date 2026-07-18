# ZenMode Engineering Docs

Public-safe engineering documentation for the ZenMode launcher. This repo is public (GPLv3) — only content suitable for open source belongs here.

## What lives here

- `adr/` — Architecture Decision Records (template: [adr/0000-template.md](adr/0000-template.md))
- Runbooks, schema notes, and other engineering references as they accumulate

## What lives elsewhere

| Location | Content |
|---|---|
| [`zenmode_docs`](https://github.com/ThammanaSrinivas/zenmode_docs) (sibling `../zenmode_docs`) | Public user-facing docs (Docusaurus site) |
| `../zenmode_core_private/docs/plans/` | Feature plans & strategy (private) |
| `../zenmode-brain/docs/` | Company knowledge — BD, legal, meetings (private) |

Sibling repos are cloned next to this one (same parent directory). If one is missing, run `../zenmode-brain/setup.ps1`.

**Rules**
- Business-sensitive content (strategy, legal notes, unreleased feature plans) never goes in this repo.
- No duplication: link to the published Docusaurus pages instead of copying their content here.
