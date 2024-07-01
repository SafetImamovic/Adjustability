package io.github.safetimamovic.adjustability.custom_tooltip;

import io.github.safetimamovic.adjustability.Adjustability;
import io.github.safetimamovic.adjustability.gui_elements.Button;
import io.github.safetimamovic.adjustability.gui_elements.OptionToggle;
import io.github.safetimamovic.adjustability.render_core.RenderCore;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector2i;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>CustomTooltip</code> class draws the custom tooltip while the player is holding down a certain key:
 * it's <code>CTRL</code> by default (as annotated within the default Minecraft tooltip) and handles the logic such as:
 * <ul>
 *         <li>
 *                 Mousing over the tooltip
 *         </li>
 *         <li>
 *                 Interacting with the tooltip
 *         </li>
 * </ul>
 */
@Mod.EventBusSubscriber( modid = Adjustability.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE )
public class CustomTooltip
{
	static boolean rendering = false; // should the rendering be active
	static int x, y; // x and y coordinates for the position
	private boolean clickActive = false; // set momentarily if the player clicked a mouse button
	private final List<Button> Buttons; // stores the buttons relative to the custom tooltip that's being drawn
	private boolean arraySet = false; // when the button count has been set within the Buttons array this is set to true
	// preventing instantiation of new buttons during runtime
	// the array is cleared when the custom tooltip is destroyed
	private final List<OptionToggle> toggleableOptions;

	public CustomTooltip ()
	{
		this.Buttons = new ArrayList<>();
		this.toggleableOptions = new ArrayList<>();
	}

	/**
	 * This method cancels the default tooltip when the player pressed or is holding down a particular character
	 * or combination of characters
	 * TODO: change the key from being hardcoded for ctrl
	 *
	 * @param event RenderTooltipEvent
	 */
	@SubscribeEvent
	public static void cancelDefaultTooltip (RenderTooltipEvent event)
	{
		if ( Screen.hasControlDown() )  //TODO: Get rid of magic numbers
		{
			event.setCanceled(true);
		}
	}

//	@SubscribeEvent
//	public static void onClickTickWise (InputEvent.MouseButton event)
//	{
////		System.out.println("Action: ");
////		System.out.println(event.getAction());
////		System.out.println("Button: ");
////		System.out.println(event.getButton());
//	}

	/**
	 * This method just adds the description at the bottom of the default tooltip. It outlines which character
	 * or combination of characters the player needs to press or hold down for the custom tooltip to appear
	 *
	 * @param event ItemTooltipEvent
	 */
	@SubscribeEvent
	public static void addBottomInformationToTheDefaultTooltipForOpeningTheCustomTooltipGui (ItemTooltipEvent event)
	{
		event.getToolTip().addLast(Component.empty());
		event.getToolTip().addLast(Component.literal("Hold [CTRL] for:")); //TODO: Get rid of magic numbers
		event.getToolTip().addLast(Component.literal("<<Enchantment Adjustments>>").withColor(0xFFFF00FF)); //TODO: Get rid of magic numbers
	}

	/**
	 * The <code>mouseEventHelper</code> method determines if a mouse event occurred within the area of the tooltip
	 *
	 * @param xMouseEvent   <code>x</code> coordinate of the mouse position
	 * @param yMouseEvent   <code>y</code> coordinate of the mouse position
	 * @param pScreenWidth  the Screens width in pixels
	 * @param pScreenHeight the Screens height in pixels
	 * @return <code>true</code> if the cursor is within the tooltip boundaries, otherwise <code>false</code>
	 */
	public static boolean isCursorWithinGUIBoundariesRelativeToScreen (int xMouseEvent, int yMouseEvent, int pScreenWidth, int pScreenHeight)
	{
		Vector2ic correctedPosition = positionTooltip(
			pScreenWidth,
			pScreenHeight,
			x,
			y,
			CustomTooltipConfig.WIDTH,
			CustomTooltipConfig.HEIGHT
		);

		return RenderCore.isCursorWithinGUIBoundaries(
			xMouseEvent,
			yMouseEvent,
			correctedPosition.x() - CustomTooltipConfig.PADDING,
			correctedPosition.y() - CustomTooltipConfig.PADDING,
			CustomTooltipConfig.WIDTH,
			CustomTooltipConfig.HEIGHT
		);
	}

