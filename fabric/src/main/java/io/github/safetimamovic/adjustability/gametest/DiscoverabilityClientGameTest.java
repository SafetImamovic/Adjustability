package io.github.safetimamovic.adjustability.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.safetimamovic.adjustability.client.AdjustClient;
import io.github.safetimamovic.adjustability.client.AdjustKeys;
import io.github.safetimamovic.adjustability.client.AdjustPanel;
import io.github.safetimamovic.adjustability.client.AdjustToast;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.TutorialStep;
import io.github.safetimamovic.adjustability.config.CreatorConfig;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

/** First-install path from a real anvil result through both tutorial steps. */
public class DiscoverabilityClientGameTest implements FabricClientGameTest
{
        @Override
        public void runTest(ClientGameTestContext context)
        {
                context.runOnClient(minecraft ->
                {
                        PlayerConfig.tutorial(TutorialStep.OPEN_PANEL);
                        PlayerConfig.resetPanel();
                });
                try (TestSingleplayerContext world = context.worldBuilder().create())
                {
                        world.getConnection().waitForChunksRender();
                        world.getServer().runOnServer(server ->
                        {
                                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                                player.setGameMode(GameType.SURVIVAL);
                                player.setExperienceLevels(30);
                                BlockPos position = player.blockPosition().east();
                                player.level().setBlockAndUpdate(position, Blocks.ANVIL.defaultBlockState());
                                player.openMenu(Blocks.ANVIL.defaultBlockState()
                                        .getMenuProvider(player.level(), position));
                                AnvilMenu anvil = (AnvilMenu) player.containerMenu;
                                Holder<Enchantment> fortune = player.level().registryAccess()
                                        .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
                                Holder<Enchantment> silk = player.level().registryAccess()
                                        .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
                                ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                                pickaxe.enchant(fortune, 3);
                                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                                ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                                stored.set(silk, 1);
                                book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
                                anvil.getSlot(AnvilMenu.INPUT_SLOT).set(pickaxe);
                                anvil.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(book);
                                if (!DormantEnchantments.has(anvil.getSlot(AnvilMenu.RESULT_SLOT).getItem()))
                                {
                                        throw new AssertionError("Anvil should offer an item with dormant Silk Touch");
                                }
                                anvil.broadcastChanges();
                        });
                        world.getConnection().waitForClientboundPackets();
                        context.waitForScreen(AnvilScreen.class);
                        context.waitTicks(25);
                        context.runOnClient(minecraft ->
                        {
                                if (minecraft.gui.toastManager().getToast(AdjustToast.class, Toast.NO_TOKEN) != null)
                                {
                                        throw new AssertionError("An anvil preview must not start the tutorial");
                                }
                        });
                        hoverSlot(context, AnvilMenu.RESULT_SLOT);
                        context.waitTick();
                        context.takeScreenshot("discoverability_0_anvil_preview");
                        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                        world.getConnection().waitForServerboundPackets();
                        world.getConnection().waitForClientboundPackets();
                        context.runOnClient(minecraft ->
                        {
                                if (!DormantEnchantments.has(minecraft.player.containerMenu.getCarried())
                                        || minecraft.gui.toastManager()
                                                .getToast(AdjustToast.class, Toast.NO_TOKEN) != null)
                                {
                                        throw new AssertionError("A carried result must not start the tutorial");
                                }
                        });
                        int destination = context.computeOnClient(minecraft ->
                                minecraft.player.containerMenu.slots.size() - 1);
                        hoverSlot(context, destination);
                        context.waitTick();
                        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                        world.getConnection().waitForServerboundPackets();
                        world.getConnection().waitForClientboundPackets();
                        world.getServer().runOnServer(server ->
                        {
                                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                                if (player.inventoryMenu.slots.stream().map(Slot::getItem)
                                        .noneMatch(item -> item.is(Items.DIAMOND_PICKAXE)))
                                {
                                        throw new AssertionError("Anvil result should enter inventory; carried: "
                                                + player.containerMenu.getCarried() + ", result: "
                                                + player.containerMenu.getSlot(AnvilMenu.RESULT_SLOT).getItem());
                                }
                        });
                        context.waitFor(minecraft -> minecraft.gui.toastManager()
                                .getToast(AdjustToast.class, Toast.NO_TOKEN) != null);
                        context.waitTicks(15);
                        context.takeScreenshot("discoverability_1_anvil_toast");
                        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
                        context.waitForScreen(null);
                        context.getInput().pressKey(options -> options.keyInventory);
                        context.waitForScreen(InventoryScreen.class);
                        int slot = context.computeOnClient(minecraft -> minecraft.player.containerMenu.slots.stream()
                                .filter(candidate -> candidate.getItem().is(Items.DIAMOND_PICKAXE)).findFirst()
                                .orElseThrow(() -> new AssertionError("Anvil result did not reach inventory")).index);
                        hoverSlot(context, slot);
                        context.waitTick();
                        context.takeScreenshot("discoverability_2_hint");
                        context.runOnClient(minecraft ->
                        {
                                AdjustToast toast = minecraft.gui.toastManager()
                                        .getToast(AdjustToast.class, Toast.NO_TOKEN);
                                toast.update(minecraft.gui.toastManager(), 60000);
                                if (toast.getWantedVisibility() != Toast.Visibility.SHOW)
                                {
                                        throw new AssertionError("The first reminder must stay until the panel opens");
                                }
                        });
                        context.getInput().pressKey(AdjustKeys.ADJUST);
                        context.waitTick();
                        step(context, TutorialStep.SWITCH);
                        context.waitFor(minecraft -> minecraft.gui.toastManager()
                                .getToast(AdjustToast.class, Toast.NO_TOKEN) == null);
                        int[] row = context.computeOnClient(minecraft ->
                        {
                                AdjustPanel panel = AdjustClient.panel(
                                        (AbstractContainerScreen<?>) minecraft.gui.screen());
                                Holder<Enchantment> silk = minecraft.level.registryAccess()
                                        .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
                                return panel.rowCentre(silk).orElseThrow();
                        });
                        moveTo(context, row[0], row[1]);
                        allowSwitch(world, false);
                        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                        world.getConnection().waitForServerboundPackets();
                        world.getConnection().waitForClientboundPackets();
                        context.waitTicks(3);
                        step(context, TutorialStep.SWITCH);
                        allowSwitch(world, true);
                        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                        world.getConnection().waitForServerboundPackets();
                        world.getConnection().waitForClientboundPackets();
                        context.waitFor(minecraft -> PlayerConfig.tutorial() == TutorialStep.DONE);
                        step(context, TutorialStep.DONE);
                        context.waitTicks(15);
                        context.takeScreenshot("discoverability_3_cycle_toast");
                        world.getServer().runOnServer(server ->
                        {
                                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                                ItemStack stack = player.inventoryMenu.slots.stream().map(Slot::getItem)
                                        .filter(item -> item.is(Items.DIAMOND_PICKAXE)).findFirst().orElseThrow();
                                Holder<Enchantment> silk = player.level().registryAccess()
                                        .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
                                if (!DormantEnchantments.has(stack) || Adjuster.activeOf(stack).getLevel(silk) != 1)
                                {
                                        throw new AssertionError("Switched anvil result should keep both enchantments");
                                }
                        });
                        context.runOnClient(minecraft ->
                        {
                                AdjustToast toast = minecraft.gui.toastManager()
                                        .getToast(AdjustToast.class, Toast.NO_TOKEN);
                                toast.update(minecraft.gui.toastManager(), 9000);
                                if (toast.getWantedVisibility() != Toast.Visibility.HIDE)
                                {
                                        throw new AssertionError("The Cycle reminder should expire");
                                }
                        });
                        context.getInput().pressKey(AdjustKeys.ADJUST);
                        context.getInput().pressKey(options -> options.keyInventory);
                        context.waitForScreen(null);
                        context.runOnClient(minecraft -> minecraft.player.connection
                                .sendCommand("adjustability tutorial"));
                        context.waitTick();
                        step(context, TutorialStep.OPEN_PANEL);
                }
                finally
                {
                        context.runOnClient(minecraft -> PlayerConfig.tutorial(TutorialStep.DONE));
                }
        }

