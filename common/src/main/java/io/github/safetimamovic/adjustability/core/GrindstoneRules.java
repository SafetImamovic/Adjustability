package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
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
                        Adjuster.write(item, Adjuster.resplit(item, ItemEnchantments.EMPTY, registries), registries);
                }
        }

        /**
         * Experience the grindstone gives for an input's dormant enchantments, counted like vanilla counts active
         * ones (each one's minimum enchanting cost; curses give none). Zero with {@code remove_dormant} off, since they
         * stay on the item.
         */
        public static int dormantExperience(ItemStack item, HolderLookup.Provider registries)
        {
                if (!DormantEnchantments.has(item) || !AdjustabilityConfig.grindstone().removeDormant())
                {
                        return 0;
                }
                int amount = 0;
                for (Entry<Holder<Enchantment>> entry : DormantEnchantments.get(item, registries).entrySet())
                {
                        if (!entry.getKey().is(EnchantmentTags.CURSE))
                        {
                                amount += entry.getKey().value().getMinCost(entry.getIntValue());
                        }
                }
                return amount;
        }
}
