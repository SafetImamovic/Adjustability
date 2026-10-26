package io.github.safetimamovic.adjustability.api;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.ApiStatus;

/**
 * A switch about to happen ({@link AdjustabilityListener#allowSwitch}) or that just happened
 * ({@link AdjustabilityListener#onSwitched}). Always on the server thread.
 *
 * @param stack          the item; read it in {@code onSwitched} for the new split. Don't change it in
 *                       {@code allowSwitch}.
 * @param target         the enchantment being activated or individually switched off; for a whole-group None choice,
 *                       any enchantment of that group
 * @param activeBefore   the item's active enchantments before the switch
 * @param dormantBefore  its dormant enchantments before the switch, switched-off ones included
 * @param none           whether this deactivation leaves no active enchantment in {@code target}'s group, including
 *                       a group of one
 * @param deactivatesOnlyTarget whether only {@code target} is being switched off; compatible active group members stay
 */
@ApiStatus.Experimental
public record SwitchEvent(ServerPlayer player, ItemStack stack, Holder<Enchantment> target, SwitchSource source,
        ItemEnchantments activeBefore, ItemEnchantments dormantBefore, boolean none, boolean deactivatesOnlyTarget)
{
        /** The original constructor, for activation and whole-group None events. */
        public SwitchEvent(ServerPlayer player, ItemStack stack, Holder<Enchantment> target, SwitchSource source,
                ItemEnchantments activeBefore, ItemEnchantments dormantBefore, boolean none)
        {
                this(player, stack, target, source, activeBefore, dormantBefore, none, false);
        }
}
