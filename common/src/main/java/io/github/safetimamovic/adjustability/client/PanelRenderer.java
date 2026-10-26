package io.github.safetimamovic.adjustability.client;

import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.api.client.RowMark;
import io.github.safetimamovic.adjustability.client.PanelLayout.Content;
import io.github.safetimamovic.adjustability.client.PanelLayout.Header;
import io.github.safetimamovic.adjustability.client.PanelLayout.Row;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.Frame;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.GroupStyle;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.PanelStyle;
import io.github.safetimamovic.adjustability.config.AdjustabilityConfig.Text;
import java.util.function.ToDoubleFunction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? if >=26.1
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Adjust panel from its {@link Content}: the frame, the title, each row with its mark, the group headings or
 * separators, and the cost line. Static and settings-driven, so the YACL preview draws the same panel.
 */
final class PanelRenderer
{
        /** The frame for {@code border_style = SPRITE}: {@code textures/gui/sprites/panel.png}, nine-sliced. */
        static final Identifier SPRITE = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "panel");

        private PanelRenderer()
        {
        }

        /**
         * The panel with its top-left corner at {@code x, y}. {@code hovered}, a row that can be clicked, or
         * {@code hoveredHeader}, the singles' heading, gets the highlight; {@code active} gives each row's mark how
         * active it is (0 to 1), for animated switches.
         */
        static void draw(GuiGraphicsExtractor graphics, Font font, PanelStyle style, int x, int y, Content content,
                @Nullable Row hovered, @Nullable Header hoveredHeader, ToDoubleFunction<Row> active)
        {
                int right = x + content.width();
                int bottom = y + content.height();
                int padding = style.layout().padding();
                Text text = style.text();
                frame(graphics, style.frame(), x, y, right, bottom);

                if (text.showTitle())
                {
                        int titleX = x + padding;
                        int titleHeight = PanelLayout.titleHeight(font, text);
                        if (text.showIcon())
                        {
                                //? if >=26.1 {
                                graphics.fakeItem(content.icon(), titleX, y + padding);
                                //?} else
                                /*graphics.renderFakeItem(content.icon(), titleX, y + padding);*/
                                titleX += PanelLayout.ICON_SIZE + PanelLayout.ICON_GAP;
                        }
                        graphics.text(font, content.title(), titleX,
                                y + padding + (titleHeight - font.lineHeight + 1) / 2, text.title(), text.shadow());
                }

                for (Header header : content.headers())
                {
                        int top = y + header.y() + 1;
                        if (header.section() && header.group() > 0 && style.layout().groups() == GroupStyle.LINE)
                        {
                                int line = y + header.y() - PanelLayout.GROUP_GAP / 2 - 1;
                                graphics.fill(x + padding, line, right - padding, line + 1, style.layout().separator());
                        }
                        if (header == hoveredHeader)
                        {
                                graphics.fill(x + 2, top - 2, right - 2, top + PanelLayout.headerHeight(font) - 2,
                                        style.frame().highlight());
                        }
                        graphics.text(font, header.name(), x + padding, top,
                                header == hoveredHeader ? text.active() : text.footer(), false);
                        if (!content.groupCosts().isEmpty())
                        {
                                cost(graphics, font, content.groupCosts().get(header.group()), right - padding, top,
                                        text);
                        }
                }

                RowMark mark = RowMarks.get(style.marks().style());
                int labelX = x + PanelLayout.labelX(style);
                int rowHeight = style.layout().rowHeight();
                int lastGroup = -1;
                for (Row row : content.rows())
                {
                        int top = y + row.y();
                        boolean firstOfGroup = row.group() != lastGroup;
                        if (firstOfGroup && lastGroup >= 0 && style.layout().groups() == GroupStyle.LINE
                                && !content.headed(row.group()))
                        {
                                int line = top - PanelLayout.GROUP_GAP / 2 - 1;
                                graphics.fill(x + padding, line, right - padding, line + 1, style.layout().separator());
                        }
                        lastGroup = row.group();
                        boolean highlighted = row == hovered;
                        if (highlighted)
                        {
                                graphics.fill(x + 2, top - 1, right - 2, top + rowHeight - 1,
                                        style.frame().highlight());
                        }
                        mark.draw(graphics, x + padding, PanelLayout.markY(font, style, top),
                                new RowMark.State((float) active.applyAsDouble(row), highlighted, row.none(),
                                        style.marks().active(), style.marks().dormant()));
                        int textY = PanelLayout.textY(font, style, top);
                        graphics.text(font, row.label(), labelX, textY,
                                row.active() || highlighted ? text.active() : text.dormant(), text.shadow());
                        if (firstOfGroup && !content.headed(row.group()) && !content.groupCosts().isEmpty())
                        {
                                cost(graphics, font, content.groupCosts().get(row.group()), right - padding, textY,
                                        text);
                        }
                }
                if (text.showCost())
                {
                        graphics.text(font, content.footer(), x + padding, bottom - padding - font.lineHeight + 1,
                                text.footer(), false);
                }
        }

        /** Right-aligned at {@code right}. */
        private static void cost(GuiGraphicsExtractor graphics, Font font, Component cost, int right, int y, Text text)
        {
                graphics.text(font, cost, right - font.width(cost), y, text.footer(), false);
        }

        private static void frame(GuiGraphicsExtractor graphics, Frame frame, int x, int y, int right, int bottom)
        {
                switch (frame.border())
                {
                        case SPRITE ->
                        {
                                //? if >=26.1 {
                                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITE, x, y, right - x, bottom - y);
                                //?} else
                                /*graphics.blitSprite(SPRITE, x, y, right - x, bottom - y);*/
                        }
                        case GRADIENT ->
                        {
                                graphics.fill(x, y, right, bottom, frame.background());
                                // Inner border, fading from the top color to the bottom one, like a tooltip's.
                                graphics.fill(x + 1, y + 1, right - 1, y + 2, frame.borderTop());
                                graphics.fill(x + 1, bottom - 2, right - 1, bottom - 1, frame.borderBottom());
                                graphics.fillGradient(x + 1, y + 2, x + 2, bottom - 2, frame.borderTop(),
                                        frame.borderBottom());
                                graphics.fillGradient(right - 2, y + 2, right - 1, bottom - 2, frame.borderTop(),
                                        frame.borderBottom());
                        }
                        case SOLID ->
                        {
                                graphics.fill(x, y, right, bottom, frame.background());
                                graphics.fill(x + 1, y + 1, right - 1, y + 2, frame.borderTop());
                                graphics.fill(x + 1, bottom - 2, right - 1, bottom - 1, frame.borderTop());
                                graphics.fill(x + 1, y + 2, x + 2, bottom - 2, frame.borderTop());
                                graphics.fill(right - 2, y + 2, right - 1, bottom - 2, frame.borderTop());
                        }
                        case NONE -> graphics.fill(x, y, right, bottom, frame.background());
                }
                if (frame.showBrackets())
                {
                        brackets(graphics, frame, x, y, right, bottom);
                }
        }

        /** Corner brackets just outside the panel, the 2024 design's decoration. */
        private static void brackets(GuiGraphicsExtractor graphics, Frame frame, int x, int y, int right, int bottom)
        {
                int color = frame.bracket();
                int left = x - PanelLayout.BRACKET_OFFSET;
                int top = y - PanelLayout.BRACKET_OFFSET;
                int outerRight = right + PanelLayout.BRACKET_OFFSET;
                int outerBottom = bottom + PanelLayout.BRACKET_OFFSET;
                int l = frame.bracketLength();
                graphics.fill(left, top, left + l, top + 2, color);
                graphics.fill(left, top, left + 2, top + l, color);
                graphics.fill(outerRight - l, top, outerRight, top + 2, color);
                graphics.fill(outerRight - 2, top, outerRight, top + l, color);
                graphics.fill(left, outerBottom - 2, left + l, outerBottom, color);
                graphics.fill(left, outerBottom - l, left + 2, outerBottom, color);
                graphics.fill(outerRight - l, outerBottom - 2, outerRight, outerBottom, color);
                graphics.fill(outerRight - 2, outerBottom - l, outerRight, outerBottom, color);
        }
}
