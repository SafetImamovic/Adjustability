package io.github.safetimamovic.adjustability.client;

import dev.isxander.yacl3.gui.image.ImageRenderer;
import io.github.safetimamovic.adjustability.client.AdjustPanel.Row;
import io.github.safetimamovic.adjustability.client.AdjustPanel.Size;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelSide;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelStyle;
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
 * The Adjust panel in YACL's description pane, live: a pickaxe with Fortune III and a dormant Silk Touch in a mock
 * slot, and the panel next to it with the values being edited, not yet saved, drawn by the panel's own code. Silk Touch
 * is shown hovered, for the highlight color; the cost line and a None row follow the server's settings (the defaults
 * outside a world). Scaled down if the pane is narrower than the panel.
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
        private final ItemStack item = new ItemStack(Items.DIAMOND_PICKAXE);

        PanelPreview(Function<ConfigValue<?>, @Nullable Object> pending)
        {
                this.pending = pending;
                this.item.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        @Override
        public int render(GuiGraphicsExtractor graphics, int x, int y, int renderWidth, float tickDelta)
        {
                Font font = Minecraft.getInstance().font;
                PanelStyle style = PlayerConfig.panel(this.pending);
                ToggleConfig toggle = AdjustabilityConfig.toggle();
                Component title = this.item.getHoverName();
                List<Row> rows = rows(font, toggle.allowNone());
                Component footer = AdjustPanel.footer(toggle.defaultCost(), toggle.cooldownTicks());
                Size size = AdjustPanel.size(font, style, title, rows, List.of(), footer);

                // Just room for the slot and the panel, the slot on the side the panel doesn't open to.
                int sceneWidth = 2 * MARGIN + 16 + AdjustPanel.GAP_FROM_SLOT + size.width()
                        + AdjustPanel.BRACKET_OFFSET;
                int sceneHeight = size.height() + 2 * (MARGIN + AdjustPanel.BRACKET_OFFSET);
                int slotRight = style.side() == PanelSide.LEFT ? sceneWidth - MARGIN : MARGIN + 16;
                int slotTop = MARGIN + AdjustPanel.BRACKET_OFFSET + AdjustPanel.PADDING;
                int panelX = AdjustPanel.placeX(style, slotRight, size.width(), sceneWidth);
                int panelY = AdjustPanel.placeY(slotTop, size.height(), sceneHeight);

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
                graphics.fakeItem(this.item, slotLeft, slotTop);
                //?} else
                /*graphics.renderFakeItem(this.item, slotLeft, slotTop);*/
                AdjustPanel.draw(graphics, font, style, panelX, panelY, size.width(), size.height(), title, rows,
                        List.of(), footer, rows.get(1));
                //? if >=26.1 {
                graphics.pose().popMatrix();
                //?} else
                /*graphics.pose().popPose();*/
                return height;
        }

        /** Fortune III active, Silk Touch dormant, and None if the server allows it. */
        private static List<Row> rows(Font font, boolean allowNone)
        {
                List<Row> rows = new ArrayList<>();
                int top = AdjustPanel.rowsTop(font);
                rows.add(new Row(null, Component.translatable("enchantment.minecraft.fortune")
                        .append(CommonComponents.SPACE).append(Component.translatable("enchantment.level.3")), true,
                        top, 0, false));
                rows.add(new Row(null, Component.translatable("enchantment.minecraft.silk_touch"), false,
                        top + AdjustPanel.ROW_HEIGHT, 0, false));
                if (allowNone)
                {
                        rows.add(new Row(null, Component.translatable("adjustability.panel.none"), false,
                                top + 2 * AdjustPanel.ROW_HEIGHT, 0, true));
                }
                return rows;
        }

        @Override
        public void close()
        {
        }
}
