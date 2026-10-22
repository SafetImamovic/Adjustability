package io.github.safetimamovic.adjustability.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import io.github.safetimamovic.adjustability.core.ItemRules;
import io.github.safetimamovic.adjustability.core.TooltipLines;
import io.github.safetimamovic.adjustability.network.CycleEnchantmentPayload;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Client side of switching: the Adjust panel in container screens, the Cycle key in the world, and the tooltip hint.
 * Each loader calls {@link #init} with its way of sending payloads, and {@link #tick} every client tick. The container
 * screen hooks come from {@code AbstractContainerScreenMixin}.
 */
public final class AdjustClient
{
        private static Consumer<CustomPacketPayload> sender = payload ->
        {
        };

        /** The pinned panel, if any. At most one, in the screen it was opened in. */
        private static @Nullable AdjustPanel panel;

        /** The release of a click the panel took, so the screen doesn't see half a click. */
        private static boolean swallowRelease;

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

        /** The panel pinned in {@code screen}, if any; for the client GameTest. */
        public static @Nullable AdjustPanel panel(AbstractContainerScreen<?> screen)
        {
                return panel != null && panel.screen() == screen ? panel : null;
        }

        /**
         * A key pressed in a container screen. The Adjust key over an item with conflicting enchantments pins the panel
         * next to it (or closes it if it's already pinned there); Escape or the Adjust key elsewhere close it.
         *
         * @return whether the key was used
         */
        public static boolean onKey(AbstractContainerScreen<?> screen, @Nullable Slot hovered, int leftPos, int topPos,
                KeyEvent event)
        {
                AdjustPanel open = panel(screen);
                boolean adjustKey = AdjustKeys.ADJUST.matches(event);
                if (open != null && (event.key() == InputConstants.KEY_ESCAPE
                        || adjustKey && (hovered == null || hovered.index == open.slot())))
                {
                        panel = null;
                        return true;
                }
                if (!adjustKey || hovered == null || !hovered.hasItem())
                {
                        return false;
                }
                Minecraft minecraft = Minecraft.getInstance();
                // The creative inventory shows items through a client-only menu the server knows nothing about.
                if (minecraft.player == null || screen.getMenu() != minecraft.player.containerMenu)
                {
                        return false;
                }
                ItemStack stack = hovered.getItem();
                if (!ItemRules.canAdjust(stack)
                        || !DormantEnchantments.has(stack) && !Adjuster.hasConflict(Adjuster.activeOf(stack)))
                {
                        return false;
                }
                AdjustPanel opened = new AdjustPanel(screen, screen.getMenu().containerId, hovered.index,
                        leftPos + hovered.x + 16, topPos + hovered.y);
                panel = opened.refresh(minecraft.font) ? opened : null;
                return panel != null;
        }

        /**
         * A mouse press in a container screen while the panel is pinned: a click inside goes to the panel, a click
         * outside closes it. Either way the screen doesn't get it, so a click meant to close the panel can't pick up
         * an item.
         *
         * @return whether the click was used
         */
        public static boolean onMouseClicked(AbstractContainerScreen<?> screen, MouseButtonEvent event)
        {
                AdjustPanel open = panel(screen);
                if (open == null)
                {
                        return false;
                }
                if (!open.contains(event.x(), event.y()))
                {
                        panel = null;
                }
                else if (event.button() == InputConstants.MOUSE_BUTTON_LEFT)
                {
                        open.click(event.x(), event.y());
                }
                swallowRelease = true;
                return true;
        }

        /** @return whether the release belongs to a click the panel took */
        public static boolean onMouseReleased()
        {
                boolean swallow = swallowRelease;
                swallowRelease = false;
                return swallow;
        }

        /** Whether the screen should skip its item tooltip and scrolling: the panel is open. */
        public static boolean isPanelOpen(AbstractContainerScreen<?> screen)
        {
                return panel(screen) != null;
        }

        /** Draws the panel on top of everything else in the screen, closing it if its item is gone. */
        public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, int mouseX,
                int mouseY)
        {
                AdjustPanel open = panel(screen);
                if (open == null)
                {
                        return;
                }
                Minecraft minecraft = Minecraft.getInstance();
                if (!open.refresh(minecraft.font))
                {
                        panel = null;
                        return;
                }
                open.render(graphics, minecraft.font, mouseX, mouseY);
        }

        /** The screen closed: forget its panel. */
        public static void onScreenRemoved(AbstractContainerScreen<?> screen)
        {
                if (panel(screen) != null)
                {
                        panel = null;
                }
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
