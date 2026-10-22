# Compatibility

Adjustability works on Minecraft 26.3 with **Fabric** (Fabric API, Forge Config API Port) and **NeoForge 26.3.0.95 or
newer**, on both the client and the server.

## Tested with

These mods were installed alongside Adjustability 0.1.0 and the mod's full automated test suite was run: the anvil,
grindstone and switching tests on the server, and on Fabric the client test that opens the inventory, uses the Adjust
panel, the Cycle key, hold mode and the None row in a real game client.

| Mod | Version | Loader | Result |
|---|---|---|---|
| Easy Anvils (+ Puzzles Lib) | 26.3.1 (26.3.15) | Fabric, NeoForge | Works: dormant enchantments, anvil prices and limits as without it |
| JEI (+ MezzConfig) | 31.16.0.89 (0.6.8) | Fabric, NeoForge | Works: the Adjust panel draws above JEI's item list; Left Alt and clicks reach the panel |
| Mouse Tweaks | 26.3-2.31 | Fabric | Works: clicks on the panel don't reach Mouse Tweaks |
| Enchantment Descriptions (+ Bookshelf, Prickle) | 26.3.0.2 | Fabric | Works: descriptions show under active enchantments; dormant lines follow |
| AppleSkin | 3.0.10 | Fabric | Works |

On NeoForge, only the server-side tests ran with these mods; NeoForge has no client test framework.

![The Adjust panel next to a pickaxe, with JEI's item list on the right](/screenshots/adjust-panel.png)

## Known limits

- **Mods that replace the anvil entirely** can stop dormant enchantments from being made in it; switching still works
  on items that already have them, and `/adjustability add` adds them by command.
- **Enchanting tables and `/enchant`** follow vanilla's conflicts, not the server's `[exclusivity]` rules.
- **The creative inventory** has no Adjust panel (it uses a menu only the client knows); switch in survival, or use the
  Cycle key or `/adjustability toggle`.
- **Keys:** Left Alt is the Adjust key by default. If another mod uses Left Alt in inventories, rebind one of them
  under Controls.

Found something that doesn't work together? Use the
[enchantment compat issue form](https://github.com/SafetImamovic/Adjustability/issues/new/choose).
