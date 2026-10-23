package io.github.safetimamovic.adjustability.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.safetimamovic.adjustability.client.AdjustClient;
import io.github.safetimamovic.adjustability.client.AdjustKeys;
import io.github.safetimamovic.adjustability.client.AdjustPanel;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import java.lang.reflect.Field;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;

/**
 * Records the frames of the switch GIF on the README, the docs home page and Modrinth: a netherite pickaxe with
 * Fortune III active and Silk Touch dormant, Left Alt, click Silk Touch, and the tooltip after. Default settings, so
 * the switch is free. Frames go to {@code fabric/versions/26.3/runs/clientgametest/screenshots} as {@code gif_*}; CONTRIBUTING.md has the
 * ffmpeg command that turns them into {@code docs/public/screenshots/switch.gif}.
 */
public class SwitchGifClientGameTest implements FabricClientGameTest
{
        private static final int HOTBAR_0 = InventoryMenu.USE_ROW_SLOT_START;

        @Override
        public void runTest(ClientGameTestContext context)
        {
                try (TestSingleplayerContext world = context.worldBuilder().create())
                {
                        world.getConnection().waitForChunksRender();
                        context.runOnClient(minecraft -> minecraft.player.connection.sendCommand(
                                "adjustability panel reset"));
                        world.getServer().runOnServer(server ->
                        {
                                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                                player.setGameMode(GameType.SURVIVAL);
                                player.getInventory().setItem(0, pickaxe(player));
                        });
                        world.getConnection().waitForClientboundPackets();
                        // Lets the held pickaxe finish its equip animation, so it's the same in every frame.
                        context.waitTicks(20);

                        context.getInput().pressKey(options -> options.keyInventory);
                        context.waitForScreen(InventoryScreen.class);
                        hoverSlot(context, HOTBAR_0);
                        frame(context, "gif_1_tooltip");

                        context.getInput().pressKey(AdjustKeys.ADJUST);
                        frame(context, "gif_2_panel");

                        int[] row = context.computeOnClient(minecraft ->
                        {
                                AdjustPanel panel = AdjustClient.panel(
                                        (AbstractContainerScreen<?>) minecraft.gui.screen());
                                Holder<Enchantment> silkTouch = minecraft.level.registryAccess()
                                        .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
                                return panel.rowCentre(silkTouch).orElseThrow(
                                        () -> new AssertionError("No Silk Touch row in the panel"));
                        });
                        moveTo(context, row[0], row[1]);
                        frame(context, "gif_3_hover_row");

                        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                        world.getConnection().waitForServerboundPackets();
                        world.getConnection().waitForClientboundPackets();
                        context.waitTicks(2);
                        frame(context, "gif_4_switched");

                        context.getInput().pressKey(AdjustKeys.ADJUST);
                        hoverSlot(context, HOTBAR_0);
                        frame(context, "gif_5_tooltip_after");

                        world.getServer().runOnServer(server ->
                        {
                                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                                ItemEnchantments active = Adjuster.activeOf(player.getInventory().getItem(0));
                                if (active.getLevel(enchantment(player, Enchantments.SILK_TOUCH)) != 1)
                                {
                                        throw new AssertionError("Silk Touch should be active, active: " + active);
                                }
                        });
                        context.getInput().pressKey(options -> options.keyInventory);
                        context.waitForScreen(null);
                }
        }

        /** One frame of the GIF, without chat lines or toasts (the switch earns an advancement). */
        private static void frame(ClientGameTestContext context, String name)
        {
                context.waitTick();
                context.runOnClient(minecraft ->
                {
                        minecraft.gui.hud.getChat().clearMessages(false);
                        minecraft.gui.toastManager().clear();
                });
                context.waitTick();
                context.takeScreenshot(name);
        }

        private static ItemStack pickaxe(ServerPlayer player)
        {
                ItemStack pickaxe = new ItemStack(Items.NETHERITE_PICKAXE);
                ItemEnchantments.Mutable active = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                active.set(enchantment(player, Enchantments.FORTUNE), 3);
                active.set(enchantment(player, Enchantments.EFFICIENCY), 5);
                active.set(enchantment(player, Enchantments.UNBREAKING), 3);
                active.set(enchantment(player, Enchantments.MENDING), 1);
                pickaxe.set(DataComponents.ENCHANTMENTS, active.toImmutable());
                ItemEnchantments.Mutable dormant = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                dormant.set(enchantment(player, Enchantments.SILK_TOUCH), 1);
                DormantEnchantments.set(pickaxe, dormant.toImmutable());
                return pickaxe;
        }

        private static Holder<Enchantment> enchantment(ServerPlayer player, ResourceKey<Enchantment> key)
        {
                return player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        }

        private static void hoverSlot(ClientGameTestContext context, int slotIndex)
        {
                int[] position = context.computeOnClient(minecraft ->
                {
                        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) minecraft.gui.screen();
                        Slot slot = screen.getMenu().getSlot(slotIndex);
                        return new int[]{field(screen, "leftPos") + slot.x + 8, field(screen, "topPos") + slot.y + 8};
                });
                moveTo(context, position[0], position[1]);
        }

        private static void moveTo(ClientGameTestContext context, int guiX, int guiY)
        {
                int scale = context.computeOnClient(minecraft -> minecraft.getWindow().getGuiScale());
                context.getInput().setCursorPos(guiX * scale, guiY * scale);
        }

        private static int field(AbstractContainerScreen<?> screen, String name)
        {
                try
                {
                        Field field = AbstractContainerScreen.class.getDeclaredField(name);
                        field.setAccessible(true);
                        return field.getInt(screen);
                }
                catch (ReflectiveOperationException exception)
                {
                        throw new AssertionError("No " + name + " on the container screen", exception);
                }
        }
}