	/**
	 * This method cancels the <code>MouseDragged</code> event within the
	 * <code>Screen</code> if the cursors is hovering over the boundaries
	 * of the <code>CustomTooltip</code>.
	 *
	 * @param event ScreenEvent.MouseDragged.Pre
	 */
	@SubscribeEvent
	public static void blockScreenInputMouseDragged (ScreenEvent.MouseDragged.Pre event)
	{
		if ( rendering )
		{
			event.setCanceled(isCursorWithinGUIBoundariesRelativeToScreen((( int ) event.getMouseX()), (( int ) event.getMouseY()), event.getScreen().width, event.getScreen().height));
		}
	}

	/**
	 * This method cancels the <code>MouseButtonPressed</code> event within the
	 * <code>Screen</code> if the cursors is hovering over the boundaries
	 *
	 * @param event ScreenEvent.MouseButtonPressed.Pre
	 */
	@SubscribeEvent
	public static void blockScreenInputMouseClick (ScreenEvent.MouseButtonPressed.Pre event)
	{
		if ( rendering )
		{
			event.setCanceled(isCursorWithinGUIBoundariesRelativeToScreen((( int ) event.getMouseX()), (( int ) event.getMouseY()), event.getScreen().width, event.getScreen().height));
		}
	}

	/**
	 * This method cancels the <code>MouseButtonReleased</code> event within the
	 * <code>Screen</code> if the cursors is hovering over the boundaries
	 *
	 * @param event ScreenEvent.MouseButtonReleased.Pre
	 */
	@SubscribeEvent
	public static void blockScreenInputMouseClickRelease (ScreenEvent.MouseButtonReleased.Pre event)
	{
		if ( rendering )
		{
			event.setCanceled(isCursorWithinGUIBoundariesRelativeToScreen((( int ) event.getMouseX()), (( int ) event.getMouseY()), event.getScreen().width, event.getScreen().height));
		}
	}

	/**
	 * This method cancels the <code>MouseScrolled</code> event within the
	 * <code>Screen</code> if the cursors is hovering over the boundaries
	 *
	 * @param event ScreenEvent.MouseScrolled.Pre
	 */
	@SubscribeEvent
	public static void blockScreenInputMouseScroll (ScreenEvent.MouseScrolled.Pre event)
	{
		if ( rendering )
		{
			event.setCanceled(isCursorWithinGUIBoundariesRelativeToScreen((( int ) event.getMouseX()), (( int ) event.getMouseY()), event.getScreen().width, event.getScreen().height));
		}
	}

	/**
	 * The <code>letGoOfKey</code> method checks when the player let's go of the key, then sets the <code>rendering</code>
	 * variable to false
	 *
	 * @param event ScreenEvent.KeyReleased
	 */
	@SubscribeEvent
	public static void letGoOfKey (ScreenEvent.KeyReleased event)
	{
		if ( event.getKeyCode() == 341 )  //TODO: Get rid of magic numbers
		{
			rendering = false;
		}
	}

	/**
	 * The <code>shouldRender</code> method examines if the rendering process should proceed based on if the player is holding
	 * down, or if the player pressed the correct key, or combination there of.
	 * TODO: Also change the key from being hardcoded as ctrl
	 *
	 * @param mouseX is the last x coordinate of the cursor position when the player initiated the render
	 * @param mouseY is the last y coordinate of the cursor position when the player initiated the render
	 * @return <code>true</code> if the player is holding the correct key and if the <code>rendering</code>
	 * variable is set to false
	 */
	private static boolean shouldRender (int mouseX, int mouseY)
	{
		if ( Screen.hasControlDown() && !rendering )
		{
			rendering = true;
			x = mouseX;
			y = mouseY;
			return true;
		}
		else
		{
			return rendering;
		}
	}

