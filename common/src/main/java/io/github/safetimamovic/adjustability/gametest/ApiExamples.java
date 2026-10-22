package io.github.safetimamovic.adjustability.gametest;

import io.github.safetimamovic.adjustability.api.AdjustabilityApi;
import io.github.safetimamovic.adjustability.api.AdjustabilityListener;
import io.github.safetimamovic.adjustability.api.AnvilEvent;
import io.github.safetimamovic.adjustability.api.SwitchEvent;
import io.github.safetimamovic.adjustability.api.SwitchResult;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The examples on the Java API docs page (docs/guide/java-api.md), compiled and run by the GameTests so they can't
 * rot. Keep the two in sync.
 */
final class ApiExamples
{
        private ApiExamples()
        {
        }

        /** No switching while on fire. */
        static final AdjustabilityListener NO_SWITCHING_WHILE_BURNING = new AdjustabilityListener()
        {
                @Override
                public boolean allowSwitch(SwitchEvent event)
                {
                        return !event.player().isOnFire();
                }
        };

        /** Golden tools can't hold dormant enchantments made in an anvil. */
        static final AdjustabilityListener NO_ADJUSTABLE_GOLD = new AdjustabilityListener()
        {
                @Override
                public boolean allowAnvilResult(AnvilEvent event)
                {
                        return !event.result().is(Items.GOLDEN_PICKAXE);
                }
        };

        /** A reward that adds an enchantment, dormant if it conflicts, and switches to it right away. */
        static SwitchResult giveAndSwitch(ServerPlayer player, ItemStack tool, Holder<Enchantment> enchantment)
        {
                AdjustabilityApi.addEnchantment(tool, enchantment, 1, player.level().registryAccess());
                if (AdjustabilityApi.activeEnchantments(tool).getLevel(enchantment) > 0)
                {
                        return SwitchResult.DONE;
                }
                return AdjustabilityApi.activate(player, tool, enchantment);
        }
}
