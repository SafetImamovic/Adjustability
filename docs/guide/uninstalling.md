# Uninstalling

::: tip Short version
Removing Adjustability never deletes or breaks an item. Items keep their **active** enchantments and work as usual.
Their **dormant** enchantments just stop doing anything (they did nothing anyway) and stop showing. To remove them for
good, run `/adjustability flatten @a` with everyone online **before** removing the mod.
:::

## What happens to items when the mod is gone

Dormant enchantments are stored on each item in vanilla's `minecraft:custom_data` component (see
[Where dormant enchantments are stored](storage.md)). Without Adjustability:

| | Without flattening | After `/adjustability flatten` |
|---|---|---|
| Active enchantments | Kept, work as usual | Kept, work as usual |
| Dormant enchantments | Stay in the item's `custom_data`, invisible and inert | Gone |
| The item | Loads and works like any vanilla item | A plain vanilla item |
| Putting the mod back | The dormant enchantments are back | Nothing to bring back |

Nothing is lost by skipping the flatten, and you can still decide later: reinstalling brings the dormant enchantments
back, and flattening then removes them.

## Flattening before you uninstall

`/adjustability flatten [<targets>]` (operators) removes the dormant enchantments from every item in the targets'
**inventories and ender chests**, and says how many items it changed. Without targets it flattens your own.

1. Get every player online (offline players can't be reached).
2. Run `/adjustability flatten @a`.
3. Stop the server and remove Adjustability from the server **and** every client.

What flattening does **not** reach, because it only looks at players:

- items in chests, barrels, shulker boxes placed in the world, hoppers, item frames, armor stands or on the ground;
- items inside shulker boxes or bundles that are themselves in an inventory;
- players who aren't online.

Those keep their `adjustability:dormant` entry. That's harmless: it's inert data in a vanilla component. If you want
them gone too, put the items in a player's inventory and flatten again, or edit them with `/data` or `/item`.

## Removing it from only one side

Adjustability is meant to be on both the server and the clients. A client without it may be refused by a server that
has it, or join without any of its features; a server without it leaves clients' switching keys and panel with nothing
to talk to. Remove it from both.
