# Tags and exclusive sets

Two kinds of data decide what Adjustability does: vanilla's **exclusive sets** decide which enchantments conflict, and
Adjustability's **tags** decide what the mod is allowed to do about it. Both live in datapacks and reload with `/reload`.

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

### Two ways to change what conflicts

| | Datapack (`exclusive_set`) | Server config (`[exclusivity]`) |
|---|---|---|
| Affects | Vanilla and Adjustability: the enchanting table, `/enchant`, the anvil | Adjustability only |
| Where | `data/<namespace>/enchantment/<name>.json` or the `#minecraft:exclusive_set/*` tags | `adjustability-server.toml` |
| Good for | Changing the game's rules for everyone | Making extra pairs switchable, or letting a pair stack, without touching vanilla |

See [Which enchantments conflict](server-config.md#which-enchantments-conflict) for the config.

## Adjustability's tags

| Tag | Type | Default | Meaning |
|---|---|---|---|
| `#adjustability:never_dormant` | Enchantment | `#minecraft:curse` | Always active. Can't be switched off, so curses can't be dodged. |
| `#adjustability:not_combinable` | Enchantment | empty | Conflicts involving these keep vanilla behavior: the anvil skips them. |
| `#adjustability:not_adjustable` | Item | empty | Items that never hold dormant enchantments, in either mode. |
| `#adjustability:adjustable` | Item | empty | With `[items] mode = ALLOWLIST` in the [server config](server-config.md#items): the only items that can hold dormant enchantments. |

Add to a tag without replacing the defaults:

```json
{
  "replace": false,
  "values": ["minecraft:infinity"]
}
```

saved as `data/adjustability/tags/enchantment/not_combinable.json` in your datapack: Infinity and Mending can no longer
be combined, everything else still can.
