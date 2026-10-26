package io.github.safetimamovic.adjustability.gametest;

import io.github.safetimamovic.adjustability.api.client.RowMark;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The row mark example on the Java API docs page (docs/guide/java-api.md); {@link PanelStylesClientGameTest} registers
 * it and takes its picture, so it can't rot. Keep the two in sync.
 */
public final class DiamondMark implements RowMark
{
        @Override
        public int width()
        {
                return 7;
        }

        /** A diamond outline, filled when active. */
        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y, State state)
        {
                int outline = state.color();
                int inside = State.withAlpha(state.activeColor(), state.active());
                for (int row = 0; row < HEIGHT; row++)
                {
                        int half = 3 - Math.abs(3 - row);
                        graphics.fill(x + 3 - half, y + row, x + 4 + half, y + row + 1, inside);
                        graphics.fill(x + 3 - half, y + row, x + 4 - half, y + row + 1, outline);
                        graphics.fill(x + 3 + half, y + row, x + 4 + half, y + row + 1, outline);
                }
        }
}
