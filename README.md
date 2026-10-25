<p align="center">
  <img src="common/src/main/resources/adjustability.png" alt="Adjustability icon: a netherite pickaxe" width="160">
</p>

<h1 align="center">Adjustability</h1>

<p align="center">
  Put Fortune and Silk Touch on the same pickaxe, and switch between them. Items hold mutually exclusive enchantments;
  one side of each conflict is active at a time, never both.
</p>

<p align="center">
  <a href="https://github.com/SafetImamovic/Adjustability/actions/workflows/release.yml"><img src="https://github.com/SafetImamovic/Adjustability/actions/workflows/release.yml/badge.svg" alt="Release"></a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/adjustability"><img src="https://img.shields.io/curseforge/dt/1763607?logo=curseforge&label=CurseForge&color=f16436" alt="CurseForge downloads"></a>
  <a href="https://github.com/SafetImamovic/Adjustability/releases"><img src="https://img.shields.io/github/v/release/SafetImamovic/Adjustability?include_prereleases&sort=semver&label=release" alt="Latest release"></a>
  <a href="https://safetimamovic.github.io/Adjustability/"><img src="https://img.shields.io/badge/docs-online-c71fc0" alt="Docs"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue" alt="MIT license"></a>
  <a href="https://ko-fi.com/safetimamovic"><img src="https://img.shields.io/badge/Ko--fi-support-ff5e5b?logo=kofi&logoColor=white" alt="Support on Ko-fi"></a>
</p>

<p align="center">
  <a href="https://www.curseforge.com/minecraft/mc-mods/adjustability"><b>Download on CurseForge</b></a> ·
  <a href="https://github.com/SafetImamovic/Adjustability/releases"><b>GitHub releases</b></a> ·
  <a href="https://safetimamovic.github.io/Adjustability/"><b>Docs</b></a> ·
  <a href="CHANGELOG.md"><b>Changelog</b></a> ·
  <a href="https://github.com/SafetImamovic/Adjustability/issues/new/choose"><b>Report a bug</b></a> ·
  <a href="https://ko-fi.com/safetimamovic"><b>Support on Ko-fi</b></a>
</p>

Minecraft Java **1.21.1** and **26.3** · **Fabric** + **NeoForge** · MIT · **Beta**

<p align="center">
  <img src="docs/public/screenshots/showcase.gif" alt="A netherite pickaxe gets Silk Touch from a book in an anvil and keeps Fortune; switched to Fortune in the Adjust panel it mines diamonds, switched to Silk Touch it mines the ore blocks" width="640">
</p>

## Requirements

Install on both the client and the server. Optional mods are client only.

