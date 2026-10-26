package io.github.safetimamovic.adjustability.api.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.ApiStatus;

/**
 * The mark in front of each row of the Adjust panel, like the circle (filled when active) or the toggle switch. Players
 * pick one by id with {@code mark_style} in {@code [panel.marks]}.
 *
 * <p>A mark draws into a box {@link #width()} wide and {@link #HEIGHT} tall, centred on the row; the row's text
 * starts a few pixels after it. Draw with {@code fill} and sprites rather than font glyphs, which change with resource
 * packs and the Unicode font setting. Called every frame, on the render thread; keep it cheap and keep no state: the
 * panel animates switches through {@link State#active()}.
 *
 * <p>Experimental until 1.0: names may still change.
 */
@ApiStatus.Experimental
public interface RowMark
{
        /** Height of the box every mark draws in, in GUI pixels. */
        int HEIGHT = 7;

        /** Width of the box, in GUI pixels; 0 for no mark. */
        int width();

        /**
         * Draws the mark with the box's top-left corner at {@code x, y}.
         */
        void draw(GuiGraphicsExtractor graphics, int x, int y, State state);

        /**
         * One row's mark.
         *
         * @param active       1 for an active enchantment, 0 for a dormant one, in between while the panel animates a
         *                     switch
         * @param hovered      the mouse is over the row (only dormant rows can be clicked)
         * @param none         the row is a group's None choice, not an enchantment
         * @param activeColor  the player's {@code active_color}, ARGB
         * @param dormantColor the player's {@code dormant_mark_color}, ARGB
         */
        record State(float active, boolean hovered, boolean none, int activeColor, int dormantColor)
        {
                /** The dormant color fading into the active one as {@link #active()} goes from 0 to 1. */
                public int color()
                {
                        return blend(this.dormantColor, this.activeColor, this.active);
                }

                /** {@code color} with its opacity multiplied by {@code alpha} (0 to 1). */
                public static int withAlpha(int color, float alpha)
                {
                        int a = Math.round((color >>> 24) * Math.clamp(alpha, 0, 1));
                        return a << 24 | color & 0xFFFFFF;
                }

                /** Each ARGB channel from {@code from} to {@code to}; {@code t} is 0 to 1. */
                public static int blend(int from, int to, float t)
                {
                        float clamped = Math.clamp(t, 0, 1);
                        int out = 0;
                        for (int shift = 0; shift < 32; shift += 8)
                        {
                                int a = from >>> shift & 0xFF;
                                int b = to >>> shift & 0xFF;
                                out |= Math.round(a + (b - a) * clamped) << shift;
                        }
                        return out;
                }
        }
}
