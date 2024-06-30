package io.github.safetimamovic.adjustability;

import com.sun.jna.platform.win32.Wincon;
import io.github.safetimamovic.adjustability.gui_elements.Button;
import io.github.safetimamovic.adjustability.render_core.RenderCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import net.minecraftforge.client.event.InputEvent;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
	/**
	 * Constants and variables used for tooltip rendering.
	 * <br>
	 * These fields define various properties such as offsets, padding, and colors
	 * for rendering tooltips, as well as control the rendering state and position.
	 * <br>
	 * These fields are essentially copied over from the <code>TooltipRenderUtil</code> class
	 *
	 * @see TooltipRenderUtil
	 */
	static final int MOUSE_OFFSET = TooltipRenderUtil.MOUSE_OFFSET;
	static final int PADDING = TooltipRenderUtil.PADDING_TOP;
	static final int BACKGROUND_COLOR = 0xFF010000; // -267386864
	static final int BORDER_COLOR_TOP = 0x40FF00FF; // 1347420415
	static final int BORDER_COLOR_BOTTOM = 0x40DC0CB0; // 1344798847
	static final int BLENDED_COLOR = 0xFF410040;
	static boolean rendering = false;
	static int x, y;
	private boolean clickActive = false;
	private List<Button> Buttons;
	private boolean arraySet = false;

	public CustomTooltip ()
	{
		this.Buttons = new ArrayList<>();
	}

	/**
	 * This method cancels the default tooltip when the player pressed or is holding down a particular character
	 * or combination of characters
	 *
	 * @param event
	 */
	@SubscribeEvent
	public static void cancelDefaultTooltip (RenderTooltipEvent event)
	{
		if ( Screen.hasControlDown() )
		{
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onClickTickWise (InputEvent.MouseButton event)
	{
//		System.out.println("Action: ");
//		System.out.println(event.getAction());
//		System.out.println("Button: ");
//		System.out.println(event.getButton());
	}

	/**
	 * This method just adds the description at the bottom of the default tooltip. It outlines which character
	 * or combination of characters the player needs to press or hold down for the custom tooltip to appear
	 *
	 * @param event
	 */
	@SubscribeEvent
	public static void addBottomInformationToTheDefaultTooltipForOpeningTheCustomTooltipGui (ItemTooltipEvent event)
	{
		event.getToolTip().addLast(Component.empty());
		event.getToolTip().addLast(Component.literal("Hold [CTRL] for:"));
		event.getToolTip().addLast(Component.literal("<<Enchantment Adjustments>>").withColor(0xFFFF00FF));
	}

	/**
	 * The <code>mouseEventHelper</code> method determines if a mouse event occurred within the area of the tooltip
	 *
	 * @param xMouseEvent <code>x</code> coordinate of the mouse position
	 * @param yMouseEvent <code>y</code> coordinate of the mouse position
	 * @param width       the GUI's width in pixels
	 * @param height      the GUI's height in pixels
	 * @return <code>true</code> if the cursor is within the tooltip boundaries, otherwise <code>false</code>
	 */
	public static boolean isCursorWithinGUIBoundaries (int xMouseEvent, int yMouseEvent, int width, int height)
	{
		Vector2ic correctedPosition = positionTooltip(
			width,
			height,
			x,
			y,
			102,
			102
		);

		return RenderCore.isCursorWithinGUIBoundaries(
			xMouseEvent,
			yMouseEvent,
			correctedPosition.x() - PADDING,
			correctedPosition.y() - PADDING,
			102,
			102
		);
	}

	/**
	 * This method cancels the <code>MouseDragged</code> event within the
	 * <code>Screen</code> if the cursors is hovering over the boundaries
	 * of the <code>CustomTooltip</code>.
	 *
	 * @param event
	 */
	@SubscribeEvent
	public static void blockScreenInputMouseDragged (ScreenEvent.MouseDragged.Pre event)
	{
		event.setCanceled(isCursorWithinGUIBoundaries((( int ) event.getMouseX()), (( int ) event.getMouseY()), event.getScreen().width, event.getScreen().height));
	}

	/**
	 * This method cancels the <code>MouseButtonPressed</code> event within the
	 * <code>Screen</code> if the cursors is hovering over the boundaries
	 *
	 * @param event
	 */
	@SubscribeEvent
	public static void blockScreenInputMouseClick (ScreenEvent.MouseButtonPressed.Pre event)
	{
		event.setCanceled(isCursorWithinGUIBoundaries((( int ) event.getMouseX()), (( int ) event.getMouseY()), event.getScreen().width, event.getScreen().height));
	}

	/**
	 * This method cancels the <code>MouseButtonReleased</code> event within the
	 * <code>Screen</code> if the cursors is hovering over the boundaries
	 *
	 * @param event
	 */
	@SubscribeEvent
	public static void blockScreenInputMouseClickRelease (ScreenEvent.MouseButtonReleased.Pre event)
	{
		event.setCanceled(isCursorWithinGUIBoundaries((( int ) event.getMouseX()), (( int ) event.getMouseY()), event.getScreen().width, event.getScreen().height));
	}

	/**
	 * This method cancels the <code>MouseScrolled</code> event within the
	 * <code>Screen</code> if the cursors is hovering over the boundaries
	 *
	 * @param event
	 */
	@SubscribeEvent
	public static void blockScreenInputMouseScroll (ScreenEvent.MouseScrolled.Pre event)
	{
		event.setCanceled(isCursorWithinGUIBoundaries((( int ) event.getMouseX()), (( int ) event.getMouseY()), event.getScreen().width, event.getScreen().height));
	}


	/**
	 * The <code>letGoOfKey</code> method checks when the player let's go of the key, then sets the <code>rendering</code>
	 * variable to false
	 *
	 * @param event
	 */
	@SubscribeEvent
	public static void letGoOfKey (ScreenEvent.KeyReleased event)
	{
		if ( event.getKeyCode() == 341 )
		{
			rendering = false;
		}
	}

	/**
	 * The <code>shouldRender</code> method examines if the rendering process should proceed based on if the player is holding
	 * down, or if the player pressed the correct key, or combination there of.
	 *
	 * @param mouseX is the last x coordinate of the cursor position when the player initiated the render
	 * @param mouseY is the last y coordinate of the cursor position when the player initiated the render
	 * @return <code>true</code> if the player is holding the the correct key and if the <code>rendering</code>
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
		else if ( !rendering )
		{
			return false;
		}
		return true;
	}

	/**
	 * This method draws the base rectangle, including the rectangle shape with the borders for the <code>CustomToolip</code>
	 *
	 * @param event
	 * @param position
	 */
	public static void drawBaseRectangleWithBorders (ScreenEvent.Render event, Vector2ic position)
	{
		int x = position.x() - PADDING;
		int y = position.y() - PADDING;
		int size = 100;
		int borderSize = 102;

		RenderCore.drawRectangle(
			event.getGuiGraphics().pose(),
			x,
			y,
			size,
			size,
			BACKGROUND_COLOR,
			BACKGROUND_COLOR,
			900
		);

		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			x,
			y,
			size,
			size,
			BORDER_COLOR_TOP,
			BORDER_COLOR_BOTTOM,
			true,
			900
		);

		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			x - 1,
			y - 1,
			borderSize,
			borderSize,
			BACKGROUND_COLOR,
			BACKGROUND_COLOR,
			false,
			900
		);
	}

	/**
	 * This method draws the little corner decorations for the <code>CustomTooltip</code>.<br>
	 * TODO: Add configuration options for designing this
	 *
	 * @param event
	 * @param correctedPosition
	 */
	public static void drawDecorationsAroundTooltip (ScreenEvent.Render event, Vector2ic correctedPosition)
	{
		int verticalDecorLength = 16;
		int horizontalDecorLength = 16;

		// Top Left Vertical Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - PADDING - 5,
			correctedPosition.y() - PADDING - 5,
			3,
			verticalDecorLength,
			BLENDED_COLOR,
			BLENDED_COLOR,
			false,
			900
		);

		// Top Left Horizontal Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - PADDING - 5,
			correctedPosition.y() - PADDING - 5,
			horizontalDecorLength,
			3,
			BLENDED_COLOR,
			BLENDED_COLOR,
			false,
			900
		);

		// Top Right Vertical Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - PADDING + 100 + 5 - 3,
			correctedPosition.y() - PADDING - 5,
			3,
			verticalDecorLength,
			BLENDED_COLOR,
			BLENDED_COLOR,
			false,
			900
		);

		// Top Right Horizontal Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - PADDING + 100 - horizontalDecorLength + 5,
			correctedPosition.y() - PADDING - 5,
			horizontalDecorLength,
			3,
			BLENDED_COLOR,
			BLENDED_COLOR,
			false,
			900
		);

		// Bottom Left Vertical Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - PADDING - 5,
			correctedPosition.y() - PADDING - verticalDecorLength + 5 + 100,
			3,
			verticalDecorLength,
			BLENDED_COLOR,
			BLENDED_COLOR,
			false,
			900
		);

		// Bottom Left Horizontal Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - PADDING - 5,
			correctedPosition.y() - PADDING + 5 + 100 - 3,
			horizontalDecorLength,
			3,
			BLENDED_COLOR,
			BLENDED_COLOR,
			false,
			900
		);

		// Bottom Right Vertical Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - PADDING + 100 + 5 - 3,
			correctedPosition.y() - PADDING + 5 + 100 - verticalDecorLength,
			3,
			verticalDecorLength,
			BLENDED_COLOR,
			BLENDED_COLOR,
			false,
			900
		);

		// Bottom Right Horizontal Decor
		RenderCore.drawBoxBorders(
			event.getGuiGraphics().pose(),
			correctedPosition.x() - PADDING + 100 - horizontalDecorLength + 5,
			correctedPosition.y() - PADDING + 5 + 100 - 3,
			horizontalDecorLength,
			3,
			BLENDED_COLOR,
			BLENDED_COLOR,
			false,
			900
		);
	}

	/**
	 * This method calls the other underlying render methods to draw the different elements, such as
	 * rectangles, lines, text...
	 *
	 * @param event
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
			102,
			102
		);

		drawBaseRectangleWithBorders(event, correctedPosition);

		drawDecorationsAroundTooltip(event, correctedPosition);

		RenderCore.renderText(
			"Example",
			correctedPosition.x(),
			correctedPosition.y(),
			901,
			0xFFFFFFFF,
			true,
			event.getGuiGraphics()
		);

		if ( this.arraySet == false )
		{
			for ( int i = 0; i < numberOfButtons; i++ )
			{
				this.Buttons.add(new Button());
			}
			this.arraySet = true;
		}

		for ( int i = 0; i < 1; i++ )
		{
			this.Buttons.get(i).setProperties(
				correctedPosition.x(),
				correctedPosition.y() + 100 - 12 - PADDING - 3,
				12,
				12,
				BACKGROUND_COLOR,
				"NICE"
			);

			this.Buttons.get(i + 1).setProperties(
				correctedPosition.x() - 12 + 100 - 2 * 3,
				correctedPosition.y() + 100 - 12 - PADDING - 3,
				12,
				12,
				BACKGROUND_COLOR,
				"NICE"
			);

			this.Buttons.get(i).render(event.getGuiGraphics().pose());
			this.Buttons.get(i).hover(event.getMouseX(), event.getMouseY());

			this.Buttons.get(i + 1).render(event.getGuiGraphics().pose());
			this.Buttons.get(i + 1).hover(event.getMouseX(), event.getMouseY());
		}

		if ( this.clickActive )
		{
			//B.click(event.getMouseX(), event.getMouseY());
			this.clickActive = false;
		}

		System.out.println("SIZE: " + this.Buttons.size());
	}

	@SubscribeEvent
	public void click (ScreenEvent.MouseButtonPressed event)
	{
		this.clickActive = true;
	}

	/**
	 * The <code>positionTooltip</code> method repositions the custom tooltip based on it's current position in relation to the screen height and width.
	 * So if the tooltip crosses the boundaries of the screen it gets repositioned to be rendered within the boundaries of the screen.
	 * Much like how the default tooltip repositioner works
	 *
	 * @param pScreenWidth
	 * @param pScreenHeight
	 * @param pMouseX
	 * @param pMouseY
	 * @param pTooltipWidth
	 * @param pTooltipHeight
	 * @return Vector2ic for the corrected position
	 */
	public static Vector2ic positionTooltip (int pScreenWidth, int pScreenHeight, int pMouseX, int pMouseY, int pTooltipWidth, int pTooltipHeight)
	{
		Vector2i $$6 = (new Vector2i(pMouseX, pMouseY)).add(12, -12);
		positionTooltip(pScreenWidth, pScreenHeight, $$6, pTooltipWidth, pTooltipHeight);
		return $$6;
	}

	/**
	 * The <code>positionTooltip</code> method repositions the custom tooltip based on it's current position in relation to the screen height and width.
	 * So if the tooltip crosses the boundaries of the screen it gets repositioned to be rendered within the boundaries of the screen.
	 * Much like how the default tooltip repositioner works
	 *
	 * @param pScreenWidth
	 * @param pScreenHeight
	 * @param pTooltipPos
	 * @param pTooltipWidth
	 * @param pTooltipHeight
	 */
	public static void positionTooltip (int pScreenWidth, int pScreenHeight, Vector2i pTooltipPos, int pTooltipWidth, int pTooltipHeight)
	{
		if ( pTooltipPos.x + pTooltipWidth > pScreenWidth )
		{
			pTooltipPos.x = Math.max(pTooltipPos.x - 24 - pTooltipWidth, 4);
		}

		int $$5 = pTooltipHeight + 3;
		if ( pTooltipPos.y + $$5 > pScreenHeight )
		{
			pTooltipPos.y = pScreenHeight - $$5;
		}
	}
}