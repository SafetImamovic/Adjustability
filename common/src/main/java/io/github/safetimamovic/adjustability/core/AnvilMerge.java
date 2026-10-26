package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.api.AnvilEvent;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.AnvilConfig;
import io.github.safetimamovic.adjustability.tags.ModTags;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The anvil rules. Vanilla's {@code AnvilMenu.createResult} does the merging; Adjustability only (1) lets conflicting
 * enchantments through its compatibility check ({@link #mayCombine}) and (2) splits the result into active and dormant
 * and adds the dormant cost afterwards ({@link #afterCreateResult}). Vanilla's prices, level rules, repair and renaming
 * stay as they are. Called from {@code AnvilMenuMixin} on both loaders.
 */
public final class AnvilMerge
{
        /** Vanilla's survival limit: a price of 40 or more shows "Too Expensive!". */
        private static final int TOO_EXPENSIVE = 40;

        private AnvilMerge()
        {
        }

        /** Whether the anvil may put {@code a} and {@code b} on {@code input} together although they conflict. */
        public static boolean mayCombine(ItemStack input, Holder<Enchantment> a, Holder<Enchantment> b)
        {
                AnvilConfig config = AdjustabilityConfig.anvil();
                if (!config.enabled())
                {
                        return false;
                }
                // Books aren't items that hold dormant enchantments; whether they combine is their own setting.
                if (input.is(Items.ENCHANTED_BOOK) ? !config.combineBooks() : !ItemRules.canAdjust(input))
                {
                        return false;
                }
                if (a.is(ModTags.NOT_COMBINABLE) || b.is(ModTags.NOT_COMBINABLE))
                {
                        return false;
                }
                // Both would have to stay active, which they can't.
                return !(a.is(ModTags.NEVER_DORMANT) && b.is(ModTags.NEVER_DORMANT));
        }

        /**
         * After vanilla computed the result: splits it into active and dormant, merges in both inputs' dormant
         * enchantments, checks the limits and adds the dormant cost. Dormant entries for enchantments that aren't
         * registered come along from both inputs, and count for nothing.
         *
         * @param result the stack in the result slot; changed in place
         * @return the result to keep in the slot: {@code result}, or {@link ItemStack#EMPTY} if a limit or the price
         *         rules it out
         */
        public static ItemStack afterCreateResult(Player player, ItemStack input, ItemStack addition, ItemStack result,
                DataSlot cost)
        {
                // Books hold every enchantment as stored; there is nothing to make dormant until one goes on an item.
                if (result.isEmpty() || result.is(Items.ENCHANTED_BOOK) || !ItemRules.canAdjust(result))
                {
                        return result;
                }
                HolderLookup.Provider registries = player.level().registryAccess();
                // The left item's are already on the result, a copy of it.
                DormantEnchantments.addUnknown(result, DormantEnchantments.unknown(addition, registries), registries);
                ItemEnchantments merged = result.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                ItemEnchantments inputDormant = DormantEnchantments.get(input, registries);
                ItemEnchantments additionDormant = DormantEnchantments.get(addition, registries);
                if (inputDormant.isEmpty() && additionDormant.isEmpty() && !Adjuster.hasConflict(merged))
                {
                        return result;
                }

                ItemEnchantments.Mutable all = new ItemEnchantments.Mutable(merged);
                Adjuster.mergeInto(all, inputDormant);
                Adjuster.mergeInto(all, additionDormant);
                Set<Holder<Enchantment>> keepActive = input.getOrDefault(DataComponents.ENCHANTMENTS,
                        ItemEnchantments.EMPTY).keySet();
                // What the player switched off on the left item stays off; the right item's choices don't count.
                Set<Holder<Enchantment>> off = DormantEnchantments.off(input, registries);
                // With allow_none, what was dormant on the left item stays dormant: a group left on none stays so.
                Adjuster.Split split = AdjustabilityConfig.toggle().allowNone()
                        ? Adjuster.split(all.toImmutable(), keepActive,
                                enchantment -> inputDormant.getLevel(enchantment) == 0, off)
                        : Adjuster.split(all.toImmutable(), keepActive, off);

                AnvilConfig config = AdjustabilityConfig.anvil();
                int maxDormant = ItemRules.maxDormant(result, config.maxDormantPerItem());
                // Switched-off enchantments wait behind nothing; the limit is about holding conflicts.
                if (split.dormant().size() - split.off().size() > maxDormant
                        || config.maxConflictsPerComponent() > 0
                        && split.largestComponent() > config.maxConflictsPerComponent())
                {
                        cost.set(0);
                        return ItemStack.EMPTY;
                }

                if (!Listeners.allowAnvilResult(new AnvilEvent(player, input, addition, result, split.active(),
                        split.dormant())))
                {
                        cost.set(0);
                        return ItemStack.EMPTY;
                }

                int newlyDormantLevels = 0;
                for (Holder<Enchantment> enchantment : split.dormant().keySet())
                {
                        if (inputDormant.getLevel(enchantment) == 0)
                        {
                                newlyDormantLevels += split.dormant().getLevel(enchantment);
                        }
                }
                Adjuster.write(result, split, registries);

                int price = (int) Math.min(Integer.MAX_VALUE,
                        (long) cost.get() + (long) newlyDormantLevels * config.dormantCostPerLevel());
                cost.set(price);
                if (price >= TOO_EXPENSIVE && !player.hasInfiniteMaterials() && !config.allowTooExpensive())
                {
                        return ItemStack.EMPTY;
                }
                return result;
        }
}
