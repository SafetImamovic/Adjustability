# Changelog

Changes per version that players, pack devs and mod devs will notice. Format:
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow [Semantic Versioning](https://semver.org).

## [Unreleased]

### Added

- Combine conflicting enchantments in an anvil, from a book or another item: what was active stays active, the new
  conflicting enchantment is kept as dormant (shown in the tooltip, doing nothing). Vanilla's price plus 1 level per
  level made dormant.
- Two books with conflicting enchantments combine into one book that holds them all.
- Grindstones remove dormant enchantments too.
- Server config `adjustability-server.toml` with `[anvil]` (on/off, book combining, dormant cost, limits) and
  `[grindstone]` settings.
- Tags `#adjustability:never_dormant` (curses), `#adjustability:not_combinable` and `#adjustability:not_adjustable`.
- Operator commands `/adjustability inspect` and `/adjustability add <enchantment> [level]`.

### Changed

- Complete rewrite for Minecraft 26.3 on Fabric and NeoForge. The 2024 prototype for Forge 1.20.6 was never released
  and is kept under the `legacy-1.20.6-forge` tag.
- License changed from All Rights Reserved to MIT.
