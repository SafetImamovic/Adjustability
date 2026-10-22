package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.graph.ConflictGraph;
import io.github.safetimamovic.adjustability.graph.Selection;
import io.github.safetimamovic.adjustability.tags.ModTags;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Splits an item's enchantments into active and dormant. Active ones live in vanilla's {@code enchantments}, dormant
 * ones in {@link DormantEnchantments}. After every write:
 * <ol>
 *     <li>no two active enchantments conflict,</li>
 *     <li>an enchantment is either active or dormant, never both,</li>
 *     <li>every dormant enchantment conflicts with an active one,</li>
 *     <li>enchantments in {@link ModTags#NEVER_DORMANT} are active.</li>
 * </ol>
 */
public final class Adjuster
{
        /** Stable order for the graph: by id, so results don't depend on hash order. */
        private static final Comparator<Holder<Enchantment>> BY_ID = Comparator.comparing(Adjuster::id);

        private Adjuster()
        {
        }

        /** Whether two enchantments can't both be active: vanilla's exclusive sets and the server's own rules. */
        public static boolean conflict(Holder<Enchantment> a, Holder<Enchantment> b)
        {
                return Exclusivity.conflict(a, b);
        }

        /** Whether any two of these conflict; false for a normal vanilla item. */
        public static boolean hasConflict(ItemEnchantments enchantments)
        {
                List<Holder<Enchantment>> list = new ArrayList<>(enchantments.keySet());
                for (int i = 0; i < list.size(); i++)
                {
                        for (int j = i + 1; j < list.size(); j++)
                        {
                                if (conflict(list.get(i), list.get(j)))
                                {
                                        return true;
                                }
                        }
                }
                return false;
        }

        /** The conflict graph of {@code all}, vertices in id order. */
        public static ConflictGraph<Holder<Enchantment>> graph(ItemEnchantments all)
        {
                List<Holder<Enchantment>> vertices = new ArrayList<>(all.keySet());
                vertices.sort(BY_ID);
                return ConflictGraph.of(vertices, Adjuster::conflict);
        }

        /**
         * Splits {@code all} into active and dormant, keeping {@code preferred} active where possible. Never-dormant
         * enchantments come first, then {@code preferred} in id order, then the rest in id order.
         */
        public static Split split(ItemEnchantments all, Set<Holder<Enchantment>> preferred)
        {
                ConflictGraph<Holder<Enchantment>> graph = graph(all);
                List<Holder<Enchantment>> order = new ArrayList<>();
                graph.vertices().stream().filter(enchantment -> enchantment.is(ModTags.NEVER_DORMANT)).forEach(order::add);
                graph.vertices().stream().filter(preferred::contains).forEach(order::add);
                Selection<Holder<Enchantment>> selection = graph.select(order);
                return new Split(graph, levels(all, selection.active()), levels(all, selection.dormant()));
        }

        /**
         * Adds {@code extra} to everything the item holds (vanilla's anvil level rule for duplicates), then splits it,
         * keeping what's active now active where possible. Doesn't write anything.
         */
        public static Split resplit(ItemStack stack, ItemEnchantments extra, HolderLookup.Provider registries)
        {
                ItemEnchantments active = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                ItemEnchantments.Mutable all = new ItemEnchantments.Mutable(active);
                mergeInto(all, DormantEnchantments.get(stack, registries));
                mergeInto(all, extra);
                return split(all.toImmutable(), active.keySet());
        }

        /** A split from a {@link Selection} of {@code graph}, with levels from {@code all}. */
        public static Split split(ConflictGraph<Holder<Enchantment>> graph, ItemEnchantments all,
                Selection<Holder<Enchantment>> selection)
        {
                return new Split(graph, levels(all, selection.active()), levels(all, selection.dormant()));
        }

        /** The item's active enchantments, as stored (not re-split). */
        public static ItemEnchantments activeOf(ItemStack stack)
        {
                return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        }

        /** The item's dormant enchantments, as stored (not re-split). */
        public static ItemEnchantments dormantOf(ItemStack stack, HolderLookup.Provider registries)
        {
                return DormantEnchantments.get(stack, registries);
        }

        /** Writes a split to the item. */
        public static void write(ItemStack stack, Split split)
        {
                EnchantmentHelper.setEnchantments(stack, split.active());
                DormantEnchantments.set(stack, split.dormant());
        }

        /**
         * Adds levels like the anvil: the same level twice goes up one (up to the enchantment's max level), different
         * levels keep the higher.
         */
        public static void mergeInto(ItemEnchantments.Mutable target, ItemEnchantments source)
        {
                for (Entry<Holder<Enchantment>> entry : source.entrySet())
                {
                        Holder<Enchantment> enchantment = entry.getKey();
                        int current = target.getLevel(enchantment);
                        int level = entry.getIntValue();
                        if (current == level)
                        {
                                level = Math.max(level, Math.min(level + 1, enchantment.value().getMaxLevel()));
                        }
                        target.set(enchantment, Math.max(level, current));
                }
        }

        public static String id(Holder<Enchantment> enchantment)
        {
                return enchantment.unwrapKey().map(key -> key.identifier().toString()).orElse("");
        }

        private static ItemEnchantments levels(ItemEnchantments all, Set<Holder<Enchantment>> keys)
        {
                ItemEnchantments.Mutable result = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                keys.forEach(enchantment -> result.set(enchantment, all.getLevel(enchantment)));
                return result.toImmutable();
        }

        /** An item's enchantments, split. */
        public record Split(ConflictGraph<Holder<Enchantment>> graph, ItemEnchantments active, ItemEnchantments dormant)
        {
                /** Size of the largest group of enchantments that conflict with each other, directly or not. */
                public int largestComponent()
                {
                        return this.graph.components().stream().mapToInt(Set::size).max().orElse(0);
                }
        }
}
