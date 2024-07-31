package io.github.safetimamovic.adjustability.enchantments_logic;

import io.github.safetimamovic.adjustability.Adjustability;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber( modid = Adjustability.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE )
public class AnvilHandler
{

	@SubscribeEvent
	public static void onAnvilUpdate (AnvilUpdateEvent event)
	{
		System.out.println("Anvil!");

		ItemStack left = event.getLeft();
		ItemStack right = event.getRight();

		int totalCost = 69;

		if ( !(left.isEnchanted() && right.isEnchanted()) )
		{
			return;
		}

		MutuallyExclusive.willResultInMutuallyExclusiveEnchantments(left, right);

		if ( !(left.getItem() == right.getItem()) )
		{
			return;
		}

		ItemStack output = left.copy();

		for ( Enchantment e : MutuallyExclusive.getEnchantmentsOnItem(right) )
		{
			output.enchant(e, right.getEnchantmentLevel(e));
		}

		event.setOutput(output);
		event.setCost(totalCost);
	}
}