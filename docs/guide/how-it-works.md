# How it works

Vanilla won't put some enchantments on the same item: Fortune and Silk Touch, the four Protections, Sharpness, Smite and
Bane of Arthropods, Depth Strider and Frost Walker, Multishot and Piercing, Riptide and Loyalty or Channeling, Infinity
and Mending. Adjustability lets an item **hold** them anyway, with only one side of each conflict **active**.

## Active and dormant

- **Active** enchantments work exactly like vanilla ones. Vanilla and every other mod see only these.
- **Dormant** enchantments are kept on the item with their level, shown in its tooltip, and do nothing until you make
  them active.

An item never has two conflicting enchantments active. Fortune III and Silk Touch on one pickaxe means it mines with
one of them, never both.

```
Diamond Pickaxe
Efficiency V
Fortune III
Unbreaking III
  ⇄ Silk Touch          ← dormant, dark gray, right after the active ones
```

## Getting conflicting enchantments on an item

Use an anvil, like any other enchantment:

| Left | Right | Result |
|---|---|---|
| Pickaxe with Fortune | Book with Silk Touch | Fortune stays active, Silk Touch is added dormant |
| Pickaxe with Fortune | Pickaxe with Silk Touch | Same |
| Book with Fortune | Book with Silk Touch | One book with both; putting it on a pickaxe splits them into active and dormant |

The anvil costs what vanilla charges, plus a little extra per level of each enchantment that becomes dormant (set by
the server). Enchantments that don't conflict with anything are added active, like vanilla. What was active on the left
item stays active; the new conflicting enchantment is the one that becomes dormant. If nothing was active yet (a plain
item and a book holding both), the enchantment whose id comes first alphabetically is active.

The anvil gives no result if the item would hold more dormant enchantments than the server allows, or if the price goes
over the survival limit of 39 levels.

## Switching

- **In your inventory** (or any chest, shulker box...): hover the item and press **Left Alt**. The Adjust panel
  opens next to it, like a tooltip that stays: each group of conflicting enchantments, the active one marked ●, the
  dormant ones ○. Click a dormant one to make it active. **Left Alt** again, **Escape** or a click outside closes it;
  that click never picks up an item. The item's tooltip shows the key as a reminder (`Left Alt: adjust`). Its colors,
  side and decorations are yours to change: `/adjustability panel`, or the [player settings](server-config.md#player-settings).
- **While playing:** bind **Cycle held item's enchantments** under Controls → Adjustability (unbound by default). One
  press steps the held item to its next choice, e.g. Fortune → Silk Touch → Fortune, and says which one is active above
  the hotbar.
- **By command:** `/adjustability toggle minecraft:silk_touch` on the held item.

Switching has a cooldown (1 second by default) and can cost XP, an item or durability; the server decides. Creative
players switch for free and without a cooldown. A curse is never switched off.

The Adjust key doesn't work in the creative inventory, which shows items through a menu only your client knows about;
switch to survival or use the command. Some combinations bring
more than one enchantment back: on a trident, switching from Riptide to Loyalty also makes Channeling active again,
because Loyalty and Channeling don't conflict with each other.

## Grindstones

A grindstone removes dormant enchantments too. The experience it gives comes from the active ones only, as in vanilla.
Curses stay, like vanilla. Servers can keep dormant enchantments instead (`remove_dormant` in the
[server config](server-config.md)).

## If the mod is removed

Items keep their active enchantments and keep working. The dormant ones stop showing and do nothing, because vanilla
has no way to represent them; they're kept in the item's `custom_data`, so an item never fails to load. Putting the mod
back brings them back.
