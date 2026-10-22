package io.github.safetimamovic.adjustability.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.ApiStatus;

/**
 * An anvil result about to get dormant enchantments ({@link AdjustabilityListener#allowAnvilResult}). Runs wherever
 * the anvil computes its result: on the server, and on the client for its preview.
 *
 * @param left     the item in the left slot
 * @param right    the item in the right slot
 * @param result   vanilla's result, before Adjustability splits it; don't change it
 * @param active   what would be active on the result
 * @param dormant  what would be dormant on the result
 */
@ApiStatus.Experimental
public record AnvilEvent(Player player, ItemStack left, ItemStack right, ItemStack result, ItemEnchantments active,
        ItemEnchantments dormant)
{
}
