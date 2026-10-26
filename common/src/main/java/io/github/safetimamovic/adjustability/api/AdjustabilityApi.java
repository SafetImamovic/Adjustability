package io.github.safetimamovic.adjustability.api;

import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import io.github.safetimamovic.adjustability.core.Exclusivity;
import io.github.safetimamovic.adjustability.core.ItemRules;
import io.github.safetimamovic.adjustability.core.Listeners;
import io.github.safetimamovic.adjustability.core.Toggler;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.ApiStatus;

/**
 * Adjustability's public API, for other mods. Works the same on Fabric and NeoForge; call it from common code.
 *
 * <p>Most mods need none of this: anything that reads an item's enchantments the vanilla way
 * ({@code DataComponents.ENCHANTMENTS}, {@code EnchantmentHelper}) already sees exactly the active ones. Use it to read
 * or change the dormant ones, to switch from code, or to veto switches and anvil results with a listener.
 *
 * <p>Methods that change a stack change it in place; put it back where it belongs if it isn't in a slot already. All
 * methods that take a {@link HolderLookup.Provider} need the world's registries, e.g. {@code level.registryAccess()}.
 *
 * <p>Experimental until 1.0: names may still change.
 */
@ApiStatus.Experimental
public final class AdjustabilityApi
{
        private AdjustabilityApi()
        {
        }

        /** The item's active enchantments: vanilla's {@code minecraft:enchantments}. */
        public static ItemEnchantments activeEnchantments(ItemStack stack)
        {
                return Adjuster.activeOf(stack);
        }

        /**
         * The item's dormant enchantments: the ones waiting behind an active enchantment they conflict with. Empty for
         * most items. Not the ones the player switched off; see {@link #switchedOffEnchantments}.
         */
        public static ItemEnchantments dormantEnchantments(ItemStack stack, HolderLookup.Provider registries)
        {
                Set<Holder<Enchantment>> off = DormantEnchantments.off(stack, registries);
                ItemEnchantments.Mutable result = new ItemEnchantments.Mutable(Adjuster.dormantOf(stack, registries));
                result.removeIf(off::contains);
                return result.toImmutable();
        }

        /**
         * The enchantments the player switched off: ones that conflict with nothing, so nothing but the player keeps
         * them from being active. Empty for most items.
         */
        public static ItemEnchantments switchedOffEnchantments(ItemStack stack, HolderLookup.Provider registries)
        {
                Set<Holder<Enchantment>> off = DormantEnchantments.off(stack, registries);
                ItemEnchantments.Mutable result = new ItemEnchantments.Mutable(Adjuster.dormantOf(stack, registries));
                result.removeIf(enchantment -> !off.contains(enchantment));
                return result.toImmutable();
        }

        /** Whether the item has any dormant or switched-off enchantments. Cheap; no registries needed. */
        public static boolean hasDormant(ItemStack stack)
        {
                return DormantEnchantments.has(stack);
        }

        /**
         * Whether two enchantments conflict for Adjustability: vanilla's {@code exclusive_set} and the server's own
         * {@code [exclusivity]} rules.
         */
        public static boolean conflict(Holder<Enchantment> a, Holder<Enchantment> b)
        {
                return Exclusivity.conflict(a, b);
        }

        /**
         * Whether the item may hold dormant enchantments: tags, {@code [items] mode} and {@link AdjustableItem}.
         */
        public static boolean canHoldDormant(ItemStack stack)
        {
                return ItemRules.canAdjust(stack);
        }

        /**
         * Adds an enchantment to the item: active if it conflicts with nothing active, dormant otherwise. Levels merge
         * like in an anvil. Ignores costs, limits and listeners; does nothing to items that can't hold dormant
         * enchantments (see {@link #canHoldDormant}).
         *
         * @return whether it was added
         */
        public static boolean addEnchantment(ItemStack stack, Holder<Enchantment> enchantment, int level,
                HolderLookup.Provider registries)
        {
                if (!ItemRules.canAdjust(stack) || level <= 0)
                {
                        return false;
                }
                ItemEnchantments.Mutable extra = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                extra.set(enchantment, level);
                Adjuster.write(stack, Adjuster.add(stack, extra.toImmutable(), registries), registries);
                return true;
        }

        /**
         * Makes a dormant enchantment active as if the player had clicked it in the Adjust panel: every server rule
         * applies (cooldown, cost, curses) and listeners are asked. Tells the player nothing; see the result.
         *
         * @param stack an item the player may change; put it back in its slot afterwards if needed
         */
        public static SwitchResult activate(ServerPlayer player, ItemStack stack, Holder<Enchantment> enchantment)
        {
                return Toggler.activate(player, stack, enchantment, SwitchSource.API);
        }

        /**
         * Leaves the group of conflicting enchantments that {@code anyInGroup} belongs to with nothing active, as if
         * the player had clicked None in the Adjust panel. Only when the server allows it
         * ({@code [toggle] allow_none}); otherwise {@link SwitchResult#NONE_NOT_ALLOWED}. On an enchantment that
         * conflicts with nothing, this switches it off ({@code [toggle] allow_switch_off}, else
         * {@link SwitchResult#SWITCH_OFF_NOT_ALLOWED}); {@link #activate} switches it on again.
         */
        public static SwitchResult deactivateGroup(ServerPlayer player, ItemStack stack, Holder<Enchantment> anyInGroup)
        {
                return Toggler.deactivate(player, stack, anyInGroup, SwitchSource.API);
        }

        /**
         * Makes the item a plain vanilla one, as {@code /adjustability flatten} does: enchantments the player switched
         * off become active again, the other dormant ones are removed, active ones stay.
         */
        public static void flatten(ItemStack stack, HolderLookup.Provider registries)
        {
                Adjuster.flatten(stack, registries);
        }

        /**
         * Removes all dormant enchantments from the item, switched-off ones included; its active ones stay.
         *
         * @deprecated loses switched-off enchantments; use {@link #flatten(ItemStack, HolderLookup.Provider)}
         */
        @Deprecated
        public static void flatten(ItemStack stack)
        {
                DormantEnchantments.clear(stack);
        }

        /** Adds a listener; see {@link AdjustabilityListener}. Call once, from your mod's init. */
        public static void addListener(AdjustabilityListener listener)
        {
                Listeners.add(listener);
        }

        /** Removes a listener added with {@link #addListener}. */
        public static void removeListener(AdjustabilityListener listener)
        {
                Listeners.remove(listener);
        }
}
