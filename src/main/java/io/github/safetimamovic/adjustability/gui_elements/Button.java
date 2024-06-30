package io.github.safetimamovic.adjustability.gui_elements;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.safetimamovic.adjustability.render_core.RenderCore;

/**
 * The <code>Button</code> class is a public class within the gui_elements package
 * that handles drawing the button graphical element as well as handling the logic
 * of interaction.
 */
public class Button {
	private int x;
	private int y;
	private int height;
	private int width;
	private int bgColor = 0xFF010000;
	private final int BORDER_COLOR_BOTTOM = 0x40DC0CB0;

	/**
	 * The Default <code>Button</code> constructor instantiates a Button object.
	 */
	public Button() {
	}

	/**
	 * Renders the button using the provided PoseStack.
	 *
	 * @param pStack the PoseStack used for rendering the button
	 */
	public void render(PoseStack pStack) {
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

		int BORDER_COLOR_TOP = 0x40FF00FF;
		RenderCore.drawBoxBorders(
			pStack,
			x,
			y,
			this.width,
			this.height,
			BORDER_COLOR_TOP,
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

	/**
	 * Handles click events on the button.
	 *
	 * @param mouseX the x-coordinate of the mouse click
	 * @param mouseY the y-coordinate of the mouse click
	 */
	public void click(double mouseX, double mouseY) {
		if (RenderCore.isCursorWithinGUIBoundaries(
			(int) mouseX,
			(int) mouseY,
			this.x,
			this.y,
			this.width,
			this.height
		)) {
			System.out.println("Clicked on Button!");
		}
	}

	/**
	 * Handles hover events on the button.
	 *
	 * @param mouseX the x-coordinate of the mouse hover
	 * @param mouseY the y-coordinate of the mouse hover
	 */
	public void hover(double mouseX, double mouseY) {
		this.bgColor = 0xFF010000;
		if (RenderCore.isCursorWithinGUIBoundaries(
			(int) mouseX,
			(int) mouseY,
			this.x,
			this.y,
			this.width,
			this.height
		)) {
			this.bgColor = this.BORDER_COLOR_BOTTOM;
			//System.out.println("Hovering over the Button!");
		}
	}

	/**
	 * Sets the position of the button.
	 *
	 * @param x the x-coordinate of the button's new position
	 * @param y the y-coordinate of the button's new position
	 */
	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Sets the properties of the button.
	 *
	 * @param x the x-coordinate of the button
	 * @param y the y-coordinate of the button
	 * @param height the height of the button
	 * @param width the width of the button
	 * @param bgColor the background color of the button
	 * @param text the text displayed on the button (currently unused)
	 */
	public void setProperties(int x, int y, int height, int width, int bgColor, String text) {
		this.x = x;
		this.y = y;
		this.height = height;
		this.width = width;
		this.setPosition(x, y);
	}
}