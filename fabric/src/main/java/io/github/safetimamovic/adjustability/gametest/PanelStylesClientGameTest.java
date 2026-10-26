package io.github.safetimamovic.adjustability.gametest;

import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.gui.YACLScreen;
import io.github.safetimamovic.adjustability.api.client.AdjustabilityClientApi;
import io.github.safetimamovic.adjustability.client.AdjustClient;
import io.github.safetimamovic.adjustability.client.AdjustKeys;
import io.github.safetimamovic.adjustability.client.AdjustPanel;
import io.github.safetimamovic.adjustability.client.ConfigScreens;
import io.github.safetimamovic.adjustability.config.PanelProfiles;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
import io.github.safetimamovic.adjustability.core.DormantEnchantments;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
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
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/**
 * Pictures of the Adjust panel in every row mark, every built-in profile and a few other settings, for the panel
 * styles page:
 * boots with two groups of conflicting enchantments, the panel pinned, the mouse on a dormant row. Each is cropped to
 * the slot and the panel and saved as {@code panel-*.png} next to the screenshots in
 * {@code fabric/versions/26.3/runs/clientgametest/screenshots}; copy them to {@code docs/public/screenshots}. Also
 * checks every built-in mark draws and leaves the panel open, and registers {@link DiamondMark} through the API, as
 * the docs show, and pictures it too. Then runs the profile commands as a player would (apply, save, reset, load,
 * export, import), and last opens the YACL config screen and checks the panel's sections are groups. Run with {@code ./gradlew :fabric:26.3:runClientGametest}.
 */
public class PanelStylesClientGameTest implements FabricClientGameTest
{
        private static final int HOTBAR_0 = InventoryMenu.USE_ROW_SLOT_START;
        /** Space around the slot and the panel in the cropped pictures, in GUI pixels. */
        private static final int MARGIN = 6;
        private static final String DIAMOND = "examplemod:diamond";

        @Override
        public void runTest(ClientGameTestContext context)
        {
                try (TestSingleplayerContext world = context.worldBuilder().create())
                {
                        world.getConnection().waitForChunksRender();
                        context.runOnClient(minecraft ->
                        {
                                PlayerConfig.resetPanel();
                                if (!AdjustabilityClientApi.rowMarks().contains(DIAMOND))
                                {
                                        AdjustabilityClientApi.registerRowMark(
                                                Identifier.fromNamespaceAndPath("examplemod", "diamond"),
                                                new DiamondMark());
                                }
                        });
                        world.getServer().runOnServer(server ->
                        {
                                ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                                player.setGameMode(GameType.SURVIVAL);
                                player.getInventory().setItem(0, boots(player));
                        });
                        world.getConnection().waitForClientboundPackets();

                        context.getInput().pressKey(options -> options.keyInventory);
                        context.waitForScreen(InventoryScreen.class);
                        int[] slot = slot(context, HOTBAR_0);
                        context.getInput().setCursorPos(slot[0] + 8.0 * scale(context),
                                slot[1] + 8.0 * scale(context));
                        context.waitTick();
                        context.getInput().pressKey(AdjustKeys.ADJUST);
                        context.waitTick();

                        for (String mark : PlayerConfig.BUILT_IN_MARK_STYLES)
                        {
                                set(context, "mark_style", mark);
                                picture(context, "panel-mark-" + mark.substring(mark.indexOf(':') + 1));
                        }
                        set(context, "mark_style", DIAMOND);
                        picture(context, "panel-mark-api-diamond");
                        for (String profile : PanelProfiles.BUILT_IN)
                        {
                                command(context, "panel profile " + profile);
                                picture(context, "panel-profile-" + profile);
                        }
                        Map<String, List<Object>> settings = Map.of(
                                "panel-border-sprite", List.of("border_style", "SPRITE"),
                                "panel-group-header", List.of("group_style", "HEADER"),
                                "panel-item-icon", List.of("show_item_icon", true));
                        for (Map.Entry<String, List<Object>> setting : settings.entrySet())
                        {
                                context.runOnClient(minecraft -> PlayerConfig.resetPanel());
                                set(context, (String) setting.getValue().get(0), setting.getValue().get(1));
                                picture(context, setting.getKey());
                        }
                        context.runOnClient(minecraft -> PlayerConfig.resetPanel());
                        context.getInput().pressKey(options -> options.keyInventory);
                        context.waitForScreen(null);
                        profileCommands(context);
                        yaclScreen(context);
                }
        }

