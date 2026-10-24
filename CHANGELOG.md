# Changelog

Changes per version that players, pack devs and mod devs will notice. Format:
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow [Semantic Versioning](https://semver.org).

## [Unreleased]

## [0.4.0] - 2026-10-24

### Added

- With [YACL](https://modrinth.com/mod/yacl) installed (optional), the settings screen is a YACL one, with every
  player and server setting. Server settings can only be changed in your own world. Without YACL, the screen stays as
  before.
- In the YACL screen, the Adjust panel's colors have a color picker (with opacity), and every panel setting shows a
  live preview: the panel next to a pickaxe in a slot, with the values you're changing, before you save.

## [0.3.0] - 2026-10-23

### Added

- Minecraft 1.21.1, on Fabric and NeoForge (21.1.1 or newer), with the same features as 26.3. Its server config is
  per world, in `serverconfig/`.

### Fixed

- In the creative inventory, clicks on the Adjust panel did nothing (the switch went to the wrong slot) or changed the
  creative tab under it. The panel now works there on your own items: the hotbar, and the inventory tab.

## [0.2.0] - 2026-10-23

### Added

- A sound on each switch (`adjustability:item.switch`, replaceable by resource packs; `[toggle] play_sound`).
- An advancement tab: Adjustability, Change of Plans, Best of Both, Every Angle Covered, Indecisive (100 switches) and
  the hidden Neither. Switches are counted in Statistics. Packs can make their own with the `adjustability:switched`
  trigger.
- JEI info pages on the anvils and the grindstone.
- Names for every setting in the in-game config screen.
- Docs: a reference of every setting and a table of vanilla's conflicts, both generated from the code.

### Changed

- Dormant enchantments are listed in the tooltip right under the active enchantment they conflict with, instead of
  after all of them.

## [0.1.0] - 2026-10-22

First beta, for Minecraft 26.3 on Fabric and NeoForge (26.3.0.95 or newer). A complete rewrite of the 2024 Forge
1.20.6 prototype, which was never released (kept under the `legacy-1.20.6-forge` tag). Now MIT licensed.

### Players

- Put conflicting enchantments on one item in an anvil, from a book or another item: what was active stays active,
  the new conflicting enchantment is kept **dormant** (listed in the tooltip, doing nothing). Two books with
  conflicting enchantments combine into one book that holds them all.
- **Adjust panel:** hover an item in any inventory and press Left Alt; click the enchantment you want active. Hold or
  toggle the key, your choice (`/adjustability panel activation hold`), and restyle the panel's colors, side and
  corner brackets (`/adjustability panel`).
- **Cycle key** (unbound by default): steps the held item through its choices; Sneak + Cycle picks which group
  (boots: Protections, or Depth Strider / Frost Walker). Tridents bring Channeling back with Loyalty.
- Grindstones remove dormant enchantments too, and give their experience.
- Items that somehow have two conflicting enchantments active are fixed by their first switch; nothing is lost.
- Removing the mod never breaks an item; `/adjustability flatten` removes dormant enchantments for good.

### Server admins

- `adjustability-server.toml`:
  - `[exclusivity]`: your own conflict groups, groups that never conflict, or vanilla's conflicts off (ids and
    `#tags`);
  - `[items]`: denylist or allowlist mode for which items take part;
  - `[anvil]`: on/off, book combining, dormant cost, limits;
  - `[toggle]`: cooldown, cost (free, XP levels or points, an item, durability) and per-group costs (`group_costs`),
    `allow_none` (a group may have nothing active), the Adjust key's mode for everyone, Cycle key and command on/off;
  - `[grindstone]`.
- Commands: `/adjustability toggle` (everyone), `inspect`, `add` and `flatten` (operators); `help`, `config`,
  `keybinds`, `panel` on the client.

### Pack devs

- Tags `#adjustability:never_dormant` (curses), `not_combinable`, `not_adjustable` and `adjustable`. Conflicts follow
  each enchantment's `exclusive_set`, so modded enchantments and datapack changes work without setup.

### Mod devs

- Java API (experimental) in `io.github.safetimamovic.adjustability.api`: read and change active and dormant
  enchantments, switch or choose none from code, listeners that can refuse switches and anvil results, and
  `AdjustableItem` for item-specific rules. Each release has an `adjustability-api` jar to build against.
