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
  ⇄ Silk Touch          ← dormant
Unbreaking III
```

## Getting conflicting enchantments on an item

Use an anvil, like any other enchantment:

| Left | Right | Result |
|---|---|---|
| Pickaxe with Fortune | Book with Silk Touch | Fortune stays active, Silk Touch is added dormant |
| Pickaxe with Fortune | Pickaxe with Silk Touch | Same |
| Book with Fortune | Book with Silk Touch | One book with both; putting it on a pickaxe splits them into active and dormant |

The anvil costs what vanilla charges, plus a little extra per dormant enchantment (set by the server). Enchantments that
don't conflict with anything are added active, like vanilla.

## Switching

- **In your inventory:** hover the item and press **Left Alt**. A small screen lists each conflict; click the one you
  want active.
- **While playing:** bind **Cycle enchantment** under Controls → Adjustability. One press switches the held item to its
  next choice, e.g. Fortune → Silk Touch.
- **By command:** `/adjustability toggle silk_touch` on the held item.

Switching can have a short cooldown and a cost (XP, an item, durability); the server decides. Some combinations bring
more than one enchantment back: on a trident, switching from Riptide to Loyalty also makes Channeling active again,
because Loyalty and Channeling don't conflict with each other.

## Grindstones

A grindstone removes dormant enchantments too, and gives their experience back, the same as active ones. Curses stay,
like vanilla.

## If the mod is removed

Items keep their active enchantments and keep working. The dormant ones are lost, because vanilla has no way to
represent them. Server admins can clean up first with `/adjustability flatten`.
