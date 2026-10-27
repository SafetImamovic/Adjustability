# Changelog

Changes per version that players, pack devs and mod devs will notice. Format:
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow [Semantic Versioning](https://semver.org).

## [Unreleased]

## [0.6.1] - 2026-10-27

### Added

- Player setting **Exclusive None** (`panel.layout.exclusive_none`): off by default, so active enchantments can be
  switched off individually. Compatible members stay active, while conflicting enchantments still exclude each
  other. Turn it on to use a separate **None** row that switches the whole group off. Both modes follow server costs,
  cooldowns and `allow_none`.

### Changed

- New defaults: Toggle marks, item icon shown, group headers, and server `allow_none = true`. Existing saved settings
  keep their values.
- Switch events expose `deactivatesOnlyTarget()` for individual off actions; `none()` is true only when a
  deactivation leaves the whole target group inactive. The original event constructor remains available.

### Fixed

- An anvil could reject compatible enchantments, such as Unbreaking on boots, after a conflict group was set to None.
  None no longer consumes extra dormant allowance; the anvil preserves the selected groups and still enforces limits
  on conflicting alternatives.
- Grindstones can remove enchantments when every enchantment is dormant or switched off, including dormant-only
  items that have no durability. The server's existing `[grindstone] remove_dormant` setting controls this. Curses
  remain; unregistered enchantments give no experience.

## [0.5.0] - 2026-10-26

### Added

- Switching tutorial: a persistent book-icon reminder when a dormant item reaches your inventory, then a timed Cycle
  reminder after your first successful panel switch. Progress stays on your client across worlds;
  `/adjustability tutorial` restarts it. New player section `[hints]`.
- A magenta corner marks inventory slots holding dormant or switched-off enchantments. Player settings `slot_marker`
  and `slot_marker_color` control it; slot checks are cached until the item's custom data changes.
- Adjust panel styles: the mark in front of each row can be a circle (as before), square, checkbox, toggle switch,
  radio button or none, and animates when an enchantment switches. Every color and size the panel used to hard-code is
  now a setting: title, row and cost-line colors, text shadow, padding, row height, the line between groups or a
  heading with each group's name (like Mining), row order, where the None row goes, and the item's icon next to its
  name. New `[panel.marks]`, `[panel.text]` and `[panel.layout]` sections; older files keep their values.
- Border styles: `GRADIENT` (as before), `SOLID`, `NONE`, or `SPRITE`, a GUI sprite resource packs can replace.
- Panel profiles: `magenta` (the defaults), `vanilla`, `minimal` and `high_contrast` built in, plus JSON files in
  `config/adjustability/panel_profiles` that modpacks ship or `/adjustability panel save <name>` writes. Apply one with
  `/adjustability panel profile <name>` or the YACL screen's buttons. `default_profile` makes `/adjustability panel
  reset` go back to a modpack's look.
- Share codes: `/adjustability panel export` copies a short code for your look, `/adjustability panel import` (from the
  clipboard, or with the code) or YACL's Paste Code applies one. Every value is checked; what doesn't fit is skipped.
- The YACL screen shows the panel's sections as their own groups, picks the row mark from a list, and its preview shows
  two groups.
- Java API: `AdjustabilityClientApi.registerRowMark` lets other mods add row marks (client side, experimental).
- A [panel styles](https://safetimamovic.github.io/Adjustability/guide/panel-styles) docs page with a picture of each.

- Switch off any enchantment that conflicts with nothing (Knockback, Fire Aspect, a lone Silk Touch) and back on: the
  Adjust panel opens on any enchanted item and lists them under **Other enchantments**, one on/off row each, in a
  section that folds. Off stays off through anvils and repairs; the tooltip says `(off)`. Server setting
  `allow_switch_off` (on by default), player setting `singles`. Curses never.
- API: `switchedOffEnchantments`, `flatten(stack, registries)` (turns switched-off ones back on);
  `SwitchResult.SWITCH_OFF_NOT_ALLOWED`. `/adjustability toggle` and `inspect` know switched-off enchantments.

### Changed

- The key hint now says "Left Alt: switch enchantments", with a magenta key and plain gray text directly after the
  enchantment lines. The root advancement's description names your actual Adjust binding.
- `AdjustabilityApi.dormantEnchantments` no longer includes switched-off enchantments; `flatten(stack)` is deprecated.
- `/adjustability flatten` switches switched-off enchantments back on instead of removing them.

### Fixed

- Left Alt didn't close the Adjust panel when the cursor was over an empty slot (the panel often covers one).
- A dormant enchantment whose mod or datapack was removed is kept on the item instead of being dropped by the next
  switch, anvil use or repair; it works again if the enchantment comes back. Ignored until then, listed by
  `/adjustability inspect` and in advanced tooltips (F3+H), removed by the grindstone and flatten.

## [0.4.2] - 2026-10-25

### Fixed

- In creative, the Adjust key did nothing over items on the Survival Inventory tab.
- The Cycle key's name no longer runs over its key button in Controls. It's "Cycle held enchantments" now; Sneak +
  Cycle still picks the next group.

## [0.4.1] - 2026-10-24

### Changed

- Releases are also published on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/adjustability).

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
