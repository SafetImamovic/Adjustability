package io.github.safetimamovic.adjustability.render_core;

import com.mojang.blaze3d.vertex.PoseStack;

public class AdditionalElements
{
    public static void drawDropDownOption(PoseStack poseStack, int x, int y, int width, int height, int zIndex)
    {
        RenderCore.drawBoxBorders(
                poseStack,
                x,
                y,
                width,
                height,
                CustomTooltip.BORDER_COLOR_TOP,
                CustomTooltip.BORDER_COLOR_BOTTOM,
                false,
                zIndex
        );


    }
}
