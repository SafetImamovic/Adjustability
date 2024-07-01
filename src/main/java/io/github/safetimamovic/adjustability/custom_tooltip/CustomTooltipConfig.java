package io.github.safetimamovic.adjustability.custom_tooltip;

import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;

/**
 * The <code>CustomTooltipConfig</code> class defines the different properties for the <code>CustomTooltip</code>
 * static wise.
 */
public class CustomTooltipConfig
{
	public static final int BORDER_WIDTH = 1;
	public static final int BORDER_HEIGHT = 1;

	public static int MOUSE_OFFSET = TooltipRenderUtil.MOUSE_OFFSET;
	public static int PADDING = TooltipRenderUtil.PADDING_TOP;
	public static int BACKGROUND_COLOR = 0xFF010000; // -267386864
	public static int BORDER_COLOR_TOP = 0x40FF00FF; // 1347420415
	public static int BORDER_COLOR_BOTTOM = 0x40DC0CB0; // 1344798847
	public static int BLENDED_COLOR = 0xFF410040;
	public static int WIDTH = 80;
	public static int HEIGHT = 120;
	public static int Z_INDEX_BASE = 900;
	public static int Z_INDEX_BUTTONS_TEXT = 901;
	public static boolean DRAW_CORNERS = false;
	public static boolean DRAW_CORNERS_INNER = true;
	public static int VERTICAL_DECOR_LENGTH = 16;
	public static int HORIZONTAL_DECOR_LENGTH = 16;
	public static int VERTICAL_DECOR_HEIGHT = 3;
	public static int HORIZONTAL_DECOR_HEIGHT = 3;
	public static int VERTICAL_DECOR_OFFSET = 5;
	public static int HORIZONTAL_DECOR_OFFSET = 5;
	public static int BUTTON_WIDTH = 16;
	public static int BUTTON_HEIGHT = 14;
}
