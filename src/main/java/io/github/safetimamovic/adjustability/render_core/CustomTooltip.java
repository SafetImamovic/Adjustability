package io.github.safetimamovic.adjustability.render_core;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.github.safetimamovic.adjustability.Adjustability;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector2ic;

@Mod.EventBusSubscriber(modid = Adjustability.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CustomTooltip
{
    static final int MOUSE_OFFSET = TooltipRenderUtil.MOUSE_OFFSET;
    static final int PADDING = TooltipRenderUtil.PADDING_TOP;
    static final int BACKGROUND_COLOR = 0xFF010000; // -267386864
    static final int BORDER_COLOR_TOP = 0x40FF00FF; // 1347420415
    static final int BORDER_COLOR_BOTTOM = 0x40DC0CB0; // 1344798847
    static final int BLENDED_COLOR = 0xFF410040;
    static boolean rendering = false;
    static int x, y;

    @SubscribeEvent
    public static void cancelDefaultTooltip(RenderTooltipEvent event)
    {
        if (Screen.hasControlDown())
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClickTickWise(InputEvent.MouseButton event)
    {
        System.out.println("Action: ");
        System.out.println(event.getAction());
        System.out.println("Button: ");
        System.out.println(event.getButton());
    }

    @SubscribeEvent
    public static void addBottomInformationToTheDefaultTooltipForOpeningTheNewTooltipGui(ItemTooltipEvent event)
    {
        event.getToolTip().addLast(Component.empty());
        event.getToolTip().addLast(Component.literal("Hold [CTRL] for:"));
        event.getToolTip().addLast(Component.literal("<<Enchantment Adjustments>>").withColor(0xFFFF00FF));
    }

    @SubscribeEvent
    public static void blockScreenInputMouseDragged(ScreenEvent.MouseDragged.Pre event)
    {
        event.setCanceled(mouseEventHelper(((int) event.getMouseX()), ((int) event.getMouseY()), event.getScreen().width, event.getScreen().height));
    }

    @SubscribeEvent
    public static void blockScreenInputMouseClick(ScreenEvent.MouseButtonPressed.Pre event)
    {
        event.setCanceled(mouseEventHelper(((int) event.getMouseX()), ((int) event.getMouseY()), event.getScreen().width, event.getScreen().height));
    }

    @SubscribeEvent
    public static void blockScreenInputMouseClickRelease(ScreenEvent.MouseButtonReleased.Pre event)
    {
        event.setCanceled(mouseEventHelper(((int) event.getMouseX()), ((int) event.getMouseY()), event.getScreen().width, event.getScreen().height));
    }

    @SubscribeEvent
    public static void blockScreenInputMouseScroll(ScreenEvent.MouseScrolled.Pre event)
    {
        event.setCanceled(mouseEventHelper(((int) event.getMouseX()), ((int) event.getMouseY()), event.getScreen().width, event.getScreen().height));
    }

    public static boolean mouseEventHelper(int xMouseEvent, int yMouseEvent, int width, int height)
    {
        Vector2ic correctedPosition = positionTooltip(
                width,
                height,
                x,
                y,
                102,
                102
        );

        boolean check = (rendering && ((xMouseEvent > correctedPosition.x() - PADDING && xMouseEvent < correctedPosition.x() - PADDING + 102) && (yMouseEvent > correctedPosition.y() - PADDING && yMouseEvent < correctedPosition.y() - PADDING + 102))) ? true : false;
        // System.out.println(check);
        return check;
    }

    @SubscribeEvent
    public static void letGoOfKey(ScreenEvent.KeyReleased event)
    {
        if (event.getKeyCode() == 341)
        {
            rendering = false;
            System.out.println(rendering);
        }
    }

    @SubscribeEvent
    public static void drawBoxBorders(ScreenEvent.Render event)
    {
        if (Screen.hasControlDown() && !rendering)
        {
            rendering = true;
            System.out.println(rendering);

            x = event.getMouseX();
            y = event.getMouseY();
        }
        else if (!rendering)
            return;

        Vector2ic correctedPosition = positionTooltip(
                event.getScreen().width,
                event.getScreen().height,
                x,
                y,
                102,
                102
        );


        drawRectangle(
                event.getGuiGraphics().pose(),
                correctedPosition.x() - PADDING,
                correctedPosition.y() - PADDING,
                100,
                100,
                BACKGROUND_COLOR,
                BACKGROUND_COLOR,
                900
        );

        drawBoxBorders(
                event.getGuiGraphics().pose(),
                correctedPosition.x() - PADDING,
                correctedPosition.y() - PADDING,
                100,
                100,
                BORDER_COLOR_TOP,
                BORDER_COLOR_BOTTOM,
                true,
                900
        );

        drawBoxBorders(
                event.getGuiGraphics().pose(),
                correctedPosition.x() - 1 - PADDING,
                correctedPosition.y() - 1 - PADDING,
                102,
                102,
                BACKGROUND_COLOR,
                BACKGROUND_COLOR,
                false,
                900
        );

        int verticalDecorLength = 16;
        int horizontalDecorLength = 16;

        // Top Left Vertical Decor
        drawBoxBorders(
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
        drawBoxBorders(
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
        drawBoxBorders(
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
        drawBoxBorders(
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
        drawBoxBorders(
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
        drawBoxBorders(
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
        drawBoxBorders(
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
        drawBoxBorders(
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

    public static void drawBoxBorders(PoseStack poseStack, int x, int y, int width, int height, int colorPrimary, int colorSecondary, boolean drawCorner, int zIndex)
    {
        int additionalPixel = 1;

        if (drawCorner)
            additionalPixel = 0;

        // Top Horizontal Line
        drawRectangle(
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
        drawRectangle(
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
        drawRectangle(
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
        drawRectangle(
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

    public static void drawGradientRectangle(Matrix4f matrix, int zLevel, int left, int top, int right, int bottom, int colorPrimary, int colorSecondary)
    {
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        drawGradientRectangle(matrix, bufferBuilder, left, top, right, bottom, colorPrimary, colorSecondary, zLevel);
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

    public static Vector2ic positionTooltip(int pScreenWidth, int pScreenHeight, int pMouseX, int pMouseY, int pTooltipWidth, int pTooltipHeight)
    {
        Vector2i $$6 = (new Vector2i(pMouseX, pMouseY)).add(12, -12);
        positionTooltip(pScreenWidth, pScreenHeight, $$6, pTooltipWidth, pTooltipHeight);
        return $$6;
    }

    public static void positionTooltip(int pScreenWidth, int pScreenHeight, Vector2i pTooltipPos, int pTooltipWidth, int pTooltipHeight)
    {
        if (pTooltipPos.x + pTooltipWidth > pScreenWidth)
        {
            pTooltipPos.x = Math.max(pTooltipPos.x - 24 - pTooltipWidth, 4);
        }

        int $$5 = pTooltipHeight + 3;
        if (pTooltipPos.y + $$5 > pScreenHeight)
        {
            pTooltipPos.y = pScreenHeight - $$5;
        }
    }
}