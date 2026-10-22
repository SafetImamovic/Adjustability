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
 * @param target         the dormant enchantment being made active; with {@code none}, any enchantment of the group
 *                       being left with nothing active
 * @param activeBefore   the item's active enchantments before the switch
 * @param dormantBefore  its dormant enchantments before the switch
 * @param none           true for the "none" choice: every enchantment of {@code target}'s group becomes dormant
 */
@ApiStatus.Experimental
public record SwitchEvent(ServerPlayer player, ItemStack stack, Holder<Enchantment> target, SwitchSource source,
        ItemEnchantments activeBefore, ItemEnchantments dormantBefore, boolean none)
{
}
