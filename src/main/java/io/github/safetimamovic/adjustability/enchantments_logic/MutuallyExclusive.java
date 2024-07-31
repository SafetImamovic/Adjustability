package io.github.safetimamovic.adjustability.enchantments_logic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.*;

/**
 * Manages mutually exclusive enchantments and provides methods to check for their presence on an ItemStack.
 */
public class MutuallyExclusive
{
	private static final Map<Enchantment, Set<Enchantment>> mutuallyExclusiveMap = new HashMap<>();

	public void printMutuallyExclusiveMap ()
	{
		for ( Map.Entry<Enchantment, Set<Enchantment>> entry : mutuallyExclusiveMap.entrySet() )
		{
			System.out.print(entry.getKey().getDescriptionId() + " : ");
			entry.getValue().forEach(exclusive -> System.out.print(exclusive.getDescriptionId() + " "));
			System.out.println();
		}
	}

	public static void printGraph(List<List<Enchantment>> graph)
	{
		System.out.println("Graph Component Count: " + graph.size());

		for (int i = 0; i < graph.size(); i++)
		{
			System.out.println("\tSub-Graph: " + (i + 1));

			for (Enchantment e : graph.get(i))
			{
				System.out.println("\t\t" + e.getDescriptionId());
			}
		}
	}

	/**
	 * Constructs a HashMap <code>mutuallyExclusiveMap</code> from a graph structure that allows for constant time complexity (O(1)) for search operations in the best case.
	 * The HashMap maps each Enchantment to a set of mutually exclusive Enchantments.
	 * The construction process is as follows:
	 * <ol>
	 *     <li>
	 *         Iterates over each sub-graph in the provided graph structure.
	 *     </li>
	 *     <li>
	 *         For each Enchantment in a sub-graph:
	 *     </li>
	 *     <ol>
	 *     <li>
	 *         Creates a new HashSet containing all Enchantments in that sub-graph.
	 *     </li>
	 *     <li>
	 *         Removes the current Enchantment from the HashSet.
	 *     </li>
	 *     <li>
	 *         Uses the current Enchantment as the key and the modified HashSet as the value, then adds this entry to the `mutuallyExclusiveMap`.
	 *     </li>
	 *     </ol>
	 * </ol>
	 *
	 * @param mutuallyExclusiveGraph A graph data structure where each sub-graph (List) contains Enchantments that are mutually exclusive.
	 */
	public MutuallyExclusive (List<List<Enchantment>> mutuallyExclusiveGraph)
	{
		for ( List<Enchantment> group : mutuallyExclusiveGraph )
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
	 * Returns the Enchantment Graph of mutually exclusive sub-graphs if any are present on the ItemStack
	 *
	 * @param itemStack the ItemStack to check
	 * @return Enchantment Graph if the item stack contains mutually exclusive enchantments, false otherwise
	 */
	public static List<List<Enchantment>> getMutuallyExclusiveEnchantments (ItemStack itemStack)
	{
		List<Enchantment> enchantmentsOnItem = new ArrayList<>(getEnchantmentsOnItem(itemStack));

		List<List<Enchantment>> foundGroups = new ArrayList<>();

		for ( Enchantment enchantment : enchantmentsOnItem )
		{
			Set<Enchantment> exclusives = mutuallyExclusiveMap.get(enchantment);

			if ( exclusives == null )
			{
				continue; //might want to break instead, because no one group should be null, if one is null it could be an indicator that the data isn't valid
			}

			List<Enchantment> group = new ArrayList<>();
			group.add(enchantment);

			Set<Enchantment> enchantmentSet = new HashSet<>(enchantmentsOnItem);

			for ( Enchantment exclusive : exclusives )
			{
				if ( enchantmentSet.contains(exclusive) )
				{
					group.add(exclusive);
				}
			}

			if ( group.size() > 1 )
			{
				foundGroups.add(group);
				break;
			}
		}

		printGraph(foundGroups);

		return foundGroups;
	}


	/**
	 * Extracts the enchantments from the given item stack.
	 *
	 * @param itemStack the item stack to extract enchantments from
	 * @return a set of enchantments present on the item stack
	 */
	private static Set<Enchantment> getEnchantmentsOnItem (ItemStack itemStack)
	{
		Set<Enchantment> enchantments = new HashSet<>();

		itemStack
			.getEnchantments()
			.keySet()
			.forEach(e -> enchantments.add(e.get()));

		return enchantments;
	}
}