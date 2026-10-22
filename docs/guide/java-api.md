# Java API

For mod devs who want to read or change an item's active and dormant enchantments. Everything will be in
`io.github.safetimamovic.adjustability.api` and work the same on Fabric and NeoForge.

> **Planned for M4.** Until then there is no public API, and nothing outside `api` should be relied on. Most mods need
> none of this: anything that reads an item's enchantments the vanilla way already sees only the active ones.

## Planned entry points

| Call | Use it for |
|---|---|
| `AdjustabilityApi.getActive(ItemStack)` / `getDormant(ItemStack)` | Read the split |
| `AdjustabilityApi.addEnchantment(ItemStack, Holder<Enchantment>, int)` | Add an enchantment, dormant if it conflicts |
| `AdjustabilityApi.activate(ServerPlayer, ItemStack, Holder<Enchantment>)` | Switch from code, with the server's costs |
| `AdjustabilityApi.flatten(ItemStack)` | Remove dormant enchantments |

Plus a cancellable event before every switch, one to change which enchantments an anvil makes dormant, and an
`AdjustableItem` interface for items that want their own rules.
