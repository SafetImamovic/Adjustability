package io.github.safetimamovic.adjustability.enchantments_logic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;

public class CustomEnchantedItem
{
	private ItemStack itemStack;
	private ArrayList<Enchantment> enchantmentArray;

	public CustomEnchantedItem ()
	{
		this.enchantmentArray = new ArrayList<Enchantment>();
	}

}
