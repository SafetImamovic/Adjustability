# Java API

For mod devs who want to read or change an item's active and dormant enchantments, switch from code, or veto
switches and anvil results. Everything is in `io.github.safetimamovic.adjustability.api` and works the same on Fabric
and NeoForge; call it from your common code.

> **Experimental until 1.0.** The API is marked `@ApiStatus.Experimental`: names may still change. There's no separate
> `-api` artifact yet; depend on the mod jar `compileOnly` (and at runtime in dev).

**Most mods need none of this.** Anything that reads enchantments the vanilla way (`DataComponents.ENCHANTMENTS`,
`EnchantmentHelper`) already sees exactly the active ones, and dormant ones do nothing. See
[where dormant enchantments are stored](storage.md) for the data itself.

## `AdjustabilityApi`

| Method | What it does |
|---|---|
| `activeEnchantments(stack)` | The active enchantments (vanilla's component) |
| `dormantEnchantments(stack, registries)` | The dormant ones; empty for most items |
| `hasDormant(stack)` | Cheap check, no registries needed |
| `conflict(a, b)` | Whether two enchantments conflict for Adjustability: vanilla's `exclusive_set` plus the server's `[exclusivity]` rules |
| `canHoldDormant(stack)` | Tags, `[items] mode` and `AdjustableItem` together |
| `addEnchantment(stack, enchantment, level, registries)` | Adds it: active if it conflicts with nothing active, dormant otherwise. Ignores costs, limits and listeners |
| `activate(player, stack, enchantment)` | Switches as if the player clicked it in the Adjust panel: cooldown, cost, curses and listeners all apply. Returns a `SwitchResult` and tells the player nothing |
| `flatten(stack)` | Removes the dormant enchantments; active ones stay |
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

`SwitchEvent` has the player, the stack, the target enchantment, what asked for it (`SwitchSource`: `PANEL`,
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

Implement it on your `Item` class for item-specific rules. The server's tags and config still apply on top.

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
