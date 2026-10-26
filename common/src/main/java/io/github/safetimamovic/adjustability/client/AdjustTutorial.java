package io.github.safetimamovic.adjustability.client;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.TutorialStep;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import io.github.safetimamovic.adjustability.core.ItemRules;
import io.github.safetimamovic.adjustability.network.ToggleEnchantmentPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

/** Client tutorial progress belongs to the install, not to a world or server. */
public final class AdjustTutorial
{
        private static @Nullable LocalPlayer player;
        private static @Nullable AdjustToast toast;
        private static @Nullable Pending pending;
        private static int remainingTicks;
        private static int inventoryDelay;
        private static @Nullable TutorialStep shownStep;

        private record Pending(Slot slot, ItemStack before, Holder<Enchantment> target, boolean none)
        {
        }

        private AdjustTutorial()
        {
        }

        public static void tick(Minecraft minecraft)
        {
                if (player != minecraft.player)
                {
                        clear();
                        player = minecraft.player;
                }
                if (player == null || minecraft.level == null || !PlayerConfig.canChange())
                {
                        return;
                }
                TutorialStep step = PlayerConfig.tutorial();
                if (shownStep != null && step != shownStep)
                {
                        hide();
                }
                if (step == TutorialStep.OPEN_PANEL && toast == null && inventoryDelay-- <= 0)
                {
                        inventoryDelay = 20;
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
                        {
                                ItemStack stack = player.getInventory().getItem(i);
                                if (ItemRules.canAdjust(stack) && DormantEnchantments.has(stack)
                                        && !DormantEnchantments.get(stack, minecraft.level.registryAccess()).isEmpty())
                                {
                                        Component description = AdjustKeys.ADJUST.isUnbound()
                                                ? Component.translatable("adjustability.tutorial.bind_adjust")
                                                : Component.translatable("adjustability.tutorial.open",
                                                        AdjustKeys.ADJUST.getTranslatedKeyMessage());
                                        show(minecraft, "open", description, false, step);
                                        break;
                                }
                        }
                }
                if (step != TutorialStep.SWITCH || pending == null)
                {
                        pending = null;
                        return;
                }
                ItemStack stack = pending.slot().getItem();
                if (ItemStack.isSameItem(stack, pending.before())
                        && !Adjuster.activeOf(stack).equals(Adjuster.activeOf(pending.before()))
                        && (Adjuster.activeOf(stack).getLevel(pending.target()) == 0) == pending.none())
                {
                        PlayerConfig.tutorial(TutorialStep.DONE);
                        Component description = AdjustKeys.CYCLE.isUnbound()
                                ? Component.translatable("adjustability.tutorial.bind_cycle")
                                : Component.translatable("adjustability.tutorial.cycle",
                                        AdjustKeys.CYCLE.getTranslatedKeyMessage());
                        show(minecraft, "cycle", description, true, TutorialStep.DONE);
                        pending = null;
                }
                else if (--remainingTicks <= 0)
                {
                        pending = null;
                }
        }

        /** A successfully opened panel completes the first step even if its toast was never needed. */
        static void openedPanel()
        {
                if (PlayerConfig.tutorial() == TutorialStep.OPEN_PANEL)
                {
                        PlayerConfig.tutorial(TutorialStep.SWITCH);
                        hide();
                }
        }

        /** Wait for the server's item update; sending a request alone does not complete the tutorial. */
        static void requestedSwitch(ToggleEnchantmentPayload payload)
        {
                Minecraft minecraft = Minecraft.getInstance();
                if (PlayerConfig.tutorial() != TutorialStep.SWITCH || minecraft.player == null
                        || minecraft.level == null)
                {
                        return;
                }
                AbstractContainerMenu menu = minecraft.player.containerMenu;
                if (menu.containerId != payload.containerId())
                {
                        menu = minecraft.player.inventoryMenu;
                }
                if (menu.containerId != payload.containerId() || payload.slot() < 0
                        || payload.slot() >= menu.slots.size())
                {
                        return;
                }
                Slot slot = menu.getSlot(payload.slot());
                minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(payload.target())
                        .ifPresent(target ->
                        {
                                pending = new Pending(slot, slot.getItem().copy(), target, payload.none());
                                remainingTicks = 100;
                        });
        }

        /** Restarts both steps without changing the player's other hint settings. */
        public static int restart()
        {
                clear();
                return PlayerConfig.tutorial(TutorialStep.OPEN_PANEL) ? 1 : 0;
        }

        private static void show(Minecraft minecraft, String title, Component description, boolean timed,
                TutorialStep step)
        {
                hide();
                toast = new AdjustToast(minecraft.font,
                        Component.translatable("adjustability.tutorial." + title + ".title"), description, timed);
                shownStep = step;
                AdjustToast.add(minecraft, toast);
        }

        private static void hide()
        {
                if (toast != null)
                {
                        toast.hide();
                        toast = null;
                }
                shownStep = null;
        }

        private static void clear()
        {
                hide();
                pending = null;
                inventoryDelay = 0;
        }
}
