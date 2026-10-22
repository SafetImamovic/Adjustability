package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.api.AdjustableItem;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ItemMode;
import io.github.safetimamovic.adjustability.tags.ModTags;
import net.minecraft.world.item.ItemStack;

/**
 * Which items take part. The one place the item tags, {@code [items] mode} and {@link AdjustableItem} are checked.
 */
public final class ItemRules
{
        private ItemRules()
        {
        }

        /**
         * Whether {@code stack} may hold dormant enchantments: never if it's in {@code #adjustability:not_adjustable}
         * or its item says no ({@link AdjustableItem}); otherwise always in DENYLIST mode, and only if it's in
         * {@code #adjustability:adjustable} in ALLOWLIST mode.
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
                if (stack.getItem() instanceof AdjustableItem item && !item.canHoldDormant(stack))
                {
                        return false;
                }
                return mode == ItemMode.DENYLIST || stack.is(ModTags.ADJUSTABLE);
        }

        /**
         * Most dormant enchantments {@code stack} may hold: the lower of the server's {@code max_dormant_per_item}
         * (0 there means no limit) and its item's own ({@link AdjustableItem#maxDormant}, -1 for none).
         *
         * @return {@link Integer#MAX_VALUE} for no limit
         */
        public static int maxDormant(ItemStack stack, int serverLimit)
        {
                int limit = serverLimit <= 0 ? Integer.MAX_VALUE : serverLimit;
                if (stack.getItem() instanceof AdjustableItem item && item.maxDormant(stack) >= 0)
                {
                        limit = Math.min(limit, item.maxDormant(stack));
                }
                return limit;
        }
}
