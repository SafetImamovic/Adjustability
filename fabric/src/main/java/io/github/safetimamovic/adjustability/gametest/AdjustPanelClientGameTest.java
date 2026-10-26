package io.github.safetimamovic.adjustability.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.safetimamovic.adjustability.client.AdjustClient;
import io.github.safetimamovic.adjustability.client.AdjustKeys;
import io.github.safetimamovic.adjustability.client.AdjustPanel;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ServerPanelActivation;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.TutorialStep;
import io.github.safetimamovic.adjustability.config.CreatorConfig;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * Drives a real client through the Adjust panel: open the inventory, hover a Fortune pickaxe holding a dormant Silk
 * Touch, press the Adjust key, click the Silk Touch row, and check the server switched it and the inventory is still
 * open. Screenshots go to {@code fabric/versions/26.3/runs/clientgametest/screenshots}. Run with {@code ./gradlew :fabric:26.3:runClientGametest}.
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
                        // Mining switches cost a lapis, Protections a level, the boots group the default (free).
                        groupCosts(world, List.of("#minecraft:exclusive_set/mining = ITEM 1 minecraft:lapis_lazuli",
                                "#minecraft:exclusive_set/armor = XP_LEVELS 1"));
                        world.getServer().runOnServer(server ->
                        {
                                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                                // Survival: the creative inventory uses a client-only menu, where Adjust is off.
                                player.setGameMode(GameType.SURVIVAL);
                                player.getInventory().setItem(0, pickaxe(player));
                                player.getInventory().setItem(1, boots(player));
                                player.getInventory().setItem(9, new ItemStack(Items.LAPIS_LAZULI, 5));
                                player.setExperienceLevels(10);
                        });
                        world.getConnection().waitForClientboundPackets();

                        context.getInput().pressKey(options -> options.keyInventory);
                        context.waitForScreen(InventoryScreen.class);
                        hoverSlot(context, HOTBAR_0);
                        context.waitTick();
                        hint(context);
                        cleanScreen(context);
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
                        cleanScreen(context);
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
                                if (player.getInventory().getItem(9).getCount() != 4)
                                {
                                        throw new AssertionError("The mining group's cost is one lapis, lapis left: "
                                                + player.getInventory().getItem(9).getCount());
                                }
                        });

                        // The panel's look from chat: settings change, are saved, and show right away.
                        command(context, "panel side left");
                        command(context, "panel border_top_color #FF00E5FF");
                        command(context, "panel bracket_color 00E5FF");
                        command(context, "panel bracket_length 99");
                        String side = context.computeOnClient(minecraft -> String.valueOf(setting("side")));
                        int bracketLength = context.computeOnClient(minecraft -> (Integer) setting("bracket_length"));
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

                        // HOLD: open while the key is held, closed when it's let go.
                        command(context, "panel activation hold");
                        hoverSlot(context, HOTBAR_0);
                        context.waitTick();
                        context.getInput().holdKey(AdjustKeys.ADJUST);
                        context.waitTick();
                        if (!panelOpen(context))
                        {
                                throw new AssertionError("HOLD: holding the Adjust key should open the panel");
                        }
                        context.getInput().releaseKey(AdjustKeys.ADJUST);
                        context.waitTick();
                        if (panelOpen(context))
                        {
                                throw new AssertionError("HOLD: letting go of the Adjust key should close the panel");
                        }
                        command(context, "panel activation toggle");

                        // Two groups with different costs: each shows its own on its first row.
                        hoverSlot(context, HOTBAR_0 + 1);
                        context.waitTick();
                        context.getInput().pressKey(AdjustKeys.ADJUST);
                        context.waitTick();
                        context.takeScreenshot("adjustability_5b_group_costs");
                        context.getInput().pressKey(AdjustKeys.ADJUST);
                        context.waitTick();
                        context.getInput().pressKey(options -> options.keyInventory);
                        context.waitForScreen(null);

                        noneAndServerActivation(context, world);
                        switchOffSingle(context, world);
                        cycleKey(context, world);
                        groupCosts(world, List.of());
                        creativeInventory(context, world);
                        individualToggles(context, world);
                }
        }

        /** Both None modes use real clicks and server updates; compatible members stay independently active. */
        private static void individualToggles(ClientGameTestContext context, TestSingleplayerContext world)
        {
                command(context, "panel reset");
                context.runOnClient(minecraft ->
                {
                        if (!setting("mark_style").equals("adjustability:toggle")
                                || !setting("show_item_icon").equals(true)
                                || !setting("group_style").toString().equals("HEADER")
                                || !setting("exclusive_none").equals(false))
                        {
                                throw new AssertionError("Reset must use toggle marks, icon, headers and inline off");
                        }
                });
                serverSetting(world, "toggle.allow_none", true);
                serverSetting(world, "toggle.cooldown_ticks", 0);
                world.getServer().runOnServer(server ->
                {
                        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.SURVIVAL);
                        player.getInventory().setItem(0, pickaxe(player));
                });
                world.getConnection().waitForClientboundPackets();
                context.getInput().pressKey(options -> options.keyInventory);
                context.waitForScreen(InventoryScreen.class);
                hoverSlot(context, HOTBAR_0);
                context.waitTick();
                context.getInput().pressKey(AdjustKeys.ADJUST);
                context.waitTick();
                assertNoneRow(context, false);
                cleanScreen(context);
                context.takeScreenshot("individual_1_defaults");
                context.runOnClient(minecraft -> PlayerConfig.tutorial(TutorialStep.SWITCH));
                clickRow(context, world, Enchantments.FORTUNE);
                assertMining(world, false, false);
                context.runOnClient(minecraft ->
                {
                        if (PlayerConfig.tutorial() != TutorialStep.DONE)
                        {
                                throw new AssertionError("An individual off switch must complete the tutorial");
                        }
                });
                cleanScreen(context);
                context.takeScreenshot("individual_2_both_off");
                clickRow(context, world, Enchantments.SILK_TOUCH);
                assertMining(world, false, true);
                clickRow(context, world, Enchantments.FORTUNE);
                assertMining(world, true, false);

                command(context, "panel exclusive_none true");
                assertNoneRow(context, true);
                clickRow(context, world, Enchantments.FORTUNE);
                assertMining(world, true, false);
                int[] none = context.computeOnClient(minecraft -> AdjustClient
                        .panel((AbstractContainerScreen<?>) minecraft.gui.screen()).noneRowCentre(minecraft.level
                                .registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                                .getOrThrow(Enchantments.FORTUNE)).orElseThrow());
                moveTo(context, none[0], none[1]);
                context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                world.getConnection().waitForServerboundPackets();
                world.getConnection().waitForClientboundPackets();
                context.waitTicks(2);
                assertMining(world, false, false);
                cleanScreen(context);
                context.takeScreenshot("individual_3_exclusive_none");

                command(context, "panel exclusive_none false");
                assertNoneRow(context, false);
                clickRow(context, world, Enchantments.FORTUNE);
                serverSetting(world, "toggle.allow_none", false);
                context.waitTicks(2);
                clickRow(context, world, Enchantments.FORTUNE);
                assertMining(world, true, false);
                assertNoneRow(context, false);
                context.getInput().pressKey(options -> options.keyInventory);
                context.waitForScreen(null);
                serverSetting(world, "toggle.allow_none", true);
        }

        private static void assertNoneRow(ClientGameTestContext context, boolean expected)
        {
                boolean present = context.computeOnClient(minecraft -> AdjustClient
                        .panel((AbstractContainerScreen<?>) minecraft.gui.screen()).noneRowCentre(minecraft.level
                                .registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                                .getOrThrow(Enchantments.FORTUNE)).isPresent());
                if (present != expected)
                {
                        throw new AssertionError("None row present: " + present + ", expected " + expected);
                }
        }

        private static void clickRow(ClientGameTestContext context, TestSingleplayerContext world,
                ResourceKey<Enchantment> enchantment)
        {
                int[] row = context.computeOnClient(minecraft -> AdjustClient
                        .panel((AbstractContainerScreen<?>) minecraft.gui.screen()).rowCentre(minecraft.level
                                .registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment))
                        .orElseThrow());
                moveTo(context, row[0], row[1]);
                context.waitTick();
                context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                world.getConnection().waitForServerboundPackets();
                world.getConnection().waitForClientboundPackets();
                context.waitTicks(2);
        }

        private static void assertMining(TestSingleplayerContext world, boolean fortune, boolean silkTouch)
        {
                world.getServer().runOnServer(server ->
                {
                        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                        ItemEnchantments active = Adjuster.activeOf(player.getInventory().getItem(0));
                        if ((active.getLevel(enchantment(player, Enchantments.FORTUNE)) > 0) != fortune
                                || (active.getLevel(enchantment(player, Enchantments.SILK_TOUCH)) > 0) != silkTouch)
                        {
                                throw new AssertionError("Expected Fortune=" + fortune + ", Silk Touch=" + silkTouch
                                        + ", got " + active);
                        }
                });
        }

        private static void hint(ClientGameTestContext context)
        {
                context.runOnClient(minecraft ->
                {
                        ItemStack stack = minecraft.player.getInventory().getItem(0).copy();
                        stack.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("After enchantments"))));
                        List<Component> lines = stack.getTooltipLines(Item.TooltipContext.of(minecraft.level),
                                minecraft.player, TooltipFlag.NORMAL);
                        String key = AdjustKeys.ADJUST.getTranslatedKeyMessage().getString();
                        Component hint = lines.stream().filter(line -> line.getString()
                                .equals(key + ": switch enchantments")).findFirst().orElseThrow(
                                        () -> new AssertionError("No switch enchantments hint: " + lines));
                        int index = lines.indexOf(hint);
                        if (index < 1 || !lines.get(index + 1).getString().equals("After enchantments"))
                        {
                                throw new AssertionError("Hint should follow enchantments, before lore: " + lines);
                        }
                        hint.visit((style, text) ->
                        {
                                ChatFormatting color = text.equals(key) ? ChatFormatting.LIGHT_PURPLE
                                        : ChatFormatting.GRAY;
                                if (style.isItalic() || style.getColor() == null
                                        || !style.getColor().equals(TextColor.fromLegacyFormat(color)))
                                {
                                        throw new AssertionError("Hint style: " + text + " " + style);
                                }
                                return Optional.empty();
                        }, Style.EMPTY);
                });
        }

        /**
         * In creative the inventory is a menu only the client has (the server keeps the player's inventory menu), so
         * a click on the panel must name the item's slot in the server's menu: switching from the hotbar works there.
         */
        private static void creativeInventory(ClientGameTestContext context, TestSingleplayerContext world)
        {
                world.getServer().runOnServer(server ->
                {
                        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.CREATIVE);
                        player.getInventory().setItem(0, pickaxe(player));
                        player.getInventory().setItem(9, pickaxe(player));
                });
                world.getConnection().waitForClientboundPackets();
                // The Cycle key's switches started the 1 second cooldown.
                context.waitTicks(25);
                context.getInput().pressKey(options -> options.keyInventory);
                context.waitForScreen(CreativeModeInventoryScreen.class);
                int hotbar = context.computeOnClient(minecraft ->
                {
                        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) minecraft.gui.screen();
                        ItemStack pickaxe = minecraft.player.getInventory().getItem(0);
                        return screen.getMenu().slots.stream().filter(slot -> slot.getItem() == pickaxe).findFirst()
                                .orElseThrow(() -> new AssertionError("The creative screen doesn't show the hotbar"))
                                .index;
                });
                hoverSlot(context, hotbar);
                context.waitTick();
                context.getInput().pressKey(AdjustKeys.ADJUST);
                context.waitTick();
                int[] row = context.computeOnClient(minecraft ->
                {
                        AdjustPanel panel = AdjustClient.panel((AbstractContainerScreen<?>) minecraft.gui.screen());
                        if (panel == null)
                        {
                                throw new AssertionError("Creative: Left Alt over a hotbar item should open the panel");
                        }
                        Holder<Enchantment> silkTouch = minecraft.level.registryAccess()
                                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
                        return panel.rowCentre(silkTouch).orElseThrow(
                                () -> new AssertionError("Creative: no Silk Touch row in the panel"));
                });
                moveTo(context, row[0], row[1]);
                context.waitTick();
                context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                world.getConnection().waitForServerboundPackets();
                world.getConnection().waitForClientboundPackets();
                context.waitTicks(2);
                assertActive(world, Enchantments.SILK_TOUCH, "creative: after clicking Silk Touch in the panel");
                context.getInput().pressKey(AdjustKeys.ADJUST);
                world.getConnection().waitForClientboundPackets();
                context.waitTick();

                // The Survival Inventory tab swaps in wrappers of the inventory menu's slots.
                int inventorySlot = context.computeOnClient(minecraft ->
                {
                        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) minecraft.gui.screen();
                        CreativeModeTab inventoryTab = CreativeModeTabs.allTabs().stream()
                                .filter(tab -> tab.getType() == CreativeModeTab.Type.INVENTORY).findFirst().orElseThrow();
                        try
                        {
                                Method selectTab = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab",
                                        CreativeModeTab.class);
                                selectTab.setAccessible(true);
                                selectTab.invoke(screen, inventoryTab);
                        }
                        catch (ReflectiveOperationException exception)
                        {
                                throw new AssertionError(exception);
                        }
                        // The pickaxe in main inventory slot 9. The wrappers' own index is 0, so by position.
                        ItemStack pickaxe = minecraft.player.getInventory().getItem(9);
                        List<Slot> slots = screen.getMenu().slots;
                        for (int i = 0; i < slots.size(); i++)
                        {
                                if (!pickaxe.isEmpty() && slots.get(i).getItem() == pickaxe)
                                {
                                        return i;
                                }
                        }
                        throw new AssertionError("The inventory tab doesn't show the pickaxe");
                });
                hoverSlot(context, inventorySlot);
                context.waitTick();
                context.getInput().pressKey(AdjustKeys.ADJUST);
                context.waitTick();
                context.takeScreenshot("adjustability_6_creative_inventory_tab");
                context.runOnClient(minecraft ->
                {
                        if (AdjustClient.panel((AbstractContainerScreen<?>) minecraft.gui.screen()) == null)
                        {
                                throw new AssertionError("Creative: Left Alt over an item on the Survival Inventory tab"
                                        + " should open the panel");
                        }
                });
                context.getInput().pressKey(AdjustKeys.ADJUST);
                context.getInput().pressKey(options -> options.keyInventory);
                context.waitForScreen(null);
                world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
                        .setGameMode(GameType.SURVIVAL));
        }

        /**
         * With {@code allow_none}: the panel's None row leaves the mining group with nothing active. And the server's
         * {@code panel_activation = HOLD} wins over the player's TOGGLE: a press (down and up) doesn't leave it open.
         */
        private static void noneAndServerActivation(ClientGameTestContext context, TestSingleplayerContext world)
        {
                command(context, "panel exclusive_none true");
                serverSetting(world, "toggle.allow_none", true);
                world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().getInventory()
                        .setItem(0, pickaxe(server.getPlayerList().getPlayers().getFirst())));
                world.getConnection().waitForClientboundPackets();
                context.waitTicks(25);
                context.getInput().pressKey(options -> options.keyInventory);
                context.waitForScreen(InventoryScreen.class);
                hoverSlot(context, HOTBAR_0);
                context.waitTick();
                context.getInput().pressKey(AdjustKeys.ADJUST);
                context.waitTick();
                context.takeScreenshot("adjustability_7_none_row");
                int[] none = context.computeOnClient(minecraft ->
                {
                        AdjustPanel panel = AdjustClient.panel((AbstractContainerScreen<?>) minecraft.gui.screen());
                        Holder<Enchantment> fortune = minecraft.level.registryAccess()
                                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
                        return panel.noneRowCentre(fortune).orElseThrow(() -> new AssertionError("No None row"));
                });
                moveTo(context, none[0], none[1]);
                context.waitTick();
                context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                world.getConnection().waitForServerboundPackets();
                world.getConnection().waitForClientboundPackets();
                context.waitTicks(2);
                world.getServer().runOnServer(server ->
                {
                        ItemEnchantments active = Adjuster.activeOf(server.getPlayerList().getPlayers().getFirst()
                                .getInventory().getItem(0));
                        if (active.getLevel(enchantment(server.getPlayerList().getPlayers().getFirst(),
                                Enchantments.FORTUNE)) > 0)
                        {
                                throw new AssertionError("None: Fortune should be dormant, active: " + active);
                        }
                });
                context.getInput().pressKey(AdjustKeys.ADJUST);
                context.waitTick();

                serverSetting(world, "toggle.panel_activation", ServerPanelActivation.HOLD);
                world.getConnection().waitForClientboundPackets();
                context.getInput().pressKey(AdjustKeys.ADJUST);
                context.waitTick();
                if (panelOpen(context))
                {
                        throw new AssertionError("Server HOLD: a press and release should leave the panel closed");
                }
                serverSetting(world, "toggle.panel_activation", ServerPanelActivation.PLAYER);
                serverSetting(world, "toggle.allow_none", false);
                command(context, "panel exclusive_none false");
                context.getInput().pressKey(options -> options.keyInventory);
                context.waitForScreen(null);
        }

        /**
         * The section of single enchantments: a click on Efficiency's row switches it off on the server; a click on
         * the section's heading folds it and another unfolds it.
         */
        private static void switchOffSingle(ClientGameTestContext context, TestSingleplayerContext world)
        {
                world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().getInventory()
                        .setItem(0, pickaxe(server.getPlayerList().getPlayers().getFirst())));
                world.getConnection().waitForClientboundPackets();
                context.waitTicks(25);
                context.getInput().pressKey(options -> options.keyInventory);
                context.waitForScreen(InventoryScreen.class);
                hoverSlot(context, HOTBAR_0);
                context.waitTick();
                context.getInput().pressKey(AdjustKeys.ADJUST);
                context.waitTick();
                cleanScreen(context);
                context.takeScreenshot("adjustability_8_singles");

                int[] row = context.computeOnClient(minecraft ->
                {
                        AdjustPanel panel = AdjustClient.panel((AbstractContainerScreen<?>) minecraft.gui.screen());
                        Holder<Enchantment> efficiency = minecraft.level.registryAccess()
                                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY);
                        return panel.rowCentre(efficiency).orElseThrow(() -> new AssertionError("No Efficiency row"));
                });
                moveTo(context, row[0], row[1]);
                context.waitTick();
                context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                world.getConnection().waitForServerboundPackets();
                world.getConnection().waitForClientboundPackets();
                context.waitTicks(2);
                cleanScreen(context);
                context.takeScreenshot("adjustability_8b_switched_off");
                world.getServer().runOnServer(server ->
                {
                        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                        ItemStack pickaxe = player.getInventory().getItem(0);
                        Holder<Enchantment> efficiency = enchantment(player, Enchantments.EFFICIENCY);
                        if (Adjuster.activeOf(pickaxe).getLevel(efficiency) > 0
                                || !DormantEnchantments.off(pickaxe, player.level().registryAccess())
                                        .contains(efficiency))
                        {
                                throw new AssertionError("Efficiency should be switched off: "
                                        + Adjuster.activeOf(pickaxe));
                        }
                });

                for (boolean folded : new boolean[]{true, false})
                {
                        int[] heading = context.computeOnClient(minecraft -> AdjustClient
                                .panel((AbstractContainerScreen<?>) minecraft.gui.screen()).singlesHeadingCentre()
                                .orElseThrow(() -> new AssertionError("No heading for the single enchantments")));
                        moveTo(context, heading[0], heading[1]);
                        context.waitTick();
                        context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
                        context.waitTicks(2);
                        if (folded)
                        {
                                context.takeScreenshot("adjustability_8c_folded");
                        }
                        boolean rowShown = context.computeOnClient(minecraft -> AdjustClient
                                .panel((AbstractContainerScreen<?>) minecraft.gui.screen())
                                .rowCentre(minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                                        .getOrThrow(Enchantments.EFFICIENCY)).isPresent());
                        if (rowShown == folded)
                        {
                                throw new AssertionError(folded ? "Folded: no Efficiency row expected"
                                        : "Unfolded: the Efficiency row should be back");
                        }
                }
                context.getInput().pressKey(AdjustKeys.ADJUST);
                context.waitTick();
                context.getInput().pressKey(options -> options.keyInventory);
                context.waitForScreen(null);
        }

        @SuppressWarnings("unchecked")
        private static <T> void serverSetting(TestSingleplayerContext world, String path, T value)
        {
                world.getServer().runOnServer(server -> ((ConfigValue<T>) CreatorConfig.SPEC.getValues().get(path))
                        .set(value));
        }

        /**
         * Sets {@code [toggle] group_costs} on the running server through the config spec (the integrated server and
         * the client share it), as an admin editing the file would.
         */
        @SuppressWarnings("unchecked")
        private static void groupCosts(TestSingleplayerContext world, List<String> entries)
        {
                world.getServer().runOnServer(server ->
                {
                        ConfigValue<List<? extends String>> value =
                                (ConfigValue<List<? extends String>>) CreatorConfig.SPEC
                                .getValues().get("toggle.group_costs");
                        value.set(entries);
                });
        }

        /**
         * The Cycle key on boots with two conflict groups: Cycle steps the first (Depth Strider / Frost Walker),
         * Sneak + Cycle picks the second (the Protections) and the next Cycle steps that one.
         */
        private static void cycleKey(ClientGameTestContext context, TestSingleplayerContext world)
        {
                context.runOnClient(minecraft ->
                {
                        AdjustKeys.CYCLE.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_V));
                        KeyMapping.resetMapping();
                });
                world.getServer().runOnServer(server ->
                {
                        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                        player.getInventory().setSelectedSlot(0);
                        player.getInventory().setItem(0, boots(player));
                });
                world.getConnection().waitForClientboundPackets();
                // The switch above started the 1 second cooldown.
                context.waitTicks(25);

                context.getInput().pressKey(AdjustKeys.CYCLE);
                world.getConnection().waitForServerboundPackets();
                assertActive(world, Enchantments.FROST_WALKER, "first press: the boots group");
                context.waitTicks(25);

                context.getInput().holdShift();
                context.waitTick();
                context.getInput().pressKey(AdjustKeys.CYCLE);
                context.getInput().releaseShift();
                context.waitTick();
                context.takeScreenshot("adjustability_6_cycle_group");
                context.getInput().pressKey(AdjustKeys.CYCLE);
                world.getConnection().waitForServerboundPackets();
                assertActive(world, Enchantments.FIRE_PROTECTION, "after Sneak + Cycle: the Protections");

                context.runOnClient(minecraft ->
                {
                        AdjustKeys.CYCLE.setKey(InputConstants.UNKNOWN);
                        KeyMapping.resetMapping();
                });
        }

        private static void assertActive(TestSingleplayerContext world, ResourceKey<Enchantment> key, String when)
        {
                world.getServer().runOnServer(server ->
                {
                        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                        ItemEnchantments active = Adjuster.activeOf(player.getInventory().getItem(0));
                        if (active.getLevel(enchantment(player, key)) == 0)
                        {
                                throw new AssertionError(when + ": " + key.identifier() + " should be active, active: "
                                        + active);
                        }
                });
        }

        private static ItemStack boots(ServerPlayer player)
        {
                ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
                ItemEnchantments.Mutable active = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                active.set(enchantment(player, Enchantments.PROTECTION), 4);
                active.set(enchantment(player, Enchantments.DEPTH_STRIDER), 3);
                boots.set(DataComponents.ENCHANTMENTS, active.toImmutable());
                ItemEnchantments.Mutable dormant = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                dormant.set(enchantment(player, Enchantments.FIRE_PROTECTION), 4);
                dormant.set(enchantment(player, Enchantments.FROST_WALKER), 2);
                DormantEnchantments.set(boots, dormant.toImmutable(), player.level().registryAccess());
                return boots;
        }

        private static boolean panelOpen(ClientGameTestContext context)
        {
                return context.computeOnClient(
                        minecraft -> AdjustClient.panel((AbstractContainerScreen<?>) minecraft.gui.screen()) != null);
        }

        private static Object setting(String name)
        {
                return PlayerConfig.PANEL_SETTINGS.stream().filter(value -> PlayerConfig.name(value).equals(name))
                        .findFirst().map(PlayerConfig::current).orElseThrow();
        }

        /** No chat lines or toasts (command feedback, advancements) in the screenshots the docs use. */
        private static void cleanScreen(ClientGameTestContext context)
        {
                context.runOnClient(minecraft ->
                {
                        minecraft.gui.hud.getChat().clearMessages(false);
                        minecraft.gui.toastManager().clear();
                });
                context.waitTick();
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
                DormantEnchantments.set(pickaxe, dormant.toImmutable(), player.level().registryAccess());
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
