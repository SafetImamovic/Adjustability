# Modded enchantments

Modded enchantments work without any setup. Since Minecraft 1.21, every enchantment, vanilla or modded, is a datapack
file with an `exclusive_set`, and Adjustability reads conflicts from the game instead of from its own list. A mod that
adds an enchantment conflicting with Fortune gets the same treatment as Silk Touch.

What a mod's enchantment needs to behave well:

- Its effects come from the item's enchantments the normal way (through `EnchantmentHelper`), so a dormant enchantment
  does nothing. Enchantments that check the item's data themselves in unusual ways may still see dormant ones; report
  those with the enchantment compat issue form.
- Its conflicts are in its `exclusive_set`. A mod that blocks combinations in code instead (in its own anvil logic) is
  invisible to Adjustability.

To check one, hold an item with it and run `/adjustability inspect` (operators): it lists the active and dormant
enchantments and which ones conflict. `/adjustability add <enchantment> [level]` puts one on the held item, dormant if
it conflicts.

Mods that want to read or change dormant enchantments, or veto switches, use the [Java API](java-api.md).

Mods that replace the anvil completely can stop Adjustability's anvil path from running. Players can still switch
enchantments on items that already hold them.