        /**
         * A built-in profile applies; saved as a file it survives a reset and loads again; its code, exported and
         * imported, gives the same look.
         */
        private static void profileCommands(ClientGameTestContext context)
        {
                command(context, "panel profile high_contrast");
                Map<ConfigValue<?>, Object> highContrast = context.computeOnClient(minecraft ->
                        PlayerConfig.currentLook());
                if (!"adjustability:checkbox".equals(setting(highContrast, "mark_style")))
                {
                        throw new AssertionError("high_contrast should use checkboxes: " + highContrast);
                }
                command(context, "panel save adjustability_test");
                command(context, "panel reset");
                assertLook(context, false, highContrast, "after a reset");
                command(context, "panel profile adjustability_test");
                assertLook(context, true, highContrast, "after loading the saved profile");

                // Export puts the code on the clipboard; import without a code reads it from there.
                command(context, "panel export");
                String code = context.computeOnClient(minecraft -> minecraft.keyboardHandler.getClipboard());
                command(context, "panel reset");
                command(context, "panel import");
                assertLook(context, true, highContrast, "after importing its code " + code);

                command(context, "panel reset");
                context.runOnClient(minecraft ->
                {
                        try
                        {
                                Files.delete(PanelProfiles.folder().resolve("adjustability_test.json"));
                        }
                        catch (IOException exception)
                        {
                                throw new UncheckedIOException(exception);
                        }
                });
        }

        private static void assertLook(ClientGameTestContext context, boolean same, Map<ConfigValue<?>, Object> look,
                String when)
        {
                Map<ConfigValue<?>, Object> current = context.computeOnClient(minecraft -> PlayerConfig.currentLook());
                if (current.equals(look) != same)
                {
                        throw new AssertionError((same ? "The look should be " : "The look shouldn't be ") + look
                                + " " + when + ", it's " + current);
                }
        }

        private static Object setting(Map<ConfigValue<?>, Object> look, String name)
        {
                return look.get(PlayerConfig.lookSetting(name).orElseThrow());
        }

        /** Runs an {@code /adjustability} client command as if typed in chat. */
        private static void command(ClientGameTestContext context, String arguments)
        {
                context.runOnClient(minecraft -> minecraft.player.connection.sendCommand("adjustability " + arguments));
                context.waitTick();
        }

        /** The YACL screen builds, with a group for {@code [panel]} and each of its sections. */
        private static void yaclScreen(ClientGameTestContext context)
        {
                context.runOnClient(minecraft -> minecraft.gui.setScreen(
                        ConfigScreens.withYacl(parent -> null).apply(null)));
                context.waitForScreen(YACLScreen.class);
                context.waitTicks(2);
                context.takeScreenshot("yacl_player");
                context.runOnClient(minecraft ->
                {
                        List<String> groups = ((YACLScreen) minecraft.gui.screen()).config.categories().getFirst()
                                .groups().stream().map(OptionGroup::name).map(name -> name.getString()).toList();
                        for (String expected : List.of("Adjust Panel", "Adjust Panel: Marks", "Adjust Panel: Text",
                                "Adjust Panel: Layout"))
                        {
                                if (!groups.contains(expected))
                                {
                                        throw new AssertionError("No YACL group " + expected + " in " + groups);
                                }
                        }
                        minecraft.gui.setScreen(null);
                });
        }

