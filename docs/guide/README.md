# Adjustability guide

For players, server admins, pack devs and mod devs: how Adjustability lets one item hold conflicting enchantments, and
how to change its rules. It's written next to the code, so it matches the current version. Read it on the
[docs site](https://safetimamovic.github.io/Adjustability/guide/) or here on GitHub.

> **In development.** The 26.3 rewrite hasn't reached its first beta yet. Pages describe the planned behavior from
> [ADJUSTABILITY.md](https://github.com/SafetImamovic/Adjustability/blob/master/ADJUSTABILITY.md) and are filled in as
> each part lands.

| Page | For | What's in it |
|---|---|---|
| [How it works](how-it-works.md) | Players | Active and dormant enchantments, the anvil, switching, grindstones |
| [Where dormant enchantments are stored](storage.md) | Everyone | Exactly where and how they're kept on an item, with a `/give` example |
| [Uninstalling](uninstalling.md) | Server admins, players | What happens to items without the mod, and `/adjustability flatten` |
| [Server config](server-config.md) | Server admins | Every setting in `adjustability-server.toml`, including your own conflict rules and per-group costs |
| [Tags and exclusive sets](tags.md) | Pack devs | Adjustability's tags, and how vanilla decides which enchantments conflict |
| [Modded enchantments](modded-enchantments.md) | Pack devs, mod devs | Why they work without setup, and how to check one |
| [Java API](java-api.md) | Mod devs | Reading and changing active and dormant enchantments from code |
| [The math](math.md) | Everyone curious | Conflict graphs, independent sets, and what a toggle does |
| [Cookbook](cookbook.md) | Everyone | Ready-made rule sets |

## Three ways to change the rules

From no code to full code:

1. **Config** (`config/adjustability-server.toml`): which enchantments conflict (on top of or instead of vanilla's),
   costs per group, cooldowns, limits, which sources (anvil, books) are allowed.
2. **Datapacks**: Adjustability's tags (which enchantments are never dormant, which can't be combined, which items take
   part), and vanilla's own `exclusive_set` in enchantment files, which decides what conflicts in the first place.
3. **Java**: other mods read and change an item's active and dormant enchantments through `AdjustabilityApi`.
