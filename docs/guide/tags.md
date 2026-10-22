# Tags and exclusive sets

Two kinds of data decide what Adjustability does: vanilla's **exclusive sets** decide which enchantments conflict, and
Adjustability's **tags** decide what the mod is allowed to do about it. Both live in datapacks and reload with `/reload`.

> **Planned.** The tags arrive in M1–M3; names and defaults may still change before 0.1.0.

## Exclusive sets (vanilla)

Each enchantment file (`data/<namespace>/enchantment/<name>.json`) has an `exclusive_set`, usually a tag:

```json
{
  "exclusive_set": "#minecraft:exclusive_set/mining"
}
```

Two enchantments conflict when either one's exclusive set contains the other. Vanilla's sets include
`#minecraft:exclusive_set/armor`, `/damage`, `/mining`, `/boots`, `/bow`, `/crossbow` and `/riptide`. Change them, and
both vanilla and Adjustability follow: add an enchantment to a set to make it conflict, remove it to let it stack like
any other.

## Adjustability's tags

| Tag | Type | Default | Meaning |
|---|---|---|---|
| `#adjustability:never_dormant` | Enchantment | `#minecraft:curse` | Always active. Can't be switched off, so curses can't be dodged. |
| `#adjustability:not_combinable` | Enchantment | empty | Conflicts involving these keep vanilla behavior: the anvil skips them. |
| `#adjustability:not_adjustable` | Item | empty | Items that never hold dormant enchantments. |

Add to a tag without replacing the defaults:

```json
{
  "replace": false,
  "values": ["minecraft:infinity"]
}
```

saved as `data/adjustability/tags/enchantment/not_combinable.json` in your datapack: Infinity and Mending can no longer
be combined, everything else still can.
