package io.github.safetimamovic.adjustability.api;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * Implement on an {@code Item} class to set its own rules. The server's tags and config still apply on top: an item
 * in {@code #adjustability:not_adjustable}, or outside the allowlist in ALLOWLIST mode, never takes part.
 */
@ApiStatus.Experimental
public interface AdjustableItem
{
        /** Whether this stack may hold dormant enchantments at all. */
        default boolean canHoldDormant(ItemStack stack)
        {
                return true;
        }

        /**
         * Most dormant enchantments this stack may hold, or -1 for the server's {@code max_dormant_per_item}. The lower
         * of the two applies.
         */
        default int maxDormant(ItemStack stack)
        {
                return -1;
        }
}
