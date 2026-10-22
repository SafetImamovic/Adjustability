# Server config

Gameplay rules live in `config/adjustability-server.toml`. The server sends them to every player, so everyone plays
by the same rules. To use different rules for one world, put a copy in that world's `serverconfig/` folder (Fabric) or
`syncedconfig/` folder (NeoForge); it wins over the global file.

Each player's own settings are in `config/adjustability-client.toml` and can be changed in game with
`/adjustability config` (or the mod list on NeoForge, Mod Menu on Fabric): see [Player settings](#player-settings).

Which enchantments and items take part isn't set here: use [tags](tags.md).

Keys may still change before 0.1.0.

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

## Grindstone

```toml
[grindstone]
remove_dormant = true
```

| Key | Default | What it does |
|---|---|---|
| `remove_dormant` | `true` | A grindstone removes dormant enchantments too. Off: they stay, and become active once nothing active conflicts with them. Experience only comes from active enchantments either way. |

## Player settings

`config/adjustability-client.toml`, each player's own.

```toml
[tooltip]
show_dormant = true
show_hint = true
```

| Key | Default | What it does |
|---|---|---|
| `show_dormant` | `true` | List dormant enchantments in item tooltips. |
| `show_hint` | `true` | On items with dormant enchantments, show which key opens the Adjust screen. |
