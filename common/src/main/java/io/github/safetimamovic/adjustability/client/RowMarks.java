package io.github.safetimamovic.adjustability.client;

import io.github.safetimamovic.adjustability.Constants;
import io.github.safetimamovic.adjustability.api.client.RowMark;
import io.github.safetimamovic.adjustability.api.client.RowMark.State;
import io.github.safetimamovic.adjustability.config.PlayerConfig;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The row marks by id: Adjustability's own (see {@link PlayerConfig#BUILT_IN_MARK_STYLES}) and those other mods add
 * through {@code AdjustabilityClientApi}. Every built-in one is drawn with fills, pixel by pixel, so fonts don't change
 * it.
 */
public final class RowMarks
{
        private static final Map<String, RowMark> MARKS = new LinkedHashMap<>();
        /** Ids already reported as unknown, so the log gets one line each, not one a frame. */
        private static final Set<String> REPORTED = new HashSet<>();

        /** The dormant track of the toggle: the dormant color, this opaque. */
        private static final float TRACK_ALPHA = 0.45F;
        private static final int KNOB = 0xFFFFFFFF;

        static
        {
                MARKS.put(PlayerConfig.DEFAULT_MARK_STYLE, mark(7, RowMarks::circle));
                MARKS.put("adjustability:square", mark(7, RowMarks::square));
                MARKS.put("adjustability:checkbox", mark(7, RowMarks::checkbox));
                MARKS.put("adjustability:toggle", mark(13, RowMarks::toggle));
                MARKS.put("adjustability:radio", mark(7, RowMarks::radio));
                MARKS.put("adjustability:text_only", mark(0, (graphics, x, y, state) ->
                {
                }));
                PlayerConfig.markStyles(RowMarks::ids);
        }

        private RowMarks()
        {
        }

        /** Makes {@code mark_style} offer the registered ids; the client calls it once at startup. */
        static void init()
        {
        }

        public static void register(String id, RowMark mark)
        {
                if (MARKS.putIfAbsent(id, mark) != null)
                {
                        throw new IllegalArgumentException("A row mark with the id " + id + " is already registered");
                }
        }

        public static Set<String> ids()
        {
                return Collections.unmodifiableSet(MARKS.keySet());
        }

        /** The mark with this id, or the circle if no mod registered it. */
        static RowMark get(String id)
        {
                RowMark mark = MARKS.get(id);
                if (mark != null)
                {
                        return mark;
                }
                if (REPORTED.add(id))
                {
                        Constants.LOG.warn("No row mark {} (was the mod that added it removed?); using {}", id,
                                PlayerConfig.DEFAULT_MARK_STYLE);
                }
                return MARKS.get(PlayerConfig.DEFAULT_MARK_STYLE);
        }

        /** ○ when dormant, ● when active. */
        private static void circle(GuiGraphicsExtractor graphics, int x, int y, State state)
        {
                ring(graphics, x, y, state.color());
                int inside = State.withAlpha(state.activeColor(), state.active());
                fill(graphics, x + 2, y + 1, 3, 1, inside);
                fill(graphics, x + 1, y + 2, 5, 3, inside);
                fill(graphics, x + 2, y + 5, 3, 1, inside);
        }

        /** A hollow square, filled when active. */
        private static void square(GuiGraphicsExtractor graphics, int x, int y, State state)
        {
                box(graphics, x, y, 7, 7, state.color());
                fill(graphics, x + 1, y + 1, 5, 5, State.withAlpha(state.activeColor(), state.active()));
        }

        /** A box, ticked when active. */
        private static void checkbox(GuiGraphicsExtractor graphics, int x, int y, State state)
        {
                box(graphics, x, y, 7, 7, state.color());
                int tick = State.withAlpha(state.activeColor(), state.active());
                fill(graphics, x + 1, y + 3, 1, 1, tick);
                fill(graphics, x + 2, y + 4, 1, 1, tick);
                fill(graphics, x + 3, y + 3, 1, 1, tick);
                fill(graphics, x + 4, y + 2, 1, 1, tick);
                fill(graphics, x + 5, y + 1, 1, 1, tick);
        }

        /** A switch: the knob left on a dim track when dormant, right on a colored one when active. */
        private static void toggle(GuiGraphicsExtractor graphics, int x, int y, State state)
        {
                int track = State.blend(State.withAlpha(state.dormantColor(), TRACK_ALPHA), state.activeColor(),
                        state.active());
                fill(graphics, x + 1, y, 11, 7, track);
                fill(graphics, x, y + 1, 13, 5, track);
                int knob = x + 1 + Math.round(state.active() * 6);
                fill(graphics, knob + 1, y + 1, 3, 5, KNOB);
                fill(graphics, knob, y + 2, 5, 3, KNOB);
        }

        /** A ring with a dot inside when active. */
        private static void radio(GuiGraphicsExtractor graphics, int x, int y, State state)
        {
                ring(graphics, x, y, state.color());
                fill(graphics, x + 2, y + 2, 3, 3, State.withAlpha(state.activeColor(), state.active()));
        }

        /** A 7 × 7 pixel ring. */
        private static void ring(GuiGraphicsExtractor graphics, int x, int y, int color)
        {
                fill(graphics, x + 2, y, 3, 1, color);
                fill(graphics, x + 2, y + 6, 3, 1, color);
                fill(graphics, x, y + 2, 1, 3, color);
                fill(graphics, x + 6, y + 2, 1, 3, color);
                fill(graphics, x + 1, y + 1, 1, 1, color);
                fill(graphics, x + 5, y + 1, 1, 1, color);
                fill(graphics, x + 1, y + 5, 1, 1, color);
                fill(graphics, x + 5, y + 5, 1, 1, color);
        }

        /** A one-pixel outline. */
        private static void box(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color)
        {
                fill(graphics, x, y, width, 1, color);
                fill(graphics, x, y + height - 1, width, 1, color);
                fill(graphics, x, y + 1, 1, height - 2, color);
                fill(graphics, x + width - 1, y + 1, 1, height - 2, color);
        }

        private static void fill(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color)
        {
                if (color >>> 24 != 0)
                {
                        graphics.fill(x, y, x + width, y + height, color);
                }
        }

        private static RowMark mark(int width, Drawer drawer)
        {
                return new RowMark()
                {
                        @Override
                        public int width()
                        {
                                return width;
                        }

                        @Override
                        public void draw(GuiGraphicsExtractor graphics, int x, int y, State state)
                        {
                                drawer.draw(graphics, x, y, state);
                        }
                };
        }

        @FunctionalInterface
        private interface Drawer
        {
                void draw(GuiGraphicsExtractor graphics, int x, int y, State state);
        }
}