| | Minecraft 1.21.1 | Minecraft 26.3 |
|---|---|---|
| **Fabric, required** | [Fabric Loader](https://fabricmc.net/use/) 0.16.0+<br>[Fabric API](https://modrinth.com/mod/fabric-api) 0.102.0+<br>[Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) 21.1.0+ | [Fabric Loader](https://fabricmc.net/use/) 0.19.5+<br>[Fabric API](https://modrinth.com/mod/fabric-api)<br>[Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) 26.3.1+ |
| **NeoForge, required** | [NeoForge](https://neoforged.net/) 21.1.1+ | [NeoForge](https://neoforged.net/) 26.3.0.95+ |
| **Optional** (both loaders) | [JEI](https://modrinth.com/mod/jei): info pages on the anvils and the grindstone<br>[YACL](https://modrinth.com/mod/yacl) 3.8.2+: a nicer settings screen | [JEI](https://modrinth.com/mod/jei): info pages on the anvils and the grindstone<br>[YACL](https://modrinth.com/mod/yacl) 3.9.7+: a nicer settings screen |

### Tested with

On Minecraft 26.3, with the full automated test suite (details:
[compatibility](https://safetimamovic.github.io/Adjustability/guide/compatibility)):

| Mod | Version | Loader | Result |
|---|---|---|---|
| [Easy Anvils](https://modrinth.com/mod/easy-anvils) | 26.3.1 | Fabric, NeoForge | Works |
| [JEI](https://modrinth.com/mod/jei) | 31.16.0.89 | Fabric, NeoForge | Works: the Adjust panel draws above JEI's item list |
| [Mouse Tweaks](https://modrinth.com/mod/mouse-tweaks) | 26.3-2.31 | Fabric | Works |
| [Enchantment Descriptions](https://modrinth.com/mod/enchantment-descriptions) | 26.3.0.2 | Fabric | Works |
| [AppleSkin](https://modrinth.com/mod/appleskin) | 3.0.10 | Fabric | Works |
| [Enchantment Switch](https://modrinth.com/mod/enchantment-switch) | 26.3.0 | Fabric | Works; its key is unbound by default, so Left Alt stays with Adjustability |
| [YACL](https://modrinth.com/mod/yacl) | 3.9.7 | Fabric, NeoForge | Works: the settings screen is YACL's. On NeoForge, a warning about YACL's `logoFile` shows at startup; that's YACL's own metadata ([fix pending](https://github.com/isXander/YetAnotherConfigLib/pull/359)), click Proceed |

Not tested with other mods on 1.21.1 yet. Found a mod that doesn't work with it? Use the
[compat issue form](https://github.com/SafetImamovic/Adjustability/issues/new/choose).

The 2024 prototype for Forge 1.20.6 is kept under the
[`legacy-1.20.6-forge`](https://github.com/SafetImamovic/Adjustability/tree/legacy-1.20.6-forge) tag.

Design, math, API and roadmap: [ADJUSTABILITY.md](ADJUSTABILITY.md).

## What it does

- **Combine:** put conflicting enchantments on one item in an anvil, from books or another item. Two books with
  conflicting enchantments make one book that holds them all.
- **Active and dormant:** one side of each conflict is active and works like vanilla; the others are dormant, shown in
  the tooltip, doing nothing. Fortune and Silk Touch never work at the same time.
- **Switch:** in your inventory (hover, Left Alt) or with one key while playing. Tridents do the right thing:
  switching to Loyalty brings Channeling back with it.
- **Your rules:** cost and cooldown of switching, anvil price, how many enchantments an item can hold, and which ones
  may be combined at all.
- **Modded enchantments just work:** conflicts come from the game's own enchantment data, so modded enchantments and
  datapack changes need no setup.

## Controls

All rebindable under Controls → Adjustability, or with `/adjustability keybinds`.

| Action | Default |
|---|---|
| Open the Adjust panel | Left Alt, while hovering an item in your inventory |
| Cycle the held item's enchantments | Unbound (set it under Controls → Adjustability) |

## Commands

| Command | Who | What |
|---|---|---|
| `/adjustability help` | Everyone | Lists these commands |
| `/adjustability config` | Everyone | Opens the settings screen |
| `/adjustability keybinds` | Everyone | Opens Controls to rebind the keys |
| `/adjustability panel [<setting> <value>\|reset]` | Everyone | Shows or changes the Adjust panel's look |
| `/adjustability toggle <enchantment>` | Everyone | Makes an enchantment on the held item active |
| `/adjustability inspect` | Operators | Shows the held item's active and dormant enchantments and how they conflict |
| `/adjustability add <enchantment> [level]` | Operators | Adds an enchantment to the held item, dormant if it conflicts |
| `/adjustability flatten [<targets>]` | Operators | Removes dormant enchantments from items, before uninstalling |

## Configuration

- `config/adjustability-server.toml`: gameplay, synced from the server (costs, cooldowns, limits).
- `config/adjustability-client.toml`: personal (tooltip hint, sounds), editable in game.
- Your own conflict rules (`[exclusivity]`: extra groups, pairs that never conflict, vanilla's on or off) and costs
  per group (`group_costs`), in the server file.
- Datapacks: Adjustability's tags, and vanilla's `exclusive_set` in enchantment files. Reload with `/reload`.

Guide for admins, pack devs and mod devs: [safetimamovic.github.io/Adjustability](https://safetimamovic.github.io/Adjustability/)
(source in [docs/guide](docs/guide/README.md)).

## Uninstalling

Removing Adjustability never breaks an item: dormant enchantments are stored in vanilla's `minecraft:custom_data` under
`adjustability:dormant`, so items keep their active enchantments and load fine without the mod. To remove the dormant
ones for good, run `/adjustability flatten @a` with everyone online first. Details:
[Uninstalling](https://safetimamovic.github.io/Adjustability/guide/uninstalling) and
[where dormant enchantments are stored](https://safetimamovic.github.io/Adjustability/guide/storage).

## Issues and ideas

- Bugs and crashes: [open a bug report](https://github.com/SafetImamovic/Adjustability/issues/new/choose) with your
  `latest.log` uploaded to [mclo.gs](https://mclo.gs).
- A modded enchantment that misbehaves: use the enchantment compat form.
- Changes per version: [CHANGELOG.md](CHANGELOG.md).

## Building

Requires JDK 25.

```sh
./gradlew build                                # jars in {fabric,neoforge}/versions/<minecraft version>/build/libs
./gradlew :graph:test                          # unit tests
./gradlew :fabric:26.3:runClient               # dev client (Fabric)
./gradlew :neoforge:26.3:runClient             # dev client (NeoForge); :neoforge:26.3:runClient2 for a second one
./gradlew :fabric:26.3:runServer               # dedicated server
./gradlew :fabric:26.3:runClient --debug-jvm   # wait for a debugger on port 5005
```

## Layout

| Module | Contents |
|---|---|
| `graph/` | Pure-Java conflict graph logic (independent sets, toggling). No Minecraft imports; unit tested. |
| `common/` | Almost all code. Compiles against vanilla only. Public API in `api/`. |
| `fabric/` | Fabric entrypoint and Fabric-specific glue. |
| `neoforge/` | NeoForge entrypoint and NeoForge-specific glue. |
| `build-logic/` | Shared Gradle conventions. |

Loader-specific calls from `common` go through `platform/Services` (Java `ServiceLoader`).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md): short branches off `master`, atomic Conventional Commits, PRs against `master`.

Based on [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template).
