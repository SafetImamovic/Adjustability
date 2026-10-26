package io.github.safetimamovic.adjustability.client;

import io.github.safetimamovic.adjustability.api.client.RowMark;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.GroupStyle;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.Layout;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.NonePosition;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelStyle;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jspecify.annotations.Nullable;

/**
 * Where everything in the Adjust panel goes: rows and group headings from the panel's top, the panel's size, and its
 * place next to the slot. Pure arithmetic on the player's settings, shared by the panel and the YACL preview.
 */
final class PanelLayout
{
        static final int GAP_FROM_SLOT = 6;
        static final int BRACKET_OFFSET = 3;
        static final int GROUP_GAP = 5;
        /** Between a row's mark and its text. */
        static final int MARK_GAP = 3;
        /** Between the title and the first row. */
        private static final int TITLE_GAP = 3;
        /** The item icon in front of the title, and the space after it. */
        static final int ICON_SIZE = 16;
        static final int ICON_GAP = 2;
        /** Between a group's text and its cost, when they share a line. */
        private static final int COST_GAP = 8;

        private PanelLayout()
        {
        }

        /** Places the groups' rows and headings and works out the panel's size. */
        static Content layout(Font font, PanelStyle style, Component title, ItemStack icon, List<Group> groups,
                List<Component> groupCosts, Component footer)
        {
                Layout layout = style.layout();
                List<Row> rows = new ArrayList<>();
                List<Header> headers = new ArrayList<>();
                int y = rowsTop(font, style);
                for (int index = 0; index < groups.size(); index++)
                {
                        Group group = groups.get(index);
                        if (index > 0 && layout.groups() != GroupStyle.NONE)
                        {
                                y += GROUP_GAP;
                        }
                        if (layout.groups() == GroupStyle.HEADER)
                        {
                                headers.add(new Header(group.name(), y, index));
                                y += headerHeight(font);
                        }
                        for (Entry entry : ordered(group, style))
                        {
                                rows.add(new Row(entry.enchantment(), entry.label(), entry.active(), y, index,
                                        entry.none()));
                                y += layout.rowHeight();
                        }
                }
                Content content = new Content(title, icon, rows, headers, groupCosts, footer, 0, 0);
                return content.sized(size(font, style, content));
        }

        /** A group's rows in the player's order, the None row first or last. */
        private static List<Entry> ordered(Group group, PanelStyle style)
        {
                List<Entry> entries = new ArrayList<>(group.entries());
                switch (style.layout().order())
                {
                        case ALPHABETICAL -> entries.sort(Comparator.comparing(entry -> entry.label().getString()));
                        case ACTIVE_FIRST -> entries.sort(Comparator.comparing(entry -> !entry.active()));
                        case REGISTRY ->
                        {
                        }
                }
                if (group.none() != null)
                {
                        entries.add(style.layout().none() == NonePosition.FIRST ? 0 : entries.size(), group.none());
                }
                return entries;
        }

        /** Where the first row (or heading) starts, from the panel's top. */
        static int rowsTop(Font font, PanelStyle style)
        {
                int padding = style.layout().padding();
                return style.text().showTitle() ? padding + titleHeight(font, style.text()) + TITLE_GAP : padding;
        }

        static int titleHeight(Font font, Text text)
        {
                return text.showIcon() ? ICON_SIZE : font.lineHeight;
        }

        static int headerHeight(Font font)
        {
                return font.lineHeight + 1;
        }

        /** Where a row's text starts, from the panel's left edge. */
        static int labelX(PanelStyle style)
        {
                int markWidth = RowMarks.get(style.marks().style()).width();
                return style.layout().padding() + (markWidth > 0 ? markWidth + MARK_GAP : 0);
        }

        /** The text's top in a row whose top is {@code rowTop}; marks share it, so they line up with the letters. */
        static int textY(Font font, PanelStyle style, int rowTop)
        {
                return rowTop + Math.max(0, (style.layout().rowHeight() - font.lineHeight + 1) / 2);
        }

        /** The mark's top: its box centred on the letters' height. */
        static int markY(Font font, PanelStyle style, int rowTop)
        {
                return textY(font, style, rowTop) + Math.max(0, (font.lineHeight - 2 - RowMark.HEIGHT) / 2);
        }

        private static Size size(Font font, PanelStyle style, Content content)
        {
                Layout layout = style.layout();
                Text text = style.text();
                boolean costOnHeader = layout.groups() == GroupStyle.HEADER;
                int textWidth = 0;
                if (text.showTitle())
                {
                        textWidth = font.width(content.title()) + (text.showIcon() ? ICON_SIZE + ICON_GAP : 0);
                }
                int labelX = labelX(style) - layout.padding();
                int lastGroup = -1;
                for (Row row : content.rows())
                {
                        int width = labelX + font.width(row.label());
                        if (row.group() != lastGroup && !costOnHeader && !content.groupCosts().isEmpty())
                        {
                                width += COST_GAP + font.width(content.groupCosts().get(row.group()));
                        }
                        lastGroup = row.group();
                        textWidth = Math.max(textWidth, width);
                }
                for (Header header : content.headers())
                {
                        int width = font.width(header.name());
                        if (!content.groupCosts().isEmpty())
                        {
                                width += COST_GAP + font.width(content.groupCosts().get(header.group()));
                        }
                        textWidth = Math.max(textWidth, width);
                }
                int bottom = content.rows().isEmpty() ? rowsTop(font, style)
                        : content.rows().getLast().y() + layout.rowHeight();
                if (text.showCost())
                {
                        textWidth = Math.max(textWidth, font.width(content.footer()));
                        bottom += 3 + font.lineHeight;
                }
                return new Size(textWidth + 2 * layout.padding(), bottom + layout.padding());
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

        static int placeY(PanelStyle style, int slotTop, int height, int screenHeight)
        {
                return Math.max(BRACKET_OFFSET, Math.min(slotTop - style.layout().padding(),
                        screenHeight - height - BRACKET_OFFSET));
        }

        /**
         * A group of conflicting enchantments before layout.
         *
         * @param name its heading, for {@code group_style = HEADER}
         * @param none its None row, if the server allows switching the group off
         */
        record Group(Component name, List<Entry> entries, @Nullable Entry none)
        {
        }

        /**
         * A row before layout: an enchantment, or with {@code none}, the group's None choice ({@code enchantment} is
         * any of it). {@code enchantment} is {@code null} only in {@link PanelPreview}, which has no registries.
         */
        record Entry(@Nullable Holder<Enchantment> enchantment, Component label, boolean active, boolean none)
        {
        }

        /** A placed row; {@code y} is from the panel's top. */
        record Row(@Nullable Holder<Enchantment> enchantment, Component label, boolean active, int y, int group,
                boolean none)
        {
        }

        /** A placed group heading; {@code y} is from the panel's top. */
        record Header(Component name, int y, int group)
        {
        }

        /**
         * Everything the panel shows, placed, and its size.
         *
         * @param groupCosts each group's cost, shown on its heading or first row; empty when the footer has the one
         *                   cost for all
         */
        record Content(Component title, ItemStack icon, List<Row> rows, List<Header> headers,
                List<Component> groupCosts, Component footer, int width, int height)
        {
                Content sized(Size size)
                {
                        return new Content(this.title, this.icon, this.rows, this.headers, this.groupCosts,
                                this.footer, size.width(), size.height());
                }
        }

        record Size(int width, int height)
        {
        }
}
