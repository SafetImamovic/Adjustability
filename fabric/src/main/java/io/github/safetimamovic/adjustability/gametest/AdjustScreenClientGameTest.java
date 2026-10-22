package io.github.safetimamovic.adjustability.gametest;

import io.github.safetimamovic.adjustability.client.AdjustKeys;
import io.github.safetimamovic.adjustability.client.AdjustScreen;
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
 * Drives a real client through the Adjust screen: open the inventory, hover a Fortune pickaxe holding a dormant Silk
 * Touch, press the Adjust key, click Silk Touch, and check the server switched it and the inventory is still open.
 * Screenshots go to {@code runs/clientgametest/screenshots}. Run with {@code ./gradlew :fabric:runClientGametest}.
 * Fabric only: NeoForge has no client test framework; the screen and mixins are the same common code on both.
 */
public class AdjustScreenClientGameTest implements FabricClientGameTest
{
        /** The first hotbar slot in the player's inventory menu. */
        private static final int HOTBAR_0 = InventoryMenu.USE_ROW_SLOT_START;

        @Override
        public void runTest(ClientGameTestContext context)
        {
                try (TestSingleplayerContext world = context.worldBuilder().create())
                {
                        world.getConnection().waitForChunksRender();
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
                        context.waitForScreen(AdjustScreen.class);
                        context.takeScreenshot("adjustability_2_adjust_screen");

                        context.clickScreenButton("enchantment.minecraft.silk_touch");
                        world.getConnection().waitForServerboundPackets();
                        world.getConnection().waitForClientboundPackets();
                        context.waitTicks(2);
                        context.takeScreenshot("adjustability_3_after_switch");

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
                        });

                        context.clickScreenButton("gui.done");
                        context.waitForScreen(InventoryScreen.class);
                        context.getInput().pressKey(options -> options.keyInventory);
                        context.waitForScreen(null);
                }
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

        private static Holder<Enchantment> enchantment(ServerPlayer player,
                ResourceKey<Enchantment> key)
        {
                return player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        }

        /** Puts the cursor over a slot of the open container screen, in window pixels. */
        private static void hoverSlot(ClientGameTestContext context, int slotIndex)
        {
                double[] position = context.computeOnClient(minecraft ->
                {
                        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) minecraft.gui.screen();
                        Slot slot = screen.getMenu().getSlot(slotIndex);
                        int scale = minecraft.getWindow().getGuiScale();
                        return new double[]{(field(screen, "leftPos") + slot.x + 8) * scale,
                                (field(screen, "topPos") + slot.y + 8) * scale};
                });
                context.getInput().setCursorPos(position[0], position[1]);
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
