package io.github.safetimamovic.adjustability.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelActivation;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import io.github.safetimamovic.adjustability.core.ItemRules;
import io.github.safetimamovic.adjustability.core.TooltipLines;
import io.github.safetimamovic.adjustability.network.CycleEnchantmentPayload;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
//? if >=26.1 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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

        /** Which group of conflicting enchantments the Cycle key steps; Sneak + Cycle picks the next one. */
        private static int cycleGroup;

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

        /**
         * Cycle key presses outside screens: steps the held item's chosen group of conflicting enchantments. With
         * Sneak held it picks the next group instead (boots have Protections and Depth Strider / Frost Walker) and
         * says which, above the hotbar.
         */
        public static void tick(Minecraft minecraft)
        {
                while (AdjustKeys.CYCLE.consumeClick())
                {
                        //? if >=26.1 {
                        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null)
                        //?} else
                        /*if (minecraft.player == null || minecraft.level == null || minecraft.screen != null)*/
                        {
                                continue;
                        }
                        List<Set<Holder<Enchantment>>> groups = groups(minecraft.player.getMainHandItem(),
                                minecraft.level.registryAccess());
                        if (groups.isEmpty())
                        {
                                continue;
                        }
                        if (minecraft.player.isShiftKeyDown())
                        {
                                cycleGroup = (cycleGroup + 1) % groups.size();
                                //? if >=26.1 {
                                minecraft.gui.hud.setOverlayMessage(Component.translatable("adjustability.cycle.group",
                                //?} else
                                /*minecraft.gui.setOverlayMessage(Component.translatable("adjustability.cycle.group",*/
                                        names(groups.get(cycleGroup))), false);
                                continue;
                        }
                        if (cycleGroup >= groups.size())
                        {
                                cycleGroup = 0;
                        }
                        send(new CycleEnchantmentPayload(cycleGroup));
                }
        }

        private static List<Set<Holder<Enchantment>>> groups(ItemStack stack, HolderLookup.Provider registries)
        {
                if (!ItemRules.canAdjust(stack)
                        || !DormantEnchantments.has(stack) && !Adjuster.hasConflict(Adjuster.activeOf(stack)))
                {
                        return List.of();
                }
                return Adjuster.resplit(stack, ItemEnchantments.EMPTY, registries).graph().components();
        }

        /** "Fortune / Silk Touch". */
        private static Component names(Set<Holder<Enchantment>> group)
        {
                MutableComponent names = Component.empty();
                boolean first = true;
                for (Holder<Enchantment> enchantment : group)
                {
                        if (!first)
                        {
                                names.append(" / ");
                        }
                        names.append(enchantment.value().description());
                        first = false;
                }
                return names;
        }

        /** The panel pinned in {@code screen}, if any; for the client GameTest. */
        public static @Nullable AdjustPanel panel(AbstractContainerScreen<?> screen)
        {
                return panel != null && panel.screen() == screen ? panel : null;
        }

        /**
         * A key pressed in a container screen. The Adjust key over an item with conflicting enchantments pins the panel
         * next to it. In TOGGLE mode, pressing it again over the same item (or not over an item) closes it; in HOLD
         * mode, letting go does ({@link #onKeyReleased}). Escape closes it in both.
         *
         * @return whether the key was used
         */
        //? if >=26.1 {
        public static boolean onKey(AbstractContainerScreen<?> screen, @Nullable Slot hovered, int leftPos, int topPos,
                KeyEvent event)
        {
                boolean adjustKey = AdjustKeys.ADJUST.matches(event);
                int key = event.key();
        //?} else {
        /*public static boolean onKey(AbstractContainerScreen<?> screen, @Nullable Slot hovered, int leftPos, int topPos,
                int key, int scanCode)
        {
                boolean adjustKey = AdjustKeys.ADJUST.matches(key, scanCode);
        *///?}
                AdjustPanel open = panel(screen);
                boolean toggle = activation() == PanelActivation.TOGGLE;
                if (open != null && (key == InputConstants.KEY_ESCAPE
                        || adjustKey && toggle && (hovered == null || hovered.index == open.slot())))
                {
                        panel = null;
                        return true;
                }
                if (open != null && adjustKey && (hovered == null || hovered.index == open.slot()))
                {
                        // HOLD: already open over this item.
                        return true;
                }
                if (!adjustKey || hovered == null || !hovered.hasItem())
                {
                        return false;
                }
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player == null || screen.getMenu() != minecraft.player.containerMenu)
                {
                        return false;
                }
                int[] serverSlot = serverSlot(screen, hovered, minecraft.player);
                if (serverSlot == null)
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
                        serverSlot[0], serverSlot[1], leftPos + hovered.x + 16, topPos + hovered.y);
                panel = opened.refresh(minecraft.font) ? opened : null;
                return panel != null;
        }

        /**
         * Where the server finds the hovered item: its open menu's id and the slot's index in it, or null if the
         * server doesn't have the item. The creative inventory is a menu only the client knows (it makes itself the
         * player's open menu there, while the server keeps the player's inventory menu), so its slots are matched to
         * the inventory menu's by the item they show: the hotbar, and on the inventory tab every slot, show the
         * player's own stacks. The creative tabs' items aren't the player's and match nothing.
         */
        private static int @Nullable [] serverSlot(AbstractContainerScreen<?> screen, Slot hovered, LocalPlayer player)
        {
                if (!(screen instanceof CreativeModeInventoryScreen))
                {
                        return new int[]{screen.getMenu().containerId, hovered.index};
                }
                ItemStack item = hovered.getItem();
                for (Slot slot : player.inventoryMenu.slots)
                {
                        if (slot.getItem() == item)
                        {
                                return new int[]{player.inventoryMenu.containerId, slot.index};
                        }
                }
                return null;
        }

        /**
         * A key let go in a container screen: in HOLD mode, letting go of the Adjust key closes the panel.
         *
         * @return whether the key was used
         */
        //? if >=26.1 {
        public static boolean onKeyReleased(AbstractContainerScreen<?> screen, KeyEvent event)
        {
                boolean adjustKey = AdjustKeys.ADJUST.matches(event);
        //?} else {
        /*public static boolean onKeyReleased(AbstractContainerScreen<?> screen, int key, int scanCode)
        {
                boolean adjustKey = AdjustKeys.ADJUST.matches(key, scanCode);
        *///?}
                if (panel(screen) == null || !adjustKey
                        || activation() != PanelActivation.HOLD)
                {
                        return false;
                }
                panel = null;
                return true;
        }

        /**
         * A mouse press in a container screen while the panel is pinned: a click inside goes to the panel, a click
         * outside closes it. Either way the screen doesn't get it, so a click meant to close the panel can't pick up
         * an item.
         *
         * @return whether the click was used
         */
        //? if >=26.1 {
        public static boolean onMouseClicked(AbstractContainerScreen<?> screen, MouseButtonEvent event)
        {
                double x = event.x();
                double y = event.y();
                int button = event.button();
        //?} else {
        /*public static boolean onMouseClicked(AbstractContainerScreen<?> screen, double x, double y, int button)
        {
        *///?}
                AdjustPanel open = panel(screen);
                if (open == null)
                {
                        return false;
                }
                if (!open.contains(x, y))
                {
                        panel = null;
                }
                else if (button == InputConstants.MOUSE_BUTTON_LEFT)
                {
                        open.click(x, y);
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

        /** How the Adjust key works: the server's {@code panel_activation} if it sets one, else the player's own. */
        private static PanelActivation activation()
        {
                return switch (AdjustabilityConfig.toggle().panelActivation())
                {
                        case TOGGLE -> PanelActivation.TOGGLE;
                        case HOLD -> PanelActivation.HOLD;
                        case PLAYER -> AdjustabilityConfig.player().panel().activation();
                };
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