	/**
	 * This method draws the base rectangle, including the rectangle shape with the borders for the <code>CustomTooltip</code>
	 *
	 * @param event    ScreenEvent.Render
	 * @param position <code>Vector2ic</code> position for the rectangle
	 */
	public static void drawBaseRectangleWithBorders (ScreenEvent.Render event, Vector2ic position)
	{
		int x = position.x() - CustomTooltipConfig.PADDING;
		int y = position.y() - CustomTooltipConfig.PADDING;

		RenderCore.drawRectangle(
			event.getGuiGraphics().pose(),
			x,
			y,
			CustomTooltipConfig.WIDTH,
			CustomTooltipConfig.HEIGHT,
			CustomTooltipConfig.BACKGROUND_COLOR,
			CustomTooltipConfig.BACKGROUND_COLOR,
			CustomTooltipConfig.Z_INDEX_BASE
		);

		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			x,
			y,
			CustomTooltipConfig.WIDTH,
			CustomTooltipConfig.HEIGHT,
			CustomTooltipConfig.BORDER_COLOR_TOP,
			CustomTooltipConfig.BORDER_COLOR_BOTTOM,
			CustomTooltipConfig.DRAW_CORNERS_INNER,
			CustomTooltipConfig.Z_INDEX_BASE
		);

		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			x - CustomTooltipConfig.BORDER_WIDTH,
			y - CustomTooltipConfig.BORDER_HEIGHT,
			CustomTooltipConfig.WIDTH + 2 * CustomTooltipConfig.BORDER_WIDTH,
			CustomTooltipConfig.HEIGHT + 2 * CustomTooltipConfig.BORDER_HEIGHT,
			CustomTooltipConfig.BACKGROUND_COLOR,
			CustomTooltipConfig.BACKGROUND_COLOR,
			CustomTooltipConfig.DRAW_CORNERS,
			CustomTooltipConfig.Z_INDEX_BASE
		);
	}

	/**
	 * This method draws the little corner decorations for the <code>CustomTooltip</code>.<br>
	 * TODO: Add configuration options for designing this
	 *
	 * @param event             ScreenEvent.Render
	 * @param correctedPosition corrected position for the decorations
	 */
	public static void drawDecorationsAroundTooltip (ScreenEvent.Render event, Vector2ic correctedPosition)
	{
		// Top Left Vertical Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - CustomTooltipConfig.PADDING - CustomTooltipConfig.VERTICAL_DECOR_OFFSET,
			correctedPosition.y() - CustomTooltipConfig.PADDING - CustomTooltipConfig.VERTICAL_DECOR_OFFSET,
			CustomTooltipConfig.VERTICAL_DECOR_HEIGHT,
			CustomTooltipConfig.VERTICAL_DECOR_LENGTH,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.DRAW_CORNERS,
			CustomTooltipConfig.Z_INDEX_BASE
		);

		// Top Left Horizontal Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - CustomTooltipConfig.PADDING - CustomTooltipConfig.HORIZONTAL_DECOR_OFFSET,
			correctedPosition.y() - CustomTooltipConfig.PADDING - CustomTooltipConfig.HORIZONTAL_DECOR_OFFSET,
			CustomTooltipConfig.HORIZONTAL_DECOR_LENGTH,
			CustomTooltipConfig.HORIZONTAL_DECOR_HEIGHT,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.DRAW_CORNERS,
			CustomTooltipConfig.Z_INDEX_BASE
		);

		// Top Right Vertical Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - CustomTooltipConfig.PADDING + CustomTooltipConfig.WIDTH + CustomTooltipConfig.VERTICAL_DECOR_OFFSET - CustomTooltipConfig.HORIZONTAL_DECOR_HEIGHT,
			correctedPosition.y() - CustomTooltipConfig.PADDING - CustomTooltipConfig.VERTICAL_DECOR_OFFSET,
			CustomTooltipConfig.VERTICAL_DECOR_HEIGHT,
			CustomTooltipConfig.VERTICAL_DECOR_LENGTH,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.DRAW_CORNERS,
			CustomTooltipConfig.Z_INDEX_BASE
		);

		// Top Right Horizontal Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - CustomTooltipConfig.PADDING + CustomTooltipConfig.WIDTH - CustomTooltipConfig.HORIZONTAL_DECOR_LENGTH + CustomTooltipConfig.HORIZONTAL_DECOR_OFFSET,
			correctedPosition.y() - CustomTooltipConfig.PADDING - CustomTooltipConfig.HORIZONTAL_DECOR_OFFSET,
			CustomTooltipConfig.HORIZONTAL_DECOR_LENGTH,
			CustomTooltipConfig.HORIZONTAL_DECOR_HEIGHT,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.DRAW_CORNERS,
			CustomTooltipConfig.Z_INDEX_BASE
		);

		// Bottom Left Vertical Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - CustomTooltipConfig.PADDING - CustomTooltipConfig.VERTICAL_DECOR_OFFSET,
			correctedPosition.y() - CustomTooltipConfig.PADDING - CustomTooltipConfig.VERTICAL_DECOR_LENGTH + CustomTooltipConfig.VERTICAL_DECOR_OFFSET + CustomTooltipConfig.HEIGHT,
			CustomTooltipConfig.VERTICAL_DECOR_HEIGHT,
			CustomTooltipConfig.VERTICAL_DECOR_LENGTH,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.DRAW_CORNERS,
			CustomTooltipConfig.Z_INDEX_BASE
		);

		// Bottom Left Horizontal Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - CustomTooltipConfig.PADDING - CustomTooltipConfig.HORIZONTAL_DECOR_OFFSET,
			correctedPosition.y() - CustomTooltipConfig.PADDING + CustomTooltipConfig.HORIZONTAL_DECOR_OFFSET + CustomTooltipConfig.HEIGHT - CustomTooltipConfig.HORIZONTAL_DECOR_HEIGHT,
			CustomTooltipConfig.HORIZONTAL_DECOR_LENGTH,
			CustomTooltipConfig.HORIZONTAL_DECOR_HEIGHT,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.DRAW_CORNERS,
			CustomTooltipConfig.Z_INDEX_BASE
		);

		// Bottom Right Vertical Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - CustomTooltipConfig.PADDING + CustomTooltipConfig.WIDTH + CustomTooltipConfig.VERTICAL_DECOR_OFFSET - CustomTooltipConfig.VERTICAL_DECOR_HEIGHT,
			correctedPosition.y() - CustomTooltipConfig.PADDING + CustomTooltipConfig.VERTICAL_DECOR_OFFSET + CustomTooltipConfig.HEIGHT - CustomTooltipConfig.VERTICAL_DECOR_LENGTH,
			CustomTooltipConfig.VERTICAL_DECOR_HEIGHT,
			CustomTooltipConfig.VERTICAL_DECOR_LENGTH,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.DRAW_CORNERS,
			CustomTooltipConfig.Z_INDEX_BASE
		);

		// Bottom Right Horizontal Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - CustomTooltipConfig.PADDING + CustomTooltipConfig.WIDTH - CustomTooltipConfig.HORIZONTAL_DECOR_LENGTH + CustomTooltipConfig.HORIZONTAL_DECOR_OFFSET,
			correctedPosition.y() - CustomTooltipConfig.PADDING + CustomTooltipConfig.HORIZONTAL_DECOR_OFFSET + CustomTooltipConfig.HEIGHT - CustomTooltipConfig.HORIZONTAL_DECOR_HEIGHT,
			CustomTooltipConfig.HORIZONTAL_DECOR_LENGTH,
			CustomTooltipConfig.HORIZONTAL_DECOR_HEIGHT,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.BLENDED_COLOR,
			CustomTooltipConfig.DRAW_CORNERS,
			CustomTooltipConfig.Z_INDEX_BASE
		);
	}

	/**
	 * This method calls the other underlying render methods to draw the different elements, such as
	 * rectangles, lines, text...
	 *
	 * @param event ScreenEvent.Render
	 * @see ScreenEvent.Render
	 */
	@SubscribeEvent
	public void drawBoxBorders (ScreenEvent.Render event)
	{
		int numberOfButtons = 2;

		if ( !shouldRender(event.getMouseX(), event.getMouseY()) )
		{
			return;
		}

		Vector2ic correctedPosition = positionTooltip(
			event.getScreen().width,
			event.getScreen().height,
			x,
			y,
			CustomTooltipConfig.WIDTH + CustomTooltipConfig.BORDER_WIDTH + CustomTooltipConfig.BORDER_HEIGHT,
			CustomTooltipConfig.HEIGHT + CustomTooltipConfig.BORDER_WIDTH + CustomTooltipConfig.BORDER_HEIGHT
		);

		drawBaseRectangleWithBorders(event, correctedPosition);

		drawDecorationsAroundTooltip(event, correctedPosition);

		RenderCore.renderText(
			"Nice",
			correctedPosition.x(),
			correctedPosition.y(),
			CustomTooltipConfig.Z_INDEX_BUTTONS_TEXT,
			event.getGuiGraphics()
		);

		if ( !this.arraySet )
		{
			this.toggleableOptions.add(new OptionToggle(
				0, 1,
				this.x,
				this.y,
				"ENCHANTMENT!"
			));

			this.arraySet = true;
		}

		for ( int i = 0; i < this.toggleableOptions.size(); i++ )
		{
			this.toggleableOptions.get(0).update(correctedPosition.x(), correctedPosition.y(), event.getMouseX(), event.getMouseY());

			this.toggleableOptions.get(0).leftButton.render(event.getGuiGraphics());
			this.toggleableOptions.get(0).rightButton.render(event.getGuiGraphics());

			System.out.println("SIZE: " + this.toggleableOptions.size());

			if ( this.clickActive )
			{
				this.toggleableOptions.get(0).leftButton.click(event.getMouseX(), event.getMouseY());
				this.toggleableOptions.get(0).rightButton.click(event.getMouseX(), event.getMouseY());

				this.clickActive = false;
			}
		}
	}

	@SubscribeEvent
	public void click (ScreenEvent.MouseButtonPressed event)
	{
		this.clickActive = true;
	}

	/**
	 * The <code>positionTooltip</code> method repositions the custom tooltip based on its current position in relation to the screen height and width.
	 * So if the tooltip crosses the boundaries of the screen it gets repositioned to be rendered within the boundaries of the screen.
	 * Much like how the default tooltip re-positioner works
	 *
	 * @param pScreenWidth   width of the <code>Screen</code>
	 * @param pScreenHeight  height of the <code>Screen</code>
	 * @param pMouseX        mouse x coordinate
	 * @param pMouseY        mouse y coordinate
	 * @param pTooltipWidth  width of the tooltip
	 * @param pTooltipHeight height of the tooltip
	 * @return <code>Vector2ic</code> for the corrected position
	 */
	public static Vector2ic positionTooltip (int pScreenWidth, int pScreenHeight, int pMouseX, int pMouseY, int pTooltipWidth, int pTooltipHeight)
	{
		Vector2i correction = (new Vector2i(pMouseX, pMouseY)).add(CustomTooltipConfig.MOUSE_OFFSET, -CustomTooltipConfig.MOUSE_OFFSET);
		positionTooltip(pScreenWidth, pScreenHeight, correction, pTooltipWidth, pTooltipHeight);
		return correction;
	}

	/**
	 * The <code>positionTooltip</code> method repositions the custom tooltip based on its current position in relation to the screen height and width.
	 * So if the tooltip crosses the boundaries of the screen it gets repositioned to be rendered within the boundaries of the screen.
	 * Much like how the default tooltip re-positioner works
	 *
	 * @param pScreenWidth   width of the <code>Screen</code>
	 * @param pScreenHeight  height of the <code>Screen</code>
	 * @param pTooltipPos    <code>Vector2i</code> of the tooltip position
	 * @param pTooltipWidth  width of the tooltip
	 * @param pTooltipHeight height of the tooltip
	 */
	public static void positionTooltip (int pScreenWidth, int pScreenHeight, Vector2i pTooltipPos, int pTooltipWidth, int pTooltipHeight)
	{
		if ( pTooltipPos.x + pTooltipWidth > pScreenWidth )
		{
			pTooltipPos.x = Math.max(pTooltipPos.x - 2 * CustomTooltipConfig.MOUSE_OFFSET - pTooltipWidth, CustomTooltipConfig.PADDING);
		}

		int $$5 = pTooltipHeight + CustomTooltipConfig.PADDING;
		if ( pTooltipPos.y + $$5 > pScreenHeight )
		{
			pTooltipPos.y = pScreenHeight - $$5;
		}
	}
}