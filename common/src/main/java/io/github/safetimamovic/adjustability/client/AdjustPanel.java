package io.github.safetimamovic.adjustability.client;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelStyle;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ToggleConfig;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.Toggler;
import io.github.safetimamovic.adjustability.core.TooltipLines;
import io.github.safetimamovic.adjustability.graph.ConflictGraph;
import io.github.safetimamovic.adjustability.network.ToggleEnchantmentPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The Adjust panel: a tooltip-like menu pinned next to an item in a container screen, one row per enchantment in each
 * group of conflicting enchantments. Clicking a dormant row asks the server to make it active. It is drawn by the
 * container screen itself (see {@code AbstractContainerScreenMixin}), so the container stays open and nothing about the
 * menu changes on the client. Rows are rebuilt from the slot's item every frame, so the panel shows what the server
 * says, never a switch it refused.
 *
 * <p>The look is Adjustability's own: a dark panel with a magenta gradient border and bracket decorations at the
 * corners. Every color, the side it opens on, the brackets and the cost line are player settings ({@code [panel]} in
 * {@code adjustability-client.toml}, or {@code /adjustability panel}), read every frame.
 */
public final class AdjustPanel
{
        private static final int PADDING = 5;
        private static final int ROW_HEIGHT = 11;
        private static final int GROUP_GAP = 5;
        private static final int GAP_FROM_SLOT = 6;

        private static final int BRACKET_OFFSET = 3;
        private static final int ACTIVE_TEXT = 0xFFFFFFFF;
        private static final int DORMANT_TEXT = 0xFFAAAAAA;
        private static final int FOOTER_TEXT = 0xFF808080;

        private final AbstractContainerScreen<?> screen;
        private final int containerId;
        private final int slot;
        /** Top-right corner of the slot, in screen coordinates. */
        private final int slotRight;
        private final int slotTop;

        private int x;
        private int y;
        private int width;
        private int height;
        private final List<Row> rows = new ArrayList<>();

        AdjustPanel(AbstractContainerScreen<?> screen, int containerId, int slot, int slotRight, int slotTop)
        {
                this.screen = screen;
                this.containerId = containerId;
                this.slot = slot;
                this.slotRight = slotRight;
                this.slotTop = slotTop;
        }

        public AbstractContainerScreen<?> screen()
        {
                return this.screen;
        }

        int slot()
        {
                return this.slot;
        }

        /**
         * Rebuilds the rows from the slot's item and places the panel next to the slot, inside the screen.
         *
         * @return false if there is nothing left to adjust (the item moved, or lost its conflicts), and the panel should
         *         close
         */
        boolean refresh(Font font)
        {
                PanelStyle style = AdjustabilityConfig.player().panel();
                ItemStack stack = this.item();
                Minecraft minecraft = Minecraft.getInstance();
                this.rows.clear();
                if (stack.isEmpty() || minecraft.level == null)
                {
                        return false;
                }
                ItemEnchantments active = Adjuster.activeOf(stack);
                ItemEnchantments.Mutable all = new ItemEnchantments.Mutable(active);
                Adjuster.dormantOf(stack, minecraft.level.registryAccess()).entrySet()
                        .forEach(entry -> all.set(entry.getKey(), entry.getIntValue()));
                ItemEnchantments levels = all.toImmutable();
                ConflictGraph<Holder<Enchantment>> graph = Adjuster.graph(levels);
                // What the server will treat as active: the stored split, repaired if it conflicts (see Toggler).
                ItemEnchantments shownActive = Adjuster.split(levels, active.keySet()).active();
                List<Set<Holder<Enchantment>>> groups = graph.components();
                if (groups.isEmpty())
                {
                        return false;
                }

                int rowY = font.lineHeight + PADDING + 3;
                int textWidth = font.width(stack.getHoverName());
                for (int group = 0; group < groups.size(); group++)
                {
                        if (group > 0)
                        {
                                rowY += GROUP_GAP;
                        }
                        for (Holder<Enchantment> enchantment : graph.vertices())
                        {
                                if (groups.get(group).contains(enchantment))
                                {
                                        Component label = TooltipLines.name(enchantment, levels.getLevel(enchantment));
                                        this.rows.add(new Row(enchantment, label, shownActive.getLevel(enchantment) > 0,
                                                rowY, group));
                                        textWidth = Math.max(textWidth, font.width(label) + 10);
                                        rowY += ROW_HEIGHT;
                                }
                        }
                }
                if (style.showCost())
                {
                        textWidth = Math.max(textWidth, font.width(footer()));
                        rowY += 3 + font.lineHeight;
                }
                this.width = textWidth + 2 * PADDING;
                this.height = rowY + PADDING;

                // AUTO: right of the slot if it fits, else left of it. Never past the screen's edges.
                int right = this.slotRight + GAP_FROM_SLOT;
                int left = this.slotRight - 16 - GAP_FROM_SLOT - this.width;
                this.x = switch (style.side())
                {
                        case RIGHT -> right;
                        case LEFT -> left;
                        case AUTO -> right + this.width > this.screen.width - BRACKET_OFFSET ? left : right;
                };
                this.x = Math.min(this.x, this.screen.width - this.width - BRACKET_OFFSET);
                this.x = Math.max(BRACKET_OFFSET, this.x);
                this.y = Math.max(BRACKET_OFFSET,
                        Math.min(this.slotTop - PADDING, this.screen.height - this.height - BRACKET_OFFSET));
                return true;
        }

