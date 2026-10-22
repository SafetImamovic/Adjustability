# Adjustability — Central Design, Audit & Roadmap

> Put Fortune **and** Silk Touch on one pickaxe, and switch between them. Adjustability lets an item hold mutually
> exclusive enchantments; exactly one side of each conflict is active at a time, and the player toggles which.
>
> This file covers: what the mod is, an audit of the 2024 (1.20.6 / MinecraftForge) code, what changed in Minecraft
> between 1.20.6 and 26.3 that matters for enchantments, the target project layout (identical to Throwable Mobs), the
> design for 26.3, the math, config, datapacks, Java API, compatibility, testing, docs site, the step-by-step migration,
> and a milestone roadmap.
>
> Written 2026-10-22, alongside Throwable Mobs 0.1.0 (26.3, Fabric + NeoForge). Anything marked **(verify)** should be
> checked against 26.3 sources before relying on it. For ecosystem background (loaders, Modrinth vs CurseForge, JDK 25,
> Neovim, Stonecutter) see `THROWABLE_MOBS.md` §1–§5 in the Throwable Mobs repo; it all applies here unchanged and is not
> repeated.

---

## Table of contents

1. [What Adjustability is](#1-what-adjustability-is)
2. [Audit of the current code (1.20.6, 2024)](#2-audit-of-the-current-code-1206-2024)
3. [What changed between 1.20.6 and 26.3](#3-what-changed-between-1206-and-263)
4. [Target layout (same as Throwable Mobs)](#4-target-layout-same-as-throwable-mobs)
5. [Mod architecture](#5-mod-architecture)
6. [Core mechanic: active and dormant enchantments](#6-core-mechanic-active-and-dormant-enchantments)
7. [The math: conflict graphs and independent sets](#7-the-math-conflict-graphs-and-independent-sets)
8. [Getting conflicting enchantments onto an item](#8-getting-conflicting-enchantments-onto-an-item)
9. [Toggling: controls, UI and networking](#9-toggling-controls-ui-and-networking)
10. [Configuration](#10-configuration)
11. [Datapacks and tags](#11-datapacks-and-tags)
12. [Java API](#12-java-api)
13. [Balance and exploits](#13-balance-and-exploits)
14. [Compatibility checklist](#14-compatibility-checklist)
15. [Testing](#15-testing)
16. [Repo files, CI and publishing](#16-repo-files-ci-and-publishing)
17. [Public documentation (VitePress)](#17-public-documentation-vitepress)
18. [Migration: from the old repo to the new layout](#18-migration-from-the-old-repo-to-the-new-layout)
19. [Roadmap](#19-roadmap)
20. [Decisions](#20-decisions)
21. [Open questions](#21-open-questions)
22. [Links](#22-links)

---

## 1. What Adjustability is

Vanilla blocks some enchantment pairs from ever sharing an item: Fortune / Silk Touch, the four Protections, Sharpness /
Smite / Bane of Arthropods (and Density / Breach on the mace), Depth Strider / Frost Walker, Multishot / Piercing,
Riptide / (Loyalty, Channeling), Infinity / Mending. Players end up carrying two pickaxes, or swapping armor sets.

Adjustability's answer, unchanged from the 2024 idea:

- An item may **hold** conflicting enchantments.
- Only a **compatible subset** is **active** at any time. The rest are **dormant**: kept on the item, shown in the
  tooltip, doing nothing.
- The player **toggles** which side of each conflict is active, in the inventory or with a key in the world.
- **No stacking**, ever. A pickaxe with Fortune III and Silk Touch never mines with both. The mod saves inventory slots;
  it doesn't make items stronger than vanilla allows at any one moment.
- Pack devs and server admins control the cost (XP, cooldown, anvil price), which pairs may be combined at all, and
  which items take part.

**Audiences** (same split as Throwable Mobs): players, server admins, pack devs (config + datapacks), mod devs (Java
API, and "my modded enchantments just work").

**Prior art to check before 1.0 (verify each on Modrinth/CurseForge for 26.x):** mods that remove exclusivity outright
(e.g. "Enchantment Compatibility"-style mods, Apotheosis' enchanting overhaul), and "toggle enchantment" mods. Most
remove the conflict and let enchantments stack. The toggle-without-stacking angle is Adjustability's niche; say so on
the mod page.

---

## 2. Audit of the current code (1.20.6, 2024)

The 2024 code is a proof of concept: a mutual-exclusivity lookup, a first anvil hook, and a hand-drawn overlay that
appears when holding Ctrl over an item. None of it can be ported as is: the enchantment system, the GUI renderer and the
build were all replaced upstream (§3). What's worth keeping is the **idea, the docs' reasoning, and the lookup
concept**. Line numbers below are against commit `353e60c`.

### 2.1 What exists

| File | What it does | Keep? |
|---|---|---|
| `Adjustability.java` | `@Mod` entry. Dumps every registered enchantment id to `config/adjustability/enchantment_data.json`, loads exclusivity groups, prints them. Registers `AnvilHandler` and `CustomTooltip` on the Forge bus. | Rewrite (becomes ~20 lines of common init). |
| `custom_data/PersistData.java` | Gson read/write of two JSON files: all enchantment ids, and hard-coded exclusivity groups. | Drop. Exclusivity comes from the game's registry now (§3). |
| `enchantments_logic/MutuallyExclusive.java` | Builds `Map<Enchantment, Set<Enchantment>>` from groups; finds conflicting groups on a stack. | Keep the concept, rewrite as a pure-Java graph module (§7). |
| `enchantments_logic/AnvilHandler.java` | `AnvilUpdateEvent`: if both inputs are the same item and enchanted, copies the right item's enchantments onto the left, cost 69. | Rewrite (§8). |
| `custom_tooltip/CustomTooltip.java` + `CustomTooltipConfig.java` | Cancels the vanilla tooltip while Ctrl is held, draws a custom box with corner decorations and two arrow buttons, swallows mouse events over it. | Drop. Replace with a vanilla `Screen` + tooltip lines (§9). Keep the visual style (dark box, magenta borders, corner brackets) as a design reference. |
| `gui_elements/Button.java`, `OptionToggle.java` | Hand-rolled buttons with hover and click. | Drop; vanilla `Button` / `CycleButton` widgets. |
| `render_core/RenderCore.java` | Immediate-mode quads via `Tesselator`/`BufferBuilder`, z-index translation. | Drop. That API no longer exists in this form (§3). |
| `Config.java` | Commented-out MDK example; `SPEC` never registered. | Drop; config moves to `common/config` (§10). |
| `AdjustabilityDocs/` (Writerside) + `docs/` (generated Writerside HTML) | Mechanics, storage, DSA, math (SVGs), UML class diagrams. | Port the reasoning to VitePress pages (§17), drop the generated HTML. |

### 2.2 Bugs and design problems (why a straight port won't work)

**Correctness**

1. **The main use case never fires.** `AnvilHandler.java:24` returns unless `left.isEnchanted() && right.isEnchanted()`.
   An enchanted *book* keeps its enchantments in `stored_enchantments`, not `enchantments`, so `isEnchanted()` is false
   for books → Pickaxe(Fortune) + Book(Silk Touch) is ignored. Even past that, `AnvilHandler.java:31` requires
   `left.getItem() == right.getItem()`, which a book never is.
2. **It stacks, which is exactly what the mod promises not to do.** The anvil output (`AnvilHandler.java:36-41`) puts
   every enchantment of the right item onto the left as *active*. There is no dormant state anywhere yet, so the result
   would be a pickaxe with working Fortune and Silk Touch.
3. **Fixed cost of 69 levels** (`AnvilHandler.java:22`) is above the survival "Too Expensive!" limit (40), so the output
   can't be taken in survival anyway; it also ignores the vanilla cost formula and prior-work penalty.
4. **Only the first conflict is found.** `MutuallyExclusive.getMutuallyExclusiveEnchantments` `break`s after the first
   group (`MutuallyExclusive.java:117`). An armor piece with Protection + Fire Protection *and* boots with Depth Strider
   + Frost Walker would only report one.
5. **The exclusivity data is wrong and incomplete.** The hard-coded groups (`PersistData.java:109-116`) model every
   conflict as a clique (everyone conflicts with everyone in the group). That isn't how vanilla works: Riptide conflicts
   with Loyalty and with Channeling, but Loyalty and Channeling are compatible (a "star", not a clique; Loyalty is also
   missing from the data). Infinity / Mending and the 1.21 mace enchantments (Density, Breach vs Smite/Sharpness/Bane)
   are missing entirely. Any hand-written list rots; the game already knows the answer (§3).
6. **Static state filled by a constructor.** `mutuallyExclusiveMap` is `static` but populated by `new
   MutuallyExclusive(...)` (`MutuallyExclusive.java:13`, `:66-75`), and `getMutuallyExclusiveEnchantments` is static.
   Creating the object twice silently merges data; calling the static method before construction silently finds
   nothing.
7. **Unknown ids become `null` entries.** `PersistData.loadMutuallyExclusiveData` adds whatever
   `ForgeRegistries.ENCHANTMENTS.getValue(...)` returns; a typo or a removed mod's enchantment puts `null` into the
   group and later into the map key set.

**Client / server**

8. **Dedicated server crash.** `commonSetup` registers `new CustomTooltip()` (`Adjustability.java:116`), a class that
   imports `Screen`, `RenderTooltipEvent`, `ScreenEvent`. On a dedicated server those classes don't exist →
   `NoClassDefFoundError` at startup. Client-only code must only be reachable from a client entrypoint.
9. **No networking at all.** A toggle clicked in a client GUI has to be sent to the server, validated and applied there,
   or it does nothing (or desyncs) in multiplayer. Nothing like that exists yet.
10. **Item stacks have no identity.** `Storage.md` plans a per-world database keyed by "the ItemStack's unique
    identifier". Item stacks don't have one: they are copied, split, merged, moved between containers, dropped as
    entities and re-created all the time, and nothing stable survives that. The only robust place for per-item data is
    **on the stack itself** (a data component), which is what §6 does. The doc's concern ("invasive, stays forever if the
    mod is removed") is real and is handled in §6.3 instead.

**Code quality**

11. Mod bus listener registered twice (`Adjustability.java:48` and `:53`).
12. Enchantment registry read in the mod constructor (`Adjustability.java:67`), before other mods' registrations are
    guaranteed to be done; and the `enchantment_data.json` dump is rewritten on every launch for no reader.
13. `CustomTooltip` mixes `static` and instance handlers on one class registered both by `@EventBusSubscriber` (statics)
    and by `register(new CustomTooltip())` (instance methods); `this.rendering` (`CustomTooltip.java:428`) writes a
    static through `this`.
14. Ctrl is checked with `Screen.hasControlDown()` but release is detected by raw keycode `341` (`CustomTooltip.java:426`,
    left Ctrl only): pressing **right** Ctrl opens the overlay and it never closes. Not rebindable.
15. Every item tooltip gets "Hold [CTRL] for: <<Enchantment Adjustments>>" (`CustomTooltip.java:61`), including
    dirt. Hard-coded English instead of translation keys.
16. Any mouse click anywhere sets `clickActive` (`CustomTooltip.java:500`); the toggle loop always uses `get(0)`
    (`CustomTooltip.java:484`).
17. `System.out.println` / `printStackTrace` everywhere instead of the logger; debug prints on every anvil update.
18. License "All Rights Reserved" (`gradle.properties`) while the README invites forks and PRs. README links a
    `README.bs.md` that doesn't exist and hot-links the logo from the wiki.

**Verdict:** start a clean 26.3 multiloader project (Throwable Mobs layout), keep the old code only in git history
(tag it, §18), and carry over the design ideas and the docs' reasoning.

### 2.3 What was good and should survive

- The core design rule: **toggle, never stack.**
- The instinct to be **non-invasive** to vanilla data and to survive the mod being removed (§6.3).
- Precomputing a conflict lookup instead of scanning on every query (§7), and documenting the data structures and their
  complexity.
- Thinking in terms of "items with conflicts" vs "items without": the mod should cost nothing for the second kind.
- Wanting modded enchantments to work automatically (the registry dump was a step toward that).
- The tooltip look (dark panel, magenta gradient border, corner brackets) as the mod's visual identity.

---

## 3. What changed between 1.20.6 and 26.3

Only what matters for this mod. General ecosystem changes (year-based versions, unobfuscated game, Java 25, Mojang names
everywhere, `Identifier` instead of `ResourceLocation`, typed payloads, Fabric + NeoForge, no MinecraftForge) are in
`THROWABLE_MOBS.md` §1.

| Change | Since | What it means here |
|---|---|---|
| **Enchantments are data-driven.** They're a *dynamic (datapack) registry*: `data/<ns>/enchantment/<name>.json`, `Registries.ENCHANTMENT`, referenced as `Holder<Enchantment>` / `ResourceKey<Enchantment>`. No `Enchantment` subclasses, no `ForgeRegistries.ENCHANTMENTS`. | 1.21 | Enchantments exist only once a world's registries are loaded, via `RegistryAccess` (server: `server.registryAccess()`; client: `level.registryAccess()`). Nothing enchantment-related can happen at mod init. Modded enchantments are just more JSON, so they're supported for free. |
| **Exclusivity is data too.** Each enchantment's JSON has `exclusive_set` (an enchantment tag, e.g. `#minecraft:exclusive_set/mining`). `Enchantment.areCompatible(a, b)` is true when neither is in the other's set (and a ≠ b). | 1.21 | Delete the hand-written groups. Ask the game. A pack that edits `exclusive_set` changes Adjustability's behavior automatically. The relation is **symmetric but not transitive** (Riptide/Loyalty/Channeling), so it's a graph, not a list of groups (§7). |
| **Data components replaced item NBT.** Active enchantments: `DataComponents.ENCHANTMENTS` (`ItemEnchantments`). Books: `DataComponents.STORED_ENCHANTMENTS`. Free-form data: `DataComponents.CUSTOM_DATA`. Mods register their own `DataComponentType`s with a codec + stream codec. | 1.20.5 | Dormant enchantments live in a component on the stack (§6). |
| **Enchantment effects are component-based** (`effects` in the JSON: `minecraft:block_experience`, attributes, damage...), evaluated by `EnchantmentHelper` from `DataComponents.ENCHANTMENTS`. | 1.21 | Anything not in `ENCHANTMENTS` has no effect. Dormant = "not in that component". **No mixins into enchantment logic needed**, which is the biggest simplification over the 2024 plan. |
| **GUI rendering rewritten.** `GuiGraphics` now records into a render state that's drawn later (1.21.6+); `PoseStack` in GUI became a 2D matrix stack; the old `Tesselator.getBuilder()`/`BufferUploader` immediate mode and z-translation tricks are gone **(verify exact 26.3 API)**. | 1.21.2–1.21.6 | `RenderCore` is unportable. Use vanilla widgets and `GuiGraphics.fill` / `blitSprite` / text calls only; they survive renderer changes. |
| **Tooltip API.** `Item.appendHoverText` takes a `TooltipDisplay`; enchantment lines are added from the components by `ItemStack.addToTooltip` **(verify)**. Loader events: Fabric `ItemTooltipCallback`, NeoForge `ItemTooltipEvent`. | 1.21.5 | Add dormant lines through the loader tooltip events, called into one common method. |
| **Key mappings have categories as objects** (`KeyMapping.Category`, see Throwable Mobs `CarryKeys`). | 26.x | Copy the Throwable Mobs pattern. |
| **Anvil.** `AnvilMenu.createResult()` merges enchantments, calling `Enchantment.areCompatible` per pair, and computes cost; NeoForge fires `AnvilUpdateEvent` first. Fabric has no anvil event **(verify for Fabric API 0.161)**. | — | One common mixin into `AnvilMenu` covers both loaders the same way (§8). |
| **Grindstone** strips non-curse enchantments from `ENCHANTMENTS` and `STORED_ENCHANTMENTS` and gives XP. | — | Must also clear the dormant component, or grindstoning becomes a dupe/keep trick (§13). |
| **Game tests** have a new registry-based framework (test functions, environments as data) on both loaders **(verify 26.3 shape)**. | 1.21.5 | Anvil, toggle and grindstone behavior get GameTests (§15). |

---

## 4. Target layout (same as Throwable Mobs)

Exactly the Throwable Mobs repository, renamed. `physics/` becomes `graph/`: the same role (pure Java, no Minecraft
imports, unit tested, compiled into both loader jars).

```
Adjustability/
├─ .editorconfig                  # copied 1:1
├─ .gitattributes                 # copied 1:1
├─ .gitignore                     # copied 1:1 (docs/plans/, node_modules, vitepress cache/dist, runs, hs_err)
├─ .github/
│  ├─ ISSUE_TEMPLATE/
│  │  ├─ bug_report.yml           # placeholder "Pick up a creeper" → "Put a Silk Touch book on a Fortune pickaxe"
│  │  ├─ config.yml
│  │  ├─ feature_request.yml
│  │  └─ enchantment_compat.yml   # replaces mob_behavior.yml: "this modded enchantment misbehaves when toggled"
│  ├─ dependabot.yml              # copied 1:1 (GitHub Actions only)
│  ├─ pull_request_template.md    # copied 1:1
│  └─ workflows/
│     ├─ build.yml                # artifact name adjustability-jars
│     ├─ docs.yml                 # copied 1:1
│     └─ release.yml              # jar globs adjustability-{fabric,neoforge}-*
├─ ADJUSTABILITY.md               # this file (≙ THROWABLE_MOBS.md)
├─ CHANGELOG.md                   # Keep a Changelog, starts with [Unreleased]
├─ CONTRIBUTING.md                # copied, scopes: common, fabric, neoforge, api, graph, anvil, toggle, client, config, network, data
├─ LICENSE                        # MIT (see §20)
├─ README.md                      # same structure as Throwable Mobs README
├─ SECURITY.md                    # "a packet that lets a player toggle/activate what they shouldn't, or dupes enchantments"
├─ build-logic/
│  ├─ build.gradle
│  └─ src/main/groovy/
│     ├─ multiloader-common.gradle
│     └─ multiloader-loader.gradle   # physicsJava → graphJava
├─ build.gradle                   # Loom 1.18.2 + ModDevGradle 2.0.148, apply false
├─ settings.gradle                # rootProject.name = 'Adjustability'; include graph, common, fabric, neoforge
├─ gradle.properties              # see §18.3
├─ gradle/wrapper/                # Gradle 9.7.1, copied
├─ gradlew, gradlew.bat
├─ package.json / package-lock.json   # "adjustability-docs", vitepress 1.6.4
├─ graph/                         # pure Java: conflict graph, independent sets, toggle algorithm (§7)
│  ├─ build.gradle
│  └─ src/{main,test}/java/io/github/safetimamovic/adjustability/graph/
├─ common/
│  ├─ build.gradle
│  └─ src/main/
│     ├─ java/io/github/safetimamovic/adjustability/
│     │  ├─ Constants.java              # MOD_ID, LOG, id()
│     │  ├─ Adjustability.java          # common init
│     │  ├─ AdjustabilityCommand.java   # /adjustability ...
│     │  ├─ ModRegistries.java          # data component type(s), same Registrar pattern as Throwable Mobs
│     │  ├─ api/                        # PUBLIC: AdjustabilityApi, AdjustableItem, ToggleContext, events
│     │  ├─ core/
│     │  │  ├─ DormantEnchantments.java # read/write the dormant component (§6)
│     │  │  ├─ ConflictResolver.java    # Holder<Enchantment> ↔ graph module
│     │  │  ├─ Toggler.java             # server-side toggle: validate, cost, apply
│     │  │  └─ AnvilMerge.java          # merge rules used by the anvil mixin
│     │  ├─ tags/ModTags.java           # enchantment + item TagKeys
│     │  ├─ config/                     # CreatorConfig, PlayerConfig, AdjustabilityConfig
│     │  ├─ network/                    # ToggleEnchantmentPayload, CycleEnchantmentPayload
│     │  ├─ client/                     # AdjustKeys, AdjustPanel, AdjustClient, client command
│     │  ├─ mixin/                      # AnvilMenuMixin, GrindstoneMenuMixin (MixinExtras only)
│     │  └─ platform/                   # Services + IPlatformHelper (ServiceLoader)
│     └─ resources/
│        ├─ adjustability.mixins.json
│        ├─ adjustability.png           # mod icon
│        ├─ pack.mcmeta
│        ├─ assets/adjustability/lang/en_us.json
│        └─ data/adjustability/
│           ├─ tags/enchantment/{never_dormant,not_combinable}.json
│           ├─ tags/item/{adjustable,not_adjustable}.json
│           └─ advancement/...          # optional, M6
├─ fabric/
│  ├─ build.gradle
│  └─ src/main/
│     ├─ java/io/github/safetimamovic/adjustability/
│     │  ├─ AdjustabilityFabric.java
│     │  ├─ AdjustabilityFabricClient.java
│     │  └─ platform/FabricPlatformHelper.java
│     └─ resources/
│        ├─ fabric.mod.json
│        ├─ adjustability.fabric.mixins.json
│        └─ META-INF/services/io.github.safetimamovic.adjustability.platform.services.IPlatformHelper
├─ neoforge/
│  ├─ build.gradle
│  └─ src/main/
│     ├─ java/io/github/safetimamovic/adjustability/
│     │  ├─ AdjustabilityNeoForge.java
│     │  ├─ AdjustabilityNeoForgeClient.java
│     │  └─ platform/NeoForgePlatformHelper.java
│     └─ resources/
│        ├─ META-INF/neoforge.mods.toml
│        ├─ adjustability.neoforge.mixins.json
│        └─ META-INF/services/io.github.safetimamovic.adjustability.platform.services.IPlatformHelper
└─ docs/                          # VitePress (§17)
   ├─ .vitepress/{config.mts, theme/index.ts, theme/style.css}
   ├─ index.md
   ├─ public/adjustability.png
   └─ guide/
      ├─ README.md                # rewritten to guide/index.md
      ├─ how-it-works.md
      ├─ server-config.md
      ├─ tags.md
      ├─ modded-enchantments.md
      ├─ math.md
      ├─ java-api.md
      └─ cookbook.md
```

Removed from the old repo: `src/`, `AdjustabilityDocs/`, the Writerside-generated `docs/` (its HTML would collide with
the VitePress `docs/` folder), the MDK `build.gradle`, `Config.java`.

Code style: the Throwable Mobs one (`CONTRIBUTING.md`): Java 25, **8-space indentation** (the old code uses tabs and
`if ( x )` spacing — switch), Allman braces, ≤120 columns, Mojang names, translation keys for all text.

---

## 5. Mod architecture

**Design rules** (mirroring Throwable Mobs §6):

- **The item stays a vanilla item.** No wrapper items, no custom enchantment classes, no replacing stacks. Vanilla's
  `enchantments` component holds the active set; one extra component holds the dormant set. Everything vanilla and every
  other mod reads keeps working, because from their point of view the item simply has its active enchantments.
- **Server-authoritative.** The client sends *intent* ("activate Silk Touch on slot 12"); the server validates (slot,
  item, enchantment present and dormant, cost affordable, cooldown) and applies. Clients only render.
- **Exclusivity comes from the game.** `Enchantment.areCompatible` on the current registry. No copy of that data in the
  mod, ever.
- **Graph logic in pure Java.** `graph/` takes generic vertices and a compatibility predicate, returns sets. Unit-tested
  without Minecraft, same reason as `physics/`.
- **Everything goes through the API**, including the mod's own anvil and toggle code (§12).
- **Zero cost for normal items.** Every hook starts with "does this stack have the dormant component?"; if not, return.

**Data flow**

```
             ┌────────────── server ──────────────────────────────────────────┐
 Anvil  ───► │ AnvilMenuMixin → AnvilMerge → ConflictResolver(graph) →        │
             │   active ⊆ compatible, rest → DormantEnchantments component    │
             │                                                                │
 Payload ──► │ Toggler.activate(player, slot, enchantment)                    │
 (client     │   validate → cost → graph.activate(...) → write both components│
  intent)    │   → item updates sync to the client the normal way             │
             └────────────────────────────────────────────────────────────────┘
 Client: tooltip lines from the components; the Adjust panel lists conflicts; keys send payloads.
```

---

## 6. Core mechanic: active and dormant enchantments

### 6.1 Data model

| Where | Holds | Read by |
|---|---|---|
| `minecraft:enchantments` (vanilla) | **Active** enchantments, always pairwise compatible. | Vanilla, every mod. |
| `adjustability:dormant` (see §6.3 for the exact container) | **Dormant** enchantments with their levels, as `ItemEnchantments`. | Adjustability only. |

Invariants, checked after every write (and by GameTests):

1. Active set is pairwise compatible (an independent set in the conflict graph, §7).
2. Active ∩ dormant = ∅ (an enchantment is in exactly one place; levels are not split).
3. Every dormant enchantment conflicts with at least one active one (otherwise it should be active; see `auto_fill`).
4. Enchantments in `#adjustability:never_dormant` (curses by default) are always active.
5. An item with an empty dormant set has **no** dormant component at all (stacks stay equal to vanilla ones, so they
   still stack, compare and match recipes like vanilla).

### 6.2 Tooltip

```
Diamond Pickaxe
Efficiency V
Fortune III
Unbreaking III
  ⇄ Silk Touch                ← dormant, dark gray, indented, prefixed
Mending
Adjust: [Alt] / hover + [Alt]  ← only on items that have dormant enchantments
```

Dormant lines sit directly under the active enchantment they conflict with (first one in tooltip order), so the pair
reads together. The hint line only shows on items with something to adjust (fixes audit #15), names the bound key, and
can be turned off in the player config.

### 6.3 Where the dormant set is stored (and what happens if the mod is removed)

The 2024 docs worried that data on the stack is "invasive and stays forever". Two options:

| | Own component `adjustability:dormant_enchantments` | Inside `minecraft:custom_data` under key `adjustability:dormant` |
|---|---|---|
| Type safety | Codec-validated, `ItemEnchantments` directly | Manual (de)serialization of a tag |
| Network | Synced with its stream codec | Synced as part of `custom_data` |
| Mod removed | Unknown component type on load: **decide by test.** Vanilla may fail to decode the whole stack (item lost) or drop the component **(verify on 26.3, both loaders)** | Always loads. Data sits unused; item keeps its active enchantments. Harmless. |
| Other mods | Invisible unless they look | `custom_data` is visible to `/data`, some tooltip mods |

**Recommendation:** hide the choice behind `core/DormantEnchantments` (one class reads/writes it). Start with
**`custom_data`**, because "removing Adjustability must never delete a player's item" outranks elegance. Write the
"remove the mod" GameTest/manual test in M1; if the own component is proven safe on both loaders, switch before 1.0
(changing storage after 1.0 means migrating stacks forever). Either way, ship `/adjustability flatten` (§9.5) for admins
who want to uninstall cleanly.

What the player loses on uninstall: the dormant enchantments (by design: vanilla can't represent them). The item keeps
working with its active set.

---

## 7. The math: conflict graphs and independent sets

This replaces the old `Math.md` / `Data-Structure-Algorithms-Implementation.md` pages, and fixes the clique assumption
(audit #5). It goes on the docs site as `guide/math.md`.

### 7.1 The conflict graph of an item

For one item, let **V** be all enchantments it holds (active ∪ dormant). Draw an edge between `a` and `b` when
`!Enchantment.areCompatible(a, b)`. That's the item's **conflict graph G = (V, E)**. Edges are symmetric, never
reflexive, and **not transitive**:

```
Pickaxe:   Fortune ── Silk Touch                 (one edge)
Chestplate: Prot ─ Fire ─ Blast ─ Proj, all pairs (K4, a clique)
Trident:   Loyalty ── Riptide ── Channeling       (a path / star: Loyalty and Channeling are compatible)
Bow:       Infinity ── Mending
```

A valid **active set** is an **independent set** of G (no two active enchantments share an edge). A *configuration* the
player can pick is a **maximal** independent set (MIS): nothing dormant could be added without a conflict. The toggle UI
is a way to move between MISs.

### 7.2 How many configurations

- If G splits into components C₁…C_k (it always does: mining, armor, damage, boots... never touch), configurations
  multiply: `#MIS(G) = Π #MIS(Cᵢ)`.
- A clique Kₙ (Protections) has n MISs: pick one.
- The trident path `L – R – C` has 2 MISs: `{R}` or `{L, C}`. This is the case the old group model got wrong: activating
  Loyalty must *also* bring Channeling back, and activating Riptide must turn off both.
- Vanilla items have tiny graphs (|V| ≤ ~10, components ≤ 4–6 vertices), so brute force is fine. The theoretical worst
  case (Moon–Moser) is 3^(n/3) MISs; with a modded pack adding dozens of conflicting enchantments to one item, the
  module must not enumerate everything. The toggle algorithm below is linear in |V| + |E| and never enumerates.

### 7.3 The toggle operation

`activate(e)`: make dormant enchantment `e` active, with the smallest change.

```
active'  = (active \ N(e)) ∪ {e}            # N(e): e's neighbours, i.e. what it conflicts with
dormant' = (dormant \ {e}) ∪ (active ∩ N(e))
if auto_fill:
    for d in dormant' in stable order:       # registry order, then id
        if N(d) ∩ active' = ∅ and d ∉ never_dormant-violations:
            move d from dormant' to active'
```

Properties (each is a unit test in `graph/`):

- `active'` is independent (if `active` was).
- `active'` is maximal when `auto_fill` is on.
- Deterministic: same input → same output, on server and in the client preview.
- Idempotent: `activate(e)` twice = once.
- Reversible in one step for cliques: toggling back restores the previous active set exactly. For non-clique graphs it
  restores it after `auto_fill` (trident: R → {L,C} → R).
- `cycle(component)`: steps through the MISs of one component in a fixed order (for the in-world key, §9.2). For
  components small enough (≤ `max_enumerated_vertices`, default 12) MISs are enumerated with Bron–Kerbosch (pivoting)
  and cached per graph shape; above that, `cycle` falls back to `activate(next dormant vertex)`.

### 7.4 Precomputation and complexity (the old HashMap, corrected)

The 2024 doc stored `Map<Enchantment, Set<Enchantment>>`, n² strings for a clique of n. Keep the idea, but:

- Build it per **registry reload**, not per launch, from `Registries.ENCHANTMENT` and `areCompatible`: for N
  enchantments that's N(N−1)/2 checks once (vanilla ~42 → ~860 checks; trivial), cached in `ConflictResolver` and
  dropped on `/reload` / server stop.
- Store neighbour sets as `BitSet`s indexed by registry id: O(N²/64) words total, O(1) "do a and b conflict", fast
  `N(e) ∩ active`.
- Per item, the graph is the induced subgraph on its ≤ ~15 enchantments: built in O(|V|²) bit ops on demand, no
  allocation for items without the dormant component.

### 7.5 The `graph/` module API (sketch)

```java
public final class ConflictGraph<T>
{
        public static <T> ConflictGraph<T> of(Collection<T> vertices, BiPredicate<T, T> conflicts);
        public boolean isIndependent(Set<T> set);
        public Selection<T> activate(Selection<T> current, T vertex, boolean autoFill);
        public Selection<T> cycle(Selection<T> current, T anyVertexOfComponent);
        public List<Set<T>> components();
}

public record Selection<T>(Set<T> active, Set<T> dormant) {}
```

No Minecraft types, `T` is `Holder<Enchantment>` in `common` and `String` in tests.

---

## 8. Getting conflicting enchantments onto an item

### 8.1 Sources

| Source | Vanilla today | Adjustability |
|---|---|---|
| **Anvil: item + book** | Incompatible enchantments on the book are skipped (cost +1 each); if nothing applies, no result. | Incompatible ones are added as **dormant** (M1). The main path. |
| **Anvil: item + same item** | Same skipping. | Same as book; dormant sets of both inputs merge too. |
| **Anvil: book + book** | Same skipping. | **Combined** (config `combine_books`, default **on**, M1). The result is an *adjustable book*: every enchantment goes into vanilla's `stored_enchantments`, conflicts included (books have no effect, so there's nothing to keep dormant). Applying it to an item runs the normal merge (§8.2), which splits active/dormant. The tooltip marks the conflicting entries. Same cost rules as item + book. |
| **Enchanting table** | Rolls only compatible sets. | Untouched. |
| **Commands / loot / creative** | `/enchant` refuses incompatible; `/give` with components can do anything. | `/adjustability add <enchantment> [level]` for testing; anything that puts conflicting enchantments straight into `enchantments` (e.g. `/give`) is **repaired** on first toggle or anvil use: active set reduced to an MIS, the rest made dormant (never deletes). |
| **Villager trades / loot tables** | Books with one enchantment. | Untouched; books are the input to the anvil path. |

### 8.2 Anvil merge rules (applies to both inputs' active and dormant sets)

1. Union all enchantments from left (active + dormant) and right (`enchantments` + `stored_enchantments` + dormant).
   Same enchantment on both: vanilla level rules (equal levels → +1 up to max; else the higher).
2. Drop enchantments that can't go on the left item (`supportedItems`, vanilla rule), and those in
   `#adjustability:not_combinable` if they'd conflict (they keep vanilla behavior: skipped).
3. Keep the left item's current active choices where possible; new enchantments that conflict go **dormant**; new ones
   that don't conflict go active (vanilla behavior).
4. Cost = vanilla cost (including prior-work penalty, which still doubles per use) + `dormant_cost_per_level × level` for
   each enchantment that became dormant, capped by vanilla's "Too Expensive" rule unless `allow_too_expensive`.
5. Enforce `max_dormant_per_item` (default 4) and `max_conflicts_per_component` (0 = no limit). Over the limit: that
   enchantment is skipped like vanilla, with a tooltip-style hint in the anvil (client) via the result being absent.

### 8.3 Hooking the anvil on both loaders

`AnvilMenu.createResult()` is where vanilla merges. Options:

| Option | Fabric | NeoForge | Notes |
|---|---|---|---|
| A. Common mixin in `AnvilMenu.createResult` | ✅ | ✅ | One code path, one behavior. `@WrapOperation` on the `Enchantment.areCompatible` call inside it to record conflicts instead of skipping, then `@Inject` at the end to move conflicting ones to dormant and adjust cost. **Recommended.** |
| B. NeoForge `AnvilUpdateEvent` + Fabric mixin | mixin | event | Two implementations to keep in sync; the event replaces vanilla's whole computation, so cost/prior-work logic must be re-done by hand. |

Decision: **A**, MixinExtras only (no `@Redirect`/`@Overwrite`), per `CONTRIBUTING.md`. Exception to "prefer events"
documented in `CONTRIBUTING.md`, like the config one in Throwable Mobs. Other anvil mods (e.g. ones that raise the level
cap) usually also touch `createResult`; `@WrapOperation` chains with them.

### 8.4 Grindstone

`GrindstoneMenu` removes non-curse enchantments: also remove the dormant set and include its levels in the XP reward
(mixin at the result computation, **verify** method name on 26.3). Without this, grindstone → keep dormant → re-anvil is
a free way to strip prior-work cost (§13).

---

## 9. Toggling: controls, UI and networking

### 9.1 In the inventory: the Adjust screen

> **Superseded (2026-10-22):** shipped first as a separate screen built from vanilla buttons, then replaced at the
> user's request by the **Adjust panel**: a tooltip-like menu pinned next to the item inside the container screen, in
> the 2024 magenta style (dark panel, magenta gradient border, corner brackets). Left Alt pins it, a click on a row
> switches, Left Alt / Escape / a click outside closes it. Drawn with plain fills and text, no sprite. See "M2 state".

- Hover an item with dormant enchantments, press **Adjust** (default **Left Alt**; the 2024 Ctrl conflicts with vanilla
  Ctrl+Q / Ctrl-click in some screens and with many inventory mods — **verify**; rebindable under Controls →
  Adjustability).
- A small `Screen` opens over the container (vanilla widgets, no custom renderer), titled with the item name and showing
  one row per conflict component:

```
┌─ Diamond Pickaxe ─────────────────────┐
│ Mining     ( Fortune III ) [ Silk Touch ] │   ← radio-style CycleButton per component
│                                         │
│ Cost: 1 level  ·  Cooldown: 1s          │
│                       [ Done ]          │
└─────────────────────────────────────────┘
```

- Clicking a choice sends `ToggleEnchantmentPayload(containerSlot, enchantmentKey)`; the screen re-reads the stack when
  the server's slot update arrives (no client-side apply, no desync).
- The 2024 look (dark panel, magenta gradient border, corner brackets) becomes a nine-slice GUI sprite in
  `assets/adjustability/textures/gui/sprites/` drawn with `blitSprite`. Same identity, survives renderer rewrites, and
  resource packs can restyle it.
- Works in the player inventory and any container screen whose slots map to a menu slot (`AbstractContainerScreen`).
  Creative inventory tabs other than the survival one: **verify** slot mapping, else disabled there.

### 9.2 In the world: cycle key

- **Cycle enchantment** (default **unbound**, suggested `V`): cycles the first conflict component of the main-hand item
  to its next MIS (§7.3). With Alt held / a second key: next component.
- Feedback: action bar "Silk Touch active" + a soft enchant sound (client, configurable).
- Payload `CycleEnchantmentPayload(hand, componentIndex)`.

This is what makes the mod useful while mining: one key to switch Fortune ↔ Silk Touch without opening anything.

### 9.3 Payloads (server-side checks)

| Payload | Direction | Server validates |
|---|---|---|
| `ToggleEnchantmentPayload(int slot, ResourceKey<Enchantment> target)` | C → S | Player's open menu contains the slot and the player may take from it; stack has `target` **dormant**; item not in `#adjustability:not_adjustable`; cost + cooldown (§10); then `Toggler.activate`. |
| `CycleEnchantmentPayload(InteractionHand hand, int component)` | C → S | Hand has a stack with a dormant set; component index in range; cost + cooldown. |

Rate-limit both (cooldown also prevents macro-switching every tick to get "both" effects in practice, §13). Never
trust client indexes: re-derive components on the server.

### 9.4 Cost of toggling

Configurable (§10): free (default), XP levels or points, an item (e.g. 1 lapis, configurable item/tag), durability, and
a cooldown shown with the vanilla item cooldown overlay (`player.getCooldowns()`). Creative players: free.

### 9.5 Commands

| Command | Who | What |
|---|---|---|
| `/adjustability help` | Everyone | Lists commands |
| `/adjustability config` | Everyone | Opens player settings (client command, like Throwable Mobs) |
| `/adjustability keybinds` | Everyone | Opens Controls |
| `/adjustability toggle <enchantment>` | Everyone (configurable) | Toggle on the main-hand item; also the only way on vanilla clients if a server-only mode ever exists (§19, M8) |
| `/adjustability inspect` | Operators | Shows the main-hand item's graph: active, dormant, components, configurations |
| `/adjustability add <enchantment> [level]` | Operators | Adds an enchantment, dormant if it conflicts |
| `/adjustability flatten [<targets>]` | Operators | Removes the dormant component from all items in targets' inventories and ender chests (pre-uninstall cleanup) |

---

## 10. Configuration

Same system as Throwable Mobs: **Forge Config API Port** on Fabric, NeoForge's built-in on NeoForge, both specs in
`common/config`, read only through `AdjustabilityConfig`. Creator file synced, player file editable in game.

| Kind | Where | Synced? | Example |
|---|---|---|---|
| Creator file | `config/adjustability-server.toml` (+ per-world `serverconfig/` / `syncedconfig/` override) | Server → client | costs, limits, which sources are allowed |
| Player file | `config/adjustability-client.toml` | No | tooltip hint, sounds, screen position |
| Keys | Controls → Adjustability | No | Adjust, Cycle |
| Per-enchantment / per-item | Tags (§11) | Datapack sync | curses never dormant |

Server config sketch:

```toml
[anvil]
enabled = true
combine_books = true                # book + book with conflicting enchantments -> adjustable book
dormant_cost_per_level = 1          # extra anvil levels per level of each enchantment made dormant
allow_too_expensive = false         # survival 40-level cap still applies
max_dormant_per_item = 4            # 0 = no limit
max_conflicts_per_component = 0     # 0 = no limit; e.g. 2 = at most 2 Protections on one piece

[toggle]
enabled = true
auto_fill = true                    # bring back dormant enchantments that no longer conflict (trident)
cooldown_ticks = 20
cost_type = "NONE"                  # NONE | XP_LEVELS | XP_POINTS | ITEM | DURABILITY
cost_amount = 1
cost_item = "minecraft:lapis_lazuli"
allow_in_world_cycle = true
allow_command = true

[grindstone]
remove_dormant = true               # and give their XP
```

Player config sketch:

```toml
[tooltip]
show_dormant = true
show_hint = true
[feedback]
action_bar = true
sound = true
```

Naming rules from Throwable Mobs §11 apply: `snake_case`, one term per concept ("dormant" everywhere, never "inactive"
in one key and "disabled" in another), pick names before 1.0.

---

## 11. Datapacks and tags

Exclusivity itself is vanilla data: packs change it by editing `exclusive_set` in enchantment JSON or the
`#minecraft:exclusive_set/*` tags. Adjustability adds only its own policy tags:

| Tag | Type | Default contents | Meaning |
|---|---|---|---|
| `#adjustability:never_dormant` | enchantment | `#minecraft:curse` | Always active; can't be toggled off (so Curse of Binding/Vanishing can't be dodged by a modded conflict). |
| `#adjustability:not_combinable` | enchantment | empty | Conflicts involving these keep vanilla behavior (skipped in the anvil). Pack devs who think Infinity + Mending is too strong put `minecraft:infinity` here. |
| `#adjustability:not_adjustable` | item | empty | Items that never get a dormant set. |
| `#adjustability:adjustable` | item | `#minecraft:enchantable/...` union **(verify tag names)** | Optional allowlist mode via config `item_mode = DENYLIST | ALLOWLIST`. |

Convention tags: if `c:` has relevant enchantment tags by 26.3 **(verify)**, add them as default *contents* of our tags,
never checked in code (Throwable Mobs rule).

---

## 12. Java API

Small, in `io.github.safetimamovic.adjustability.api`, same rules as Throwable Mobs (Javadoc, `@ApiStatus`, semver).

| Call | Use |
|---|---|
| `AdjustabilityApi.getDormant(ItemStack)` / `getActive(ItemStack)` | Read the split (returns `ItemEnchantments`). |
| `AdjustabilityApi.activate(ServerPlayer, ItemStack, Holder<Enchantment>)` | Toggle from code (respects config unless `ToggleContext.free()`). |
| `AdjustabilityApi.addEnchantment(ItemStack, Holder<Enchantment>, int level)` | Adds, dormant if it conflicts. For mods that award enchantments. |
| `AdjustabilityApi.flatten(ItemStack)` | Remove the dormant set. |
| `AdjustableItem` interface on an `Item` | Opt an item out / customize (e.g. a tool that only allows one dormant). |
| Events: `ToggleEvent` (cancellable, before), `AnvilDormantEvent` (modify which go dormant) | Through a tiny loader-neutral event list in `api/` (no loader event bus needed). |

Enchantment-modifying mods that read `DataComponents.ENCHANTMENTS` need nothing: they see the active set.

---

## 13. Balance and exploits

The mod's value proposition depends on "toggle, never stack". Things that could break it:

| Risk | Decision (M3, 2026-10-22) | Covered by |
|---|---|---|
| Rapid switching to get effectively both (Fortune for ores, Silk Touch for glass) | Intended use. The cooldown (1 s default, `cooldown_ticks`) stops per-tick switching. **Switching mid-break is allowed, on purpose:** drops are worked out when the block breaks, from what's active then, so it never gives both; blocking it would only annoy. | `toggle_cooldown` GameTest |
| Armor: all four Protections "available" | Only one is ever active. Packs cap how many an item holds with `max_conflicts_per_component`. | Graph unit tests; `max_conflicts_per_component` in the anvil |
| Infinity + Mending | Allowed (one active at a time), decided 2026-10-22. Packs opt out with `#adjustability:not_combinable`. | Cookbook recipe |
| Grindstone keeps dormant / resets the prior-work penalty | The grindstone removes dormant enchantments too, and pays their experience like active ones. | `grindstone_removes_dormant`, `grindstone_dormant_experience` GameTests |
| Dupe via anvil output desync | The server computes the result; the mixin only changes vanilla's own calculation, no extra slots or transfers. | Anvil GameTests run the real menu |
| Packet spoofing (another player's container, a slot not open, a result slot, an enchantment not on the item) | Rejected silently by `ToggleNetwork` before `Toggler` runs; listed in `SECURITY.md`. | `toggle_network_checks_slot` GameTest |
| Curses dodged | `#adjustability:never_dormant` (curses by default) always stays active: first in every split, and a switch that would turn one off is refused. No vanilla curse conflicts with anything, so there's no vanilla case to test; a test needs a test datapack (later). | Code path only |
| Levels above max via merging dormant duplicates | Vanilla's level rule, capped at the max level; an enchantment is either active or dormant, never both. | `anvil_caps_merged_level` GameTest |
| Prior-work penalty bypass | Switching never touches `repair_cost`; anvil merges add vanilla's penalty. | `toggle_keeps_repair_cost` GameTest |
| Items with conflicting active enchantments (`/give`, other mods) | Repaired by the first switch or anvil use, never by deleting: what was active stays active where possible, the rest becomes dormant. The panel and Cycle key already show the repaired split. | `toggle_repairs_conflicting_active` GameTest |
| Items a pack doesn't want adjustable | `#adjustability:not_adjustable`, or `[items] mode = ALLOWLIST` with `#adjustability:adjustable`. | `allowlist_mode` GameTest |

---

## 14. Compatibility checklist

- [ ] Items without a dormant set are bit-for-bit vanilla (no component added, ever).
- [ ] Removing the mod never deletes items (§6.3), tested on both loaders.
- [ ] Modded enchantments work with zero config (registry + `areCompatible`).
- [ ] Packs that edit `exclusive_set` change behavior after `/reload`; the conflict cache is rebuilt on reload.
- [ ] Datapack removes an enchantment that's dormant on an item: unknown holder on load → **verify** vanilla behavior
      for `ItemEnchantments` with missing entries; don't crash, drop that entry with a log line.
- [ ] Anvil mods (level-cap raisers, Apotheosis-style overhauls): `@WrapOperation` chains; test with the popular ones
      on both loaders. If a mod fully replaces the anvil, document "anvil path off, use `/adjustability add` or their
      mechanic".
- [ ] Enchantment-overhaul mods that remove exclusivity entirely: nothing to adjust; no conflict, no harm.
- [ ] Inventory mods (Mouse Tweaks, Inventory Profiles Next, Item Scroller...): Adjust key doesn't clash by default
      **(verify)**; screen opens over any `AbstractContainerScreen`.
- [ ] Tooltip mods (Enchantment Descriptions, Legendary Tooltips, EMI/JEI hover): dormant lines are plain components
      added through the loader event, after vanilla lines.
- [ ] Recipe viewers: none needed; maybe an info page in EMI/JEI later.
- [ ] Dedicated server: no `net.minecraft.client` reachable from common/server paths. Test with `runServer` (audit #8).
- [ ] Mixins minimal (anvil, grindstone), MixinExtras only.
- [ ] Required on both sides (client UI + server logic). Document it. A vanilla client on a modded server would see
      items with their active enchantments only and no toggle; with `custom_data` storage, no crash.

---

## 15. Testing

| Kind | Where | What |
|---|---|---|
| Unit (JUnit 5) | `graph/src/test` | `activate`/`cycle` properties from §7.3 on: single edge, K4, trident path, two components, random graphs (property-style: independence + maximality + determinism over 10k random graphs with a fixed seed). |
| GameTests | `common` (registered per loader) | Anvil: Fortune pickaxe + Silk book → Fortune active, Silk dormant, cost = vanilla + dormant cost. Book + book → one book with all stored enchantments; that book on a pickaxe splits active/dormant. Toggle via `Toggler` flips sets and charges cost. Cooldown blocks a second toggle. Grindstone clears dormant + XP. Curse can't go dormant. `/give` with conflicting active enchantments is repaired, nothing lost. `/reload` with edited `exclusive_set` changes the graph. |
| Manual | `runClient`, `runServer`, `runClient2` | Adjust screen in inventory/chest/shulker, cycle key while mining, multiplayer desync check (two clients watching the same chest), uninstall test (§6.3). |
| Modpacks | Prism Launcher | One big NeoForge pack, one Fabric pack with anvil/enchantment mods. |

Same CI as Throwable Mobs: `./gradlew build` runs unit tests; GameTests headless in CI once the 26.3 framework is
wired (shared with Throwable Mobs' setup).

---

## 16. Repo files, CI and publishing

Copy from Throwable Mobs, then change only what's listed:

| File | Change |
|---|---|
| `README.md` | Same structure: centered icon, title, one-line pitch, badges (Build, release, docs, MIT), links row, "Minecraft Java **26.3** · **Fabric** + **NeoForge** · MIT · **Beta**", install notes (Fabric: Fabric API + Forge Config API Port), "What it does", Controls, Commands, Configuration, Issues, Building, Layout (`graph/` instead of `physics/`), Contributing, "Based on MultiLoader-Template". Remove the bordered `<div>`s and the personal school email from the old README; contact via issues. |
| `CHANGELOG.md` | Fresh, `## [Unreleased]`. First release section: `## [0.1.0] - <date>`. Note "Complete rewrite of the 2024 1.20.6 prototype" under 0.1.0. |
| `CONTRIBUTING.md` | Search/replace names; scopes list (§4); "Suggesting features & mob behaviors" → "Suggesting features & enchantment compat"; mixin exception for anvil/grindstone; `graph` module instead of `physics` in Testing. |
| `SECURITY.md` | Examples: a packet that toggles enchantments on items the player can't access, activates enchantments not on the item, dupes items or enchantments in the anvil/grindstone. |
| `.github/ISSUE_TEMPLATE/*` | URLs → Adjustability; replace `mob_behavior.yml` with `enchantment_compat.yml` (fields: enchantment + mod, item, what happened when combined/toggled, other enchantment mods installed). |
| `.github/workflows/build.yml` | Artifact `adjustability-jars`. |
| `.github/workflows/release.yml` | Release name "Adjustability ${version}", jar globs `adjustability-{fabric,neoforge}-*-${version}.jar`. |
| `.github/workflows/docs.yml` | Unchanged. Enable Pages (Settings → Pages → Source: GitHub Actions). |
| `.github/dependabot.yml`, `.editorconfig`, `.gitattributes`, `.gitignore` | Unchanged. |
| `package.json` | `"name": "adjustability-docs"`, description. |
| `LICENSE` | MIT, "Copyright (c) 2024-2026 SafetImamovic". |

Jar naming (from `multiloader-common.gradle`): `adjustability-fabric-26.3-0.1.0.jar`, `adjustability-neoforge-26.3-0.1.0.jar`.

Publishing: same as Throwable Mobs (GitHub Release by tag, Modrinth by hand, `mc-publish` later). Mod page lives or dies
on one GIF: Fortune pickaxe mining diamonds, one key press, Silk Touch mining glass.

Repo hygiene: the GitHub repo already exists (`SafetImamovic/Adjustability`); its Pages site currently serves the
Writerside build. The docs workflow replaces it at the same URL (`safetimamovic.github.io/Adjustability/`).

---

## 17. Public documentation (VitePress)

Same tooling and files as Throwable Mobs: VitePress 1.6.4, `docs/`, deployed to GitHub Pages by `docs.yml`, local search,
"edit this page" links, `lastUpdated`, `guide/README.md` rewritten to `guide/index.md` so it reads on GitHub too,
`docs/plans/` gitignored and excluded with `srcExclude`.

### 17.1 Pages

| Page | For | Content | Source from old docs |
|---|---|---|---|
| `index.md` (home) | Everyone | Hero "Adjustability — Every enchantment. One at a time.", 4 features (Toggle, No stacking, Your rules, Modded enchantments just work), Install table, Controls, Commands. | README |
| `guide/README.md` | Pack devs, admins, mod devs | Page table, "three ways to change it": config, datapacks (tags + vanilla `exclusive_set`), Java. | — |
| `guide/how-it-works.md` | Players | Active vs dormant, anvil, Adjust screen, cycle key, tooltip, what grindstones do, what happens if the mod is removed. GIFs. | `starter.md` "Mechanics" |
| `guide/server-config.md` | Admins | Every key of `adjustability-server.toml` with defaults and examples (format of Throwable Mobs' page). | — |
| `guide/tags.md` | Pack devs | Our four tags, defaults, examples; how vanilla `exclusive_set` works and how to edit it. | `Vanilla-Data.md` (rewritten for 1.21+ data-driven enchantments) |
| `guide/modded-enchantments.md` | Pack devs, mod devs | "Works automatically", how to check with `/adjustability inspect`, known compat notes. | — |
| `guide/math.md` | Curious players, contributors | §7 of this file: conflict graph, independent sets, toggle, counting configurations, complexity. Diagrams as Mermaid (VitePress plugin `vitepress-plugin-mermaid`) or the existing SVGs redrawn. | `Math.md`, `Data-Structure-Algorithms-Implementation.md`, the SVGs (corrected: graph, not groups) |
| `guide/java-api.md` | Mod devs | §12 with code samples. | Class diagrams (dropped; Javadoc replaces them) |
| `guide/cookbook.md` | Everyone | "Make toggling cost 1 lapis", "Ban Infinity + Mending", "Allow at most 2 Protections", "Only netherite tools are adjustable", "Hardcore preset", "Uninstall cleanly with `/adjustability flatten`". | — |

### 17.2 `docs/.vitepress/config.mts` (Throwable Mobs' file, changed fields)

```ts
const repo = 'https://github.com/SafetImamovic/Adjustability'

export default defineConfig({
  title: 'Adjustability',
  description: 'Hold mutually exclusive enchantments on one item and switch between them. Docs for players, pack devs and mod devs.',
  base: '/Adjustability/',
  head: [['link', { rel: 'icon', type: 'image/png', href: '/Adjustability/adjustability.png' }]],
  themeConfig: {
    logo: '/adjustability.png',
    nav: [
      { text: 'Guide', link: '/guide/' },
      { text: 'Cookbook', link: '/guide/cookbook' },
      { text: 'Links', items: [ /* releases, changelog, report a bug: same as Throwable Mobs */ ] },
    ],
    sidebar: {
      '/guide/': [
        { text: 'Guide', items: [{ text: 'Overview', link: '/guide/' }, { text: 'How it works', link: '/guide/how-it-works' }] },
        { text: 'Server admins', items: [{ text: 'Server config', link: '/guide/server-config' }] },
        { text: 'Pack devs', items: [
          { text: 'Tags and exclusive sets', link: '/guide/tags' },
          { text: 'Modded enchantments', link: '/guide/modded-enchantments' },
        ] },
        { text: 'Mod devs', items: [{ text: 'Java API', link: '/guide/java-api' }] },
        { text: 'Background', items: [{ text: 'The math', link: '/guide/math' }] },
        { text: 'Examples', items: [{ text: 'Cookbook', link: '/guide/cookbook' }] },
      ],
    },
    // socialLinks, search, editLink, footer: identical to Throwable Mobs
  },
})
```

### 17.3 Theme

`theme/style.css`: same structure, brand color from the old tooltip magenta instead of orange. Contrast-checked like
Throwable Mobs' (≥ 4.5:1): light `--vp-c-brand-1: #a3159a` (darker magenta), dark `--vp-c-brand-1: #f07ae6`; hero
gradient `#ff00ff → #7b5cff` (enchantment glint purple). Keep `image-rendering: pixelated` for the icon.

### 17.4 Generated references (later, same plan as Throwable Mobs §17)

`generateDocs` task: config reference from the spec, default tag contents, and a **vanilla conflict table** generated
from the 26.3 enchantment registry (every enchantment, its `exclusive_set`, its components), so `tags.md` never drifts.

---

## 18. Migration: from the old repo to the new layout

Keep the repo and its history (stars, issues, Pages URL); replace the contents.

### 18.1 Steps

1. `git switch master && git tag legacy-1.20.6-forge 353e60c && git push origin legacy-1.20.6-forge` — the prototype
   stays reachable forever.
2. `git switch -c chore/multiloader-26.3`.
3. Delete: `src/`, `AdjustabilityDocs/`, `docs/` (Writerside HTML), `build.gradle`, `settings.gradle`,
   `gradle.properties`, `gradle/`, `gradlew*`, `.gitignore`, `.gitattributes`.
4. Copy from `../ThrowableMobs` (no `.git`, `build/`, `.gradle/`, `node_modules/`, `runs/`, `.settings/`, `.project`):
   `build-logic/`, `gradle/`, `gradlew`, `gradlew.bat`, `build.gradle`, `settings.gradle`, `gradle.properties`,
   `.editorconfig`, `.gitattributes`, `.gitignore`, `.github/`, `package.json`, `docs/.vitepress/`,
   `common/build.gradle`, `fabric/build.gradle`, `neoforge/build.gradle`, `physics/build.gradle` → `graph/build.gradle`,
   `CONTRIBUTING.md`, `SECURITY.md`, `LICENSE`. Then run `npm install` to regenerate `package-lock.json` for the new
   package name.
5. Rename `physics` → `graph` in `settings.gradle`, `common/build.gradle` (`api(project(':graph'))`) and
   `multiloader-loader.gradle` (`physicsJava` → `graphJava`, three places plus the configuration).
6. Fill `gradle.properties` (§18.3).
7. Create the Java skeleton: `Constants`, `Adjustability.init()`, `platform/Services` + `IPlatformHelper` with Fabric and
   NeoForge implementations and `META-INF/services` files, the two entrypoints and two client entrypoints,
   `fabric.mod.json`, `neoforge.mods.toml`, both mixin JSONs (empty), `pack.mcmeta`, `en_us.json`, icon. All copied from
   Throwable Mobs and stripped of Throwable Mobs logic.
8. `./gradlew build`, `:fabric:runClient`, `:neoforge:runClient`, `:fabric:runServer` — the M0 bar.
9. README, CHANGELOG, this file, `docs/` pages as stubs; `npm run docs:build` passes.
10. PR `chore/multiloader-26.3` → `master` (GitHub flow from here on), atomic commits, e.g.:
    `chore: tag and remove the 1.20.6 forge prototype` · `build: add the multiloader 26.3 gradle setup` ·
    `build: add the graph module` · `feat: add common, fabric and neoforge entrypoints` ·
    `docs: add the vitepress site` · `ci: add build, docs and release workflows` · `docs: add the design and roadmap doc`.

### 18.2 What to carry over from the old code

Nothing is copied verbatim. Reused as ideas: the conflict lookup (§7.4), the tooltip look (§9.1 sprite), the doc text
explaining the mechanics and why stack data beats per-item databases (rewritten into `how-it-works.md`, `math.md`).

### 18.3 `gradle.properties`

```properties
# Important Notes:
# Every field you add must be added to build-logic/src/main/groovy/multiloader-common.gradle expandProps map.

# Project
version=0.1.0
group=io.github.safetimamovic.adjustability
java_version=25

# Common
minecraft_version=26.3
mod_name=Adjustability
mod_author=SafetImamovic
mod_id=adjustability
license=MIT
credits=
description=Hold mutually exclusive enchantments on one item and switch between them. Fortune or Silk Touch, one pickaxe, never both at once.
minecraft_version_range=[26.3, 26.4)
homepage=https://github.com/SafetImamovic/Adjustability
sources=https://github.com/SafetImamovic/Adjustability
issues=https://github.com/SafetImamovic/Adjustability/issues
neo_form_version=26.3-1

# Fabric
fabric_version=0.161.0+26.3
fabric_loader_version=0.19.5

# Forge Config API Port
forgeconfigapiport_version=26.3.1
modmenu_version=21.0.0

# NeoForge
neoforge_version=26.3.0.41-beta
neoforge_loader_version_range=[4,)

# Testing
junit_version=5.13.4

# Gradle
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=false
```

Bump Fabric API / NeoForge together with Throwable Mobs (same versions in both repos makes testing them in one instance
trivial). `group` and the Java package stay `io.github.safetimamovic.adjustability`, the same as 2024.

---

## 19. Roadmap

| Milestone | Scope | Done when |
|---|---|---|
| **M0 — Setup** | §18 migration: multiloader 26.3 build, `graph/` module, entrypoints, platform services, CI, docs site skeleton, legacy tag. | `./gradlew build` green in CI; both loaders load the mod on client and dedicated server; docs site live. |
| **M1 — Data model + anvil** | `graph/` with tests (§7); `ConflictResolver` cache rebuilt on reload; `DormantEnchantments` storage (§6.3, including the uninstall test that decides the container); anvil mixin (§8.2–8.3) for item + book, item + item and book + book; grindstone (§8.4); invariants; tooltip dormant lines; server config `[anvil]`, `[grindstone]`; `/adjustability inspect`, `add`. | Fortune pickaxe + Silk book in survival → Fortune active, Silk dormant, sane cost, on both loaders. GameTests for it. |
| **M2 — Toggle** | Payloads + `Toggler` with all checks (§9.3); Adjust screen (§9.1) with the sprite; key mappings (Adjust, Cycle) in their own category; cycle key with action bar (§9.2); costs + cooldown (§9.4); player config + in-game screen; `/adjustability toggle`, `flatten`, `help`, `config`, `keybinds`. | Toggle works in inventory, chests and in the world, single-player and dedicated server with two clients, no desync. |
| **M3 — Rules & tags** | `never_dormant`, `not_combinable`, `not_adjustable`, allowlist mode; limits (`max_dormant_per_item`, `max_conflicts_per_component`); `/give`-repair; exploit pass (§13). | Every row of §13 has a test or a documented decision. |
| **M4 — API** | `api/` package (§12), events, `AdjustableItem`, Javadoc, `@ApiStatus`. Own anvil/toggle code goes through it. | A sample mod in `docs/` compiles against it. |
| **M5 — Release 0.1.0 (beta)** | Docs pages written (§17.1), GIFs, CHANGELOG, Modrinth page, GitHub release via tag. Compat pass with popular anvil/tooltip/inventory mods (§14). | Tagged `v0.1.0`, jars on GitHub + Modrinth. |
| **M6 — Polish** | Sounds, advancements ("Indecisive": toggle 100 times; "Best of Both": hold every Protection on one chestplate), EMI/JEI info page, translations, generated docs references (§17.4). | — |
| **M7 — Backport 1.21.1** | Same as Throwable Mobs M5: Stonecutter. 1.21.1 already has data-driven enchantments and `exclusive_set`, so the port is mostly `ResourceLocation`/mapping differences. Share the Stonecutter setup with Throwable Mobs. | 1.21.1 jars for both loaders. |
| **M8 — Server-only mode** (optional) | Works without the client mod: `/adjustability toggle` and a chest-GUI menu (vanilla `ChestMenu` with item icons) for vanilla clients; or a Paper plugin sharing `graph/` (like Throwable Mobs M10). | Decide after 0.1.0 based on demand. |
| **M9 — 1.0** | API frozen, storage decision final, docs versioned, CurseForge page, `mc-publish`. | `v1.0.0`. |

Effort guess, for planning only: M0 one evening (it's copying), M1 and M2 are the real work (a couple of weekends
each), M3–M5 another couple of weekends. The mod is much smaller than Throwable Mobs: no physics, no entity state, one
item component and two mixins.

---

## 20. Decisions

Settled 2026-10-22:

| Topic | Decision |
|---|---|
| Mod id / name / package | `adjustability` / "Adjustability" / `io.github.safetimamovic.adjustability` (unchanged from 2024). |
| Versions | **26.3 first**, 1.21.1 backport after 0.1.0 (M7). No 1.20.x: enchantments weren't data-driven yet, it'd be a different mod. |
| Loaders | Fabric + NeoForge. **No MinecraftForge** (the 2024 target). |
| Layout | Identical to Throwable Mobs: MultiLoader-Template, `build-logic/`, `common/`, `fabric/`, `neoforge/`, pure-Java `graph/` module. |
| Core rule | Toggle, never stack. Active set always an independent set of the conflict graph. |
| Exclusivity source | Vanilla `exclusive_set` via `Enchantment.areCompatible`. No own exclusivity data. |
| Storage | On the stack. `custom_data` first; own component only if the uninstall test proves it safe on both loaders (§6.3). No per-world database. |
| Anvil hook | One common MixinExtras mixin into `AnvilMenu` for both loaders (§8.3). |
| UI | Vanilla `Screen` + widgets + GUI sprite. No custom render code. |
| Default key | Adjust: **Left Alt** while hovering. Cycle: unbound. Check clashes with inventory mods during M2. |
| Config | Forge Config API Port, creator (synced) + player file, same as Throwable Mobs. |
| Defaults | Balanced survival: toggling free with 1 s cooldown, anvil +1 level per dormant level, max 4 dormant per item, curses never dormant, book + book **on**, Infinity + Mending **allowed** (one active at a time; packs opt out with `#adjustability:not_combinable`). |
| License | **MIT** (was "All Rights Reserved"). |
| Branching / commits / releases | GitHub flow, Conventional Commits, tags `v*`, Keep a Changelog — exactly `CONTRIBUTING.md` from Throwable Mobs. |
| Docs | VitePress on GitHub Pages at the existing `safetimamovic.github.io/Adjustability/` URL. Writerside dropped. |
| Old code | Tagged `legacy-1.20.6-forge`, removed from `master`. |
| Repository | **Keep `SafetImamovic/Adjustability`** and rewrite in place (§18): keeps history, stars, the Pages URL and the name. The prototype stays reachable through the tag. |

### Current setup state (M0 done, 2026-10-22)

- Prototype tagged `legacy-1.20.6-forge` and pushed; rewrite on branch `chore/multiloader-26.3`.
- Throwable Mobs layout: `build-logic/`, `graph/`, `common/`, `fabric/`, `neoforge/`, same pinned versions (MC 26.3,
  Fabric Loader 0.19.5, Fabric API 0.161.0+26.3, NeoForge 26.3.0.41-beta, Loom 1.18.2, ModDevGradle 2.0.148, Gradle
  9.7.1, Java 25).
- `./gradlew build` produces `adjustability-{fabric,neoforge}-26.3-0.1.0.jar`; `graph/` has `ConflictGraph` with 5 unit
  tests (edge, clique, trident path, components, unknown vertices). The toggle operation (§7.3) comes in M1.
- Both loaders verified loading the mod on a dev dedicated server.
- Docs site (VitePress) builds; pages for unbuilt parts are marked planned.
- Mod icon `adjustability.png` (512×512, PNG: in-game mod lists don't animate GIFs) in both loaders' metadata, the
  README and the docs site.
### M1 state (anvil and data model, 2026-10-22)

- **Storage decided: `custom_data`** under `adjustability:dormant`, as plain enchantment id → level (§6.3). Inherently
  safe on uninstall (vanilla component, plain ids, no registry codec at load), so the component experiment was skipped.
  `core/DormantEnchantments` is still the only class that knows the format.
- `graph/`: `select` (keep preferred active, maximal independent set) and `activate` (§7.3), 11 unit tests including a
  10,000-graph property test.
- Anvil: common mixin. `@WrapOperation` on `Enchantment.areCompatible` in `createResult` / NeoForge's
  `createResultInternal` lets conflicts through, so vanilla's own level rules and prices apply; a `TAIL` inject on
  `createResult` splits the result (on NeoForge after `AnvilUpdateEvent`). Item + book, item + item and book + book.
  What was active on the left stays active; with nothing active, id order decides. Limits make the anvil give no
  result rather than drop enchantments.
- Grindstone: dormant removed with the active ones. **Not done:** experience for dormant enchantments (vanilla's XP code
  sits in an anonymous slot class; NeoForge has `GrindstoneEvent`); revisit in M3.
- Tooltip: dormant lines after the active enchantments (mixin into `ItemEnchantments.addToTooltip`), not yet under
  their conflict partner (§6.2 polish, M6).
- Server config `[anvil]` + `[grindstone]` through Forge Config API Port; commands `inspect`, `add`.
- 6 GameTests (vanilla framework, mock survival player driving the real menus) pass on both loaders:
  `./gradlew :fabric:runGametest :neoforge:runGameTestServer`, also in CI. They need no EULA.

### M2 state (switching, 2026-10-22)

- Server: `core/Toggler` holds every check (item can take part, target dormant, no curse switched off, cooldown,
  cost) for all three ways in: the Adjust panel, the Cycle key and `/adjustability toggle`. `network/ToggleNetwork`
  re-checks the payload: the menu id must be the open one, the slot one the player could take from, never a result
  slot. Cooldown is per player (server ticks), not vanilla's item cooldown, so the tool itself isn't blocked.
- Client: the **Adjust panel**, a tooltip-like menu pinned next to the item inside the container screen, in the 2024
  magenta style. One row per enchantment per conflict group (● active, ○ dormant, hovered row highlighted), cost line
  at the bottom. Rows are rebuilt from the slot's item every frame, so it shows the server's state. A client mixin on
  `AbstractContainerScreen` gives it the hovered slot and screen position, the Adjust key and Escape, mouse clicks
  (a click outside closes it and never reaches the slots), and draws it from `extractTooltip`, which every container
  screen calls last (recipe book screens skip the base `extractRenderState`). Drawn with `GuiGraphicsExtractor` fills
  and text only (26.x's renamed GUI drawing). Off in the creative inventory (client-only menu). It replaced a first
  version that was a separate screen of vanilla buttons. Its look is the player's: `[panel]` in
  `adjustability-client.toml` (side, every color, brackets on/off and length, cost line), editable in the config
  screen or with `/adjustability panel [<setting> <value>|reset]`. The command tree is built from the config spec
  (`PlayerConfig.PANEL_SETTINGS`) by `PanelCommand`, once for the client and once as a server stub for suggestions.
- Cycle key: steps the first conflict group of the main-hand item (`ConflictGraph.next`). **Not done:** choosing
  another group (§9.2's modifier), blocking switches mid-block-break (§13).
- Tests: 13 GameTests (6 new: switch, trident auto fill, refusal, cooldown, payload slot checks, cycle) on both loaders,
  and one **Fabric client GameTest** (`./gradlew :fabric:runClientGametest`) that opens the inventory in a real client,
  hovers, presses Left Alt, clicks the Silk Touch row, checks the server (switched, inventory still open, nothing
  picked up), closes the panel with Left Alt, and saves screenshots. NeoForge has no client test
  framework; its client was checked to start, and the screen code is the same common code. The client test isn't in
  CI yet (needs a virtual display).
- Player config `adjustability-client.toml` (`show_dormant`, `show_hint`) with the in-game config screen (NeoForge
  mod list, Mod Menu on Fabric, `/adjustability config`).

### M3 state (rules and exploits, 2026-10-22)

- Every row of §13 has a decision and a test, or says why not (curses: no vanilla case).
- `[items] mode` (DENYLIST / ALLOWLIST) and `#adjustability:adjustable`; one check, `core/ItemRules`, used by the
  anvil, switching, the command and the panel. Books follow `combine_books`, not the item rules.
- Repair: `Toggler` and the Cycle key work on the repaired split (`Adjuster.resplit`), so an item with conflicting
  active enchantments is fixed by its first switch, losing nothing.
- Grindstone experience for dormant enchantments: a mixin on the result slot (`GrindstoneMenu$4`, the same anonymous
  class on both loaders) adds them where vanilla adds up active ones. The GameTest samples the real method 40 times;
  with the mixin removed it fails (average 26 instead of about 36).
- Sneak + Cycle picks which conflict group the Cycle key steps, shown above the hotbar; covered by the client
  GameTest (boots: Depth Strider / Frost Walker, then the Protections).
- 18 GameTests on both loaders.

### After M3: server-defined conflicts and per-group costs (2026-10-22)

- `core/Exclusivity` is the one place conflicts are decided: vanilla's `exclusive_set` (`[exclusivity] use_vanilla`),
  plus `extra_groups`, minus `compatible`; entries are ids and `#tags`. `Adjuster.conflict` delegates to it, so the
  anvil split, switching, the panel and the Cycle key all follow it. Vanilla's own systems (enchanting table,
  `/enchant`) still use `exclusive_set`.
- `[toggle] group_costs`: per-group `Cost` of any type (`NONE`, `XP_LEVELS`, `XP_POINTS`, `DURABILITY`, `ITEM n
  item`); the first matching entry wins, else the default cost. The panel shows per-group costs when they differ.
- Docs: `guide/storage.md` (exact format, a `/give` example checked by a GameTest) and `guide/uninstalling.md`
  (flatten, what it doesn't reach), linked from the home page, the README and the sidebar.
- 22 GameTests on both loaders; the Fabric client GameTest sets `group_costs` through the config spec and checks a
  mining switch takes one lapis.

### M4 state (Java API, 2026-10-22)

- `api/`: `AdjustabilityApi` (read active/dormant, `conflict`, `canHoldDormant`, `addEnchantment`, `activate`,
  `flatten`, listeners), `AdjustabilityListener` (`allowSwitch`, `onSwitched`, `allowAnvilResult`), `SwitchEvent`,
  `AnvilEvent`, `SwitchResult`, `SwitchSource`, `AdjustableItem`. All `@ApiStatus.Experimental` until 1.0, with Javadoc.
  The roadmap's "modify which go dormant" anvil event became a veto (`allowAnvilResult`): simpler, and enough so far.
- The mod's own code goes through the same paths: the panel, Cycle key and command call `Toggler.activate` with their
  `SwitchSource`, which asks the listeners; the anvil asks them before splitting; `ItemRules` consults
  `AdjustableItem`. Listener failures are logged and count as allowing.
- The docs page's examples are a real class (`gametest/ApiExamples`) the GameTests run. 26 GameTests on both loaders.
- `AdjustableItem` is tested with `gametest/TestAdjustableItem`, registered only in the development environment
  (`Services.PLATFORM.isDevelopmentEnvironment()`); the `Registrar` takes suppliers so NeoForge creates it during
  its register event.
- `common:apiJar` / `apiSourcesJar`: `adjustability-api-<mc>-<version>.jar` with only the `api` package, built with
  `build`, attached to releases and CI artifacts. No Maven repository yet (M9).
- `[panel] activation = TOGGLE | HOLD` (player setting and `/adjustability panel activation`); HOLD closes on the
  key's release through a `keyReleased` override the client mixin adds to `AbstractContainerScreen` (only the
  creative screen overrides it, where the panel is off). Covered by the client GameTest.
- `[toggle] allow_none`: a group may have nothing active. The rule "every dormant enchantment conflicts with an
  active one" holds only when it's off. `Adjuster.normalize` (switching, the panel, `inspect`) and `Adjuster.add`
  (command, API) then only fix conflicts and never activate a dormant enchantment; the anvil keeps what was dormant on
  the left item dormant; `ConflictGraph.activate` only auto fills the target's own group. None rows in the panel,
  `Toggler.deactivate`, a none step in the Cycle key (`isLastChoice`), `AdjustabilityApi.deactivateGroup`,
  `SwitchEvent.none`, `SwitchResult.NONE_NOT_ALLOWED`.
- `[toggle] panel_activation = PLAYER | TOGGLE | HOLD`: the server can decide the Adjust key's mode for everyone.
- NeoForge client: no client test framework there; a dev run that force-loads the container screen confirms the
  client mixin applies to NeoForge's patched class. The panel itself is only driven on Fabric. 27 GameTests on both
  loaders.

### M5 state (0.1.0 beta, 2026-10-22)

- Fabric API 0.164.0+26.3 and NeoForge 26.3.0.95 (first stable; mods.toml now requires it), both needed by the compat
  mods below.
- Compatibility pass: Easy Anvils + Puzzles Lib and JEI on Fabric and NeoForge, plus Mouse Tweaks, AppleSkin and
  Enchantment Descriptions (+ Bookshelf, Prickle) on Fabric, downloaded from Modrinth with their sha512 checked. All
  31 GameTests pass with them on both loaders, and the Fabric client GameTest passes with 66 mods loaded. Results on
  `guide/compatibility.md`. NeoForge's client mixin checked again on 26.3.0.95.
- Docs: beta wording, screenshots (from the client GameTest), the compatibility page, a changelog grouped by audience.
- Release: tag `v0.1.0` builds both loaders and the API jar into a GitHub release. Modrinth/CurseForge uploads are by
  hand (description text prepared outside the repo). No GIFs yet.

---

## 21. Open questions

1. ~~**Storage container**~~: decided, `custom_data` (see M1 state above).
2. **Armor toggling in combat**: should switching armor be blocked while the player took damage in the last N seconds?
   Not done in M3: the cooldown already limits it; revisit if players report it.
3. **Server-only mode** (M8): demand check after the beta.
4. **Name collision**: search Modrinth/CurseForge for "Adjustability" before the first public release.

Resolved 2026-10-22: license MIT, Adjust key Left Alt, Infinity + Mending allowed, book + book in 1.0 (default on),
keep this repository.

---

## 22. Links

- Throwable Mobs repo (template, conventions, `THROWABLE_MOBS.md` §1–5 for ecosystem setup): `../ThrowableMobs`
- Enchantment definition format (1.21+): https://minecraft.wiki/w/Enchantment_definition
- Enchantment tags / `exclusive_set`: https://minecraft.wiki/w/Tag_(Java_Edition)#Enchantments
- Data components: https://minecraft.wiki/w/Data_component_format
- Anvil mechanics and costs: https://minecraft.wiki/w/Anvil_mechanics
- NeoForge docs (data components, events, game tests): https://docs.neoforged.net
- Fabric docs: https://docs.fabricmc.net
- MultiLoader-Template: https://github.com/jaredlll08/MultiLoader-Template
- MixinExtras: https://github.com/LlamaLad7/MixinExtras
- Forge Config API Port: https://modrinth.com/mod/forge-config-api-port
- Maximal independent sets, Bron–Kerbosch: https://en.wikipedia.org/wiki/Bron%E2%80%93Kerbosch_algorithm
- Moon–Moser bound: https://en.wikipedia.org/wiki/Maximal_independent_set#Number_of_maximal_independent_sets
- VitePress: https://vitepress.dev · Keep a Changelog: https://keepachangelog.com · Conventional Commits:
  https://www.conventionalcommits.org
