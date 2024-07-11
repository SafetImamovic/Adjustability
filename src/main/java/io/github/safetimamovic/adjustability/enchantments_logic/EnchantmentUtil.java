package io.github.safetimamovic.adjustability.enchantments_logic;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.util.ArrayList;
import java.util.List;

public class EnchantmentUtil
{
	private static final String NBT_ACTIVE_ENCHANTMENT = "ActiveEnchantment";
	private static final String NBT_POSSIBLE_ENCHANTMENTS = "PossibleEnchantments";

	public static void setActiveEnchantment (ItemStack itemStack)
	{
		DataComponentMap DCM = itemStack.getComponents();



		System.out.println("DCM Count:" + DCM.stream().count());



//		public static ItemEnchantments updateEnchantments(ItemStack pStack, Consumer<ItemEnchantments.Mutable> pUpdater) {
//		DataComponentType<ItemEnchantments> datacomponenttype = getComponentType(pStack);
//		ItemEnchantments itemenchantments = pStack.get(datacomponenttype);
//		if (itemenchantments == null) {
//			return ItemEnchantments.EMPTY;
//		} else {
//			ItemEnchantments.Mutable itemenchantments$mutable = new ItemEnchantments.Mutable(itemenchantments);
//			pUpdater.accept(itemenchantments$mutable);
//			ItemEnchantments itemenchantments1 = itemenchantments$mutable.toImmutable();
//			pStack.set(datacomponenttype, itemenchantments1);
//			return itemenchantments1;
//		}


		DCM.stream().forEach((s) ->
				     {
					     System.out.println(s.toString());
				     });

		itemStack.applyComponents(DCM);
	}

	public static List<Enchantment> getEnchantmentsList (ItemStack item)
	{
		List<Enchantment> enchantments = new ArrayList<>();

		ItemEnchantments itemEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(item);

		for ( Holder<Enchantment> entry : itemEnchantments.keySet() )
		{
			Enchantment enchantment = entry.get();
			enchantments.add(enchantment);
		}

		return enchantments;
	}
}