        void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY)
        {
                PanelStyle style = AdjustabilityConfig.player().panel();
                graphics.nextStratum();
                int right = this.x + this.width;
                int bottom = this.y + this.height;
                graphics.fill(this.x, this.y, right, bottom, style.background());
                // Inner border, fading from the top color to the bottom one, like a tooltip's.
                graphics.fill(this.x + 1, this.y + 1, right - 1, this.y + 2, style.borderTop());
                graphics.fill(this.x + 1, bottom - 2, right - 1, bottom - 1, style.borderBottom());
                graphics.fillGradient(this.x + 1, this.y + 2, this.x + 2, bottom - 2, style.borderTop(),
                        style.borderBottom());
                graphics.fillGradient(right - 2, this.y + 2, right - 1, bottom - 2, style.borderTop(),
                        style.borderBottom());
                if (style.showBrackets())
                {
                        this.brackets(graphics, right, bottom, style);
                }

                ItemStack stack = this.item();
                graphics.text(font, stack.getHoverName(), this.x + PADDING, this.y + PADDING, ACTIVE_TEXT, true);
                int lastGroup = -1;
                for (Row row : this.rows)
                {
                        int top = this.y + row.y();
                        if (row.group() != lastGroup && lastGroup >= 0)
                        {
                                graphics.fill(this.x + PADDING, top - GROUP_GAP / 2 - 1, right - PADDING,
                                        top - GROUP_GAP / 2, separator(style.borderTop()));
                        }
                        lastGroup = row.group();
                        boolean hovered = !row.active() && this.rowAt(mouseX, mouseY).orElse(null) == row;
                        if (hovered)
                        {
                                graphics.fill(this.x + 2, top - 1, right - 2, top + ROW_HEIGHT - 1, style.highlight());
                        }
                        graphics.text(font, row.active() ? "●" : "○", this.x + PADDING, top + 1,
                                row.active() ? style.activeMark() : DORMANT_TEXT, false);
                        graphics.text(font, row.label(), this.x + PADDING + 10, top + 1,
                                row.active() || hovered ? ACTIVE_TEXT : DORMANT_TEXT, true);
                }
                if (style.showCost())
                {
                        graphics.text(font, footer(), this.x + PADDING, bottom - PADDING - font.lineHeight + 1,
                                FOOTER_TEXT, false);
                }
        }

        /** The line between conflict groups: the top border's color, a quarter as opaque. */
        private static int separator(int borderTop)
        {
                return (borderTop >>> 26 << 24) | (borderTop & 0xFFFFFF);
        }

        /** Corner brackets just outside the panel, the 2024 design's decoration. */
        private void brackets(GuiGraphicsExtractor graphics, int right, int bottom, PanelStyle style)
        {
                int color = style.bracket();
                int left = this.x - BRACKET_OFFSET;
                int top = this.y - BRACKET_OFFSET;
                int outerRight = right + BRACKET_OFFSET;
                int outerBottom = bottom + BRACKET_OFFSET;
                int l = style.bracketLength();
                graphics.fill(left, top, left + l, top + 2, color);
                graphics.fill(left, top, left + 2, top + l, color);
                graphics.fill(outerRight - l, top, outerRight, top + 2, color);
                graphics.fill(outerRight - 2, top, outerRight, top + l, color);
                graphics.fill(left, outerBottom - 2, left + l, outerBottom, color);
                graphics.fill(left, outerBottom - l, left + 2, outerBottom, color);
                graphics.fill(outerRight - l, outerBottom - 2, outerRight, outerBottom, color);
                graphics.fill(outerRight - 2, outerBottom - l, outerRight, outerBottom, color);
        }

        boolean contains(double mouseX, double mouseY)
        {
                return mouseX >= this.x - BRACKET_OFFSET && mouseX < this.x + this.width + BRACKET_OFFSET
                        && mouseY >= this.y - BRACKET_OFFSET && mouseY < this.y + this.height + BRACKET_OFFSET;
        }

        /** A left click inside the panel: on a dormant row, asks the server to make it active. */
        void click(double mouseX, double mouseY)
        {
                this.rowAt(mouseX, mouseY).filter(row -> !row.active()).ifPresent(row -> row.enchantment().unwrapKey()
                        .ifPresent(key -> AdjustClient.send(
                                new ToggleEnchantmentPayload(this.containerId, this.slot, key))));
        }

        /** Centre of an enchantment's row, in screen coordinates; for the client GameTest. */
        public Optional<int[]> rowCentre(Holder<Enchantment> enchantment)
        {
                return this.rows.stream().filter(row -> row.enchantment().equals(enchantment)).findFirst()
                        .map(row -> new int[]{this.x + this.width / 2, this.y + row.y() + ROW_HEIGHT / 2});
        }

        private Optional<Row> rowAt(double mouseX, double mouseY)
        {
                if (mouseX < this.x || mouseX >= this.x + this.width)
                {
                        return Optional.empty();
                }
                return this.rows.stream().filter(row -> mouseY >= this.y + row.y() - 1
                        && mouseY < this.y + row.y() + ROW_HEIGHT - 1).findFirst();
        }

        private ItemStack item()
        {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player == null)
                {
                        return ItemStack.EMPTY;
                }
                AbstractContainerMenu menu = minecraft.player.containerMenu;
                if (menu.containerId != this.containerId || this.slot < 0 || this.slot >= menu.slots.size())
                {
                        return ItemStack.EMPTY;
                }
                return menu.getSlot(this.slot).getItem();
        }

        private static Component footer()
        {
                ToggleConfig config = AdjustabilityConfig.toggle();
                return Component.translatable("adjustability.panel.cost", Toggler.costText(config),
                        String.format(Locale.ROOT, "%.1f", config.cooldownTicks() / 20.0));
        }

        private record Row(Holder<Enchantment> enchantment, Component label, boolean active, int y, int group)
        {
        }
}
