package io.github.safetimamovic.adjustability.client;

import dev.isxander.yacl3.gui.image.ImageRenderer;
import io.github.safetimamovic.adjustability.client.PanelLayout.Content;
import io.github.safetimamovic.adjustability.client.PanelLayout.Entry;
import io.github.safetimamovic.adjustability.client.PanelLayout.Group;
import io.github.safetimamovic.adjustability.client.PanelLayout.Row;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelSide;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelStyle;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.SinglesSection;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ToggleConfig;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import org.jspecify.annotations.Nullable;

/**
 * The Adjust panel in YACL's description pane, live: boots with Protection IV and Depth Strider III active, Fire
 * Protection IV and Frost Walker II dormant, in a mock slot, and the panel next to it with the values being edited, not
 * yet saved, drawn by the panel's own code. Two groups, so separators and headings show; Fire Protection is hovered,
 * for the highlight color. The cost line and None rows follow the server's settings (the defaults outside a world).
 * Scaled down if the pane is narrower than the panel.
 */
final class PanelPreview implements ImageRenderer
{
        /** A container screen's gray, which the panel is normally seen on. */
        private static final int BACKDROP = 0xFFC6C6C6;
        private static final int SLOT = 0xFF8B8B8B;
        private static final int SLOT_SHADOW = 0xFF373737;
        private static final int SLOT_LIGHT = 0xFFFFFFFF;
        /** Space around the slot and the panel's brackets. */
        private static final int MARGIN = 4;

        /** A setting's value on the screen, or {@code null} for the saved one. */
        private final Function<ConfigValue<?>, @Nullable Object> pending;
        /** Made on first draw that can: see {@link #item()}. */
        private @Nullable ItemStack item;

        PanelPreview(Function<ConfigValue<?>, @Nullable Object> pending)
        {
                this.pending = pending;
        }

        @Override
        public int render(GuiGraphicsExtractor graphics, int x, int y, int renderWidth, float tickDelta)
        {
                Font font = Minecraft.getInstance().font;
                PanelStyle style = PlayerConfig.panel(this.pending);
                ToggleConfig toggle = AdjustabilityConfig.toggle();
                Component footer = AdjustPanel.footer(toggle.defaultCost(), toggle.cooldownTicks());
                ItemStack item = item();
                Component title = item.isEmpty() ? Component.translatable(Items.DIAMOND_BOOTS.getDescriptionId())
                        : item.getHoverName();
                Content content = PanelLayout.layout(font, style, title, item,
                        groups(toggle.allowNone() && style.layout().exclusiveNone(),
                                toggle.allowSwitchOff() ? style.layout().singles()
                                        : SinglesSection.HIDDEN), List.of(), footer);

                // Just room for the slot and the panel, the slot on the side the panel doesn't open to.
                int sceneWidth = 2 * MARGIN + 16 + PanelLayout.GAP_FROM_SLOT + content.width()
                        + PanelLayout.BRACKET_OFFSET;
                int sceneHeight = content.height() + 2 * (MARGIN + PanelLayout.BRACKET_OFFSET);
                int slotRight = style.side() == PanelSide.LEFT ? sceneWidth - MARGIN : MARGIN + 16;
                int slotTop = MARGIN + PanelLayout.BRACKET_OFFSET + style.layout().padding();
                int panelX = PanelLayout.placeX(style, slotRight, content.width(), sceneWidth);
                int panelY = PanelLayout.placeY(style, slotTop, content.height(), sceneHeight);

                float scale = Math.min(1, (float) renderWidth / sceneWidth);
                int height = Math.round(sceneHeight * scale);
                graphics.fill(x, y, x + renderWidth, y + height, BACKDROP);
                //? if >=26.1 {
                graphics.pose().pushMatrix();
                graphics.pose().translate(x + (renderWidth - sceneWidth * scale) / 2, y);
                graphics.pose().scale(scale);
                //?} else {
                /*graphics.pose().pushPose();
                graphics.pose().translate(x + (renderWidth - sceneWidth * scale) / 2, y, 0);
                graphics.pose().scale(scale, scale, 1);
                *///?}
                int slotLeft = slotRight - 16;
                graphics.fill(slotLeft - 1, slotTop - 1, slotLeft + 17, slotTop + 17, SLOT_LIGHT);
                graphics.fill(slotLeft - 1, slotTop - 1, slotLeft + 16, slotTop + 16, SLOT_SHADOW);
                graphics.fill(slotLeft, slotTop, slotLeft + 16, slotTop + 16, SLOT);
                //? if >=26.1 {
                graphics.fakeItem(item, slotLeft, slotTop);
                //?} else
                /*graphics.renderFakeItem(item, slotLeft, slotTop);*/
                Row hovered = content.rows().stream().filter(row -> !row.active() && !row.none()).findFirst()
                        .orElse(null);
                PanelRenderer.draw(graphics, font, style, panelX, panelY, content, hovered, null,
                        row -> row.active() ? 1 : 0);
                //? if >=26.1 {
                graphics.pose().popMatrix();
                //?} else
                /*graphics.pose().popPose();*/
                return height;
        }

        /**
         * The glinting boots, or an empty stack (an empty slot, the title without an icon) while it can't be made:
         * from 26.1 an item's components are only bound once a world's registries load, so not on the title screen
         * before a world has been joined.
         */
        private ItemStack item()
        {
                //? if >=26.1 {
                if (this.item == null && Items.DIAMOND_BOOTS.builtInRegistryHolder().areComponentsBound())
                //?} else
                /*if (this.item == null)*/
                {
                        this.item = new ItemStack(Items.DIAMOND_BOOTS);
                        this.item.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                }
                return this.item == null ? ItemStack.EMPTY : this.item;
        }

        /**
         * The Protections and the boots group, each with a None row if the server allows it, then the single
         * enchantments as {@code singles} says.
         */
        private static List<Group> groups(boolean allowNone, SinglesSection singles)
        {
                List<Group> groups = new ArrayList<>(List.of(
                        group("armor", allowNone, entry("protection", 4, true), entry("fire_protection", 4, false)),
                        group("boots", allowNone, entry("depth_strider", 3, true), entry("frost_walker", 2, false))));
                if (singles != SinglesSection.HIDDEN)
                {
                        boolean collapsed = singles == SinglesSection.COLLAPSED;
                        groups.add(new Group(AdjustPanel.singlesHeading(collapsed, 1), collapsed ? List.of()
                                : List.of(entry("unbreaking", 3, true), entry("mending", 1, false)), null, true));
                }
                return groups;
        }

        private static Group group(String tag, boolean allowNone, Entry... entries)
        {
                Entry none = allowNone
                        ? new Entry(null, Component.translatable("adjustability.panel.none"), false, true) : null;
                return new Group(Component.translatable("adjustability.group.minecraft.exclusive_set." + tag),
                        List.of(entries), none, false);
        }

        private static Entry entry(String enchantment, int level, boolean active)
        {
                return new Entry(null, Component.translatable("enchantment.minecraft." + enchantment)
                        .append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + level)),
                        active, false);
        }

        @Override
        public void close()
        {
        }
}
