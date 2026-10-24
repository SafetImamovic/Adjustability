package io.github.safetimamovic.adjustability.client;

import io.github.safetimamovic.adjustability.config.AdjustabilityConfig;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.Cost;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelStyle;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.ToggleConfig;
import io.github.safetimamovic.adjustability.core.Adjuster;
import io.github.safetimamovic.adjustability.core.Exclusivity;
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
import org.jspecify.annotations.Nullable;

/**
 * The Adjust panel: a tooltip-like menu pinned next to an item in a container screen, one row per enchantment in each
 * group of conflicting enchantments. Clicking a dormant row asks the server to make it active. It is drawn by the
 * container screen itself (see {@code AbstractContainerScreenMixin}), so the container stays open and nothing about the
 * menu changes on the client. Rows are rebuilt from the slot's item every frame, so the panel shows what the server
 * says, never a switch it refused.
 *
 * <p>The look is Adjustability's own: a dark panel with a magenta gradient border and bracket decorations at the
 * corners. Every color, the side it opens on, the brackets and the cost line are player settings ({@code [panel]} in
 * {@code adjustability-client.toml}, or {@code /adjustability panel}), read every frame. Sizing, placing and drawing
 * are static and take the settings, so the YACL screen's preview ({@link PanelPreview}) draws the same panel.
 */
public final class AdjustPanel
{
        static final int PADDING = 5;
        static final int ROW_HEIGHT = 11;
        private static final int GROUP_GAP = 5;
        static final int GAP_FROM_SLOT = 6;

        static final int BRACKET_OFFSET = 3;
        private static final int ACTIVE_TEXT = 0xFFFFFFFF;
        private static final int DORMANT_TEXT = 0xFFAAAAAA;
        private static final int FOOTER_TEXT = 0xFF808080;

        private final AbstractContainerScreen<?> screen;
        private final int containerId;
        private final int slot;
        /** The same item in the menu the server has open; only differs from the above in the creative inventory. */
        private final int serverContainerId;
        private final int serverSlot;
        /** Top-right corner of the slot, in screen coordinates. */
        private final int slotRight;
        private final int slotTop;

        private int x;
        private int y;
        private int width;
        private int height;
        private final List<Row> rows = new ArrayList<>();
        /** Each group's switching cost when the groups' costs differ; empty when one cost covers all (footer). */
        private final List<Component> groupCosts = new ArrayList<>();
        private Component footer = Component.empty();

        AdjustPanel(AbstractContainerScreen<?> screen, int containerId, int slot, int serverContainerId, int serverSlot,
                int slotRight, int slotTop)
        {
                this.screen = screen;
                this.containerId = containerId;
                this.slot = slot;
                this.serverContainerId = serverContainerId;
                this.serverSlot = serverSlot;
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
                this.groupCosts.clear();
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
                ItemEnchantments shownActive = Adjuster.normalize(stack, minecraft.level.registryAccess()).active();
                boolean allowNone = AdjustabilityConfig.toggle().allowNone();
                List<Set<Holder<Enchantment>>> groups = graph.components();
                if (groups.isEmpty())
                {
                        return false;
                }

                // One cost for every group goes in the footer; otherwise each group shows its own on its first row.
                ToggleConfig toggle = AdjustabilityConfig.toggle();
                List<Cost> costs = groups.stream()
                        .map(group -> Exclusivity.groupCost(group).orElseGet(toggle::defaultCost)).toList();
                boolean oneCost = costs.stream().distinct().count() == 1;
                this.footer = footer(oneCost ? costs.getFirst() : null, toggle.cooldownTicks());
                if (!oneCost && style.showCost())
                {
                        costs.forEach(cost -> this.groupCosts.add(Toggler.costText(cost)));
                }

                int rowY = rowsTop(font);
                for (int group = 0; group < groups.size(); group++)
                {
                        if (group > 0)
                        {
                                rowY += GROUP_GAP;
                        }
                        boolean anyActive = groups.get(group).stream()
                                .anyMatch(enchantment -> shownActive.getLevel(enchantment) > 0);
                        for (Holder<Enchantment> enchantment : graph.vertices())
                        {
                                if (groups.get(group).contains(enchantment))
                                {
                                        Component label = TooltipLines.name(enchantment, levels.getLevel(enchantment));
                                        this.rows.add(new Row(enchantment, label,
                                                shownActive.getLevel(enchantment) > 0, rowY, group, false));
                                        rowY += ROW_HEIGHT;
                                }
                        }
                        if (allowNone)
                        {
                                // Any enchantment of the group stands for it; the server makes the whole group
                                // dormant.
                                Holder<Enchantment> any = graph.vertices().stream().filter(groups.get(group)::contains)
                                        .findFirst().orElseThrow();
                                Component label = Component.translatable("adjustability.panel.none");
                                this.rows.add(new Row(any, label, !anyActive, rowY, group, true));
                                rowY += ROW_HEIGHT;
                        }
                }
                Size size = size(font, style, stack.getHoverName(), this.rows, this.groupCosts, this.footer);
                this.width = size.width();
                this.height = size.height();
                this.x = placeX(style, this.slotRight, this.width, this.screen.width);
                this.y = placeY(this.slotTop, this.height, this.screen.height);
                return true;
        }

