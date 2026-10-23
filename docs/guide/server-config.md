# Server config

Gameplay rules live in `config/adjustability-server.toml`. The server sends them to every player, so everyone plays
by the same rules. To use different rules for one world, put a copy in that world's `serverconfig/` folder (Fabric) or
`syncedconfig/` folder (NeoForge); it wins over the global file.

On **Minecraft 1.21.1** the file is per world on both loaders: `<world>/serverconfig/adjustability-server.toml`,
made when the world first loads. To start new worlds with your rules, put a copy in `defaultconfigs/`.

Each player's own settings are in `config/adjustability-client.toml` and can be changed in game with
`/adjustability config` (or the mod list on NeoForge, Mod Menu on Fabric): see [Player settings](#player-settings).

Which enchantments and items take part isn't set here: use [tags](tags.md).

Keys may still change before 0.1.0.

## Items

```toml
[items]
mode = "DENYLIST"
```

| Key | Default | What it does |
|---|---|---|
| `mode` | `DENYLIST` | Which items can hold dormant enchantments. `DENYLIST`: every item except those in `#adjustability:not_adjustable`. `ALLOWLIST`: only items in `#adjustability:adjustable` (empty by default), and still not those in `#adjustability:not_adjustable`. |

Enchanted books aren't affected: whether two books combine is `combine_books`. See [Tags](tags.md).

## Which enchantments conflict

```toml
[exclusivity]
use_vanilla = true
extra_groups = []
compatible = []
```

By default Adjustability uses vanilla's conflicts (each enchantment's `exclusive_set`). These settings change that for
Adjustability: what can be combined on one item, and what only one of is active at a time.

| Key | Default | What it does |
|---|---|---|
| `use_vanilla` | `true` | Start from vanilla's conflicts (Fortune / Silk Touch, the Protections, ...). Off: only `extra_groups` conflict. |
| `extra_groups` | `[]` | More groups that conflict, e.g. `["minecraft:sharpness, minecraft:fire_aspect"]`. |
| `compatible` | `[]` | Groups that never conflict here, whatever vanilla or `extra_groups` say, e.g. `["minecraft:infinity, minecraft:mending"]`. With the anvil on, they can then be combined and are **both active**: they stack. |

Each entry is enchantment ids and `#tags`, separated by commas. Two enchantments match an entry when each matches one
of its parts, so a single tag is a whole group: `"#minecraft:exclusive_set/mining"`. Modded enchantments work the same
way.

These settings are Adjustability's only. Vanilla's enchanting table and `/enchant` still follow vanilla's
`exclusive_set`; to change those too, edit `exclusive_set` in a datapack ([Tags](tags.md)).

An item that already holds two enchantments that now conflict (because you changed these settings) keeps both active
until it's next switched or goes through an anvil, which makes one of them dormant. Nothing is deleted.

## Anvil

```toml
[anvil]
enabled = true
combine_books = true
dormant_cost_per_level = 1
allow_too_expensive = false
max_dormant_per_item = 4
max_conflicts_per_component = 0
```

| Key | Default | What it does |
|---|---|---|
| `enabled` | `true` | Conflicting enchantments can be combined in an anvil. Off: vanilla behavior. |
| `combine_books` | `true` | Two books with conflicting enchantments make one book that holds them all. |
| `dormant_cost_per_level` | `1` | Extra anvil levels per level of each enchantment that becomes dormant. |
| `allow_too_expensive` | `false` | Lets the extra cost take the price past the survival limit of 39 levels. |
| `max_dormant_per_item` | `4` | Most dormant enchantments one item can hold; the anvil gives no result past it. `0`: no limit. |
| `max_conflicts_per_component` | `0` | Most enchantments in one group of conflicting ones (e.g. `2` = at most two Protections on one piece); the anvil gives no result past it. `0`: no limit. |

## Switching

```toml
[toggle]
enabled = true
auto_fill = true
cooldown_ticks = 20
cost_type = "NONE"
cost_amount = 1
cost_item = "minecraft:lapis_lazuli"
allow_in_world_cycle = true
allow_command = true
group_costs = []
allow_none = false
panel_activation = "PLAYER"
```

