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

public class RenderCore
{
    public static void renderText(String text, float x, float y, int zIndex, int color, boolean dropShadow, GuiGraphics guiGraphics)
    {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.translate(0, 0, zIndex); // Set the Z-index
        MultiBufferSource bufferSource = guiGraphics.bufferSource();

        RenderSystem.enableBlend();
        Minecraft.getInstance().font.drawInBatch(
                "Example",
                x + TooltipRenderUtil.MOUSE_OFFSET + TooltipRenderUtil.PADDING_LEFT,
                y - TooltipRenderUtil.MOUSE_OFFSET + TooltipRenderUtil.PADDING_TOP,
                0xFFFFFFFF,
                true,
                guiGraphics.pose().last().pose(),
                bufferSource,
                Font.DisplayMode.NORMAL,
                0x00000000,
                1
        );

        RenderSystem.disableBlend();
    }

    public static void drawBoxBorders(PoseStack poseStack, int x, int y, int width, int height, int colorPrimary, int colorSecondary, boolean drawCorner, int zIndex)
    {
        int additionalPixel = 1;

        if (drawCorner)
            additionalPixel = 0;

        // Top Horizontal Line
        RenderCore.drawRectangle(
                poseStack,
                x + (additionalPixel),
                y,
                width - (2 * additionalPixel),
                1,
                colorPrimary,
                colorPrimary,
                zIndex
        );

        // Bottom Horizontal Line
        RenderCore.drawRectangle(
                poseStack,
                x + (additionalPixel),
                y + height - 1,
                width - (2 * additionalPixel),
                1,
                colorSecondary,
                colorSecondary,
                zIndex
        );

        // Left Vertical Line
        RenderCore.drawRectangle(
                poseStack,
                x,
                y + 1,
                1,
                height - 2,
                colorPrimary,
                colorSecondary,
                zIndex
        );

        // Right Vertical Line
        RenderCore.drawRectangle(
                poseStack,
                x + width - 1,
                y + 1,
                1,
                height - 2,
                colorPrimary,
                colorSecondary,
                zIndex
        );
    }

    public static void drawRectangle(PoseStack poseStack, int x, int y, int width, int height, int colorPrimary, int colorSecondary, int zIndex)
    {
        poseStack.pushPose();
        Matrix4f matrix = poseStack.last().pose();
        drawGradientRectangle(matrix, zIndex, x, y, x + width, y + height, colorPrimary, colorSecondary);
        poseStack.popPose();


    }

    public static void drawGradientRectangle(Matrix4f matrix, int zIndex, int left, int top, int right, int bottom, int colorPrimary, int colorSecondary)
    {
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        drawGradientRectangle(matrix, bufferBuilder, left, top, right, bottom, colorPrimary, colorSecondary, zIndex);
        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.disableBlend();
    }

    public static void drawGradientRectangle(Matrix4f matrix, BufferBuilder bufferBuilder, int left, int top, int right, int bottom, int colorPrimary, int colorSecondary, int zIndex)
    {
        bufferBuilder.vertex(matrix, right, top, zIndex).color(colorPrimary).endVertex();
        bufferBuilder.vertex(matrix, left, top, zIndex).color(colorPrimary).endVertex();
        bufferBuilder.vertex(matrix, left, bottom, zIndex).color(colorSecondary).endVertex();
        bufferBuilder.vertex(matrix, right, bottom, zIndex).color(colorSecondary).endVertex();
    }
}