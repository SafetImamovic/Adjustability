# Where dormant enchantments are stored

Adjustability adds no blocks, no entities, no world files and no database. Everything it knows about an item is on
the item itself, in vanilla's own data, so it travels with the item: between chests, players, servers, through `/give`
and in structure files.

## Active enchantments: vanilla, untouched

Active enchantments are ordinary enchantments in vanilla's `minecraft:enchantments` component. Vanilla and every other
mod see them exactly as without Adjustability. That's also why a dormant enchantment does nothing: the game only looks
there.

## Dormant enchantments: one entry in `custom_data`

Dormant enchantments are kept in vanilla's `minecraft:custom_data` component (free-form data any mod or datapack can
put on an item), under one key, `adjustability:dormant`, as **enchantment id → level**:

```snbt
minecraft:custom_data={
    "adjustability:dormant": {
        "minecraft:silk_touch": 1
    }
}
```

A Fortune III pickaxe with a dormant Silk Touch, as a `/give` command:

```
/give @s minecraft:diamond_pickaxe[minecraft:enchantments={"minecraft:fortune":3},minecraft:custom_data={"adjustability:dormant":{"minecraft:silk_touch":1}}]
```

(This exact command is checked by a test, so it stays correct.)

| | |
|---|---|
| Component | `minecraft:custom_data` (vanilla) |
| Key | `adjustability:dormant` |
| Value | A compound: enchantment id (string) → level (int) |
| Other keys in `custom_data` | Left alone; other mods' and datapacks' data stays as it is |
| No dormant enchantments | No `adjustability:dormant` key, and no `custom_data` at all if nothing else uses it, so the item is exactly a vanilla item |
| Books | Never have dormant enchantments; a book that holds conflicting enchantments keeps them all in vanilla's `minecraft:stored_enchantments` |

To see an item's split in game, hold it and run `/adjustability inspect` (operators): it lists the active and dormant
enchantments and which ones conflict.

## Why there, and not somewhere else

- **Removing the mod never breaks an item.** `custom_data` is vanilla, so an item with dormant enchantments always
  loads, with or without Adjustability. See [Uninstalling](uninstalling.md).
- **Plain ids.** Enchantments are stored by name, not by an internal number, so the data reads the same on every
  server and needs nothing from the world to load. An enchantment that no longer exists (a datapack or mod removed) is
  skipped and kept in the data, and comes back if the enchantment does.
- **No per-world database.** Items are copied, split and moved all the time; data that lives anywhere other than on
  the item gets lost or out of sync.

## For mod and datapack developers

- Read the vanilla `minecraft:enchantments` component as usual: it's always the active set, and never holds two
  enchantments that conflict once Adjustability has touched the item.
- Don't write `adjustability:dormant` by hand unless you mean to: the entry is checked on every switch, but nothing
  stops a datapack from putting an enchantment there that conflicts with nothing. It then shows as dormant and becomes
  active the next time the item is switched or goes through an anvil.
- A Java API for reading and changing the split is planned ([Java API](java-api.md)).
