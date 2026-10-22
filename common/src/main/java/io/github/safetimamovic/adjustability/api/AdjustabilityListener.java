package io.github.safetimamovic.adjustability.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * Hooks for other mods, registered with {@link AdjustabilityApi#addListener}. Every method has a default, so implement
 * only what you need. A listener that throws is logged and treated as allowing; it can't break a switch or an anvil.
 */
@ApiStatus.Experimental
public interface AdjustabilityListener
{
        /**
         * Before a switch, after Adjustability's own checks pass and before the cost is taken.
         *
         * @return false to refuse it; the player sees {@link SwitchResult#CANCELLED}'s message
         */
        default boolean allowSwitch(SwitchEvent event)
        {
                return true;
        }

        /** After a switch was written to the item and paid for. */
        default void onSwitched(SwitchEvent event)
        {
        }

        /**
         * Before an anvil result gets dormant enchantments.
         *
         * @return false for no result, as if the combination weren't allowed
         */
        default boolean allowAnvilResult(AnvilEvent event)
        {
                return true;
        }
}