        private static void step(ClientGameTestContext context, TutorialStep expected)
        {
                context.runOnClient(minecraft ->
                {
                        Path file = minecraft.gameDirectory.toPath().resolve("config").resolve(PlayerConfig.FILE_NAME);
                        try
                        {
                                if (PlayerConfig.tutorial() != expected
                                        || !Files.readString(file).contains("tutorial = \"" + expected + "\""))
                                {
                                        throw new AssertionError("Tutorial progress should be " + expected
                                                + " and saved to disk");
                                }
                        }
                        catch (IOException exception)
                        {
                                throw new AssertionError(exception);
                        }
                });
        }

        private static void allowSwitch(TestSingleplayerContext world, boolean allowed)
        {
                world.getServer().runOnServer(server ->
                {
                        BooleanValue value = (BooleanValue) CreatorConfig.SPEC.getValues().get("toggle.enabled");
                        value.set(allowed);
                });
        }

        private static void hoverSlot(ClientGameTestContext context, int index)
        {
                int[] position = context.computeOnClient(minecraft ->
                {
                        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) minecraft.gui.screen();
                        Slot slot = screen.getMenu().getSlot(index);
                        try
                        {
                                Field left = AbstractContainerScreen.class.getDeclaredField("leftPos");
                                Field top = AbstractContainerScreen.class.getDeclaredField("topPos");
                                left.setAccessible(true);
                                top.setAccessible(true);
                                return new int[]{left.getInt(screen) + slot.x + 8, top.getInt(screen) + slot.y + 8};
                        }
                        catch (ReflectiveOperationException exception)
                        {
                                throw new AssertionError(exception);
                        }
                });
                moveTo(context, position[0], position[1]);
        }

        private static void moveTo(ClientGameTestContext context, int x, int y)
        {
                int scale = context.computeOnClient(minecraft -> minecraft.getWindow().getGuiScale());
                context.getInput().setCursorPos(x * scale, y * scale);
        }
}
