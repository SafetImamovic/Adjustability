---
layout: home

hero:
  name: Adjustability
  text: Every enchantment. One at a time.
  tagline: A Minecraft mod for Fabric and NeoForge. Put Fortune and Silk Touch on the same pickaxe, all four Protections on one chestplate, and switch between them whenever you want.
  image:
    src: /adjustability.png
    alt: A netherite pickaxe
  actions:
    - theme: brand
      text: Download
      link: https://github.com/SafetImamovic/Adjustability/releases
    - theme: alt
      text: Read the guide
      link: /guide/
    - theme: alt
      text: GitHub
      link: https://github.com/SafetImamovic/Adjustability

features:
  - icon: ⇄
    title: Toggle, don't juggle
    details: Combine conflicting enchantments in an anvil. One side of each conflict is active, the other waits on the item. Switch in your inventory or with one key while mining.
  - icon: ⚖️
    title: Never stacks
    details: Fortune and Silk Touch are never active together. The mod saves inventory slots; it doesn't make items stronger than vanilla allows at any moment.
  - icon: 🛠️
    title: Your rules
    details: Server admins and pack devs set the cost of toggling, the anvil price, how many enchantments an item can hold, and which ones may be combined at all.
  - icon: 🧩
    title: Modded enchantments just work
    details: Conflicts come from the game's own enchantment data, so modded enchantments and datapack changes are picked up with no setup.
    link: /guide/modded-enchantments
    linkText: How it works
---

![A netherite pickaxe gets Silk Touch from a book in an anvil and keeps Fortune; switched to Fortune in the Adjust panel it mines diamonds, switched to Silk Touch it mines the ore blocks](/screenshots/showcase.gif)

::: info Beta
0.1.0 is the first beta: everything here works and is tested, and settings may still change before 1.0.
[Changelog](https://github.com/SafetImamovic/Adjustability/blob/master/CHANGELOG.md).
:::

::: tip Safe to remove
Dormant enchantments live in vanilla item data, so removing the mod never breaks an item: it keeps its active
enchantments. [Uninstalling](/guide/uninstalling) explains `/adjustability flatten` and
[where they're stored](/guide/storage).
:::

## Install

Minecraft Java **26.3**, on the client **and** the server.

| Loader | Also install |
|---|---|
| Fabric | [Fabric API](https://modrinth.com/mod/fabric-api), [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) |
| NeoForge (26.3.0.95 or newer) | Nothing |

## Controls

| Action | Default |
|---|---|
| Open the Adjust panel | Hover an item in your inventory, press Left Alt |
| Cycle the held item's enchantments | Unbound (set it under Controls → Adjustability) |

## Commands

| Command | Who | What |
|---|---|---|
| `/adjustability help` | Everyone | Lists the commands |
| `/adjustability config` | Everyone | Opens the settings |
| `/adjustability keybinds` | Everyone | Opens Controls |
| `/adjustability panel [<setting> <value>\|reset]` | Everyone | Shows or changes the Adjust panel's look |
| `/adjustability toggle <enchantment>` | Everyone | Makes an enchantment on the held item active |
| `/adjustability inspect` | Operators | Shows the held item's active and dormant enchantments and how they conflict |
| `/adjustability add <enchantment> [level]` | Operators | Adds an enchantment to the held item, dormant if it conflicts |
| `/adjustability flatten [<targets>]` | Operators | Removes dormant enchantments from items, before uninstalling |
