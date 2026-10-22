package io.github.safetimamovic.adjustability.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.safetimamovic.adjustability.client.AdjustClient;
import io.github.safetimamovic.adjustability.client.AdjustKeys;
import io.github.safetimamovic.adjustability.client.AdjustPanel;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
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
 * Drives a real client through the Adjust panel: open the inventory, hover a Fortune pickaxe holding a dormant Silk
 * Touch, press the Adjust key, click the Silk Touch row, and check the server switched it and the inventory is still
 * open. Screenshots go to {@code runs/clientgametest/screenshots}. Run with {@code ./gradlew :fabric:runClientGametest}.
 * Fabric only: NeoForge has no client test framework; the panel and mixin are the same common code on both.
 */
public class AdjustPanelClientGameTest implements FabricClientGameTest
{
        /** The first hotbar slot in the player's inventory menu. */
        private static final int HOTBAR_0 = InventoryMenu.USE_ROW_SLOT_START;

        @Override
        public void runTest(ClientGameTestContext context)
        {
                try (TestSingleplayerContext world = context.worldBuilder().create())
                {
                        world.getConnection().waitForChunksRender();
                        command(context, "panel reset");
                        world.getServer().runOnServer(server ->
                        {
                                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                                // Survival: the creative inventory uses a client-only menu, where Adjust is off.
                                player.setGameMode(GameType.SURVIVAL);
                                player.getInventory().setItem(0, pickaxe(player));
                        });
                        world.getConnection().waitForClientboundPackets();

                        context.getInput().pressKey(options -> options.keyInventory);
                        context.waitForScreen(InventoryScreen.class);
                        hoverSlot(context, HOTBAR_0);
                        context.waitTick();
                        context.takeScreenshot("adjustability_1_tooltip");

                        context.getInput().pressKey(AdjustKeys.ADJUST);
                        context.waitTick();
                        context.takeScreenshot("adjustability_2_panel");

                        int[] row = context.computeOnClient(minecraft ->
                        {
                                AdjustPanel panel = AdjustClient.panel(
                                        (AbstractContainerScreen<?>) minecraft.gui.screen());
                                if (panel == null)
                                {
                                        throw new AssertionError("Left Alt should have pinned the panel");
                                }
                                Holder<Enchantment> silkTouch = minecraft.level.registryAccess()
                                        .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
                                return panel.rowCentre(silkTouch).orElseThrow(
                                        () -> new AssertionError("No Silk Touch row in the panel"));
                        });
                        moveTo(context, row[0], row[1]);
                        context.waitTick();
                        context.takeScreenshot("adjustability_3_hover_row");
                        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                        world.getConnection().waitForServerboundPackets();
                        world.getConnection().waitForClientboundPackets();
                        context.waitTicks(2);
                        context.takeScreenshot("adjustability_4_after_switch");

                        world.getServer().runOnServer(server ->
                        {
                                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                                ItemEnchantments active = Adjuster.activeOf(player.getInventory().getItem(0));
                                Holder<Enchantment> silkTouch = enchantment(player, Enchantments.SILK_TOUCH);
                                if (active.getLevel(silkTouch) != 1)
                                {
                                        throw new AssertionError("Silk Touch should be active, active: " + active);
                                }
                                if (player.containerMenu != player.inventoryMenu)
                                {
                                        throw new AssertionError("The inventory should still be the open menu");
                                }
                                if (!player.containerMenu.getCarried().isEmpty())
                                {
                                        throw new AssertionError("The click must not have picked anything up");
                                }
                        });

                        // The panel's look from chat: settings change, are saved, and show right away.
                        command(context, "panel side left");
                        command(context, "panel border_top_color #FF00E5FF");
                        command(context, "panel bracket_color 00E5FF");
                        command(context, "panel bracket_length 99");
                        String side = context.computeOnClient(minecraft -> String.valueOf(
                                PlayerConfig.current(PlayerConfig.PANEL_SETTINGS.getFirst())));
                        int bracketLength = context.computeOnClient(minecraft -> (Integer) PlayerConfig.current(
                                PlayerConfig.PANEL_SETTINGS.get(6)));
                        if (!side.equals("LEFT") || bracketLength != 9)
                        {
                                throw new AssertionError("side should be LEFT and an out-of-range bracket_length "
                                        + "refused, got " + side + " and " + bracketLength);
                        }
                        context.waitTick();
                        context.takeScreenshot("adjustability_5_custom_style");
                        command(context, "panel reset");

                        context.getInput().pressKey(AdjustKeys.ADJUST);
                        context.waitTick();
                        boolean closed = context.computeOnClient(
                                minecraft -> AdjustClient.panel((AbstractContainerScreen<?>) minecraft.gui.screen())
                                        == null);
                        if (!closed)
                        {
                                throw new AssertionError("Left Alt again should close the panel");
                        }
                        context.getInput().pressKey(options -> options.keyInventory);
                        context.waitForScreen(null);
                }
        }

        /** Runs an {@code /adjustability} client command as if typed in chat. */
        private static void command(ClientGameTestContext context, String arguments)
        {
                context.runOnClient(minecraft -> minecraft.player.connection.sendCommand("adjustability " + arguments));
                context.waitTick();
        }

        private static ItemStack pickaxe(ServerPlayer player)
        {
                ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                ItemEnchantments.Mutable active = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                active.set(enchantment(player, Enchantments.FORTUNE), 3);
                active.set(enchantment(player, Enchantments.EFFICIENCY), 5);
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

        /** Puts the cursor over a slot of the open container screen. */
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

        /** Moves the cursor to a point in screen (GUI) coordinates; the input API takes window pixels. */
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
