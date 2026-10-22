package io.github.safetimamovic.adjustability.compat.jei;

import io.github.safetimamovic.adjustability.Constants;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * JEI info pages on the anvils and the grindstone, explaining what Adjustability changes there. JEI finds this class
 * itself ({@link JeiPlugin} on NeoForge, the {@code jei_mod_plugin} entrypoint on Fabric), so it's never loaded
 * without JEI.
 */
@JeiPlugin
public class AdjustabilityJeiPlugin implements IModPlugin
{
        @Override
        public Identifier getPluginUid()
        {
                return Constants.id("jei");
        }

        @Override
        public void registerRecipes(IRecipeRegistration registration)
        {
                registration.addItemStackInfo(
                        List.of(new ItemStack(Items.ANVIL), new ItemStack(Items.CHIPPED_ANVIL),
                                new ItemStack(Items.DAMAGED_ANVIL)),
                        Component.translatable("adjustability.jei.anvil"));
                registration.addItemStackInfo(new ItemStack(Items.GRINDSTONE),
                        Component.translatable("adjustability.jei.grindstone"));
        }
}
