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

@Mod.EventBusSubscriber(modid = Adjustability.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AnvilHandler
{
    public AnvilHandler()
    {
        System.out.println("Called!");
    }

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        System.out.println("Anvil!");

        List<Enchantment> enchantments = getEnchantmentsList(event.getLeft());

        for (int i = 0; i < event.getLeft().getEnchantments().size(); i++)
        {
            System.out.println(enchantments.get(i).getDescriptionId());
        }

        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        int baseCost = 1; // Base cost for the anvil operation

        // Check if both items have enchantments
        if (hasEnchantments(left) && hasEnchantments(right)) {
            // Determine the number of enchantments being combined
            int combinedEnchantmentCount = EnchantmentHelper.getEnchantmentsForCrafting(left).size() + EnchantmentHelper.getEnchantmentsForCrafting(right).size();

            // Increase the base cost based on the number of combined enchantments
            int totalCost = baseCost + combinedEnchantmentCount;

            // Check for mutually exclusive enchantments
            if (areMutuallyExclusive(left, right)) {
                totalCost += 1; // Increase cost for mutually exclusive enchantments
            }

            // Apply the enchantment of the right item to the left item
            Enchantment rightEnchantment = getEnchantment(right);
            if (rightEnchantment != null) {
                ItemStack output = left.copy();
                output.enchant(rightEnchantment, right.getEnchantmentLevel(rightEnchantment));
                event.setOutput(output);
                event.setCost(totalCost); // Set the total cost for the anvil operation
                return; // Exit the method after setting the output
            }
        }

        // If no enchantments were applied, set the output to the original left item
    }



    private static boolean hasEnchantments(ItemStack item)
    {
        return !item.isEmpty()
                && EnchantmentHelper.getEnchantmentsForCrafting(item).size() > 0;
    }

    private static boolean areMutuallyExclusive(ItemStack left, ItemStack right)
    {
        // Check for mutually exclusive enchantments
        Enchantment leftEnchantment = getEnchantment(left);
        Enchantment rightEnchantment = getEnchantment(right);

        if (leftEnchantment != null && rightEnchantment != null)
        {
            String leftName = leftEnchantment.getDescriptionId().replace("enchantment.minecraft.", "");
            String rightName = rightEnchantment.getDescriptionId().replace("enchantment.minecraft.", "");

            // Check against the list of mutually exclusive enchantments
            if ((leftName.equals("bane_of_arthropods") && (rightName.equals("sharpness") || rightName.equals("smite")))
                    || (leftName.equals("blast_protection") && (rightName.equals("fire_protection") || rightName.equals("protection") || rightName.equals("projectile_protection")))
                    || (leftName.equals("channeling") && rightName.equals("riptide"))
                    || (leftName.equals("depth_strider") && rightName.equals("frost_walker"))
                    || (leftName.equals("fortune") && rightName.equals("silk_touch"))
                    || (leftName.equals("infinity") && rightName.equals("mending"))
                    || (leftName.equals("loyalty") && rightName.equals("riptide"))
                    || (leftName.equals("multishot") && rightName.equals("piercing")))
            {
                return true; // Mutually exclusive enchantments found
            }
        }
        return false; // No mutually exclusive enchantments found
    }


    public static Enchantment getEnchantment(ItemStack item)
    {
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(item);
        if (!enchantments.isEmpty())
        {
            return enchantments.keySet().stream().findFirst().orElse(null).get();
        }
        else
        {
            return null;
        }
    }

    public static List<Enchantment> getEnchantmentsList(ItemStack item)
    {
        List<Enchantment> enchantments = new ArrayList<Enchantment>();

        for (int i = 0; i < item.getEnchantments().size(); i++)
        {
            enchantments.addLast(item.getEnchantments().keySet().iterator().next().get());
        }

        return enchantments;
    }
}