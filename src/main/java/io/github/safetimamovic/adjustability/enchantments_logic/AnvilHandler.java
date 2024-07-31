package io.github.safetimamovic.adjustability.enchantments_logic;

import io.github.safetimamovic.adjustability.Adjustability;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber( modid = Adjustability.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE )
public class AnvilHandler
{
	public AnvilHandler ()
	{
		System.out.println("Called!");
	}

	@SubscribeEvent
	public static void onAnvilUpdate (AnvilUpdateEvent event)
	{
		System.out.println("Anvil!");

		ItemStack result = event.getOutput();



		MutuallyExclusive.getMutuallyExclusiveEnchantments(result);

	}
}