        void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY)
        {
                PanelStyle style = AdjustabilityConfig.player().panel();
                //? if >=26.1 {
                graphics.nextStratum();
                //?} else {
                /*// Above the items and their counts, where tooltips draw.
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 400);
                *///?}
                Row hovered = this.rowAt(mouseX, mouseY).filter(row -> !row.active()).orElse(null);
                draw(graphics, font, style, this.x, this.y, this.width, this.height, this.item().getHoverName(),
                        this.rows, this.groupCosts, this.footer, hovered);
                //? if <26.1
                /*graphics.pose().popPose();*/
        }

        /** Where the first row starts, below the item's name; rows are {@link #ROW_HEIGHT} apart. */
        static int rowsTop(Font font)
        {
                return font.lineHeight + PADDING + 3;
        }

        /** The line at the bottom: the cooldown, and the cost of a switch if one cost covers every group. */
        static Component footer(@Nullable Cost oneCost, int cooldownTicks)
        {
                String cooldown = String.format(Locale.ROOT, "%.1f", cooldownTicks / 20.0);
                return oneCost != null
                        ? Component.translatable("adjustability.panel.cost", Toggler.costText(oneCost), cooldown)
                        : Component.translatable("adjustability.panel.cooldown", cooldown);
        }

        /** {@code groupCosts}: each group's cost for its first row, or empty when the footer has the one cost. */
        static Size size(Font font, PanelStyle style, Component title, List<Row> rows, List<Component> groupCosts,
                Component footer)
        {
                int textWidth = font.width(title);
                int lastGroup = -1;
                for (Row row : rows)
                {
                        int width = font.width(row.label()) + 10;
                        if (row.group() != lastGroup && !groupCosts.isEmpty())
                        {
                                width += 8 + font.width(groupCosts.get(row.group()));
                        }
                        lastGroup = row.group();
                        textWidth = Math.max(textWidth, width);
                }
                int bottom = rows.isEmpty() ? rowsTop(font) : rows.getLast().y() + ROW_HEIGHT;
                if (style.showCost())
                {
                        textWidth = Math.max(textWidth, font.width(footer));
                        bottom += 3 + font.lineHeight;
                }
                return new Size(textWidth + 2 * PADDING, bottom + PADDING);
        }

        /**
         * Left edge of a panel next to the slot whose top-right corner is at {@code slotRight}. AUTO: right of the slot
         * if it fits, else left of it. Never past the screen's edges.
         */
        static int placeX(PanelStyle style, int slotRight, int width, int screenWidth)
        {
                int right = slotRight + GAP_FROM_SLOT;
                int left = slotRight - 16 - GAP_FROM_SLOT - width;
                int x = switch (style.side())
                {
                        case RIGHT -> right;
                        case LEFT -> left;
                        case AUTO -> right + width > screenWidth - BRACKET_OFFSET ? left : right;
                };
                x = Math.min(x, screenWidth - width - BRACKET_OFFSET);
                return Math.max(BRACKET_OFFSET, x);
        }

        static int placeY(int slotTop, int height, int screenHeight)
        {
                return Math.max(BRACKET_OFFSET, Math.min(slotTop - PADDING, screenHeight - height - BRACKET_OFFSET));
        }

        /** The panel with its top-left corner at {@code x, y}; {@code hovered}, a dormant row, gets the highlight. */
        static void draw(GuiGraphicsExtractor graphics, Font font, PanelStyle style, int x, int y, int width,
                int height, Component title, List<Row> rows, List<Component> groupCosts, Component footer,
                @Nullable Row hovered)
        {
                int right = x + width;
                int bottom = y + height;
                graphics.fill(x, y, right, bottom, style.background());
                // Inner border, fading from the top color to the bottom one, like a tooltip's.
                graphics.fill(x + 1, y + 1, right - 1, y + 2, style.borderTop());
                graphics.fill(x + 1, bottom - 2, right - 1, bottom - 1, style.borderBottom());
                graphics.fillGradient(x + 1, y + 2, x + 2, bottom - 2, style.borderTop(), style.borderBottom());
                graphics.fillGradient(right - 2, y + 2, right - 1, bottom - 2, style.borderTop(), style.borderBottom());
                if (style.showBrackets())
                {
                        brackets(graphics, x, y, right, bottom, style);
                }

                graphics.text(font, title, x + PADDING, y + PADDING, ACTIVE_TEXT, true);
                int lastGroup = -1;
                for (Row row : rows)
                {
                        int top = y + row.y();
                        if (row.group() != lastGroup && lastGroup >= 0)
                        {
                                graphics.fill(x + PADDING, top - GROUP_GAP / 2 - 1, right - PADDING,
                                        top - GROUP_GAP / 2, separator(style.borderTop()));
                        }
                        boolean firstOfGroup = row.group() != lastGroup;
                        lastGroup = row.group();
                        boolean highlighted = row == hovered;
                        if (highlighted)
                        {
                                graphics.fill(x + 2, top - 1, right - 2, top + ROW_HEIGHT - 1, style.highlight());
                        }
                        graphics.text(font, row.active() ? "●" : "○", x + PADDING, top + 1,
                                row.active() ? style.activeMark() : DORMANT_TEXT, false);
                        graphics.text(font, row.label(), x + PADDING + 10, top + 1,
                                row.active() || highlighted ? ACTIVE_TEXT : DORMANT_TEXT, true);
                        if (firstOfGroup && !groupCosts.isEmpty())
                        {
                                Component cost = groupCosts.get(row.group());
                                graphics.text(font, cost, right - PADDING - font.width(cost), top + 1, FOOTER_TEXT,
                                        false);
                        }
                }
                if (style.showCost())
                {
                        graphics.text(font, footer, x + PADDING, bottom - PADDING - font.lineHeight + 1, FOOTER_TEXT,
                                false);
                }
        }

        /** The line between conflict groups: the top border's color, a quarter as opaque. */
        private static int separator(int borderTop)
        {
                return (borderTop >>> 26 << 24) | (borderTop & 0xFFFFFF);
        }

        /** Corner brackets just outside the panel, the 2024 design's decoration. */
        private static void brackets(GuiGraphicsExtractor graphics, int x, int y, int right, int bottom,
                PanelStyle style)
        {
                int color = style.bracket();
                int left = x - BRACKET_OFFSET;
                int top = y - BRACKET_OFFSET;
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
                this.rowAt(mouseX, mouseY).filter(row -> !row.active())
                        .flatMap(row -> Optional.ofNullable(row.enchantment()).flatMap(Holder::unwrapKey)
                                .map(key -> new ToggleEnchantmentPayload(this.serverContainerId, this.serverSlot, key,
                                        row.none())))
                        .ifPresent(AdjustClient::send);
        }

        /** Centre of an enchantment's row, in screen coordinates; for the client GameTest. */
        public Optional<int[]> rowCentre(Holder<Enchantment> enchantment)
        {
                return this.rows.stream().filter(row -> !row.none() && enchantment.equals(row.enchantment()))
                        .findFirst().map(row -> new int[]{this.x + this.width / 2, this.y + row.y() + ROW_HEIGHT / 2});
        }

        /** Centre of the None row of the group {@code anyInGroup} belongs to; for the client GameTest. */
        public Optional<int[]> noneRowCentre(Holder<Enchantment> anyInGroup)
        {
                Optional<Integer> group = this.rows.stream().filter(row -> anyInGroup.equals(row.enchantment()))
                        .map(Row::group).findFirst();
                return group.flatMap(index -> this.rows.stream().filter(row -> row.none() && row.group() == index)
                        .findFirst().map(row -> new int[]{this.x + this.width / 2, this.y + row.y() + ROW_HEIGHT / 2}));
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

        /**
         * A row: an enchantment, or with {@code none}, the group's None choice ({@code enchantment} is any of it).
         * {@code enchantment} is {@code null} only in {@link PanelPreview}, which has no registries to take one from.
         * {@code y} is from the panel's top.
         */
        record Row(@Nullable Holder<Enchantment> enchantment, Component label, boolean active, int y, int group,
                boolean none)
        {
        }

        record Size(int width, int height)
        {
        }
}
