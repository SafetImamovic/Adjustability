# Cookbook

Ready-made rule sets. Config keys are in [Server config](server-config.md), tags in
[Tags and exclusive sets](tags.md).

> **Planned.** These recipes use the planned config keys and tags; they'll be checked against the real ones before
> 0.1.0.

## Make switching cost 1 lapis

```toml
[toggle]
cost_type = "ITEM"
cost_amount = 1
cost_item = "minecraft:lapis_lazuli"
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

## Uninstall cleanly

Before removing the mod, run `/adjustability flatten @a` with everyone online. Items keep their active enchantments;
dormant ones are removed.
