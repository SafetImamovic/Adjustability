package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** What a grindstone does with dormant enchantments. Called from {@code GrindstoneMenuMixin} on both loaders. */
public final class GrindstoneRules
{
        private GrindstoneRules()
        {
        }

        /**
         * After vanilla removed the active non-curse enchantments from {@code item}: removes the dormant ones too, or,
         * with {@code remove_dormant} off, makes the ones that no longer conflict with anything active again.
         */
        public static void afterRemovingEnchantments(ItemStack item, HolderLookup.Provider registries)
        {
                if (!DormantEnchantments.has(item))
                {
                        return;
                }
                if (AdjustabilityConfig.grindstone().removeDormant())
                {
                        DormantEnchantments.clear(item);
                }
                else
                {
                        Adjuster.write(item, Adjuster.resplit(item, ItemEnchantments.EMPTY, registries));
                }
        }
}