        /**
         * Points at the first dormant row (for the highlight), lets marks finish animating, and saves the slot and
         * panel.
         */
        private static void picture(ClientGameTestContext context, String name)
        {
                context.waitTicks(2);
                int[] bounds = context.computeOnClient(minecraft ->
                {
                        AdjustPanel panel = AdjustClient.panel((AbstractContainerScreen<?>) minecraft.gui.screen());
                        if (panel == null)
                        {
                                throw new AssertionError(name + ": the panel should still be open");
                        }
                        Holder<Enchantment> fireProtection = minecraft.level.registryAccess()
                                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_PROTECTION);
                        int[] row = panel.rowCentre(fireProtection)
                                .orElseThrow(() -> new AssertionError(name + ": no Fire Protection row"));
                        int[] box = panel.bounds();
                        return new int[]{box[0], box[1], box[2], box[3], row[0], row[1]};
                });
                int scale = scale(context);
                context.getInput().setCursorPos(bounds[4] * scale, bounds[5] * scale);
                context.waitTicks(4);
                context.runOnClient(minecraft ->
                {
                        minecraft.gui.hud.getChat().clearMessages(false);
                        minecraft.gui.toastManager().clear();
                });
                context.waitTick();
                Path screenshot = context.takeScreenshot(name + "_full");
                int[] slot = slot(context, HOTBAR_0);
                int left = Math.min(slot[0] / scale, bounds[0]) - MARGIN;
                int top = Math.min(slot[1] / scale, bounds[1]) - MARGIN;
                int right = Math.max(slot[0] / scale + 16, bounds[0] + bounds[2]) + MARGIN;
                int bottom = Math.max(slot[1] / scale + 16, bounds[1] + bounds[3]) + MARGIN;
                crop(screenshot, screenshot.resolveSibling(name + ".png"), left * scale, top * scale,
                        (right - left) * scale, (bottom - top) * scale);
        }

        private static void crop(Path from, Path to, int x, int y, int width, int height)
        {
                try
                {
                        BufferedImage image = ImageIO.read(from.toFile());
                        int left = Math.max(0, x);
                        int top = Math.max(0, y);
                        BufferedImage cropped = image.getSubimage(left, top,
                                Math.min(width, image.getWidth() - left), Math.min(height, image.getHeight() - top));
                        ImageIO.write(cropped, "png", to.toFile());
                }
                catch (IOException exception)
                {
                        throw new UncheckedIOException(exception);
                }
        }

        @SuppressWarnings("unchecked")
        private static void set(ClientGameTestContext context, String name, Object value)
        {
                context.runOnClient(minecraft ->
                {
                        ConfigValue<Object> setting = (ConfigValue<Object>) PlayerConfig.PANEL_SETTINGS.stream()
                                .filter(candidate -> PlayerConfig.name(candidate).equals(name)).findFirst()
                                .orElseThrow();
                        Object typed = setting.getDefault() instanceof Enum<?> constant
                                ? Enum.valueOf(constant.getDeclaringClass(), (String) value) : value;
                        if (!PlayerConfig.change(setting, typed))
                        {
                                throw new AssertionError(name + " should take " + value);
                        }
                });
        }

        /** Top-left corner of a slot of the open screen, in window pixels. */
        private static int[] slot(ClientGameTestContext context, int slotIndex)
        {
                int scale = scale(context);
                return context.computeOnClient(minecraft ->
                {
                        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) minecraft.gui.screen();
                        Slot slot = screen.getMenu().getSlot(slotIndex);
                        return new int[]{(field(screen, "leftPos") + slot.x) * scale,
                                (field(screen, "topPos") + slot.y) * scale};
                });
        }

        private static int scale(ClientGameTestContext context)
        {
                return context.computeOnClient(minecraft -> minecraft.getWindow().getGuiScale());
        }

        private static ItemStack boots(ServerPlayer player)
        {
                ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
                ItemEnchantments.Mutable active = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                active.set(enchantment(player, Enchantments.PROTECTION), 4);
                active.set(enchantment(player, Enchantments.DEPTH_STRIDER), 3);
                active.set(enchantment(player, Enchantments.UNBREAKING), 3);
                boots.set(DataComponents.ENCHANTMENTS, active.toImmutable());
                ItemEnchantments.Mutable dormant = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                dormant.set(enchantment(player, Enchantments.FIRE_PROTECTION), 4);
                dormant.set(enchantment(player, Enchantments.FROST_WALKER), 2);
                DormantEnchantments.set(boots, dormant.toImmutable());
                return boots;
        }

        private static Holder<Enchantment> enchantment(ServerPlayer player, ResourceKey<Enchantment> key)
        {
                return player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
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
