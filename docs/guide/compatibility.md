# Compatibility

Adjustability works on Minecraft **1.21.1**, **26.1**, **26.2** and **26.3** with **Fabric** (Fabric API, Forge Config API Port) and
**NeoForge** (21.1.1 or newer for 1.21.1, 26.1.2.125 or newer for 26.1, 26.2.0.93 or newer for 26.2, 26.3.0.95 or newer for 26.3), on both the client and the server. Both
versions have the same features.

## Tested with

These mods were installed alongside Adjustability 0.1.0 on 26.3 and the mod's full automated test suite was run: the anvil,
grindstone and switching tests on the server, and on Fabric the client test that opens the inventory, uses the Adjust
panel, the Cycle key, hold mode and the None row in a real game client.

| Mod | Version | Loader | Result |
|---|---|---|---|
| Easy Anvils (+ Puzzles Lib) | 26.3.1 (26.3.15) | Fabric, NeoForge | Works: dormant enchantments, anvil prices and limits as without it |
| JEI (+ MezzConfig) | 31.16.0.89 (0.6.8) | Fabric, NeoForge | Works: the Adjust panel draws above JEI's item list; Left Alt and clicks reach the panel. Info pages on the anvils and the grindstone (after 0.1.0) |
| Mouse Tweaks | 26.3-2.31 | Fabric | Works: clicks on the panel don't reach Mouse Tweaks |
| Enchantment Descriptions (+ Bookshelf, Prickle) | 26.3.0.2 | Fabric | Works: descriptions show under active enchantments; dormant lines follow |
| AppleSkin | 3.0.10 | Fabric | Works |
| Enchantment Switch (+ Puzzles Lib) | 26.3.0 (26.3.15) | Fabric | Loads and every test passes (0.2.0). Its Edit Enchantments key is unbound by default, so Left Alt stays with Adjustability. Not tested: its menu on an item that also has dormant enchantments |

On NeoForge, only the server-side tests ran with these mods; NeoForge has no client test framework.

## Mods that add enchantments or change the anvil

Adjustability reads conflicts from the game's `exclusive_set` tags, so what a mod changes there is followed, not
fought. These ran the server-side test suite (anvil, grindstone, switching, tooltips) and were also played through by
hand in a real client (tooltips, the Adjust panel, the anvil, mining, rejoining a world). 1.21.1 has no automated
tests; there, Apotheosis 8.9.0 (with Apothic Enchanting 1.6.4) on NeoForge and Enchancement 1.21-r14 on Fabric were
played through by hand only, including the Enchancement notice and the dormant tooltip lines.

| Mod | Version | Loader | Result |
|---|---|---|---|
| Apotheosis (+ Apothic Enchanting, Attributes, Spawners, Placebo, Patchouli) | 9.1.0 (2.1.0, 3.1.0, 2.0.1, 10.0.2, 94) | NeoForge 26.1.2 | Works, with one fix after 0.7.0: Apothic Enchanting writes enchantment tooltips itself, which hid the dormant lines; they are now added to the finished tooltip. It also removes Protection from `exclusive_set/armor` and Sharpness from `exclusive_set/damage`, so those stop conflicting and are not made dormant. 63 of 66 tests pass; the other three assume vanilla's conflicts or a vanilla enchantment list |
| Enchancement (+ StrawberryLib) | 26.3-r4 | Fabric 26.3 | Works: all 66 tests pass, including its own enchantments with one-sided conflicts against vanilla (Extracting / Silk Touch, Frostbite / Fire Aspect, Bouncy / Feather Falling). Tested with its defaults relaxed: as shipped it disallows 36 vanilla enchantments (Fortune among them) and limits items to one enchantment, which is its own rule |
| Enchancement (+ StrawberryLib, Forgified Fabric API, Launchpad) | 26.1-r25 | NeoForge 26.1.2 | The notice and its own enchantments' conflicts work (all 36 disallowed vanilla enchantments are reported with its defaults; Extracting / Silk Touch and the others conflict both ways). On this build Unbreaking is removed from items even with its config relaxed and `isEnchantmentAllowed` saying it is allowed (a plain `ItemStack.enchant` leaves it at level 0), so 13 of our tests that use Unbreaking can't run there; the other 53 pass |

![The Adjust panel next to a pickaxe, with JEI's item list on the right](/screenshots/adjust-panel-jei.png)

## Known limits

- **Mods that replace the anvil entirely** can stop dormant enchantments from being made in it; switching still works
  on items that already have them, and `/adjustability add` adds them by command.
- **Enchanting tables and `/enchant`** follow vanilla's conflicts, not the server's `[exclusivity]` rules.
- **The creative inventory:** the Adjust panel works on your own items (the hotbar, and every slot on the inventory
  tab), not on the creative tabs' items.
- **Keys:** Left Alt is the Adjust key by default. If another mod uses Left Alt in inventories, rebind one of them
  under Controls.

Found something that doesn't work together? Use the
[enchantment compat issue form](https://github.com/SafetImamovic/Adjustability/issues/new/choose).
