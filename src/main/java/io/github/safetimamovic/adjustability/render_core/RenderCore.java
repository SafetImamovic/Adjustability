package io.github.safetimamovic.adjustability.render_core;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

/**
 * The RenderCore class provides static methods for rendering various graphical elements
 * on the screen using Minecraft's rendering system.
 * This class includes methods for rendering text, drawing box borders, and drawing filled
 * rectangles with gradient colors.
 */
public class RenderCore
{
	/**
	 * The <code>mouseEventHelper</code> method determines if a mouse event occurred within the area of the tooltip
	 *
	 * @param mouseX <code>x</code> coordinate of the mouse position
	 * @param mouseY <code>y</code> coordinate of the mouse position
	 * @param x x coordinate
	 * @param y y coordinate
	 * @param width       the GUI's width in pixels
	 * @param height      the GUI's height in pixels
	 * @return <code>true</code> if the cursor is within the tooltip boundaries, otherwise <code>false</code>
	 */
	public static boolean isCursorWithinGUIBoundaries (int mouseX, int mouseY, int x, int y, int width, int height)
	{
		return (mouseX > x
			&& mouseX < x + width)
			&& (mouseY > y
			&& mouseY < y + height);
	}

	/**
	 * Renders a text string on the screen with specified parameters.
	 *
	 * @param x           The x-coordinate position where the text should be rendered.
	 * @param y           The y-coordinate position where the text should be rendered.
	 * @param zIndex      The z-index or layering order of the text in 3D space.
	 * @param guiGraphics The GUI graphics context used for rendering.
	 */
	public static void renderText (float x, float y, int zIndex, GuiGraphics guiGraphics)
	{
		PoseStack poseStack = guiGraphics.pose();
		poseStack.translate(0, 0, zIndex); // Set the Z-index
		MultiBufferSource bufferSource = guiGraphics.bufferSource();
		RenderSystem.disableDepthTest();
		//RenderSystem.enableBlend();
		Minecraft.getInstance().font.drawInBatch(
			"Example",
			x + TooltipRenderUtil.MOUSE_OFFSET + TooltipRenderUtil.PADDING_LEFT,
			y - TooltipRenderUtil.MOUSE_OFFSET + TooltipRenderUtil.PADDING_TOP,
			0xFFFFFF,
			true,
			guiGraphics.pose().last().pose(),
			bufferSource,
			Font.DisplayMode.NORMAL,
			0xFFFFFF,
			0xFFFFFF
		);
		RenderSystem.enableDepthTest();
		//RenderSystem.disableBlend();
	}

	/**
	 * This method draws the Box Borders for the <code>CustomTooltip</code> GUI.
	 * @param poseStack     	The PoseStack used for rendering transformations.
	 * @param x             	The x-coordinate of the top-left corner of the box.
	 * @param y             	The y-coordinate of the top-left corner of the box.
	 * @param width         	The width of the box.
	 * @param height        	The height of the box.
	 * @param colorPrimary  	The color of the primary borders (top and left).
	 * @param colorSecondary 	The color of the secondary borders (bottom and right).
	 * @param drawCorner    	If true, the corners of the box are drawn; otherwise, they are not drawn.
	 * @param zIndex        	The rendering priority or depth index.
	 */
	public static void drawBoxBorders (PoseStack poseStack, int x, int y, int width, int height, int colorPrimary, int colorSecondary, boolean drawCorner, int zIndex)
	{
		int offset = drawCorner ? 0 : 1;

		// Top Horizontal Line
		RenderCore.drawRectangle(poseStack, x + offset, y, width - 2 * offset, 1, colorPrimary, colorPrimary, zIndex);

		// Bottom Horizontal Line
		RenderCore.drawRectangle(poseStack, x + offset, y + height - 1, width - 2 * offset, 1, colorSecondary, colorSecondary, zIndex);

		// Left Vertical Line
		RenderCore.drawRectangle(poseStack, x, y + 1, 1, height - 2, colorPrimary, colorSecondary, zIndex);

		// Right Vertical Line
		RenderCore.drawRectangle(poseStack, x + width - 1, y + 1, 1, height - 2, colorPrimary, colorSecondary, zIndex);
	}

	/**
	 * Draws a filled rectangle with gradient colors on the screen.
	 *
	 * @param poseStack     	The PoseStack used for rendering transformations.
	 * @param x             	The x-coordinate of the top-left corner of the rectangle.
	 * @param y             	The y-coordinate of the top-left corner of the rectangle.
	 * @param width         	The width of the rectangle.
	 * @param height        	The height of the rectangle.
	 * @param colorPrimary  	The color of the top-left corner (starting gradient color).
	 * @param colorSecondary 	The color of the bottom-right corner (ending gradient color).
	 * @param zIndex        	The rendering priority or depth index.
	 */
	public static void drawRectangle (PoseStack poseStack, int x, int y, int width, int height, int colorPrimary, int colorSecondary, int zIndex)
	{
		poseStack.pushPose();
		Matrix4f matrix = poseStack.last().pose();
		drawGradientRectangle(matrix, zIndex, x, y, x + width, y + height, colorPrimary, colorSecondary);
		poseStack.popPose();
	}

	/**
	 * Draws a filled rectangle with gradient colors using specified rendering parameters.
	 *
	 * @param matrix         The transformation matrix for rendering.
	 * @param zIndex         The rendering priority or depth index.
	 * @param left           The x-coordinate of the left edge of the rectangle.
	 * @param top            The y-coordinate of the top edge of the rectangle.
	 * @param right          The x-coordinate of the right edge of the rectangle.
	 * @param bottom         The y-coordinate of the bottom edge of the rectangle.
	 * @param colorPrimary   The color of the top-left corner (starting gradient color).
	 * @param colorSecondary The color of the bottom-right corner (ending gradient color).
	 */
	public static void drawGradientRectangle (Matrix4f matrix, int zIndex, int left, int top, int right, int bottom, int colorPrimary, int colorSecondary)
	{
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.getBuilder();
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		drawGradientRectangle(matrix, bufferBuilder, left, top, right, bottom, colorPrimary, colorSecondary, zIndex);
		BufferUploader.drawWithShader(bufferBuilder.end());

		RenderSystem.disableBlend();
	}

	/**
	 * Draws a filled rectangle with gradient colors using specified rendering parameters.
	 *
	 * @param matrix         The transformation matrix for rendering.
	 * @param bufferBuilder  The BufferBuilder instance for vertex data.
	 * @param left           The x-coordinate of the left edge of the rectangle.
	 * @param top            The y-coordinate of the top edge of the rectangle.
	 * @param right          The x-coordinate of the right edge of the rectangle.
	 * @param bottom         The y-coordinate of the bottom edge of the rectangle.
	 * @param colorPrimary   The color of the top-left corner (starting gradient color).
	 * @param colorSecondary The color of the bottom-right corner (ending gradient color).
	 * @param zIndex         The rendering priority or depth index.
	 */
	public static void drawGradientRectangle (Matrix4f matrix, BufferBuilder bufferBuilder, int left, int top, int right, int bottom, int colorPrimary, int colorSecondary, int zIndex)
	{
		bufferBuilder.vertex(matrix, right, top, zIndex).color(colorPrimary).endVertex();
		bufferBuilder.vertex(matrix, left, top, zIndex).color(colorPrimary).endVertex();
		bufferBuilder.vertex(matrix, left, bottom, zIndex).color(colorSecondary).endVertex();
		bufferBuilder.vertex(matrix, right, bottom, zIndex).color(colorSecondary).endVertex();
	}
}