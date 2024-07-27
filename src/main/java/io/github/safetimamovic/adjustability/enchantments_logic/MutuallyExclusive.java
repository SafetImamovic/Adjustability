package io.github.safetimamovic.adjustability.enchantments_logic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.*;

/**
 * Manages mutually exclusive enchantments and provides methods to check for their presence on an item stack.
 */
public class MutuallyExclusive
{

	private final Map<Enchantment, Set<Enchantment>> mutuallyExclusiveMap = new HashMap<>();

	public void printMutuallyExclusiveMap ()
	{
		for ( Map.Entry<Enchantment, Set<Enchantment>> entry : mutuallyExclusiveMap.entrySet() )
		{
			System.out.print(entry.getKey().getDescriptionId() + " -> ");
			entry.getValue().forEach(exclusive -> System.out.print(exclusive.getDescriptionId() + " "));
			System.out.println();
		}
	}

	public MutuallyExclusive (List<List<Enchantment>> mutuallyExclusiveGroups)
	{
		for ( List<Enchantment> group : mutuallyExclusiveGroups )
		{
			for ( Enchantment enchantment : group )
			{
				Set<Enchantment> exclusives = new HashSet<>(group);
				exclusives.remove(enchantment);
				mutuallyExclusiveMap.put(enchantment, exclusives);
			}
		}
	}

	/**
	 * Checks if the given item stack contains any mutually exclusive enchantments.
	 *
	 * @param itemStack the item stack to check
	 * @return true if the item stack contains mutually exclusive enchantments, false otherwise
	 */
	public boolean hasMutuallyExclusiveEnchantments (ItemStack itemStack)
	{
		Set<Enchantment> enchantmentsOnItem = getEnchantmentsOnItem(itemStack);
		for ( Enchantment enchantment : enchantmentsOnItem )
		{
			Set<Enchantment> exclusives = mutuallyExclusiveMap.get(enchantment);
			if ( exclusives != null )
			{
				for ( Enchantment exclusive : exclusives )
				{
					if ( enchantmentsOnItem.contains(exclusive) )
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Extracts the enchantments from the given item stack.
	 *
	 * @param itemStack the item stack to extract enchantments from
	 * @return a set of enchantments present on the item stack
	 */
	private Set<Enchantment> getEnchantmentsOnItem (ItemStack itemStack)
	{
		Set<Enchantment> enchantments = new HashSet<>();

		itemStack
			.getEnchantments()
			.keySet()
			.forEach((e) -> enchantments.add(e.get()));

		return enchantments;
	}
}