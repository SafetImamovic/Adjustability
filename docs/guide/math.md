# The math

How Adjustability decides which enchantments can be active together, and what a toggle changes. The code for this is
the `graph` module: pure Java, no Minecraft, unit tested.

## Which enchantments conflict

Since Minecraft 1.21, every enchantment is a JSON file with an `exclusive_set`: a tag of enchantments it can't share an
item with. Two enchantments conflict when either one lists the other. Adjustability asks the game
(`Enchantment.areCompatible`) instead of keeping its own list, so datapacks and modded enchantments are picked up
automatically.

Conflicts are **symmetric** (if A conflicts with B, B conflicts with A) but **not transitive**:

- Riptide conflicts with Loyalty.
- Riptide conflicts with Channeling.
- Loyalty and Channeling are compatible.

So conflicts can't be described as groups where everyone excludes everyone. They form a graph.

## The conflict graph of an item

Take every enchantment an item holds, active or dormant, as a vertex, and connect two with an edge when they conflict.

```
Pickaxe:     Fortune ── Silk Touch
Chestplate:  Protection, Fire, Blast and Projectile Protection, every pair connected
Trident:     Loyalty ── Riptide ── Channeling
Bow:         Infinity ── Mending
```

An **active set** is valid when no two of its enchantments share an edge: an *independent set* of the graph. A *choice*
the player can make is a **maximal** independent set: no dormant enchantment could be added without a conflict.

## How many choices

Conflicts on one item usually fall into separate pieces (*components*) that never touch, like Fortune/Silk Touch and the
Protections. Choices multiply across them:

| Component | Shape | Choices |
|---|---|---|
| Fortune, Silk Touch | one edge | 2 |
| Four Protections | every pair connected | 4 |
| Loyalty, Riptide, Channeling | a path | 2: Riptide, or Loyalty + Channeling |

Boots with all four Protections and Depth Strider + Frost Walker have 4 × 2 = 8 choices.

## What a toggle does

Making a dormant enchantment `e` active:

1. Everything active that conflicts with `e` becomes dormant.
2. `e` becomes active.
3. Every dormant enchantment that no longer conflicts with anything active becomes active again, in a fixed order
   (this is what brings Channeling back with Loyalty).

The result is always valid, always maximal, and the same on the server and in the client's preview.

## Cost

Building an item's graph compares each pair of its enchantments once, and an item rarely holds more than 15. The game's
full conflict table (every enchantment against every other) is built once per datapack reload. Items without dormant
enchantments skip all of this.
