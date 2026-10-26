# Cookbook

Ready-made rule sets. Config keys are in [Server config](server-config.md), tags in
[Tags and exclusive sets](tags.md).

## Make switching cost 1 lapis

```toml
[toggle]
cost_type = "ITEM"
cost_amount = 1
cost_item = "minecraft:lapis_lazuli"
```

## Make Sharpness and Fire Aspect switchable

```toml
[exclusivity]
extra_groups = ["minecraft:sharpness, minecraft:fire_aspect"]
```

They now conflict for Adjustability: a sword can hold both, one active at a time.

## Let Infinity and Mending stack

```toml
[exclusivity]
compatible = ["minecraft:infinity, minecraft:mending"]
```

They no longer conflict for Adjustability, so the anvil puts both on a bow, both active.

## Mining switches cost lapis, armor switches are free

```toml
[toggle]
group_costs = [
    "#minecraft:exclusive_set/mining = ITEM 1 minecraft:lapis_lazuli",
    "#minecraft:exclusive_set/armor = NONE",
]
```

## Keep Infinity and Mending apart

`data/adjustability/tags/enchantment/not_combinable.json`:

```json
{
  "replace": false,
  "values": ["minecraft:infinity"]
}
```

## At most two Protections per armor piece

```toml
[anvil]
max_conflicts_per_component = 2
```

## Only netherite tools are adjustable

`[items]` in the server config:

```toml
[items]
mode = "ALLOWLIST"
```

and `data/adjustability/tags/item/adjustable.json` in your datapack:

```json
{
  "values": [
    "minecraft:netherite_pickaxe",
    "minecraft:netherite_axe",
    "minecraft:netherite_shovel",
    "minecraft:netherite_hoe",
    "minecraft:netherite_sword"
  ]
}
```

## Hardcore: expensive and slow

```toml
[anvil]
dormant_cost_per_level = 3
max_dormant_per_item = 2

[toggle]
cost_type = "XP_LEVELS"
cost_amount = 2
cooldown_ticks = 200
allow_in_world_cycle = false
```

## An advancement for your pack

The `adjustability:switched` trigger fires on every switch. All its conditions are optional: `none` (`true`: only
switches to none, `false`: only to an enchantment), `min_switches` (the player's total switches, including this one)
and `holds` (enchantments, a list or a `#tag`, that the item must all hold afterwards, active or dormant). Put it in
`data/<your pack>/advancement/`:

```json
{
    "parent": "adjustability:root",
    "criteria": {
        "trident_master": {
            "trigger": "adjustability:switched",
            "conditions": {
                "holds": ["minecraft:riptide", "minecraft:loyalty", "minecraft:channeling"],
                "min_switches": 10
            }
        }
    },
    "display": {
        "icon": { "id": "minecraft:trident" },
        "title": "Weather Permitting",
        "description": "Switch a trident with Riptide, Loyalty and Channeling, ten switches in"
    }
}
```

## Uninstall cleanly

Before removing the mod, run `/adjustability flatten @a` with everyone online. Items keep their active enchantments;
switched-off ones come back on, the other dormant ones are removed. What it doesn't reach (chests in the world, shulker boxes) is harmless: see
[Uninstalling](uninstalling.md).
