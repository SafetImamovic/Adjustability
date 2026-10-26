package io.github.safetimamovic.adjustability.core;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.graph.ConflictGraph;
import io.github.safetimamovic.adjustability.graph.Selection;
import io.github.safetimamovic.adjustability.tags.ModTags;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
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
 *     <li>every dormant enchantment conflicts with an active one, or was switched off by the player (see
 *     {@link DormantEnchantments#off}), or {@code [toggle] allow_none} is on,</li>
 *     <li>enchantments in {@link ModTags#NEVER_DORMANT} are active.</li>
 * </ol>
 * No split turns a switched-off enchantment back on: only switching it on does.
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
         * enchantments come first, then {@code preferred} in id order, then the rest in id order. Those in
         * {@code off} stay dormant unless they're preferred.
         */
        public static Split split(ItemEnchantments all, Set<Holder<Enchantment>> preferred,
                Set<Holder<Enchantment>> off)
        {
                return split(all, preferred, enchantment -> true, off);
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
                return split(all.toImmutable(), active.keySet(), DormantEnchantments.off(stack, registries));
        }

        /**
         * A split from a {@link Selection} of {@code graph}, with levels from {@code all}; {@code off} are the
         * switched-off ones (only the dormant ones count).
         */
        public static Split split(ConflictGraph<Holder<Enchantment>> graph, ItemEnchantments all,
                Selection<Holder<Enchantment>> selection, Set<Holder<Enchantment>> off)
        {
                return Split.of(graph, all, selection, off);
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

        /**
         * The item's split as switching sees it: repaired if two active enchantments conflict. Normally also
         * completed (every dormant enchantment conflicts with an active one, see {@link #resplit}); with
         * {@code [toggle] allow_none} a group may stay with nothing active, so only conflicts are fixed and nothing
         * dormant is made active, curses aside.
         */
        public static Split normalize(ItemStack stack, HolderLookup.Provider registries)
        {
                if (!AdjustabilityConfig.toggle().allowNone())
                {
                        return resplit(stack, ItemEnchantments.EMPTY, registries);
                }
                ItemEnchantments active = activeOf(stack);
                ItemEnchantments.Mutable all = new ItemEnchantments.Mutable(active);
                mergeInto(all, dormantOf(stack, registries));
                return split(all.toImmutable(), active.keySet(), enchantment -> false,
                        DormantEnchantments.off(stack, registries));
        }

        /**
         * {@link #split(ItemEnchantments, Set, Set)}, but an enchantment outside {@code preferred} only becomes active
         * if {@code mayActivate} allows it and it isn't in {@code off} (never-dormant ones always may).
         */
        public static Split split(ItemEnchantments all, Set<Holder<Enchantment>> preferred,
                Predicate<Holder<Enchantment>> mayActivate, Set<Holder<Enchantment>> off)
        {
                ConflictGraph<Holder<Enchantment>> graph = graph(all);
                List<Holder<Enchantment>> order = new ArrayList<>();
                graph.vertices().stream().filter(enchantment -> enchantment.is(ModTags.NEVER_DORMANT))
                        .forEach(order::add);
                graph.vertices().stream().filter(preferred::contains).forEach(order::add);
                Selection<Holder<Enchantment>> selection = graph.select(order,
                        enchantment -> enchantment.is(ModTags.NEVER_DORMANT)
                                || !off.contains(enchantment) && mayActivate.test(enchantment));
                return Split.of(graph, all, selection, off);
        }

        /**
         * Everything the item holds plus {@code extra}, split: what's active stays active where possible, and an extra
         * enchantment is active unless it conflicts. With {@code [toggle] allow_none}, dormant ones stay dormant (a
         * group left on none stays so); otherwise the result is completed like {@link #resplit}.
         */
        public static Split add(ItemStack stack, ItemEnchantments extra, HolderLookup.Provider registries)
        {
                if (!AdjustabilityConfig.toggle().allowNone())
                {
                        return resplit(stack, extra, registries);
                }
                ItemEnchantments active = activeOf(stack);
                ItemEnchantments.Mutable all = new ItemEnchantments.Mutable(active);
                mergeInto(all, dormantOf(stack, registries));
                mergeInto(all, extra);
                return split(all.toImmutable(), active.keySet(), enchantment -> extra.getLevel(enchantment) > 0,
                        DormantEnchantments.off(stack, registries));
        }

        /**
         * Writes a split to the item, with its switched-off flags. Dormant entries for enchantments that aren't
         * registered stay.
         */
        public static void write(ItemStack stack, Split split, HolderLookup.Provider registries)
        {
                EnchantmentHelper.setEnchantments(stack, split.active());
                DormantEnchantments.set(stack, split.dormant(), split.off(), registries);
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

        /** An item's enchantments, split; {@code off} are the dormant ones the player switched off. */
        public record Split(ConflictGraph<Holder<Enchantment>> graph, ItemEnchantments active, ItemEnchantments dormant,
                Set<Holder<Enchantment>> off)
        {
                static Split of(ConflictGraph<Holder<Enchantment>> graph, ItemEnchantments all,
                        Selection<Holder<Enchantment>> selection, Set<Holder<Enchantment>> off)
                {
                        Set<Holder<Enchantment>> dormantOff = new LinkedHashSet<>(off);
                        dormantOff.retainAll(selection.dormant());
                        return new Split(graph, levels(all, selection.active()), levels(all, selection.dormant()),
                                Set.copyOf(dormantOff));
                }

                /** Size of the largest group of enchantments that conflict with each other, directly or not. */
                public int largestComponent()
                {
                        return this.graph.components().stream().mapToInt(Set::size).max().orElse(0);
                }
        }
}
