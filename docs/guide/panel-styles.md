# Panel styles

Every part of the Adjust panel's look is a player setting in `config/adjustability-client.toml`: the marks in front of
the rows, the colors, the border, the spacing, and how groups of conflicting enchantments are told apart. Change them in
game in the config screen (with [YACL](https://modrinth.com/mod/yacl) installed, it shows a live preview), or from chat
with `/adjustability panel`. [Player settings](server-config.md#player-settings) lists every key.

The pictures below are taken by the mod's client test, on diamond boots with two groups (the Protections, and Depth
Strider or Frost Walker), the mouse on Fire Protection.

## Presets

A preset sets every look setting at once; your key mode (`activation`) and `side` stay. Apply one with
`/adjustability panel preset <name>`, or with the Apply Preset buttons in the YACL screen (saved when you press Save).
Change anything afterwards as usual; `/adjustability panel reset` goes back to the defaults.

| `magenta` (the defaults) | `vanilla` |
|---|---|
| ![Magenta preset](/screenshots/panel-preset-magenta.png) | ![Vanilla preset](/screenshots/panel-preset-vanilla.png) |
| **`minimal`** | **`high_contrast`** |
| ![Minimal preset](/screenshots/panel-preset-minimal.png) | ![High contrast preset](/screenshots/panel-preset-high_contrast.png) |

## Row marks

`mark_style` in `[panel.marks]` picks the mark in front of each row. Active ones use `active_color`, dormant ones
`dormant_mark_color`. With `animate_marks` on, a mark fades (or a toggle's knob slides) when its enchantment switches.

| `adjustability:circle` (default) | `adjustability:square` | `adjustability:checkbox` |
|---|---|---|
| ![Circle](/screenshots/panel-mark-circle.png) | ![Square](/screenshots/panel-mark-square.png) | ![Checkbox](/screenshots/panel-mark-checkbox.png) |
| **`adjustability:toggle`** | **`adjustability:radio`** | **`adjustability:text_only`** |
| ![Toggle](/screenshots/panel-mark-toggle.png) | ![Radio](/screenshots/panel-mark-radio.png) | ![Text only](/screenshots/panel-mark-text_only.png) |

Other mods can add marks; they show up in the same list. See the [Java API](java-api.md#row-marks-client).

## Groups

`group_style` in `[panel.layout]` sets what goes between groups: `LINE` (the default, a thin line in
`separator_color`), `GAP`, `NONE`, or `HEADER`, a heading with the group's name. Vanilla's groups are named (Mining,
Protection, Boots, Damage, Bow, Crossbow, Trident); a modded group gets the last word of its exclusive-set tag, or its
first enchantment's name. With different costs per group, the cost goes on the heading.

![Group headings](/screenshots/panel-group-header.png)

`row_order` sorts the rows in a group (`REGISTRY`, `ALPHABETICAL` or `ACTIVE_FIRST`), and `none_row` puts the None row
`FIRST` or `LAST` when the server allows switching a group off.

## Border and resource packs

`border_style` in `[panel]` is `GRADIENT` (the default, like a tooltip), `SOLID`, `NONE`, or `SPRITE`. `SPRITE` draws
the `adjustability:panel` GUI sprite instead of colors: a nine-sliced 16 × 16 texture at
`assets/adjustability/textures/gui/sprites/panel.png`. A resource pack can replace it (and its `.mcmeta`) to restyle the
panel; the sprite includes the background, so `background_color` and the border colors don't apply with it. The corner
brackets are separate (`show_brackets`).

![Sprite border](/screenshots/panel-border-sprite.png)

## Text and layout

`[panel.text]` has the title, row and cost-line colors, `text_shadow`, `show_title` and `show_item_icon` (the item in
front of its name). `[panel.layout]` has `padding` and `row_height`.

![Item icon in the title](/screenshots/panel-item-icon.png)
