package io.github.safetimamovic.adjustability.gui_elements;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.safetimamovic.adjustability.render_core.RenderCore;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>Button</code> class is a public class within the gui_elements package
 * that handles drawing the button graphical element as well as handling the logic
 * of interaction
 */
public class Button
{
	private int x;
	private int y;
	private int height;
	private int width;
	private int bgColor = 0xFF010000;
	private String text;
	private final int BORDER_COLOR_TOP = 0x40FF00FF;
	private final int BORDER_COLOR_BOTTOM = 0x40DC0CB0;


	/**
	 * The Default <code>Button</code> constructor instantiates a Button object.
	 */
	public Button ()
	{

	}

	/**
	 * <code>Button</code> constructor
	 *
	 * @param x
	 * @param y
	 * @param height
	 * @param width
	 * @param bgColor
	 * @param text
	 */
	public Button (int x, int y, int height, int width, int bgColor, String text)
	{
		this.x = x;
		this.y = y;
		this.height = height;
		this.width = width;
		this.bgColor = bgColor; // Pure blue
		this.text = text;
	}

	public void render (PoseStack pStack)
	{
		RenderCore.drawRectangle(
			pStack,
			this.x,
			this.y,
			this.width,
			this.height,
			this.bgColor,
			this.bgColor,
			901
		);

		RenderCore.drawBoxBorders(
			pStack,
			x,
			y,
			this.width,
			this.height,
			this.BORDER_COLOR_TOP,
			this.BORDER_COLOR_BOTTOM,
			false,
			901
		);

		RenderCore.drawBoxBorders(
			pStack,
			x - 1,
			y - 1,
			this.width + 2,
			this.height + 2,
			this.bgColor,
			this.bgColor,
			false,
			901
		);
	}

	public void click (double mouseX, double mouseY)
	{
		if ( RenderCore.isCursorWithinGUIBoundaries(
			( int ) mouseX,
			( int ) mouseY,
			this.x,
			this.y,
			this.width,
			this.height
		) )
		{
			System.out.println("Clicked on Button!");
		}
	}

	public void hover (double mouseX, double mouseY)
	{
		this.bgColor = 0xFF010000;
		if ( RenderCore.isCursorWithinGUIBoundaries(
			( int ) mouseX,
			( int ) mouseY,
			this.x,
			this.y,
			this.width,
			this.height
		) )
		{
			this.bgColor = this.BORDER_COLOR_BOTTOM;
			//System.out.println("Hovering over the Button!");
		}
	}

	public void setPosition(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	public void setProperties(int x, int y, int height, int width, int bgColor, String text)
	{
		this.x = x;
		this.y = y;
		this.height = height;
		this.width = width;
		this.text = text;
		this.setPosition(x, y);
	}
}
