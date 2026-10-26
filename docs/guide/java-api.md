# Java API

For mod devs who want to read or change an item's active and dormant enchantments, switch from code, veto switches
and anvil results, or add a row mark to the Adjust panel. Everything is in `io.github.safetimamovic.adjustability.api`
and works the same on Fabric and NeoForge; call it from your common code, except [row marks](#row-marks-client), which
are client-only.

> **Experimental until 1.0.** The API is marked `@ApiStatus.Experimental`: names may still change.

## Depending on it

Every [release](https://github.com/SafetImamovic/Adjustability/releases) has an `adjustability-api-<mc>-<version>.jar`
(and its `-sources.jar`) with just the `api` package. Build against it `compileOnly`, and put the full mod jar (Fabric
or NeoForge) in your dev runtime. There's no Maven repository yet; drop the jars in a `libs/` folder:

```groovy
dependencies {
    compileOnly(files('libs/adjustability-api-26.3-0.1.0.jar'))
    // Fabric (Loom): localRuntime(files('libs/adjustability-fabric-26.3-0.1.0.jar'))
    // NeoForge (ModDevGradle): runtimeOnly(files('libs/adjustability-neoforge-26.3-0.1.0.jar'))
}
```

Players install Adjustability themselves; don't bundle it.

**Most mods need none of this.** Anything that reads enchantments the vanilla way (`DataComponents.ENCHANTMENTS`,
`EnchantmentHelper`) already sees exactly the active ones, and dormant ones do nothing. See
[where dormant enchantments are stored](storage.md) for the data itself.

## `AdjustabilityApi`

| Method | What it does |
|---|---|
| `activeEnchantments(stack)` | The active enchantments (vanilla's component) |
| `dormantEnchantments(stack, registries)` | The dormant ones, waiting behind an active enchantment they conflict with; empty for most items |
| `switchedOffEnchantments(stack, registries)` | The ones the player switched off (they conflict with nothing) |
| `hasDormant(stack)` | Cheap check, no registries needed |
| `conflict(a, b)` | Whether two enchantments conflict for Adjustability: vanilla's `exclusive_set` plus the server's `[exclusivity]` rules |
| `canHoldDormant(stack)` | Tags, `[items] mode` and `AdjustableItem` together |
| `addEnchantment(stack, enchantment, level, registries)` | Adds it: active if it conflicts with nothing active, dormant otherwise. Ignores costs, limits and listeners |
| `activate(player, stack, enchantment)` | Switches as if the player clicked it in the Adjust panel: cooldown, cost, curses and listeners all apply. Returns a `SwitchResult` and tells the player nothing |
| `deactivateGroup(player, stack, anyInGroup)` | The None choice: leaves that enchantment's group with nothing active. Only if the server has `allow_none`, else `SwitchResult.NONE_NOT_ALLOWED`. On an enchantment that conflicts with nothing it switches it off (`allow_switch_off`, else `SWITCH_OFF_NOT_ALLOWED`); `activate` switches it on |
| `flatten(stack, registries)` | What `/adjustability flatten` does: switched-off enchantments become active again, the other dormant ones are removed |
| `flatten(stack)` | Deprecated: removes every dormant enchantment, switched-off ones included |
| `addListener(listener)` / `removeListener(listener)` | See below |

`registries` is the world's, e.g. `level.registryAccess()`. Methods change the stack in place.

```java
/** A reward that adds an enchantment, dormant if it conflicts, and switches to it right away. */
static SwitchResult giveAndSwitch(ServerPlayer player, ItemStack tool, Holder<Enchantment> enchantment)
{
        AdjustabilityApi.addEnchantment(tool, enchantment, 1, player.level().registryAccess());
        if (AdjustabilityApi.activeEnchantments(tool).getLevel(enchantment) > 0)
        {
                return SwitchResult.DONE;
        }
        return AdjustabilityApi.activate(player, tool, enchantment);
}
```

## Listeners

Implement `AdjustabilityListener` (every method has a default) and register it once from your init with
`AdjustabilityApi.addListener`.

| Method | When | Return |
|---|---|---|
| `allowSwitch(SwitchEvent)` | Before a switch, after Adjustability's own checks, before the cost is taken | `false` refuses it (`SwitchResult.CANCELLED`) |
| `onSwitched(SwitchEvent)` | After a switch was written and paid for | |
| `allowAnvilResult(AnvilEvent)` | Before an anvil result gets dormant enchantments (on the server, and on the client for its preview) | `false` means no result |

`SwitchEvent` has the player, the stack, the target enchantment (for a None switch, `none()` is true and the target
is any enchantment of the group), what asked for it (`SwitchSource`: `PANEL`,
`CYCLE_KEY`, `COMMAND`, `API`) and the active and dormant enchantments before the switch. `AnvilEvent` has the player,
both inputs, vanilla's result and the proposed active and dormant split. A listener that throws is logged and counts
as allowing.

```java
/** No switching while on fire. */
static final AdjustabilityListener NO_SWITCHING_WHILE_BURNING = new AdjustabilityListener()
{
        @Override
        public boolean allowSwitch(SwitchEvent event)
        {
                return !event.player().isOnFire();
        }
};

/** Golden tools can't hold dormant enchantments made in an anvil. */
static final AdjustabilityListener NO_ADJUSTABLE_GOLD = new AdjustabilityListener()
{
        @Override
        public boolean allowAnvilResult(AnvilEvent event)
        {
                return !event.result().is(Items.GOLDEN_PICKAXE);
        }
};
```

These examples are compiled and run by the mod's own tests (`gametest/ApiExamples`), so they stay correct.

## `AdjustableItem`

Implement it on your `Item` class for item-specific rules. The server's tags and config still apply on top. (The
mod's tests use a development-only item, `gametest/TestAdjustableItem`, that does exactly this.)

| Method | Default | Meaning |
|---|---|---|
| `canHoldDormant(stack)` | `true` | `false`: this item never holds dormant enchantments |
| `maxDormant(stack)` | `-1` (server's `max_dormant_per_item`) | Most dormant enchantments; the lower of this and the server's applies |

```java
public class StaffItem extends Item implements AdjustableItem
{
        @Override
        public int maxDormant(ItemStack stack)
        {
                return 1;
        }
}
```

## Row marks (client)

Players pick the mark in front of each row of the Adjust panel with `mark_style` (see
[panel styles](panel-styles.md)). A mod can add its own with
`io.github.safetimamovic.adjustability.api.client.AdjustabilityClientApi`, from client code only (a Fabric client
entrypoint, NeoForge's `FMLClientSetupEvent`):

| Method | What it does |
|---|---|
| `registerRowMark(id, mark)` | Adds a `RowMark` players can pick by its id; it shows up in the config screens and `/adjustability panel`. Use your mod's namespace |
| `rowMarks()` | The ids of every row mark, Adjustability's own first |

A `RowMark` draws into a box `width()` wide and `RowMark.HEIGHT` (7) tall, lined up with the row's text. Its
`State` says how active the row is (`active()`, 0 to 1; in between while the panel animates a switch), whether the
mouse is over it, whether it's a None row, and the player's two mark colors; `color()` fades between them. Draw with
`fill` or sprites, not font glyphs, and keep no state: it's called every frame.

```java
/** A diamond outline, filled when active. */
public final class DiamondMark implements RowMark
{
        @Override
        public int width()
        {
                return 7;
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y, State state)
        {
                int outline = state.color();
                int inside = State.withAlpha(state.activeColor(), state.active());
                for (int row = 0; row < HEIGHT; row++)
                {
                        int half = 3 - Math.abs(3 - row);
                        graphics.fill(x + 3 - half, y + row, x + 4 + half, y + row + 1, inside);
                        graphics.fill(x + 3 - half, y + row, x + 4 - half, y + row + 1, outline);
                        graphics.fill(x + 3 + half, y + row, x + 4 + half, y + row + 1, outline);
                }
        }
}

// In your client setup:
AdjustabilityClientApi.registerRowMark(Identifier.fromNamespaceAndPath("examplemod", "diamond"), new DiamondMark());
```

On 1.21.1, `GuiGraphicsExtractor` is `GuiGraphics` and `Identifier` is `ResourceLocation`.

![The Adjust panel with the diamond mark](/screenshots/panel-mark-api-diamond.png)

If a player's `mark_style` names a mark no installed mod registers (say yours was removed), the panel uses
`adjustability:circle` and logs it once.