| Key | Default | What it does |
|---|---|---|
| `enabled` | `true` | Players can switch active enchantments. |
| `auto_fill` | `true` | After a switch, dormant enchantments that no longer conflict become active again (the trident case). |
| `cooldown_ticks` | `20` | Time a player waits between switches (20 ticks = 1 second). Not for creative players. |
| `cost_type` | `NONE` | `NONE`, `XP_LEVELS`, `XP_POINTS`, `ITEM` (taken from the inventory) or `DURABILITY` (never breaks the item: a switch that would is refused). Creative players switch for free. |
| `cost_amount` | `1` | How much of `cost_type` one switch costs. |
| `cost_item` | `minecraft:lapis_lazuli` | The item used when `cost_type` is `ITEM`. |
| `allow_in_world_cycle` | `true` | The Cycle key works outside the inventory. |
| `allow_command` | `true` | Players can use `/adjustability toggle`. |
| `group_costs` | `[]` | Own costs for particular groups, instead of `cost_type` / `cost_amount` / `cost_item`. See below. |
| `allow_none` | `false` | A group may have **none** of its enchantments active: the Adjust panel gets a **None** row per group, and the Cycle key steps through none after the last choice. Switching to none costs the same as any switch in that group. Turned back off, items left on none get one of the group active again at their next switch or anvil use. |
| `panel_activation` | `PLAYER` | How the Adjust key works for everyone: `PLAYER` (each player's own `activation` setting), `TOGGLE` or `HOLD`. |
| `play_sound` | `true` | A sound on each switch (`adjustability:item.switch`, an enchanting table sound by default; resource packs can replace it). The player and anyone nearby hear it. |

### Per-group costs

```toml
group_costs = [
    "#minecraft:exclusive_set/mining = ITEM 2 minecraft:lapis_lazuli",
    "minecraft:riptide = XP_LEVELS 5",
    "#minecraft:exclusive_set/armor = NONE",
]
```

Each entry is `<ids and #tags> = <cost>`, where `<cost>` is one of `NONE`, `XP_LEVELS <n>`, `XP_POINTS <n>`,
`DURABILITY <n>` or `ITEM <n> <item id>`. An entry covers a group when any enchantment in the group matches it; the
**first** entry that covers a group wins. Groups no entry covers use `cost_type`, `cost_amount` and `cost_item`.
Creative players switch for free. When an item's groups cost different amounts, the Adjust panel shows each group's
cost next to it.

## Grindstone

```toml
[grindstone]
remove_dormant = true
```

| Key | Default | What it does |
|---|---|---|
| `remove_dormant` | `true` | A grindstone removes dormant enchantments too, and gives experience for them like for active ones. Off: they stay (and give no experience), and become active once nothing active conflicts with them. |

## Player settings

`config/adjustability-client.toml`, each player's own.

```toml
[tooltip]
show_dormant = true
show_hint = true

[panel]
activation = "TOGGLE"
side = "AUTO"
background_color = "#F0080008"
border_top_color = "#C0FF00FF"
border_bottom_color = "#C0A00090"
show_brackets = true
bracket_color = "#FFC71FC0"
bracket_length = 9
highlight_color = "#50FF00FF"
active_color = "#FFFF55FF"
show_cost = true
```

| Key | Default | What it does |
|---|---|---|
| `show_dormant` | `true` | List dormant enchantments in item tooltips. |
| `show_hint` | `true` | On items with dormant enchantments, show which key opens the Adjust panel. |
| `activation` | `TOGGLE` | How the Adjust key works (unless the server sets `panel_activation`). `TOGGLE`: press it to pin the panel, press it again (or Escape, or click outside) to close it. `HOLD`: the panel is open while you hold the key and closes when you let go. |
| `side` | `AUTO` | Where the Adjust panel opens: `AUTO` (right of the item, left if there's no room), `RIGHT` or `LEFT`. It always stays inside the screen. |
| `background_color` | `#F0080008` | The panel's background. |
| `border_top_color` | `#C0FF00FF` | The border at the top; fades into `border_bottom_color` down the sides. |
| `border_bottom_color` | `#C0A00090` | The border at the bottom. |
| `show_brackets` | `true` | The bracket decorations at the corners. |
| `bracket_color` | `#FFC71FC0` | The corner brackets. |
| `bracket_length` | `9` | Length of each bracket arm, 2 to 32 GUI pixels. |
| `highlight_color` | `#50FF00FF` | Behind the row under the mouse. |
| `active_color` | `#FFFF55FF` | The ● in front of active enchantments. |
| `show_cost` | `true` | The cost and cooldown line at the bottom. |

Colors are `#RRGGBB` or `#AARRGGBB`, where `AA` is the opacity (`FF` solid, `00` invisible).

The panel settings can also be changed from chat, and apply right away:

| Command | What it does |
|---|---|
| `/adjustability panel` | Lists the panel settings and their values; click one to change it |
| `/adjustability panel <setting> <value>` | Changes one, e.g. `/adjustability panel activation hold`, `/adjustability panel side left` or `/adjustability panel bracket_color #00E5FF` |
| `/adjustability panel reset` | Puts them all back to the defaults |
