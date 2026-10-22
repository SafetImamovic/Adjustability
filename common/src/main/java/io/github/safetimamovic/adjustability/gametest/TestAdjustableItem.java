package io.github.safetimamovic.adjustability.gametest;

import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.ModRegistries;
import io.github.safetimamovic.adjustability.api.AdjustableItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * An item that uses {@link AdjustableItem}, for the GameTests only: registered in the development environment, never
 * in a released game. It holds at most one dormant enchantment, and none once it's renamed.
 */
public final class TestAdjustableItem extends Item implements AdjustableItem
{
        private static @Nullable Item instance;

        private TestAdjustableItem(Item.Properties properties)
        {
                super(properties);
        }

        public static void register(ModRegistries.Registrar registrar)
        {
                ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Constants.id("test_adjustable_item"));
                registrar.register(Registries.ITEM, key.identifier(), () ->
                {
                        instance = new TestAdjustableItem(new Item.Properties().setId(key));
                        return instance;
                });
        }

        /** The registered item; null outside the development environment. */
        static @Nullable Item instance()
        {
                return instance;
        }

        @Override
        public boolean canHoldDormant(ItemStack stack)
        {
                return !stack.has(DataComponents.CUSTOM_NAME);
        }

        @Override
        public int maxDormant(ItemStack stack)
        {
                return 1;
        }
}
