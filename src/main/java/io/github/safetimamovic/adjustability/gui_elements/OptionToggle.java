package io.github.safetimamovic.adjustability.gui_elements;

import io.github.safetimamovic.adjustability.custom_tooltip.CustomTooltipConfig;
import io.github.safetimamovic.adjustability.render_core.RenderCore;
import net.minecraft.client.gui.GuiGraphics;

public class OptionToggle
{
	public Button leftButton = new Button();
	public Button rightButton = new Button();
	private String text;
	private int x;
	private int y;

	public OptionToggle(int ID1, int ID2, int x, int y, String text)
	{
		this.text = text;

		this.leftButton.setProperties(
			ID1,
			x,
			y,
			CustomTooltipConfig.BUTTON_WIDTH,
			CustomTooltipConfig.BUTTON_HEIGHT,
			CustomTooltipConfig.BACKGROUND_COLOR,
			"<-",
			true
		);

		this.rightButton.setProperties(
			ID2,
			x,
			y,
			CustomTooltipConfig.BUTTON_WIDTH,
			CustomTooltipConfig.BUTTON_HEIGHT,
			CustomTooltipConfig.BACKGROUND_COLOR,
			"->",
			false
		);
	}

	public void render(GuiGraphics pGuiGraphics)
	{
		this.leftButton.render(pGuiGraphics);
		this.rightButton.render(pGuiGraphics);
		RenderCore.renderText(
			this.text,
			this.x + CustomTooltipConfig.BUTTON_WIDTH + CustomTooltipConfig.PADDING,
			this.y,
			CustomTooltipConfig.Z_INDEX_BUTTONS_TEXT,
			pGuiGraphics
		);
	}

	public void update(int x, int y, int mouseX, int mouseY)
	{
		leftButton.positionButtonLeft(x, y);
		rightButton.positionButtonRight(x, y);

		leftButton.hover(mouseX, mouseY);
		rightButton.hover(mouseX, mouseY);
	}
}
