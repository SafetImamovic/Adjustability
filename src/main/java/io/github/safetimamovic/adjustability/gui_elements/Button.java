package io.github.safetimamovic.adjustability.gui_elements;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.safetimamovic.adjustability.render_core.RenderCore;

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
	private int bgColor;
	private String text;

	/**
	 * The Default <code>Button</code> constructor instantiates a Button object and assigns it the following values:
	 * <ol>
	 *         <li>x position = 0</li>
	 *         <li>y position = 0</li>
	 *         <li>height = 50</li>
	 *         <li>width = 50</li>
	 *         <li>background color = pure blue</li>
	 *         <li>text = "Example Button"</li>
	 * </ol>
	 */
	public Button()
	{
		this.x = 0;
		this.y = 0;
		this.height = 50;
		this.width = 50;
		this.bgColor = 0xFF0000FF; // Pure blue
		this.text = "Example Button";
	}

	/**
	 * <code>Button</code> constructor
	 * @param x
	 * @param y
	 * @param height
	 * @param width
	 * @param bgColor
	 * @param text
	 */
	public Button(int x, int y, int height, int width, int bgColor, String text)
	{
		this.x = x;
		this.y = y;
		this.height = height;
		this.width = width;
		this.bgColor = bgColor; // Pure blue
		this.text = text;
	}

	public void render(PoseStack pStack)
	{
		RenderCore.drawRectangle(
			pStack,
			this.x,
			this.y,
			this.width,
			this.height,
			this.bgColor,
			this.bgColor,
			1000
		);
	}
}
