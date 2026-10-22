package io.github.safetimamovic.adjustability.network;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.core.Toggler;
import io.github.safetimamovic.adjustability.api.SwitchResult;
import io.github.safetimamovic.adjustability.api.SwitchSource;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Server side of the toggle payloads. Nothing from the client is trusted: the menu must be the one open, the slot one
 * the player could take the item from, and the switch itself is checked again by {@link Toggler}. Requests that fail
 * those checks are dropped silently; a failed switch is reported above the hotbar.
 */
public final class ToggleNetwork
{
        private ToggleNetwork()
        {
        }

        public static void onToggle(ServerPlayer player, ToggleEnchantmentPayload payload)
        {
                AbstractContainerMenu menu = player.containerMenu;
                if (menu.containerId != payload.containerId() || payload.slot() < 0
                        || payload.slot() >= menu.slots.size())
                {
                        return;
                }
                Slot slot = menu.getSlot(payload.slot());
                // Result slots (anvil, crafting) show a preview, not an item the player has yet.
                if (slot.container instanceof ResultContainer || !slot.mayPickup(player)
                        || !slot.allowModification(player))
                {
                        return;
                }
                Optional<Holder.Reference<Enchantment>> target = player.level().registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT).get(payload.target());
                if (target.isEmpty())
                {
                        return;
                }
                ItemStack stack = slot.getItem();
                SwitchResult outcome = Toggler.activate(player, stack, target.get(), SwitchSource.PANEL);
                if (outcome == SwitchResult.DONE)
                {
                        slot.setChanged();
                        menu.broadcastChanges();
                }
                Toggler.report(player, outcome, target.get(), stack);
        }

        public static void onCycle(ServerPlayer player, CycleEnchantmentPayload payload)
        {
                if (!AdjustabilityConfig.toggle().allowInWorldCycle())
                {
                        Toggler.report(player, SwitchResult.DISABLED, null, ItemStack.EMPTY);
                        return;
                }
                ItemStack stack = player.getMainHandItem();
                Optional<Holder<Enchantment>> target = Toggler.nextInCycle(stack, payload.component(),
                        player.level().registryAccess());
                if (target.isEmpty())
                {
                        return;
                }
                Toggler.report(player, Toggler.activate(player, stack, target.get(), SwitchSource.CYCLE_KEY),
                        target.get(), stack);
        }
}
