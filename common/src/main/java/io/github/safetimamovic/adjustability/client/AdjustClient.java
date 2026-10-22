package io.github.safetimamovic.adjustability.client;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import io.github.safetimamovic.adjustability.core.TooltipLines;
import io.github.safetimamovic.adjustability.network.CycleEnchantmentPayload;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Client side of switching: the Adjust key in inventories, the Cycle key in the world, and the tooltip hint. Each
 * loader calls {@link #init} with its way of sending payloads, and {@link #tick} every client tick.
 */
public final class AdjustClient
{
        private static Consumer<CustomPacketPayload> sender = payload ->
        {
        };

        /** Set while the Adjust screen replaces a container screen, so the container stays open (see the mixin). */
        private static boolean openingAdjust;

        private AdjustClient()
        {
        }

        public static void init(Consumer<CustomPacketPayload> payloadSender)
        {
                sender = payloadSender;
                TooltipLines.setClientHooks(() -> AdjustabilityConfig.player().showDormant(), AdjustClient::hint);
        }

        public static void send(CustomPacketPayload payload)
        {
                sender.accept(payload);
        }

        /** Cycle key presses outside screens. */
        public static void tick(Minecraft minecraft)
        {
                while (AdjustKeys.CYCLE.consumeClick())
                {
                        if (minecraft.player != null && minecraft.gui.screen() == null)
                        {
                                send(new CycleEnchantmentPayload(0));
                        }
                }
        }

        /**
         * A key pressed in a container screen. The Adjust key over an item that holds conflicting enchantments opens
         * the Adjust screen for it.
         *
         * @return whether the key was used
         */
        public static boolean onContainerKey(AbstractContainerScreen<?> screen, @Nullable Slot hovered, KeyEvent event)
        {
                Minecraft minecraft = Minecraft.getInstance();
                if (!AdjustKeys.ADJUST.matches(event) || hovered == null || !hovered.hasItem()
                        || minecraft.player == null || minecraft.level == null)
                {
                        return false;
                }
                // The creative inventory shows items through a client-only menu the server knows nothing about.
                if (screen.getMenu() != minecraft.player.containerMenu)
                {
                        return false;
                }
                ItemStack stack = hovered.getItem();
                if (!DormantEnchantments.has(stack) && !Adjuster.hasConflict(Adjuster.activeOf(stack)))
                {
                        return false;
                }
                openingAdjust = true;
                minecraft.gui.setScreen(new AdjustScreen(screen, screen.getMenu().containerId, hovered.index,
                        stack.getHoverName()));
                openingAdjust = false;
                return true;
        }

        /** True once, while the Adjust screen is replacing a container screen. */
        public static boolean consumeOpeningAdjust()
        {
                boolean opening = openingAdjust;
                openingAdjust = false;
                return opening;
        }

        private static @Nullable Component hint()
        {
                if (!AdjustabilityConfig.player().showHint() || AdjustKeys.ADJUST.isUnbound())
                {
                        return null;
                }
                return Component.translatable("adjustability.tooltip.hint", AdjustKeys.ADJUST.getTranslatedKeyMessage())
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        }
}
