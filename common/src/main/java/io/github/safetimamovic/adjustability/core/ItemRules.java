package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ItemMode;
import io.github.safetimamovic.adjustability.tags.ModTags;
import net.minecraft.world.item.ItemStack;

/** Which items take part. The one place the item tags and {@code [items] mode} are checked. */
public final class ItemRules
{
        private ItemRules()
        {
        }

        /**
         * Whether {@code stack} may hold dormant enchantments: never if it's in {@code #adjustability:not_adjustable};
         * otherwise always in DENYLIST mode, and only if it's in {@code #adjustability:adjustable} in ALLOWLIST mode.
         */
        public static boolean canAdjust(ItemStack stack)
        {
                return canAdjust(stack, AdjustabilityConfig.itemMode());
        }

        /** {@link #canAdjust(ItemStack)} for a given mode. */
        public static boolean canAdjust(ItemStack stack, ItemMode mode)
        {
                if (stack.isEmpty() || stack.is(ModTags.NOT_ADJUSTABLE))
                {
                        return false;
                }
                return mode == ItemMode.DENYLIST || stack.is(ModTags.ADJUSTABLE);
        }
}
